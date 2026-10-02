package com.example.cashia.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.daos.DayTotalsRow
import com.example.cashia.daos.PeriodMetricsRow
import com.example.cashia.daos.TransactionDao
import com.example.cashia.utility.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val filterByDay: Boolean = false,
    val selectedFilter: TransactionType = TransactionType.ALL,
    val filteredTransactions: List<MpesaTransactions> = emptyList(),
    val upcomingForDay: List<UpcomingTransaction> = emptyList()
)

/**
 * Everything the month grid draws, resolved from a grouped SQL projection instead of entity
 * rows: which days saw money move (the dots) and how much (the long-press preview).
 */
data class MonthActivity(
    val totalsByDate: Map<String, DayTotalsRow> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    dao: TransactionDao,
    upcomingFlow: StateFlow<List<UpcomingTransaction>>
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _filterByDay = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow(TransactionType.ALL)

    /**
     * The month actually on screen. FullMonthCalendar's HorizontalPager reports it back
     * through [handleDayClick]'s month-change path as the user swipes, so every query below
     * stays centered on it rather than loading the whole table.
     */
    private val visibleMonth: StateFlow<YearMonth> = _selectedDate
        .map { YearMonth.from(it) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearMonth.now())

    /**
     * Full rows for the ledger list, kept at the visible month ± 1 so that swiping the month
     * pager renders instantly instead of flashing an empty list while a new query lands —
     * the neighbours are a deliberate prefetch, not an oversight. The dots and the summary
     * cards no longer read this: they use SQL projections below.
     */
    private val windowedTransactionsFlow: StateFlow<List<MpesaTransactions>> = visibleMonth
        .flatMapLatest { month ->
            dao.getTransactionsInRange(
                month.minusMonths(1).atDay(1).toString(),
                month.plusMonths(1).atEndOfMonth().toString()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Month-grid dots and amounts. Covers the same ± 1 month span as the pager so adjacent
     * pages are already populated as they scroll in.
     */
    val monthActivity: StateFlow<MonthActivity> = visibleMonth
        .flatMapLatest { month ->
            dao.getDayTotalsInRange(
                month.minusMonths(1).atDay(1).toString(),
                month.plusMonths(1).atEndOfMonth().toString()
            )
        }
        .map { rows -> MonthActivity(rows.associateBy { it.date }) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthActivity())

    /**
     * Summary-card totals for the visible month, summed in SQL by the same query the
     * dashboard and analytics use — previously these were re-derived in the UI with a
     * formula that omitted transaction fees, so Calendar could disagree with Dashboard
     * about the same month's spend.
     */
    val monthMetrics: StateFlow<PeriodMetricsRow> = visibleMonth
        .flatMapLatest { month ->
            dao.getPeriodMetricsFlow(month.atDay(1).toString(), month.atEndOfMonth().toString())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeriodMetricsRow())

    val uiState: StateFlow<CalendarUiState> = combine(
        windowedTransactionsFlow,
        upcomingFlow,
        _selectedDate,
        _filterByDay,
        _selectedFilter
    ) { txs, upcomingTxs, date, byDay, filter ->

        // `date` is ISO yyyy-MM-dd, so day and month membership are both prefix/equality
        // checks on the stored string — no per-row LocalDate parsing or ParsedTx wrapper.
        val dateStr = date.toString()
        val monthPrefix = dateStr.substring(0, 7)

        val filteredTxs = txs.filter { tx ->
            val dateMatch = if (byDay) tx.date == dateStr else tx.date.startsWith(monthPrefix)
            dateMatch && when (filter) {
                TransactionType.ALL -> true
                TransactionType.INCOME -> tx.type.equals("income", ignoreCase = true)
                TransactionType.EXPENSE -> tx.type.equals("expense", ignoreCase = true)
            }
        }

        CalendarUiState(
            selectedDate = date,
            filterByDay = byDay,
            selectedFilter = filter,
            filteredTransactions = filteredTxs,
            upcomingForDay = upcomingTxs.filter { it.dueDate == dateStr }
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    // ── Actions ──────────────────────────────────────────────────────────────

    fun handleDayClick(clickedDate: LocalDate) {
        val current = _selectedDate.value
        _filterByDay.value =
            !(clickedDate.month != current.month || clickedDate.year != current.year)
        _selectedDate.value = clickedDate
    }

    fun setFilter(filter: TransactionType) {
        _selectedFilter.value = filter
    }

    fun clearDayFilter() {
        _filterByDay.value = false
    }

    fun resetToToday() {
        _selectedDate.value = LocalDate.now()
        _filterByDay.value = true
    }

    // ── Factory ──────────────────────────────────────────────────────────────
    companion object {
        fun provideFactory(
            dao: TransactionDao,
            upcomingFlow: StateFlow<List<UpcomingTransaction>>
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(dao, upcomingFlow) as T
            }
        }
    }
}
