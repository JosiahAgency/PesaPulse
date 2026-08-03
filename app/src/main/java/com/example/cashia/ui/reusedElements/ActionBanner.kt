package com.example.cashia.ui.reusedElements

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared "icon circle + title + subtitle + chevron" banner card — the Banani export uses this
 * exact shape for both Dashboard's due-soon summary and History's unmatched-SMS entry point.
 * [badgeCount], when set and > 0, overlays a small numbered circle on the icon (matching the
 * unmatched-SMS mockup); omit it when the count is already stated in [title] (due-soon banner).
 */
@Composable
fun ActionBanner(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null,
) {
    val colorScheme = MaterialTheme.colorScheme

    GlassySurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconTint.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (badgeCount != null && badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .background(colorScheme.error, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (badgeCount > 9) "9+" else "$badgeCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Black,
                            color = colorScheme.onError
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Subtitles(subtitle)
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
