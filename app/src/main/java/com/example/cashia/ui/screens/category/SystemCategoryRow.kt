package com.example.cashia.ui.screens.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SystemCategoryRow(
    emoji: String, label: String, isModified: Boolean = false,
    isSubcategory: Boolean = false,
    onEdit: () -> Unit,
    onReset: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val lineColor = colorScheme.onSurface.copy(alpha = 0.12f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSubcategory) Modifier.drawBehind {
                    val x = 22.dp.toPx()
                    drawLine(
                        color = lineColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height / 2f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawLine(
                        color = lineColor,
                        start = Offset(x, size.height / 2f),
                        end = Offset(x + 12.dp.toPx(), size.height / 2f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                } else Modifier
            )
            .padding(start = if (isSubcategory) 40.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically) {

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(if (isSubcategory) 14.dp else 18.dp),
            color = if (isSubcategory) colorScheme.surface.copy(alpha = 0.15f) else colorScheme.surface.copy(
                alpha = 0.3f
            ),
            border = BorderStroke(
                if (isModified) 1.dp else 0.5.dp,
                if (isModified) colorScheme.primary.copy(alpha = 0.3f)
                else colorScheme.onSurface.copy(alpha = if (isSubcategory) 0.04f else 0.05f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isSubcategory) 12.dp else 16.dp,
                        vertical = if (isSubcategory) 8.dp else 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSubcategory) 34.dp else 44.dp)
                        .background(
                            colorScheme.surfaceVariant.copy(alpha = if (isSubcategory) 0.3f else 0.5f),
                            RoundedCornerShape(if (isSubcategory) 8.dp else 12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = if (isSubcategory) 16.sp else 22.sp)
                }
                Spacer(Modifier.width(if (isSubcategory) 10.dp else 14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        label,
                        style = if (isSubcategory) MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal
                        ) else MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface.copy(alpha = if (isSubcategory) 0.55f else 0.7f)
                    )
                    if (isModified) {
                        Text(
                            "Modified",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(if (isSubcategory) 30.dp else 36.dp)
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        null,
                        modifier = Modifier.size(if (isSubcategory) 14.dp else 18.dp),
                        tint = colorScheme.primary.copy(alpha = if (isSubcategory) 0.5f else 1f)
                    )
                }
                if (onReset != null) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(if (isSubcategory) 30.dp else 36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            null,
                            modifier = Modifier.size(if (isSubcategory) 14.dp else 18.dp),
                            tint = colorScheme.error
                        )
                    }
                }
            }
        }
    }
}