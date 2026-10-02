package com.example.cashia.ui.reusedElements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared caveat for totals that are understated because some M-PESA messages never stated a
 * transaction cost. Renders nothing when every fee is known, so the warning only appears when
 * it is actually true of the data on screen.
 */
@Composable
fun UnknownFeesNotice(
    unknownFeeCount: Int,
    modifier: Modifier = Modifier,
) {
    if (unknownFeeCount <= 0) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Fees missing on $unknownFeeCount " +
                        "${if (unknownFeeCount == 1) "transaction" else "transactions"} — " +
                        "totals may be understated.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
