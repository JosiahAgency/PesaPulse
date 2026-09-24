package com.example.cashia.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.daos.TransactionDao
import com.example.cashia.utility.ParsedTx
import com.example.cashia.utility.TransactionType
import com.example.cashia.utility.buildDayGroups
import com.example.cashia.utility.toParsedTx
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
    val dayGroups: List<DayGroup> = emptyList(),
    val upcomingForDay: List<UpcomingTransaction> = emptyList()
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
     * Bounded to the selected month plus its immediate neighbors instead of the full table.
     * FullMonthCalendar's HorizontalPager reports the visible month back through
     * [handleDayClick]'s month-change path as the user swipes, so this window stays centered
     * on whatever month is actually on screen. Exposed for FinancialOverviewCards /
     * FullMonthCalendar, which previously read the full transaction table directly.
     */
    val windowedTransactionsFlow: StateFlow<List<MpesaTransactions>> = _selectedDate
        .map { date ->
            val month = YearMonth.from(date)
            month.minusMonths(1).atDay(1).toString() to month.plusMonths(1).atEndOfMonth()
                .toString()
        }
        .distinctUntilChanged()
        .flatMapLatest { (start, end) -> dao.getTransactionsInRange(start, end) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val parsedWindowFlow: StateFlow<List<ParsedTx>> = windowedTransactionsFlow
        .map { txs -> txs.map { it.toParsedTx() } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<CalendarUiState> = combine(
        parsedWindowFlow,   // dates already derived once, upstream
        upcomingFlow,
        _selectedDate,
        _filterByDay,
        _selectedFilter
    ) { parsedTxs, upcomingTxs, date, byDay, filter ->

        // Extremely fast filtering using object properties instead of strings
        val filteredTxs = parsedTxs.filter { pTx ->
            val dateMatch = if (byDay) {
                pTx.date == date
            } else {
                pTx.date.year == date.year && pTx.date.monthValue == date.monthValue
            }

            if (!dateMatch) return@filter false

            when (filter) {
                TransactionType.ALL -> true
                TransactionType.INCOME -> pTx.tx.type.equals("income", ignoreCase = true)
                TransactionType.EXPENSE -> pTx.tx.type.equals("expense", ignoreCase = true)
            }
        }.map { it.tx }

        val upcomingForDay = upcomingTxs.filter { it.dueDate == date.toString() }

        CalendarUiState(
            selectedDate = date,
            filterByDay = byDay,
            selectedFilter = filter,
            filteredTransactions = filteredTxs,
            dayGroups = buildDayGroups(filteredTxs),
            upcomingForDay = upcomingForDay
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, CalendarUiState())

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
