package com.example.cashia.ui.screens.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SystemCategoryRow(
    emoji: String,
    label: String,
    isModified: Boolean = false,
    isSubcategory: Boolean = false,
    onEdit: () -> Unit,
    onReset: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val indent = if (isSubcategory) 28.dp else 0.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent),
        shape = RoundedCornerShape(if (isSubcategory) 12.dp else 16.dp),
        color = when {
            isModified -> colorScheme.primaryContainer.copy(alpha = 0.2f)
            isSubcategory -> colorScheme.surface.copy(alpha = 0.5f)
            else -> colorScheme.surface.copy(alpha = 0.75f)
        },
        tonalElevation = if (isSubcategory) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isSubcategory) 12.dp else 14.dp,
                    vertical = if (isSubcategory) 8.dp else 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = if (isSubcategory) 16.sp else 20.sp)

            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = if (isSubcategory)
                        MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurface.copy(
                                alpha = 0.65f
                            )
                        )
                    else
                        MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurfaceVariant
                        ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isModified) {
                    Text(
                        "Customised",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.Edit, null,
                    Modifier.size(15.dp),
                    tint = colorScheme.primary.copy(alpha = if (isSubcategory) 0.5f else 0.85f)
                )
            }

            if (onReset != null) {
                IconButton(onClick = onReset, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Close, null,
                        Modifier.size(15.dp),
                        tint = colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}