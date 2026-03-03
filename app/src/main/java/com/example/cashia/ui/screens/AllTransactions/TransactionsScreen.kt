package com.example.cashia.ui.screens.AllTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.screens.Categorys.CategoryPickerSheet
import com.example.cashia.ui.theme.primaryGradient
import kotlinx.coroutines.launch
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
) {
    val state = viewModel.state
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val allCategories by categoryViewModel.allCategories.collectAsState()

    // ── Search state ──────────────────────────────────────────────────────
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf(listOf<String>()) }
    var dateRangeStart by remember { mutableStateOf<LocalDate?>(null) }
    var dateRangeEnd by remember { mutableStateOf<LocalDate?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    // Build category lookup map once
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }

    // ── Pre-parse all dates once ──────────────────────────────────────────
    val parsedAll by remember(state.allTransactions) {
        derivedStateOf {
            state.allTransactions.map { tx ->
                val date = runCatching { LocalDate.parse(tx.date) }.getOrElse { LocalDate.now() }
                ParsedTx(tx, date, weekBucket(date))
            }
        }
    }

    // ── Filter ────────────────────────────────────────────────────────────
    val filtered by remember(
        parsedAll, searchQuery, selectedCategoryFilter, dateRangeStart, dateRangeEnd
    ) {
        derivedStateOf {
            val amountQuery = searchQuery.trim().toDoubleOrNull()
            parsedAll.filter { p ->
                val tx = p.tx
                // Text search
                val textMatch = searchQuery.isBlank() ||
                        tx.recipient.contains(searchQuery, ignoreCase = true) ||
                        tx.id.contains(searchQuery, ignoreCase = true) ||
                        (amountQuery != null && tx.amount == amountQuery) ||
                        categoryMap[tx.category]?.label?.contains(
                            searchQuery,
                            ignoreCase = true
                        ) == true
                // Category filter chip
                val catMatch = selectedCategoryFilter == null ||
                        tx.category == selectedCategoryFilter
                // Date range
                val dateMatch = (dateRangeStart == null || !p.date.isBefore(dateRangeStart!!)) &&
                        (dateRangeEnd == null || !p.date.isAfter(dateRangeEnd!!))
                textMatch && catMatch && dateMatch
            }
        }
    }

    // ── Group: bucket → date → transactions ──────────────────────────────
    data class DayGroup(
        val date: LocalDate,
        val transactions: List<MpesaTransactions>,
        val dayIncome: Double,
        val dayExpense: Double,
        val dayBalance: Double   // last known balance in that day
    )

    data class WeekGroup(val bucket: String, val days: List<DayGroup>)

    val weekGroups by remember(filtered) {
        derivedStateOf {
            filtered
                .groupBy { it.bucket }
                .entries
                .sortedByDescending { entry ->
                    // Sort buckets by the latest date in each
                    entry.value.maxOf { it.date }
                }
                .map { (bucket, rows) ->
                    val days = rows
                        .groupBy { it.date }
                        .entries
                        .sortedByDescending { it.key }
                        .map { (date, dayRows) ->
                            val txs = dayRows.map { it.tx }
                            DayGroup(
                                date = date,
                                transactions = txs,
                                dayIncome = txs.filter { it.type == "income" }.sumOf { it.amount },
                                dayExpense = txs.filter { it.type == "expense" }
                                    .sumOf { it.amount },
                                dayBalance = txs.filter { it.balance > 0 }.lastOrNull()?.balance
                                    ?: 0.0
                            )
                        }
                    WeekGroup(bucket, days)
                }
        }
    }

    // ── Collapsed state ───────────────────────────────────────────────────
    var collapsedDates by remember { mutableStateOf(setOf<LocalDate>()) }
    var collapsedBuckets by remember { mutableStateOf(setOf<String>()) }

    // ── Back-to-top visibility ────────────────────────────────────────────
    val showBackToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    // ── Category quick-pick state (from swipe) ────────────────────────────
    var swipeCategoryTarget by remember { mutableStateOf<MpesaTransactions?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = Color.Transparent) { _ ->
            swipeCategoryTarget?.let { tx ->
                CategoryPickerSheet(
                    allCategories = allCategories,
                    currentCategoryId = tx.category,
                    onPick = { newId ->
                        categoryViewModel.updateTransactionCategory(tx.id, newId)
                        swipeCategoryTarget = null
                    },
                    onDismiss = { swipeCategoryTarget = null }
                )
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refreshFromSms() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ── Summary card ───────────────────────────────────────
                    item(key = "summary") {
                        CompactTransactionSummary(
                            balance = state.latestBalance,
                            income = state.totalIncome,
                            expenses = state.totalExpenses
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── Search + filter bar ────────────────────────────────
                    item(key = "search") {
                        EnhancedSearchBar(
                            query = searchQuery,
                            expanded = searchExpanded,
                            recentSearches = recentSearches,
                            allCategories = allCategories,
                            selectedCategory = selectedCategoryFilter,
                            dateRangeStart = dateRangeStart,
                            dateRangeEnd = dateRangeEnd,
                            onQueryChange = { searchQuery = it },
                            onExpandChange = { searchExpanded = it },
                            onSearch = { q ->
                                if (q.isNotBlank() && !recentSearches.contains(q)) {
                                    recentSearches = (listOf(q) + recentSearches).take(5)
                                }
                                searchExpanded = false
                            },
                            onRecentPick = { searchQuery = it; searchExpanded = false },
                            onCategoryPick = { selectedCategoryFilter = it },
                            onDateRange = { s, e -> dateRangeStart = s; dateRangeEnd = e },
                            onClearFilters = {
                                searchQuery = ""
                                selectedCategoryFilter = null
                                dateRangeStart = null
                                dateRangeEnd = null
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // ── Active filter pills ────────────────────────────────
                    val hasFilters = selectedCategoryFilter != null ||
                            dateRangeStart != null || searchQuery.isNotBlank()
                    if (hasFilters) {
                        item(key = "filter_pills") {
                            ActiveFilterPills(
                                searchQuery = searchQuery,
                                selectedCategory = selectedCategoryFilter?.let { categoryMap[it] },
                                dateRangeStart = dateRangeStart,
                                dateRangeEnd = dateRangeEnd,
                                onClearSearch = { searchQuery = "" },
                                onClearCategory = { selectedCategoryFilter = null },
                                onClearDate = { dateRangeStart = null; dateRangeEnd = null }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // ── Empty state ────────────────────────────────────────
                    if (filtered.isEmpty() && !state.isLoading) {
                        item(key = "empty") {
                            EmptyStateMessage(
                                if (hasFilters) "No matching transactions" else "No History Found",
                                haptic
                            )
                        }
                    }

                    // ── Week + day grouped list ────────────────────────────
                    weekGroups.forEach { weekGroup ->
                        val bucketCollapsed = collapsedBuckets.contains(weekGroup.bucket)

                        // Week bucket header
                        stickyHeader(key = "bucket_${weekGroup.bucket}") {
                            WeekBucketHeader(
                                label = weekGroup.bucket,
                                txCount = weekGroup.days.sumOf { it.transactions.size },
                                isCollapsed = bucketCollapsed,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    collapsedBuckets = if (bucketCollapsed)
                                        collapsedBuckets - weekGroup.bucket
                                    else collapsedBuckets + weekGroup.bucket
                                }
                            )
                        }

                        if (!bucketCollapsed) {
                            weekGroup.days.forEach { dayGroup ->
                                val dayCollapsed = collapsedDates.contains(dayGroup.date)
                                if (!dayCollapsed) {
                                    items(
                                        items = dayGroup.transactions,
                                        key = { it.id }
                                    ) { tx ->
                                        SwipeableTransactionItem(
                                            tx,
                                            resolveCategory(
                                                tx.category,
                                                allCategories
                                            ),
                                            allCategories,
                                            categoryViewModel,
                                            hapticFeedback = haptic,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showBackToTop,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clickable {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center, modifier = Modifier.background(
                        brush = primaryGradient()
                    )
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Back to top",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
