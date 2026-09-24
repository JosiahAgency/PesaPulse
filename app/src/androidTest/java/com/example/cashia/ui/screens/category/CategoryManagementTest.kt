package com.example.cashia.ui.screens.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cashia.AppDatabase
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.CustomCategory
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.RecurrenceType
import com.example.cashia.models.TransactionCategory
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingType
import com.example.cashia.models.UserCorrection
import com.example.cashia.ui.theme.DarkColorScheme
import com.example.cashia.ui.theme.LightColorScheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CategoryManagementTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var viewModel: CategoryViewModel
    private val store = ViewModelStore()
    private val largeText = mutableStateOf(false)
    private val darkTheme = mutableStateOf(true)

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        viewModel = CategoryViewModel(context, db)
        store.put("categories", viewModel)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, if (largeText.value) 1.5f else 1f)
            ) {
                MaterialTheme(colorScheme = if (darkTheme.value) DarkColorScheme else LightColorScheme) {
                    CategoryManagementScreen(viewModel)
                }
            }
        }
    }

    @After
    fun tearDown() {
        compose.runOnIdle { store.clear() }
        db.close()
    }

    @Test
    fun searchFindsSubcategoriesAndKeepsTheirParentVisible() {
        compose.onNodeWithText("Search names or your keywords").performTextInput("Groceries")
        compose.onNodeWithText("Food & Drinks").assertIsDisplayed()
        compose.onNode(hasText("Groceries") and !hasSetTextAction()).assertIsDisplayed()
        compose.onNodeWithText("Restaurants & Cafes").assertDoesNotExist()
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.onNodeWithText("New category").assertIsDisplayed()
    }

    @Test
    fun createCategorySavesNameAndKeywordsWithAccessiblePersistentSaveAction() {
        compose.onNodeWithText("New category").performClick()
        compose.onNodeWithText("Create category").assertIsNotEnabled()
        compose.onNodeWithText("Category name").performTextInput("Family")
        compose.onNodeWithText("Matching keywords").performScrollTo().performTextInput("mum, school fees")
        compose.onNodeWithText("Create category").assertIsDisplayed().assertIsEnabled().performClick()
        compose.waitUntil(5_000) { !viewModel.uiState.showCreateSheet }

        val category = runBlocking { db.customCategoryDao().getAllCustomCategoriesOnce().single() }
        assertEquals("Family", category.label)
        assertEquals("mum, school fees", category.keywords)
        compose.onNodeWithText("Family").assertIsDisplayed()
    }

    @Test
    fun unsavedChangesRequireConfirmationAndKeepEditingPreservesDraft() {
        compose.onNodeWithText("New category").performClick()
        compose.onNodeWithText("Category name").performTextInput("Family")
        compose.onNodeWithContentDescription("Close category editor").performClick()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("Category name").assertIsDisplayed()
        assertEquals("Family", viewModel.uiState.draftLabel)
        compose.onNodeWithContentDescription("Close category editor").performClick()
        compose.onNodeWithText("Discard changes").performClick()
        compose.onNodeWithText("New category").assertIsDisplayed()
        assertTrue(runBlocking { db.customCategoryDao().getAllCustomCategoriesOnce().isEmpty() })
    }

    @Test
    fun builtInEditorSavesPresentationAndDoesNotOfferUnsupportedKeywords() {
        compose.runOnIdle { viewModel.openEditSystemSheet(TransactionCategory.FOOD) }
        compose.onNodeWithText("Matching keywords").assertDoesNotExist()
        compose.runOnIdle {
            viewModel.onLabelChange("Meals")
            viewModel.onEmojiChange("🍴")
        }
        compose.onNodeWithText("Save changes").performClick()
        compose.waitUntil(5_000) { !viewModel.uiState.showCreateSheet }
        val override = runBlocking { db.systemCategoryOverrideDao().getAllOverridesOnce().single() }
        assertEquals("Meals", override.label)
        assertEquals("🍴", override.emoji)
        compose.runOnIdle { viewModel.resetSystemCategory(TransactionCategory.FOOD) }
        compose.waitUntil(5_000) { !viewModel.isProcessing }
        assertTrue(runBlocking { db.systemCategoryOverrideDao().getAllOverridesOnce().isEmpty() })
    }

    @Test
    fun deleteAlsoRemovesLearnedMatchesAndRepairsUpcomingAndChildReferences() {
        val category = CustomCategory("custom_family", "Family", "X", "mum")
        runBlocking {
            db.customCategoryDao().upsertCategory(category)
            db.customCategoryDao().upsertCategory(category.copy(id = "child", parentId = category.id))
            db.userCorrectionDao().upsertCorrection(UserCorrection("mum", category.id))
            db.upcomingTransactionDao().upsert(
                UpcomingTransaction(
                    id = "upcoming", title = "School", amount = 100.0,
                    categoryId = category.id, dueDate = "2026-10-01",
                    type = UpcomingType.EXPENSE, recurrence = RecurrenceType.NONE
                )
            )
        }
        compose.runOnIdle { viewModel.deleteCategory(category) }
        compose.waitUntil(5_000) { !viewModel.isProcessing }
        runBlocking {
            assertEquals("OTHER", db.upcomingTransactionDao().getAll().first().single().categoryId)
            assertNull(db.userCorrectionDao().getCorrectionByRecipient("mum"))
            val remaining = db.customCategoryDao().getAllCustomCategoriesOnce().single()
            assertEquals("child", remaining.id)
            assertNull(remaining.parentId)
        }
    }

    @Test
    fun autoSortConfirmsAndReportsWhenNoPaymentsMatch() {
        compose.onNodeWithText("Auto-sort").performClick()
        compose.onNodeWithText("Organise uncategorised payments?").assertIsDisplayed()
        compose.onNodeWithText("Organise payments").performClick()
        compose.waitUntil(5_000) { !viewModel.isProcessing && viewModel.operationMessage != null }
        assertFalse(viewModel.isProcessing)
        compose.onNodeWithText("Auto-sort complete: 0 transactions updated. No new matches found.").assertIsDisplayed()
    }

    @Test
    fun autoSortReportsOnlyChangedTransactionsAndDoesNotCountThemAgain() {
        verifyAutoSortCount(2, "Auto-sort complete: 2 transactions updated.")
    }

    @Test
    fun autoSortReportsOneChangedTransaction() {
        verifyAutoSortCount(1, "Auto-sort complete: 1 transaction updated.")
    }

    private fun verifyAutoSortCount(matches: Int, expectedMessage: String) {
        val category = CustomCategory("custom_test", "Test category", "X", "testrecipient")
        val base = MpesaTransactions(
            id = "1", smsId = 1L, amount = 100.0, transactionCost = 0.0,
            recipient = "testrecipient", date = "2026-09-24", type = "expense",
            balance = 500.0, time = "10:00 AM", timestamp = 1L
        )
        val unchanged = listOf(
            base.copy(id = "existing", smsId = 100L, category = TransactionCategory.FOOD.name),
            base.copy(id = "unmatched", smsId = 101L, recipient = "")
        )
        runBlocking {
            db.customCategoryDao().upsertCategory(category)
            db.transactionDao().insertTransactions(
                (1..matches).map { base.copy(id = it.toString(), smsId = it.toLong()) } + unchanged
            )
        }

        compose.onNodeWithText("Auto-sort").performClick()
        compose.onNodeWithText("Organise payments").performClick()
        compose.waitUntil(5_000) { !viewModel.isProcessing && viewModel.operationMessage != null }
        compose.onNodeWithText(expectedMessage).assertIsDisplayed()
        runBlocking {
            val transactions = db.transactionDao().getAllTransactionsOnce()
            assertEquals(matches, transactions.count { it.category == category.id })
            unchanged.forEach { original ->
                assertEquals(original.category, transactions.single { it.id == original.id }.category)
            }
        }

        compose.onNodeWithContentDescription("Dismiss message").performClick()
        compose.onNodeWithText(expectedMessage).assertDoesNotExist()
        compose.onNodeWithText("Auto-sort").performClick()
        compose.onNodeWithText("Organise payments").performClick()
        compose.waitUntil(5_000) { !viewModel.isProcessing && viewModel.operationMessage != null }
        compose.onNodeWithText("Auto-sort complete: 0 transactions updated. No new matches found.").assertIsDisplayed()
    }

    @Test
    fun lightThemeAndLargeTextKeepPrimaryActionsReachable() {
        compose.runOnIdle {
            darkTheme.value = false
            largeText.value = true
        }
        compose.onNodeWithText("New category").assertIsDisplayed().performClick()
        compose.onNodeWithText("Category name").performScrollTo().performTextInput("Family")
        compose.onNodeWithText("Matching keywords").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Create category").assertIsDisplayed().assertIsEnabled()
    }
}
