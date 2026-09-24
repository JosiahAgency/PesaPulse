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
    val monthlyBudget =
        combine(_startDate, PesaTimeConfiguration.changes) { date, _ -> YearMonth.from(date) }
            .flatMapLatest { month ->
                dao.getTransactionsInRange(
                    month.atDay(1).toString(),
                    month.atEndOfMonth().toString()
                ).map { rows ->
                    val spending = rows.filter { it.type == "expense" && it.reversedTxId == null }
                    val spent = spending.sumOf { it.amount + it.knownFees }
                    BudgetProgress(
                        PesaTimeConfiguration.getMonthlyBudget(month), spent,
                        rows.filter { it.type == "income" && it.reversedTxId == null }
                            .sumOf { it.amount } - spent,
                        spending.count { it.transactionCost == null }
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        .stateIn(viewModelScope, SharingStarted.Eagerly, AnalyticsUiState())

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