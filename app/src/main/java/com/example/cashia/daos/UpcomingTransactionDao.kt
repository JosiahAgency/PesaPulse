package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.cashia.models.UpcomingTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UpcomingTransactionDao {

    @Query("SELECT * FROM UpcomingTransactions ORDER BY dueDate ASC")
    fun getAll(): Flow<List<UpcomingTransaction>>

    @Query("SELECT * FROM UpcomingTransactions WHERE status = 'PENDING' ORDER BY dueDate ASC")
    fun getPending(): Flow<List<UpcomingTransaction>>

    @Query("SELECT * FROM UpcomingTransactions WHERE status = 'PENDING' ORDER BY dueDate ASC")
    suspend fun getPendingOnce(): List<UpcomingTransaction>

    @Query("SELECT * FROM UpcomingTransactions WHERE id = :id")
    suspend fun getById(id: String): UpcomingTransaction?

    @Query("SELECT EXISTS(SELECT 1 FROM UpcomingTransactions WHERE matchedTransactionId = :transactionId)")
    suspend fun isTransactionMatched(transactionId: String): Boolean

    @Query("UPDATE UpcomingTransactions SET status = :status, matchedTransactionId = :transactionId WHERE id = :id AND status = 'PENDING'")
    suspend fun completePending(id: String, status: String, transactionId: String?): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tx: UpcomingTransaction)

    @Update
    suspend fun update(tx: UpcomingTransaction)

    @Query("DELETE FROM UpcomingTransactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE UpcomingTransactions SET categoryId = 'OTHER' WHERE categoryId = :categoryId")
    suspend fun resetDeletedCategory(categoryId: String)

    @Query("UPDATE UpcomingTransactions SET status = :status, matchedTransactionId = :matchedId WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, matchedId: String? = null)
}