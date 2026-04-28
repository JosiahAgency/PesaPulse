package com.example.cashia.ui.screens.category

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.CustomCategory
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.utility.EmojiPicker
import java.util.UUID


private val INCOME_IDS = setOf(
    "SALARY", "BUSINESS_INCOME", "RECEIVED_MONEY",
    "LOAN_DISBURSEMENT", "INVESTMENT_RETURNS", "GOVERNMENT_REFUND"
)
private val HIDDEN_IDS = setOf("OTHER", "BALANCE_INQUIRY")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    allCategories: List<ResolvedCategory>,
    currentCategoryId: String,
    categoryViewModel: CategoryViewModel,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme

    var searchQuery by remember { mutableStateOf("") }
    var drilldownParent by remember { mutableStateOf<ResolvedCategory?>(null) }
    var showAddForm by remember { mutableStateOf(false) }

    // ── Add-form draft state ───────────────────────────────────────────────
    var newLabel by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("📦") }
    var newKeywords by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }

    // ── Derived lists ─────────────────────────────────────────────────────
    val customCats = remember(allCategories) { allCategories.filter { !it.isSystem } }
    val systemTop = remember(allCategories) {
        categoryViewModel.topLevelCategories(allCategories).filter { it.isSystem }
    }
    val spendingCats = remember(systemTop) {
        systemTop.filter { it.id !in INCOME_IDS && it.id !in HIDDEN_IDS }
    }
    val incomeCats = remember(systemTop) { systemTop.filter { it.id in INCOME_IDS } }

    val searchResults = remember(searchQuery, allCategories) {
        if (searchQuery.isBlank()) emptyList()
        else allCategories.filter {
            it.id !in HIDDEN_IDS &&
                    it.label.contains(searchQuery, ignoreCase = true)
        }
    }
    BackHandler(enabled = drilldownParent != null) { drilldownParent = null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(drilldownParent, label = "header") { parent ->
                    if (parent != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { drilldownParent = null }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            Text(
                                text = "${parent.emoji}  ${parent.label}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Category",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    "Pick one, or create your own",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalIconButton(
                                onClick = {
                                    showAddForm = !showAddForm
                                    if (!showAddForm) {
                                        newLabel = ""; newEmoji = "📦"; newKeywords = ""; addError =
                                            null
                                    }
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (showAddForm) colorScheme.errorContainer
                                    else colorScheme.primaryContainer,
                                    contentColor = if (showAddForm) colorScheme.error
                                    else colorScheme.primary
                                )
                            ) {
                                Icon(
                                    if (showAddForm) Icons.Rounded.Close else Icons.Rounded.Add,
                                    null
                                )
                            }
                        }
                    }
                }
            }

            // ── Inline add form ───────────────────────────────────────────
            AnimatedVisibility(
                showAddForm && drilldownParent == null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(newEmoji, fontSize = 28.sp)
                        OutlinedTextField(
                            value = newLabel,
                            onValueChange = { newLabel = it; addError = null },
                            label = { Text("Category name") },
                            isError = addError != null,
                            supportingText = addError?.let {
                                {
                                    Text(
                                        it,
                                        color = colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                        )
                    }

                    EmojiPicker(selected = newEmoji, onSelect = { newEmoji = it })

                    OutlinedTextField(
                        value = newKeywords,
                        onValueChange = { newKeywords = it },
                        label = { Text("Auto-match keywords (optional)") },
                        placeholder = { Text("e.g. wife, mum, home") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f)
                        )
                    )

                    OutlinedButton(
                        onClick = {
                            if (newLabel.isBlank()) {
                                addError = "Name is required"
                            } else {
                                val id = "custom_${UUID.randomUUID().toString().take(8)}"
                                categoryViewModel.saveInlineCategory(
                                    CustomCategory(
                                        id,
                                        newLabel.trim(),
                                        newEmoji,
                                        newKeywords.trim()
                                    )
                                )
                                onPick(id)
                                showAddForm = false
                                newLabel = ""; newEmoji = "📦"; newKeywords = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create Category", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Search bar ────────────────────────────────────────────────
            AnimatedVisibility(
                visible = drilldownParent == null && !showAddForm,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search categories…",
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search, null,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, null, Modifier.size(16.dp))
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.1f),
                        focusedBorderColor = colorScheme.primary
                    )
                )
            }


            // ── Category list ─────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                when {
                    drilldownParent != null -> {
                        val parent = drilldownParent!!
                        val children = categoryViewModel.childCategories(allCategories, parent.id)

                        item {
                            PickerRow(
                                emoji = parent.emoji,
                                label = "All ${parent.label}",
                                isSelected = currentCategoryId == parent.id,
                                dimLabel = true,
                                onClick = { onPick(parent.id) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = colorScheme.onSurface.copy(alpha = 0.06f)
                            )
                        }
                        items(children, key = { it.id }) { child ->
                            PickerRow(
                                emoji = child.emoji,
                                label = child.label,
                                isSelected = currentCategoryId == child.id,
                                onClick = { onPick(child.id) }
                            )
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }

                    searchQuery.isNotBlank() -> {
                        if (searchResults.isEmpty()) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔍", fontSize = 32.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "No categories match",
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(searchResults, key = { it.id }) { cat ->
                                PickerRow(
                                    emoji = cat.emoji,
                                    label = cat.label,
                                    isSelected = currentCategoryId == cat.id,
                                    onClick = { onPick(cat.id) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }

                    // ── Normal browsing ───────────────────────────────────
                    else -> {
                        // Custom categories (if any)
                        if (customCats.isNotEmpty()) {
                            item { PickerSectionLabel("My Categories") }
                            items(customCats, key = { it.id }) { cat ->
                                PickerRow(
                                    emoji = cat.emoji,
                                    label = cat.label,
                                    isSelected = currentCategoryId == cat.id,
                                    onClick = { onPick(cat.id) }
                                )
                            }
                            item { Spacer(Modifier.height(8.dp)) }
                        }

                        // Spending
                        item { PickerSectionLabel("Spending") }
                        items(spendingCats, key = { it.id }) { cat ->
                            val children = categoryViewModel.childCategories(allCategories, cat.id)
                            val childSelected = children.any { it.id == currentCategoryId }
                            PickerRow(
                                emoji = cat.emoji,
                                label = cat.label,
                                isSelected = currentCategoryId == cat.id || childSelected,
                                hasChildren = children.isNotEmpty(),
                                onClick = {
                                    if (children.isNotEmpty()) drilldownParent = cat
                                    else onPick(cat.id)
                                }
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }

                        // Income
                        item { PickerSectionLabel("Income") }
                        items(incomeCats, key = { it.id }) { cat ->
                            PickerRow(
                                emoji = cat.emoji,
                                label = cat.label,
                                isSelected = currentCategoryId == cat.id,
                                onClick = { onPick(cat.id) }
                            )
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            PickerRow(
                                emoji = "📦", label = "Other",
                                isSelected = currentCategoryId == "OTHER",
                                onClick = { onPick("OTHER") }
                            )
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    emoji: String,
    label: String,
    isSelected: Boolean,
    hasChildren: Boolean = false,
    dimLabel: Boolean = false,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.45f)
        else colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(emoji, fontSize = 20.sp)
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        isSelected -> colorScheme.primary
                        dimLabel -> colorScheme.onSurface.copy(alpha = 0.55f)
                        else -> colorScheme.onSurface
                    }
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            when {
                hasChildren -> Icon(
                    Icons.Rounded.ChevronRight, null,
                    Modifier.size(20.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )

                isSelected -> Icon(
                    Icons.Rounded.Check, null,
                    Modifier.size(18.dp),
                    tint = colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PickerSectionLabel(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 0.9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)
    )
}