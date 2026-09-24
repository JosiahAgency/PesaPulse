package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.UserCorrection
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCorrectionDao {
    /** Observe all corrections as a live Flow (used by the StateFlow in ViewModel). */
    @Query("SELECT * FROM UserCorrections ORDER BY hitCount DESC")
    fun getAllCorrections(): Flow<List<UserCorrection>>

    /** One-shot read for batch reclassify operations (called from a coroutine). */
    @Query("SELECT * FROM UserCorrections")
    suspend fun getAllCorrectionsOnce(): List<UserCorrection>

    /** Look up a single correction by the normalized recipient key. */
    @Query("SELECT * FROM UserCorrections WHERE recipient = :recipient LIMIT 1")
    suspend fun getCorrectionByRecipient(recipient: String): UserCorrection?

    /** Insert or replace (handles both create and update in one call). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCorrection(correction: UserCorrection)

    /** Remove a specific correction so the engine falls back to keyword rules. */
    @Query("DELETE FROM UserCorrections WHERE recipient = :recipient")
    suspend fun deleteCorrectionByRecipient(recipient: String)

    @Query("DELETE FROM UserCorrections WHERE categoryId = :categoryId")
    suspend fun deleteCorrectionsForCategory(categoryId: String)

    /** Wipe everything – useful for a "reset all corrections" settings action. */
    @Query("DELETE FROM UserCorrections")
    suspend fun clearAll()
}