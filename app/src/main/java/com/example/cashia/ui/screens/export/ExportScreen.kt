package com.example.cashia.ui.screens.export

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.ExportViewModel
import com.example.cashia.ui.reusedElements.DateRangePickerDialog
import com.example.cashia.ui.reusedElements.HarmonizedActionFab
import com.example.cashia.ui.reusedElements.SectionCard
import com.example.cashia.ui.theme.screenContentPadding
import com.example.cashia.utility.ExportColumn
import com.example.cashia.utility.PesaActions
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Lets the user pick which columns and which period to export the M-PESA history as CSV. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    categoryViewModel: CategoryViewModel,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val categoryLabelMap = remember(allCategories) { allCategories.associate { it.id to it.label } }

    val today = remember { LocalDate.now() }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.exportTo(uri, categoryLabelMap, defaultFileName())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.exportEvent.collect { event ->
            when (event) {
                is ExportViewModel.ExportEvent.Success -> {
                    PesaActions.playSuccess(haptic)
                    Toast.makeText(
                        context,
                        "Exported ${event.count} transaction${if (event.count == 1) "" else "s"} to ${event.fileName}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is ExportViewModel.ExportEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    var showCustomRangeDialog by remember { mutableStateOf(false) }

    if (showCustomRangeDialog) {
        DateRangePickerDialog(
            initialStart = viewModel.customStart ?: today.minusDays(30),
            initialEnd = viewModel.customEnd ?: today,
            onDismiss = { showCustomRangeDialog = false },
            onConfirm = { start, end ->
                viewModel.setCustomRange(start, end)
                showCustomRangeDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = screenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SectionCard("Period") {
                Column(modifier = Modifier.padding(16.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ExportViewModel.PeriodPreset.entries) { preset ->
                            FilterChip(
                                selected = viewModel.selectedPreset == preset,
                                onClick = {
                                    PesaActions.playClick(haptic)
                                    if (preset == ExportViewModel.PeriodPreset.CUSTOM) {
                                        showCustomRangeDialog = true
                                    } else {
                                        viewModel.selectPreset(preset)
                                    }
                                },
                                label = { Text(preset.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                )
                            )
                        }
                    }
                    if (viewModel.selectedPreset == ExportViewModel.PeriodPreset.CUSTOM) {
                        Spacer(Modifier.height(12.dp))
                        val fmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }
                        Text(
                            text = if (viewModel.customStart != null && viewModel.customEnd != null) {
                                "${viewModel.customStart!!.format(fmt)} — ${
                                    viewModel.customEnd!!.format(
                                        fmt
                                    )
                                }"
                            } else {
                                "Tap to choose a custom date range"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { showCustomRangeDialog = true }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = viewModel.matchingCount?.let { count ->
                            "$count transaction${if (count == 1) "" else "s"} match this period"
                        } ?: "Counting matching transactions…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            SectionCard("Columns") {
                ExportColumn.entries.forEach { column ->
                    val checked = column in viewModel.selectedColumns
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                PesaActions.playClick(haptic)
                                viewModel.toggleColumn(column)
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            column.header,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Checkbox(checked = checked, onCheckedChange = {
                            PesaActions.playClick(haptic)
                            viewModel.toggleColumn(column)
                        })
                    }
                }
            }
        }

        item {
            HarmonizedActionFab(
                icon = if (viewModel.isExporting) Icons.Rounded.Checklist else Icons.Rounded.FileDownload,
                text = if (viewModel.isExporting) "Preparing export…" else "Export as CSV",
                contentDescription = "Export as CSV",
                onClick = {
                    if (!viewModel.isExporting) {
                        PesaActions.playSuccess(haptic)
                        createDocumentLauncher.launch(defaultFileName())
                    }
                },
                fullWidth = true
            )
        }

        if (viewModel.isExporting) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

private fun defaultFileName(): String {
    val stamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return "pesapulse_transactions_$stamp.csv"
}
