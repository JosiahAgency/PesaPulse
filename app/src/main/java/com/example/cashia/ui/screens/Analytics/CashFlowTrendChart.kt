package com.example.cashia.ui.screens.Analytics

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.MpesaTransactions
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun CashFlowTrendChart(
    allTransactions: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val days = remember(startDate, endDate) {
        val list = mutableListOf<LocalDate>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            list.add(cursor)
            cursor = cursor.plusDays(1)
        }
        list
    }

    val incomeByDay = remember(allTransactions, startDate, endDate) {
        val startTs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTs =
            endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        allTransactions.filter { it.timestamp in startTs..endTs && it.type == "income" }.groupBy {
            LocalDate.ofInstant(
                Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()
            )
        }.mapValues { (_, txs) -> txs.sumOf { it.amount } }

    }

    val expenseByDay = remember(allTransactions, startDate, endDate) {
        val startTs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTs =
            endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        allTransactions.filter { it.timestamp in startTs..endTs && it.type == "expense" }.groupBy {
            LocalDate.ofInstant(
                Instant.ofEpochMilli(it.timestamp), ZoneId.systemDefault()
            )
        }.mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }

    val incomePoints = remember(days, incomeByDay) {
        var running = 0.0
        days.map { day -> running += (incomeByDay[day] ?: 0.0); running.toFloat() }
    }

    val expensePoints = remember(days, expenseByDay) {
        var running = 0.0
        days.map { day -> running += (expenseByDay[day] ?: 0.0); running.toFloat() }
    }

    val hasData = incomePoints.last() > 0f || expensePoints.last() > 0f

    if (!hasData) return

    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    val maxVal = remember(incomePoints, expensePoints) {
        maxOf(incomePoints.max(), expensePoints.max()) * 1.15f  // 15% headroom
    }

    // X-axis label candidates — show at most 5 evenly spaced dates
    val xLabels = remember(days) {
        if (days.size <= 5) days
        else {
            val step = (days.size - 1) / 4
            (0..4).map { days[it * step] }
        }
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Cashflow Trend",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Cumulative income vs. expenses",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = incomeColor, label = "Income")
                LegendDot(color = expenseColor, label = "Expenses")
            }
            Spacer(Modifier.height(16.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val w = size.width
                val h = size.height
                val padLeft = 8.dp.toPx()
                val padBottom = 24.dp.toPx()
                val chartW = w - padLeft
                val chartH = h - padBottom

                val n = days.size

                // Grid lines (4 horizontal)
                val gridCount = 4
                for (i in 0..gridCount) {
                    val y = chartH * (1f - i.toFloat() / gridCount)
                    drawLine(
                        color = gridColor,
                        start = Offset(padLeft, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                fun xOf(index: Int) =
                    padLeft + (index.toFloat() / (n - 1).coerceAtLeast(1)) * chartW

                fun yOf(value: Float) = chartH * (1f - (value / maxVal).coerceIn(0f, 1f))

                // Draw filled areas under each line for depth
                if (n > 1) {
                    // Income fill
                    android.graphics.Path().apply {
                        moveTo(xOf(0), yOf(incomePoints[0]))
                        for (i in 1 until n) lineTo(xOf(i), yOf(incomePoints[i]))
                        lineTo(xOf(n - 1), chartH)
                        lineTo(xOf(0), chartH)
                        close()
                    }
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(xOf(0), yOf(incomePoints[0]))
                            for (i in 1 until n) lineTo(xOf(i), yOf(incomePoints[i]))
                            lineTo(xOf(n - 1), chartH)
                            lineTo(xOf(0), chartH)
                            close()
                        },
                        brush = Brush.verticalGradient(
                            colors = listOf(incomeColor.copy(alpha = 0.18f), Color.Transparent),
                            startY = 0f,
                            endY = chartH
                        )
                    )

                    // Expense fill
                    drawPath(
                        path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(xOf(0), yOf(expensePoints[0]))
                            for (i in 1 until n) lineTo(xOf(i), yOf(expensePoints[i]))
                            lineTo(xOf(n - 1), chartH)
                            lineTo(xOf(0), chartH)
                            close()
                        },
                        brush = Brush.verticalGradient(
                            colors = listOf(expenseColor.copy(alpha = 0.18f), Color.Transparent),
                            startY = 0f,
                            endY = chartH
                        )
                    )

                    // Income line
                    for (i in 1 until n) {
                        drawLine(
                            color = incomeColor,
                            start = Offset(xOf(i - 1), yOf(incomePoints[i - 1])),
                            end = Offset(xOf(i), yOf(incomePoints[i])),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Expense line
                    for (i in 1 until n) {
                        drawLine(
                            color = expenseColor,
                            start = Offset(xOf(i - 1), yOf(expensePoints[i - 1])),
                            end = Offset(xOf(i), yOf(expensePoints[i])),
                            strokeWidth = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // End-point dots
                    drawCircle(
                        incomeColor,
                        radius = 4.dp.toPx(),
                        center = Offset(xOf(n - 1), yOf(incomePoints.last()))
                    )
                    drawCircle(
                        Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(xOf(n - 1), yOf(incomePoints.last()))
                    )
                    drawCircle(
                        expenseColor,
                        radius = 4.dp.toPx(),
                        center = Offset(xOf(n - 1), yOf(expensePoints.last()))
                    )
                    drawCircle(
                        Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(xOf(n - 1), yOf(expensePoints.last()))
                    )
                }
            }

            // ── X-axis labels ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                xLabels.forEach { date ->
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("dd MMM")),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        fontSize = 10.sp
                    )
                }
            }

            // ── Net cashflow summary pill ─────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            val net = incomePoints.lastOrNull() ?: 0f
            val spent = expensePoints.lastOrNull() ?: 0f
            val netValue = net - spent
            val netColor = if (netValue >= 0) incomeColor else expenseColor

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = netColor.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net for period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${if (netValue >= 0) "+" else ""}${
                            PesaTimeConfiguration.formatCurrency(
                                netValue.toDouble()
                            )
                        }",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = netColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}