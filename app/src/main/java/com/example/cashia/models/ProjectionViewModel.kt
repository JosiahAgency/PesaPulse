package com.example.cashia.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.utility.FinanceProjectionEngine
import com.example.cashia.utility.ParsedTx
import com.example.cashia.utility.ProjectionPeriod
import com.example.cashia.utility.computeConfidence
import com.example.cashia.utility.daysUntilZero
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ProjectionUiState(
    val selectedPeriodIndex: Int = 1,
    val balanceOverride: String = "",
    val configExpanded: Boolean = false,
    val effectiveBalance: Double = 0.0,
    val period: ProjectionPeriod = ProjectionPeriod.entries[1],
    val recentTransactionsCount: Int = 0,
    /** Null only before the first combine emission. */
    val projection: FinanceProjection? = null,
    val confidence: Int = 0,
    val zeroCrossingDay: Int? = null,
    val overdue: List<UpcomingTransaction> = emptyList(),
    val dueSoon: List<UpcomingTransaction> = emptyList(),
    val later: List<UpcomingTransaction> = emptyList()
)

class ProjectionViewModel(
    transactionsStateFlow: StateFlow<TransactionState>,
    parsedTransactionsFlow: StateFlow<List<ParsedTx>>,
    upcomingFlow: StateFlow<List<UpcomingTransaction>>
) : ViewModel() {

    private val _selectedPeriodIndex = MutableStateFlow(1)
    private val _balanceOverride = MutableStateFlow("")
    private val _configExpanded = MutableStateFlow(false)

    // Pair the two transaction inputs so the main combine stays within 5 flows.
    private val txDataFlow = combine(
        transactionsStateFlow,
        parsedTransactionsFlow
    ) { state, parsed -> state to parsed }

    val uiState: StateFlow<ProjectionUiState> = combine(
        txDataFlow,
        upcomingFlow,
        _selectedPeriodIndex,
        _balanceOverride,
        _configExpanded
    ) { (txState, parsedTxs), upcomingAll, periodIdx, overrideStr, expanded ->

        val today = LocalDate.now()
        val period = ProjectionPeriod.entries[periodIdx]
        val horizonDays = period.daysRemaining()

        val effectiveBalance =
            overrideStr.toDoubleOrNull()?.takeIf { it >= 0 } ?: txState.latestBalance

        // 1. Efficient Date Search — dates are pre-derived upstream, no parsing here
        val earliestDate = parsedTxs.minOfOrNull { it.date } ?: today.minusDays(365)

        val maxLookBack = ChronoUnit.DAYS.between(earliestDate, today).toInt().coerceIn(30, 365)
        val cutoff = today.minusDays(maxLookBack.toLong())

        // 2. Filter Recent
        val recentTransactions = parsedTxs.filter { it.date >= cutoff }.map { it.tx }

        // 3. Pending Upcoming
        val pendingUpcoming = upcomingAll.filter { it.status == UpcomingStatus.PENDING }

        // 4. Execute Engine on Background Thread
        val projection = FinanceProjectionEngine.project(
            transactions = recentTransactions,
            currentBalance = effectiveBalance,
            balanceAsOfDate = if (overrideStr.toDoubleOrNull() != null) today else txState.latestBalanceDateParsed,
            daysForward = horizonDays,
            upcomingTransactions = pendingUpcoming
        )

        val confidence =
            computeConfidence(recentTransactions.size, maxLookBack, horizonDays, projection)
        val zeroCrossingDay = daysUntilZero(projection.dailyTrend, effectiveBalance)

        // 5. Categorize Upcoming
        val overdue = mutableListOf<UpcomingTransaction>()
        val dueSoon = mutableListOf<UpcomingTransaction>()
        val later = mutableListOf<UpcomingTransaction>()
        val soonCutoff = today.plusDays(8)

        for (tx in pendingUpcoming) {
            val d = tx.dueDateParsed ?: continue
            when {
                d.isBefore(today) -> overdue.add(tx)
                d.isBefore(soonCutoff) -> dueSoon.add(tx)
                else -> later.add(tx)
            }
        }

        ProjectionUiState(
            selectedPeriodIndex = periodIdx,
            balanceOverride = overrideStr,
            configExpanded = expanded,
            effectiveBalance = effectiveBalance,
            period = period,
            recentTransactionsCount = recentTransactions.size,
            projection = projection,
            confidence = confidence,
            zeroCrossingDay = zeroCrossingDay,
            overdue = overdue.sortedBy { it.dueDate },
            dueSoon = dueSoon.sortedBy { it.dueDate },
            later = later.sortedBy { it.dueDate }
        )
    }
        .flowOn(Dispatchers.Default) // Crucial: Moves all math off the UI thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectionUiState())

    // ── Actions ──────────────────────────────────────────────────────────────

    fun setPeriod(index: Int) {
        _selectedPeriodIndex.value = index
    }

    fun setBalanceOverride(value: String) {
        _balanceOverride.value = value
    }

    fun toggleConfig() {
        _configExpanded.value = !_configExpanded.value
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    companion object {
        fun provideFactory(
            transactionsStateFlow: StateFlow<TransactionState>,
            parsedTransactionsFlow: StateFlow<List<ParsedTx>>,
            upcomingFlow: StateFlow<List<UpcomingTransaction>>
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProjectionViewModel(
                    transactionsStateFlow,
                    parsedTransactionsFlow,
                    upcomingFlow
                ) as T
            }
        }
    }
}