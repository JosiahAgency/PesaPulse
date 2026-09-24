package com.example.cashia.ui.screens.category

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.cashia.models.CategoryManageState
import com.example.cashia.utility.EmojiPicker
import com.example.cashia.utility.PesaActions

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
    val isSystem = uiState.editingSystemCategory != null
    val isEditing = isSystem || uiState.editingCategory != null
    var showEmojiPicker by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val keywords = uiState.draftKeywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    BackHandler { onDismiss() }

    Column(
        Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding().navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isEditing) "Make it yours" else "Create a category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isSystem) "Personalise a built-in category" else "A little organisation goes a long way",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss, enabled = !uiState.isSaving) {
                Icon(Icons.Rounded.Close, "Close category editor")
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                onClick = { showEmojiPicker = !showEmojiPicker },
                enabled = !uiState.isSaving,
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(uiState.draftEmoji.ifBlank { "📦" }, fontSize = 36.sp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            uiState.draftLabel.ifBlank { "Your category" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (showEmojiPicker) "Hide icon picker" else "Tap to choose an icon",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            AnimatedVisibility(showEmojiPicker && !uiState.isSaving) {
                EmojiPicker(
                    selected = uiState.draftEmoji,
                    onSelect = { onEmojiChange(it); showEmojiPicker = false }
                )
            }
            OutlinedTextField(
                value = uiState.draftLabel,
                onValueChange = onLabelChange,
                enabled = !uiState.isSaving,
                label = { Text("Category name") },
                placeholder = { Text("e.g. Family or Side hustle") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = if (isSystem) ImeAction.Done else ImeAction.Next
                )
            )
            if (isSystem) {
                Text(
                    "Only the name and icon change. Built-in matching rules and existing transactions stay intact. You can restore the original details at any time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Let keywords do the sorting", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Optional. A recipient containing any of these words matches this category. Separate words or phrases with commas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = uiState.draftKeywords,
                        onValueChange = onKeywordsChange,
                        enabled = !uiState.isSaving,
                        label = { Text("Matching keywords") },
                        placeholder = { Text("e.g. mum, school fees, family") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    if (keywords.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            keywords.forEach { keyword ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(keyword, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                    Text(
                        if (keywords.isEmpty()) "No keywords? You can assign payments to this category yourself."
                        else "Saving also applies these keywords to payments currently in Other.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                if (isSystem) "Built-in category" else "Your custom category",
                modifier = Modifier.padding(bottom = 16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(tonalElevation = 3.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = {
                        keyboard?.hide()
                        focus.clearFocus()
                        PesaActions.playClick(haptic)
                        onSave()
                    },
                    enabled = !uiState.isSaving && uiState.draftLabel.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isEditing) "Save changes" else "Create category")
                    }
                }
            }
        }
    }
}
