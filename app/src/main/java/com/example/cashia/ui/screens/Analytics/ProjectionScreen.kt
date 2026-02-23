package com.example.cashia.ui.screens.Analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashia.Utility.FinanceProjectionEngine
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.TransactionsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionScreen() {
    val context = LocalContext.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )

    val state = viewModel.state
    val transactions = state.allTransactions

    // Use last 90 days of data to build the projection
    val recentTransactions = remember(transactions) {
        val cutoff = LocalDate.now().minusDays(90)
        transactions.filter {
            try {
                LocalDate.parse(it.date) >= cutoff
            } catch (e: Exception) {
                false
            }
        }
    }

    val projection = remember(recentTransactions, state.latestBalance) {
        FinanceProjectionEngine.project(
            transactions = recentTransactions,
            currentBalance = state.latestBalance,
            daysForward = 30
        )
    }

    val colorScheme = MaterialTheme.colorScheme
    if (colorScheme.background.luminance() > 0.5f) 0.93f else 0.80f

    Scaffold(containerColor = Color.Transparent) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Header ──────────────────────────────────────────────────────
            item {
                ProjectionHeader()
            }

            // ── Predicted Balance Hero ───────────────────────────────────────
            item {
                PredictedBalanceCard(
                    currentBalance = state.latestBalance,
                    projection = projection
                )
            }

            // ── Trend Chart ──────────────────────────────────────────────────
            item {
                ProjectionTrendChart(
                    projection = projection,
                    currentBalance = state.latestBalance
                )
            }

            // ── Income / Expense / Net row ───────────────────────────────────
            item {
                ProjectionTripleRow(projection = projection)
            }

            // ── Confidence & Insights ────────────────────────────────────────
            item {
                ProjectionInsightsCard(
                    projection = projection,
                    transactionCount = recentTransactions.size
                )
            }

            // ── Daily Breakdown peek ─────────────────────────────────────────
            item {
                DailyTrendMiniList(projection = projection)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectionHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "30-Day Outlook",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Based on your last 90 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.AutoGraph,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(10.dp)
                    .size(22.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Balance Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PredictedBalanceCard(
    currentBalance: Double,
    projection: FinanceProjection
) {
    val netPositive = projection.predictedNet >= 0
    val balanceColor = if (netPositive)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val changePercent = if (currentBalance > 0)
        ((projection.predictedBalance - currentBalance) / currentBalance * 100)
    else 0.0

    // Animate the balance number
    val animatedBalance = remember { Animatable(0f) }
    LaunchedEffect(projection.predictedBalance) {
        animatedBalance.animateTo(
            targetValue = projection.predictedBalance.toFloat(),
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Predicted Balance in 30 days",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = PesaTimeConfiguration.formatCurrency(animatedBalance.value.toDouble()),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = balanceColor
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (netPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = balanceColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${if (changePercent >= 0) "+" else ""}${
                        String.format(
                            "%.1f",
                            changePercent
                        )
                    }% from current balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(
                    label = "Current",
                    value = PesaTimeConfiguration.formatCurrency(currentBalance),
                    color = MaterialTheme.colorScheme.primary
                )
                VerticalDivider(
                    modifier = Modifier.height(36.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                MiniStat(
                    label = "Net Change",
                    value = "${if (projection.predictedNet >= 0) "+" else ""}${
                        PesaTimeConfiguration.formatCurrency(
                            projection.predictedNet
                        )
                    }",
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Projection Trend Chart
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectionTrendChart(
    projection: FinanceProjection,
    currentBalance: Double
) {
    if (projection.dailyTrend.isEmpty()) return

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    MaterialTheme.colorScheme.surfaceVariant

    // Animate the draw progress
    val progress = remember { Animatable(0f) }
    LaunchedEffect(projection) {
        progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }

    val allValues = listOf(currentBalance) + projection.dailyTrend.map { it.second }
    val minVal = allValues.minOrNull() ?: 0.0
    val maxVal = allValues.maxOrNull() ?: 1.0
    val range = if (maxVal != minVal) maxVal - minVal else 1.0

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Balance Trajectory",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Projected path over next 30 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val w = size.width
                val h = size.height
                val points = allValues.mapIndexed { i, v ->
                    val x = i / (allValues.size - 1).toFloat() * w
                    val y = h - ((v - minVal) / range * h).toFloat()
                    Offset(x, y.coerceIn(0f, h))
                }

                val drawCount = (points.size * progress.value).toInt().coerceAtLeast(2)
                val visiblePoints = points.take(drawCount)

                // Gradient fill under curve
                if (visiblePoints.size >= 2) {
                    val pathFill = Path().apply {
                        moveTo(visiblePoints.first().x, h)
                        visiblePoints.forEach { lineTo(it.x, it.y) }
                        lineTo(visiblePoints.last().x, h)
                        close()
                    }
                    drawPath(
                        pathFill,
                        brush = Brush.verticalGradient(
                            colors = listOf(primary.copy(alpha = 0.25f), Color.Transparent),
                            startY = 0f, endY = h
                        )
                    )

                    // Line
                    val linePath = Path().apply {
                        moveTo(visiblePoints.first().x, visiblePoints.first().y)
                        visiblePoints.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        linePath,
                        color = primary,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Zero baseline marker if balance goes negative
                if (minVal < 0) {
                    val zeroY = h - ((-minVal) / range * h).toFloat()
                    drawLine(
                        color = error.copy(alpha = 0.4f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                // Today dot
                if (visiblePoints.isNotEmpty()) {
                    drawCircle(tertiary, radius = 6.dp.toPx(), center = points.first())
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = points.first())
                }

                // End dot
                if (drawCount >= points.size) {
                    val last = points.last()
                    drawCircle(primary, radius = 6.dp.toPx(), center = last)
                    drawCircle(Color.White, radius = 3.dp.toPx(), center = last)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = LocalDate.now().plusDays(30)
                        .format(DateTimeFormatter.ofPattern("dd MMM")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Triple Row: Income / Expenses / Net
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectionTripleRow(projection: FinanceProjection) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectionStatCard(
            modifier = Modifier.weight(1f),
            label = "Income",
            value = PesaTimeConfiguration.formatCurrency(projection.predictedIncome),
            icon = Icons.Default.ArrowDownward,
            color = MaterialTheme.colorScheme.tertiary
        )
        ProjectionStatCard(
            modifier = Modifier.weight(1f),
            label = "Expenses",
            value = PesaTimeConfiguration.formatCurrency(projection.predictedExpenses),
            icon = Icons.Default.ArrowUpward,
            color = MaterialTheme.colorScheme.error
        )
        ProjectionStatCard(
            modifier = Modifier.weight(1f),
            label = "Net",
            value = "${if (projection.predictedNet >= 0) "+" else ""}${
                PesaTimeConfiguration.formatCurrency(
                    abs(projection.predictedNet)
                )
            }",
            icon = if (projection.predictedNet >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
            color = if (projection.predictedNet >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ProjectionStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    GlassySurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Insights Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectionInsightsCard(
    projection: FinanceProjection,
    transactionCount: Int
) {
    val savingsRate = if (projection.predictedIncome > 0)
        (projection.predictedNet / projection.predictedIncome * 100)
    else 0.0

    val dailyAvgExpense = projection.predictedExpenses / 30.0

    val insights = buildList {
        add(
            Triple(
                Icons.Default.Savings,
                "Savings Rate",
                "${String.format("%.1f", savingsRate)}% of projected income"
            )
        )
        add(
            Triple(
                Icons.Default.CalendarToday,
                "Daily Avg Spend",
                PesaTimeConfiguration.formatCurrency(dailyAvgExpense)
            )
        )
        add(
            Triple(
                Icons.Default.DataUsage,
                "Data Points",
                "$transactionCount transactions analyzed"
            )
        )
        if (projection.predictedBalance < 0) {
            add(
                Triple(
                    Icons.Default.Warning,
                    "Alert",
                    "Balance may go negative — reduce spending"
                )
            )
        }
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Projection Insights",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            insights.forEachIndexed { i, (icon, label, desc) ->
                if (i > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
                }
                val isAlert = label == "Alert"
                val tint =
                    if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = tint.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAlert) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Daily Trend Mini List (first 7 days)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyTrendMiniList(projection: FinanceProjection) {
    if (projection.dailyTrend.isEmpty()) return

    val preview = projection.dailyTrend.take(7)
    val maxAbs = preview.maxOfOrNull { abs(it.second) } ?: 1.0

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Next 7 Days",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            preview.forEachIndexed { i, (date, balance) ->
                if (i > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
                }
                val positive = balance >= 0
                val barColor = if (positive) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error
                val barFraction = (abs(balance) / maxAbs).toFloat().coerceIn(0.02f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(100.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor.copy(alpha = 0.7f))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = PesaTimeConfiguration.formatCurrency(balance),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}