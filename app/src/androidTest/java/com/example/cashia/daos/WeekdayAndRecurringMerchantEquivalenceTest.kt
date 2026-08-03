package com.example.cashia.daos

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cashia.AppDatabase
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.utility.detectRecurringMerchants
import com.example.cashia.utility.weekdaySpendPattern
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/**
 * Verifies that [TransactionDao.getWeekdaySpendPattern] / [TransactionDao.getRecurringMerchants]
 * (SQL aggregates, run over a bounded/full table) produce the same results as the original
 * in-memory [weekdaySpendPattern] / [detectRecurringMerchants] functions on the same fixture
 * data — the precondition for switching AnalyticsScreen's WeekdaySpendCard/RecurringMerchantsCard
 * off client-side computation.
 */
@RunWith(AndroidJUnit4::class)
class WeekdayAndRecurringMerchantEquivalenceTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.transactionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tx(
        id: String,
        date: String,
        amount: Double,
        recipient: String = "Test Merchant",
        type: String = "expense",
        time: String = "10:00 AM",
        reversedTxId: String? = null,
    ): MpesaTransactions {
        val timestamp = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        return MpesaTransactions(
            id = id,
            smsId = id.toLong(), // MpesaTransactions.smsId has a UNIQUE index — default 0L for
            // every row would collide and get silently dropped by OnConflictStrategy.IGNORE.
            amount = amount,
            transactionCost = 0.0,
            recipient = recipient,
            date = date,
            type = type,
            balance = 1000.0,
            time = time,
            timestamp = timestamp,
            reversedTxId = reversedTxId,
        )
    }

    @Test
    fun weekdaySpendPattern_matchesInMemoryReference() = runBlocking {
        // Spread across several weeks so weekday grouping and multi-day averaging both exercise.
        val fixture = listOf(
            tx("1", "2026-07-06", 100.0), // Monday
            tx("2", "2026-07-13", 300.0), // Monday
            tx("3", "2026-07-07", 50.0),  // Tuesday
            tx("4", "2026-07-12", 200.0), // Sunday
            tx("5", "2026-07-19", 400.0), // Sunday
            tx("6", "2026-07-10", 75.0, type = "income"), // Friday, different type — excluded
            tx("7", "2026-07-11", 60.0, reversedTxId = "1"), // reversed — excluded
        )
        dao.insertTransactions(fixture)

        val expected = weekdaySpendPattern(fixture, "expense")
        val actualRows = dao.getWeekdaySpendPattern("expense").first()
        val byDow = actualRows.associateBy { it.dow }
        val actual = DayOfWeek.entries.map { dow ->
            val row = byDow[dow.value % 7]
            Triple(
                dow,
                if (row == null || row.activeDays == 0) 0.0 else row.totalAmount / row.activeDays,
                row?.txCount ?: 0
            )
        }

        assertEquals(expected.size, actual.size)
        expected.forEachIndexed { i, e ->
            assertEquals("day", e.day, actual[i].first)
            assertEquals("avgAmount for ${e.day}", e.avgAmount, actual[i].second, 0.001)
            assertEquals("txCount for ${e.day}", e.txCount, actual[i].third)
        }
    }

    @Test
    fun recurringMerchants_matchesInMemoryReference() = runBlocking {
        val fixture = listOf(
            // "Java House" appears in 3 distinct months -> recurring
            tx("1", "2026-05-05", 500.0, recipient = "Java House"),
            tx("2", "2026-06-05", 600.0, recipient = "JAVA HOUSE"),
            tx("3", "2026-07-05", 700.0, recipient = "java house"),
            // "Corner Shop" appears in only 1 month -> not recurring
            tx("4", "2026-07-01", 50.0, recipient = "Corner Shop"),
            tx("5", "2026-07-02", 60.0, recipient = "Corner Shop"),
            // Different type, otherwise would qualify -> excluded
            tx("6", "2026-01-01", 10.0, recipient = "Java House", type = "income"),
            // Reversed -> excluded
            tx("7", "2026-08-05", 999.0, recipient = "Java House", reversedTxId = "1"),
        )
        dao.insertTransactions(fixture)

        // Production feeds detectRecurringMerchants() from getAllTransactions() (ORDER BY
        // timestamp DESC), so `txs.first()` per group resolves to the most recent transaction.
        // Mirror that ordering here — the SQL query picks the same row via a MAX(timestamp)
        // bare-column trick (see TransactionDao.getRecurringMerchants).
        val expected = detectRecurringMerchants(fixture.sortedByDescending { it.timestamp }, "expense")
        val actualRows = dao.getRecurringMerchants("expense", minMonths = 2, limit = 8).first()

        assertEquals(expected.size, actualRows.size)
        expected.forEachIndexed { i, e ->
            val a = actualRows[i]
            assertEquals("recipient", e.recipient.trim().lowercase(), a.recipient.trim().lowercase())
            assertEquals("occurrences", e.occurrences, a.occurrences)
            assertEquals("totalAmount", e.totalAmount, a.totalAmount, 0.001)
            assertEquals("avgAmount", e.avgAmount, a.totalAmount / a.occurrences, 0.001)
            assertEquals("lastDate", e.lastDate.toString(), a.lastDate)
        }
    }
}
