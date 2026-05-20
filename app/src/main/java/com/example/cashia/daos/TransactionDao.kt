package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.MpesaTransactions
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT id FROM MpesaTransactions")
    suspend fun getAllTransactionIdsOnce(): List<String>

    @Query("SELECT * FROM MpesaTransactions WHERE id IN (:ids)")
    suspend fun getTransactionsByIds(ids: List<String>): List<MpesaTransactions>

    @Query("SELECT * FROM MpesaTransactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<MpesaTransactions>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<MpesaTransactions>)

    @Query("SELECT MAX(timestamp) FROM MpesaTransactions")
    suspend fun getLastTransactionTimestamp(): Long?

    @Query("SELECT SUM(amount) FROM MpesaTransactions WHERE type = :type AND timestamp BETWEEN :start AND :end")
    suspend fun getSumByType(type: String, start: Long, end: Long): Double?

    // ── Category updates ──────────────────────────────────────────────────

    @Query("UPDATE MpesaTransactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: String, category: String)

    /** Fetch all transactions whose category is 'OTHER' for re-classification. */
    @Query("SELECT * FROM MpesaTransactions WHERE category = 'OTHER'")
    suspend fun getUncategorizedTransactions(): List<MpesaTransactions>

    /** Bulk-update categories (used during re-categorization). */
    @Query("UPDATE MpesaTransactions SET category = :category WHERE id = :id")
    suspend fun updateCategoryById(id: String, category: String)

    /** Re-assign all transactions that had a now-deleted custom category back to OTHER. */
    @Query("UPDATE MpesaTransactions SET category = 'OTHER' WHERE category = :deletedCategoryId")
    suspend fun resetDeletedCategory(deletedCategoryId: String)

    @Query("SELECT * FROM MpesaTransactions")
    suspend fun getAllTransactionsOnce(): List<MpesaTransactions>

    /** Fetch a single transaction by primary key — used by the corrections feedback loop. */
    @Query("SELECT * FROM MpesaTransactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): MpesaTransactions?

    // In TransactionDao

    @Query("SELECT * FROM MpesaTransactions WHERE reversedTxId = :txId LIMIT 1")
    suspend fun getReversalFor(txId: String): MpesaTransactions?

    /**
     * Re-classify every transaction from the same recipient when the user
     * manually overrides a category, skipping the transaction they just changed.
     */
    @Query(
        """
        UPDATE MpesaTransactions
           SET category = :newCategory
         WHERE LOWER(recipient) = :recipientLower
           AND id != :excludeTxId
        """
    )
    suspend fun updateCategoryForRecipient(
        recipientLower: String,
        newCategory: String,
        excludeTxId: String
    )
}