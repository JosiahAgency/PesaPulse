package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.UnmatchedSms
import kotlinx.coroutines.flow.Flow

@Dao
interface UnmatchedSmsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sms: UnmatchedSms)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<UnmatchedSms>)

    @Query("SELECT COUNT(*) FROM UnmatchedSms WHERE reviewed = 0")
    fun pendingCountFlow(): Flow<Int>

    @Query("SELECT * FROM UnmatchedSms ORDER BY receivedAt DESC")
    fun allUnmatchedFlow(): Flow<List<UnmatchedSms>>

    @Query("SELECT * FROM UnmatchedSms WHERE reviewed = 0 ORDER BY receivedAt DESC LIMIT 1")
    fun nextPendingFlow(): Flow<UnmatchedSms?>

    @Query("UPDATE UnmatchedSms SET reviewed = 1 WHERE id = :id")
    suspend fun markReviewed(id: Long)

    @Delete
    suspend fun delete(sms: UnmatchedSms)

    @Query("SELECT MAX(receivedAt) FROM UnmatchedSms")
    suspend fun getLastReceivedAt(): Long?
}