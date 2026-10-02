package com.example.cashia.utility

import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cashia.AppDatabase
import com.example.cashia.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PaymentReliabilityTest {
    private lateinit var db: AppDatabase
    private lateinit var service: UpcomingPaymentService
    private val payment = UpcomingTransaction(
        id = "rent", title = "Jane Wanjiku", amount = 1000.0, categoryId = "OTHER",
        dueDate = "2024-01-31", type = UpcomingType.EXPENSE, recurrence = RecurrenceType.MONTHLY
    )
    private val tx = MpesaTransactions(
        id = "ABC1234567", smsId = 1, amount = 1000.0, transactionCost = null,
        recipient = "Jane Wanjiku", date = "2024-01-31", type = "expense",
        balance = null, time = null, timestamp = 1706659200000
    )

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        service = UpcomingPaymentService(db)
    }
    @After fun tearDown() { db.close() }

    @Test fun catchupCreatesSuggestionsWithoutCompletingPaymentsAndDismissalSurvives() = runBlocking {
        db.upcomingTransactionDao().upsert(payment)
        db.transactionDao().insertTransactions(listOf(tx))
        service.reconcilePending()
        assertEquals(UpcomingStatus.PENDING, db.upcomingTransactionDao().getById(payment.id)!!.status)
        assertEquals(1, db.paymentMatchDao().pendingReviews().first().size)
        service.reconcilePending()
        assertEquals(1, db.paymentMatchDao().pendingReviews().first().size)
        db.paymentMatchDao().dismiss(payment.id, tx.id)
        service.reconcilePending()
        assertTrue(db.paymentMatchDao().pendingReviews().first().isEmpty())
        assertFalse(service.confirm(payment.id, tx.id))
    }

    @Test fun simultaneousConfirmationCreatesExactlyOneAnchoredSuccessor() = runBlocking {
        db.upcomingTransactionDao().upsert(payment)
        db.transactionDao().insertTransactions(listOf(tx))
        service.suggest(listOf(tx))
        val results = (1..12).map { async(Dispatchers.IO) { service.confirm(payment.id, tx.id) } }.awaitAll()
        assertEquals(1, results.count { it })
        assertEquals(UpcomingStatus.PAID, db.upcomingTransactionDao().getById(payment.id)!!.status)
        val february = db.upcomingTransactionDao().getPendingOnce().single()
        assertEquals("2024-02-29", february.dueDate)
        assertEquals("2024-01-31", february.recurrenceAnchor)
        assertTrue(service.complete(february.id, UpcomingStatus.SKIPPED))
        assertFalse(service.complete(february.id, UpcomingStatus.SKIPPED))
        assertEquals("2024-03-31", db.upcomingTransactionDao().getPendingOnce().single().dueDate)
    }

    @Test fun oneTransactionCannotCompleteTwoPayments() = runBlocking {
        db.upcomingTransactionDao().upsert(payment)
        db.upcomingTransactionDao().upsert(payment.copy(id = "second"))
        db.transactionDao().insertTransactions(listOf(tx))
        service.suggest(listOf(tx))
        assertEquals(2, db.paymentMatchDao().pendingReviews().first().size)
        val results = listOf(payment.id, "second").map {
            async(Dispatchers.IO) { service.confirm(it, tx.id) }
        }.awaitAll()
        assertEquals(1, results.count { it })
        assertTrue(db.paymentMatchDao().pendingReviews().first().isEmpty())
    }

    @Test fun reversalOrEditedRecipientInvalidatesConfirmation() = runBlocking {
        db.upcomingTransactionDao().upsert(payment)
        db.transactionDao().insertTransactions(listOf(tx))
        service.suggest(listOf(tx))
        db.upcomingTransactionDao().upsert(payment.copy(title = "Someone Else"))
        assertFalse(service.confirm(payment.id, tx.id))
        db.upcomingTransactionDao().upsert(payment)
        db.transactionDao().insertTransactions(listOf(tx.copy(id = "REV1234567", smsId = 2, type = "income", reversedTxId = tx.id)))
        assertFalse(service.confirm(payment.id, tx.id))
        assertTrue(db.paymentMatchDao().pendingReviews().first().isEmpty())
    }

    @Test fun nullFeesNeverRemoveThePrincipalFromSqlTotalsAndConflictsRollback() = runBlocking {
        db.transactionDao().insertCorrection(tx)
        val totals = db.transactionDao().getFilteredTotals("", null, emptyList(), null, null, null).first()
        assertEquals(1000.0, totals.expenses, 0.0)
        assertNull(db.transactionDao().getLatestHeadlineOnce()?.balance)
        db.unmatchedSmsDao().insert(UnmatchedSms(id = 1, smsId = 1, rawBody = "Original"))
        try {
            db.withTransaction {
                db.unmatchedSmsDao().setStatus(1, SmsStatus.SAVED)
                db.transactionDao().insertCorrection(tx.copy(id = "OTHER12345"))
            }
            fail("Duplicate SMS must fail")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            assertEquals(1, db.unmatchedSmsDao().pendingCount())
        }
    }

    @Test fun sharedIngestionCommitsSuggestionsAndCursorWithoutAnOpenUi() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db.upcomingTransactionDao().upsert(payment)
        val body = "ABC1234567 Confirmed. Ksh1,000.00 sent to Jane Wanjiku on 31/1/24 at 1:30 PM. New M-PESA balance is Ksh500.00. Transaction cost, Ksh10.00."
        val messages = listOf(SmsRepository.SmsMessage(42, body, tx.timestamp))
        assertEquals(1, syncTransactions(context, db) { _, cursor ->
            assertEquals(0L, cursor)
            messages
        }.size)
        assertEquals(42L, db.syncStateDao().getLastSmsId("mpesa_sync"))
        assertEquals(1, db.paymentMatchDao().pendingReviews().first().size)
        assertEquals(UpcomingStatus.PENDING, db.upcomingTransactionDao().getById(payment.id)!!.status)
        assertTrue(syncTransactions(context, db) { _, cursor ->
            assertEquals(42L, cursor)
            emptyList()
        }.isEmpty())
        assertEquals(1, db.paymentMatchDao().pendingReviews().first().size)
    }

    @Test fun failedSuggestionPersistenceDoesNotAdvanceTheCursorOrInsertTransaction() = runBlocking {
        db.upcomingTransactionDao().upsert(payment)
        db.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_match BEFORE INSERT ON PaymentMatches BEGIN SELECT RAISE(ABORT, 'test failure'); END"
        )
        val body = "ABC1234567 Confirmed. Ksh1,000.00 sent to Jane Wanjiku on 31/1/24 at 1:30 PM. New M-PESA balance is Ksh500.00."
        try {
            syncTransactions(InstrumentationRegistry.getInstrumentation().targetContext, db) { _, _ ->
                listOf(SmsRepository.SmsMessage(42, body, tx.timestamp))
            }
            fail("Match failure must roll back ingestion")
        } catch (_: android.database.sqlite.SQLiteException) {
            assertNull(db.syncStateDao().getLastSmsId("mpesa_sync"))
            assertTrue(db.transactionDao().getAllTransactionsOnce().isEmpty())
        }
    }
}
