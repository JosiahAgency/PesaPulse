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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
)

data class ReversalIndex(
    val byOriginal: Map<String, MpesaTransactions> = emptyMap(),
    val byReversal: Map<String, MpesaTransactions> = emptyMap(),
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
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    private val _dateRangeStart = MutableStateFlow<LocalDate?>(null)
    private val _dateRangeEnd = MutableStateFlow<LocalDate?>(null)
    private val _categoryLabelsMap = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _dashboardPeriodLabel = MutableStateFlow("")
    val dashboardPeriodLabel = _dashboardPeriodLabel.asStateFlow()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()


    val state: StateFlow<TransactionState> = combine(
        dao.getAllTransactions(),       // No debounce — DB changes propagate immediately
        _searchQuery.debounce(300),    // Only search input benefits from debouncing
        _selectedCategoryFilter,
        _dateRangeStart,
        _dateRangeEnd,
        _categoryLabelsMap
    ) { args ->
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

    var isBalanceVisible by mutableStateOf(false)
        private set

    // Callback is now only invoked after a real SMS sync, not on every filter
    // change. See silentSync() below.
    var onNewTransactionsSynced: ((List<MpesaTransactions>) -> Unit)? = null

    val pendingReviewCount: StateFlow<Int> = unmatchedDao
        .pendingCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val nextPending: StateFlow<UnmatchedSms?> = unmatchedDao
        .nextPendingFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val allUnmatched: StateFlow<List<UnmatchedSms>> = unmatchedDao
        .allUnmatchedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reversalIndex: StateFlow<ReversalIndex> = dao
        .getAllTransactions()
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

    var recurringSheetTransaction by mutableStateOf<MpesaTransactions?>(null)
        private set

    fun showRecurringSheet(tx: MpesaTransactions) {
        recurringSheetTransaction = tx
    }

    fun hideRecurringSheet() {
        recurringSheetTransaction = null
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            silentSync()
        }
    }

    private suspend fun silentSync() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        try {
            syncTransactions(context, database)
            val fresh = withContext(Dispatchers.IO) {
                dao.getAllTransactionsOnce()
            }
            if (fresh.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    onNewTransactionsSynced?.invoke(fresh)
                }
            }
        } catch (e: Exception) {
            Log.e("Transactions", "Background sync failure", e)
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Updates the reactive filters fed into the background pipeline.
     */
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

    fun setDashboardScope(date: LocalDate, scope: TimeScope, categoryMap: Map<String, String>) {
        viewModelScope.launch(Dispatchers.Default) {
            val firstDay = PesaTimeConfiguration.firstDayOfWeek
            val (start, end) = when (scope) {
                TimeScope.DAY -> date to date
                TimeScope.WEEK -> {
                    val s = date.with(TemporalAdjusters.previousOrSame(firstDay))
                    s to s.plusDays(6)
                }

                TimeScope.MONTH -> date.with(TemporalAdjusters.firstDayOfMonth()) to date.with(
                    TemporalAdjusters.lastDayOfMonth()
                )

                TimeScope.YEAR -> date.with(TemporalAdjusters.firstDayOfYear()) to date.with(
                    TemporalAdjusters.lastDayOfYear()
                )

                TimeScope.CUSTOM -> date to date
            }

            val label = when (scope) {
                TimeScope.DAY -> date.format(dayFormatter)
                TimeScope.WEEK -> "${start.format(weekFormatter)} – ${end.format(weekFormatter)}"
                TimeScope.MONTH -> date.format(monthFormatter)
                TimeScope.YEAR -> date.format(yearFormatter)
                else -> ""
            }

            _dashboardPeriodLabel.value = label

            updateFilters(
                query = state.value.searchQuery,
                categoryFilter = state.value.selectedCategory,
                start = start,
                end = end,
                categoryMap = categoryMap
            )
        }
    }

    private fun computeFilteredState(
        allTxs: List<MpesaTransactions>,
        query: String,
        catFilter: String?,
        start: LocalDate?,
        end: LocalDate?,
        catMap: Map<String, String>
    ): TransactionState {
        val today = LocalDate.now()
        val amountQuery = query.trim().toDoubleOrNull()
        val isQueryBlank = query.isBlank()

        val filteredList = allTxs.filter { tx ->
            val textMatch = isQueryBlank ||
                    tx.recipient.contains(query, ignoreCase = true) ||
                    tx.id.contains(query, ignoreCase = true) ||
                    (amountQuery != null && tx.amount == amountQuery) ||
                    catMap[tx.category]?.contains(query, ignoreCase = true) == true

            val catMatch = catFilter == null || tx.category == catFilter

            val txDate = runCatching { LocalDate.parse(tx.date) }.getOrElse { today }
            val dateMatch = (start == null || !txDate.isBefore(start)) &&
                    (end == null || !txDate.isAfter(end))

            textMatch && catMatch && dateMatch
        }

        var totalIncome = 0.0
        var totalExpenses = 0.0
        var totalCost = 0.0
        val now = LocalDate.now()

        for (tx in filteredList) {
            totalCost += tx.transactionCost
            when (tx.type) {
                "income" -> totalIncome += tx.amount
                "expense" -> totalExpenses += tx.amount + tx.transactionCost
            }
        }

        val groupedTransactions = filteredList
            .map { tx ->
                val date = runCatching { LocalDate.parse(tx.date) }.getOrElse { today }
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
                        val txs = dayRows.map { it.tx }
                        var inc = 0.0
                        var exp = 0.0
                        for (tx in txs) {
                            when (tx.type) {
                                "income" -> inc += tx.amount
                                "expense" -> exp += tx.amount
                            }
                        }
                        DayGroup(
                            date = date,
                            transactions = txs,
                            dayIncome = inc,
                            dayExpense = exp,
                            dayBalance = txs.lastOrNull { it.balance > 0 }?.balance ?: 0.0
                        )
                    }
                WeekGroup(bucket, days)
            }

        val latestTx = allTxs.firstOrNull()
        val latestBalance = latestTx?.balance ?: 0.0
        val latestBalanceDate = latestTx?.date ?: ""
        val latestBalanceDateParsed =
            runCatching { LocalDate.parse(latestBalanceDate) }.getOrElse { now }

        return TransactionState(
            allTransactions = allTxs,
            filteredTransactions = filteredList,
            groupedTransactions = groupedTransactions,
            latestBalance = latestBalance,
            latestBalanceDate = latestBalanceDate,
            latestBalanceDateParsed = latestBalanceDateParsed,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            totalTransactionCost = totalCost,
            currentMonth = "${now.month.getDisplayName(TextStyle.FULL, Locale.US)} ${now.year}",
            selectedCategory = catFilter,
            searchQuery = query
        )
    }

    fun toggleBalanceVisibility() {
        isBalanceVisible = !isBalanceVisible
    }

    fun onPermissionGranted() {
        viewModelScope.launch { refreshFromSms() }
    }

    fun refreshFromSms() {
        viewModelScope.launch(Dispatchers.IO) {
            silentSync()
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