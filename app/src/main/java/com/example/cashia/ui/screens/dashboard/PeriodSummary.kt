package com.example.cashia.ui.screens.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SelectableBox
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TimeScope


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
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    TimeScope.DAY,
                    TimeScope.WEEK,
                    TimeScope.MONTH,
                    TimeScope.YEAR
                ).forEach { scope ->
                    val isSelected = selectedScope == scope
                    SelectableBox(
                        hapticFeedback,
                        isSelected,
                        { onScopeChange(scope) },
                        Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (scope) {
                                TimeScope.DAY -> "Day"
                                TimeScope.WEEK -> "Week"
                                TimeScope.MONTH -> "Month"
                                TimeScope.YEAR -> "Year"
                                else -> ""
                            },
                            color = if (isSelected) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Subtitles(periodLabel)
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PeriodStat(
                    label = "Money In",
                    value = PesaTimeConfiguration.formatCurrency(summaryData.income),
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.AutoMirrored.Filled.TrendingUp
                )
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
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    alignEnd = true
                )
            }
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
                    Subtitles("Net")
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