package com.example.cashia.ui.screens.upcomingTransactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.theme.screenContentPadding
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration

/**
 * Full CRUD "Upcoming Payments" screen — relocated out of History (which now only shows a
 * compact preview + "View all" link here). Uses a single LazyColumn as the root scrollable
 * so payment cards are only composed as they scroll into view, instead of the previous
 * eager `Column { forEach { ... } }` inside a `verticalScroll` container.
 */
@Composable
fun UpcomingPaymentsScreen(
    upcomingViewModel: UpcomingViewModel,
    categoryViewModel: CategoryViewModel,
) {
    val haptic = LocalHapticFeedback.current
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val upcomingUiState by upcomingViewModel.uiState.collectAsStateWithLifecycle()
    val upcoming by upcomingViewModel.upcoming.collectAsStateWithLifecycle()
    val matches by upcomingViewModel.possibleMatches.collectAsStateWithLifecycle()
    val isWorking by upcomingViewModel.isWorking.collectAsStateWithLifecycle()

    var expanded by remember { mutableStateOf(true) }
    var deleteTarget by remember { mutableStateOf<UpcomingTransaction?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = screenContentPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (matches.isNotEmpty()) {
                item(key = "matches_header") {
                    SectionLabel("Possible matches - confirm payment")
                    Text("Payments remain pending until you confirm.", color = MaterialTheme.colorScheme.onBackground)
                }
                items(matches, key = { "match_${it.upcomingId}_${it.transactionId}" }) { match ->
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(match.title, style = MaterialTheme.typography.titleMedium)
                            Text("Due ${match.dueDate}: ${PesaTimeConfiguration.formatCurrency(match.expectedAmount)}")
                            Text("${match.recipient} / ${match.date}: ${PesaTimeConfiguration.formatCurrency(match.amount)}")
                            Text("Reference: ${match.reference ?: "Unknown"}")
                            Text(match.reason, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { upcomingViewModel.dismissMatch(match) }, enabled = !isWorking) {
                                    Text("Not this payment")
                                }
                                Button(onClick = { upcomingViewModel.confirmMatch(match) }, enabled = !isWorking) {
                                    Text("Confirm payment")
                                }
                            }
                        }
                    }
                }
            }
            item(key = "upcoming_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                        .clickable { PesaActions.playClick(haptic); expanded = !expanded }
                        .padding(16.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        SectionLabel("upcoming payments")
                        Subtitles("${upcoming.size} pending")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                PesaActions.playClick(haptic)
                                upcomingViewModel.openCreate()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "Add upcoming payment",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Icon(
                            if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (expanded) {
                if (upcoming.isEmpty()) {
                    item(key = "upcoming_empty") {
                        EmptyStateMessage(
                            title = "No upcoming payments",
                            message = "Recurring bills you add or auto-detect will show up here.",
                            emoji = "📅"
                        )
                    }
                } else {
                    items(upcoming, key = { it.id }) { tx ->
                        UpcomingTransactionCard(
                            tx = tx,
                            allCategories = allCategories,
                            onMarkPaid = { upcomingViewModel.markPaid(tx) },
                            onSkip = { upcomingViewModel.skip(tx) },
                            onEdit = { upcomingViewModel.openEdit(tx) },
                            onDelete = { deleteTarget = tx }
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"${tx.title}\"?") },
            text = { Text("This upcoming payment will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = { upcomingViewModel.delete(tx); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
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
