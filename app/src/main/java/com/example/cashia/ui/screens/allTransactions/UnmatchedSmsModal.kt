package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.screens.category.CategoryPickerSheet
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.partialFieldsFromJson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UnmatchedSmsModal(
    sms: UnmatchedSms,
    pendingCount: Int,
    allCategories: List<ResolvedCategory>,
    categoryViewModel: CategoryViewModel,
    onSave: (type: String, category: String) -> Unit,
    onSkip: () -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val partial = remember(sms.extractedFields) { partialFieldsFromJson(sms.extractedFields) }

    // [OPT] These three were recomputed on every recomposition; memoize on their inputs
    val hasPartialData = remember(partial) {
        partial != null && (partial.amount != null || partial.date != null || partial.transactionId != null)
    }
    val confidenceLabel = remember(sms.confidence) {
        when {
            sms.confidence >= 0.65f -> "Partial match · some fields extracted"
            sms.confidence >= 0.35f -> "Low confidence · verify details"
            else -> "Unrecognised · fill in manually"
        }
    }
    val confidenceColor: Color = remember(sms.confidence, colorScheme) {
        when {
            sms.confidence >= 0.65f -> colorScheme.tertiary
            sms.confidence >= 0.35f -> colorScheme.secondary
            else -> colorScheme.onSurfaceVariant
        }
    }

    var selectedType by remember(sms.id) { mutableStateOf(partial?.likelyType ?: "expense") }
    var selectedCategory by remember(sms.id) {
        mutableStateOf(allCategories.firstOrNull()?.id ?: "")
    }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showIgnoreConfirm by remember { mutableStateOf(false) }

    val resolvedCategory = remember(selectedCategory, allCategories) {
        allCategories.find { it.id == selectedCategory }
    }
    val receivedDate = remember(sms.receivedAt) {
        SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(sms.receivedAt))
    }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            allCategories = allCategories,
            currentCategoryId = selectedCategory,
            onPick = { newId -> selectedCategory = newId; showCategoryPicker = false },
            onDismiss = { showCategoryPicker = false },
            categoryViewModel = categoryViewModel,
        )
    }

    // [OPT] Fixed AnimatedVisibility(visible = true) anti-pattern. When visible starts
    //        as true, Compose skips the enter animation entirely — the dialog just pops
    //        in without the intended fade+scale. Drive it via a LaunchedEffect instead.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.92f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Header ─────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        colorScheme.primary.copy(alpha = 0.12f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.HelpOutline,
                                    tint = colorScheme.primary,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                SectionLabel("unrecognised message")
                                if (pendingCount > 1) Subtitles("$pendingCount messages need review")
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.background(colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // ── Raw SMS body ───────────────────────────────────────
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                0.5.dp,
                                colorScheme.onSurface.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = sms.rawBody,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 18.sp
                                ),
                                color = colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Subtitles("Received $receivedDate")
                        }
                    }

                    // ── Confidence / partial data banner ───────────────────
                    if (hasPartialData || sms.confidence > 0f) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = confidenceColor.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    0.5.dp,
                                    confidenceColor.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoFixHigh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = confidenceColor
                                    )
                                    Text(
                                        confidenceLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = confidenceColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (hasPartialData) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        partial?.amount?.let {
                                            ExtractedFieldPill(
                                                "${PesaTimeConfiguration.CURRENCY} $it",
                                                confidenceColor
                                            )
                                        }
                                        partial?.date?.let {
                                            ExtractedFieldPill(
                                                it,
                                                confidenceColor
                                            )
                                        }
                                        partial?.likelyType?.let {
                                            ExtractedFieldPill(
                                                if (it == "income") "💰 income" else "💸 expense",
                                                confidenceColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.07f))

                    // ── Type toggle ────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Transaction type",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("income" to "💰 Income", "expense" to "💸 Expense")
                                .forEach { (value, label) ->
                                    val selected = selectedType == value
                                    val chipColor =
                                        if (value == "income") colorScheme.tertiary else colorScheme.error
                                    FilterChip(
                                        selected = selected,
                                        onClick = { selectedType = value },
                                        label = {
                                            Text(
                                                label,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = chipColor.copy(alpha = 0.15f),
                                            selectedLabelColor = chipColor,
                                            selectedLeadingIconColor = chipColor
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true, selected = selected,
                                            selectedBorderColor = chipColor.copy(alpha = 0.4f),
                                            borderColor = colorScheme.onSurface.copy(alpha = 0.12f)
                                        )
                                    )
                                }
                        }
                    }

                    // ── Category picker ────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Category",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurfaceVariant
                        )
                        Surface(
                            onClick = { showCategoryPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.primary.copy(alpha = 0.07f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    0.5.dp,
                                    colorScheme.primary.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(resolvedCategory?.emoji ?: "❓", fontSize = 20.sp)
                                Text(
                                    resolvedCategory?.label ?: "Pick a category",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "Change →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // ── Actions ────────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Skip", fontWeight = FontWeight.SemiBold) }

                        Button(
                            onClick = { onSave(selectedType, selectedCategory) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = selectedCategory.isNotBlank()
                        ) { Text("Save", fontWeight = FontWeight.Bold) }
                    }

                    if (!showIgnoreConfirm) {
                        TextButton(
                            onClick = { showIgnoreConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(15.dp)
                                    .padding(end = 4.dp),
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Ignore this message",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.errorContainer.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    0.5.dp,
                                    colorScheme.error.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Permanently ignore?",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onErrorContainer
                                )
                                Text(
                                    "This message won't appear again. Use this for failed transactions, promos, or duplicate alerts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onErrorContainer.copy(alpha = 0.75f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showIgnoreConfirm = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Cancel") }

                                    Button(
                                        onClick = onIgnore,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colorScheme.error
                                        )
                                    ) { Text("Ignore", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractedFieldPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}