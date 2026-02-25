package com.example.cashia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashia.Utility.FinanceProjectionEngine
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.TransactionsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionScreen() {
    val context = LocalContext.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    val haptic = LocalHapticFeedback.current
    val state = viewModel.state
    val transactions = state.allTransactions

    var lookbackDays by remember { mutableFloatStateOf(90f) }
    var horizonDays by remember { mutableIntStateOf(30) }
    var balanceOverride by remember { mutableStateOf("") }
    var configExpanded by remember { mutableStateOf(false) }

    val effectiveBalance = remember(balanceOverride, state.latestBalance) {
        val parsed = balanceOverride.toDoubleOrNull()
        when {
            parsed == null -> state.latestBalance
            parsed < 0 -> state.latestBalance
            parsed > state.latestBalance * 10
                    && state.latestBalance > 0 -> state.latestBalance

            else -> parsed
        }
    }

    val earliestDate = remember(transactions) {
        transactions.mapNotNull {
            try {
                LocalDate.parse(it.date)
            } catch (e: Exception) {
                null
            }
        }.minOrNull() ?: LocalDate.now().minusDays(365)
    }
    val maxLookback = remember(earliestDate) {
        ChronoUnit.DAYS.between(earliestDate, LocalDate.now()).toInt().coerceIn(30, 365)
    }


    val recentTransactions = remember(transactions, lookbackDays) {
        val cutoff = LocalDate.now().minusDays(lookbackDays.toLong())
        transactions.filter {
            try {
                LocalDate.parse(it.date) >= cutoff
            } catch (e: Exception) {
                false
            }
        }
    }

    val projection =
        remember(recentTransactions, effectiveBalance, horizonDays, state.latestBalanceDateParsed) {
            FinanceProjectionEngine.project(
                transactions = recentTransactions,
                currentBalance = effectiveBalance,
                balanceAsOfDate = if (balanceOverride.toDoubleOrNull() != null) LocalDate.now() else state.latestBalanceDateParsed,
                horizonDays
            )
        }

    Scaffold(containerColor = Color.Transparent) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Header ──────────────────────────────────────────────────────
            item {
                ProjectionConfigCard(
                    isExpanded = configExpanded,
                    onToggle = { PesaActions.playClick(haptic); configExpanded = !configExpanded },
                    lookbackDays = lookbackDays,
                    maxLookback = maxLookback,
                    onLookbackChange = { lookbackDays = it },
                    horizonDays = horizonDays,
                    onHorizonChange = { horizonDays = it },
                    balanceOverride = balanceOverride,
                    onBalanceOverrideChange = { balanceOverride = it },
                    autoBalance = state.latestBalance,
                    autoBalanceDate = state.latestBalanceDate,
                    haptic = haptic
                )
            }

            item {
                ProjectionConfigSummary(
                    lookbackDays = lookbackDays.toInt(),
                    horizonDays = horizonDays,
                    isBalanceOverridden = balanceOverride.toDoubleOrNull() != null,
                    dataPointCount = recentTransactions.size
                )
            }

            // ── Predicted Balance Hero ───────────────────────────────────────
            item {
                PredictedBalanceCard(
                    currentBalance = effectiveBalance,
                    horizonDays,
                    projection = projection
                )
            }

            // ── Trend Chart ──────────────────────────────────────────────────
            item {
                ProjectionTrendChart(
                    projection = projection,
                    horizonDays,
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
                    transactionCount = recentTransactions.size,
                    horizonDays
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
private fun ProjectionConfigCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    lookbackDays: Float,
    maxLookback: Int,
    onLookbackChange: (Float) -> Unit,
    horizonDays: Int,
    onHorizonChange: (Int) -> Unit,
    balanceOverride: String,
    onBalanceOverrideChange: (String) -> Unit,
    autoBalance: Double,
    autoBalanceDate: String,
    haptic: HapticFeedback
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f

    val overrideValue = balanceOverride.toDoubleOrNull()
    val overrideIsNegative = overrideValue != null && overrideValue < 0
    val overrideIsUnreasonable = overrideValue != null &&
            autoBalance > 0 && overrideValue > autoBalance * 10  // 10x sanity check


    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Header — always visible ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Projection Basis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (isExpanded) "Tap to collapse"
                            else "Tap to configure",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp
                    else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // ── Collapsible body ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(250)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp, bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))

                    // ── Lookback slider ───────────────────────────────────────
                    ConfigSection(
                        label = "DATA SOURCE",
                        sublabel = "Analyzing transactions from ${lookbackDays.toInt()} days ago • ${
                            LocalDate.now().minusDays(lookbackDays.toLong())
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        } → Today\n" +
                                "More days provide a stable trend; fewer days prioritize recent spending."
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Slider(
                                value = lookbackDays,
                                onValueChange = {
                                    onLookbackChange(it)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                valueRange = 7f..maxLookback.toFloat(),
                                steps = 0,   // continuous
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = colorScheme.primary,
                                    activeTrackColor = colorScheme.primary,
                                    inactiveTrackColor = colorScheme.primary.copy(alpha = 0.2f)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "7 days",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                                Text(
                                    text = "${maxLookback}d (all data)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }

                    // ── Horizon chips ─────────────────────────────────────────
                    ConfigSection(
                        label = "FORECAST WINDOW",
                        sublabel = "Projecting balance through ${
                            LocalDate.now().plusDays(horizonDays.toLong())
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        } (${horizonDays} days)\n" +
                                "Short-term forecasts are more reliable. Accuracy peaks within 30 days."
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(7 to "7d", 14 to "14d", 30 to "30d", 60 to "60d")
                                .forEach { (days, label) ->
                                    ConfigChip(
                                        label = label,
                                        selected = horizonDays == days,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            PesaActions.playClick(haptic)
                                            onHorizonChange(days)
                                        }
                                    )
                                }
                        }
                    }

                    // ── Balance override ──────────────────────────────────────
                    ConfigSection(
                        label = "STARTING BALANCE",
                        sublabel = buildString {
                            append("Detected from your M-Pesa SMS")
                            if (autoBalanceDate.isNotEmpty()) append(" on $autoBalanceDate")
                            append(". Manually adjust if your current balance is different.")
                        }
                    ) {
                        OutlinedTextField(
                            value = balanceOverride,
                            onValueChange = onBalanceOverrideChange,
                            placeholder = {
                                Text(
                                    text = autoBalance.toLong().toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            },
                            prefix = {
                                Text(
                                    "KES ",
                                    color = if (overrideIsNegative) colorScheme.error else colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingIcon = {
                                if (balanceOverride.isNotEmpty()) {
                                    IconButton(onClick = { onBalanceOverrideChange("") }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp),
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            isError = overrideIsNegative || overrideIsUnreasonable,
                            supportingText = when {
                                overrideIsNegative -> {
                                    {
                                        Text(
                                            "Balance can't be negative", color = colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                overrideIsUnreasonable -> {
                                    {
                                        Text(
                                            "Seems unusually high — double check",
                                            color = colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                overrideValue != null -> {
                                    {
                                        Text(
                                            "Using custom balance instead of auto-detected value",
                                            color = colorScheme.primary.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                else -> null
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (overrideIsNegative || overrideIsUnreasonable)
                                    colorScheme.error else colorScheme.primary,
                                unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f),
                                focusedContainerColor = colorScheme.primary.copy(alpha = 0.04f),
                                unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                errorBorderColor = colorScheme.error,
                                errorContainerColor = colorScheme.error.copy(alpha = 0.04f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectionConfigSummary(
    lookbackDays: Int,
    horizonDays: Int,
    isBalanceOverridden: Boolean,
    dataPointCount: Int
) {
    MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill(
            modifier = Modifier.weight(1f),
            label = "Last ${lookbackDays}d",
            icon = Icons.Default.History,
            isCustom = lookbackDays != 90
        )
        SummaryPill(
            modifier = Modifier.weight(1f),
            label = "${horizonDays}d forecast",
            icon = Icons.Default.AutoGraph,
            isCustom = horizonDays != 30
        )
        SummaryPill(
            modifier = Modifier.weight(1f),
            label = "$dataPointCount txns",
            icon = Icons.Default.DataUsage,
            isCustom = isBalanceOverridden,
            overrideLabel = if (isBalanceOverridden) "Custom bal." else null
        )
    }
}

@Composable
private fun SummaryPill(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    isCustom: Boolean,
    overrideLabel: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val color = if (isCustom) colorScheme.primary
    else colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = color.copy(alpha = if (isCustom) 0.08f else 0.04f),
        border = BorderStroke(
            0.5.dp,
            color.copy(alpha = if (isCustom) 0.25f else 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(
                text = overrideLabel ?: label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConfigSection(
    label: String,
    sublabel: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
        content()
    }
}

@Composable
private fun ConfigChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50.dp),
        color = if (selected) colorScheme.primary
        else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) null
        else BorderStroke(0.5.dp, colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 9.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White
            else colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Balance Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PredictedBalanceCard(
    currentBalance: Double,
    horizonDays: Int,
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

    _root_ide_package_.com.example.cashia.ui.screens.Analytics.GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Predicted Balance in $horizonDays days",
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
    horizonDays: Int,
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

    _root_ide_package_.com.example.cashia.ui.screens.Analytics.GlassySurface(modifier = Modifier.fillMaxWidth()) {
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
                    text = LocalDate.now().plusDays(horizonDays.toLong())
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
    icon: ImageVector,
    color: Color
) {
    _root_ide_package_.com.example.cashia.ui.screens.Analytics.GlassySurface(modifier = modifier) {
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
    transactionCount: Int,
    horizonDays: Int
) {
    val savingsRate = if (projection.predictedIncome > 0)
        (projection.predictedNet / projection.predictedIncome * 100)
    else 0.0

    val dailyAvgExpense = projection.predictedExpenses / horizonDays.toDouble()

    buildList {
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

    _root_ide_package_.com.example.cashia.ui.screens.Analytics.GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Projection Insights",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Text(
                    text = "Based on a blend of your long-term spending trend (40%) " +
                            "and recent ${minOf(14, transactionCount)}-day activity (60%). " +
                            "Projected daily net: ${
                                if (projection.projectedDailyNet >= 0) "+" else ""
                            }${PesaTimeConfiguration.formatCurrency(projection.projectedDailyNet)}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 16.sp
                )
            }

            // ── Data quality warning — only shown if there's a concern ────────
            if (projection.dataQualityNote.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.06f),
                    border = BorderStroke(0.5.dp, Color.Red.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 1.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = projection.dataQualityNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(Modifier.height(12.dp))

            // ── Stats ─────────────────────────────────────────────────────────
            val insights = buildList {
                add(
                    Triple(
                        Icons.Default.Savings, "Savings Rate",
                        "${String.format("%.1f", savingsRate)}% of projected income"
                    )
                )
                add(
                    Triple(
                        Icons.Default.CalendarToday, "Daily Avg Spend",
                        PesaTimeConfiguration.formatCurrency(dailyAvgExpense)
                    )
                )
                add(
                    Triple(
                        Icons.Default.DataUsage, "Data Points",
                        "$transactionCount transactions across lookback window"
                    )
                )
                if (projection.predictedBalance < 0) {
                    add(
                        Triple(
                            Icons.Default.Warning, "Alert",
                            "Balance may go negative — reduce spending"
                        )
                    )
                }
            }

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

    _root_ide_package_.com.example.cashia.ui.screens.Analytics.GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Next 7 Days — Projected Balance",
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