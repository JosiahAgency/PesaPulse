package com.example.cashia.ui.reusedElements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.theme.primaryGradient

/**
 * Full-width, tonal call-to-action used inline in scrollable content (e.g. "Export as CSV").
 * This is a regular content item, not a floating overlay — kept deliberately understated:
 * a soft tonal surface instead of a solid/bordered bar.
 */
@Composable
fun HarmonizedActionFab(
    text: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        modifier = if (fullWidth) modifier.fillMaxWidth() else modifier
    ) {
        Row(
            modifier = Modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = if (fullWidth) Arrangement.Center else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = " $text",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Compact, icon-only floating quick action (e.g. "Today", "Back to top") meant to be anchored
 * in a screen corner via `Modifier.align(...)`. Small footprint with the app's primary brand
 * gradient (same one used on the balance card, calendar selection, etc.) so it reads as an
 * on-brand accent rather than a generic system control. The label surfaces as a tooltip on
 * long-press, and is always exposed to screen readers via [contentDescription].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingQuickActionFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(contentDescription) } },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 3.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(primaryGradient()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = Color.White
                )
            }
        }
    }
}
