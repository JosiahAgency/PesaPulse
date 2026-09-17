package com.example.cashia.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.AppDatabase
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TimeScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
private val weekFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

data class DashboardState(
    val filteredTransactions: List<MpesaTransactions> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val periodLabel: String = "",
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
    /** False until the first Room emission arrives; distinguishes "loading" from "loaded but empty". */
    val hasLoaded: Boolean = false,
)

/**
 * "Pace this month" — a relabeled reuse of Insights' burn-rate/projected-pace math
 * (see AnalyticsExtras.kt's BurnRateCard), NOT a persisted budget limit. [progress] is
 * spend-so-far ÷ pace-projected-total-for-the-month, independent of the Day/Week/Month/Year
 * scope selector so it always reads as "this calendar month" regardless of what's selected.
 */
data class MonthPaceState(
    val progress: Float = 0f,
    val percentLabel: Int = 0,
    val remainingLabel: Double = 0.0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    context: Context,
) : ViewModel() {

    private val dao = AppDatabase.getDatabase(context).transactionDao()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _scope = MutableStateFlow(TimeScope.DAY)
    val scope = _scope.asStateFlow()

    private val _visibleWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(PesaTimeConfiguration.firstDayOfWeek))
    )

    /**
     * Bounded to the visible week plus a one-week buffer on each side instead of the full
     * table. [WeeklyCalendar] reports the week it's actually showing back through
     * [setVisibleWeek] as the user swipes, so this stays centered on-screen.
     */
    val weeklyCalendarTransactionsFlow: StateFlow<List<MpesaTransactions>> = _visibleWeekStart
        .flatMapLatest { weekStart ->
            dao.getTransactionsInRange(
                weekStart.minusWeeks(1).toString(),
                weekStart.plusWeeks(2).minusDays(1).toString()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _isListExpanded = MutableStateFlow(true)
    val isListExpanded = _isListExpanded.asStateFlow()

    val monthPace: StateFlow<MonthPaceState> = run {
        val today = LocalDate.now()
        val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
        val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())
        dao.getTransactionsInRange(monthStart.toString(), monthEnd.toString())
            .map { txs ->
                val totalSpend = txs
                    .filter { it.type == "expense" && it.reversedTxId == null }
                    .sumOf { it.amount }
                val totalDays = ChronoUnit.DAYS.between(monthStart, monthEnd).toInt() + 1
                val elapsedDays = (ChronoUnit.DAYS.between(monthStart, today.coerceAtMost(monthEnd))
                    .toInt() + 1).coerceAtLeast(1)
                val dailyRate = totalSpend / elapsedDays
                val projectedTotal = dailyRate * totalDays
                val progress = if (projectedTotal <= 0.0) 0f
                else (totalSpend / projectedTotal).toFloat().coerceIn(0f, 1f)

                MonthPaceState(
                    progress = progress,
                    percentLabel = (progress * 100).roundToInt(),
                    remainingLabel = (projectedTotal - totalSpend).coerceAtLeast(0.0)
                )
            }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthPaceState())
    }

    // The date-range filter runs in SQL: only the rows inside the visible period
    // are ever loaded, and Room re-emits automatically on any table change.
    val dashboardState: StateFlow<DashboardState> = combine(
        _selectedDate,
        _scope,
    ) { date, scope -> date to scope }
        .flatMapLatest { (date, scope) ->
            val firstDay = PesaTimeConfiguration.firstDayOfWeek
            val (start, end) = computeRange(date, scope, firstDay)

            val label = when (scope) {
                TimeScope.DAY -> date.format(dayFormatter)
                TimeScope.WEEK -> "${start.format(weekFormatter)} – ${end.format(weekFormatter)}"
                TimeScope.MONTH -> date.format(monthFormatter)
                TimeScope.YEAR -> date.format(yearFormatter)
                else -> "Invalid Time Scope Range = No Label"
            }

            dao.getTransactionsInRange(start.toString(), end.toString()).map { txs ->
                var income = 0.0
                var expenses = 0.0
                for (tx in txs) {
                    when (tx.type) {
                        "income" -> income += tx.amount
                        "expense" -> expenses += (tx.amount + tx.transactionCost)
                    }
                }

                DashboardState(
                    filteredTransactions = txs,
                    totalIncome = income,
                    totalExpenses = expenses,
                    periodLabel = label,
                    dateRangeStart = start,
                    dateRangeEnd = end,
                    hasLoaded = true,
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    // ── Actions ────────────────────────────────────────────────────────────────

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setVisibleWeek(weekStart: LocalDate) {
        _visibleWeekStart.value = weekStart
    }

    fun setScope(scope: TimeScope) {
        _scope.value = scope
    }

    fun toggleListExpanded() {
        _isListExpanded.value = !_isListExpanded.value
    }

    private fun computeRange(
        date: LocalDate,
        scope: TimeScope,
        firstDay: DayOfWeek
    ): Pair<LocalDate, LocalDate> {
        return when (scope) {
            TimeScope.DAY -> date to date
            TimeScope.WEEK -> {
                val s = date.with(TemporalAdjusters.previousOrSame(firstDay))
                s to s.plusDays(6)
            }

            TimeScope.MONTH -> date.with(TemporalAdjusters.firstDayOfMonth()) to
                    date.with(TemporalAdjusters.lastDayOfMonth())

            TimeScope.YEAR -> date.with(TemporalAdjusters.firstDayOfYear()) to
                    date.with(TemporalAdjusters.lastDayOfYear())

            else -> date to date
        }
    }

    companion object {
        fun provideFactory(
            context: Context,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DashboardViewModel::class.java))
                        return DashboardViewModel(context) as T
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}