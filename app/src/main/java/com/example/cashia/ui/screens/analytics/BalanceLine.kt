package com.example.cashia.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration
import kotlin.math.roundToInt


@Composable
fun BalanceSparklineCard(
    allTransactionsInPeriod: List<MpesaTransactions>
) {
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
    var expanded by remember { mutableStateOf(true) }

    LaunchedEffect(balancePoints, expanded) {
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
                    SectionLabel("BALANCE TREND")
                    Subtitles("M-Pesa balance across the period")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = lineColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "${if (deltaPositive) "▲" else "▼"} ${
                                PesaTimeConfiguration.formatCurrency(
                                    kotlin.math.abs(delta).toDouble()
                                )
                            }",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = lineColor
                        )
                    }
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
                        Column {
                            Subtitles("PERIOD START")
                            Text(
                                PesaTimeConfiguration.formatCurrency(startBalance.toDouble()),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Subtitles("PERIOD END")
                            Text(
                                PesaTimeConfiguration.formatCurrency(endBalance.toDouble()),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = lineColor
                            )
                        }
                    }
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
                        if (points.size >= 2) {
                            val lastIdx = points.lastIndex
                            val lx = if (n > 1) lastIdx / (n - 1).toFloat() * w else w / 2f
                            val ly = h - ((points.last() - minBalance) / range) * h
                            drawCircle(
                                color = lineColor,
                                radius = 5.dp.toPx(),
                                center = Offset(lx, ly)
                            )
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
    }
}