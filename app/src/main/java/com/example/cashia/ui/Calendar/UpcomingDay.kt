package com.example.cashia.ui.Calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingType
import com.example.cashia.ui.screens.Analytics.GlassySurface
import com.example.cashia.ui.theme.primaryGradient


@Composable
fun UpcomingDaySection(
    upcoming: List<UpcomingTransaction>,
    allCategories: List<ResolvedCategory>,
    onMarkPaid: (UpcomingTransaction) -> Unit
) {
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "SCHEDULED TODAY",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.2.sp,
                brush = primaryGradient()
            ),
            fontWeight = FontWeight.Black,
        )
        upcoming.forEach { tx ->
            val resolved = categoryMap[tx.categoryId]
            GlassySurface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(resolved?.emoji ?: "📦", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tx.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            resolved?.label ?: "Other",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            PesaTimeConfiguration.formatCurrency(tx.amount),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = if (tx.type == UpcomingType.INCOME)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = { onMarkPaid(tx) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                "Mark Paid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}