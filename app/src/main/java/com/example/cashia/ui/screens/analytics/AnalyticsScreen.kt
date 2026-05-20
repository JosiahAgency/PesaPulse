package com.example.cashia.ui.screens.analytics

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.screens.allTransactions.LargestTransactions
import com.example.cashia.utility.TimeScope
import com.example.cashia.utility.TransactionType
import com.example.cashia.utility.calculatePreviousPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

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
                TimeScope.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
                        today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

                TimeScope.MONTH -> today.withDayOfMonth(1) to
                        today.with(TemporalAdjusters.lastDayOfMonth())

                else -> startDate to endDate
            }
            startDate = start
            endDate = end
        }
    }

    val startTs by remember(startDate) {
        derivedStateOf { startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    }
    val endTs by remember(endDate) {
        derivedStateOf {
            endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
    val typeStr by remember(selectedType) { derivedStateOf { selectedType.name.lowercase() } }

    val filteredTransactions by remember(transactions, typeStr, startTs, endTs) {
        derivedStateOf { transactions.filter { it.timestamp in startTs..endTs && it.type == typeStr } }
    }
    val previousPeriodAmount by remember(transactions, startDate, endDate, selectedType) {
        derivedStateOf { calculatePreviousPeriod(transactions, startDate, endDate, selectedType) }
    }
    val periodLabel by remember(startDate, endDate, selectedScope) {
        derivedStateOf {
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
    }

    Scaffold(containerColor = Color.Transparent) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { transactionsViewModel.refreshFromSms() },
            modifier = Modifier
                .fillMaxSize()
                .padding(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Period picker
                item {
                    PeriodSelectorCard(
                        hapticFeedback, selectedScope,
                        startDate = startDate, endDate = endDate,
                        onScopeChange = { selectedScope = it },
                        onStartDateSelected = { startDate = it; selectedScope = TimeScope.CUSTOM },
                        onEndDateSelected = { endDate = it; selectedScope = TimeScope.CUSTOM }
                    )
                }

                item {
                    TypeSelectorCard(
                        hapticFeedback, selectedType = selectedType,
                        onTypeChange = { selectedType = it },
                        transactions = transactions, startTs = startTs, endTs = endTs
                    )
                }

                item { CashFlowTrendChart(transactions, startDate, endDate) }

//                item { BalanceTrendCard(transactions, startDate, endDate) }

                if (filteredTransactions.isNotEmpty()) {
//                    item {
//                        SummaryAmountHeader(
//                            hapticFeedback,
//                            totalAmount = filteredTransactions.sumOf { it.amount },
//                            transactionCount = filteredTransactions.size,
//                            type = selectedType,
//                            periodLabel = periodLabel,
//                            previousPeriodAmount = previousPeriodAmount
//                        )
//                    }
                    item {
                        CategoryPieChart(
                            filteredTransactions, allCategories, selectedType, hapticFeedback
                        )
                    }
//
//                    item {
//                        TopMerchantsCard(
//                            filteredTransactions = filteredTransactions,
//                            type = selectedType
//                        )
//                    }

                    if (selectedType == TransactionType.EXPENSE) {
                        item {
                            BurnRateCard(
                                transactions = transactions,
                                startDate = startDate,
                                endDate = endDate
                            )
                        }
                    }

                    item {
                        WeekdaySpendCard(
                            transactions = transactions,   // full history for richer pattern
                            type = typeStr
                        )
                    }

                    // Recurring merchants
                    item {
                        RecurringMerchantsCard(
                            transactions = transactions,
                            type = typeStr
                        )
                    }

                    // Time-of-day heatmap
                    item {
                        TimeOfDayCard(
                            transactions = filteredTransactions,
                            type = typeStr
                        )
                    }

                    // 7. Largest individual transactions
//                    item {
//                        LargestTransactions(
//                            filteredTransactions = filteredTransactions,
//                            allCategories = allCategories,
//                            type = selectedType
//                        )
//                    }
                } else {
                    item { InsightsEmptyState(selectedType, periodLabel) }
                }
            }
        }
    }
}

@Composable
private fun InsightsEmptyState(selectedType: TransactionType, periodLabel: String) {
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
                text = "No ${selectedType.name.lowercase()} transactions found for $periodLabel. " +
                        "Try a different period or switch to " +
                        "${if (selectedType == TransactionType.EXPENSE) "Income" else "Expenses"}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
