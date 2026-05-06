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
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.SmsStatus
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.ui.reusedElements.Subtitles
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UnmatchedSmsItem(
    sms: UnmatchedSms,
    onReviewClick: (UnmatchedSms) -> Unit,
    onIgnoreClick: (UnmatchedSms) -> Unit,      // ← new
) {
    var expanded by remember { mutableStateOf(false) }
    var showIgnoreConfirm by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.6f else 0.4f
    val accentColor = colorScheme.tertiary

    val formattedDate = remember(sms.receivedAt) {
        SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.getDefault()).format(Date(sms.receivedAt))
    }
    val bodyPreview = remember(sms.rawBody) {
        sms.rawBody.lineSequence().firstOrNull { it.isNotBlank() }?.take(60) ?: sms.rawBody.take(60)
    }

    val statusLabel = when (sms.status) {
        SmsStatus.SKIPPED -> "Skipped"
        SmsStatus.PENDING -> "Needs review"
        else -> ""
    }
    val statusColor = when (sms.status) {
        SmsStatus.SKIPPED -> colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> colorScheme.tertiary
    }

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
                // ── Collapsed row ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                            style = MaterialTheme.typography.bodyMedium,
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
                        if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
                }

                // ── Expanded panel ─────────────────────────────────────────
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

                        Text(
                            sms.rawBody,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            ),
                            color = colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))

                        // Action row: Categorise + Ignore
                        if (sms.status == SmsStatus.PENDING || sms.status == SmsStatus.SKIPPED) {
                            if (!showIgnoreConfirm) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Categorise chip
                                    if (sms.status == SmsStatus.PENDING) {
                                        Surface(
                                            onClick = { onReviewClick(sms) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = colorScheme.primary.copy(alpha = 0.1f),
                                            border = BorderStroke(
                                                0.5.dp,
                                                colorScheme.primary.copy(alpha = 0.25f)
                                            ),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Categorise →",
                                                modifier = Modifier.padding(
                                                    horizontal = 14.dp, vertical = 8.dp
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colorScheme.primary
                                            )
                                        }
                                    }

                                    // Ignore chip
                                    Surface(
                                        onClick = { showIgnoreConfirm = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        border = BorderStroke(
                                            0.5.dp,
                                            colorScheme.onSurface.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 8.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.VisibilityOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                "Ignore",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Inline confirmation
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colorScheme.errorContainer.copy(alpha = 0.3f),
                                    border = BorderStroke(
                                        0.5.dp,
                                        colorScheme.error.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Permanently ignore?",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onErrorContainer
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { showIgnoreConfirm = false },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    "Cancel",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }

                                            Button(
                                                onClick = { onIgnoreClick(sms) },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = colorScheme.error
                                                )
                                            ) {
                                                Text(
                                                    "Ignore",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}