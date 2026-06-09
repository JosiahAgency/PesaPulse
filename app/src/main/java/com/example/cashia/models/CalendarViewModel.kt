package com.example.cashia.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.utility.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CalendarUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val filterByDay: Boolean = false,
    val selectedFilter: TransactionType = TransactionType.ALL,
    val filteredTransactions: List<MpesaTransactions> = emptyList(),
    val upcomingForDay: List<UpcomingTransaction> = emptyList() // Adjust type based on your Upcoming model
)

private data class ParsedCalTx(val tx: MpesaTransactions, val date: LocalDate)

class CalendarViewModel(
    allTransactionsFlow: StateFlow<List<MpesaTransactions>>,
    upcomingFlow: StateFlow<List<UpcomingTransaction>>
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _filterByDay = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow(TransactionType.ALL)

    // 1. Pre-parse transaction dates ONCE in the background
    private val parsedTxsFlow = allTransactionsFlow.map { txList ->
        txList.map { tx ->
            val date = runCatching { LocalDate.parse(tx.date) }.getOrElse { LocalDate.MIN }
            ParsedCalTx(tx, date)
        }
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<CalendarUiState> = combine(
        parsedTxsFlow,
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
            upcomingForDay = upcomingForDay
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
            allTransactionsFlow: StateFlow<List<MpesaTransactions>>,
            upcomingFlow: StateFlow<List<UpcomingTransaction>> // Adjust type
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CalendarViewModel(allTransactionsFlow, upcomingFlow) as T
            }
        }
    }
}