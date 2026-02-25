package com.example.cashia.ui.screens.Analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.MpesaTransactions

@Composable
fun TypeSelectorCard(
    haptic: HapticFeedback,
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit,
    transactions: List<MpesaTransactions>,
    startTs: Long,
    endTs: Long
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f

    val incomeTotal = remember(transactions, startTs, endTs) {
        transactions.filter { it.timestamp in startTs..endTs && it.type == "income" }
            .sumOf { it.amount }
    }
    val expenseTotal = remember(transactions, startTs, endTs) {
        transactions.filter { it.timestamp in startTs..endTs && it.type == "expense" }
            .sumOf { it.amount }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(TransactionType.INCOME, TransactionType.EXPENSE).forEach { type ->
                val isSelected = selectedType == type
                val typeColor = if (type == TransactionType.INCOME)
                    colorScheme.tertiary else colorScheme.error
                val total = if (type == TransactionType.INCOME) incomeTotal else expenseTotal

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) typeColor.copy(alpha = 0.12f)
                    else colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 0.5.dp,
                        if (isSelected) typeColor.copy(alpha = 0.4f)
                        else colorScheme.onSurface.copy(alpha = 0.06f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            PesaActions.playClick(haptic)
                            onTypeChange(type)
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (type == TransactionType.INCOME)
                                    Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) typeColor
                                else colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) typeColor
                                else colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = PesaTimeConfiguration.formatCurrency(total),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) typeColor
                            else colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}