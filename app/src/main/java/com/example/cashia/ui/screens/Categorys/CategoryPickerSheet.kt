package com.example.cashia.ui.screens.Categorys

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaActions
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.CustomCategory
import com.example.cashia.models.ResolvedCategory
import java.util.UUID


// ── Inline edit state ────────────────────────────────────────────────────────

private data class InlineEditState(
    val categoryId: String,
    val label: String,
    val emoji: String,
    val keywords: String
)

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
    val haptic = LocalHapticFeedback.current
    rememberCoroutineScope()

    // Inline editing state
    var inlineEdit by remember { mutableStateOf<InlineEditState?>(null) }
    var deleteTarget by remember { mutableStateOf<ResolvedCategory?>(null) }
    var showAddForm by remember { mutableStateOf(false) }

    // New category draft
    var newLabel by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("📦") }
    var newKeywords by remember { mutableStateOf("") }
    var newError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // ── Handle ────────────────────────────────────────────────────
            Box(
                Modifier
                    .size(40.dp, 4.dp)
                    .background(colorScheme.onSurface.copy(alpha = 0.15f), CircleShape)
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Choose Category",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                // Add new category button
                Surface(
                    modifier = Modifier.clickable {
                        PesaActions.playClick(haptic)
                        showAddForm = !showAddForm
                        inlineEdit = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (showAddForm) colorScheme.primary
                    else colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (showAddForm) Icons.Rounded.Close else Icons.Rounded.Add,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = if (showAddForm) colorScheme.onPrimary
                            else colorScheme.primary
                        )
                        Text(
                            if (showAddForm) "Cancel" else "New",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (showAddForm) colorScheme.onPrimary
                            else colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Inline add form ───────────────────────────────────────────
            AnimatedVisibility(
                showAddForm,
                enter = expandVertically(tween(250)) + fadeIn(),
                exit = shrinkVertically(tween(200)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .background(
                            colorScheme.primary.copy(alpha = 0.04f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "NEW CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    // Emoji preview + label row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Emoji badge (tap to cycle)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    // cycle through suggestions
                                    val idx = ALL_EMOJIS.indexOf(newEmoji)
                                    newEmoji = ALL_EMOJIS.getOrElse(idx + 1) { ALL_EMOJIS.first() }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(newEmoji, fontSize = 24.sp)
                        }

                        OutlinedTextField(
                            value = newLabel,
                            onValueChange = { newLabel = it; newError = null },
                            label = { Text("Name") },
                            isError = newError != null,
                            supportingText = newError?.let {
                                {
                                    Text(
                                        it,
                                        color = colorScheme.error
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f),
                                focusedBorderColor = colorScheme.primary
                            )
                        )
                    }

                    OutlinedTextField(
                        value = newKeywords,
                        onValueChange = { newKeywords = it },
                        label = { Text("Keywords (comma-separated)") },
                        placeholder = { Text("e.g. rent, landlord, lease") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f),
                            focusedBorderColor = colorScheme.primary
                        )
                    )

                    // Mini emoji picker
                    EmojiPicker(selected = newEmoji, onSelect = { newEmoji = it })

                    Button(
                        onClick = {
                            if (newLabel.isBlank()) {
                                newError = "Name cannot be empty"
                                return@Button
                            }
                            PesaActions.playSuccess(haptic)
                            val id =
                                "custom_${UUID.randomUUID().toString().replace("-", "").take(8)}"
                            categoryViewModel.saveInlineCategory(
                                CustomCategory(
                                    id = id,
                                    label = newLabel.trim(),
                                    emoji = newEmoji,
                                    keywords = newKeywords.trim()
                                )
                            )
                            // Reset and select the new category
                            onPick(id)
                            showAddForm = false
                            newLabel = ""; newEmoji = "📦"; newKeywords = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create & Select", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showAddForm) Spacer(Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = colorScheme.onSurface.copy(alpha = 0.06f)
            )

            Spacer(Modifier.height(8.dp))

            // ── Category list ─────────────────────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // System categories (non-editable labels, but show clearly)
                val systemCats = allCategories.filter { it.isSystem }
                val customCats = allCategories.filter { !it.isSystem }

                if (customCats.isNotEmpty()) {
                    item {
                        Text(
                            "YOUR CATEGORIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(customCats, key = { "custom_${it.id}" }) { cat ->
                        PickerCategoryRow(
                            cat = cat,
                            isSelected = cat.id == currentCategoryId,
                            isEditing = inlineEdit?.categoryId == cat.id,
                            inlineEdit = inlineEdit,
                            onPick = { onPick(cat.id) },
                            onEdit = {
                                inlineEdit = if (inlineEdit?.categoryId == cat.id) null
                                else InlineEditState(cat.id, cat.label, cat.emoji, "")
                                showAddForm = false
                            },
                            onEditChange = { inlineEdit = it },
                            onSave = { state ->
                                categoryViewModel.saveInlineCategory(
                                    CustomCategory(
                                        id = state.categoryId,
                                        label = state.label.trim(),
                                        emoji = state.emoji,
                                        keywords = state.keywords.trim()
                                    )
                                )
                                inlineEdit = null
                            },
                            onDelete = { deleteTarget = cat },
                        )
                    }

                    item { Spacer(Modifier.height(4.dp)) }
                }

                item {
                    Text(
                        "BUILT-IN",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(systemCats, key = { "sys_${it.id}" }) { cat ->
                    val isSelected = cat.id == currentCategoryId
                    val bgColor by animateColorAsState(
                        if (isSelected) colorScheme.primary.copy(alpha = 0.1f)
                        else colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        label = "catBg"
                    )
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(cat.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = bgColor,
                        border = if (isSelected)
                            BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(cat.emoji, fontSize = 20.sp)
                            Text(
                                cat.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold
                                    else FontWeight.Normal
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check, null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // ── Delete confirmation ────────────────────────────────────────────────
    deleteTarget?.let { cat ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete \"${cat.label}\"?") },
            text = {
                Text(
                    "Transactions in this category will move to \"Other\". Cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryViewModel.deleteResolvedCategory(cat)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

// ── Single row in picker with inline expand-to-edit ───────────────────────────

@Composable
private fun PickerCategoryRow(
    cat: ResolvedCategory,
    isSelected: Boolean,
    isEditing: Boolean,
    inlineEdit: InlineEditState?,
    onPick: () -> Unit,
    onEdit: () -> Unit,
    onEditChange: (InlineEditState?) -> Unit,
    onSave: (InlineEditState) -> Unit,
    onDelete: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    if (isEditing) RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                    else RoundedCornerShape(14.dp)
                )
                .clickable { onPick() },
            shape = if (isEditing) RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
            else RoundedCornerShape(14.dp),
            color = when {
                isEditing -> colorScheme.primary.copy(alpha = 0.08f)
                isSelected -> colorScheme.primary.copy(alpha = 0.1f)
                else -> colorScheme.surfaceVariant.copy(alpha = 0.5f)
            },
            border = when {
                isEditing -> BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.3f))
                isSelected -> BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f))
                else -> null
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (isEditing) (inlineEdit?.emoji ?: cat.emoji) else cat.emoji,
                    fontSize = 20.sp
                )
                Text(
                    if (isEditing) (inlineEdit?.label?.ifBlank { cat.label } ?: cat.label)
                    else cat.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (isSelected && !isEditing) {
                    Icon(
                        Icons.Rounded.Check, null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                // Edit button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEditing) colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isEditing) Icons.Rounded.Close else Icons.Rounded.Edit,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isEditing) colorScheme.primary
                        else colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                // Delete button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = colorScheme.error.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // ── Inline edit form (expands below the row) ──────────────────────
        AnimatedVisibility(
            isEditing && inlineEdit != null,
            enter = expandVertically(tween(220)) + fadeIn(),
            exit = shrinkVertically(tween(180)) + fadeOut()
        ) {
            val state = inlineEdit ?: return@AnimatedVisibility
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colorScheme.primary.copy(alpha = 0.04f),
                        RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.label,
                        onValueChange = { onEditChange(state.copy(label = it)) },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f),
                            focusedBorderColor = colorScheme.primary
                        )
                    )
                    // Emoji preview
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                colorScheme.primaryContainer.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(state.emoji, fontSize = 22.sp)
                    }
                }

                OutlinedTextField(
                    value = state.keywords,
                    onValueChange = { onEditChange(state.copy(keywords = it)) },
                    label = { Text("Keywords") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 2,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedBorderColor = colorScheme.primary
                    )
                )

                EmojiPicker(
                    selected = state.emoji,
                    onSelect = { onEditChange(state.copy(emoji = it)) }
                )

                Button(
                    onClick = { onSave(state) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Save Changes",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}