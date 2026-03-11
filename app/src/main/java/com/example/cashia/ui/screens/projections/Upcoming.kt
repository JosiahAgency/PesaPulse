package com.example.cashia.ui.screens.projections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.utility.PesaActions
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingEditorSheet
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionCard
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingSection(
    viewModel: TransactionsViewModel,
    overdue: List<UpcomingTransaction>,
    dueSoon: List<UpcomingTransaction>,
    later: List<UpcomingTransaction>,
    allCategories: List<ResolvedCategory>,
    upcomingViewModel: UpcomingViewModel,
    haptic: HapticFeedback,
    categoryViewModel: CategoryViewModel,
) {
    val uiState = upcomingViewModel.uiState
    var deleteTarget by remember { mutableStateOf<UpcomingTransaction?>(null) }
    val total = overdue.size + dueSoon.size + later.size


    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SCHEDULED PAYMENTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black, letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
                Text(
                    "$total factored into forecast",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var matchRunning by remember { mutableStateOf(false) }
                val allTx = viewModel.state.allTransactions

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (!matchRunning) {
                                PesaActions.playClick(haptic)
                                matchRunning = true
                                upcomingViewModel.matchAllPending(allTx)
                                MainScope().launch {
                                    delay(2000)
                                    matchRunning = false
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (matchRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Sync, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { PesaActions.playClick(haptic); upcomingViewModel.openCreate() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp)) }
            }
        }

        if (total == 0) {
            GlassySurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📅", fontSize = 32.sp)
                    Text(
                        "No scheduled payments",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Add rent, subscriptions, or salary to make your forecast more accurate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Overdue
            if (overdue.isNotEmpty()) {
                UpcomingSubSection(
                    title = "⚠️ OVERDUE",
                    color = MaterialTheme.colorScheme.error,
                    items = overdue,
                    allCategories = allCategories,
                    onMarkPaid = { upcomingViewModel.markPaid(it) },
                    onSkip = { upcomingViewModel.skip(it) },
                    onEdit = { upcomingViewModel.openEdit(it) },
                    onDelete = { deleteTarget = it }
                )
            }
            // Due soon
            if (dueSoon.isNotEmpty()) {
                UpcomingSubSection(
                    title = "🔔 DUE THIS WEEK",
                    color = Color(0xFFF59E0B),
                    items = dueSoon,
                    allCategories = allCategories,
                    onMarkPaid = { upcomingViewModel.markPaid(it) },
                    onSkip = { upcomingViewModel.skip(it) },
                    onEdit = { upcomingViewModel.openEdit(it) },
                    onDelete = { deleteTarget = it }
                )
            }
            // Later
            if (later.isNotEmpty()) {
                UpcomingSubSection(
                    title = "📆 LATER",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    items = later,
                    allCategories = allCategories,
                    onMarkPaid = { upcomingViewModel.markPaid(it) },
                    onSkip = { upcomingViewModel.skip(it) },
                    onEdit = { upcomingViewModel.openEdit(it) },
                    onDelete = { deleteTarget = it }
                )
            }

        }

        // Editor sheet
        if (uiState.showSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = upcomingViewModel::closeSheet,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .navigationBarsPadding()
                ) {
                    UpcomingEditorSheet(
                        uiState = uiState,
                        allCategories = allCategories,
                        onTitleChange = upcomingViewModel::onTitleChange,
                        onAmountChange = upcomingViewModel::onAmountChange,
                        onCategoryChange = upcomingViewModel::onCategoryChange,
                        onDueDateChange = upcomingViewModel::onDueDateChange,
                        onTypeChange = upcomingViewModel::onTypeChange,
                        onRecurrenceChange = upcomingViewModel::onRecurrenceChange,
                        onNotesChange = upcomingViewModel::onNotesChange,
                        onSave = upcomingViewModel::save,
                        onDismiss = upcomingViewModel::closeSheet,
                        categoryViewModel = categoryViewModel

                    )
                }
            }
        }

        deleteTarget?.let { tx ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete \"${tx.title}\"?") },
                text = { Text("This scheduled payment will be permanently removed from your forecast.") },
                confirmButton = {
                    Button(
                        onClick = { upcomingViewModel.delete(tx); deleteTarget = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun UpcomingSubSection(
    title: String,
    color: Color,
    items: List<UpcomingTransaction>,
    allCategories: List<ResolvedCategory>,
    onMarkPaid: (UpcomingTransaction) -> Unit,
    onSkip: (UpcomingTransaction) -> Unit,
    onEdit: (UpcomingTransaction) -> Unit,
    onDelete: (UpcomingTransaction) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black, letterSpacing = 1.2.sp
                ),
                color = color
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
                    Text(
                        "${items.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { tx ->
                    UpcomingTransactionCard(
                        tx = tx,
                        allCategories = allCategories,
                        onMarkPaid = { onMarkPaid(tx) },
                        onSkip = { onSkip(tx) },
                        onEdit = { onEdit(tx) },
                        onDelete = { onDelete(tx) }
                    )
                }
            }
        }
    }
}
