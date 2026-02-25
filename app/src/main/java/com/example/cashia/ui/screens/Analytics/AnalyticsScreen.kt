package com.example.cashia.ui.screens.Analytics

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TimeScope
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    val hapticFeedback = LocalHapticFeedback.current
    val state = viewModel.state
    val transactions = viewModel.state.allTransactions

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedScope by remember { mutableStateOf(TimeScope.MONTH) }
    val today = LocalDate.now()
    var startDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(selectedScope) {
        if (selectedScope != TimeScope.CUSTOM) {
            val (start, end) = when (selectedScope) {
                TimeScope.DAY -> today to today
                TimeScope.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) to
                        today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

                TimeScope.MONTH -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
                else -> startDate to endDate
            }
            startDate = start
            endDate = end
        }
    }

    val startTs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endTs =
        endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val filteredTransactions = remember(transactions, selectedType, startTs, endTs) {
        transactions.filter { tx ->
            tx.timestamp in startTs..endTs && tx.type == selectedType.name.lowercase()
        }
    }

    // Human-readable period label displayed on every stat so there's no ambiguity
    val periodLabel = remember(startDate, endDate, selectedScope) {
        when (selectedScope) {
            TimeScope.DAY -> today.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
            TimeScope.WEEK -> "${startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                endDate.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                )
            }"

            TimeScope.MONTH -> today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimeScope.CUSTOM -> "${startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                endDate.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                )
            }"

            else -> ""
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f
    colorScheme.surface.copy(alpha = glassAlpha)
    colorScheme.onSurface.copy(alpha = 0.12f)

    Scaffold(containerColor = Color.Transparent) { _ ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refreshFromSms() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PeriodSelectorCard(
                        hapticFeedback,
                        selectedScope,
                        startDate = startDate,
                        endDate = endDate,
                        onScopeChange = { selectedScope = it },
                        onStartDateSelected = {
                            startDate = it; selectedScope = TimeScope.CUSTOM
                        },
                        onEndDateSelected = {
                            endDate = it; selectedScope = TimeScope.CUSTOM
                        }
                    )
                }
                item {
                    CashFlowTrendChart(transactions, startDate, endDate)
                }
                item {
                    TypeSelectorCard(
                        hapticFeedback,
                        selectedType = selectedType,
                        onTypeChange = { selectedType = it },
                        transactions = transactions,
                        startTs = startTs,
                        endTs = endTs
                    )
                }

                // ── Summary header — period label is the first line ───────────
                item {
                    SummaryAmountHeader(
                        hapticFeedback,
                        totalAmount = filteredTransactions.sumOf { it.amount },
                        transactionCount = filteredTransactions.size,
                        type = selectedType,
                        periodLabel = periodLabel,
                        previousPeriodAmount = calculatePreviousPeriod(
                            transactions,
                            startDate,
                            endDate,
                            selectedType
                        )
                    )
                }

                // ── Chart — subtitle says what period ─────────────────────────
                if (filteredTransactions.isNotEmpty()) {
                    item {
                        GlassyTrendChart(
                            transactions = filteredTransactions,
                            type = selectedType
                        )
                    }
                }

                // ── Quick insights ────────────────────────────────────────────
                if (filteredTransactions.isNotEmpty()) {
                    item {
                        QuickInsightsRow(filteredTransactions, selectedType)
                    }
                }

                // ── Category breakdown ────────────────────────────────────────
                item {
                    TopBreakdownSection(
                        filteredTransactions = filteredTransactions,
                        selectedType = selectedType
                    )
                }

                // ── Empty state with helpful guidance ─────────────────────────
                if (filteredTransactions.isEmpty()) {
                    item {
                        InsightsEmptyState(
                            selectedType = selectedType,
                            periodLabel = periodLabel
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Summary Amount Header — now takes periodLabel so every number has context
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SummaryAmountHeader(
    hapticFeedback: HapticFeedback,
    totalAmount: Double,
    transactionCount: Int,
    type: TransactionType,
    periodLabel: String,            // ← added
    previousPeriodAmount: Double
) {
    val color = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val percentageChange = if (previousPeriodAmount > 0) {
        ((totalAmount - previousPeriodAmount) / previousPeriodAmount * 100)
    } else 0.0

    GlassySurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable { PesaActions.playClick(hapticFeedback) }
        ) {
            // Period label at the top — answers "this data is for WHAT period?"
            Text(
                text = periodLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total ${type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = PesaTimeConfiguration.formatCurrency(totalAmount),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )

                    if (percentageChange != 0.0) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (percentageChange > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (percentageChange > 0) color else color.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${if (percentageChange > 0) "+" else ""}${percentageChange.roundToInt()}% vs previous period",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Surface(
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = transactionCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = color,
                            lineHeight = 24.sp
                        )
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.5.sp,
                                fontSize = 9.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = color.copy(alpha = 0.8f),
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state that tells the user WHY there's nothing and what to try
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InsightsEmptyState(
    selectedType: TransactionType,
    periodLabel: String
) {
    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (selectedType == TransactionType.EXPENSE) "💸" else "💰",
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = "No ${selectedType.name.lowercase()} data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "No ${selectedType.name.lowercase()} transactions found for $periodLabel. Try a different period or switch to ${if (selectedType == TransactionType.EXPENSE) "Income" else "Expenses"}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables (GlassySurface, DateColumn, helpers)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DateColumn(label: String, date: LocalDate, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun GlassySurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.95f else 0.8f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.5.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper functions
// ─────────────────────────────────────────────────────────────────────────────

fun calculatePreviousPeriod(
    transactions: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate,
    type: TransactionType
): Double {
    val periodLength = Period.between(startDate, endDate).days + 1
    val previousStart = startDate.minusDays(periodLength.toLong())
    val previousEnd = startDate.minusDays(1)
    val previousStartTs =
        previousStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val previousEndTs =
        previousEnd.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    return transactions.filter { tx ->
        tx.timestamp in previousStartTs..previousEndTs && tx.type == type.name.lowercase()
    }.sumOf { it.amount }
}

fun formatCompactAmount(amount: Double): String = when {
    amount >= 1_000_000 -> "${(amount / 1_000_000).toInt()}M"
    amount >= 1_000 -> "${(amount / 1_000).toInt()}k"
    else -> amount.toInt().toString()
}

fun formatXLabel(key: String): String {
    return if (key.length > 7) {
        LocalDate.parse(key).format(DateTimeFormatter.ofPattern("dd MMM"))
    } else {
        YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMM yy"))
    }
}

fun showDatePicker(
    context: Context,
    startDate: LocalDate,
    endDate: LocalDate,
    isStart: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val initialDate = if (isStart) startDate else endDate
    val calendar = Calendar.getInstance().apply {
        set(initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth)
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val newDate = LocalDate.of(year, month + 1, day)
            if (isStart && newDate.isAfter(endDate)) onDateSelected(endDate)
            else if (!isStart && newDate.isBefore(startDate)) onDateSelected(startDate)
            else onDateSelected(newDate)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}