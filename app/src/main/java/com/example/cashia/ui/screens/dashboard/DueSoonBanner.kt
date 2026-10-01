package com.example.cashia.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.ui.reusedElements.ActionBanner
import com.example.cashia.utility.PesaTimeConfiguration

/** How many of the remaining due items are listed before collapsing the tail into "+N more". */
private const val MAX_PREVIEW_ROWS = 3

@Composable
fun DueSoonBanner(dueSoon: List<UpcomingTransaction>, onClick: () -> Unit) {
    if (dueSoon.isEmpty()) return
    val soonest = dueSoon.first()
    val rest = dueSoon.drop(1)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ActionBanner(
            icon = Icons.Rounded.Schedule,
            iconTint = MaterialTheme.colorScheme.error,
            title = "${dueSoon.size} payment${if (dueSoon.size == 1) "" else "s"} due soon",
            subtitle = "${soonest.title} · ${PesaTimeConfiguration.formatCurrency(soonest.amount)}" +
                    " — ${soonest.relativeDueLabel.lowercase()}",
            onClick = onClick
        )
        // The banner itself only names the soonest item; listing the rest keeps the whole
        // window glanceable without navigating to the Upcoming screen.
        if (rest.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                rest.take(MAX_PREVIEW_ROWS).forEach { tx ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DueSoonRowText(
                            text = "${tx.title} · ${PesaTimeConfiguration.formatCurrency(tx.amount)}",
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(end = 8.dp)
                        )
                        DueSoonRowText(text = tx.relativeDueLabel.lowercase())
                    }
                }
                val hidden = rest.size - MAX_PREVIEW_ROWS
                if (hidden > 0) {
                    DueSoonRowText(
                        text = "+$hidden more",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun DueSoonRowText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color =
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
