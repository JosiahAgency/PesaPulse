package com.example.cashia.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.screens.allTransactions.TransactionDetailDialog
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CategoryTransactionRow(tx: MpesaTransactions, accentColor: Color, haptic: HapticFeedback) {
    var showDetails by remember { mutableStateOf(false) }
    val date = remember(tx.date) {
        runCatching {
            LocalDate.parse(tx.date)
                .format(DateTimeFormatter.ofPattern("dd MMM"))
        }.getOrDefault(tx.date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .clickable {
                PesaActions.playClick(haptic)
                showDetails = true
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = accentColor.copy(alpha = 0.1f),
            modifier = Modifier.width(44.dp)
        ) {
            Text(
                date,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
        }
        Text(
            tx.recipient,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            PesaTimeConfiguration.formatCurrency(tx.amount),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = if (tx.type == "income")
                MaterialTheme.colorScheme.tertiary
            else
                MaterialTheme.colorScheme.error
        )
    }
    if (showDetails){
        TransactionDetailDialog(
            tx,
            accentColor,
            { showDetails = false }
        )
    }
}