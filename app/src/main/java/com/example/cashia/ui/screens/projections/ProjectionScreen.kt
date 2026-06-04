package com.example.cashia.ui.screens.projections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingStatus
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.utility.FinanceProjectionEngine
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.ProjectionPeriod
import com.example.cashia.utility.computeConfidence
import com.example.cashia.utility.daysUntilZero
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionScreen(
    transactionsViewModel: TransactionsViewModel,
    upcomingViewModel: UpcomingViewModel,
    categoryViewModel: CategoryViewModel
) {
    val haptic = LocalHapticFeedback.current

    val state by transactionsViewModel.state.collectAsState()
    val transactions = state.allTransactions

    val allCategories by categoryViewModel.allCategories.collectAsState()
    val upcomingAll by upcomingViewModel.upcoming.collectAsState()

    var selectedPeriod by remember { mutableIntStateOf(1) }
    val period = ProjectionPeriod.entries[selectedPeriod]
    val horizonDays = remember(selectedPeriod) { period.daysRemaining() }

    var balanceOverride by remember { mutableStateOf("") }
    var configExpanded by remember { mutableStateOf(false) }

    val effectiveBalance = remember(balanceOverride, state.latestBalance) {
        balanceOverride.toDoubleOrNull()?.takeIf { it >= 0 } ?: state.latestBalance
    }

    val earliestDate = remember(transactions) {
        transactions
            .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .minOrNull() ?: LocalDate.now().minusDays(365)
    }

    val maxLookBack = remember(earliestDate) {
        ChronoUnit.DAYS.between(earliestDate, LocalDate.now()).toInt().coerceIn(30, 365)
    }

    val recentTransactions = remember(transactions, maxLookBack) {
        val cutoff = LocalDate.now().minusDays(maxLookBack.toLong())
        transactions.filter {
            runCatching { LocalDate.parse(it.date) >= cutoff }.getOrDefault(false)
        }
    }

    val pendingUpcoming = remember(upcomingAll) {
        upcomingAll.filter { it.status == UpcomingStatus.PENDING }
    }

    val projection = remember(
        recentTransactions, effectiveBalance, horizonDays,
        state.latestBalanceDateParsed, pendingUpcoming
    ) {
        FinanceProjectionEngine.project(
            transactions = recentTransactions,
            currentBalance = effectiveBalance,
            balanceAsOfDate = if (balanceOverride.toDoubleOrNull() != null) LocalDate.now()
            else state.latestBalanceDateParsed,
            daysForward = horizonDays,
            upcomingTransactions = pendingUpcoming
        )
    }

    val confidence = remember(recentTransactions.size, maxLookBack, horizonDays, projection) {
        computeConfidence(recentTransactions.size, maxLookBack, horizonDays, projection)
    }

    val zeroCrossingDay = remember(projection, effectiveBalance) {
        daysUntilZero(projection.dailyTrend, effectiveBalance)
    }

    val today = remember { LocalDate.now() }

    // [FIX] Original used LocalDate.parse(tx.dueDate) directly — no try-catch, would crash
    // on any malformed date string. Now uses tx.dueDateParsed (lazy, already guarded) and
    // skips the item with ?: continue instead of crashing.
    val (overdue, dueSoon, later) = remember(pendingUpcoming, today) {
        val ov = mutableListOf<UpcomingTransaction>()
        val ds = mutableListOf<UpcomingTransaction>()
        val lt = mutableListOf<UpcomingTransaction>()
        val soonCutoff = today.plusDays(8)

        for (tx in pendingUpcoming) {
            val d = tx.dueDateParsed ?: continue     // skip if date is malformed
            when {
                d.isBefore(today) -> ov.add(tx)
                d.isBefore(soonCutoff) -> ds.add(tx)
                else -> lt.add(tx)
            }
        }

        // Sort by dueDate string — ISO-8601 sorts lexicographically correctly.
        Triple(
            ov.sortedBy { it.dueDate },
            ds.sortedBy { it.dueDate },
            lt.sortedBy { it.dueDate }
        )
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        // [FIX] Added keys to every item so Compose can skip recomposition for
        // sections whose inputs haven't changed. Without keys, the framework
        // recomposes all items whenever any single input changes.
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(),
            contentPadding = PaddingValues(16.dp, 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "config") {
                ProjectionConfigCard(
                    isExpanded = configExpanded,
                    onToggle = { PesaActions.playClick(haptic); configExpanded = !configExpanded },
                    selectedPeriod = selectedPeriod,
                    onPeriodChange = { selectedPeriod = it },
                    balanceOverride = balanceOverride,
                    onBalanceOverrideChange = { balanceOverride = it },
                    autoBalance = state.latestBalance,
                    autoBalanceDate = state.latestBalanceDate,
                    haptic = haptic
                )
            }

            item(key = "balance_card") {
                PredictedBalanceCard(
                    currentBalance = effectiveBalance,
                    period = period,
                    projection = projection,
                    confidence = confidence,
                    zeroCrossingDay = zeroCrossingDay
                )
            }

            item(key = "trend_chart") {
                ProjectionTrendChart(
                    projection = projection,
                    period = period,
                    currentBalance = effectiveBalance,
                    confidence = confidence
                )
            }

            item(key = "upcoming_section") {
                UpcomingSection(
                    transactionsViewModel,
                    overdue,
                    dueSoon,
                    later,
                    allCategories,
                    upcomingViewModel,
                    haptic,
                    categoryViewModel
                )
            }

            item(key = "insights_card") {
                ProjectionInsightsCard(
                    projection = projection,
                    transactionCount = recentTransactions.size,
                    period = period,
                    selectedPeriodIndex = selectedPeriod,
                    onPeriodChange = { selectedPeriod = it },
                    transactions,
                    allCategories
                )
            }
        }
    }
}