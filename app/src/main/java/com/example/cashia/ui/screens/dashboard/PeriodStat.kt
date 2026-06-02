package com.example.cashia.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import com.example.cashia.ui.reusedElements.Subtitles

@Composable
fun PeriodStat(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    alignEnd: Boolean = false,
    modifier: Modifier = Modifier,

    ) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!alignEnd) {
                Icon(icon, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
            Subtitles(label)
            if (alignEnd) {
                Icon(icon, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}