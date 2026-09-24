package com.example.cashia.ui.screens.category

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.CustomCategory
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.TransactionCategory
import com.example.cashia.ui.theme.ScreenContentTopPadding
import com.example.cashia.utility.PesaActions

internal data class CategoryGroup(
    val parent: ResolvedCategory,
    val children: List<ResolvedCategory>
)

internal fun categoryGroups(
    categories: List<ResolvedCategory>,
    query: String
): List<CategoryGroup> {
    val search = query.trim()
    val systems = categories.filter { it.isSystem }
    val children = systems.filter { it.parentId != null }.groupBy { it.parentId }
    return systems.filter { it.parentId == null }.mapNotNull { parent ->
        val allChildren = children[parent.id].orEmpty()
        val parentMatches = parent.label.contains(search, ignoreCase = true)
        val matches = if (parentMatches) allChildren else allChildren.filter {
            it.label.contains(search, ignoreCase = true)
        }
        if (parentMatches || matches.isNotEmpty()) CategoryGroup(parent, matches) else null
    }.sortedBy { it.parent.label.lowercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(viewModel: CategoryViewModel) {
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val overrides by viewModel.systemOverrides.collectAsStateWithLifecycle()
    val uiState = viewModel.uiState
    val haptic = LocalHapticFeedback.current
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("All") }
    var expandedIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var deleteTarget by remember { mutableStateOf<CustomCategory?>(null) }
    var resetTarget by remember { mutableStateOf<ResolvedCategory?>(null) }
    var confirmClassify by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    LaunchedEffect(query, filter) { listState.scrollToItem(0) }
    LaunchedEffect(viewModel.operationMessage, customCategories.size) {
        if (viewModel.operationMessage != null) listState.scrollToItem(0)
    }

    val customs = remember(customCategories, query) {
        customCategories.filter {
            it.label.contains(query.trim(), ignoreCase = true) ||
                it.keywords.contains(query.trim(), ignoreCase = true)
        }.sortedBy { it.label.lowercase() }
    }
    val groups = remember(allCategories, query) { categoryGroups(allCategories, query) }
    val showCustom = filter != "Built-in"
    val showSystem = filter != "Yours"
    val noResults = (!showCustom || customs.isEmpty()) && (!showSystem || groups.isEmpty())

    Column(
        modifier = Modifier.fillMaxSize().padding(top = ScreenContentTopPadding)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "A place for every payment",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Make categories your own. Tap one to edit its details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search names or your keywords") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "All" to allCategories.size,
                    "Yours" to customCategories.size,
                    "Built-in" to allCategories.count { it.isSystem }
                ).forEach { (label, count) ->
                    FilterChip(
                        selected = filter == label,
                        onClick = { filter = label },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        label = { Text("$label ($count)") }
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (showCustom && customs.isNotEmpty()) {
                item(key = "your-header") { CategorySectionTitle("Made by you", customs.size) }
                items(customs, key = { "custom_${it.id}" }) { category ->
                    CustomCategoryRow(
                        category = category,
                        onEdit = { PesaActions.playClick(haptic); viewModel.openEditSheet(category) },
                        onDelete = { deleteTarget = category },
                        enabled = !viewModel.isProcessing
                    )
                }
            }
            if (showCustom && customCategories.isEmpty() && query.isBlank()) {
                item(key = "empty-custom") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Your life, your categories", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Create a category for family, your side hustle, or anything that matters to you. Add keywords to sort matching payments automatically.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (showSystem && groups.isNotEmpty()) {
                item(key = "system-header") { CategorySectionTitle("Built-in library", groups.sumOf { 1 + it.children.size }) }
                groups.forEach { group ->
                    val expanded = query.isNotBlank() || group.parent.id in expandedIds
                    item(key = "system_${group.parent.id}") {
                        SystemCategoryRow(
                            category = group.parent,
                            isModified = overrides.containsKey(group.parent.id),
                            onEdit = { viewModel.openEditSystemSheet(TransactionCategory.valueOf(group.parent.id)) },
                            onReset = { resetTarget = group.parent },
                            childCount = group.children.size,
                            expanded = expanded,
                            onToggle = {
                                expandedIds = if (group.parent.id in expandedIds) expandedIds - group.parent.id
                                else expandedIds + group.parent.id
                            },
                            enabled = !viewModel.isProcessing
                        )
                    }
                    if (expanded) {
                        items(group.children, key = { "system_${it.id}" }) { child ->
                            SystemCategoryRow(
                                category = child,
                                isModified = overrides.containsKey(child.id),
                                onEdit = { viewModel.openEditSystemSheet(TransactionCategory.valueOf(child.id)) },
                                onReset = { resetTarget = child },
                                enabled = !viewModel.isProcessing
                            )
                        }
                    }
                }
            }
            if (noResults && (query.isNotBlank() || filter == "Built-in")) {
                item(key = "no-results") {
                    Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No matching categories", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Try a different name or keyword, or switch to All.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { query = ""; filter = "All" }) { Text("Clear filters") }
                    }
                }
            }
        }
        Surface(tonalElevation = 3.dp) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.operationMessage?.let { message ->
                    Surface(
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = viewModel::clearOperationMessage) {
                                Icon(Icons.Rounded.Close, "Dismiss message")
                            }
                        }
                    }
                }
                if (viewModel.isProcessing) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { confirmClassify = true },
                        enabled = !viewModel.isProcessing
                    ) {
                        Icon(Icons.Rounded.AutoFixHigh, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Auto-sort")
                    }
                    Button(
                        onClick = { PesaActions.playClick(haptic); viewModel.openCreateSheet() },
                        enabled = !viewModel.isProcessing,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("New category")
                    }
                }
            }
        }
    }

    if (uiState.showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeSheet,
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { value ->
                    if (value == SheetValue.Hidden) {
                        viewModel.closeSheet()
                        !viewModel.uiState.showCreateSheet
                    } else true
                }
            ),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            CategoryEditorSheet(
                uiState,
                viewModel::onLabelChange,
                viewModel::onEmojiChange,
                viewModel::onKeywordsChange,
                viewModel::saveCategory,
                viewModel::closeSheet,
                haptic
            )
        }
    }
    deleteTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${category.label}?") },
            text = { Text("Transactions and upcoming payments in this category will move to Other. Its keyword rules and learned recipient matches will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteCategory(category); deleteTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete category") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep category") } }
        )
    }
    resetTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { resetTarget = null },
            title = { Text("Restore built-in details?") },
            text = { Text("Reset the name and icon for ${category.label}. Your transactions will stay in this category.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetSystemCategory(TransactionCategory.valueOf(category.id))
                    resetTarget = null
                }) { Text("Restore defaults") }
            },
            dismissButton = { TextButton(onClick = { resetTarget = null }) { Text("Cancel") } }
        )
    }
    if (confirmClassify) {
        AlertDialog(
            onDismissRequest = { confirmClassify = false },
            title = { Text("Organise uncategorised payments?") },
            text = { Text("Apply your keywords and built-in matching rules to payments in Other. Payments already in another category will not change.") },
            confirmButton = {
                TextButton(onClick = { confirmClassify = false; viewModel.reclassifyAll() }) { Text("Organise payments") }
            },
            dismissButton = { TextButton(onClick = { confirmClassify = false }) { Text("Cancel") } }
        )
    }
    if (viewModel.showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = viewModel::keepEditing,
            title = { Text("Discard your changes?") },
            text = { Text("Your category has not been saved yet.") },
            confirmButton = { TextButton(onClick = viewModel::discardDraft) { Text("Discard changes") } },
            dismissButton = { TextButton(onClick = viewModel::keepEditing) { Text("Keep editing") } }
        )
    }
}

@Composable
private fun CategorySectionTitle(title: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
