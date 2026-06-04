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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@SuppressLint("DefaultLocale")
@Composable
fun ProjectionInsightsCard(
    projection: FinanceProjection,
    transactionCount: Int,
    period: ProjectionPeriod,
    selectedPeriodIndex: Int,
    onPeriodChange: (Int) -> Unit,
    allTransactions: List<MpesaTransactions>,
    allCategories: List<ResolvedCategory>
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // [FIX] Hoist today so all remember blocks key off the same value.
    // Originally LocalDate.now() was called inside remember{} lambdas with
    // no date-based key — if the day changed while the app was open, daysElapsed
    // and periodCutoff would silently stale until the user triggered recomposition
    // by changing selectedPeriodIndex. Hoisting also makes the dependency explicit.
    val today = remember { LocalDate.now() }

    // ── Period cutoff ──────────────────────────────────────────────────────────
    val periodCutoff = remember(selectedPeriodIndex, today) {
        when (ProjectionPeriod.entries[selectedPeriodIndex]) {
            ProjectionPeriod.THIS_WEEK -> today.with(DayOfWeek.MONDAY)
            ProjectionPeriod.THIS_MONTH -> today.withDayOfMonth(1)
            ProjectionPeriod.THIS_QUARTER -> {
                val qStartMonth = ((today.monthValue - 1) / 3) * 3 + 1
                today.withMonth(qStartMonth).withDayOfMonth(1)
            }

            ProjectionPeriod.THIS_YEAR -> today.withDayOfYear(1)
        }
    }

    // [FIX] Added periodCutoff to the remember key. The lambda captures it, so it
    // was already an implicit dependency — making it explicit prevents confusion if
    // the key list is ever refactored.
    val periodTransactions = remember(allTransactions, periodCutoff) {
        allTransactions.filter {
            runCatching { LocalDate.parse(it.date) >= periodCutoff }.getOrDefault(false)
        }
    }

    // [PERF] Replaced two separate filter+sumOf passes (2× list traversal, 2×
    // intermediate list allocation) with a single accumulation loop. For a
    // transaction list of n items this halves the work: O(n) instead of O(2n).
    // [FIX] Uses isIncome / isExpense from the improved MpesaTransactions model
    // instead of raw string comparison.
    val (periodIncome, periodExpense) = remember(periodTransactions) {
        var income = 0.0
        var expense = 0.0
        for (tx in periodTransactions) {
            when {
                tx.isIncome -> income += tx.amount
                tx.isExpense -> expense += tx.amount
            }
        }
        income to expense
    }

    val savingsRate = when {
        periodIncome > 0 -> ((periodIncome - periodExpense) / periodIncome) * 100
        periodExpense > 0 -> -100.0
        else -> 0.0
    }

    // [FIX] today now used consistently — no hidden LocalDate.now() call inside remember.
    val daysElapsed = remember(periodCutoff, today) {
        ChronoUnit.DAYS.between(periodCutoff, today).toInt().coerceAtLeast(1)
    }

    val dailyAvgIncome = periodIncome / daysElapsed
    val dailyAvgSpend = periodExpense / daysElapsed

    // [FIX] Removed two dead remember{} blocks:
    //
    //   monthlyBurnRate — called allTransactions.groupBy{}.filter{}.map{}.average()
    //   on every allTransactions change. The result was never assigned to a variable
    //   that the UI reads; it was silently dropped. O(n) work per recomposition for 0 benefit.
    //
    //   topCategory — called filter{}.groupBy{}.mapValues{}.maxByOrNull{}.find{}
    //   on periodTransactions. Same issue: result discarded. The Triple was likely
    //   prepared for a UI element that was cut before shipping.
    //
    // Both are fully removed. Re-add only when the UI actually uses the values.

    val daysUntilZero = remember(projection) {
        projection.dailyTrend
            .indexOfFirst { it.second <= 0 }
            .takeIf { it >= 0 }
            ?.plus(1)
    }

    // [PERF] trendDirection, confidenceLabel, runwaySeverity removed from remember{}.
    // They're pure expressions on stable inputs (Double, Int, Int?). The remember
    // lambda overhead — boxing keys, computing a cache hash, invoking the lambda on
    // first call — costs more than these trivial when-expressions. Plain val is faster
    // and clearer.
    val trendDirection: String = when {
        projection.projectedDailyNet > 0 -> "Growing"
        projection.projectedDailyNet < 0 -> "Declining"
        else -> "Stable"
    }

    val confidenceLabel: String = when {
        transactionCount > 60 -> "High Confidence"
        transactionCount > 30 -> "Moderate Confidence"
        else -> "Low Confidence"
    }

    val runwaySeverity: String? = when {
        daysUntilZero == null -> null
        daysUntilZero <= 3 -> "critical"
        daysUntilZero <= 7 -> "warning"
        else -> "watch"
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI
    // ──────────────────────────────────────────────────────────────────────────

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header + period chips ──────────────────────────────────────────
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
                        SelectableBox(haptic, selectedPeriodIndex == i, { onPeriodChange(i) }) {
                            Text(
                                shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedPeriodIndex == i) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedPeriodIndex == i) Color.White
                                else colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Trend summary pill ─────────────────────────────────────────────
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

            // ── Runway warning ─────────────────────────────────────────────────
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

            // ── Savings Rate ───────────────────────────────────────────────────
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
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
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

            // ── Daily Avg Income + Spend ───────────────────────────────────────
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