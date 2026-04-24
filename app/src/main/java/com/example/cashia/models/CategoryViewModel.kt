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

    val customCategories: StateFlow<List<CustomCategory>> =
        categoryDao.getAllCustomCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemOverrides: StateFlow<Map<String, SystemCategoryOverride>> =
        overrideDao.getAllOverrides()
            .map { list -> list.associateBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val allCategories: StateFlow<List<ResolvedCategory>> =
        combine(
            overrideDao.getAllOverrides()
                .distinctUntilChanged()         // ← skip if overrides unchanged
                .map { it.associateBy { o -> o.id } },
            categoryDao.getAllCustomCategories()
                .distinctUntilChanged()         // ← skip if custom cats unchanged
        ) { overrides, customs ->
            // Runs on IO via flowOn below
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


    var uiState by mutableStateOf(CategoryManageState())
        private set

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

    fun onLabelChange(value: String) {
        uiState = uiState.copy(draftLabel = value, error = null)
    }

    fun onEmojiChange(value: String) {
        uiState = uiState.copy(draftEmoji = value)
    }

    fun onKeywordsChange(value: String) {
        uiState = uiState.copy(draftKeywords = value)
    }

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
                // Saving a system category override
                overrideDao.upsertOverride(
                    SystemCategoryOverride(
                        id = systemCat.name,
                        label = label,
                        emoji = uiState.draftEmoji.ifBlank { systemCat.emoji }
                    )
                )
            } else {
                // Saving a custom category (create or edit)
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

                if (keywords.isNotBlank()) {
                    reclassifyWithCustomCategory(category)
                }
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

    fun reclassifyAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val customs = categoryDao.getAllCustomCategoriesOnce()
            val uncategorized = txDao.getUncategorizedTransactions()
            val updates = uncategorized.mapNotNull { tx ->
                val guessed = guessCategory(tx.recipient, tx.type, customs)
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

    // ── Manual override for a single transaction ──────────────────────────

    fun updateTransactionCategory(txId: String, categoryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            txDao.updateCategory(txId, categoryId)
        }
    }

    fun topLevelCategories(all: List<ResolvedCategory>): List<ResolvedCategory> =
        all.filter { cat ->
            if (cat.isSystem) {
                TransactionCategory.entries.find { it.name == cat.id }?.parentId == null
            } else {
                cat.parentId == null
            }
        }

    fun childCategories(all: List<ResolvedCategory>, parentId: String): List<ResolvedCategory> =
        all.filter { cat ->
            if (cat.isSystem) {
                TransactionCategory.entries.find { it.name == cat.id }?.parentId == parentId
            } else {
                cat.parentId == parentId
            }
        }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
                        return CategoryViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}