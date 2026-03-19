package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration

@Composable
fun TransactionDetailDialog(tx: MpesaTransactions, accentColor: Color, onDismiss: () -> Unit) {
    val isIncome = tx.type == "income"
    val isExpense = tx.type == "expense"
    val colorScheme = MaterialTheme.colorScheme

    Dialog(
        onDismissRequest = onDismiss, properties = DialogProperties(
            usePlatformDefaultWidth =
                false
        )
    ) {
        AnimatedVisibility(
            visible = true, enter = fadeIn(tween(200)) + scaleIn(
                tween(200),
                initialScale = 0.92f
            ), exit = fadeOut(tween(150)) + scaleOut(tween(150))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp), shape =
                    RoundedCornerShape(24.dp), color = colorScheme.surface, tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement
                            .SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            SectionLabel("transaction details")
                            Subtitles("${tx.displayDate} · ${tx.displayTime}")
                        }
                        IconButton(
                            onClick = onDismiss, modifier = Modifier
                                .size(32.dp)
                                .background(colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                Icons.Rounded.Close, null, tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accentColor.copy(alpha = 0.08f)
                    )
                    {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${if (isIncome) "+" else if (isExpense) "-" else ""}${
                                    PesaTimeConfiguration.formatCurrency(tx.amount)
                                }",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = accentColor
                            )
                            Text(
                                tx.recipient,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.onSurface.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.07f))
                    Spacer(Modifier.height(12.dp))
                    DetailLine("Reference", tx.id, monospace = true)
                    if (isExpense && tx.transactionCost > 0) {
                        DetailLine(
                            "Transaction Fee",
                            PesaTimeConfiguration.formatCurrency(tx.transactionCost)
                        )
                        DetailLine(
                            "Total Deducted", PesaTimeConfiguration.formatCurrency(
                                tx
                                    .amount + tx.transactionCost
                            ), bold = true
                        )
                    }
                    if (tx.balance > 0) {
                        DetailLine(
                            "Balance After",
                            PesaTimeConfiguration.formatCurrency(tx.balance),
                            bold = true
                        )
                    }
                    DetailLine("Type", tx.type.replaceFirstChar { it.uppercase() })
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                        Text("Close", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String, value: String, bold: Boolean = false, monospace: Boolean =
        false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement
            .SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (bold) FontWeight.Black else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}