package com.example.cashia.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SelectableBox
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TimeScope

private val PERIOD_SCOPES = listOf(
    TimeScope.DAY to "Day",
    TimeScope.WEEK to "Week",
    TimeScope.MONTH to "Month",
    TimeScope.YEAR to "Year",
)

@Composable
fun PeriodSummaryCard(
    selectedScope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    periodLabel: String,
    income: Double,
    expenses: Double,
    hapticFeedback: HapticFeedback
) {
    val net = income - expenses
    val netPositive = net >= 0
    val netColor = if (netPositive) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PERIOD_SCOPES.forEach { (scope, label) ->
                    val isSelected = selectedScope == scope

                    SelectableBox(
                        hapticFeedback = hapticFeedback,
                        isSelected = isSelected,
                        onClick = { onScopeChange(scope) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Subtitles(periodLabel)

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PeriodStat(
                    label = "Money In",
                    value = PesaTimeConfiguration.formatCurrency(income),
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
                    value = PesaTimeConfiguration.formatCurrency(expenses),
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    alignEnd = true
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = netColor.copy(alpha = if (netPositive) 0.10f else 0.12f),
                border = BorderStroke(
                    width = 0.5.dp,
                    color = netColor.copy(alpha = 0.18f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Subtitles("Net")

                    Text(
                        text = "${if (netPositive) "+" else ""}${
                            PesaTimeConfiguration.formatCurrency(net)
                        }",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = netColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}