package com.example.cashia.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.daos.TransactionDao
import com.example.cashia.utility.TimeScope
import com.example.cashia.utility.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class AnalyticsUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedScope: TimeScope = TimeScope.MONTH,
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now(),
    val periodLabel: String = "",
    val startTs: Long = 0L,
    val endTs: Long = 0L,
    val filteredTransactions: List<MpesaTransactions> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val dao: TransactionDao
) : ViewModel() {

    private val today = LocalDate.now()

    private val _selectedType = MutableStateFlow(TransactionType.EXPENSE)
    private val _selectedScope = MutableStateFlow(TimeScope.MONTH)
    private val _startDate = MutableStateFlow(today.withDayOfMonth(1))
    private val _endDate = MutableStateFlow(today)

    // Type + time-range filtering runs in SQL: only the matching rows are loaded,
    // and Room re-emits automatically whenever the table changes.
    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedType,
        _selectedScope,
        _startDate,
        _endDate
    ) { type, scope, start, end ->
        AnalyticsUiState(
            selectedType = type,
            selectedScope = scope,
            startDate = start,
            endDate = end,
            periodLabel = periodLabel(scope, start, end),
            startTs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            endTs = end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli(),
        )
    }
        .flatMapLatest { base ->
            dao.getByTypeInRange(base.selectedType.name.lowercase(), base.startTs, base.endTs)
                .map { filtered -> base.copy(filteredTransactions = filtered) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    private fun periodLabel(scope: TimeScope, start: LocalDate, end: LocalDate): String =
        when (scope) {
            TimeScope.DAY -> today.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
            TimeScope.WEEK -> "${start.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                end.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                )
            }"

            TimeScope.MONTH -> today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimeScope.CUSTOM -> "${start.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                end.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                )
            }"

            else -> ""
        }

    // ── Actions ──────────────────────────────────────────────────────────────

    fun setType(type: TransactionType) {
        _selectedType.value = type
    }

    fun setScope(scope: TimeScope) {
        _selectedScope.value = scope
        if (scope != TimeScope.CUSTOM) {
            val (newStart, newEnd) = when (scope) {
                TimeScope.DAY -> today to today
                TimeScope.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
                        today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

                TimeScope.MONTH -> today.withDayOfMonth(1) to
                        today.with(TemporalAdjusters.lastDayOfMonth())

                else -> _startDate.value to _endDate.value
            }
            _startDate.value = newStart
            _endDate.value = newEnd
        }
    }

    fun setCustomStartDate(date: LocalDate) {
        _startDate.value = date
        _selectedScope.value = TimeScope.CUSTOM
    }

    fun setCustomEndDate(date: LocalDate) {
        _endDate.value = date
        _selectedScope.value = TimeScope.CUSTOM
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    companion object {
        fun provideFactory(
            dao: TransactionDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AnalyticsViewModel(dao) as T
            }
        }
    }
}