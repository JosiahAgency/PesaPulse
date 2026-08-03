package com.example.cashia.ui.screens.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.ui.reusedElements.ActionBanner
import com.example.cashia.utility.PesaTimeConfiguration

@Composable
fun DueSoonBanner(dueSoon: List<UpcomingTransaction>, onClick: () -> Unit) {
    if (dueSoon.isEmpty()) return
    val soonest = dueSoon.first()

    ActionBanner(
        icon = Icons.Rounded.Schedule,
        iconTint = MaterialTheme.colorScheme.error,
        title = "${dueSoon.size} payment${if (dueSoon.size == 1) "" else "s"} due soon",
        subtitle = "${soonest.title} · ${PesaTimeConfiguration.formatCurrency(soonest.amount)}" +
                " — ${soonest.relativeDueLabel.lowercase()}",
        onClick = onClick
    )
}
