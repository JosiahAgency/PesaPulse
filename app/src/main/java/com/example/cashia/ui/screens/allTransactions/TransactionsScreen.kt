package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.EnhancedSearchBar
import com.example.cashia.ui.reusedElements.PendingReviewBadge
import com.example.cashia.ui.screens.category.CategoryPickerSheet
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.ParsedTx
import com.example.cashia.utility.weekBucket
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class DayGroup(
    val date: LocalDate,
    val transactions: List<MpesaTransactions>,
    val dayIncome: Double,
    val dayExpense: Double,
    val dayBalance: Double
)

private data class WeekGroup(val bucket: String, val days: List<DayGroup>)

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
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf(listOf<String>()) }
    var dateRangeStart by remember { mutableStateOf<LocalDate?>(null) }
    var dateRangeEnd by remember { mutableStateOf<LocalDate?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val today = remember { LocalDate.now() }

    val parsedAll by remember(state.allTransactions) {
        derivedStateOf {
            state.allTransactions.map { tx ->
                val date = runCatching { LocalDate.parse(tx.date) }.getOrElse { today }
                ParsedTx(tx, date, weekBucket(date))
            }
        }
    }
    val filtered by remember(
        parsedAll, searchQuery, selectedCategoryFilter, dateRangeStart, dateRangeEnd
    ) {
        derivedStateOf {
            val amountQuery = searchQuery.trim().toDoubleOrNull()
            parsedAll.filter { p ->
                val tx = p.tx
                val textMatch = searchQuery.isBlank() ||
                        tx.recipient.contains(searchQuery, ignoreCase = true) ||
                        tx.id.contains(searchQuery, ignoreCase = true) ||
                        (amountQuery != null && tx.amount == amountQuery) ||
                        categoryMap[tx.category]?.label?.contains(
                            searchQuery,
                            ignoreCase = true
                        ) == true
                val catMatch =
                    selectedCategoryFilter == null || tx.category == selectedCategoryFilter
                val dateMatch =
                    (dateRangeStart == null || !p.date.isBefore(dateRangeStart!!)) &&
                            (dateRangeEnd == null || !p.date.isAfter(dateRangeEnd!!))
                textMatch && catMatch && dateMatch
            }
        }
    }
    val weekGroups by remember(filtered) {
        derivedStateOf {
            filtered
                .groupBy { it.bucket }
                .entries
                .sortedByDescending { entry -> entry.value.maxOf { it.date } }
                .map { (bucket, rows) ->
                    val days = rows
                        .groupBy { it.date }
                        .entries
                        .sortedByDescending { it.key }
                        .map { (date, dayRows) ->
                            val txs = dayRows.map { it.tx }
                            var inc = 0.0
                            var exp = 0.0
                            for (tx in txs) {
                                when (tx.type) {
                                    "income" -> inc += tx.amount
                                    "expense" -> exp += tx.amount
                                }
                            }
                            DayGroup(
                                date = date,
                                transactions = txs,
                                dayIncome = inc,
                                dayExpense = exp,
                                dayBalance = txs.lastOrNull { it.balance > 0 }?.balance ?: 0.0
                            )
                        }
                    WeekGroup(bucket, days)
                }
        }
    }

    var collapsedBuckets by remember { mutableStateOf(setOf<String>()) }
    val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }
    var swipeCategoryTarget by remember { mutableStateOf<MpesaTransactions?>(null) }

    val pendingCount by viewModel.pendingReviewCount.collectAsState()
    val nextPending by viewModel.nextPending.collectAsState()
    val allUnmatched by viewModel.allUnmatched.collectAsState()

    var showUnmatchedModal by remember { mutableStateOf(false) }
    // Holds whichever specific SMS the modal is currently showing.
    // Kept separate from nextPending so the user can tap any item in the list to open it.
    var modalTargetSms by remember { mutableStateOf<UnmatchedSms?>(null) }
    var unmatchedExpanded by remember { mutableStateOf(false) }

    // ── Auto-open: show modal once on first load if there are pending items ──
    // Uses a flag so navigating away and back doesn't re-open it.
    var hasAutoOpened by remember { mutableStateOf(false) }
    LaunchedEffect(pendingCount) {
        if (pendingCount > 0 && !hasAutoOpened) {
            hasAutoOpened = true
            modalTargetSms = nextPending   // snapshot current value; nextPending may change later
            showUnmatchedModal = true
        }
    }

    // ── After save/skip: advance to the next pending item ────────────────────
    // When the ViewModel emits a new nextPending we update the modal target
    // WITHOUT closing it, so the user flows through the queue seamlessly.
    // If nextPending becomes null the queue is empty — close the modal.
    LaunchedEffect(nextPending) {
        if (showUnmatchedModal) {
            if (nextPending == null) {
                showUnmatchedModal = false
                modalTargetSms = null
            } else {
                // Only advance automatically when the modal is already open (i.e. user
                // just saved/skipped). If they opened a specific item from the list we
                // don't want to replace it mid-interaction — that's handled by onSave/onSkip.
                // So we only update here when modalTargetSms is null (queue just emptied)
                // or matches the previous nextPending (we were in auto-advance mode).
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = Color.Transparent) { padding ->

            // ── Category swipe sheet ──────────────────────────────────────
            swipeCategoryTarget?.let { tx ->
                CategoryPickerSheet(
                    allCategories = allCategories,
                    currentCategoryId = tx.category,
                    onPick = { newId ->
                        categoryViewModel.updateTransactionCategory(tx.id, newId)
                        swipeCategoryTarget = null
                    },
                    onDismiss = { swipeCategoryTarget = null },
                    categoryViewModel = categoryViewModel
                )
            }

            // ── Unmatched SMS modal ───────────────────────────────────────
            // Guard on both showUnmatchedModal AND modalTargetSms != null so we
            // never render the modal with a null target (which would crash or show stale data).
            if (showUnmatchedModal) {
                modalTargetSms?.let { target ->
                    UnmatchedSmsModal(
                        target,
                        pendingCount,
                        allCategories,
                        categoryViewModel,
                        { type, category ->
                            viewModel.saveManualTransaction(target, type, category)
                            val next = nextPending?.takeIf { it.id != target.id }
                            if (next != null) {
                                modalTargetSms = next
                            } else {
                                showUnmatchedModal = false
                                modalTargetSms = null
                            }
                        },
                        {
                            viewModel.markSkipped(target.id)
                            val next = nextPending?.takeIf { it.id != target.id }
                            if (next != null) {
                                modalTargetSms = next
                            } else {
                                showUnmatchedModal = false
                                modalTargetSms = null
                            }
                        },
                        {
                            showUnmatchedModal = false
                        },
                        {
                            showUnmatchedModal = false
                        }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refreshFromSms() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ── Search bar ────────────────────────────────────────
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
                            onRecentPick = { searchQuery = it; searchExpanded = false },
                            onCategoryPick = { selectedCategoryFilter = it },
                            onDateRange = { s, e -> dateRangeStart = s; dateRangeEnd = e },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // ── Active filter pills ───────────────────────────────
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

                    // ── Empty state ───────────────────────────────────────
                    if (filtered.isEmpty() && !state.isLoading) {
                        item(key = "empty") {
                            EmptyStateMessage(
                                if (hasFilters) "No matching transactions" else "No History Found"
                            )
                        }
                    }

                    // ── Pending review badge ──────────────────────────────
                    item(key = "pending_badge") {
                        PendingReviewBadge(
                            count = pendingCount,
                            onClick = {
                                // Always snap to the current nextPending when tapping the badge,
                                // not whatever stale modalTargetSms might be sitting there.
                                modalTargetSms = nextPending
                                showUnmatchedModal = true
                            },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // ── Unmatched SMS list ────────────────────────────────
                    if (allUnmatched.isNotEmpty()) {
                        item(key = "unmatched_header") {
                            Surface(
                                onClick = { unmatchedExpanded = !unmatchedExpanded },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 14.dp,
                                        vertical = 10.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Unrecognised Messages (${allUnmatched.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        if (unmatchedExpanded) Icons.Rounded.KeyboardArrowUp
                                        else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        if (unmatchedExpanded) {
                            items(items = allUnmatched, key = { "unmatched_${it.id}" }) { sms ->
                                UnmatchedSmsItem(
                                    sms,
                                    { clicked ->
                                        modalTargetSms = clicked
                                        showUnmatchedModal = true
                                    },
                                    { clicked ->
                                        viewModel.markIgnored(clicked)
                                    }
                                )
                            }
                            item(key = "unmatched_divider") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }

                    // ── Transactions grouped by week then day ─────────────
                    weekGroups.forEach { weekGroup ->
                        val bucketCollapsed = collapsedBuckets.contains(weekGroup.bucket)

                        stickyHeader(key = "bucket_${weekGroup.bucket}") {
                            WeekBucketHeader(
                                label = weekGroup.bucket,
                                txCount = weekGroup.days.sumOf { it.transactions.size },
                                isCollapsed = bucketCollapsed,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    collapsedBuckets = if (bucketCollapsed)
                                        collapsedBuckets - weekGroup.bucket
                                    else
                                        collapsedBuckets + weekGroup.bucket
                                }
                            )
                        }

                        if (!bucketCollapsed) {
                            weekGroup.days.forEach { dayGroup ->
                                items(items = dayGroup.transactions, key = { it.id }) { tx ->
                                    TransactionItem(
                                        tx,
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

        // ── Back-to-top FAB ───────────────────────────────────────────────
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
                        coroutineScope.launch { listState.scrollToItem(0) }
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.background(brush = primaryGradient())
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