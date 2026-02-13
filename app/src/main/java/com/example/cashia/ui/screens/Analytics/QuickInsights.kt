package com.example.cashia.ui.screens.Analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.MpesaTransactions


@Composable
fun QuickInsightsRow(
    transactions: List<MpesaTransactions>,
    type: TransactionType
) {
    val largest = transactions.maxByOrNull { it.amount }
    val average =
        if (transactions.isNotEmpty()) transactions.sumOf { it.amount } / transactions.size else 0.0

    val color = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Quick Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            largest?.let {
                InsightCard(
                    modifier = Modifier.weight(1f),
                    title = "Largest",
                    amount = it.amount,
                    subtitle = it.recipient,
                    color = color
                )
            }
            InsightCard(
                modifier = Modifier.weight(1f),
                title = "Average",
                amount = average,
                subtitle = "per transaction",
                color = color
            )
        }
    }
}

@Composable
fun InsightCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    subtitle: String,
    color: Color
) {
    val hapticFeedback = LocalHapticFeedback.current

    GlassySurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .clickable {
                    PesaActions.playClick(hapticFeedback)
                }
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = PesaTimeConfiguration.formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
