package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.ui.reusedElements.Subtitles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UnmatchedSmsItem(
    sms: UnmatchedSms,
    onReviewClick: (UnmatchedSms) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.6f else 0.4f
    val accentColor = colorScheme.tertiary

    val formattedDate = remember(sms.receivedAt) {
        SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault())
            .format(Date(sms.receivedAt))
    }
    // First line of the SMS body — used as a preview in collapsed state
    val bodyPreview = remember(sms.rawBody) {
        sms.rawBody.lineSequence().firstOrNull { it.isNotBlank() }?.take(60) ?: sms.rawBody.take(60)
    }
    val statusLabel = if (sms.reviewed) "Skipped" else "Needs review"
    val statusColor = if (sms.reviewed)
        colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    else
        colorScheme.tertiary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(sms.id) {
                    detectTapGestures(onTap = { expanded = !expanded })
                },
            shape = RoundedCornerShape(20.dp),
            color = colorScheme.surface.copy(alpha = glassAlpha),
            border = BorderStroke(
                if (expanded) 1.dp else 0.5.dp,
                if (expanded) accentColor.copy(alpha = 0.3f)
                else colorScheme.onSurface.copy(alpha = 0.08f)
            )
        ) {
            Column {
                // ── Collapsed row ────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon bubble
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .background(accentColor.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Text("❓", fontSize = 21.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            bodyPreview,
                            style = MaterialTheme.typography.bodyMedium.copy(),
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Subtitles(formattedDate)
                        Surface(
                            modifier = Modifier.padding(top = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = statusColor.copy(alpha = 0.1f),
                            border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.25f))
                        ) {
                            Text(
                                statusLabel,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowUp
                        else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }

                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(200)) + fadeIn(),
                    exit = shrinkVertically(tween(180)) + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(accentColor.copy(alpha = 0.04f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(color = accentColor.copy(alpha = 0.1f))
                        Spacer(Modifier.height(2.dp))
                        // Full raw SMS body
                        Text(
                            sms.rawBody,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            ),
                            color = colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                        // Review action chip — only for unreviewed
                        if (!sms.reviewed) {
                            Surface(
                                onClick = { onReviewClick(sms) },
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.primary.copy(alpha = 0.1f),
                                border = BorderStroke(
                                    0.5.dp,
                                    colorScheme.primary.copy(alpha = 0.25f)
                                )
                            ) {
                                Text(
                                    text = "Categorise →",
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 8.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}