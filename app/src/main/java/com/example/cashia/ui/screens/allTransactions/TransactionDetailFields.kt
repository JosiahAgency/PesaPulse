package com.example.cashia.ui.screens.allTransactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.utility.PesaTimeConfiguration

/**
 * The canonical read-only detail body for a single transaction.
 *
 * Both transaction-detail surfaces render this: the inline expand/collapse panel in
 * [NormalTransactionItem] (History, Calendar, Dashboard) and the full-screen
 * [TransactionDetailDialog] (Analytics). Each surface keeps its own chrome — container,
 * animations, swipe gestures and quick-action chips — but the field list itself lives here
 * so the two can no longer drift apart.
 *
 * @param compactLabels caps, abbreviated labels ("REF") for the tight inline panel; set
 *   `false` for the roomier dialog ("Reference").
 * @param showPhone whether to render the recipient phone row; masked numbers show "Redacted".
 * @param reversalTx when this transaction was reversed, the reversal entry to summarise below
 *   the main fields.
 * @param spacing vertical gap between rows.
 */
@Composable
fun TransactionDetailFields(
    tx: MpesaTransactions,
    modifier: Modifier = Modifier,
    compactLabels: Boolean = true,
    showPhone: Boolean = false,
    reversalTx: MpesaTransactions? = null,
    spacing: Dp = 8.dp,
) {
    val colorScheme = MaterialTheme.colorScheme
    // Neutral transactions (e.g. Fuliza limit updates) carry no fee breakdown worth showing.
    val showFees = tx.type != "income"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        InlineDetailRow(
            if (compactLabels) "REF" else "Reference",
            tx.reference ?: "Unknown",
            monospace = true
        )

        if (showFees) {
            InlineDetailRow(
                if (compactLabels) "FEES" else "Transaction Fee",
                tx.transactionCost?.let { PesaTimeConfiguration.formatCurrency(it) } ?: "Unknown"
            )
            InlineDetailRow(
                if (compactLabels) "TOTAL" else "Total Deducted",
                if (tx.transactionCost == null) "Unknown (fees missing)"
                else PesaTimeConfiguration.formatCurrency(tx.amount + tx.knownFees),
                bold = true
            )
        }

        InlineDetailRow(
            if (compactLabels) "BALANCE AFTER" else "Balance After",
            tx.balance?.let { PesaTimeConfiguration.formatCurrency(it) } ?: "Unknown",
            bold = true
        )

        if (showPhone) {
            tx.recipientPhone?.let { phone ->
                InlineDetailRow(
                    if (compactLabels) "PHONE" else "Phone",
                    if (phone.contains("*")) "Redacted" else phone
                )
            }
        }

        reversalTx?.let { rev ->
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))
            Text(
                "REVERSAL",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                color = colorScheme.tertiary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold
            )
            InlineDetailRow(
                if (compactLabels) "REF" else "Reference",
                rev.reference ?: "Unknown",
                monospace = true
            )
            InlineDetailRow(
                if (compactLabels) "CREDITED BACK" else "Credited Back",
                PesaTimeConfiguration.formatCurrency(rev.amount),
                bold = true
            )
            InlineDetailRow(
                if (compactLabels) "DATE" else "Date",
                "${rev.displayDate} · ${rev.displayTime}"
            )
        }
    }
}
