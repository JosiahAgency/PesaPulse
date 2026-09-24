package com.example.cashia.ui.reusedElements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.ui.theme.primaryGradient
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSearchBar(
    query: String,
    expanded: Boolean,
    recentSearches: List<String>,
    allCategories: List<ResolvedCategory>,
    selectedCategory: String?,
    dateRangeStart: LocalDate?,
    dateRangeEnd: LocalDate?,
    onQueryChange: (String) -> Unit,
    onExpandChange: (Boolean) -> Unit,
    onRecentPick: (String) -> Unit,
    onCategoryPick: (String?) -> Unit,
    onDateRange: (LocalDate?, LocalDate?) -> Unit,
) {
    var showFilterPanel by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    val hasActiveFilter = selectedCategory != null || dateRangeStart != null

    if (showDateRangeDialog) {
        DateRangePickerDialog(
            initialStart = dateRangeStart ?: LocalDate.now().minusMonths(1),
            initialEnd = dateRangeEnd ?: LocalDate.now(),
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { start, end ->
                onDateRange(start, end)
                showDateRangeDialog = false
            }
        )
    }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                border = BorderStroke(
                    0.6.dp,
                    if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Search, null,
                        modifier = Modifier.size(18.dp),
                        tint = if (expanded) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    BasicTextField(
                        value = query,
                        onValueChange = { onQueryChange(it); onExpandChange(true) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    "Search by name, amount or category…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            }
                            inner()
                        },
                        cursorBrush = primaryGradient()
                    )

                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = { onQueryChange(""); onExpandChange(false) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close, "Clear search", modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Single external filter entry point — reveals category + date-range controls below.
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                border = BorderStroke(
                    0.6.dp,
                    if (hasActiveFilter) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            ) {
                IconButton(
                    onClick = { showFilterPanel = !showFilterPanel },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        Icons.Rounded.FilterList, "Filters", modifier = Modifier.size(20.dp),
                        tint = if (hasActiveFilter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Recent searches dropdown
        AnimatedVisibility(visible = expanded && query.isEmpty() && recentSearches.isNotEmpty()) {
            GlassySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        "RECENT",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Black
                    )
                    recentSearches.forEach { term ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRecentPick(term) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.History, null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                term,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Filter panel — category chips + date-range trigger, both behind the one filter icon
        AnimatedVisibility(visible = showFilterPanel) {
            GlassySurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "FILTER BY CATEGORY",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Black
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { onCategoryPick(null) },
                                label = {
                                    Text(
                                        "✦ All",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                        items(allCategories, key = { it.id }) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.id,
                                onClick = {
                                    onCategoryPick(if (selectedCategory == cat.id) null else cat.id)
                                },
                                label = {
                                    Text(
                                        "${cat.emoji} ${cat.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.15f
                                    ),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Text(
                        "DATE RANGE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Black
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDateRangeDialog = true }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth, null,
                            modifier = Modifier.size(16.dp),
                            tint = if (dateRangeStart != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            if (dateRangeStart != null && dateRangeEnd != null)
                                "$dateRangeStart – $dateRangeEnd"
                            else "Pick a date range",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (dateRangeStart != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (dateRangeStart != null) {
                            IconButton(
                                onClick = { onDateRange(null, null) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close, "Clear date range",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}