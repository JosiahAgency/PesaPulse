package com.example.cashia.ui.screens.AllTransactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.ui.screens.Analytics.formatCompactAmount
import com.example.cashia.ui.theme.primaryGradient


@Composable
fun CompactTransactionSummary(balance: Double, income: Double, expenses: Double) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surface.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummarySmallItem("Income", income, colorScheme.tertiary, Icons.Default.TrendingUp)
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                    ),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Black
                )
                Text(
                    PesaTimeConfiguration.formatCurrency(balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black, color = colorScheme.onSurface
                )
            }
            SummarySmallItem(
                "Expenses",
                expenses,
                colorScheme.error,
                Icons.Default.TrendingDown,
                true
            )
        }
    }
}

@Composable
fun SummarySmallItem(
    label: String, amount: Double, color: Color,
    icon: ImageVector, alignEnd: Boolean = false
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!alignEnd) Icon(
                icon,
                null,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
            Text(
                label, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            if (alignEnd) Icon(
                icon,
                null,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            formatCompactAmount(amount), style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black, color = color
        )
    }
}