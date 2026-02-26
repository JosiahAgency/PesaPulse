package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cashia.models.Category

@Dao
interface CategoryDao {
	   
	   @Query("SELECT * FROM Categories ORDER BY isDefault DESC, name ASC")
	   fun getAllCategories(): List<Category>
	   
	   // One-shot (non-Flow) version for use in coroutines/sync logic
	   @Query("SELECT * FROM Categories ORDER BY isDefault DESC, name ASC")
	   suspend fun getAllCategoriesOnce(): List<Category>
	   
	   @Insert(onConflict = OnConflictStrategy.IGNORE)
	   suspend fun insertCategory(category: Category)
	   
	   @Update
	   suspend fun updateCategory(category: Category)
	   
	   @Delete
	   suspend fun deleteCategory(category: Category)
	   
	   @Query("SELECT * FROM Categories WHERE id = :id")
	   suspend fun getCategoryById(id: String): Category?
}