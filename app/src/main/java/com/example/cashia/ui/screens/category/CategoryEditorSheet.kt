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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
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
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null)
            }
        }

        // ── Name field (hidden for system edits) ──────────────────────────
        if (!isSystemEdit) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(
                            colorScheme.primaryContainer.copy(alpha = 0.35f),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            colorScheme.primary.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(previewEmoji, fontSize = 26.sp)
                }
                OutlinedTextField(
                    value = uiState.draftLabel,
                    onValueChange = onLabelChange,
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Family Expenses") },
                    isError = uiState.error != null,
                    supportingText = uiState.error?.let {
                        { Text(it, color = colorScheme.error) }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f)
                    )
                )
            }
        }

        // ── Keywords ───────────────────────────────────────────────────────
        OutlinedTextField(
            value = uiState.draftKeywords,
            onValueChange = onKeywordsChange,
            label = {
                Text(if (isSystemEdit) "Extra Keywords (optional)" else "Auto-match Keywords (optional)")
            },
            placeholder = { Text("e.g. wife, mum, school, church") },
            supportingText = {
                Text(
                    if (isSystemEdit)
                        "Transactions whose recipient contains any of these words will be assigned here"
                    else
                        "Comma-separated — transactions matching any keyword are auto-assigned here",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f)
            )
        )

        // ── Emoji picker (hidden for system edits) ─────────────────────────
        if (!isSystemEdit) {
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
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
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