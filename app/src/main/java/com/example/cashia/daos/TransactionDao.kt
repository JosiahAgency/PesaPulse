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
    /** Expenses whose SMS never stated a transaction cost, so [expenses] understates reality. */
    val unknownFeeCount: Int = 0,
)

/** SQL-side weekday spend aggregate. `dow` is SQLite's `strftime('%w')`: 0=Sunday..6=Saturday. */
data class WeekdaySpendRow(
    val dow: Int,
    val totalAmount: Double,
    val activeDays: Int,
    val txCount: Int,
)

/** SQL-side recurring-merchant aggregate (recipients seen in >= minMonths distinct calendar months). */
data class RecurringMerchantRow(
    val recipient: String,
    val totalAmount: Double,
    val occurrences: Int,
    val lastDate: String,
    val lastTimestamp: Long,
)

/**
 * Two-column projection backing the dashboard calendar's activity dots. The strip only needs
 * to know *whether* a day had income/expense, so full entity rows are never hydrated.
 */
data class DayActivityRow(
    val date: String,
    val type: String,
)

/**
 * Per-day income/expense totals for the month grid. Carries both the dot indicators
 * (`income > 0` / `expense > 0`) and the long-press amount preview, so the month calendar
 * needs no entity rows at all. Expense totals include known fees, matching every other
 * spend figure in the app.
 */
data class DayTotalsRow(
    val date: String,
    val income: Double,
    val expense: Double,
)

/**
 * Latest-row headline projection — balance, its date and the stamp used for "updated"
 * labels. Shared by every caller that only needs the newest row's headline fields
 * (Dashboard's reactive balance, the widget's one-shot refresh, and anything that would
 * otherwise hydrate the whole latest [MpesaTransactions] for 2-3 scalars).
 */
data class LatestHeadlineRow(
    val balance: Double?,
    val date: String,
    val time: String?,
)

/**
 * Income/expense totals for a date range, summed in SQL.
 *
 * Mirrors exactly the in-memory loops it replaces: `neutral` rows (including the Fuliza
 * limit-update pseudo-row) contribute to neither total, and reversed transactions are
 * *not* excluded — the callers never excluded them either.
 */
data class PeriodMetricsRow(
    val income: Double = 0.0,
    val expenses: Double = 0.0,
)

/**
 * Budget aggregate over non-reversed rows in a date range: spend including known fees,
 * income, and how many of those expenses never reported a fee.
 */
data class RangeSpendRow(
    val spent: Double = 0.0,
    val earned: Double = 0.0,
    val unknownFeeCount: Int = 0,
)

@Dao
interface TransactionDao {

    companion object {
        /**
         * SQLite's default `SQLITE_MAX_VARIABLE_NUMBER` is 999. Bulk `IN (:ids)` queries must
         * chunk below it, with headroom for the statement's other bound parameters.
         */
        const val MAX_SQL_VARIABLES = 900

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
                 OR reference LIKE '%' || :query || '%'
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
               COALESCE(SUM(CASE WHEN type = 'expense' THEN amount + COALESCE(transactionCost, 0) ELSE 0 END), 0) AS expenses,
               COALESCE(SUM(transactionCost), 0) AS transactionCost,
               COALESCE(SUM(CASE WHEN type = 'expense' AND reversedTxId IS NULL AND transactionCost IS NULL THEN 1 ELSE 0 END), 0) AS unknownFeeCount
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

    /**
     * Distinct (day, type) pairs for the calendar strip's activity dots — at most two rows
     * per day regardless of how many transactions that day holds.
     */
    @Query(
        """
        SELECT DISTINCT date, type FROM MpesaTransactions
        WHERE date >= :startDate AND date <= :endDate
          AND type IN ('income', 'expense')
          AND NOT (category = 'FULIZA' AND type = 'neutral' AND recipient = 'Fuliza Limit Update')
        """
    )
    fun getDayActivityInRange(startDate: String, endDate: String): Flow<List<DayActivityRow>>

    /**
     * Per-day income/expense totals, grouped in SQL. One row per active day instead of one
     * per transaction, which is all the month grid needs for both its dots and its amount
     * preview.
     */
    @Query(
        """
        SELECT date,
               COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS income,
               COALESCE(SUM(CASE WHEN type = 'expense' THEN amount + COALESCE(transactionCost, 0) ELSE 0 END), 0) AS expense
        FROM MpesaTransactions
        WHERE date >= :startDate AND date <= :endDate
          AND NOT (category = 'FULIZA' AND type = 'neutral' AND recipient = 'Fuliza Limit Update')
        GROUP BY date
        HAVING income > 0 OR expense > 0
        """
    )
    fun getDayTotalsInRange(startDate: String, endDate: String): Flow<List<DayTotalsRow>>

    /**
     * Total spend (amount + known fees) for a date range, summed in SQL. Reversed
     * transactions are excluded. The Fuliza limit-update pseudo-row is `type = 'neutral'`,
     * so the `type = 'expense'` filter already excludes it.
     */
    @Query(
        """
        SELECT COALESCE(SUM(amount + COALESCE(transactionCost, 0)), 0) FROM MpesaTransactions
        WHERE date >= :startDate AND date <= :endDate
          AND type = 'expense' AND reversedTxId IS NULL
        """
    )
    fun getExpenseTotalInRange(startDate: String, endDate: String): Flow<Double>

    /** Reactive headline row — the dashboard balance never needs the rest of the row. */
    @Query("SELECT balance, date, time FROM MpesaTransactions ORDER BY timestamp DESC LIMIT 1")
    fun latestBalanceFlow(): Flow<LatestHeadlineRow?>

    /** Widget headline — balance/date/time only, instead of the full latest entity. */
    @Query("SELECT balance, date, time FROM MpesaTransactions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHeadlineOnce(): LatestHeadlineRow?

    /**
     * Income/expense totals for a date range, summed in SQL. Used wherever a caller needs
     * only the two scalars and would otherwise hydrate every row in the period.
     */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS income,
               COALESCE(SUM(CASE WHEN type = 'expense' THEN amount + COALESCE(transactionCost, 0) ELSE 0 END), 0) AS expenses
        FROM MpesaTransactions
        WHERE date >= :startDate AND date <= :endDate
        """
    )
    suspend fun getPeriodMetricsOnce(startDate: String, endDate: String): PeriodMetricsRow

    /** Flow variant of [getPeriodMetricsOnce] for comparison periods that only need totals. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS income,
               COALESCE(SUM(CASE WHEN type = 'expense' THEN amount + COALESCE(transactionCost, 0) ELSE 0 END), 0) AS expenses
        FROM MpesaTransactions
        WHERE date >= :startDate AND date <= :endDate
        """
    )
    fun getPeriodMetricsFlow(startDate: String, endDate: String): Flow<PeriodMetricsRow>

    /**
     * Budget aggregate for a date range over non-reversed rows only. `reversedTxId IS NULL`
     * sits in the WHERE clause because every column selected here applied that same filter
     * when this was computed in Kotlin.
     */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'expense' THEN amount + COALESCE(transactionCost, 0) ELSE 0 END), 0) AS spent,
               COALESCE(SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END), 0) AS earned,
               COALESCE(SUM(CASE WHEN type = 'expense' AND transactionCost IS NULL THEN 1 ELSE 0 END), 0) AS unknownFeeCount
        FROM MpesaTransactions
        WHERE date >= :startDate AND date <= :endDate AND reversedTxId IS NULL
        """
    )
    fun getRangeSpendFlow(startDate: String, endDate: String): Flow<RangeSpendRow>

    /** Earliest transaction date in the whole table — used to size the Projection lookback window without loading all rows. */
    @Query("SELECT MIN(date) FROM MpesaTransactions")
    fun getEarliestTransactionDateFlow(): Flow<String?>

    @Query("SELECT * FROM MpesaTransactions WHERE id IN (:ids)")
    suspend fun getTransactionsByIds(ids: List<String>): List<MpesaTransactions>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactions(transactions: List<MpesaTransactions>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCorrection(transaction: MpesaTransactions)

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

    /**
     * Sets one category across many transactions in a single statement.
     *
     * Callers must chunk [ids] to stay under SQLite's host-parameter limit — see
     * [MAX_SQL_VARIABLES].
     */
    @Query("UPDATE MpesaTransactions SET category = :category WHERE id IN (:ids)")
    suspend fun updateCategoryForIds(ids: List<String>, category: String)

    /** Re-assign all transactions that had a now-deleted custom category back to OTHER. */
    @Query("UPDATE MpesaTransactions SET category = 'OTHER' WHERE category = :deletedCategoryId")
    suspend fun resetDeletedCategory(deletedCategoryId: String)

    @Query("SELECT * FROM MpesaTransactions")
    suspend fun getAllTransactionsOnce(): List<MpesaTransactions>

    /** Row count only — avoids hydrating every column of every row just to call `.size`. */
    @Query("SELECT COUNT(*) FROM MpesaTransactions")
    suspend fun getTransactionCountOnce(): Int

    /** Row count for a date range only — the counting counterpart of [getTransactionsForDateRange]. */
    @Query("SELECT COUNT(*) FROM MpesaTransactions WHERE date >= :start AND date <= :end")
    suspend fun getTransactionCountForDateRange(start: String, end: String): Int

    /** Fetch a single transaction by primary key — used by the corrections feedback loop. */
    @Query("SELECT * FROM MpesaTransactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): MpesaTransactions?
    /** Used by summary workers — pure suspend, no Flow overhead */
    @Query("""SELECT * FROM MpesaTransactions WHERE date >= :start AND date <= :end ORDER BY date DESC""")
    suspend fun getTransactionsForDateRange(start: String, end: String): List<MpesaTransactions>
    @Query("SELECT * FROM MpesaTransactions WHERE reversedTxId = :txId LIMIT 1")
    suspend fun getReversalFor(txId: String): MpesaTransactions?

    /** All transactions that reverse another transaction (bounded — used to rebuild ReversalIndex without the full table). */
    @Query("SELECT * FROM MpesaTransactions WHERE reversedTxId IS NOT NULL")
    fun getReversalsFlow(): Flow<List<MpesaTransactions>>

    /** Per-weekday spend/income aggregate, pushed into SQL so raw rows never materialize client-side. */
    @Query(
        """
        SELECT CAST(strftime('%w', date) AS INTEGER) AS dow,
               SUM(amount) AS totalAmount,
               COUNT(DISTINCT date) AS activeDays,
               COUNT(*) AS txCount
        FROM MpesaTransactions
        WHERE type = :type AND reversedTxId IS NULL
        GROUP BY dow
        """
    )
    fun getWeekdaySpendPattern(type: String): Flow<List<WeekdaySpendRow>>

    /**
     * Recipients seen in >= minMonths distinct calendar months, pushed into SQL.
     * `recipient` and `lastDate` are bare (non-aggregate) columns alongside a single
     * MAX(timestamp) — SQLite's documented "bare column in an aggregate query" behavior
     * guarantees both are read from the same row as that MAX(), i.e. the most recent
     * transaction in the group, matching the original in-memory `txs.first()` (the source
     * list was timestamp-DESC ordered).
     */
    @Query(
        """
        SELECT recipient,
               date AS lastDate,
               SUM(amount) AS totalAmount,
               COUNT(*) AS occurrences,
               MAX(timestamp) AS lastTimestamp
        FROM MpesaTransactions
        WHERE type = :type AND reversedTxId IS NULL
        GROUP BY LOWER(TRIM(recipient))
        HAVING COUNT(DISTINCT substr(date, 1, 7)) >= :minMonths
        ORDER BY occurrences DESC
        LIMIT :limit
        """
    )
    fun getRecurringMerchants(type: String, minMonths: Int = 2, limit: Int = 8): Flow<List<RecurringMerchantRow>>

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