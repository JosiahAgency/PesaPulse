package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


@Composable
fun TransactionActivity(
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
        val busiestLabel = busiestDate?.key?.let { it ->
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

    val animated = remember { Animatable(0f) }
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(expanded) {
        if (expanded) {
            animated.snapTo(0f)
            animated.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("TRANSACTION ACTIVITY")
                    Subtitles(
                        "${stats.activeDays} Active Day${
                            if (stats.activeDays != 1) "s"
                            else ""
                        } of ${stats.periodDays}"
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📊", fontSize = 22.sp)
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded
                            .KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }
            }
            AnimatedVisibility(
                visible = !expanded,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                    val incomeRatio =
                        if (stats.total > 0) stats.incomeCount.toFloat() / stats.total else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Subtitles(
                                "💰 ${stats.incomeCount} IN",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Subtitles(
                                "${stats.expenseCount} OUT 💸",
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
