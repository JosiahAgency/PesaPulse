package com.example.cashia.utility

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cashia.AppDatabase
import com.example.cashia.models.*
import com.example.cashia.ui.screens.allTransactions.UnmatchedSmsModal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CorrectionFormTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AppDatabase
    private lateinit var transactions: TransactionsViewModel
    private lateinit var categories: CategoryViewModel
    private val store = ViewModelStore()
    private val visible = mutableStateOf(true)
    private val sms = UnmatchedSms(id = 1, smsId = 42, rawBody = "Unrecognised M-PESA payment")

    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        transactions = TransactionsViewModel(context, db)
        categories = CategoryViewModel(context, db)
        store.put("transactions", transactions)
        store.put("categories", categories)
        runBlocking { db.unmatchedSmsDao().insert(sms) }
        compose.setContent {
            MaterialTheme {
                if (visible.value) UnmatchedSmsModal(
                    sms, 1, emptyList(), categories,
                    onSave = { transactions.saveTransactionFromSms(sms, it) },
                    onSkip = {}, onIgnore = {}, onDismiss = { visible.value = false }
                )
            }
        }
    }

    @After fun tearDown() {
        compose.runOnIdle { store.clear() }
        db.close()
    }

    @Test fun invalidSaveStaysOpenAndValidCorrectionPreservesUnknowns() {
        compose.onNodeWithText("Save transaction").performClick()
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("Enter a valid amount with at most two decimal places.").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(visible.value)
        compose.onNodeWithText("Amount (KES) *").performScrollTo().performTextInput("1000")
        compose.onNodeWithText("Recipient / sender *").performScrollTo().performTextInput("Jane Wanjiku")
        compose.onNodeWithText("Date (YYYY-MM-DD) *").performScrollTo().performTextInput("2024-01-31")
        compose.onNodeWithText("Money out").performScrollTo().performClick()
        compose.onNodeWithText("Save transaction").performClick()
        compose.waitUntil(5000) { !visible.value }
        val tx = runBlocking { db.transactionDao().getAllTransactionsOnce().single() }
        assertEquals(1000.0, tx.amount, 0.0)
        assertNull(tx.time)
        assertNull(tx.balance)
        assertNull(tx.transactionCost)
        assertNull(tx.reference)
        assertEquals(0, runBlocking { db.unmatchedSmsDao().pendingCount() })
    }

    @Test fun duplicateCorrectionReturnsErrorAndLeavesMessageInReview() = runBlocking {
        val correction = TransactionCorrection(amount = "1000", recipient = "Jane Wanjiku", date = "2024-01-31", type = "expense")
        db.transactionDao().insertCorrection(correction.toTransaction(sms))
        val error = transactions.saveTransactionFromSms(sms, correction)
        assertNotNull(error)
        assertEquals(1, db.unmatchedSmsDao().pendingCount())
        assertEquals(1, db.transactionDao().getAllTransactionsOnce().size)
    }
}
