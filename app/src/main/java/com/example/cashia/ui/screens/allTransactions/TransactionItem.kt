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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.ContentCopy
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.formatAmount

@Composable
fun TransactionItem(
    tx: MpesaTransactions,
    hapticFeedback: HapticFeedback,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        TransactionItemExpanded(
            tx = tx,
            expanded = expanded,
            hapticFeedback = hapticFeedback,
            onExpandToggle = { expanded = !expanded },
        )
    }
}

@Composable
fun TransactionItemExpanded(
    tx: MpesaTransactions,
    expanded: Boolean,
    hapticFeedback: HapticFeedback,
    onExpandToggle: () -> Unit,
) {
    val isIncome = tx.type == "income"
    val isExpense = tx.type == "expense"
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = when {
        isIncome -> colorScheme.tertiary
        isExpense -> colorScheme.error
        else -> colorScheme.onSurfaceVariant
    }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val formattedAmount = remember(tx.amount) { formatAmount(tx.amount) }
    val amountPrefix = if (isIncome) "+" else if (isExpense) "-" else ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(tx.id) {
                detectTapGestures(
                    onTap = { onExpandToggle() },
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        PesaActions.copyToClipboard(
                            context,
                            clipboard,
                            hapticFeedback,
                            "Reference",
                            tx.id
                        )
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.12f),
        border = BorderStroke(
            if (expanded) 1.dp else 0.5.dp,
            if (expanded) accentColor.copy(alpha = 0.3f) else colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tx.recipientName.ifBlank { tx.recipient },
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    tx.recipientPhone?.let { phone ->
                        val isRedacted = phone.contains("*")
                        Text(
                            phone,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isRedacted)
                                accentColor.copy(alpha = 0.4f)   // dim redacted numbers
                            else
                                accentColor.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Subtitles(
                        "${tx.displayDate} · ${tx.displayTime}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(4.dp)
                ) {
                    Text(
                        text = "$amountPrefix$formattedAmount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFeatureSettings = "tnum"
                        ),
                        color = accentColor
                    )
                    Subtitles(
                        PesaTimeConfiguration.CURRENCY,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
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
                    Spacer(Modifier.height(2.dp))

                    InlineDetailRow("REF", tx.id, monospace = true)
                    if (!isIncome && tx.transactionCost > 0) {
                        InlineDetailRow(
                            "FEES",
                            PesaTimeConfiguration.formatCurrency(tx.transactionCost)
                        )
                        InlineDetailRow(
                            "TOTAL",
                            PesaTimeConfiguration.formatCurrency(tx.amount + tx.transactionCost),
                            bold = true
                        )
                    }
                    tx.recipientPhone?.takeIf { it.contains("*") }?.let {
                        InlineDetailRow("MOBILE NUMBER", "Redacted — forward SMS to 334")
                    }
                    if (tx.balance > 0) {
                        InlineDetailRow(
                            "BALANCE AFTER",
                            PesaTimeConfiguration.formatCurrency(tx.balance),
                            bold = true
                        )
                    }

                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionChip(
                            label = "Reference",
                            icon = Icons.Rounded.ContentCopy,
                            modifier = Modifier.weight(1f)
                        ) {
                            PesaActions.copyToClipboard(
                                context,
                                clipboard,
                                hapticFeedback,
                                "Reference",
                                tx.id
                            )
                        }
                        QuickActionChip(
                            label = "Amount",
                            icon = Icons.Rounded.AttachMoney,
                            modifier = Modifier.weight(1f)
                        ) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            clipboard.setText(AnnotatedString(formattedAmount))
                        }
                        tx.recipientPhone?.takeIf { !it.contains("*") }?.let { phone ->
                            QuickActionChip(
                                label = "Mobile Number",
                                Icons.Rounded.Phone,
                                Modifier.weight(1f)
                            ) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                clipboard.setText(AnnotatedString(phone))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(
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
private fun InlineDetailRow(
    label: String,
    value: String,
    bold: Boolean = false,
    monospace: Boolean = false,
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
            style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}