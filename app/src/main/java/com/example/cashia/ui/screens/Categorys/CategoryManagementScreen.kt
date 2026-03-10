package com.example.cashia.ui.screens.Categorys

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaActions
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.CustomCategory
import com.example.cashia.models.TransactionCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: CategoryViewModel
) {
    val customCategories by viewModel.customCategories.collectAsState()
    val uiState = viewModel.uiState
    val haptic = LocalHapticFeedback.current
    val systemOverrides by viewModel.systemOverrides.collectAsState()

    var deleteTarget by remember { mutableStateOf<CustomCategory?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    PesaActions.playClick(haptic)
                    viewModel.openCreateSheet()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, "Add category")
            }
        }
    ) { _ ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Re-classify button ─────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PesaActions.playSuccess(haptic)
                                viewModel.reclassifyAll()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoFixHigh,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Auto-classify transactions",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Re-runs smart detection on uncategorized transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Custom categories section ──────────────────────────────
            if (customCategories.isNotEmpty()) {
                item {
                    SectionHeader("Your Custom Categories")
                }
                items(customCategories, key = { it.id }) { cat ->
                    CustomCategoryRow(
                        category = cat,
                        onEdit = {
                            PesaActions.playClick(haptic)
                            viewModel.openEditSheet(cat)
                        },
                        onDelete = {
                            PesaActions.playClick(haptic)
                            deleteTarget = cat
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── System categories section ──────────────────────────────
            item { SectionHeader("Built-in Categories") }
            items(TransactionCategory.entries) { cat ->
                val override = systemOverrides[cat.name]
                SystemCategoryRow(
                    emoji = override?.emoji ?: cat.emoji,
                    label = override?.label ?: cat.label,
                    isModified = override != null,
                    onEdit = {
                        PesaActions.playClick(haptic)
                        viewModel.openEditSystemSheet(cat)
                    },
                    onReset = if (override != null) {
                        {
                            PesaActions.playClick(haptic)
                            viewModel.resetSystemCategory(cat)
                        }
                    } else null
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }

        // ── Bottom sheet: create / edit ────────────────────────────────
        val freshSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        if (uiState.showCreateSheet) {
            ModalBottomSheet(
                onDismissRequest = viewModel::closeSheet,
                sheetState = freshSheetState,
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

                    CategoryEditorSheet(
                        uiState = uiState,
                        onLabelChange = viewModel::onLabelChange,
                        onEmojiChange = viewModel::onEmojiChange,
                        onKeywordsChange = viewModel::onKeywordsChange,
                        onSave = viewModel::saveCategory,
                        onDismiss = viewModel::closeSheet,
                        haptic = haptic
                    )
                }

            }
        }

        // ── Delete confirmation ────────────────────────────────────────
        deleteTarget?.let { cat ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete \"${cat.label}\"?") },
                text = {
                    Text(
                        "All transactions in this category will be moved to \"Other\". This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCategory(cat)
                            deleteTarget = null
                        },
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
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

