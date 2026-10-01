package com.example.cashia.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.AppDatabase
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TimeScope
import com.example.cashia.utility.buildDayGroups
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
private val weekFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val yearFormatter = DateTimeFormatter.ofPattern("yyyy")

data class DashboardState(
    val filteredTransactions: List<MpesaTransactions> = emptyList(),
    val dayGroups: List<DayGroup> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val periodLabel: String = "",
    val dateRangeStart: LocalDate? = null,
    val dateRangeEnd: LocalDate? = null,
    /** Expenses in the period whose transaction cost never appeared in the SMS — the totals
     *  above are understated by exactly these unknown fees. */
    val unknownFeeCount: Int = 0,
    /** False until the first Room emission arrives; distinguishes "loading" from "loaded but empty". */
    val hasLoaded: Boolean = false,
)

/** "Pace this month" against the user's saved monthly target for the current month. */data class MonthPaceState(
    val hasTarget: Boolean = false,
    val targetAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val progress: Float = 0f,
    val percentLabel: Int = 0,
    val remainingLabel: Double = 0.0,
)

/**
 * Which days in the calendar strip's window saw money move. Membership is all the dots
 * encode, so this is resolved from a two-column projection rather than full rows.
 */
data class CalendarActivity(
    val incomeDates: Set<String> = emptySet(),
    val expenseDates: Set<String> = emptySet(),
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

    val weeklyCalendarActivity: StateFlow<CalendarActivity> = _visibleWeekStart
        .flatMapLatest { weekStart ->
            dao.getDayActivityInRange(
                weekStart.minusWeeks(1).toString(),
                weekStart.plusWeeks(2).minusDays(1).toString()
            )
        }
        .map { rows ->
            CalendarActivity(
                incomeDates = rows.filter { it.type == "income" }.mapTo(HashSet()) { it.date },
                expenseDates = rows.filter { it.type == "expense" }.mapTo(HashSet()) { it.date },
            )
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarActivity())

    private val _isListExpanded = MutableStateFlow(true)
    val isListExpanded = _isListExpanded.asStateFlow()

    private val _monthPaceRefresh = MutableStateFlow(0)

    /** Last month's total spend — offered as a one-tap starting point for this month's target. */
    val previousMonthSpend: StateFlow<Double> = _monthPaceRefresh
        .flatMapLatest {
            val previousMonth = YearMonth.now().minusMonths(1)
            dao.getExpenseTotalInRange(
                previousMonth.atDay(1).toString(),
                previousMonth.atEndOfMonth().toString()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val monthPace: StateFlow<MonthPaceState> = combine(
        _monthPaceRefresh, PesaTimeConfiguration.changes
    ) { refresh, _ -> refresh }
        .flatMapLatest {
            val today = LocalDate.now()
            val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
            val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())
            dao.getExpenseTotalInRange(monthStart.toString(), monthEnd.toString())
                .map { totalSpend ->
                    val target =
                        PesaTimeConfiguration.getMonthlySpendingTarget(YearMonth.from(today))
                    if (target == null || target <= 0.0) {
                        MonthPaceState(hasTarget = false, spentAmount = totalSpend)
                    } else {
                        val ratio = totalSpend / target
                        MonthPaceState(
                            hasTarget = true,
                            targetAmount = target,
                            spentAmount = totalSpend,
                            progress = ratio.toFloat().coerceIn(0f, 1f),
                            percentLabel = (ratio * 100).roundToInt(),
                            remainingLabel = (target - totalSpend).coerceAtLeast(0.0)
                        )
                    }
                }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthPaceState())

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
                var unknownFees = 0
                for (tx in txs) {
                    when (tx.type) {
                        "income" -> income += tx.amount
                        "expense" -> {
                            expenses += (tx.amount + tx.knownFees)
                            if (tx.transactionCost == null && tx.reversedTxId == null) {
                                unknownFees++
                            }
                        }
                    }
                }

                DashboardState(
                    filteredTransactions = txs,
                    dayGroups = buildDayGroups(txs),
                    totalIncome = income,
                    totalExpenses = expenses,
                    periodLabel = label,
                    dateRangeStart = start,
                    dateRangeEnd = end,
                    unknownFeeCount = unknownFees,
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

    fun refreshMonthlyContext() {
        _monthPaceRefresh.value += 1
    }

    fun saveCurrentMonthTarget(amount: Double) {
        PesaTimeConfiguration.saveMonthlySpendingTarget(YearMonth.now(), amount)
        refreshMonthlyContext()
    }

    /** Returns false (and saves nothing) when last month has no recorded spend to copy. */
    fun useLastMonthSpendAsCurrentTarget(): Boolean {
        val amount = previousMonthSpend.value
        if (amount <= 0.0) return false
        PesaTimeConfiguration.saveMonthlySpendingTarget(YearMonth.now(), amount)
        refreshMonthlyContext()
        return true
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