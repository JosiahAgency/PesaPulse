package com.example.cashia.ui.screens.dashboard

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.DashboardViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.screens.WeeklyCalendar
import com.example.cashia.ui.screens.allTransactions.TransactionItem
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.models.UpcomingStatus
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.dashboardDayBucket
import com.example.cashia.utility.localDate
import java.time.LocalDate

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel,
    dashboardViewModel: DashboardViewModel,
    onNavigateToUpcoming: () -> Unit = {},
) {
    val selectedDate by dashboardViewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedScope by dashboardViewModel.scope.collectAsStateWithLifecycle()
    val isListExpanded by dashboardViewModel.isListExpanded.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()

    val hapticFeedback = LocalHapticFeedback.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val weeklyCalendarTransactions by dashboardViewModel.weeklyCalendarTransactionsFlow.collectAsStateWithLifecycle()
    val monthPace by dashboardViewModel.monthPace.collectAsStateWithLifecycle()
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val reversalIndex by viewModel.reversalIndex.collectAsStateWithLifecycle()
    val upcomingUiState by upcomingViewModel.uiState.collectAsStateWithLifecycle()
    val upcoming by upcomingViewModel.upcoming.collectAsStateWithLifecycle()

    val groupedByDay = remember(dashboardState.filteredTransactions) {
        dashboardState.filteredTransactions
            .groupBy { it.localDate() }
            .toSortedMap(compareByDescending { it })
    }

    // Mirrors ProjectionViewModel's dueSoon bucketing (pending, due within 8 days, not overdue).
    val dueSoon = remember(upcoming) {
        val today = LocalDate.now()
        val cutoff = today.plusDays(8)
        upcoming
            .filter { it.status == UpcomingStatus.PENDING }
            .mapNotNull { tx -> tx.dueDateParsed?.let { d -> tx to d } }
            .filter { (_, d) -> !d.isBefore(today) && d.isBefore(cutoff) }
            .sortedBy { it.second }
            .map { it.first }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onPermissionGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(allCategories) {
        viewModel.updateCategories(allCategories)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 115.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                BalanceHeroCard(
                    state.latestBalance,
                    state.latestBalanceDate,
                    isVisible = viewModel.isBalanceVisible,
                    onToggleVisibility = viewModel::toggleBalanceVisibility,
                    income = dashboardState.totalIncome,
                    expenses = dashboardState.totalExpenses,
                )
            }
            item {
                PeriodSummaryCard(
                    selectedScope,
                    dashboardViewModel::setScope,
                    dashboardState.periodLabel,
                    hapticFeedback
                )
            }
            item {
                WeeklyCalendar(
                    hapticFeedback,
                    weeklyCalendarTransactions,
                    selectedDate,
                    dashboardViewModel::setDate,
                    dashboardViewModel::setVisibleWeek,
                    monthPace
                )
            }

            item {
                TransactionListHeader(
                    dashboardState.periodLabel,
                    dashboardState.filteredTransactions.size,
                    isListExpanded,
                    {
                        PesaActions.playClick(hapticFeedback)
                        dashboardViewModel.toggleListExpanded()
                    }
                )
            }

            if (dashboardState.filteredTransactions.isNotEmpty() && isListExpanded) {
                groupedByDay.forEach { (date, txs) ->
                    item(key = "day-header-$date") {
                        SectionLabel(dashboardDayBucket(date))
                    }
                    items(txs, key = { it.id }) { tx ->
                        TransactionItem(
                            tx,
                            allCategories,
                            categoryViewModel,
                            upcomingViewModel,
                            hapticFeedback,
                            reversalIndex,
                        )
                    }
                }
            } else if (dashboardState.filteredTransactions.isEmpty() && isListExpanded) {
                item {
                    EmptyStateMessage(
                        title = "No transactions",
                        message = "Nothing recorded for this period yet.",
                        emoji = "🧾"
                    )
                }
            }

            if (dueSoon.isNotEmpty()) {
                item {
                    DueSoonBanner(dueSoon = dueSoon, onClick = onNavigateToUpcoming)
                }
            }
        }
    }
    if (upcomingUiState.showSheet) {
        UpcomingTransactionSheet(
            uiState = upcomingUiState,
            viewModel = upcomingViewModel,
            allCategories = allCategories,
            categoryViewModel = categoryViewModel,
            onDismiss = upcomingViewModel::closeSheet
        )
    }
}
