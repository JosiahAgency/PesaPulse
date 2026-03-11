package com.example.cashia.ui.screens.Dashboard

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TimeScope
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.screens.AllTransactions.EmptyStateMessage
import com.example.cashia.ui.screens.AllTransactions.TransactionItem
import com.example.cashia.ui.screens.AllTransactions.resolveCategory
import com.example.cashia.ui.screens.WeeklyCalendar
import com.example.cashia.ui.theme.primaryGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TransactionsViewModel, categoryViewModel: CategoryViewModel,
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val state = viewModel.state
    val hapticFeedback = LocalHapticFeedback.current
    var isListExpanded by remember { mutableStateOf(true) }
    val allCategories by categoryViewModel.allCategories.collectAsState()

    val firstDay = PesaTimeConfiguration.firstDayOfWeek
    val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(firstDay))
    val endOfWeek = startOfWeek.plusDays(6)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onPermissionGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedScope by remember { mutableStateOf(TimeScope.DAY) }
    val summaryData by remember(selectedDate, selectedScope, state.allTransactions) {
        derivedStateOf { viewModel.calculateSummary(selectedDate, selectedScope) }
    }

    val transactionsForScope = remember(state.allTransactions, selectedDate, selectedScope) {
        state.allTransactions.filter { tx ->
            val txDate = LocalDate.parse(tx.date)
            when (selectedScope) {
                TimeScope.DAY -> txDate == selectedDate
                TimeScope.WEEK -> !txDate.isBefore(startOfWeek) && !txDate.isAfter(endOfWeek)
                TimeScope.MONTH -> txDate.month == selectedDate.month && txDate.year == selectedDate.year
                TimeScope.YEAR -> txDate.year == selectedDate.year
                else -> txDate == selectedDate
            }
        }.sortedByDescending { it.date }
    }

    val periodLabel = remember(selectedDate, selectedScope) {
        when (selectedScope) {
            TimeScope.DAY -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM"))
            TimeScope.WEEK -> "${startOfWeek.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                endOfWeek.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                )
            }"

            TimeScope.MONTH -> selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimeScope.YEAR -> selectedDate.format(DateTimeFormatter.ofPattern("yyyy"))
            else -> ""
        }
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
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refreshFromSms() },
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
                        selectedScope = selectedScope,
                        onScopeChange = { selectedScope = it },
                        periodLabel = periodLabel,
                        summaryData = summaryData,
                        hapticFeedback = hapticFeedback
                    )
                }

                item {
                    WeeklyCalendar(
                        hapticFeedback,
                        state.allTransactions,
                        selectedDate
                    ) { selectedDate = it }
                }

                item {
                    TransactionListHeader(
                        periodLabel = periodLabel,
                        count = transactionsForScope.size,
                        isExpanded = isListExpanded,
                        onToggle = {
                            PesaActions.playClick(hapticFeedback)
                            isListExpanded = !isListExpanded
                        }
                    )
                }

                if (transactionsForScope.isNotEmpty() && isListExpanded) {
                    items(transactionsForScope, key = { it.id }) { tx ->
                        TransactionItem(
                            tx,
                            categoryLabel = resolveCategory(tx.category, allCategories),
                            onClick = { tx },
                            hapticFeedback,
                            categoryViewModel
                        )
                    }
                } else if (transactionsForScope.isEmpty() && isListExpanded) {
                    item { EmptyStateMessage("No transactions in this period", hapticFeedback) }
                }
            }
        }
    }
}
