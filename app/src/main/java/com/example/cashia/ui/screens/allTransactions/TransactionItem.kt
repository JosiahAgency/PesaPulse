package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.ReversalIndex
import com.example.cashia.models.TransactionCategory
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.screens.category.CategoryPickerSheet
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.formatAmount

// ─────────────────────────────────────────────────────────────────────────────
// Entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TransactionItem(
    tx: MpesaTransactions,
    allCategories: List<ResolvedCategory>,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel,
    hapticFeedback: HapticFeedback,
    reversalIndex: ReversalIndex,
) {
    val isReversalTx = tx.reversedTxId != null
    val originalTx = if (isReversalTx) reversalIndex.byReversal[tx.id] else null
    val reversalOfThis = reversalIndex.byOriginal[tx.id]

    if (isReversalTx) {
        ReversalPairItem(
            reversalTx = tx,
            originalTx = originalTx,
            hapticFeedback = hapticFeedback,
        )
    } else {
        NormalTransactionItem(
            tx = tx,
            wasReversed = reversalOfThis != null,
            reversalTx = reversalOfThis,
            allCategories = allCategories,
            categoryViewModel = categoryViewModel,
            upcomingViewModel = upcomingViewModel,
            hapticFeedback = hapticFeedback,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reversal pair
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReversalPairItem(
    reversalTx: MpesaTransactions,
    originalTx: MpesaTransactions?,
    hapticFeedback: HapticFeedback,
) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val accentColor = colorScheme.tertiary

    val glassAlpha = remember(colorScheme.background) {
        if (colorScheme.background.luminance() > 0.5f) 0.6f else 0.4f
    }

    val formattedAmount = remember(reversalTx.amount) { formatAmount(reversalTx.amount) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(
            if (expanded) 1.dp else 0.5.dp,
            if (expanded) accentColor.copy(alpha = 0.3f)
            else colorScheme.onSurface.copy(alpha = 0.08f)
        ),
        onClick = { expanded = !expanded }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape)
                ) {
                    Text("🔄", fontSize = 21.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Transaction Reversed",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        maxLines = 1,
                    )
                    originalTx?.let {
                        Text(
                            "Original: ${it.recipientName.ifBlank { it.recipient }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Subtitles("${reversalTx.displayDate} · ${reversalTx.displayTime}")
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "+$formattedAmount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum"
                        ),
                        color = accentColor
                    )
                    Subtitles(PesaTimeConfiguration.CURRENCY)
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(180)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.04f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.1f))

                    Text(
                        "REVERSAL",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                        color = accentColor.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                    InlineDetailRow("REF", reversalTx.reference ?: "Unknown", monospace = true)
                    InlineDetailRow(
                        "AMOUNT CREDITED",
                        PesaTimeConfiguration.formatCurrency(reversalTx.amount),
                        bold = true
                    )
                    if (reversalTx.balance != null) {
                        InlineDetailRow(
                            "BALANCE AFTER",
                            PesaTimeConfiguration.formatCurrency(reversalTx.balance),
                            bold = true
                        )
                    }

                    originalTx?.let { orig ->
                        HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))
                        Text(
                            "ORIGINAL TRANSACTION",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        InlineDetailRow(
                            "REF",
                            orig.reference ?: "Unknown",
                            monospace = true,
                            strikethrough = true
                        )
                        InlineDetailRow(
                            "RECIPIENT",
                            orig.recipientName.ifBlank { orig.recipient },
                            strikethrough = true
                        )
                        InlineDetailRow(
                            "AMOUNT",
                            PesaTimeConfiguration.formatCurrency(orig.amount),
                            strikethrough = true
                        )
                        InlineDetailRow(
                            "DATE",
                            "${orig.displayDate} · ${orig.displayTime}",
                            strikethrough = true
                        )
                    } ?: run {
                        Text(
                            "Original transaction ${reversalTx.reversedTxId} not found in history",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        reversalTx.reference?.let { reference ->
                            QuickActionChip(
                                label = "Reversal Ref",
                                icon = Icons.Rounded.ContentCopy,
                                modifier = Modifier.weight(1f)
                            ) {
                                PesaActions.copyToClipboard(
                                    context, clipboard, hapticFeedback,
                                    "Reversal Reference", reference
                                )
                            }
                        }
                        originalTx?.reference?.let { reference ->
                            QuickActionChip(
                                label = "Original Ref",
                                icon = Icons.Rounded.ContentCopy,
                                modifier = Modifier.weight(1f)
                            ) {
                                PesaActions.copyToClipboard(
                                    context, clipboard, hapticFeedback,
                                    "Original Reference", reference
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Normal transaction item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NormalTransactionItem(
    tx: MpesaTransactions,
    wasReversed: Boolean,
    reversalTx: MpesaTransactions?,
    allCategories: List<ResolvedCategory>,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel,
    hapticFeedback: HapticFeedback,
) {
    var expanded by remember { mutableStateOf(false) }
    var currentCategoryId by remember(tx.id) { mutableStateOf(tx.category) }

    // [OPT] Fallback is stable — only recompute when the id or list changes
    val resolvedCategory = remember(currentCategoryId, allCategories) {
        allCategories.find { it.id == currentCategoryId }
            ?: ResolvedCategory(
                TransactionCategory.OTHER.name,
                TransactionCategory.OTHER.label,
                TransactionCategory.OTHER.emoji,
                isSystem = true
            )
    }

    var showCategoryPicker by remember { mutableStateOf(false) }

    if (showCategoryPicker) {
        CategoryPickerSheet(
            allCategories,
            currentCategoryId = currentCategoryId,
            onPick = { newId ->
                currentCategoryId = newId
                categoryViewModel.updateTransactionCategory(tx.id, newId)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
            categoryViewModel = categoryViewModel,
        )
    }

    val isIncome = tx.type == "income"
    val isExpense = tx.type == "expense"
    val colorScheme = MaterialTheme.colorScheme

    // [OPT] luminance() memoized — same rationale as ReversalPairItem
    val glassAlpha = remember(colorScheme.background) {
        if (colorScheme.background.luminance() > 0.5f) 0.6f else 0.4f
    }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    // [OPT] accentColor depends on three cheap booleans; memoize so copy() isn't called
    //        on every recomposition when nothing relevant has changed
    val accentColor = remember(isIncome, isExpense, wasReversed, colorScheme) {
        val raw = when {
            isIncome -> colorScheme.tertiary
            isExpense -> colorScheme.error
            else -> colorScheme.onSurfaceVariant
        }
        if (wasReversed) raw.copy(alpha = 0.45f) else raw
    }

    val formattedAmount = remember(tx.amount) { formatAmount(tx.amount) }
    val amountPrefix = if (isIncome) "+" else if (isExpense) "-" else ""

    // [OPT] Removed outer Box+clip wrapper — Surface already clips via its shape.
    //        The clip was creating a redundant graphics layer on every item scroll.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(tx.id) {
                detectTapGestures(
                    onTap = { expanded = !expanded },
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        tx.reference?.let { reference ->
                            PesaActions.copyToClipboard(
                                context, clipboard, hapticFeedback, "Reference", reference
                            )
                        } ?: run { expanded = true }
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(
            if (expanded) 1.dp else 0.5.dp,
            if (expanded) accentColor.copy(alpha = 0.3f)
            else colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column {
            // ── Collapsed row ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape)
                ) {
                    Text(resolvedCategory.emoji, fontSize = 21.sp)
                    if (wasReversed) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .background(colorScheme.errorContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("↩", fontSize = 8.sp, color = colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tx.recipientName.ifBlank { tx.recipient },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (wasReversed) TextDecoration.LineThrough
                            else TextDecoration.None
                        ),
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    tx.recipientPhone?.let { phone ->
                        Text(
                            phone,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = accentColor.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                    if (wasReversed) {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "REVERSED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.sp
                                ),
                                color = colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Subtitles("${tx.displayDate} · ${tx.displayTime}")
                    Surface(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable(onClick = { showCategoryPicker = true }),
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(resolvedCategory.emoji, fontSize = 11.sp)
                            Text(
                                resolvedCategory.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Icon(
                                Icons.Rounded.Edit, null,
                                modifier = Modifier.size(9.dp),
                                tint = accentColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountPrefix$formattedAmount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum",
                            textDecoration = if (wasReversed) TextDecoration.LineThrough
                            else TextDecoration.None
                        ),
                        color = accentColor
                    )
                    Subtitles(PesaTimeConfiguration.CURRENCY)
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp
                    else Icons.Rounded.KeyboardArrowDown,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            // ── Expanded detail panel ─────────────────────────────────
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(180)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(accentColor.copy(alpha = 0.04f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = accentColor.copy(alpha = 0.1f))

                    InlineDetailRow("REF", tx.reference ?: "Unknown", monospace = true)
                    if (!isIncome) {
                        InlineDetailRow(
                            "FEES",
                            tx.transactionCost?.let { PesaTimeConfiguration.formatCurrency(it) }
                                ?: "Unknown"
                        )
                        InlineDetailRow(
                            "TOTAL",
                            if (tx.transactionCost == null) "Unknown (fees missing)"
                            else PesaTimeConfiguration.formatCurrency(tx.amount + tx.knownFees),
                            bold = true
                        )
                    }
                    run {
                        InlineDetailRow(
                            "BALANCE AFTER",
                            tx.balance?.let { PesaTimeConfiguration.formatCurrency(it) }
                                ?: "Unknown",
                            bold = true
                        )
                    }

                    reversalTx?.let { rev ->
                        HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))
                        Text(
                            "REVERSAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp
                            ),
                            color = colorScheme.tertiary.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                        InlineDetailRow("REF", rev.reference ?: "Unknown", monospace = true)
                        InlineDetailRow(
                            "CREDITED BACK",
                            PesaTimeConfiguration.formatCurrency(rev.amount),
                            bold = true
                        )
                        InlineDetailRow(
                            "DATE",
                            "${rev.displayDate} · ${rev.displayTime}"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tx.reference?.let { reference ->
                            QuickActionChip(
                                label = "Reference",
                                icon = Icons.Rounded.ContentCopy,
                                modifier = Modifier.weight(1f)
                            ) {
                                PesaActions.copyToClipboard(
                                    context, clipboard, hapticFeedback, "Reference", reference
                                )
                            }
                        }
                        QuickActionChip(
                            label = "Amount",
                            icon = Icons.Rounded.AttachMoney,
                            modifier = Modifier.weight(1f)
                        ) {
                            hapticFeedback.performHapticFeedback(
                                HapticFeedbackType.TextHandleMove
                            )
                            clipboard.setText(AnnotatedString(formattedAmount))
                        }
                        tx.recipientPhone?.takeIf { !it.contains("*") }?.let { phone ->
                            QuickActionChip(
                                label = "Mobile",
                                icon = Icons.Rounded.Phone,
                                modifier = Modifier.weight(1f)
                            ) {
                                hapticFeedback.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove
                                )
                                clipboard.setText(AnnotatedString(phone))
                            }
                        }
                        QuickActionChip(
                            label = "Change",
                            icon = Icons.Rounded.Category,
                            modifier = Modifier.weight(1f),
                            tint = colorScheme.primary.copy(alpha = 0.8f),
                            containerColor = colorScheme.primary.copy(alpha = 0.08f)
                        ) { showCategoryPicker = true }
                    }
                    if (!wasReversed) {
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    upcomingViewModel.openCreateFromHistory(
                                        txId = tx.id,
                                        recipient = tx.recipientName.ifBlank { tx.recipient },
                                        amount = tx.amount,
                                        category = tx.category,
                                        type = tx.type,
                                        txDate = tx.date
                                    )
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 12.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Loop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Setup as Recurring Rule",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun QuickActionChip(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = tint)
            Spacer(Modifier.width(5.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
        }
    }
}

@Composable
fun InlineDetailRow(
    label: String,
    value: String,
    bold: Boolean = false,
    monospace: Boolean = false,
    strikethrough: Boolean = false,
    onTap: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            modifier = if (onTap != null) Modifier.clickable { onTap() } else Modifier,
            style = if (bold) MaterialTheme.typography.labelLarge
            else MaterialTheme.typography.labelMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold,
            color = if (strikethrough)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.onSurface,
            textDecoration = if (strikethrough) TextDecoration.LineThrough
            else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}