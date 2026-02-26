package com.example.cashia.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.AppDatabase
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.UUID

class CategoryViewModel(private val db: AppDatabase) : ViewModel() {
	   
	   val categories = flow { emit(db.categoryDao().getAllCategories()) }
	   
	   fun addCategory(name: String, emoji: String, keywords: String) = viewModelScope.launch {
			db.categoryDao().insertCategory(
				   Category(
						id = UUID.randomUUID().toString(),
						name = name.trim(),
						emoji = emoji.trim(),
						keywords = keywords.lowercase().trim(),
						isDefault = false
				   )
			)
	   }
	   
	   fun updateCategory(category: Category) = viewModelScope.launch {
			db.categoryDao().updateCategory(category)
			// Re-run auto-categorization for all uncategorized transactions
			val allCategories = db.categoryDao().getAllCategoriesOnce()
			val uncategorized =
				   db.transactionDao().getTransactionsByCategory("cat_uncategorized")
			val reCategorized = uncategorized.map { tx ->
				   tx.copy(
						category = com.example.cashia.Utility.CategoryMatcher.autoAssign(
							   tx, allCategories
						)
				   )
			}
			reCategorized.forEach { tx ->
				   db.transactionDao().updateCategory(tx.id, tx.category)
			}
	   }
	   
	   fun deleteCategory(category: Category) = viewModelScope.launch {
			if (category.isDefault) return@launch  // Guard: never delete defaults
			db.categoryDao().deleteCategory(category)
			// Reassign all its transactions back to uncategorized
			val ids = db.transactionDao().getTransactionIdsByCategory(category.id)
			if (ids.isNotEmpty()) {
				   db.transactionDao().updateCategoriesBulk(ids, "cat_uncategorized")
			}
	   }
	   
	   fun assignCategory(transactionId: String, categoryId: String) = viewModelScope.launch {
			db.transactionDao().updateCategory(transactionId, categoryId)
	   }
	   
	   companion object {
			fun provideFactory(context: Context): ViewModelProvider.Factory =
				   object : ViewModelProvider.Factory {
						@Suppress("UNCHECKED_CAST")
						override fun <T : ViewModel> create(modelClass: Class<T>): T {
							   if (modelClass.isAssignableFrom(CategoryViewModel::class.java)) {
									return CategoryViewModel(
										   AppDatabase.getDatabase(
												context
										   )
									) as T
							   }
							   throw IllegalArgumentException("Unknown ViewModel class")
						}
				   }
	   }
}