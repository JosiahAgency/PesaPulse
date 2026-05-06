package com.example.cashia.models

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class CategoryManageState(
    val showCreateSheet: Boolean = false,
    val editingCategory: CustomCategory? = null,
    val editingSystemCategory: TransactionCategory? = null,
    val draftLabel: String = "",
    val draftEmoji: String = "📦",
    val draftKeywords: String = "",
    val error: String? = null
)

class CategoryViewModel(private val context: Context) : ViewModel() {

    private val db = AppDatabase.getDatabase(context)
    private val categoryDao = db.customCategoryDao()
    private val txDao = db.transactionDao()
    private val overrideDao = db.systemCategoryOverrideDao()

    // ── NEW: User corrections DAO ─────────────────────────────────────────
    private val correctionDao = db.userCorrectionDao()

    // ── Existing flows ────────────────────────────────────────────────────

    val customCategories: StateFlow<List<CustomCategory>> =
        categoryDao.getAllCustomCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemOverrides: StateFlow<Map<String, SystemCategoryOverride>> =
        overrideDao.getAllOverrides()
            .map { list -> list.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Live map of user corrections: normalised recipient → categoryId.
     * Used by [guessCategory] as the highest-priority lookup.
     */
    val userCorrections: StateFlow<Map<String, String>> =
        correctionDao.getAllCorrections()
            .map { list -> list.associate { it.recipient to it.categoryId } }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allCategories: StateFlow<List<ResolvedCategory>> =
        combine(
            overrideDao.getAllOverrides()
                .distinctUntilChanged()
                .map { it.associateBy { o -> o.id } },
            categoryDao.getAllCustomCategories()
                .distinctUntilChanged()
        ) { overrides, customs ->
            TransactionCategory.entries.map { cat ->
                val ov = overrides[cat.name]
                ResolvedCategory(
                    id = cat.name,
                    label = ov?.label ?: cat.label,
                    emoji = ov?.emoji ?: cat.emoji,
                    isSystem = true,
                    parentId = cat.parentId
                )
            } + customs.map { it.toResolved() }
        }
            .flowOn(Dispatchers.IO)
            .distinctUntilChanged()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                TransactionCategory.entries.map { cat ->
                    ResolvedCategory(
                        id = cat.name,
                        label = cat.label,
                        emoji = cat.emoji,
                        isSystem = true,
                        parentId = cat.parentId
                    )
                }
            )

    // ── UI state ──────────────────────────────────────────────────────────

    var uiState by mutableStateOf(CategoryManageState())
        private set

    // ── Sheet open / close ────────────────────────────────────────────────

    fun openCreateSheet() {
        uiState = CategoryManageState(showCreateSheet = true)
    }

    fun openEditSheet(category: CustomCategory) {
        uiState = CategoryManageState(
            showCreateSheet = true,
            editingCategory = category,
            draftLabel = category.label,
            draftEmoji = category.emoji,
            draftKeywords = category.keywords
        )
    }

    fun openEditSystemSheet(category: TransactionCategory) {
        val existing = systemOverrides.value[category.name]
        uiState = CategoryManageState(
            showCreateSheet = true,
            editingSystemCategory = category,
            draftLabel = existing?.label ?: category.label,
            draftEmoji = existing?.emoji ?: category.emoji,
            draftKeywords = ""
        )
    }

    fun resetSystemCategory(category: TransactionCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            overrideDao.deleteOverride(category.name)
        }
    }

    fun closeSheet() {
        uiState = CategoryManageState()
    }

    // ── Draft field mutations ─────────────────────────────────────────────

    fun onLabelChange(value: String) {
        uiState = uiState.copy(draftLabel = value, error = null)
    }

    fun onEmojiChange(value: String) {
        uiState = uiState.copy(draftEmoji = value)
    }

    fun onKeywordsChange(value: String) {
        uiState = uiState.copy(draftKeywords = value)
    }

    // ── Save / delete custom categories ───────────────────────────────────

    fun saveInlineCategory(category: CustomCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            categoryDao.upsertCategory(category)
            if (category.keywords.isNotBlank()) {
                reclassifyWithCustomCategory(category)
            }
        }
    }

    fun deleteResolvedCategory(cat: ResolvedCategory) {
        if (!cat.isSystem) {
            viewModelScope.launch(Dispatchers.IO) {
                txDao.resetDeletedCategory(cat.id)
                categoryDao.deleteCategoryById(cat.id)
            }
        }
    }

    fun saveCategory() {
        val label = uiState.draftLabel.trim()
        if (label.isBlank()) {
            uiState = uiState.copy(error = "Category name cannot be empty")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val systemCat = uiState.editingSystemCategory
            if (systemCat != null) {
                overrideDao.upsertOverride(
                    SystemCategoryOverride(
                        id = systemCat.name,
                        label = label,
                        emoji = uiState.draftEmoji.ifBlank { systemCat.emoji }
                    )
                )
            } else {
                val keywords = uiState.draftKeywords.trim()
                val id = uiState.editingCategory?.id
                    ?: "custom_${UUID.randomUUID().toString().replace("-", "").take(8)}"
                val category = CustomCategory(
                    id = id,
                    label = label,
                    emoji = uiState.draftEmoji.ifBlank { "📦" },
                    keywords = keywords,
                    createdAt = uiState.editingCategory?.createdAt ?: System.currentTimeMillis()
                )
                categoryDao.upsertCategory(category)
                if (keywords.isNotBlank()) reclassifyWithCustomCategory(category)
            }
            uiState = CategoryManageState()
        }
    }

    fun deleteCategory(category: CustomCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            txDao.resetDeletedCategory(category.id)
            categoryDao.deleteCategoryById(category.id)
        }
    }

    // ── Reclassify ────────────────────────────────────────────────────────

    fun reclassifyAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val customs = categoryDao.getAllCustomCategoriesOnce()
            val corrections = correctionDao.getAllCorrectionsOnce()
                .associate { it.recipient to it.categoryId }
            val uncategorized = txDao.getUncategorizedTransactions()
            val updates = uncategorized.mapNotNull { tx ->
                val guessed = guessCategory(tx.recipient, tx.type, customs, corrections)
                if (guessed != TransactionCategory.OTHER.name) tx.id to guessed else null
            }
            updates.forEach { (id, cat) -> txDao.updateCategoryById(id, cat) }
        }
    }

    private suspend fun reclassifyWithCustomCategory(newCategory: CustomCategory) {
        val uncategorized = txDao.getUncategorizedTransactions()
        val keywords = newCategory.keywords.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        uncategorized.forEach { tx ->
            if (keywords.any { kw -> tx.recipient.lowercase().contains(kw) }) {
                txDao.updateCategoryById(tx.id, newCategory.id)
            }
        }
    }

    // ── Manual override with feedback loop ────────────────────────────────

    /**
     * Called when the user explicitly picks a category for a transaction.
     *
     * Two things happen:
     *  1. The transaction's category is updated immediately.
     *  2. A [UserCorrection] is persisted so that future transactions from the
     *     same recipient are auto-assigned to the same category without needing
     *     a keyword rule.  If a correction for this recipient already exists its
     *     [UserCorrection.hitCount] is incremented and the category is updated.
     */
    fun updateTransactionCategory(txId: String, categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update the specific transaction
            txDao.updateCategory(txId, categoryId)

            // 2. Persist the correction keyed on the normalised recipient
            val tx = txDao.getTransactionById(txId) ?: return@launch
            val key = tx.recipient.lowercase().trim()

            val existing = correctionDao.getCorrectionByRecipient(key)
            if (existing != null) {
                correctionDao.upsertCorrection(
                    existing.copy(
                        categoryId = categoryId,
                        hitCount = existing.hitCount + 1,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                correctionDao.upsertCorrection(
                    UserCorrection(
                        recipient = key,
                        categoryId = categoryId
                    )
                )
            }

            // 3. Optionally re-classify OTHER transactions with the same recipient
            //    that are still in "OTHER" or a different auto-guessed category.
            txDao.updateCategoryForRecipient(
                recipientLower = key,
                newCategory = categoryId,
                excludeTxId = txId
            )
        }
    }

    /**
     * Remove a stored correction so the engine falls back to keyword matching
     * for future transactions from this recipient.
     */
    fun deleteCorrection(recipient: String) {
        viewModelScope.launch(Dispatchers.IO) {
            correctionDao.deleteCorrectionByRecipient(recipient.lowercase().trim())
        }
    }

    // ── Category tree helpers ─────────────────────────────────────────────

    fun topLevelCategories(all: List<ResolvedCategory>): List<ResolvedCategory> =
        all.filter { cat ->
            if (cat.isSystem)
                TransactionCategory.entries.find { it.name == cat.id }?.parentId == null
            else
                cat.parentId == null
        }

    fun childCategories(all: List<ResolvedCategory>, parentId: String): List<ResolvedCategory> =
        all.filter { cat ->
            if (cat.isSystem)
                TransactionCategory.entries.find { it.name == cat.id }?.parentId == parentId
            else
                cat.parentId == parentId
        }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CategoryViewModel::class.java))
                        return CategoryViewModel(context) as T
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}