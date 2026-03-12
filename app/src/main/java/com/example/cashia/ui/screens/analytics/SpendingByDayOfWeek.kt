package com.example.cashia.ui.screens.analytics

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.TransactionType
import com.example.cashia.utility.formatCompactAmount
import java.time.DayOfWeek
import java.time.LocalDate

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
        DayOfWeek.entries.forEach { map[it] = 0.0 }
        for (tx in filteredTransactions) {
            val dow = runCatching { LocalDate.parse(tx.date).dayOfWeek }.getOrNull() ?: continue
            map[dow] = (map[dow] ?: 0.0) + tx.amount
        }
        map
    }

    val maxVal = dayTotals.values.maxOrNull()?.takeIf { it > 0 } ?: return
    val peakDay = dayTotals.maxByOrNull { it.value }?.key

    val animated = remember { Animatable(0f) }
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(expanded) {
        if (expanded) {
            animated.snapTo(0f)
            animated.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }
    }

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
                    SectionLabel("ACTIVITY BY DAY OF WEEK")
                    if (peakDay != null) {
                        val label = peakDay.name.lowercase()
                            .replaceFirstChar { it.uppercase() }
                        Subtitles(
                            "Most active on ${label}s",
                            color = accentColor.copy(alpha = 0.8f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📅", fontSize = 22.sp)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        DayOfWeek.entries.forEach { dow ->
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