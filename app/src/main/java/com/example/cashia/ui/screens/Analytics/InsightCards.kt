package com.example.cashia.ui.screens.Analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.ui.theme.primaryGradient
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// 1. Spending Velocity — daily pace vs projected end-of-period total
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SpendingVelocityCard(
    filteredTransactions: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate,
    type: TransactionType
) {
    val today = remember { LocalDate.now() }
    val accentColor = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val stats = remember(filteredTransactions, startDate, endDate) {
        val totalAmount = filteredTransactions.sumOf { it.amount }
        val periodDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val daysElapsed = (ChronoUnit.DAYS.between(startDate, today).toInt() + 1)
            .coerceIn(1, periodDays)
        val dailyAvg = totalAmount / daysElapsed
        val projected = dailyAvg * periodDays
        val paceRatio = (daysElapsed.toFloat() / periodDays).coerceIn(0f, 1f)
        val spendRatio = if (projected > 0)
            (totalAmount / projected).toFloat().coerceIn(0f, 1f)
        else 0f
        VelocityStats(
            totalAmount,
            dailyAvg,
            projected,
            daysElapsed,
            periodDays,
            paceRatio,
            spendRatio
        )
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("SPENDING VELOCITY")
                    Text(
                        "Daily pace & period projection",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Text("⚡", fontSize = 22.sp)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Three key metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VelocityMetric(
                    label = "DAILY AVG",
                    value = PesaTimeConfiguration.formatCurrency(stats.dailyAvg),
                    color = accentColor
                )
                VelocityMetric(
                    label = "SO FAR",
                    value = PesaTimeConfiguration.formatCurrency(stats.totalAmount),
                    color = MaterialTheme.colorScheme.onSurface
                )
                VelocityMetric(
                    label = "PROJECTED",
                    value = PesaTimeConfiguration.formatCurrency(stats.projected),
                    color = if (stats.spendRatio > stats.paceRatio + 0.1f)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.tertiary,
                    alignEnd = true
                )
            }

            // Dual-track progress bar: time elapsed (background) vs money spent (foreground)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Day ${stats.daysElapsed} of ${stats.periodDays}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                    val pace = when {
                        stats.spendRatio > stats.paceRatio + 0.12f -> "🔴 Spending fast"
                        stats.spendRatio < stats.paceRatio - 0.12f -> "🟢 Spending slow"
                        else -> "🟡 On pace"
                    }
                    Text(
                        pace,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Time track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(stats.paceRatio)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                    )
                }
                // Spend track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(stats.spendRatio)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.75f))
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendDot(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        "Time"
                    )
                    LegendDot(accentColor.copy(alpha = 0.75f), "Spend")
                }
            }
        }
    }
}

private data class VelocityStats(
    val totalAmount: Double,
    val dailyAvg: Double,
    val projected: Double,
    val daysElapsed: Int,
    val periodDays: Int,
    val paceRatio: Float,
    val spendRatio: Float
)

@Composable
private fun VelocityMetric(label: String, value: String, color: Color, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Top Merchants — who gets your money most
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TopMerchantsCard(
    filteredTransactions: List<MpesaTransactions>,
    type: TransactionType
) {
    val accentColor = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val merchants = remember(filteredTransactions) {
        filteredTransactions
            .groupBy { it.recipient.trim() }
            .map { (name, txs) ->
                MerchantStat(
                    name = name,
                    total = txs.sumOf { it.amount },
                    count = txs.size
                )
            }
            .sortedByDescending { it.total }
            .take(5)
    }

    if (merchants.isEmpty()) return

    val maxAmount = merchants.first().total

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel(if (type == TransactionType.INCOME) "TOP INCOME SOURCES" else "TOP MERCHANTS")
                    Text(
                        "Ranked by total amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Text(if (type == TransactionType.INCOME) "💰" else "🏪", fontSize = 22.sp)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            merchants.forEachIndexed { index, merchant ->
                val barFraction = if (maxAmount > 0) (merchant.total / maxAmount).toFloat() else 0f
                MerchantRow(
                    rank = index + 1,
                    merchant = merchant,
                    barFraction = barFraction,
                    accentColor = accentColor,
                    isLast = index == merchants.lastIndex
                )
                if (index < merchants.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    )
                }
            }
        }
    }
}

private data class MerchantStat(val name: String, val total: Double, val count: Int)

@Composable
private fun MerchantRow(
    rank: Int,
    merchant: MerchantStat,
    barFraction: Float,
    accentColor: Color,
    isLast: Boolean
) {
    // Animate bar width on first draw
    val animated = remember { Animatable(0f) }
    LaunchedEffect(barFraction) {
        animated.animateTo(barFraction, tween(500, easing = FastOutSlowInEasing))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rank badge
            Surface(
                shape = CircleShape,
                color = if (rank == 1) accentColor.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (rank == 1) accentColor
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    merchant.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${merchant.count} transaction${if (merchant.count != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            Text(
                PesaTimeConfiguration.formatCurrency(merchant.total),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }

        // Animated horizontal bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated.value)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.6f))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Largest Transactions — biggest individual hits in the period
// ─────────────────────────────────────────────────────────────────────────────

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM")

@Composable
fun LargestTransactionsCard(
    filteredTransactions: List<MpesaTransactions>,
    allCategories: List<ResolvedCategory>,
    type: TransactionType
) {
    val accentColor = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val top5 = remember(filteredTransactions) {
        filteredTransactions.sortedByDescending { it.amount }.take(5)
    }

    if (top5.isEmpty()) return

    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("LARGEST TRANSACTIONS")
                    Text(
                        "Biggest single ${type.name.lowercase()} events",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Text("🔍", fontSize = 22.sp)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            top5.forEachIndexed { index, tx ->
                val emoji = categoryMap[tx.category]?.emoji ?: "📦"
                val date = remember(tx.date) {
                    runCatching { LocalDate.parse(tx.date).format(DATE_FMT) }.getOrElse { tx.date }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tx.recipient,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        PesaTimeConfiguration.formatCurrency(tx.amount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }
                if (index < top5.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Spending by Day of Week — which days cost you most
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SpendingByDayOfWeekCard(
    filteredTransactions: List<MpesaTransactions>,
    type: TransactionType
) {
    val accentColor = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    // Map DayOfWeek ordinal (1=Mon…7=Sun) → total amount
    val dayTotals = remember(filteredTransactions) {
        val map = LinkedHashMap<DayOfWeek, Double>()
        DayOfWeek.values().forEach { map[it] = 0.0 }
        for (tx in filteredTransactions) {
            val dow = runCatching { LocalDate.parse(tx.date).dayOfWeek }.getOrNull() ?: continue
            map[dow] = (map[dow] ?: 0.0) + tx.amount
        }
        map
    }

    val maxVal = dayTotals.values.maxOrNull()?.takeIf { it > 0 } ?: return

    val peakDay = dayTotals.maxByOrNull { it.value }?.key

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("ACTIVITY BY DAY OF WEEK")
                    if (peakDay != null) {
                        val label = peakDay.name.lowercase()
                            .replaceFirstChar { it.uppercase() }
                        Text(
                            "Most active on ${label}s",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor.copy(alpha = 0.8f)
                        )
                    }
                }
                Text("📅", fontSize = 22.sp)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                DayOfWeek.values().forEach { dow ->
                    val amount = dayTotals[dow] ?: 0.0
                    val fraction = (amount / maxVal).toFloat()
                    val isPeak = dow == peakDay
                    DayBar(
                        dayLabel = dow.name.take(2).lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fraction = fraction,
                        amount = amount,
                        isPeak = isPeak,
                        accentColor = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DayBar(
    dayLabel: String,
    fraction: Float,
    amount: Double,
    isPeak: Boolean,
    accentColor: Color
) {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        animated.animateTo(fraction, tween(600, easing = FastOutSlowInEasing))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.width(36.dp)
    ) {
        if (isPeak && amount > 0) {
            Text(
                formatCompactAmount(amount),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        } else {
            Spacer(Modifier.height(12.dp))
        }
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(22.dp)
                    .fillMaxWidth()
                    .height((80 * animated.value).dp.coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        if (isPeak) accentColor
                        else accentColor.copy(alpha = 0.25f)
                    )
            )
        }
        Text(
            dayLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (isPeak) FontWeight.Black else FontWeight.Normal,
            color = if (isPeak) accentColor
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Transaction Activity Summary — frequency & patterns
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TransactionActivityCard(
    allTransactionsInPeriod: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val stats = remember(allTransactionsInPeriod, startDate, endDate) {
        val periodDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        var incomeCount = 0
        var expenseCount = 0
        val dayCountMap = HashMap<String, Int>()

        for (tx in allTransactionsInPeriod) {
            when (tx.type) {
                "income" -> incomeCount++
                "expense" -> expenseCount++
            }
            dayCountMap[tx.date] = (dayCountMap[tx.date] ?: 0) + 1
        }

        val busiestDate = dayCountMap.maxByOrNull { it.value }
        val busiestCount = busiestDate?.value ?: 0
        val busiestLabel = busiestDate?.key?.let {
            runCatching {
                LocalDate.parse(it).format(DateTimeFormatter.ofPattern("EEE dd MMM"))
            }.getOrElse { it }
        }

        ActivityStats(
            total = allTransactionsInPeriod.size,
            incomeCount = incomeCount,
            expenseCount = expenseCount,
            avgPerDay = allTransactionsInPeriod.size.toFloat() / periodDays.coerceAtLeast(1),
            busiestLabel = busiestLabel as String?,
            busiestCount = busiestCount,
            activeDays = dayCountMap.size,
            periodDays = periodDays
        )
    }

    if (stats.total == 0) return

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("TRANSACTION ACTIVITY")
                    Text(
                        "${stats.activeDays} active day${if (stats.activeDays != 1) "s" else ""} of ${stats.periodDays}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Text("📊", fontSize = 22.sp)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Income vs Expense count bar
            val incomeRatio = if (stats.total > 0) stats.incomeCount.toFloat() / stats.total else 0f
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "💰 ${stats.incomeCount} in",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        "${stats.expenseCount} out 💸",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(incomeRatio)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f))
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Stat grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ActivityMetric("TOTAL TXS", "${stats.total}")
                ActivityMetric("DAILY AVG", "%.1f".format(stats.avgPerDay))
                ActivityMetric(
                    "BUSIEST DAY",
                    if (stats.busiestLabel != null) "${stats.busiestCount} txs" else "–",
                    subtitle = stats.busiestLabel,
                    alignEnd = true
                )
            }
        }
    }
}

private data class ActivityStats(
    val total: Int,
    val incomeCount: Int,
    val expenseCount: Int,
    val avgPerDay: Float,
    val busiestLabel: String?,
    val busiestCount: Int,
    val activeDays: Int,
    val periodDays: Int
)

@Composable
private fun ActivityMetric(
    label: String,
    value: String,
    subtitle: String? = null,
    alignEnd: Boolean = false
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                maxLines = 1
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Balance Sparkline — running M-Pesa balance over the period
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BalanceSparklineCard(
    allTransactionsInPeriod: List<MpesaTransactions>
) {
    // Use transactions that have a non-zero balance reading, sorted by date+time
    val balancePoints = remember(allTransactionsInPeriod) {
        allTransactionsInPeriod
            .filter { it.balance > 0 }
            .sortedBy { it.timestamp }
            .map { it.balance.toFloat() }
    }

    if (balancePoints.size < 2) return

    val minBalance = balancePoints.min()
    val maxBalance = balancePoints.max()
    val startBalance = balancePoints.first()
    val endBalance = balancePoints.last()
    val delta = endBalance - startBalance
    val deltaPositive = delta >= 0

    val lineColor = if (deltaPositive)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val animated = remember { Animatable(0f) }
    LaunchedEffect(balancePoints) {
        animated.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("BALANCE TREND")
                    Text(
                        "M-Pesa balance across the period",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = lineColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        "${if (deltaPositive) "▲" else "▼"} ${
                            PesaTimeConfiguration.formatCurrency(kotlin.math.abs(delta).toDouble())
                        }",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "PERIOD START",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        PesaTimeConfiguration.formatCurrency(startBalance.toDouble()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "PERIOD END",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        PesaTimeConfiguration.formatCurrency(endBalance.toDouble()),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = lineColor
                    )
                }
            }

            // Sparkline canvas
            val progress = animated.value
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                val w = size.width
                val h = size.height
                val range = (maxBalance - minBalance).coerceAtLeast(1f)
                val n = balancePoints.size
                // How many points to draw based on animation progress
                val drawCount = (n * progress).roundToInt().coerceAtLeast(2)
                val points = balancePoints.take(drawCount)
                val path = Path()

                points.forEachIndexed { i, balance ->
                    val x = if (n > 1) i / (n - 1).toFloat() * w else w / 2f
                    val y = h - ((balance - minBalance) / range) * h
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // End dot
                if (points.size >= 2) {
                    val lastIdx = points.lastIndex
                    val lx = if (n > 1) lastIdx / (n - 1).toFloat() * w else w / 2f
                    val ly = h - ((points.last() - minBalance) / range) * h
                    drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(lx, ly))
                    drawCircle(
                        color = lineColor.copy(alpha = 0.2f),
                        radius = 9.dp.toPx(),
                        center = Offset(lx, ly)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 1.5.sp,
            brush = primaryGradient()
        ),
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}