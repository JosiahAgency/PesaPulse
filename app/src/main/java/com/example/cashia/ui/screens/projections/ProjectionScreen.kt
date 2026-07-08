package com.example.cashia.ui.screens.projections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.ProjectionViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.utility.PesaActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionScreen(
    transactionsViewModel: TransactionsViewModel,
    upcomingViewModel: UpcomingViewModel,
    categoryViewModel: CategoryViewModel,
    projectionViewModel: ProjectionViewModel
) {
    val haptic = LocalHapticFeedback.current

    // All projection math (engine run, confidence, upcoming buckets) happens in
    // ProjectionViewModel on Dispatchers.Default — this screen only renders state.
    val uiState by projectionViewModel.uiState.collectAsState()
    val state by transactionsViewModel.state.collectAsState()
    val allTransactions by transactionsViewModel.allTransactionsFlow.collectAsState()
    val allCategories by categoryViewModel.allCategories.collectAsState()

    val projection = uiState.projection

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 115.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "config") {
            ProjectionConfigCard(
                isExpanded = uiState.configExpanded,
                onToggle = { PesaActions.playClick(haptic); projectionViewModel.toggleConfig() },
                selectedPeriod = uiState.selectedPeriodIndex,
                onPeriodChange = projectionViewModel::setPeriod,
                balanceOverride = uiState.balanceOverride,
                onBalanceOverrideChange = projectionViewModel::setBalanceOverride,
                autoBalance = state.latestBalance,
                autoBalanceDate = state.latestBalanceDate,
                haptic = haptic
            )
        }

        if (projection != null) {
            item(key = "balance_card") {
                PredictedBalanceCard(
                    currentBalance = uiState.effectiveBalance,
                    period = uiState.period,
                    projection = projection,
                    confidence = uiState.confidence,
                    zeroCrossingDay = uiState.zeroCrossingDay
                )
            }

            item(key = "trend_chart") {
                ProjectionTrendChart(
                    projection = projection,
                    period = uiState.period,
                    currentBalance = uiState.effectiveBalance,
                    confidence = uiState.confidence
                )
            }
        }

        item(key = "upcoming_section") {
            UpcomingSection(
                transactionsViewModel,
                uiState.overdue,
                uiState.dueSoon,
                uiState.later,
                allCategories,
                upcomingViewModel,
                haptic,
                categoryViewModel
            )
        }

        if (projection != null) {
            item(key = "insights_card") {
                ProjectionInsightsCard(
                    projection = projection,
                    transactionCount = uiState.recentTransactionsCount,
                    period = uiState.period,
                    selectedPeriodIndex = uiState.selectedPeriodIndex,
                    onPeriodChange = projectionViewModel::setPeriod,
                    allTransactions,
                    allCategories
                )
            }
        }
    }
}
