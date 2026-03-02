package com.example.cashia.ui.screens.AllTransactions

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.TransactionCategory
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.screens.Analytics.formatCompactAmount
import com.example.cashia.ui.screens.CategoryPickerSheet
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel,
    onNavigateBack: @Composable () -> Unit = {}
) {
    val state = viewModel.state
    val hapticFeedback = LocalHapticFeedback.current

    var selectedTransaction by remember { mutableStateOf<MpesaTransactions?>(null) }
    var collapsedDates by remember { mutableStateOf(setOf<LocalDate>()) }
    var searchQuery by remember { mutableStateOf("") }

    val allCategories by categoryViewModel.allCategories.collectAsState()

    val filteredTransactions by remember(
        searchQuery, state.allTransactions, state.selectedCategory
    ) {
        derivedStateOf {
            state.allTransactions
                .filter {
                    searchQuery.isBlank() ||
                            it.recipient.contains(searchQuery, ignoreCase = true) ||
                            it.id.contains(searchQuery, ignoreCase = true)
                }
                .filter {
                    state.selectedCategory == null || it.category == state.selectedCategory
                }
        }
    }

    val groupedTransactions by remember(filteredTransactions) {
        derivedStateOf {
            filteredTransactions
                .groupBy { LocalDate.parse(it.date) }
                .toSortedMap(compareByDescending { it })
        }
    }

    Scaffold(containerColor = Color.Transparent) { _ ->

        // ── Transaction detail dialog ──────────────────────────────────────
        selectedTransaction?.let { tx ->
            TransactionDetailDialog(
                hapticFeedback = hapticFeedback,
                tx = tx,
                categoryViewModel = categoryViewModel,
                onDismiss = { selectedTransaction = null },
                onEdit = { /* TODO */ },
                onDelete = { /* TODO */ }
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refreshFromSms() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ── Summary card ───────────────────────────────────────────
                item {
                    CompactTransactionSummary(
                        balance = state.latestBalance,
                        income = state.totalIncome,
                        expenses = state.totalExpenses
                    )
                }

                // ── Search bar ─────────────────────────────────────────────
                item {
                    ModernSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // ── Category filter chips (system + custom) ────────────────
                item {
                    CategoryFilterRow(
                        allCategories = allCategories,
                        selectedCategory = state.selectedCategory,
                        onCategorySelected = { viewModel.filterByCategory(it) }
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // ── Empty state ────────────────────────────────────────────
                if (state.allTransactions.isEmpty() && !state.isLoading) {
                    item { EmptyStateMessage("No History Found", hapticFeedback) }
                } else {
                    // ── Grouped, date-sectioned transaction list ───────────
                    groupedTransactions.forEach { (date, dailyTransactions) ->
                        val isCollapsed = collapsedDates.contains(date)

                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                                    )
                                    .padding(vertical = 12.dp, horizontal = 4.dp)
                                    .clickable {
                                        PesaActions.playClick(hapticFeedback)
                                        collapsedDates = if (isCollapsed)
                                            collapsedDates - date
                                        else
                                            collapsedDates + date
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = date
                                        .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM"))
                                        .uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Icon(
                                    imageVector = if (isCollapsed)
                                        Icons.Rounded.KeyboardArrowDown
                                    else
                                        Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }

                        if (!isCollapsed) {
                            items(items = dailyTransactions, key = { it.id }) { tx ->
                                TransactionItem(
                                    tx = tx,
                                    categoryLabel = resolveCategory(tx.category, allCategories),
                                    onClick = { selectedTransaction = tx },
                                    hapticFeedback = hapticFeedback
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun resolveCategory(
    categoryId: String,
    allCategories: List<ResolvedCategory>
): Pair<String, String> {
    val found = allCategories.find { it.id == categoryId }
    return if (found != null) {
        found.emoji to found.label
    } else {
        TransactionCategory.OTHER.emoji to TransactionCategory.OTHER.label
    }
}

@Composable
fun CompactTransactionSummary(balance: Double, income: Double, expenses: Double) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surface.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummarySmallItem(
                "Income",
                income,
                colorScheme.tertiary,
                icon = Icons.Default.TrendingUp
            )
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = PesaTimeConfiguration.formatCurrency(balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = colorScheme.onSurface
                )
            }
            SummarySmallItem(
                "Expenses", expenses, colorScheme.error,
                icon = Icons.Default.TrendingDown, alignEnd = true
            )
        }
    }
}

// ─── Category filter row (uses ALL categories: system + custom) ───────────────

@Composable
fun CategoryFilterRow(
    allCategories: List<ResolvedCategory>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    // "All" sentinel + every resolved category
    val items = listOf<ResolvedCategory?>(null) + allCategories

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(items) { cat ->
            val isSelected = cat?.id == selectedCategory
            val label = if (cat == null) "✦ All" else "${cat.emoji} ${cat.label}"

            FilterChip(
                selected = isSelected,
                onClick = {
                    // Tapping the already-selected non-All chip resets to All
                    onCategorySelected(if (isSelected && cat != null) null else cat?.id)
                },
                label = {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            )
        }
    }
}

// ─── Supporting composables ───────────────────────────────────────────────────

@Composable
fun SummarySmallItem(
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    alignEnd: Boolean = false
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!alignEnd) Icon(
                icon,
                null,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            if (alignEnd) Icon(
                icon,
                null,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = formatCompactAmount(amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun ModernSearchBar(query: String, onQueryChange: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search by name or reference…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        PesaActions.playClick(haptic)
                        onQueryChange("")
                    }) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun EmptyStateMessage(message: String, hapticFeedback: HapticFeedback) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Transaction list item ────────────────────────────────────────────────────

@Composable
fun TransactionItem(
    tx: MpesaTransactions,
    categoryLabel: Pair<String, String>,   // emoji to label
    onClick: () -> Unit,
    hapticFeedback: HapticFeedback
) {
    val isIncome = tx.type.lowercase() == "income"
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.6f else 0.4f
    val accentColor = if (isIncome) colorScheme.tertiary else colorScheme.error
    val (catEmoji, catLabel) = categoryLabel

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                onClick()
                PesaActions.playClick(hapticFeedback)
            },
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.5.dp, colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category emoji avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape)
            ) {
                Text(catEmoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.recipient,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tx.displayDate} • ${tx.displayTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                // Category badge
                Text(
                    text = catLabel,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isIncome) "+${formatAmount(tx.amount)}" else "-${formatAmount(tx.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum"
                    ),
                    color = accentColor
                )
                Text(
                    text = PesaTimeConfiguration.CURRENCY,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ─── Transaction detail dialog ────────────────────────────────────────────────

@Composable
fun TransactionDetailDialog(
    hapticFeedback: HapticFeedback,
    tx: MpesaTransactions,
    categoryViewModel: CategoryViewModel,
    onDismiss: () -> Unit,
    onEdit: (MpesaTransactions) -> Unit,
    onDelete: (MpesaTransactions) -> Unit
) {
    val context = LocalContext.current
    val isIncome = tx.type.lowercase() == "income"
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = if (isIncome) colorScheme.tertiary else colorScheme.error
    val clipboard = LocalClipboardManager.current

    val allCategories by categoryViewModel.allCategories.collectAsState()
    var showCategoryPicker by remember { mutableStateOf(false) }
    var currentCategoryId by remember(tx.id) { mutableStateOf(tx.category) }

    val resolvedCategory = remember(currentCategoryId, allCategories) {
        allCategories.find { it.id == currentCategoryId }
            ?: ResolvedCategory(
                TransactionCategory.OTHER.name,
                TransactionCategory.OTHER.label,
                TransactionCategory.OTHER.emoji,
                isSystem = true
            )
    }

    val total = if (!isIncome) tx.amount + tx.transactionCost else tx.amount

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(28.dp),
            color = colorScheme.surface,
            border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f)),
            tonalElevation = 10.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(accentColor.copy(alpha = 0.2f), CircleShape)
                )
                Spacer(Modifier.height(20.dp))

                // Direction icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))

                Text(
                    text = tx.recipient,
                    modifier = Modifier.clickable {
                        PesaActions.playClick(hapticFeedback)
                        PesaActions.copyToClipboard(
                            context, clipboard, hapticFeedback,
                            if (isIncome) "Source" else "Recipient",
                            tx.recipient
                        )
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isIncome) "Transaction Received" else "Outgoing Payment",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(15.dp))

                Text(
                    text = "${if (isIncome) "+" else "-"} ${
                        PesaTimeConfiguration.formatCurrency(
                            total
                        )
                    }",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Black),
                    color = if (isIncome) colorScheme.tertiary else colorScheme.error
                )

                Spacer(Modifier.height(16.dp))

                // Details card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            colorScheme.surfaceVariant.copy(alpha = 0.04f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailRow("REFERENCE", tx.id)
                    DetailRow("TIMESTAMP", "${tx.displayTime}, ${tx.displayDate}")

                    if (!isIncome) {
                        DetailRow("SUBTOTAL", PesaTimeConfiguration.formatCurrency(tx.amount))
                        DetailRow("FEES", PesaTimeConfiguration.formatCurrency(tx.transactionCost))
                    }

                    // ── Category row with tappable chip ───────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "CATEGORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colorScheme.primary.copy(alpha = 0.1f),
                            border = BorderStroke(0.5.dp, colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable {
                                PesaActions.playClick(hapticFeedback)
                                showCategoryPicker = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(resolvedCategory.emoji, fontSize = 14.sp)
                                Text(
                                    resolvedCategory.label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    Icons.Rounded.Edit, null,
                                    modifier = Modifier.size(12.dp),
                                    tint = colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colorScheme.onSurface.copy(alpha = 0.06f))
                    )

                    DetailRow(
                        "BALANCE AFTER",
                        PesaTimeConfiguration.formatCurrency(tx.balance),
                        isBold = true
                    )
                }

                Spacer(Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Close", color = colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            PesaActions.playClick(hapticFeedback)
                            Toast.makeText(
                                context,
                                "Feature coming soon! Josiah is still polishing the pulse. ✨",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.error.copy(alpha = 0.1f),
                            contentColor = colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = null
                    ) {
                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Rendered outside Dialog to overlay correctly
    if (showCategoryPicker) {
        CategoryPickerSheet(
            allCategories = allCategories,
            currentCategoryId = currentCategoryId,
            onPick = { newId ->
                currentCategoryId = newId
                categoryViewModel.updateTransactionCategory(tx.id, newId)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }
}

// ─── Detail row ───────────────────────────────────────────────────────────────

@Composable
fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontFamily = if (value.any { it.isDigit() }) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(2f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(amount)
}