package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.CustomCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {

    @Query("SELECT * FROM CustomCategories ORDER BY createdAt ASC")
    fun getAllCustomCategories(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM CustomCategories ORDER BY createdAt ASC")
    suspend fun getAllCustomCategoriesOnce(): List<CustomCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CustomCategory)

    @Delete
    suspend fun deleteCategory(category: CustomCategory)

    @Query("DELETE FROM CustomCategories WHERE id = :id")
    suspend fun deleteCategoryById(id: String)

    @Query("UPDATE CustomCategories SET parentId = NULL WHERE parentId = :parentId")
    suspend fun detachChildren(parentId: String)
}