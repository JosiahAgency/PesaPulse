package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.MpesaTransactions
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
	   
	   @Query("SELECT * FROM MpesaTransactions ORDER BY timestamp DESC")
	   fun getAllTransactions(): Flow<List<MpesaTransactions>>
	   
	   @Insert(onConflict = OnConflictStrategy.IGNORE)
	   suspend fun insertTransactions(transactions: List<MpesaTransactions>)
	   
	   @Query("SELECT MAX(timestamp) FROM MpesaTransactions")
	   suspend fun getLastTransactionTimestamp(): Long?
	   
	   @Query("SELECT SUM(amount) FROM MpesaTransactions WHERE type = :type AND timestamp BETWEEN :start AND :end")
	   suspend fun getSumByType(type: String, start: Long, end: Long): Double?
	   
	   // --- Category support ---
	   
	   @Query("UPDATE MpesaTransactions SET category = :category WHERE id = :id")
	   suspend fun updateCategory(id: String, category: String)
	   
	   @Query("UPDATE MpesaTransactions SET category = :category WHERE id IN (:ids)")
	   suspend fun updateCategoriesBulk(ids: List<String>, category: String)
	   
	   @Query("SELECT id FROM MpesaTransactions WHERE category = :categoryId")
	   suspend fun getTransactionIdsByCategory(categoryId: String): List<String>
	   
	   @Query("SELECT * FROM MpesaTransactions WHERE category = :categoryId")
	   suspend fun getTransactionsByCategory(categoryId: String): List<MpesaTransactions>
}