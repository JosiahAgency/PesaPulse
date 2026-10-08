package com.example.cashia.ui.screens.analytics

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Back face of the flippable cash-flow card: cumulative income/expense trend for the
 * current calendar month overlaid against the same trend for the previous month, so a
 * user can see at a glance how this month is pacing.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun MonthComparisonBody(transactions: List<MpesaTransactions>) {
    val currentMonth = remember { YearMonth.now() }
    val lastMonth = remember(currentMonth) { currentMonth.minusMonths(1) }
    val dayCount = remember(currentMonth, lastMonth) {
        maxOf(currentMonth.lengthOfMonth(), lastMonth.lengthOfMonth())
    }

    data class MonthSeries(
        val income: List<Float>,
        val expense: List<Float>,
        val totalIncome: Double,
        val totalExpense: Double
    )

    fun buildSeries(month: YearMonth): MonthSeries {
        val incomeByDay = DoubleArray(month.lengthOfMonth())
        val expenseByDay = DoubleArray(month.lengthOfMonth())
        for (tx in transactions) {
            val date = Instant.ofEpochMilli(tx.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            if (YearMonth.from(date) != month) continue
            val idx = date.dayOfMonth - 1
            when (tx.type) {
                "income" -> incomeByDay[idx] += tx.amount
                "expense" -> expenseByDay[idx] += tx.amount
            }
        }
        var runningIncome = 0.0
        var runningExpense = 0.0
        val income = incomeByDay.map { runningIncome += it; runningIncome.toFloat() }
        val expense = expenseByDay.map { runningExpense += it; runningExpense.toFloat() }
        return MonthSeries(income, expense, runningIncome, runningExpense)
    }

    val thisMonthSeries = remember(transactions, currentMonth) { buildSeries(currentMonth) }
    val lastMonthSeries = remember(transactions, lastMonth) { buildSeries(lastMonth) }

    val hasData = thisMonthSeries.totalIncome > 0 || thisMonthSeries.totalExpense > 0 ||
        lastMonthSeries.totalIncome > 0 || lastMonthSeries.totalExpense > 0
    if (!hasData) {
        Subtitles("No transactions in the last two months yet")
        return
    }

    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    val maxVal = remember(thisMonthSeries, lastMonthSeries) {
        val peak = maxOf(
            thisMonthSeries.income.maxOrNull() ?: 0f,
            thisMonthSeries.expense.maxOrNull() ?: 0f,
            lastMonthSeries.income.maxOrNull() ?: 0f,
            lastMonthSeries.expense.maxOrNull() ?: 0f
        )
        (peak * 1.15f).coerceAtLeast(1f)
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(transactions) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    var selectedDayIndex by remember { mutableIntStateOf(-1) }
    var crosshairX by remember { mutableFloatStateOf(-1f) }
    var chartLeft by remember { mutableFloatStateOf(0f) }
    var chartRight by remember { mutableFloatStateOf(0f) }

    val chartDescription = remember(thisMonthSeries, lastMonthSeries) {
        "Monthly comparison chart. This month income " +
            "${PesaTimeConfiguration.formatCurrency(thisMonthSeries.totalIncome)}, expenses " +
            "${PesaTimeConfiguration.formatCurrency(thisMonthSeries.totalExpense)}. Last month income " +
            "${PesaTimeConfiguration.formatCurrency(lastMonthSeries.totalIncome)}, expenses " +
            "${PesaTimeConfiguration.formatCurrency(lastMonthSeries.totalExpense)}."
    }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Spacer(Modifier.height(8.dp))
        Subtitles("This month vs. last month, cumulative")
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(color = incomeColor, label = "Income")
            LegendDot(color = expenseColor, label = "Expenses")
        }
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashLegend(label = "This month (solid)")
            DashLegend(label = "Last month (dashed)", dashed = true)
        }
        Spacer(Modifier.height(10.dp))

        val selThisIncome = thisMonthSeries.income.getOrNull(selectedDayIndex)
        val selThisExpense = thisMonthSeries.expense.getOrNull(selectedDayIndex)
        val selLastIncome = lastMonthSeries.income.getOrNull(selectedDayIndex)
        val selLastExpense = lastMonthSeries.expense.getOrNull(selectedDayIndex)

        if (selectedDayIndex >= 0 && selThisIncome != null && selThisExpense != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Subtitles("Day ${selectedDayIndex + 1}")
                    Spacer(Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DayStat("This month", incomeColor, selThisIncome, expenseColor, selThisExpense)
                        DayStat(
                            "Last month", incomeColor, selLastIncome ?: 0f,
                            expenseColor, selLastExpense ?: 0f
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        } else {
            Spacer(Modifier.height(64.dp))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .semantics { contentDescription = chartDescription }
                .pointerInput(dayCount) {
                    detectTapGestures { offset ->
                        val n = dayCount
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
                .pointerInput(dayCount) {
                    detectDragGestures(
                        onDragEnd = { /* keep selection */ },
                        onDrag = { change, _ ->
                            val n = dayCount
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

            val n = dayCount

            fun xOf(i: Int) = padLeft + (i.toFloat() / (n - 1).coerceAtLeast(1)) * chartW
            fun yOf(v: Float) = chartH * (1f - (v / maxVal).coerceIn(0f, 1f))

            val gridTextSize = 9.sp.toPx()
            val gridPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(100, 128, 128, 128)
                textSize = gridTextSize
            }

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
                    gridPaint
                )
            }

            fun drawSeries(points: List<Float>, color: Color, dashed: Boolean) {
                if (points.size < 2) return
                val lineStroke = 2.5.dp.toPx()
                val stroke = if (dashed) Stroke(
                    width = lineStroke,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                ) else Stroke(width = lineStroke, cap = StrokeCap.Round)
                for (i in 1 until points.size) {
                    drawLine(
                        color = color,
                        start = Offset(xOf(i - 1), yOf(points[i - 1])),
                        end = Offset(xOf(i), yOf(points[i])),
                        strokeWidth = stroke.width,
                        cap = StrokeCap.Round,
                        pathEffect = stroke.pathEffect
                    )
                }
            }

            drawSeries(lastMonthSeries.income, incomeColor.copy(alpha = 0.55f), dashed = true)
            drawSeries(lastMonthSeries.expense, expenseColor.copy(alpha = 0.55f), dashed = true)
            drawSeries(thisMonthSeries.income, incomeColor, dashed = false)
            drawSeries(thisMonthSeries.expense, expenseColor, dashed = false)

            if (crosshairX >= 0 && selectedDayIndex in 0 until n) {
                val cx = crosshairX
                val dashLen = 6.dp.toPx()
                val gapLen = 4.dp.toPx()
                val dashStroke = 1.5.dp.toPx()
                val dashColor = Color.Gray.copy(alpha = 0.2f)
                var y = 0f
                while (y < chartH) {
                    drawLine(
                        dashColor,
                        Offset(cx, y),
                        Offset(cx, (y + dashLen).coerceAtMost(chartH)),
                        dashStroke
                    )
                    y += dashLen + gapLen
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
            listOf(1, dayCount / 2, dayCount).distinct().forEach { day ->
                Text(
                    text = "Day $day",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        MonthDeltaCard(
            label = "Income",
            color = incomeColor,
            thisMonth = thisMonthSeries.totalIncome,
            lastMonth = lastMonthSeries.totalIncome
        )
        Spacer(Modifier.height(8.dp))
        MonthDeltaCard(
            label = "Expenses",
            color = expenseColor,
            thisMonth = thisMonthSeries.totalExpense,
            lastMonth = lastMonthSeries.totalExpense
        )
    }
}

@Composable
private fun DashLegend(label: String, dashed: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DayStat(
    title: String,
    incomeColor: Color,
    income: Float,
    expenseColor: Color,
    expense: Float
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            PesaTimeConfiguration.formatCurrency(income.toDouble()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = incomeColor
        )
        Text(
            PesaTimeConfiguration.formatCurrency(expense.toDouble()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = expenseColor
        )
    }
}

@Composable
private fun MonthDeltaCard(label: String, color: Color, thisMonth: Double, lastMonth: Double) {
    val delta = if (lastMonth != 0.0) ((thisMonth - lastMonth) / lastMonth) * 100 else null
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Subtitles("$label This Month")
                Text(
                    PesaTimeConfiguration.formatCurrency(thisMonth),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Subtitles("vs Last Month")
                Text(
                    text = delta?.let { "${if (it >= 0) "+" else ""}${"%.0f".format(it)}%" } ?: "—",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
        }
    }
}
