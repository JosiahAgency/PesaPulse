package com.example.cashia.ui.screens.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.CustomCategory

@Composable
fun CustomCategoryRow(
    category: CustomCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true
) {
    val keywords = remember(category.keywords) {
        category.keywords.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    CategoryCard(
        emoji = category.emoji,
        label = category.label,
        subtitle = if (keywords.isEmpty()) "Manual sorting" else "Auto-match: ${keywords.take(3).joinToString(", ")}" +
            if (keywords.size > 3) " +${keywords.size - 3}" else "",
        onEdit = onEdit,
        enabled = enabled,
        menuAction = onDelete,
        menuLabel = "Delete category",
        destructive = true
    )
}

@Composable
internal fun CategoryCard(
    emoji: String,
    label: String,
    subtitle: String,
    onEdit: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    menuAction: (() -> Unit)? = null,
    menuLabel: String = "",
    destructive: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    Surface(
        onClick = onEdit,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
            if (menuAction != null) {
                Box {
                    IconButton(onClick = { showMenu = true }, enabled = enabled) {
                        Icon(Icons.Rounded.MoreVert, "Options for $label")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit category") },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(menuLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            },
                            onClick = { showMenu = false; menuAction() }
                        )
                    }
                }
            }
        }
    }
}
