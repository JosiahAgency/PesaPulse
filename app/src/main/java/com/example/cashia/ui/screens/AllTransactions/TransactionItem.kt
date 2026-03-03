package com.example.cashia.ui.screens.AllTransactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.MpesaTransactions

@Composable
fun TransactionItem(
    tx: MpesaTransactions,
    categoryLabel: Pair<String, String>,
    onClick: () -> Unit,
    hapticFeedback: HapticFeedback,
    categoryViewModel: CategoryViewModel,
) {
    val allCategories by categoryViewModel.allCategories.collectAsState()

    SwipeableTransactionItem(
        tx = tx,
        categoryLabel = categoryLabel,
        allCategories = allCategories,
        categoryViewModel = categoryViewModel,
        hapticFeedback = hapticFeedback,
    )
}