package com.example.cashia.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cashia.models.MpesaTransactions
import kotlinx.coroutines.flow.Flow

/** SQL-side aggregates over the currently filtered history set. */
data class FilteredTotals(
    val count: Int = 0,
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val transactionCost: Double = 0.0,
)

@Dao
interface TransactionDao {

    companion object {
        /**
         * Shared WHERE clause for the History screen. Mirrors the old in-memory filter:
         * excludes Fuliza limit-update pseudo-transactions, then applies free-text
         * (recipient / tx id / exact amount / matching category ids), category and
         * date-range filters. `date` is ISO yyyy-MM-dd so string comparison is safe.
         */
        const val HISTORY_FILTER = """
            NOT (category = 'FULIZA' AND type = 'neutral' AND recipient = 'Fuliza Limit Update')
            AND (:query = ''
                 OR recipient LIKE '%' || :query || '%'
                 OR id LIKE '%' || :query || '%'
                 OR (:amountQuery IS NOT NULL AND amount = :amountQuery)
                 OR category IN (:queryCategoryIds))
            AND (:category IS NULL OR category = :category)
            AND (:startDate IS NULL OR date >= :startDate)
            AND (:endDate IS NULL OR date <= :endDate)
        """
    }

    /**
     * Windowed, filtered history feed: only the newest [limit] matching rows are
     * materialized; the UI grows the window as the user scrolls.
     */
    @Query(
        """
        SELECT * FROM MpesaTransactions
        WHERE $HISTORY_FILTER
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun getFilteredWindow(
        query: String,
        amountQuery: Double?,
        queryCategoryIds: List<String>,
        category: String?,
        startDate: String?,
        endDate: String?,
        limit: Int,
    ): Flow<List<MpesaTransactions>>

    /** Aggregates over the FULL filtered set (not just the loaded window). */
    @Query(
        """
        SELECT COUNT(*) AS count,
               COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS income,
               COALESCE(SUM(CASE WHEN type = 'expense' THEN amount + transactionCost ELSE 0 END), 0) AS expenses,
               COALESCE(SUM(transactionCost), 0) AS transactionCost
        FROM MpesaTransactions
        WHERE $HISTORY_FILTER
        """
    )
    fun getFilteredTotals(
        query: String,
        amountQuery: Double?,
        queryCategoryIds: List<String>,
        category: String?,
        startDate: String?,
        endDate: String?,
    ): Flow<FilteredTotals>

    @Query("SELECT * FROM MpesaTransactions ORDER BY timestamp DESC LIMIT 1")
    fun latestTransactionFlow(): Flow<MpesaTransactions?>

    @Query(
        """
        SELECT amount FROM MpesaTransactions
        WHERE category = 'FULIZA' AND type = 'neutral' AND recipient = 'Fuliza Limit Update'
        ORDER BY timestamp DESC LIMIT 1
        """
    )
    fun latestFulizaLimitFlow(): Flow<Double?>

    /** Dashboard: only the rows inside the visible period, Fuliza limit updates excluded. */
    @Query(
        """
        SELECT * FROM MpesaTransactions
        WHERE date >= :startDate AND date <= :endDate
          AND NOT (category = 'FULIZA' AND type = 'neutral' AND recipient = 'Fuliza Limit Update')
        ORDER BY timestamp DESC
        """
    )
    fun getTransactionsInRange(startDate: String, endDate: String): Flow<List<MpesaTransactions>>

    /** Analytics: type + timestamp-range filter pushed into SQL. */
    @Query(
        """
        SELECT * FROM MpesaTransactions
        WHERE type = :type AND timestamp BETWEEN :startTs AND :endTs
        ORDER BY timestamp DESC
        """
    )
    fun getByTypeInRange(type: String, startTs: Long, endTs: Long): Flow<List<MpesaTransactions>>

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
    /** Used by summary workers — pure suspend, no Flow overhead */
    @Query("""SELECT * FROM MpesaTransactions WHERE date >= :start AND date <= :end ORDER BY date DESC""")
    suspend fun getTransactionsForDateRange(start: String, end: String): List<MpesaTransactions>
    @Query("SELECT balance FROM MpesaTransactions ORDER BY date DESC LIMIT 1")
    suspend fun getLatestBalance(): Double?
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