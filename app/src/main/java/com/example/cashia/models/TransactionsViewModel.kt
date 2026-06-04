package com.example.cashia.models

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.AppDatabase
import com.example.cashia.utility.ParsedTx
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TimeScope
import com.example.cashia.utility.partialFieldsFromJson
import com.example.cashia.utility.syncTransactions
import com.example.cashia.utility.weekBucket
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class TransactionState(
    val allTransactions: List<MpesaTransactions> = emptyList(),
    val filteredTransactions: List<MpesaTransactions> = emptyList(),
    val groupedTransactions: List<WeekGroup> = emptyList(),
    val latestBalance: Double = 0.0,
    val latestBalanceDate: String = "",
    val latestBalanceDateParsed: LocalDate = LocalDate.now(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalTransactionCost: Double = 0.0,
    val currentMonth: String = "",
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val fulizaLimit: Double = 0.0,
)

data class ReversalIndex(
    val byOriginal: Map<String, MpesaTransactions> = emptyMap(),
    val byReversal: Map<String, MpesaTransactions> = emptyMap(),
)

private data class Totals(
    val income: Double,
    val expenses: Double,
    val transactionCost: Double,
)

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
private val weekFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

@OptIn(FlowPreview::class)
class TransactionsViewModel(private val context: Context) : ViewModel() {

    private val database = AppDatabase.getDatabase(context)
    private val dao = database.transactionDao()
    private val unmatchedDao = database.unmatchedSmsDao()

    // ── Filters ────────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _dateRangeStart = MutableStateFlow<LocalDate?>(null)
    private val _dateRangeEnd = MutableStateFlow<LocalDate?>(null)
    private val _categoryLabelsMap = MutableStateFlow<Map<String, String>>(emptyMap())

    // ── Dashboard label ────────────────────────────────────────────────────────
    private val _dashboardPeriodLabel = MutableStateFlow("")
    val dashboardPeriodLabel: StateFlow<String> = _dashboardPeriodLabel.asStateFlow()

    // ── Sync state ─────────────────────────────────────────────────────────────
    private val syncMutex = Mutex()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // ── New-transactions event (replaces raw nullable callback) ────────────────
    // Collectors receive the freshly-synced list once per sync cycle.
    private val _newTransactionsSynced =
        MutableSharedFlow<List<MpesaTransactions>>(extraBufferCapacity = 1)
    val newTransactionsSynced: SharedFlow<List<MpesaTransactions>> =
        _newTransactionsSynced.asSharedFlow()

    // ── Single shared DB upstream ──────────────────────────────────────────────
    // Shared so that `state` and `reversalIndex` both consume one DB subscription
    // instead of each opening their own observer.
    private val allTransactionsShared = dao
        .getAllTransactions()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1
        )

    // ── Main state ─────────────────────────────────────────────────────────────
    val state: StateFlow<TransactionState> = combine(
        allTransactionsShared,             // DB changes propagate immediately
        _searchQuery.debounce(300),        // Only search input benefits from debouncing
        _selectedCategoryFilter,
        _dateRangeStart,
        _dateRangeEnd,
        _categoryLabelsMap
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val transactions = args[0] as List<MpesaTransactions>
        val query = args[1] as String
        val catFilter = args[2] as String?
        val start = args[3] as LocalDate?
        val end = args[4] as LocalDate?
        val catMap = args[5] as Map<String, String>
        computeFilteredState(transactions, query, catFilter, start, end, catMap)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TransactionState()
        )

    // ── Reversal index — reuses the shared upstream ────────────────────────────
    val reversalIndex: StateFlow<ReversalIndex> = allTransactionsShared
        .map { all ->
            val reversals = all.filter { it.reversedTxId != null }
            val allById = all.associateBy { it.id }

            val byOriginal = reversals.mapNotNull { rev ->
                val orig = allById[rev.reversedTxId] ?: return@mapNotNull null
                orig.id to rev
            }.toMap()

            val byReversal = reversals.mapNotNull { rev ->
                val orig = allById[rev.reversedTxId] ?: return@mapNotNull null
                rev.id to orig
            }.toMap()

            ReversalIndex(byOriginal, byReversal)
        }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReversalIndex())

    // ── Unmatched SMS ──────────────────────────────────────────────────────────
    val pendingReviewCount: StateFlow<Int> = unmatchedDao
        .pendingCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val nextPending: StateFlow<UnmatchedSms?> = unmatchedDao
        .nextPendingFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allUnmatched: StateFlow<List<UnmatchedSms>> = unmatchedDao
        .allUnmatchedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Balance visibility ─────────────────────────────────────────────────────
    // Compose-observable; intentionally not in TransactionState since it's purely
    // a UI toggle that doesn't affect computation.
    var isBalanceVisible by mutableStateOf(false)
        private set

    // ── Recurring sheet ────────────────────────────────────────────────────────
    var recurringSheetTransaction by mutableStateOf<MpesaTransactions?>(null)
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
            syncTransactions(context, database)
            val fresh = withContext(Dispatchers.IO) { dao.getAllTransactionsOnce() }
            if (fresh.isNotEmpty()) {
                _newTransactionsSynced.tryEmit(fresh)
            }
        } catch (e: Exception) {
            Log.e("TransactionsViewModel", "Background sync failure", e)
        } finally {
            _isSyncing.value = false
            syncMutex.unlock()
        }
    }

    // ── Public actions ─────────────────────────────────────────────────────────

    fun showRecurringSheet(tx: MpesaTransactions) {
        recurringSheetTransaction = tx
    }

    fun hideRecurringSheet() {
        recurringSheetTransaction = null
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

    fun updateFilters(
        query: String,
        categoryFilter: String?,
        start: LocalDate?,
        end: LocalDate?,
        categoryMap: Map<String, String>
    ) {
        _searchQuery.value = query
        _selectedCategoryFilter.value = categoryFilter
        _dateRangeStart.value = start
        _dateRangeEnd.value = end
        _categoryLabelsMap.value = categoryMap
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

    fun setDashboardScope(date: LocalDate, scope: TimeScope) {
        viewModelScope.launch(Dispatchers.Default) {
            val firstDay = PesaTimeConfiguration.firstDayOfWeek
            val (start, end) = when (scope) {
                TimeScope.DAY -> date to date
                TimeScope.WEEK -> {
                    val s = date.with(TemporalAdjusters.previousOrSame(firstDay))
                    s to s.plusDays(6)
                }

                TimeScope.MONTH -> date.with(TemporalAdjusters.firstDayOfMonth()) to
                        date.with(TemporalAdjusters.lastDayOfMonth())

                TimeScope.YEAR -> date.with(TemporalAdjusters.firstDayOfYear()) to
                        date.with(TemporalAdjusters.lastDayOfYear())

                TimeScope.CUSTOM -> date to date
            }

            _dashboardPeriodLabel.value = when (scope) {
                TimeScope.DAY -> date.format(dayFormatter)
                TimeScope.WEEK -> "${start.format(weekFormatter)} – ${end.format(weekFormatter)}"
                TimeScope.MONTH -> date.format(monthFormatter)
                TimeScope.YEAR -> date.format(yearFormatter)
                else -> ""
            }

            updateFilters(
                query = state.value.searchQuery,
                categoryFilter = state.value.selectedCategory,
                start = start,
                end = end,
                categoryMap = _categoryLabelsMap.value
            )
        }
    }

    suspend fun getTransactionById(id: String): MpesaTransactions? =
        withContext(Dispatchers.IO) { dao.getTransactionById(id) }

    suspend fun getReversalFor(txId: String): MpesaTransactions? =
        withContext(Dispatchers.IO) { dao.getReversalFor(txId) }

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

    fun saveTransactionFromSms(source: UnmatchedSms, type: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val partial = partialFieldsFromJson(source.extractedFields)
            val transaction = try {
                buildTransaction(
                    smsId = source.smsId,
                    id = source.smsId.toString(),
                    amountStr = partial?.amount?.toString() ?: "0.00",
                    recipient = "Manual Entry (from SMS)",
                    dateStr = partial?.date ?: LocalDate.now()
                        .format(DateTimeFormatter.ofPattern("d/M/yyyy")),
                    timeStr = "12:00 PM",
                    type = type,
                    balance = 0.0,
                    cost = 0.0,
                    forcedCategory = category
                )
            } catch (e: Exception) {
                Log.e("TransactionsViewModel", "Failed to build transaction from SMS", e)
                return@launch
            }
            dao.insertTransactions(listOf(transaction))
            unmatchedDao.setStatus(source.id, SmsStatus.SAVED)
        }
    }

    // ── Pure computation helpers ───────────────────────────────────────────────

    private fun computeFilteredState(
        allTxs: List<MpesaTransactions>,
        query: String,
        catFilter: String?,
        start: LocalDate?,
        end: LocalDate?,
        catMap: Map<String, String>
    ): TransactionState {
        // Single capture of "now" used throughout this computation.
        val today = LocalDate.now()
        val amountQuery = query.trim().toDoubleOrNull()
        val isQueryBlank = query.isBlank()

        val latestFulizaLimit = allTxs.firstOrNull {
            it.category == TransactionCategory.FULIZA.name &&
                    it.type == "neutral" &&
                    it.recipient == "Fuliza Limit Update"
        }?.amount ?: 0.0

        val filteredList = allTxs.filter { tx ->
            // Always exclude Fuliza limit-update pseudo-transactions from the list.
            val isLimitUpdate = tx.category == TransactionCategory.FULIZA.name &&
                    tx.type == "neutral" &&
                    tx.recipient == "Fuliza Limit Update"
            if (isLimitUpdate) return@filter false

            val textMatch = isQueryBlank ||
                    tx.recipient.contains(query, ignoreCase = true) ||
                    tx.id.contains(query, ignoreCase = true) ||
                    (amountQuery != null && tx.amount == amountQuery) ||
                    catMap[tx.category]?.contains(query, ignoreCase = true) == true

            val catMatch = catFilter == null || tx.category == catFilter

            val txDate = parseDateOrWarn(tx.date, tx.id, today)
            val dateMatch = (start == null || !txDate.isBefore(start)) &&
                    (end == null || !txDate.isAfter(end))

            textMatch && catMatch && dateMatch
        }

        val totals = computeTotals(filteredList)
        val groupedTxs = buildGroupedTransactions(filteredList, today)
        val latestTx = allTxs.firstOrNull()
        val latestBalanceDate = latestTx?.date ?: ""
        val latestBalanceDateParsed = latestBalanceDate
            .takeIf { it.isNotBlank() }
            ?.let { parseDateOrWarn(it, "latestTx", today) }
            ?: today

        return TransactionState(
            allTransactions = allTxs,
            filteredTransactions = filteredList,
            groupedTransactions = groupedTxs,
            latestBalance = latestTx?.balance ?: 0.0,
            latestBalanceDate = latestBalanceDate,
            latestBalanceDateParsed = latestBalanceDateParsed,
            totalIncome = totals.income,
            totalExpenses = totals.expenses,
            totalTransactionCost = totals.transactionCost,
            currentMonth = "${today.month.getDisplayName(TextStyle.FULL, Locale.US)} ${today.year}",
            selectedCategory = catFilter,
            searchQuery = query,
            fulizaLimit = latestFulizaLimit,
        )
    }

    /**
     * Accumulates income, expense, and transaction-cost totals for a filtered list.
     */
    private fun computeTotals(txs: List<MpesaTransactions>): Totals {
        var income = 0.0
        var expenses = 0.0
        var cost = 0.0
        for (tx in txs) {
            cost += tx.transactionCost
            when (tx.type) {
                "income" -> income += tx.amount
                "expense" -> expenses += tx.amount + tx.transactionCost
            }
        }
        return Totals(income, expenses, cost)
    }

    /**
     * Groups a filtered list into [WeekGroup]s, each containing [DayGroup]s,
     * sorted newest-first at both levels.
     */
    private fun buildGroupedTransactions(
        txs: List<MpesaTransactions>,
        today: LocalDate
    ): List<WeekGroup> =
        txs
            .map { tx ->
                val date = parseDateOrWarn(tx.date, tx.id, today)
                ParsedTx(tx, date, weekBucket(date))
            }
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
                            dayBalance = dayTxs.lastOrNull { it.balance > 0 }?.balance ?: 0.0
                        )
                    }
                WeekGroup(bucket, days)
            }

    /**
     * Parses a date string, falling back to [fallback] and emitting a warning log
     * so that malformed dates aren't silently swallowed.
     */
    private fun parseDateOrWarn(raw: String, txId: String, fallback: LocalDate): LocalDate =
        runCatching { LocalDate.parse(raw) }.getOrElse {
            Log.w(
                "TransactionsViewModel",
                "Malformed date '$raw' on tx '$txId', falling back to $fallback"
            )
            fallback
        }

    // ── Factory ────────────────────────────────────────────────────────────────

    companion object {
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