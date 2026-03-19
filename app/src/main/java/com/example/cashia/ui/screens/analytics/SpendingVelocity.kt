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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TransactionType
import java.time.LocalDate
import java.time.temporal.ChronoUnit


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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("SPENDING VELOCITY")
                    Subtitles("Daily pace & period projection")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("⚡", fontSize = 22.sp)
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Subtitles("Day ${stats.daysElapsed} of ${stats.periodDays}")
                            val pace = when {
                                stats.spendRatio > stats.paceRatio + 0.12f -> "🔴 Spending Rate: High"
                                stats.spendRatio < stats.paceRatio - 0.12f -> "🟢 Spending Rate: Low"
                                else -> "🟡 Spending Rate: Typical"
                            }
                            Subtitles(pace)
                        }
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
                                    .background(primaryGradient())
                            )
                        }
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
                                brush = primaryGradient(),
                                label = "Time"
                            )
                            LegendDot(color = accentColor.copy(alpha = 0.75f), label = "Spend")
                        }
                    }
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
        Subtitles(label)
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}