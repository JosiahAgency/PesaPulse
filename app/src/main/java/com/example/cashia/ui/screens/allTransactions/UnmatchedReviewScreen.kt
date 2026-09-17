package com.example.cashia.ui.screens.allTransactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.theme.screenContentPadding

/**
 * Full list of unmatched SMS for review — moved out of History (which now only shows a
 * single [com.example.cashia.ui.reusedElements.ActionBanner] entry point here). Reuses
 * [UnmatchedSmsItem] and [UnmatchedSmsModal] as-is; tapping a row opens the modal for just
 * that item (no auto-advance-to-next — the user can tap the next row directly from the list).
 */
@Composable
fun UnmatchedReviewScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
) {
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val allUnmatched by viewModel.allUnmatched.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingReviewCount.collectAsStateWithLifecycle()
    var modalTarget by remember { mutableStateOf<UnmatchedSms?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (allUnmatched.isEmpty()) {
                item {
                    EmptyStateMessage(
                        title = "All caught up",
                        message = "No unmatched messages to review.",
                        emoji = "✅"
                    )
                }
            } else {
                items(allUnmatched, key = { it.id }) { sms ->
                    UnmatchedSmsItem(
                        sms,
                        onReviewClick = { clicked -> modalTarget = clicked },
                        onIgnoreClick = { clicked -> viewModel.markIgnored(clicked) }
                    )
                }
            }
        }
    }

    modalTarget?.let { target ->
        UnmatchedSmsModal(
            sms = target,
            pendingCount = pendingCount,
            allCategories = allCategories,
            categoryViewModel = categoryViewModel,
            onSave = { type, category ->
                viewModel.saveTransactionFromSms(target, type, category)
                modalTarget = null
            },
            onSkip = {
                viewModel.markSkipped(target.id)
                modalTarget = null
            },
            onIgnore = {
                viewModel.markIgnored(target)
                modalTarget = null
            },
            onDismiss = { modalTarget = null }
        )
    }
}
