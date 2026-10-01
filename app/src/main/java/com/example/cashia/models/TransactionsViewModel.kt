package com.example.cashia.models

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.cashia.AppDatabase
import com.example.cashia.daos.FilteredTotals
import com.example.cashia.daos.LatestHeadlineRow
import com.example.cashia.utility.ParsedTx
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TransactionCorrection
import com.example.cashia.utility.UpcomingPaymentService
import com.example.cashia.utility.syncTransactions
import com.example.cashia.utility.toParsedTx
import com.example.cashia.widget.WidgetUpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class TransactionState(
    /** The currently loaded window of the filtered set (newest first). */
    val filteredTransactions: List<MpesaTransactions> = emptyList(),
    val groupedTransactions: List<WeekGroup> = emptyList(),
    /** Total number of rows matching the filters (whole DB, not just the window). */
    val filteredCount: Int = 0,
    /** True when more matching rows exist beyond the loaded window. */
    val hasMore: Boolean = false,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalTransactionCost: Double = 0.0,
    /** Expenses in the filtered set with no fee recorded — [totalExpenses] understates by these. */
    val unknownFeeCount: Int = 0,
    val currentMonth: String = "",
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val fulizaLimit: Double = 0.0,
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
    /** False until the first Room emission arrives; distinguishes "loading" from "loaded but empty". */
    val hasLoaded: Boolean = false,
)

data class ReversalIndex(
    val byOriginal: Map<String, MpesaTransactions> = emptyMap(),
    val byReversal: Map<String, MpesaTransactions> = emptyMap(),
)

/** Resolved SQL parameters for the History screen's filters. */
private data class HistoryFilters(
    val rawQuery: String = "",
    val sqlQuery: String = "",
    val amountQuery: Double? = null,
    val queryCategoryIds: List<String> = emptyList(),
    val category: String? = null,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) : ViewModel() {

    private val dao = database.transactionDao()
    private val unmatchedDao = database.unmatchedSmsDao()

    // ── Filters ────────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _dateRangeStart = MutableStateFlow<LocalDate?>(null)
    private val _dateRangeEnd = MutableStateFlow<LocalDate?>(null)
    private val _categoryLabelsMap = MutableStateFlow<Map<String, String>>(emptyMap())

    // ── Sync state ─────────────────────────────────────────────────────────────
    private val syncMutex = Mutex()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // ── New-transactions event (replaces raw nullable callback) ────────────────
    private val _newTransactionsSynced =
        MutableSharedFlow<List<MpesaTransactions>>(extraBufferCapacity = 1)
//    val newTransactionsSynced: SharedFlow<List<MpesaTransactions>> =
//        _newTransactionsSynced.asSharedFlow()

    // ── Sync-failure event (surfaced to the user via a Snackbar) ────────────────
    private val _syncError = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val syncError: SharedFlow<String> = _syncError.asSharedFlow()

    /**
     * Sticky mirror of [syncError]. The Snackbar event is one-shot, so a screen that is
     * composed after a failure (or one the user returns to) would otherwise render a plain
     * "no transactions" empty state and look indistinguishable from a healthy empty period.
     * Cleared by the next successful sync or by [dismissSyncError].
     */
    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    fun dismissSyncError() {
        _lastSyncError.value = null
    }

    // ── History pipeline: filters resolved once, applied in SQL ────────────────

    private val filtersFlow = combine(
        _searchQuery.debounce(300.milliseconds),        // Only search input benefits from debouncing
        _selectedCategoryFilter,
        _dateRangeStart,
        _dateRangeEnd,
        _categoryLabelsMap
    ) { query, cat, start, end, catMap ->
        val trimmed = query.trim()
        HistoryFilters(
            rawQuery = query,
            sqlQuery = trimmed.ifEmpty { "" },
            amountQuery = trimmed.toDoubleOrNull(),
            queryCategoryIds = if (trimmed.isEmpty()) emptyList()
            else catMap.filterValues { it.contains(trimmed, ignoreCase = true) }.keys.toList(),
            category = cat,
            start = start,
            end = end,
        )
    }.distinctUntilChanged()

    /** How many rows of the filtered set are materialized for the list. */
    private val _windowLimit = MutableStateFlow(WINDOW_PAGE)

    private val windowFlow = combine(filtersFlow, _windowLimit) { f, limit -> f to limit }
        .flatMapLatest { (f, limit) ->
            dao.getFilteredWindow(
                query = f.sqlQuery,
                amountQuery = f.amountQuery,
                queryCategoryIds = f.queryCategoryIds,
                category = f.category,
                startDate = f.start?.toString(),
                endDate = f.end?.toString(),
                limit = limit,
            )
        }

    private val totalsFlow = filtersFlow.flatMapLatest { f ->
        dao.getFilteredTotals(
            query = f.sqlQuery,
            amountQuery = f.amountQuery,
            queryCategoryIds = f.queryCategoryIds,
            category = f.category,
            startDate = f.start?.toString(),
            endDate = f.end?.toString(),
        )
    }

    // ── Main state ─────────────────────────────────────────────────────────────
    val state: StateFlow<TransactionState> = combine(
        windowFlow,
        totalsFlow,
        dao.latestFulizaLimitFlow(),
        filtersFlow,
    ) { window, totals, fulizaLimit, f ->
        buildState(window, totals, fulizaLimit, f)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionState()
        )

    // ── Reversal index — bounded to reversal rows + the originals they reference,
    // instead of scanning the full transaction table ───────────────────────────
    val reversalIndex: StateFlow<ReversalIndex> = dao.getReversalsFlow()
        .map { reversals ->
            if (reversals.isEmpty()) return@map ReversalIndex()

            val originalIds = reversals.mapNotNull { it.reversedTxId }.distinct()
            val originalsById = dao.getTransactionsByIds(originalIds).associateBy { it.id }

            val byOriginal = reversals.mapNotNull { rev ->
                val orig = originalsById[rev.reversedTxId] ?: return@mapNotNull null
                orig.id to rev
            }.toMap()

            val byReversal = reversals.mapNotNull { rev ->
                val orig = originalsById[rev.reversedTxId] ?: return@mapNotNull null
                rev.id to orig
            }.toMap()

            ReversalIndex(byOriginal, byReversal)
        }
        .flowOn(Dispatchers.IO)
        // Keyed on id sets rather than full data-class equality: comparing every
        // MpesaTransactions field on every Room re-emission (which fires on any table
        // write, not just reversal changes) is a needless O(n * fields) scan. The id sets
        // fully determine which rows are reversal pairs, which is all this index exposes.
        .distinctUntilChanged { old, new ->
            old.byOriginal.keys == new.byOriginal.keys && old.byReversal.keys == new.byReversal.keys
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReversalIndex())

    // ── Unmatched SMS ──────────────────────────────────────────────────────────
    val pendingReviewCount: StateFlow<Int> = unmatchedDao
        .pendingCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val allUnmatched: StateFlow<List<UnmatchedSms>> = unmatchedDao
        .allUnmatchedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Slim headline-balance feed for the Dashboard. Collecting [state] there would keep the
     * whole History pipeline (windowed query + totals + grouping in [buildGroupedTransactions])
     * warm for three fields, so this projects balance/date straight off the DAO instead.
     * `null` means "first emission not in yet", i.e. still loading.
     */
    val latestBalance: StateFlow<LatestHeadlineRow?> = dao.latestBalanceFlow()
        .map { it ?: LatestHeadlineRow(balance = null, date = "", time = null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Recent History searches, newest first. Lives in the ViewModel (backed by prefs) rather
     * than a composable `remember` so the list survives navigating away and process death.
     */
    private val _recentSearches = MutableStateFlow(PesaTimeConfiguration.getRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    /** Call when the user commits a search (keyboard action or picking a suggestion). */
    fun commitSearch(term: String) {
        _recentSearches.value = PesaTimeConfiguration.addRecentSearch(term)
    }

    fun clearRecentSearches() {
        PesaTimeConfiguration.clearRecentSearches()
        _recentSearches.value = emptyList()
    }

    // ── Balance visibility ─────────────────────────────────────────────────────
    // Compose-observable; intentionally not in TransactionState since it's purely
    // a UI toggle that doesn't affect computation.
    var isBalanceVisible by mutableStateOf(false)
        private set


    // ── Init ───────────────────────────────────────────────────────────────────
    init {
        viewModelScope.launch(Dispatchers.IO) { silentSync() }
    }

    // ── Sync ───────────────────────────────────────────────────────────────────

    /**
     * Mutex-guarded sync: at most one sync runs at a time.
     * tryLock() returns false immediately if another coroutine holds the lock,
     * so concurrent callers back off instead of queuing.
     */
    private suspend fun silentSync() {
        if (!syncMutex.tryLock()) return
        _isSyncing.value = true
        try {
            val newTxs = syncTransactions(context, database)
            _lastSyncError.value = null
            if (newTxs.isNotEmpty()) {
                _newTransactionsSynced.tryEmit(newTxs)
            }
        } catch (e: Exception) {
            Log.e("TransactionsViewModel", "Background sync failure", e)
            val message = "Couldn't sync new SMS messages. Pull to refresh to try again."
            _lastSyncError.value = message
            _syncError.tryEmit(message)
        } finally {
            _isSyncing.value = false
            syncMutex.unlock()
        }
    }

    // ── Public actions ─────────────────────────────────────────────────────────

    /** Grows the History window by one page when the user nears the end of the list. */
    fun loadMore() {
        if (state.value.hasMore) _windowLimit.value += WINDOW_PAGE
    }

    fun toggleBalanceVisibility() {
        isBalanceVisible = !isBalanceVisible
    }

    fun onPermissionGranted() {
        viewModelScope.launch { refreshFromSms() }
    }

    fun refreshFromSms() {
        viewModelScope.launch(Dispatchers.IO) { silentSync() }
    }

    fun updateCategories(categories: List<ResolvedCategory>) {
        _categoryLabelsMap.value = categories.associate { it.id to it.label }
    }

    fun setCategory(cat: String?) {
        _selectedCategoryFilter.value = cat
    }

    fun setQuery(q: String) {
        _searchQuery.value = q
    }

    fun setDateRange(start: LocalDate?, end: LocalDate?) {
        _dateRangeStart.value = start
        _dateRangeEnd.value = end
    }


    fun markSkipped(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            unmatchedDao.setStatus(id, SmsStatus.SKIPPED)
        }
    }

    fun markIgnored(sms: UnmatchedSms) {
        viewModelScope.launch(Dispatchers.IO) {
            if (sms.contentHash.isNotBlank()) {
                unmatchedDao.ignoreAllByHash(sms.contentHash)
            } else {
                unmatchedDao.setStatus(sms.id, SmsStatus.IGNORED)
            }
        }
    }

    suspend fun saveTransactionFromSms(
        source: UnmatchedSms,
        correction: TransactionCorrection
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val transaction = correction.toTransaction(source)
                database.withTransaction {
                    require(unmatchedDao.claimForCorrection(source.id) == 1) {
                        "This message has already been saved, ignored, or removed."
                    }
                    dao.insertCorrection(transaction)
                    UpcomingPaymentService(database).suggest(listOf(transaction))
                }
            } catch (e: IllegalArgumentException) {
                return@withContext e.message ?: "Please check the entered details."
            } catch (e: SQLiteException) {
                Log.e("TransactionsViewModel", "Could not save corrected SMS", e)
                return@withContext "Could not save. This SMS or reference may already exist. Your message remains in review."
            }
            WidgetUpdateManager.refreshCashiaWidgets(context)
            null
        }

    // ── Pure computation helpers ───────────────────────────────────────────────

    private fun buildState(
        window: List<MpesaTransactions>,
        totals: FilteredTotals,
        fulizaLimit: Double?,
        f: HistoryFilters,
    ): TransactionState {
        val today = LocalDate.now()
        val parsedWindow = window.map { it.toParsedTx() }

        return TransactionState(
            filteredTransactions = window,
            groupedTransactions = buildGroupedTransactions(parsedWindow),
            filteredCount = totals.count,
            hasMore = window.size < totals.count,
            totalIncome = totals.income,
            totalExpenses = totals.expenses,
            totalTransactionCost = totals.transactionCost,
            unknownFeeCount = totals.unknownFeeCount,
            currentMonth = "${today.month.getDisplayName(TextStyle.FULL, Locale.US)} ${today.year}",
            selectedCategory = f.category,
            searchQuery = f.rawQuery,
            fulizaLimit = fulizaLimit ?: 0.0,
            dateRangeStart = f.start,
            dateRangeEnd = f.end,
            hasLoaded = true,
        )
    }

    /**
     * Groups a filtered list into [WeekGroup]s, each containing [DayGroup]s,
     * sorted newest-first at both levels.
     */
    private fun buildGroupedTransactions(
        parsedTxs: List<ParsedTx>
    ): List<WeekGroup> =
        parsedTxs
            .groupBy { it.bucket }
            .entries
            .sortedByDescending { entry -> entry.value.maxOf { it.date } }
            .map { (bucket, rows) ->
                val days = rows
                    .groupBy { it.date }
                    .entries
                    .sortedByDescending { it.key }
                    .map { (date, dayRows) ->
                        val dayTxs = dayRows.map { it.tx }
                        var inc = 0.0
                        var exp = 0.0
                        for (tx in dayTxs) {
                            when (tx.type) {
                                "income" -> inc += tx.amount
                                "expense" -> exp += tx.amount
                            }
                        }
                        DayGroup(
                            date = date,
                            transactions = dayTxs,
                            dayIncome = inc,
                            dayExpense = exp,
                            dayBalance = dayTxs.firstOrNull()?.balance
                        )
                    }
                WeekGroup(bucket, days)
            }

    // ── Factory ────────────────────────────────────────────────────────────────

    companion object {
        /** Rows loaded per page of the History window. */
        private const val WINDOW_PAGE = 200

        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
                        return TransactionsViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
