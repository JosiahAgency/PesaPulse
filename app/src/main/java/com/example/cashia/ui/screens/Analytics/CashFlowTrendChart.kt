package com.example.cashia.ui.screens.Analytics

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface


    val maxVal = remember(incomePoints, expensePoints) {
        maxOf(incomePoints.max(), expensePoints.max()) * 1.15f  // 15% headroom
    }

    // X-axis label candidates — show at most 5 evenly spaced dates
    val xLabels = remember(days) {
        if (days.size <= 5) days
        else {
            val step = (days.size - 1) / 4; (0..4).map { days[it * step] }
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(allTransactions) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    var crosshairX by remember { mutableFloatStateOf(-1f) }
    var selectedDayIndex by remember { mutableIntStateOf(-1) }

    // Store chart bounds for hit testing
    var chartLeft by remember { mutableFloatStateOf(0f) }
    var chartRight by remember { mutableFloatStateOf(0f) }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Cash Flow Trend",
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
            // ── Callout box ───────────────────────────────────────────────
            val selIncome = incomePoints.getOrNull(selectedDayIndex)
            val selExpense = expensePoints.getOrNull(selectedDayIndex)
            val selDate = days.getOrNull(selectedDayIndex)

            if (selectedDayIndex >= 0 && selDate != null && selIncome != null && selExpense != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selDate.format(DateTimeFormatter.ofPattern("dd MMM")),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Income",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = incomeColor.copy(alpha = 0.7f))
                                Text(
                                    PesaTimeConfiguration.formatCurrency(selIncome.toDouble()),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = incomeColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Expenses",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = expenseColor.copy(alpha = 0.7f))
                                Text(
                                    PesaTimeConfiguration.formatCurrency(selExpense.toDouble()),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = expenseColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val net = selIncome - selExpense
                                Text("Net",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = (if (net >= 0) incomeColor else expenseColor).copy(alpha = 0.7f))
                                Text(
                                    "${if (net >= 0) "+" else ""}${PesaTimeConfiguration.formatCurrency(net.toDouble())}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (net >= 0) incomeColor else expenseColor
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(52.dp))
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(days.size) {
                        detectTapGestures { offset ->
                            val n = days.size
                            if (n < 2) return@detectTapGestures
                            val chartW = chartRight - chartLeft
                            val rawIndex = ((offset.x - chartLeft) / chartW * (n - 1))
                                .toInt().coerceIn(0, n - 1)
                            selectedDayIndex = if (selectedDayIndex == rawIndex) -1 else rawIndex
                            crosshairX = if (selectedDayIndex >= 0)
                                chartLeft + (selectedDayIndex.toFloat() / (n - 1)) * chartW
                            else -1f
                        }
                    }
                    .pointerInput(days.size) {
                        detectDragGestures(
                            onDragEnd = { /* keep selection */ },
                            onDrag = { change, _ ->
                                val n = days.size
                                if (n < 2) return@detectDragGestures
                                val chartW = chartRight - chartLeft
                                val rawIndex = ((change.position.x - chartLeft) / chartW * (n - 1))
                                    .toInt().coerceIn(0, n - 1)
                                selectedDayIndex = rawIndex
                                crosshairX = chartLeft + (rawIndex.toFloat() / (n - 1)) * chartW
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val padLeft = 8.dp.toPx()
                val padBottom = 24.dp.toPx()
                val chartW = w - padLeft
                val chartH = h - padBottom

                chartLeft = padLeft
                chartRight = w

                val n = days.size

                fun xOf(i: Int) = padLeft + (i.toFloat() / (n - 1).coerceAtLeast(1)) * chartW
                fun yOf(v: Float) = chartH * (1f - (v / maxVal).coerceIn(0f, 1f))

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
                    val value = (maxVal * i / gridCount).toInt()
                    drawContext.canvas.nativeCanvas.drawText(
                        if (value >= 1000) "${value / 1000}k" else value.toString(),
                        2f,
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(
                                100, 128, 128, 128
                            )
                            textSize = 9.sp.toPx()
                        }
                    )
                }

                if (n > 1) {
                    val drawN = (n * progress.value).toInt().coerceAtLeast(2)

                    drawPath(
                        path = Path().apply {
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
                        path = Path().apply {
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
                    if (drawN >= n) {
                        listOf(
                            incomeColor to incomePoints.last(),
                            expenseColor to expensePoints.last()
                        ).forEach { (color, v) ->
                            drawCircle(color, 4.dp.toPx(), Offset(xOf(n - 1), yOf(v)))
                            drawCircle(Color.White, 2.dp.toPx(), Offset(xOf(n - 1), yOf(v)))
                        }
                    }
                    if (crosshairX >= 0 && selectedDayIndex in 0 until n) {
                        val cx = crosshairX
                        val incomeY = yOf(incomePoints[selectedDayIndex])
                        val expenseY = yOf(expensePoints[selectedDayIndex])

                        // Vertical dashed line
                        val dashLen = 6.dp.toPx()
                        val gapLen = 4.dp.toPx()
                        var y = 0f
                        while (y < chartH) {
                            drawLine(
                                onSurface.copy(alpha = 0.2f),
                                Offset(cx, y),
                                Offset(cx, (y + dashLen).coerceAtMost(chartH)),
                                1.5.dp.toPx()
                            )
                            y += dashLen + gapLen
                        }

                        // Income intercept dot
                        drawCircle(surfaceColor, 7.dp.toPx(), Offset(cx, incomeY))
                        drawCircle(incomeColor, 5.dp.toPx(), Offset(cx, incomeY))
                        drawCircle(Color.White, 2.5.dp.toPx(), Offset(cx, incomeY))

                        // Expense intercept dot
                        drawCircle(surfaceColor, 7.dp.toPx(), Offset(cx, expenseY))
                        drawCircle(expenseColor, 5.dp.toPx(), Offset(cx, expenseY))
                        drawCircle(Color.White, 2.5.dp.toPx(), Offset(cx, expenseY))

                        // Floating value label on income line
                        val incomeLabelY = (incomeY - 18.dp.toPx()).coerceAtLeast(12.dp.toPx())
                        val label = PesaTimeConfiguration.formatCurrency(
                            incomePoints[selectedDayIndex].toDouble())
                        val paint = android.graphics.Paint().apply {
                            color = incomeColor.toArgb()
                            textSize = 9.sp.toPx()
                            isFakeBoldText = true
                        }
                        val labelWidth = paint.measureText(label)
                        val lx = (cx - labelWidth / 2).coerceIn(padLeft, w - labelWidth)
                        drawContext.canvas.nativeCanvas.drawText(label, lx, incomeLabelY, paint)

                        // Floating value label on expense line
                        val expenseLabelY = (expenseY + 18.dp.toPx()).coerceAtMost(chartH - 4.dp.toPx())
                        val eLabel = PesaTimeConfiguration.formatCurrency(
                            expensePoints[selectedDayIndex].toDouble())
                        val ePaint = android.graphics.Paint().apply {
                            color = expenseColor.toArgb()
                            textSize = 9.sp.toPx()
                            isFakeBoldText = true
                        }
                        val eLabelWidth = ePaint.measureText(eLabel)
                        val elx = (cx - eLabelWidth / 2).coerceIn(padLeft, w - eLabelWidth)
                        drawContext.canvas.nativeCanvas.drawText(eLabel, elx, expenseLabelY, ePaint)
                    }
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
            val net = (incomePoints.lastOrNull() ?: 0f) - (expensePoints.lastOrNull() ?: 0f)
            val netColor = if (net >= 0) incomeColor else expenseColor

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
                        text = "${if (net >= 0) "+" else ""}${
                            PesaTimeConfiguration.formatCurrency(
                                net.toDouble()
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