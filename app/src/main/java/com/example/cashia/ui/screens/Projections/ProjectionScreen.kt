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
import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingStatus
import com.example.cashia.models.UpcomingViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private fun computeConfidence(
    txCount: Int,
    lookbackDays: Int,
    horizonDays: Int,
    projection: FinanceProjection
): Int {
    var score = 100
    val density = txCount.toFloat() / lookbackDays.coerceAtLeast(1)
    if (density < 0.5f) score -= 20
    else if (density < 1f) score -= 10
    if (horizonDays > 30) score -= 15
    else if (horizonDays > 14) score -= 5
    if (txCount < 10) score -= 20
    else if (txCount < 30) score -= 10
    if (projection.dataQualityNote.isNotEmpty()) score -= 15
    return score.coerceIn(10, 95)
}

private fun daysUntilZero(trend: List<Pair<LocalDate, Double>>, currentBalance: Double): Int? {
    if (currentBalance <= 0) return 0
    trend.forEachIndexed { i, (_, bal) ->
        if (bal <= 0) return i + 1
    }
    return null
}

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

    var horizonDays by remember { mutableIntStateOf(30) }
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

    val projection =
        remember(
            recentTransactions,
            effectiveBalance,
            horizonDays,
            state.latestBalanceDateParsed,
            pendingUpcoming
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
                    horizonDays = horizonDays,
                    onHorizonChange = { horizonDays = it },
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
                    horizonDays = horizonDays,
                    isBalanceOverridden = balanceOverride.toDoubleOrNull() != null,
                    dataPointCount = recentTransactions.size,
                    pendingUpcoming.size
                )
            }

            item {
                PredictedBalanceCard(
                    currentBalance = effectiveBalance,
                    horizonDays = horizonDays,
                    projection = projection,
                    confidence = confidence,
                    zeroCrossingDay = zeroCrossingDay
                )
            }

            item {
                ProjectionTrendChart(
                    projection = projection,
                    horizonDays = horizonDays,
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
                    haptic
                )
            }

            item {
                ProjectionInsightsCard(
                    projection = projection,
                    transactionCount = recentTransactions.size,
                    horizonDays = horizonDays,
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
    horizonDays: Int,
    isBalanceOverridden: Boolean,
    dataPointCount: Int,
    upcomingCount: Int   // ← add
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill(Modifier.weight(1f), "All ${totalDays}d data", Icons.Default.History, false)
        SummaryPill(
            Modifier.weight(1f),
            "${horizonDays}d forecast",
            Icons.Default.AutoGraph,
            horizonDays != 30
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
