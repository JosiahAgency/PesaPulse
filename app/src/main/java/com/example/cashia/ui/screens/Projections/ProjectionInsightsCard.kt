package com.example.cashia.ui.screens.Projections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.ui.screens.Analytics.GlassySurface
import java.time.LocalDate


@Composable
fun ProjectionInsightsCard(
    projection: FinanceProjection,
    transactionCount: Int,
    horizonDays: Int,
    confidence: Int,
    allTransactions: List<MpesaTransactions>,
    allCategories: List<ResolvedCategory>
) {
    val colorScheme = MaterialTheme.colorScheme

    val periods = listOf("Week", "Month", "Year")
    var selectedPeriod by remember { mutableIntStateOf(1) }
    val haptic = LocalHapticFeedback.current

    val (periodIncome, periodExpense) = remember(allTransactions, selectedPeriod) {
        val cutoff = when (selectedPeriod) {
            0 -> LocalDate.now().minusWeeks(1)
            1 -> LocalDate.now().minusMonths(1)
            else -> LocalDate.now().minusYears(1)
        }
        val filtered = allTransactions.filter {
            runCatching { LocalDate.parse(it.date) >= cutoff }.getOrDefault(false)
        }
        val inc = filtered.filter { it.type == "income" }.sumOf { it.amount }
        val exp = filtered.filter { it.type == "expense" }.sumOf { it.amount }
        inc to exp
    }
    val savingsRate = if (periodIncome > 0)
        ((periodIncome - periodExpense) / periodIncome * 100) else 0.0

    val dailyAvgSpend = if (horizonDays > 0)
        projection.predictedExpenses / horizonDays else 0.0
    val dailyAvgIncome = if (horizonDays > 0)
        projection.predictedIncome / horizonDays else 0.0

    val monthlyBurnRate = remember(allTransactions) {
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

    remember(projection, allCategories) {
        null as String?
    }
    val topCategoryPair = remember(allTransactions, allCategories) {
        val catTotals = allTransactions
            .filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .entries.maxByOrNull { it.value }
        val cat = catTotals?.let { allCategories.find { c -> c.id == it.key } }
        val emoji = cat?.emoji ?: "📦"
        val label = cat?.label ?: catTotals?.key ?: "—"
        val amount = catTotals?.value ?: 0.0
        Triple(emoji, label, amount)
    }

    val daysUntilZero = remember(projection) {
        projection.dailyTrend.indexOfFirst { it.second <= 0 }
            .takeIf { it >= 0 }?.plus(1)
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Projection Insights",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Text(
                    "Projected daily net: ${if (projection.projectedDailyNet >= 0) "+" else ""}${
                        PesaTimeConfiguration.formatCurrency(projection.projectedDailyNet)
                    }. Based on ${transactionCount} transactions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 16.sp
                )
            }
            if (projection.dataQualityNote.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.06f),
                    border = BorderStroke(0.5.dp, Color.Red.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 1.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            projection.dataQualityNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(Modifier.height(12.dp))
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
                        }% of ${periods[selectedPeriod].lowercase()} income saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (savingsRate < 0) colorScheme.error
                        else colorScheme.onSurfaceVariant
                    )
                }
                // Period chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    periods.forEachIndexed { i, label ->
                        Surface(
                            modifier = Modifier.clickable {
                                PesaActions.playClick(haptic)
                                selectedPeriod = i
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPeriod == i) colorScheme.primary
                            else colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedPeriod == i) FontWeight.Bold
                                else FontWeight.Normal,
                                color = if (selectedPeriod == i) Color.White
                                else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = colorScheme.onSurface.copy(alpha = 0.06f)
            )

            // ── 2 & 3. Daily Avg row — Income + Spend side by side ────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InsightStatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowDownward,
                    label = "Daily Avg Income",
                    value = PesaTimeConfiguration.formatCurrency(dailyAvgIncome),
                    color = colorScheme.tertiary
                )
                InsightStatTile(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowUpward,
                    label = "Daily Avg Spend",
                    value = PesaTimeConfiguration.formatCurrency(dailyAvgSpend),
                    color = colorScheme.error
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = colorScheme.onSurface.copy(alpha = 0.06f)
            )

            // ── 4. Monthly burn rate ──────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocalFireDepartment, null,
                            tint = colorScheme.error, modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Monthly Burn Rate",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        "Average monthly expenses from history",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    PesaTimeConfiguration.formatCurrency(monthlyBurnRate),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.error
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = colorScheme.onSurface.copy(alpha = 0.06f)
            )

            // ── 5. Most expensive category ────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFA8231).copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(topCategoryPair.first, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Top Spend Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        topCategoryPair.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    PesaTimeConfiguration.formatCurrency(topCategoryPair.third),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFA8231)
                )
            }

            if (projection.predictedBalance < 0 || daysUntilZero != null) {
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
                            tint = colorScheme.error, modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (daysUntilZero != null)
                                "Balance may reach zero in ~${daysUntilZero}d — consider reducing spend"
                            else "Balance projected to go negative",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun InsightStatTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
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
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = color.copy(alpha = 0.7f))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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