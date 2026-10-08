package com.example.cashia.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.daos.TransactionDao
import com.example.cashia.utility.MonthlyBudget
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.RecurringMerchant
import com.example.cashia.utility.TimeScope
import com.example.cashia.utility.TransactionType
import com.example.cashia.utility.WeekdayPattern
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
import java.time.YearMonth
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

data class BudgetProgress(
    val budget: MonthlyBudget,
    val spent: Double,
    val netSaved: Double,
    val unknownFeeCount: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val dao: TransactionDao
) : ViewModel() {

    private val _selectedType = MutableStateFlow(TransactionType.EXPENSE)
    private val _selectedScope = MutableStateFlow(TimeScope.MONTH)
    private val _startDate = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    private val _endDate = MutableStateFlow(LocalDate.now())
    /**
     * Budget headline for the anchored month. Only four scalars are needed, so they are
     * summed in SQL rather than hydrating every row of the month — this ran as a third
     * full-row load over a window the other two flows below already cover.
     */
    val monthlyBudget =
        combine(_startDate, PesaTimeConfiguration.changes) { date, _ -> YearMonth.from(date) }
            .flatMapLatest { month ->
                dao.getRangeSpendFlow(
                    month.atDay(1).toString(),
                    month.atEndOfMonth().toString()
                ).map { row ->
                    BudgetProgress(
                        PesaTimeConfiguration.getMonthlyBudget(month),
                        row.spent,
                        row.earned - row.spent,
                        row.unknownFeeCount
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Bounded to the selected period (both transaction types) instead of the full table —
     * serves the cards that filter by date range only, not by type: TypeSelectorCard,
     * CashFlowTrendChart, BurnRateCard.
     */
    val periodTransactions: StateFlow<List<MpesaTransactions>> = combine(
        _startDate,
        _endDate
    ) { start, end -> start to end }
        .flatMapLatest { (start, end) ->
            dao.getTransactionsInRange(
                start.toString(),
                end.toString()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Weekday spend/income pattern computed in SQL (see [TransactionDao.getWeekdaySpendPattern])
     * instead of scanning the full transaction table client-side. SQLite's `strftime('%w')` is
     * 0=Sunday..6=Saturday, which does not line up 1:1 with [DayOfWeek] (MONDAY=1..SUNDAY=7), so
     * the mapping below translates explicitly rather than casting.
     */
    val weekdayPatternFlow: StateFlow<List<WeekdayPattern>> = _selectedType
        .flatMapLatest { type -> dao.getWeekdaySpendPattern(type.name.lowercase()) }
        .map { rows ->
            val byDow = rows.associateBy { it.dow }
            DayOfWeek.entries.map { dow ->
                val row = byDow[dow.value % 7] // MONDAY(1)->1 ... SATURDAY(6)->6, SUNDAY(7)->0
                WeekdayPattern(
                    day = dow,
                    avgAmount = if (row == null || row.activeDays == 0) 0.0
                    else row.totalAmount / row.activeDays,
                    txCount = row?.txCount ?: 0
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Recurring-merchant detection computed in SQL (see [TransactionDao.getRecurringMerchants])
     * instead of grouping the full transaction table client-side.
     */
    val recurringMerchantsFlow: StateFlow<List<RecurringMerchant>> = _selectedType
        .flatMapLatest { type ->
            dao.getRecurringMerchants(
                type.name.lowercase(),
                minMonths = 2,
                limit = 8
            )
        }
        .map { rows ->
            rows.map { row ->
                RecurringMerchant(
                    recipient = row.recipient.trim(),
                    avgAmount = row.totalAmount / row.occurrences,
                    occurrences = row.occurrences,
                    lastDate = LocalDate.parse(row.lastDate),
                    totalAmount = row.totalAmount
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
        .combine(periodTransactions) { base, rows ->
            // `periodTransactions` already holds exactly this window: `timestamp` is derived
            // from the same `date` string (see convertToTimestamp), and startTs/endTs span
            // those whole days. Narrowing by type in memory therefore yields the same rows
            // the dedicated type+range query returned, without a second load of the period.
            val type = base.selectedType.name.lowercase()
            base.copy(filteredTransactions = rows.filter { it.type == type })
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState())

    /**
     * This-month-vs-last-month comparison for the cash flow trend card's flip side.
     * Anchored on the real "today" (via [PesaTimeConfiguration.changes]) rather than the
     * selected period/scope, so flipping the card always compares the two most recent
     * calendar months regardless of what date range is picked above it.
     */
    val monthComparisonTransactions: StateFlow<List<MpesaTransactions>> =
        PesaTimeConfiguration.changes
            .map { YearMonth.now() }
            .flatMapLatest { currentMonth ->
                val lastMonth = currentMonth.minusMonths(1)
                dao.getTransactionsInRange(
                    lastMonth.atDay(1).toString(),
                    currentMonth.atEndOfMonth().toString()
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun periodLabel(scope: TimeScope, start: LocalDate, end: LocalDate): String =
        when (scope) {
            TimeScope.DAY -> LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))

            TimeScope.WEEK -> "${start.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                end.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                )
            }"

            TimeScope.MONTH -> LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
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
            val today = LocalDate.now()
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

//    fun refreshCurrentScopeRange() {
//        val scope = _selectedScope.value
//        if (scope != TimeScope.CUSTOM) setScope(scope)
//    }

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