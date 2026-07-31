package com.example.cashia.ui.screens.upcomingTransactions

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.RecurrenceType
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingType
import com.example.cashia.models.UpcomingUiState
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.screens.category.CategoryPickerSheet
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingTransactionsSection(
    upcomingViewModel: UpcomingViewModel,
    allCategories: List<ResolvedCategory>
) {
    val haptic = LocalHapticFeedback.current
    val upcoming by upcomingViewModel.upcoming.collectAsState()
    var expanded by remember { mutableStateOf(true) }
    var deleteTarget by remember { mutableStateOf<UpcomingTransaction?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            PesaActions.playClick(haptic)
                            upcomingViewModel.openCreate()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Add, null, tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (upcoming.isEmpty()) {
                GlassySurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📅", fontSize = 32.sp)
                        Subtitles("No Upcoming Payments Added")
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcoming.forEach { tx ->
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
}

@Composable
fun UpcomingTransactionCard(
    tx: UpcomingTransaction,
    allCategories: List<ResolvedCategory>,
    onMarkPaid: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dueDate = remember(tx.dueDate) { LocalDate.parse(tx.dueDate) }
    val today = LocalDate.now()
    val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
    val isOverdue = daysUntil < 0
    val isDueToday = daysUntil == 0L
    val isDueSoon = daysUntil in 1..3

    val accentColor = when {
        isOverdue -> MaterialTheme.colorScheme.error
        isDueToday -> Color(0xFFF59E0B)
        isDueSoon -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val resolved = allCategories.find { it.id == tx.categoryId }
    val emoji = resolved?.emoji ?: "📦"

    val dueDateLabel = when {
        isOverdue -> "Overdue by ${-daysUntil}d"
        isDueToday -> "Due Today"
        daysUntil == 1L -> "Due Tomorrow"
        else -> "Due in ${daysUntil}d · ${dueDate.format(DateTimeFormatter.ofPattern("dd MMM"))}"
    }

    var actionsExpanded by remember { mutableStateOf(false) }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Emoji badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            accentColor.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 22.sp) }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tx.title,
                        color = if (tx.type == UpcomingType.INCOME)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Due date chip
                        Surface(
                            shape = CircleShape,
                            color = accentColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                dueDateLabel,
                                modifier = Modifier.padding(8.dp, 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Recurrence badge
                        if (tx.recurrence != RecurrenceType.NONE) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    tx.recurrence.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.padding(8.dp, 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Amount + expand toggle
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        PesaTimeConfiguration.formatCurrency(tx.amount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = if (tx.type == UpcomingType.INCOME)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    IconButton(
                        onClick = { actionsExpanded = !actionsExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (actionsExpanded) Icons.Rounded.KeyboardArrowUp
                            else Icons.Rounded.KeyboardArrowDown,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Notes
            if (tx.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    tx.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }

            // Actions
            AnimatedVisibility(visible = actionsExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMarkPaid,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Mark Paid", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Rounded.Edit, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Rounded.Delete, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact preview used on History — soonest 2-3 pending items, no per-item expand/actions,
 * plus a "View all" row to the dedicated [com.example.cashia.ui.screens.upcomingTransactions.UpcomingPaymentsScreen].
 * Full CRUD stays on that screen; this is read-only at a glance.
 */
@Composable
fun UpcomingPreviewSection(
    upcomingViewModel: UpcomingViewModel,
    allCategories: List<ResolvedCategory>,
    onViewAll: () -> Unit
) {
    val upcoming by upcomingViewModel.upcoming.collectAsState()
    if (upcoming.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel("upcoming payments")
            Text(
                "View all",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }
        upcoming.take(3).forEach { tx ->
            val resolved = allCategories.find { it.id == tx.categoryId }
            val emoji = resolved?.emoji ?: "📦"
            val accentColor = if (tx.type == UpcomingType.INCOME)
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.error

            GlassySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewAll() }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(emoji, fontSize = 18.sp) }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tx.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            tx.relativeDueLabel.lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        PesaTimeConfiguration.formatCurrency(tx.amount),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingTransactionSheet(
    uiState: UpcomingUiState,
    viewModel: UpcomingViewModel,
    allCategories: List<ResolvedCategory>,
    categoryViewModel: CategoryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                onTitleChange = viewModel::onTitleChange,
                onAmountChange = viewModel::onAmountChange,
                onCategoryChange = viewModel::onCategoryChange,
                onDueDateChange = viewModel::onDueDateChange,
                onTypeChange = viewModel::onTypeChange,
                onRecurrenceChange = viewModel::onRecurrenceChange,
                onNotesChange = viewModel::onNotesChange,
                onSave = viewModel::save,
                onDismiss = onDismiss,
                categoryViewModel = categoryViewModel,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingEditorSheet(
    uiState: UpcomingUiState,
    allCategories: List<ResolvedCategory>,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDueDateChange: (LocalDate) -> Unit,
    onTypeChange: (UpcomingType) -> Unit,
    onRecurrenceChange: (RecurrenceType) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    categoryViewModel: CategoryViewModel,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var showCategoryPicker by remember { mutableStateOf(false) }
    val resolvedCategory = allCategories.find { it.id == uiState.draftCategory }
    val isEditing = uiState.editing != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isEditing) "Edit Payment" else "New Upcoming Payment",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null)
            }
        }

        // Type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UpcomingType.entries.forEach { type ->
                val selected = uiState.draftType == type
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { PesaActions.playClick(haptic); onTypeChange(type) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        when (type) {
                            UpcomingType.EXPENSE -> "💸 Expense"
                            UpcomingType.INCOME -> "💰 Income"
                            UpcomingType.SAVING -> "🏦 Saving"
                        },
                        modifier = Modifier.padding(vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Title field
        OutlinedTextField(
            value = uiState.draftTitle,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            placeholder = { Text("e.g. Rent, Netflix, Salary") },
            isError = uiState.error?.contains("Title") == true,
            supportingText = uiState.error?.takeIf { it.contains("Title") }
                ?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        // Amount field
        OutlinedTextField(
            value = uiState.draftAmount,
            onValueChange = onAmountChange,
            label = { Text("Amount (KES)") },
            isError = uiState.error?.contains("amount") == true,
            supportingText = uiState.error?.takeIf { it.contains("amount") }
                ?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        // Due date picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.draftDueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                onValueChange = {},
                label = { Text("Due Date") },
                enabled = false,
                trailingIcon = {
                    Icon(
                        Icons.Rounded.CalendarMonth, null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },

                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        val cal = Calendar.getInstance().apply {
                            set(
                                uiState.draftDueDate.year,
                                uiState.draftDueDate.monthValue - 1,
                                uiState.draftDueDate.dayOfMonth
                            )
                        }
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> onDueDateChange(LocalDate.of(y, m + 1, d)) },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.primary,
                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),

                shape = RoundedCornerShape(14.dp)
            )
        }

        // Category picker
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { showCategoryPicker = true },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(resolvedCategory?.emoji ?: "📦", fontSize = 22.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Category", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        resolvedCategory?.label ?: "Other",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }

        // Recurrence
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Recurrence",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RecurrenceType.entries) { rec ->
                    val selected = uiState.draftRecurrence == rec
                    FilterChip(
                        selected = selected,
                        onClick = { PesaActions.playClick(haptic); onRecurrenceChange(rec) },
                        label = {
                            Text(
                                when (rec) {
                                    RecurrenceType.NONE -> "One-time"
                                    RecurrenceType.WEEKLY -> "Weekly"
                                    RecurrenceType.MONTHLY -> "Monthly"
                                    RecurrenceType.YEARLY -> "Yearly"
                                }
                            )
                        }
                    )
                }
            }
        }

        // Notes
        OutlinedTextField(
            value = uiState.draftNotes,
            onValueChange = onNotesChange,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2, maxLines = 3,
            shape = RoundedCornerShape(14.dp)
        )

        // Save button
        Button(
            onClick = { PesaActions.playSuccess(haptic); onSave() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (isEditing) "Save Changes" else "Add Payment",
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            allCategories = allCategories,
            currentCategoryId = uiState.draftCategory,
            onPick = { onCategoryChange(it); showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false },
            categoryViewModel = categoryViewModel,
        )
    }
}