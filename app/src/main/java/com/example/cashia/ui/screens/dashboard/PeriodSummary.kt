package com.example.cashia.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SelectableBox
import com.example.cashia.ui.reusedElements.Subtitles
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
    hapticFeedback: HapticFeedback
) {
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
        }
    }
}
