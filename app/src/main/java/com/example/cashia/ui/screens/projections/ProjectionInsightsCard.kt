package com.example.cashia.ui.screens.projections

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.SelectableBox
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.ProjectionPeriod
import java.time.LocalDate
import java.time.temporal.ChronoUnit


@SuppressLint("DefaultLocale")
@Composable
fun ProjectionInsightsCard(
    projection: FinanceProjection,
    transactionCount: Int,
    period: ProjectionPeriod,           // ← shared period from screen
    selectedPeriodIndex: Int,           // ← for chip highlight
    onPeriodChange: (Int) -> Unit,      // ← controls the screen-level period
    allTransactions: List<MpesaTransactions>,
    allCategories: List<ResolvedCategory>
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // ── Period cutoff — derived from shared period ────────────────────────────
    val periodCutoff = remember(selectedPeriodIndex) {
        when (ProjectionPeriod.entries[selectedPeriodIndex]) {
            ProjectionPeriod.THIS_WEEK -> LocalDate.now().with(java.time.DayOfWeek.MONDAY)
            ProjectionPeriod.THIS_MONTH -> LocalDate.now().withDayOfMonth(1)
            ProjectionPeriod.THIS_QUARTER -> {
                val month = LocalDate.now().monthValue
                val qStartMonth = ((month - 1) / 3) * 3 + 1
                LocalDate.now().withMonth(qStartMonth).withDayOfMonth(1)
            }

            ProjectionPeriod.THIS_YEAR -> LocalDate.now().withDayOfYear(1)
        }
    }

    val periodTransactions = remember(allTransactions, selectedPeriodIndex) {
        allTransactions.filter {
            runCatching { LocalDate.parse(it.date) >= periodCutoff }.getOrDefault(false)
        }
    }

    val periodIncome = remember(periodTransactions) {
        periodTransactions.filter { it.type == "income" }.sumOf { it.amount }
    }
    val periodExpense = remember(periodTransactions) {
        periodTransactions.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val savingsRate = when {
        periodIncome > 0 -> ((periodIncome - periodExpense) / periodIncome) * 100
        periodExpense > 0 -> -100.0
        else -> 0.0
    }

    // Daily averages use actual days elapsed in this period (not horizon days)
    val daysElapsed = remember(selectedPeriodIndex) {
        ChronoUnit.DAYS.between(periodCutoff, LocalDate.now()).toInt().coerceAtLeast(1)
    }
    val dailyAvgSpend = periodExpense / daysElapsed
    val dailyAvgIncome = periodIncome / daysElapsed

    // Monthly burn rate — rolling average across all available months
    remember(allTransactions) {
        val byMonth = allTransactions
            .groupBy {
                runCatching {
                    val d = LocalDate.parse(it.date)
                    "${d.year}-${d.monthValue}"
                }.getOrDefault("unknown")
            }
            .filter { it.key != "unknown" }
        if (byMonth.isEmpty()) return@remember 0.0
        byMonth.values.map { txs ->
            txs.filter { it.type == "expense" }.sumOf { it.amount }
        }.average()
    }

    // Top category within the selected period
    remember(periodTransactions, allCategories) {
        val catTotals = periodTransactions
            .filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .entries.maxByOrNull { it.value }
        val cat = catTotals?.let { allCategories.find { c -> c.id == it.key } }
        Triple(cat?.emoji ?: "📦", cat?.label ?: catTotals?.key ?: "—", catTotals?.value ?: 0.0)
    }

    val daysUntilZero = remember(projection) {
        projection.dailyTrend.indexOfFirst { it.second <= 0 }
            .takeIf { it >= 0 }?.plus(1)
    }

    val trendDirection = remember(projection.projectedDailyNet) {
        when {
            projection.projectedDailyNet > 0 -> "Growing"
            projection.projectedDailyNet < 0 -> "Declining"
            else -> "Stable"
        }
    }

    val confidenceLabel = remember(projection.projectedDailyNet, transactionCount) {
        when {
            transactionCount > 60 -> "High Confidence"
            transactionCount > 30 -> "Moderate Confidence"
            else -> "Low Confidence"
        }
    }
    val runwaySeverity = remember(daysUntilZero) {
        when {
            daysUntilZero == null -> null
            daysUntilZero <= 3 -> "critical"
            daysUntilZero <= 7 -> "warning"
            else -> "watch"
        }
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header + period chips (shared with the rest of the screen) ────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("Projection Insights")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProjectionPeriod.entries.forEachIndexed { i, p ->
                        val shortLabel = when (p) {
                            ProjectionPeriod.THIS_WEEK -> "Wk"
                            ProjectionPeriod.THIS_MONTH -> "Mo"
                            ProjectionPeriod.THIS_QUARTER -> "Qtr"
                            ProjectionPeriod.THIS_YEAR -> "Yr"
                        }
                        SelectableBox(
                            haptic,
                            selectedPeriodIndex == i,
                            { onPeriodChange(i) }

                        ) {
                            Text(
                                shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedPeriodIndex == i) FontWeight.Bold
                                else FontWeight.Normal,
                                color = if (selectedPeriodIndex == i) Color.White
                                else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Text(
                    "Balance is $trendDirection at ${
                        PesaTimeConfiguration.formatCurrency(projection.projectedDailyNet)
                    }/day • $confidenceLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 16.sp
                )
            }
            if (projection.predictedBalance < 0 || daysUntilZero != null) {

                val message = when {
                    runwaySeverity == "critical" ->
                        "Balance may run out in ~${daysUntilZero}d — urgent action needed"

                    runwaySeverity == "warning" ->
                        "Balance may run out in ~${daysUntilZero}d — monitor spending"

                    daysUntilZero != null ->
                        "Balance may reach zero in ~${daysUntilZero}d"

                    else ->
                        "Balance projected to go negative"
                }

                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.error.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, colorScheme.error.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            message,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(Modifier.height(12.dp))

            // ── 1. Savings Rate ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Savings, null,
                            tint = colorScheme.primary, modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Savings Rate",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        "${
                            String.format(
                                "%.1f",
                                savingsRate
                            )
                        }% of ${period.label.lowercase()} income saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (savingsRate < 0) colorScheme.error
                        else colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${if (savingsRate >= 0) "+" else ""}${String.format("%.1f", savingsRate)}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (savingsRate >= 0) colorScheme.tertiary else colorScheme.error
                )
            }
            if (daysUntilZero != null && daysUntilZero > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Runway: ~$daysUntilZero days at current pace",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = colorScheme.onSurface.copy(alpha = 0.06f)
            )

            // ── 2 & 3. Daily Avg Income + Spend ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightStatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowDownward,
                    label = "Daily Avg Income",
                    sublabel = period.label,
                    value = PesaTimeConfiguration.formatCurrency(dailyAvgIncome),
                    color = colorScheme.tertiary
                )
                InsightStatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowUpward,
                    label = "Daily Avg Spend",
                    sublabel = period.label,
                    value = PesaTimeConfiguration.formatCurrency(dailyAvgSpend),
                    color = colorScheme.error
                )
            }
        }
    }
}


@Composable
private fun InsightStatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    sublabel: String,
    value: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.07f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color.copy(alpha = 0.7f))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}