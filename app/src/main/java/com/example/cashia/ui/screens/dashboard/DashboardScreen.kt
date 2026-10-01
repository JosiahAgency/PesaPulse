package com.example.cashia.ui.screens.dashboard

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.example.cashia.models.UpcomingStatus
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.rememberCategoryIndex
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.screens.WeeklyCalendar
import com.example.cashia.ui.screens.allTransactions.TransactionItem
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.ui.theme.screenContentPadding
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.dashboardDayBucket
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
    val latestBalance by viewModel.latestBalance.collectAsStateWithLifecycle()
    val weeklyCalendarActivity by dashboardViewModel.weeklyCalendarActivity.collectAsStateWithLifecycle()
    val monthPace by dashboardViewModel.monthPace.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncError by viewModel.lastSyncError.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val previousMonthSpend by dashboardViewModel.previousMonthSpend.collectAsStateWithLifecycle()
    var showMonthTargetDialog by remember { mutableStateOf(false) }
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val categoryIndex = rememberCategoryIndex(allCategories)
    val reversalIndex by viewModel.reversalIndex.collectAsStateWithLifecycle()
    val upcomingUiState by upcomingViewModel.uiState.collectAsStateWithLifecycle()
    val upcoming by upcomingViewModel.upcoming.collectAsStateWithLifecycle()

    // Day grouping comes pre-built (and pre-sorted) off the main thread in DashboardViewModel
    // via buildDayGroups — see DashboardState.dayGroups.

    // Pending payments due within 8 days, excluding overdue items.
    val dueSoon = remember(upcoming) {
        val today = LocalDate.now()
        val cutoff = today.plusDays(8)
        upcoming
            .asSequence()
            .filter { it.status == UpcomingStatus.PENDING }
            .mapNotNull { tx -> tx.dueDateParsed?.let { d -> tx to d } }
            .filter { (_, d) -> !d.isBefore(today) && d.isBefore(cutoff) }
            .sortedBy { it.second }
            .map { it.first }
            .toList()
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
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    isRefreshing = isSyncing,
                    state = pullToRefreshState,
                    onRefresh = viewModel::refreshFromSms
                )
        ) {
            item {
                BalanceHeroCard(
                    latestBalance?.balance,
                    latestBalance?.date.orEmpty(),
                    isVisible = viewModel.isBalanceVisible,
                    onToggleVisibility = viewModel::toggleBalanceVisibility,
                    income = dashboardState.totalIncome,
                    expenses = dashboardState.totalExpenses,
                    isLoading = latestBalance == null || !dashboardState.hasLoaded,
                    unknownFeeCount = dashboardState.unknownFeeCount,
                )
            }
            syncError?.let { message ->
                item(key = "sync-error") {
                    SyncErrorBanner(
                        message = message,
                        onRetry = {
                            PesaActions.playClick(hapticFeedback)
                            viewModel.refreshFromSms()
                        },
                        onDismiss = viewModel::dismissSyncError
                    )
                }
            }
            // Kept directly under the balance: at the foot of the list a long period of
            // transactions could push it off-screen entirely.
            if (dueSoon.isNotEmpty()) {
                item(key = "due-soon") {
                    DueSoonBanner(dueSoon = dueSoon, onClick = onNavigateToUpcoming)
                }
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
                    weeklyCalendarActivity,
                    selectedDate,
                    dashboardViewModel::setDate,
                    dashboardViewModel::setVisibleWeek,
                    monthPace,
                    onEditMonthTarget = {
                        PesaActions.playClick(hapticFeedback)
                        showMonthTargetDialog = true
                    },
                    scope = selectedScope,
                    periodLabel = dashboardState.periodLabel,
                )
            }

            item {
                TransactionListHeader(
                    dashboardState.periodLabel,
                    dashboardState.filteredTransactions.size,
                    isListExpanded,
                    onToggle = {
                        PesaActions.playClick(hapticFeedback)
                        dashboardViewModel.toggleListExpanded()
                    }
                )
            }

            if (dashboardState.filteredTransactions.isNotEmpty() && isListExpanded) {
                dashboardState.dayGroups.forEach { group ->
                    item(key = "day-header-${group.date}") {
                        SectionLabel(dashboardDayBucket(group.date))
                    }
                    items(group.transactions, key = { it.id }) { tx ->
                        TransactionItem(
                            tx,
                            categoryIndex,
                            categoryViewModel,
                            upcomingViewModel,
                            hapticFeedback,
                            reversalIndex,
                        )
                    }
                }
            } else if (dashboardState.filteredTransactions.isEmpty() && isListExpanded) {
                item {
                    when {
                        !dashboardState.hasLoaded -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        syncError != null -> EmptyStateMessage(
                            title = "Couldn't load transactions",
                            message = "The last sync failed, so this period may be incomplete. Pull down to retry.",
                            emoji = "⚠️"
                        )

                        else -> EmptyStateMessage(
                            title = "No transactions",
                            message = "Nothing recorded for this period yet.",
                            emoji = "🧾"
                        )
                    }
                }
            }
        }

        PullToRefreshDefaults.Indicator(
            state = pullToRefreshState,
            isRefreshing = isSyncing,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
    if (showMonthTargetDialog) {
        MonthTargetDialog(
            currentTarget = monthPace.targetAmount.takeIf { monthPace.hasTarget },
            previousMonthSpend = previousMonthSpend,
            onDismiss = { showMonthTargetDialog = false },
            onSave = { amount ->
                dashboardViewModel.saveCurrentMonthTarget(amount)
                showMonthTargetDialog = false
            },
            onUseLastMonthSpend = {
                dashboardViewModel.useLastMonthSpendAsCurrentTarget()
                showMonthTargetDialog = false
            }
        )
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
