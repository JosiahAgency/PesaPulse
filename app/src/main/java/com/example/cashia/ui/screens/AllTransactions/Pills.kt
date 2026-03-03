package com.example.cashia.ui.screens.AllTransactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.ResolvedCategory
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Composable
fun ActiveFilterPills(
    searchQuery: String,
    selectedCategory: ResolvedCategory?,
    dateRangeStart: LocalDate?,
    dateRangeEnd: LocalDate?,
    onClearSearch: () -> Unit,
    onClearCategory: () -> Unit,
    onClearDate: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        if (searchQuery.isNotBlank()) {
            item {
                FilterPill(
                    label = "\"$searchQuery\"",
                    onDismiss = onClearSearch
                )
            }
        }
        if (selectedCategory != null) {
            item {
                FilterPill(
                    label = "${selectedCategory.emoji} ${selectedCategory.label}",
                    onDismiss = onClearCategory
                )
            }
        }
        if (dateRangeStart != null || dateRangeEnd != null) {
            item {
                val label =
                    "${dateRangeStart?.format(DateTimeFormatter.ofPattern("dd MMM")) ?: "..."} → ${
                        dateRangeEnd?.format(DateTimeFormatter.ofPattern("dd MMM")) ?: "..."
                    }"
                FilterPill(label = label, onDismiss = onClearDate)
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, onDismiss: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close, null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
