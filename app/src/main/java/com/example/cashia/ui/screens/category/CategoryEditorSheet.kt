package com.example.cashia.ui.screens.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.utility.EmojiPicker
import com.example.cashia.utility.PesaActions
import com.example.cashia.models.CategoryManageState


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryEditorSheet(
    uiState: CategoryManageState,
    onLabelChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onKeywordsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    haptic: HapticFeedback
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.isImeVisible
    val isEditing = uiState.editingCategory != null
    val isSystemEdit = uiState.editingSystemCategory != null

    val colorScheme = MaterialTheme.colorScheme
    val previewEmoji = uiState.draftEmoji.ifBlank { "📦" }

    BackHandler(enabled = isKeyboardVisible) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Title row ─────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    isSystemEdit -> "Edit Built-in Category"
                    isEditing -> "Edit Category"
                    else -> "New Category"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null)
            }
        }

        // ── Name field (hidden for system edits) ──────────────────────────
        if (!isSystemEdit) {
            OutlinedTextField(
                value = uiState.draftLabel,
                onValueChange = onLabelChange,
                label = { Text("Category Name") },
                placeholder = { Text("e.g. Family Expenses") },
                isError = uiState.error != null,
                supportingText = uiState.error?.let {
                    { Text(it, color = colorScheme.error) }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )
        }

        // ── Keywords field ────────────────────────────────────────────────
        if (!isSystemEdit) {
            OutlinedTextField(
                value = uiState.draftKeywords,
                onValueChange = onKeywordsChange,
                label = { Text("Keywords (comma-separated)") },
                placeholder = { Text("e.g. wife, mum, family, home") },
                supportingText = {
                    Text(
                        "Transactions whose recipient matches any keyword get auto-assigned here",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        } else {
            // System categories only support keyword override
            OutlinedTextField(
                value = uiState.draftKeywords,
                onValueChange = onKeywordsChange,
                label = { Text("Additional Keywords") },
                placeholder = { Text("Extra words to match this category") },
                supportingText = {
                    Text(
                        "Add extra keywords to improve auto-detection for this built-in category",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )
        }

        // ── Emoji picker ──────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large preview badge
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            colorScheme.primaryContainer.copy(alpha = 0.4f),
                            RoundedCornerShape(18.dp)
                        )
                        .border(
                            1.dp,
                            colorScheme.primary.copy(alpha = 0.25f),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(previewEmoji, fontSize = 30.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ICON",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Browse by category or search below",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Full tabbed emoji picker
            EmojiPicker(
                selected = uiState.draftEmoji,
                onSelect = onEmojiChange
            )
        }

        // ── Save button ───────────────────────────────────────────────────
        Button(
            onClick = {
                PesaActions.playSuccess(haptic)
                onSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Check, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    isSystemEdit -> "Save Customisation"
                    isEditing -> "Save Changes"
                    else -> "Create Category"
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}