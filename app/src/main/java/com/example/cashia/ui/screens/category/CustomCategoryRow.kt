package com.example.cashia.ui.screens.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.CustomCategory


@Composable
fun CustomCategoryRow(
    category: CustomCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Emoji badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        colorScheme.primaryContainer.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(category.emoji, fontSize = 20.sp)
            }

            // Label + keywords
            Column(Modifier.weight(1f)) {
                CategoryNameLabel(category.label)
                if (category.keywords.isNotBlank()) {
                    val kwList =
                        category.keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val preview = kwList.take(4).joinToString("  ·  ")
                    val suffix = if (kwList.size > 4) "  +${kwList.size - 4}" else ""
                    Text(
                        text = preview + suffix,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action buttons
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.Edit, null,
                    Modifier.size(17.dp),
                    tint = colorScheme.primary
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Rounded.Delete, null,
                    Modifier.size(17.dp),
                    tint = colorScheme.error.copy(alpha = 0.75f)
                )
            }
        }
    }
}