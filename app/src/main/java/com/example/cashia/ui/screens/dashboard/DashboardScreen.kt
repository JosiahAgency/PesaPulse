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
import com.example.cashia.ui.screens.WeeklyCalendar
import com.example.cashia.ui.screens.allTransactions.TransactionItem
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.utility.PesaActions

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel,
    dashboardViewModel: DashboardViewModel
) {
    val selectedDate by dashboardViewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedScope by dashboardViewModel.scope.collectAsStateWithLifecycle()
    val isListExpanded by dashboardViewModel.isListExpanded.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()

    val hapticFeedback = LocalHapticFeedback.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val reversalIndex by viewModel.reversalIndex.collectAsStateWithLifecycle()
    val upcomingUiState by upcomingViewModel.uiState.collectAsStateWithLifecycle()

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
                    onToggleVisibility = viewModel::toggleBalanceVisibility
                )
            }
            item {
                PeriodSummaryCard(
                    selectedScope,
                    dashboardViewModel::setScope,
                    dashboardState.periodLabel,
                    dashboardState.totalIncome,
                    dashboardState.totalExpenses,
                    hapticFeedback
                )
            }
            item {
                WeeklyCalendar(
                    hapticFeedback,
                    state.allTransactions,
                    selectedDate,
                    dashboardViewModel::setDate
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
                items(dashboardState.filteredTransactions, key = { it.id }) { tx ->
                    TransactionItem(
                        tx,
                        allCategories,
                        categoryViewModel,
                        upcomingViewModel,
                        hapticFeedback,
                        reversalIndex,
                    )
                }
            } else if (dashboardState.filteredTransactions.isEmpty() && isListExpanded) {
                item { EmptyStateMessage("No transactions in this period") }
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
