package com.example.cashia.ui.screens.Projections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashia.Utility.FinanceProjectionEngine
import com.example.cashia.Utility.PesaActions
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingStatus
import com.example.cashia.models.UpcomingViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionScreen(
    transactionsViewModel: TransactionsViewModel,
    upcomingViewModel: UpcomingViewModel,
    categoryViewModel: CategoryViewModel
) {
    val context = LocalContext.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    val haptic = LocalHapticFeedback.current
    val state = viewModel.state
    val transactions = state.allTransactions
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val upcomingAll by upcomingViewModel.upcoming.collectAsState()

    var selectedPeriod by remember { mutableIntStateOf(1) } // default: This Month
    val period = ProjectionPeriod.entries[selectedPeriod]
    val horizonDays = remember(selectedPeriod) { period.daysRemaining() }

    var balanceOverride by remember { mutableStateOf("") }
    var configExpanded by remember { mutableStateOf(false) }

    val effectiveBalance = remember(balanceOverride, state.latestBalance) {
        balanceOverride.toDoubleOrNull()?.takeIf { it >= 0 } ?: state.latestBalance
    }

    val earliestDate = remember(transactions) {
        transactions.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .minOrNull() ?: LocalDate.now().minusDays(365)
    }

    val maxLookback = remember(earliestDate) {
        ChronoUnit.DAYS.between(earliestDate, LocalDate.now()).toInt().coerceIn(30, 365)
    }

    val recentTransactions = remember(transactions, maxLookback) {
        val cutoff = LocalDate.now().minusDays(maxLookback.toLong())
        transactions.filter { runCatching { LocalDate.parse(it.date) >= cutoff }.getOrDefault(false) }
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
            horizonDays,
            pendingUpcoming
        )
    }

    val confidence = remember(recentTransactions.size, maxLookback, horizonDays, projection) {
        computeConfidence(recentTransactions.size, maxLookback.toInt(), horizonDays, projection)
    }

    val zeroCrossingDay = remember(projection, effectiveBalance) {
        daysUntilZero(projection.dailyTrend, effectiveBalance)
    }
    val today = LocalDate.now()
    val overdue = remember(pendingUpcoming) {
        pendingUpcoming.filter { LocalDate.parse(it.dueDate).isBefore(today) }
            .sortedBy { it.dueDate }
    }
    val dueSoon = remember(pendingUpcoming) {
        pendingUpcoming.filter {
            val d = LocalDate.parse(it.dueDate)
            !d.isBefore(today) && d.isBefore(today.plusDays(8))
        }.sortedBy { it.dueDate }
    }
    val later = remember(pendingUpcoming) {
        pendingUpcoming.filter {
            LocalDate.parse(it.dueDate) >= today.plusDays(8)
        }.sortedBy { it.dueDate }
    }

    Scaffold(containerColor = Color.Transparent) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
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

            item {
                ProjectionConfigSummary(
                    totalDays = maxLookback,
                    period = period,
                    isBalanceOverridden = balanceOverride.toDoubleOrNull() != null,
                    dataPointCount = recentTransactions.size,
                    pendingUpcoming.size
                )
            }

            item {
                PredictedBalanceCard(
                    currentBalance = effectiveBalance,
                    period = period,
                    projection = projection,
                    confidence = confidence,
                    zeroCrossingDay = zeroCrossingDay
                )
            }

            item {
                ProjectionTrendChart(
                    projection = projection,
                    period = period,
                    currentBalance = effectiveBalance,
                    confidence = confidence
                )
            }

            item {
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

            item {
                ProjectionInsightsCard(
                    projection = projection,
                    transactionCount = recentTransactions.size,
                    period = period,
                    selectedPeriodIndex = selectedPeriod,
                    onPeriodChange = { selectedPeriod = it },
                    confidence = confidence,
                    transactions,
                    allCategories
                )
            }

            item { DailyTrendMiniList(projection = projection) }
        }
    }
}

@Composable
private fun SummaryPill(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    isCustom: Boolean,
    overrideLabel: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val color =
        if (isCustom) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = color.copy(alpha = if (isCustom) 0.08f else 0.04f),
        border = BorderStroke(0.5.dp, color.copy(alpha = if (isCustom) 0.25f else 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(
                overrideLabel ?: label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProjectionConfigSummary(
    totalDays: Int,
    period: ProjectionPeriod,
    isBalanceOverridden: Boolean,
    dataPointCount: Int,
    upcomingCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill(Modifier.weight(1f), "All ${totalDays}d data", Icons.Default.History, false)
        SummaryPill(
            Modifier.weight(1f),
            period.label,
            Icons.Default.AutoGraph,
            isCustom = true
        )
        SummaryPill(
            Modifier.weight(1f),
            "$upcomingCount scheduled",
            Icons.Default.CalendarToday,
            upcomingCount > 0,
            if (isBalanceOverridden) "Custom bal." else null
        )
    }
}