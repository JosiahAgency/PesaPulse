package com.example.cashia.ui.screens.category

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.example.cashia.models.ResolvedCategory

@Composable
fun SystemCategoryRow(
    category: ResolvedCategory,
    isModified: Boolean,
    onEdit: () -> Unit,
    onReset: () -> Unit,
    childCount: Int = 0,
    expanded: Boolean = false,
    onToggle: () -> Unit = {},
    enabled: Boolean = true
) {
    CategoryCard(
        emoji = category.emoji,
        label = category.label,
        subtitle = buildString {
            append(if (isModified) "Customised" else if (category.parentId != null) "Subcategory" else "Built-in")
            if (childCount > 0) append(" / $childCount subcategories")
        },
        onEdit = onEdit,
        enabled = enabled,
        modifier = Modifier.padding(start = if (category.parentId != null) 20.dp else 0.dp),
        menuAction = if (isModified) onReset else null,
        menuLabel = "Restore defaults",
        trailing = if (childCount > 0) ({
            IconButton(
                onClick = onToggle,
                modifier = Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
            ) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    if (expanded) "Collapse ${category.label}" else "Expand ${category.label}"
                )
            }
        }) else null
    )
}
