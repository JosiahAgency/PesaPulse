package com.example.cashia.ui.screens.analytics

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.AnalyticsViewModel
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.standardScreenPadding
import com.example.cashia.utility.TransactionType

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    categoryViewModel: CategoryViewModel,
    analyticsViewModel: AnalyticsViewModel
) {
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val periodTransactions by analyticsViewModel.periodTransactions.collectAsStateWithLifecycle()
    val weekdayPattern by analyticsViewModel.weekdayPatternFlow.collectAsStateWithLifecycle()
    val recurringMerchants by analyticsViewModel.recurringMerchantsFlow.collectAsStateWithLifecycle()
    val uiState by analyticsViewModel.uiState.collectAsStateWithLifecycle()

    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(Unit) { analyticsViewModel.refreshCurrentScopeRange() }
//    val transactions = allTransactions
//    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
//    var selectedScope by remember { mutableStateOf(TimeScope.MONTH) }
//    val today = remember { LocalDate.now() }
//    var startDate by remember { mutableStateOf(today.withDayOfMonth(1)) }
//    var endDate by remember { mutableStateOf(today) }

//    LaunchedEffect(selectedScope) {
//        if (selectedScope != TimeScope.CUSTOM) {
//            val (start, end) = when (selectedScope) {
//                TimeScope.DAY -> today to today
//                TimeScope.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
//                        today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
//
//                TimeScope.MONTH -> today.withDayOfMonth(1) to
//                        today.with(TemporalAdjusters.lastDayOfMonth())
//
//                else -> startDate to endDate
//            }
//            startDate = start
//            endDate = end
////        }
////    }
//
//    val startTs by remember(startDate) {
//        derivedStateOf { startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
//    }
//    val endTs by remember(endDate) {
//        derivedStateOf {
//            endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
//        }
//    }
//    val typeStr by remember(selectedType) { derivedStateOf { selectedType.name.lowercase() } }
//
//    val filteredTransactions by remember(transactions, typeStr, startTs, endTs) {
//        derivedStateOf { transactions.filter { it.timestamp in startTs..endTs && it.type == typeStr } }
//    }
//    val periodLabel by remember(startDate, endDate, selectedScope) {
//        derivedStateOf {
//            when (selectedScope) {
//                TimeScope.DAY -> today.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
//                TimeScope.WEEK -> "${startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
//                    endDate.format(
//                        DateTimeFormatter.ofPattern("dd MMM yyyy")
//                    )
//                }"
//
//                TimeScope.MONTH -> today.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
//                TimeScope.CUSTOM -> "${startDate.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
//                    endDate.format(
//                        DateTimeFormatter.ofPattern("dd MMM yyyy")
//                    )
//                }"
//
//                else -> ""
//            }
//        }
//    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = standardScreenPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Period picker
        item {
            PeriodSelectorCard(
                hapticFeedback,
                uiState.selectedScope,
                uiState.startDate,
                uiState.endDate,
                analyticsViewModel::setScope,
                analyticsViewModel::setCustomStartDate,
                analyticsViewModel::setCustomEndDate,
            )
        }

        item {
            TypeSelectorCard(
                hapticFeedback,
                uiState.selectedType,
                analyticsViewModel::setType,
                transactions = periodTransactions,
                uiState.startTs,
                uiState.endTs
            )
        }

        item { CashFlowTrendChart(periodTransactions, uiState.startDate, uiState.endDate) }

        if (uiState.filteredTransactions.isNotEmpty()) {
            item {
                CategoryPieChart(
                    uiState.filteredTransactions,
                    allCategories,
                    uiState.selectedType,
                    hapticFeedback
                )
            }

            if (uiState.selectedType == TransactionType.EXPENSE) {
                item {
                    BurnRateCard(
                        transactions = periodTransactions,
                        startDate = uiState.startDate,
                        endDate = uiState.endDate
                    )
                }
            }
            val typeStr = uiState.selectedType.name.lowercase()
            item { WeekdaySpendCard(pattern = weekdayPattern) }
            item {
                RecurringMerchantsCard(
                    merchants = recurringMerchants,
                    type = typeStr
                )
            }
            item {
                TimeOfDayCard(
                    transactions = uiState.filteredTransactions,
                    type = typeStr
                )
            }
        } else {
            item { InsightsEmptyState(uiState.selectedType, uiState.periodLabel) }
        }
    }
}

@Composable
private fun InsightsEmptyState(selectedType: TransactionType, periodLabel: String) {
    val typeLabel = selectedType.name.lowercase()
    val switchTo = if (selectedType == TransactionType.EXPENSE) "Income" else "Expenses"
    EmptyStateMessage(
        title = "No $typeLabel data",
        message = "No $typeLabel transactions found for $periodLabel. " +
                "Try a different period or switch to $switchTo.",
        emoji = if (selectedType == TransactionType.EXPENSE) "💸" else "💰"
    )
}
