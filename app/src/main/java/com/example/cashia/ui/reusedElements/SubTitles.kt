package com.example.cashia.ui.reusedElements

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun Subtitles(
    text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme
        .colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
    )
}