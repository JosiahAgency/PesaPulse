package com.example.cashia.ui.screens.Dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TimeScope
import com.example.cashia.ui.screens.Analytics.GlassySurface


@Composable
fun PeriodSummaryCard(
    selectedScope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    periodLabel: String,
    summaryData: com.example.cashia.models.SummaryData,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period scope tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    TimeScope.DAY,
                    TimeScope.WEEK,
                    TimeScope.MONTH,
                    TimeScope.YEAR
                ).forEach { scope ->
                    val isSelected = selectedScope == scope
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                PesaActions.playClick(hapticFeedback)
                                onScopeChange(scope)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = when (scope) {
                                    TimeScope.DAY -> "Day"
                                    TimeScope.WEEK -> "Week"
                                    TimeScope.MONTH -> "Month"
                                    TimeScope.YEAR -> "Year"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Period label — the key missing piece: user always knows what they're looking at
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Stats row — each stat has its own label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PeriodStat(
                    label = "Money In",
                    value = PesaTimeConfiguration.formatCurrency(summaryData.income),
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.TrendingUp
                )
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(52.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )
                PeriodStat(
                    label = "Money Out",
                    value = PesaTimeConfiguration.formatCurrency(summaryData.expenses),
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.TrendingDown,
                    alignEnd = true
                )
            }

            // Net for the period
            val net = summaryData.income - summaryData.expenses
            val netPositive = net >= 0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = (if (netPositive) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error).copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net this ${
                            when (selectedScope) {
                                TimeScope.DAY -> "day"
                                TimeScope.WEEK -> "week"
                                TimeScope.MONTH -> "month"
                                else -> "year"
                            }
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${if (netPositive) "+" else ""}${
                            PesaTimeConfiguration.formatCurrency(
                                net
                            )
                        }",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (netPositive) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}