package com.example.cashia.ui.reusedElements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun GlassySurface(modifier: Modifier = Modifier, glow: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.95f else 0.8f
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.5.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        if (glow) {
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    colorScheme.primary.copy(alpha = 0.14f),
                    colorScheme.tertiary.copy(alpha = 0.05f),
                    androidx.compose.ui.graphics.Color.Transparent
                )
            )
            Box(modifier = Modifier.background(glowBrush)) {
                content()
            }
        } else {
            content()
        }
    }
}
