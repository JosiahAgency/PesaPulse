package com.example.cashia.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration

/**
 * Compact "N payments due soon" summary — sourced from [UpcomingViewModel.upcoming] by the
 * caller (see DashboardScreen's inline `dueSoon` computation, which mirrors
 * ProjectionViewModel's overdue/dueSoon bucketing). Tapping navigates to the Forecast/
 * Projections screen, which already has the full due-soon list UI.
 */
@Composable
fun DueSoonBanner(dueSoon: List<UpcomingTransaction>, onClick: () -> Unit) {
    if (dueSoon.isEmpty()) return
    val soonest = dueSoon.first()
    val colorScheme = MaterialTheme.colorScheme

    GlassySurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colorScheme.error.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${dueSoon.size} payment${if (dueSoon.size == 1) "" else "s"} due soon",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Subtitles(
                    "${soonest.title} · ${PesaTimeConfiguration.formatCurrency(soonest.amount)}" +
                            " — ${soonest.relativeDueLabel.lowercase()}"
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
