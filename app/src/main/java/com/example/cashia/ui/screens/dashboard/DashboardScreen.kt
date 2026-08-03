package com.example.cashia.ui.screens.dashboard

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.cashia.ui.reusedElements.DataSyncBanner
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.PesaDialog
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.standardScreenPadding
import com.example.cashia.ui.screens.WeeklyCalendar
import com.example.cashia.ui.screens.allTransactions.TransactionItem
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.models.UpcomingStatus
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.dashboardDayBucket
import java.time.LocalDate
import androidx.compose.ui.text.input.KeyboardType

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
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val weeklyCalendarTransactions by dashboardViewModel.weeklyCalendarTransactionsFlow.collectAsStateWithLifecycle()
    val monthPace by dashboardViewModel.monthPace.collectAsStateWithLifecycle()
    val previousMonthSpend by dashboardViewModel.previousMonthSpend.collectAsStateWithLifecycle()
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val reversalIndex by viewModel.reversalIndex.collectAsStateWithLifecycle()
    val upcomingUiState by upcomingViewModel.uiState.collectAsStateWithLifecycle()
    val upcoming by upcomingViewModel.upcoming.collectAsStateWithLifecycle()
    var showTargetPrompt by rememberSaveable { mutableStateOf(false) }
    var monthlyTargetInput by rememberSaveable { mutableStateOf("") }
    var monthlyTargetError by rememberSaveable { mutableStateOf<String?>(null) }
    val canUsePreviousMonthSpend = previousMonthSpend > 0.0

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

    LaunchedEffect(Unit) {
        dashboardViewModel.refreshMonthlyContext()
        showTargetPrompt = !dashboardViewModel.hasCurrentMonthTarget()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = standardScreenPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isSyncing) {
                item(key = "dashboard_sync_banner") {
                    DataSyncBanner()
                }
            }
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
                dashboardState.dayGroups.forEach { dayGroup ->
                    item(key = "day-header-${dayGroup.date}") {
                        SectionLabel(dashboardDayBucket(dayGroup.date))
                    }
                    items(dayGroup.transactions, key = { it.id }) { tx ->
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

    if (showTargetPrompt) {
        PesaDialog(
            onDismissRequest = { showTargetPrompt = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Set this month spending target",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Pace will be tracked against this amount for the current month.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
            )
            Text(
                text = "Current balance: ${
                    com.example.cashia.utility.PesaTimeConfiguration.formatCurrency(
                        state.latestBalance
                    )
                }",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Last month spend: ${
                    com.example.cashia.utility.PesaTimeConfiguration.formatCurrency(previousMonthSpend)
                }",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )
            if (!canUsePreviousMonthSpend) {
                Text(
                    text = "No spending recorded last month, so this option is unavailable.",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            OutlinedTextField(
                value = monthlyTargetInput,
                onValueChange = {
                    monthlyTargetInput = it
                    monthlyTargetError = null
                },
                label = { Text("Target amount (KES)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = monthlyTargetError != null,
                supportingText = monthlyTargetError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showTargetPrompt = false }) { Text("Not now") }
                TextButton(
                    enabled = canUsePreviousMonthSpend,
                    onClick = {
                        if (dashboardViewModel.useLastMonthSpendAsCurrentTarget()) {
                            showTargetPrompt = false
                            monthlyTargetInput = ""
                            monthlyTargetError = null
                        } else {
                            monthlyTargetError = "No valid spending total from last month."
                        }
                    }
                ) { Text("Use last month spend") }
                TextButton(
                    onClick = {
                        val normalized = monthlyTargetInput.trim().replace(",", "")
                        val amount = normalized.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            monthlyTargetError = "Enter a valid amount greater than zero."
                        } else {
                            dashboardViewModel.saveCurrentMonthTarget(amount)
                            showTargetPrompt = false
                            monthlyTargetInput = ""
                            monthlyTargetError = null
                        }
                    }
                ) { Text("Save") }
            }
        }
    }
}
