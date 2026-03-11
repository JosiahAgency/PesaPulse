package com.example.cashia.ui.screens.Analytics

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cashia.Utility.TimeScope
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.CategoryViewModel
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

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    categoryViewModel: CategoryViewModel,
    transactionsViewModel: TransactionsViewModel
) {
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val state = transactionsViewModel.state
    val transactions = state.allTransactions

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedScope by remember { mutableStateOf(TimeScope.MONTH) }
    val today = remember { LocalDate.now() }
    var startDate by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(today) }

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

    // Memoize timestamp conversions — only recompute when dates change
    val startTs by remember(startDate) {
        derivedStateOf { startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    }
    val endTs by remember(endDate) {
        derivedStateOf {
            endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }

    // Stable lowercase type string — avoids repeated allocation
    val typeStr by remember(selectedType) {
        derivedStateOf { selectedType.name.lowercase() }
    }

    val filteredTransactions by remember(transactions, typeStr, startTs, endTs) {
        derivedStateOf {
            transactions.filter { tx ->
                tx.timestamp in startTs..endTs && tx.type == typeStr
            }
        }
    }

    // Memoize previous period — was previously recomputed on every recomposition
    val previousPeriodAmount by remember(transactions, startDate, endDate, selectedType) {
        derivedStateOf {
            calculatePreviousPeriod(transactions, startDate, endDate, selectedType)
        }
    }

    val periodLabel by remember(startDate, endDate, selectedScope) {
        derivedStateOf {
            when (selectedScope) {
                TimeScope.DAY -> today.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
                TimeScope.WEEK -> "${startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                    endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                }"

                TimeScope.MONTH -> today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                TimeScope.CUSTOM -> "${startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                    endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                }"

                else -> ""
            }
        }
    }

    val allInPeriod by remember(transactions, startTs, endTs) {
        derivedStateOf { transactions.filter { it.timestamp in startTs..endTs } }
    }

    Scaffold(containerColor = Color.Transparent) { _ ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { transactionsViewModel.refreshFromSms() },
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
                        onStartDateSelected = { startDate = it; selectedScope = TimeScope.CUSTOM },
                        onEndDateSelected = { endDate = it; selectedScope = TimeScope.CUSTOM }
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
                item {
                    SummaryAmountHeader(
                        hapticFeedback,
                        totalAmount = filteredTransactions.sumOf { it.amount },
                        transactionCount = filteredTransactions.size,
                        type = selectedType,
                        periodLabel = periodLabel,
                        previousPeriodAmount = previousPeriodAmount
                    )
                }
                if (filteredTransactions.isNotEmpty()) {
                    item {
                        GlassyTrendChart(
                            transactions = filteredTransactions,
                            type = selectedType
                        )
                    }
                }
                item {
                    CategoryPieChart(
                        filteredTransactions, allCategories, selectedType
                    )
                }

                // ── New insight cards ─────────────────────────────────────────
                if (filteredTransactions.isNotEmpty()) {
                    item {
                        SpendingVelocityCard(
                            filteredTransactions = filteredTransactions,
                            startDate = startDate,
                            endDate = endDate,
                            type = selectedType
                        )
                    }
                    item {
                        TopMerchantsCard(
                            filteredTransactions = filteredTransactions,
                            type = selectedType
                        )
                    }
                    item {
                        LargestTransactionsCard(
                            filteredTransactions = filteredTransactions,
                            allCategories = allCategories,
                            type = selectedType
                        )
                    }
                    item {
                        SpendingByDayOfWeekCard(
                            filteredTransactions = filteredTransactions,
                            type = selectedType
                        )
                    }
                }

                // Activity card uses all transactions in the period (income + expense)
                // regardless of the currently selected type tab
                if (allInPeriod.isNotEmpty()) {
                    item {
                        TransactionActivityCard(
                            allTransactionsInPeriod = allInPeriod,
                            startDate = startDate,
                            endDate = endDate
                        )
                    }
                    item {
                        BalanceSparklineCard(allTransactionsInPeriod = allInPeriod)
                    }
                }
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
    val typeLower = type.name.lowercase()
    return transactions.filter { tx ->
        tx.timestamp in previousStartTs..previousEndTs && tx.type == typeLower
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