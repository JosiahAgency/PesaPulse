package com.example.cashia.ui.screens.upcomingTransactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.UpcomingViewModel

/**
 * Full CRUD "Upcoming Payments" screen — hosts [UpcomingTransactionsSection] unchanged, just
 * relocated out of History (which now only shows a compact preview + "View all" link here).
 */
@Composable
fun UpcomingPaymentsScreen(
    upcomingViewModel: UpcomingViewModel,
    categoryViewModel: CategoryViewModel,
) {
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val upcomingUiState by upcomingViewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 115.dp, bottom = 32.dp)
        ) {
            UpcomingTransactionsSection(
                upcomingViewModel = upcomingViewModel,
                allCategories = allCategories
            )
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
