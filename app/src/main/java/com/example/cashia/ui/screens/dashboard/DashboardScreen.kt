package com.example.cashia.ui.screens.dashboard

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.screens.WeeklyCalendar
import com.example.cashia.ui.screens.allTransactions.TransactionItem
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.TimeScope
import java.time.LocalDate

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel
) {
    val today = remember { LocalDate.now() }

    var selectedDate by remember { mutableStateOf(today) }
    var selectedScope by remember { mutableStateOf(TimeScope.DAY) }
    var isListExpanded by remember { mutableStateOf(true) }

    val hapticFeedback = LocalHapticFeedback.current

    val state by viewModel.state.collectAsState()
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val reversalIndex by viewModel.reversalIndex.collectAsState()
    val periodLabel by viewModel.dashboardPeriodLabel.collectAsState()
    val upcomingUiState = upcomingViewModel.uiState

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onPermissionGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val categoryLabelsMap = remember(allCategories) {
        allCategories.associate { it.id to it.label }
    }

    LaunchedEffect(selectedDate, selectedScope, categoryLabelsMap) {
        viewModel.setDashboardScope(selectedDate, selectedScope, categoryLabelsMap)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedDate != today) {
                ExtendedFloatingActionButton(
                    onClick = { selectedDate = today },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    text = { Text("Back to Today", fontWeight = FontWeight.SemiBold) },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(brush = primaryGradient())
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        { selectedScope = it },
                        periodLabel,
                        state.totalIncome,
                        state.totalExpenses,
                        hapticFeedback
                    )
                }
                item {
                    WeeklyCalendar(
                        hapticFeedback,
                        state.allTransactions,
                        selectedDate
                    ) { newDate ->
                        selectedDate = newDate
                    }
                }

                item {
                    TransactionListHeader(
                        periodLabel,
                        state.filteredTransactions.size,
                        isListExpanded,
                        {
                            PesaActions.playClick(hapticFeedback)
                            isListExpanded = !isListExpanded
                        }
                    )
                }

                if (state.filteredTransactions.isNotEmpty() && isListExpanded) {
                    items(state.filteredTransactions, key = { it.id }) { tx ->
                        TransactionItem(
                            tx,
                            allCategories,
                            categoryViewModel,
                            upcomingViewModel,
                            hapticFeedback,
                            reversalIndex,
                        )
                    }
                } else if (state.filteredTransactions.isEmpty() && isListExpanded) {
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
                onDismiss = { upcomingViewModel.closeSheet() }
            )
        }
    }
}