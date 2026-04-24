package com.example.cashia.ui.screens.category

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
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.CustomCategory
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.SystemCategoryOverride
import com.example.cashia.models.TransactionCategory
import com.example.cashia.utility.PesaActions

private sealed class CatListItem {
    data class Header(val title: String) : CatListItem()
    data class CustomRow(val cat: CustomCategory) : CatListItem()
    data class SystemParent(val cat: ResolvedCategory, val override: SystemCategoryOverride?) :
        CatListItem()

    data class SystemChild(val cat: ResolvedCategory, val override: SystemCategoryOverride?) :
        CatListItem()

    object Spacer : CatListItem()
    object ReclassifyButton : CatListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: CategoryViewModel
) {
    val customCategories by viewModel.customCategories.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val uiState = viewModel.uiState
    val haptic = LocalHapticFeedback.current
    val systemOverrides by viewModel.systemOverrides.collectAsState()
    var deleteTarget by remember { mutableStateOf<CustomCategory?>(null) }

    val listItems = remember(customCategories, allCategories, systemOverrides) {
        buildList {
            add(CatListItem.ReclassifyButton)
            if (customCategories.isNotEmpty()) {
                add(CatListItem.Header("Your Custom Categories"))
                customCategories.forEach { add(CatListItem.CustomRow(it)) }
                add(CatListItem.Spacer)
            }
            add(CatListItem.Header("Built-in Categories"))
            val topLevel = viewModel.topLevelCategories(allCategories).filter { it.isSystem }
            topLevel.forEach { parent ->
                TransactionCategory.entries.find { it.name == parent.id }
                add(CatListItem.SystemParent(parent, systemOverrides[parent.id]))
                val children =
                    viewModel.childCategories(allCategories, parent.id).filter { it.isSystem }
                children.forEach { child ->
                    add(CatListItem.SystemChild(child, systemOverrides[child.id]))
                }
            }
            add(CatListItem.Spacer)
        }
    }

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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(listItems, key = { item ->
                when (item) {
                    is CatListItem.Header -> "header_${item.title}"
                    is CatListItem.CustomRow -> "custom_${item.cat.id}"
                    is CatListItem.SystemParent -> "parent_${item.cat.id}"
                    is CatListItem.SystemChild -> "child_${item.cat.id}"
                    is CatListItem.Spacer -> "spacer_${listItems.indexOf(item)}"
                    CatListItem.ReclassifyButton -> "reclassify"
                }
            }) { item ->
                when (item) {
                    CatListItem.ReclassifyButton -> {
                        Column {
                            ReclassifyBanner(
                                {
                                    PesaActions.playSuccess(haptic)
                                    viewModel.reclassifyAll()
                                }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    is CatListItem.Header -> SectionHeader(item.title)
                    is CatListItem.CustomRow -> {
                        CustomCategoryRow(
                            item.cat,
                            {
                                PesaActions.playClick(haptic)
                                viewModel.openEditSheet(item.cat)
                            },
                            {
                                PesaActions.playClick(haptic)
                                deleteTarget = item.cat
                            }
                        )
                    }

                    is CatListItem.SystemParent -> {
                        val ov = item.override
                        SystemCategoryRow(
                            ov?.emoji ?: item.cat.emoji,
                            ov?.label ?: item.cat.label,
                            ov != null,
                            false,
                            {
                                PesaActions.playClick(haptic)
                                val enumCat =
                                    TransactionCategory.entries.find { it.name == item.cat.id }
                                        ?: return@SystemCategoryRow
                                viewModel.openEditSystemSheet(enumCat)
                            },
                            if (ov != null) {
                                {
                                    PesaActions.playClick(haptic)
                                    val enumCat =
                                        TransactionCategory.entries.find { it.name == item.cat.id }
                                            ?: return@SystemCategoryRow
                                    viewModel.resetSystemCategory(enumCat)
                                }
                            } else null
                        )
                    }

                    is CatListItem.SystemChild -> {
                        val ov = item.override
                        SystemCategoryRow(
                            ov?.emoji ?: item.cat.emoji,
                            ov?.label ?: item.cat.label,
                            ov != null,
                            true,
                            {
                                PesaActions.playClick(haptic)
                                val enumCat =
                                    TransactionCategory.entries.find { it.name == item.cat.id }
                                        ?: return@SystemCategoryRow
                                viewModel.openEditSystemSheet(enumCat)
                            },
                            if (ov != null) {
                                {
                                    PesaActions.playClick(haptic)
                                    val enumCat =
                                        TransactionCategory.entries.find { it.name == item.cat.id }
                                            ?: return@SystemCategoryRow
                                    viewModel.resetSystemCategory(enumCat)
                                }
                            } else null
                        )
                    }

                    is CatListItem.Spacer -> Spacer(Modifier.height(6.dp))
                }
            }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
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
        modifier = Modifier.padding(4.dp)
    )
}

@Composable
private fun ReclassifyBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp), verticalAlignment = Alignment.CenterVertically,
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
                    "Auto-classify Transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

