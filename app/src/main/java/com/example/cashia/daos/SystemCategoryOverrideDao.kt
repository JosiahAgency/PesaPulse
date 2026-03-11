package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.SystemCategoryOverride
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemCategoryOverrideDao {

    @Query("SELECT * FROM SystemCategoryOverrides")
    fun getAllOverrides(): Flow<List<SystemCategoryOverride>>

    @Query("SELECT * FROM SystemCategoryOverrides")
    suspend fun getAllOverridesOnce(): List<SystemCategoryOverride>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: SystemCategoryOverride)

    @Query("DELETE FROM SystemCategoryOverrides WHERE id = :id")
    suspend fun deleteOverride(id: String)
}