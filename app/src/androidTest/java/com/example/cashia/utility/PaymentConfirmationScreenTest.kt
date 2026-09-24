package com.example.cashia.utility

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cashia.AppDatabase
import com.example.cashia.models.*
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingPaymentsScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PaymentConfirmationScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AppDatabase
    private lateinit var upcoming: UpcomingViewModel
    private val store = ViewModelStore()
    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        runBlocking {
            db.upcomingTransactionDao().upsert(UpcomingTransaction(
                id = "bill", title = "Jane Wanjiku", amount = 1000.0, categoryId = "OTHER",
                dueDate = "2024-01-31", type = UpcomingType.EXPENSE, recurrence = RecurrenceType.MONTHLY
            ))
            db.transactionDao().insertTransactions(listOf(MpesaTransactions(
                id = "ABC1234567", smsId = 1, amount = 1000.0, recipient = "Jane Wanjiku",
                date = "2024-01-31", type = "expense", timestamp = 1706659200000,
                transactionCost = null, balance = null, time = null
            )))
        }
        upcoming = UpcomingViewModel(context, db)
        val categories = CategoryViewModel(context, db)
        store.put("upcoming", upcoming)
        store.put("categories", categories)
        compose.setContent { MaterialTheme { UpcomingPaymentsScreen(upcoming, categories) } }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Confirm payment").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun tearDown() {
        compose.runOnIdle { store.clear() }
        db.close()
    }
    @Test fun paymentRemainsPendingUntilExplicitConfirmation() {
        assertEquals(UpcomingStatus.PENDING, runBlocking { db.upcomingTransactionDao().getById("bill")!!.status })
        compose.onNodeWithText("Confirm payment").performScrollTo().performClick()
        compose.waitUntil(5000) { upcoming.operationMessage.value == "Payment confirmed." }
        assertEquals(UpcomingStatus.PAID, runBlocking { db.upcomingTransactionDao().getById("bill")!!.status })
        assertEquals("2024-02-29", runBlocking { db.upcomingTransactionDao().getPendingOnce().single().dueDate })
    }
    @Test fun dismissDoesNotCompleteOrRecreateTheSuggestion() {
        compose.onNodeWithText("Not this payment").performScrollTo().performClick()
        compose.waitUntil(5000) { upcoming.operationMessage.value?.startsWith("Suggestion dismissed.") == true }
        runBlocking { UpcomingPaymentService(db).reconcilePending() }
        assertEquals(UpcomingStatus.PENDING, runBlocking { db.upcomingTransactionDao().getById("bill")!!.status })
        compose.onNodeWithText("Confirm payment").assertDoesNotExist()
    }
}
