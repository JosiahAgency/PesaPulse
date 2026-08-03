package com.example.cashia.ui.reusedElements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.theme.CurrencyAmountStyle

enum class StatPillTone { INCOME, EXPENSE, NEUTRAL }

/**
 * Non-interactive tinted pill for a labeled currency figure (income/expense/neutral stat) —
 * the mockup's merged-hero-card style. For stat displays that also need click-to-filter /
 * selected-state behavior (e.g. Calendar's SmallMetricCard), keep using that component instead.
 */
@Composable
fun StatPill(
    label: String,
    amount: String,
    icon: ImageVector,
    tone: StatPillTone,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val toneColor = when (tone) {
        StatPillTone.INCOME -> colorScheme.tertiary
        StatPillTone.EXPENSE -> colorScheme.error
        StatPillTone.NEUTRAL -> colorScheme.primary
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = toneColor.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, toneColor.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(toneColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = toneColor, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Subtitles(label, color = toneColor.copy(alpha = 0.85f))
                Text(
                    amount,
                    style = MaterialTheme.typography.titleMedium.merge(CurrencyAmountStyle),
                    color = toneColor
                )
            }
        }
    }
}
