package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.ActionBanner
import com.example.cashia.ui.reusedElements.rememberCategoryIndex
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.EnhancedSearchBar
import com.example.cashia.ui.reusedElements.UnknownFeesNotice
import com.example.cashia.ui.reusedElements.FloatingQuickActionFab
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingPreviewSection
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.ui.theme.LocalNavigationDockHeight
import com.example.cashia.ui.theme.screenContentPadding
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel,
    onNavigateToUpcoming: () -> Unit = {},
    onNavigateToUnmatched: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val categoryIndex = rememberCategoryIndex(allCategories)
    val reversalIndex by viewModel.reversalIndex.collectAsStateWithLifecycle()
    val upcomingUiState by upcomingViewModel.uiState.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingReviewCount.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()


    var searchExpanded by remember { mutableStateOf(false) }

    var collapsedBuckets by remember { mutableStateOf(setOf<String>()) }

    val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }

    var localQuery by remember { mutableStateOf(state.searchQuery) }

    val hasFilters by remember {
        derivedStateOf {
            state.selectedCategory != null || localQuery.isNotBlank() || state.dateRangeStart != null
        }
    }

    LaunchedEffect(localQuery) {
        viewModel.setQuery(localQuery)
    }

    // Grow the DB window when the user scrolls near the end of the loaded rows.
    val nearListEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 15
        }
    }
    LaunchedEffect(nearListEnd, state.hasMore) {
        if (nearListEnd && state.hasMore) viewModel.loadMore()
    }

    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = Color.Transparent) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(
                        isRefreshing = isSyncing,
                        state = pullToRefreshState,
                        onRefresh = viewModel::refreshFromSms
                    ),
                contentPadding = screenContentPadding(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item(key = "known_fees_notice") {
                    UnknownFeesNotice(
                        unknownFeeCount = state.unknownFeeCount,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                item(key = "search") {
                    EnhancedSearchBar(
                        query = localQuery,
                        expanded = searchExpanded,
                        recentSearches = recentSearches,
                        allCategories = allCategories,
                        selectedCategory = state.selectedCategory,
                        dateRangeStart = state.dateRangeStart,
                        dateRangeEnd = state.dateRangeEnd,
                        onQueryChange = { localQuery = it },
                        onExpandChange = { searchExpanded = it },
                        onRecentPick = {
                            localQuery = it
                            searchExpanded = false
                            viewModel.commitSearch(it)
                        },
                        onCommitSearch = viewModel::commitSearch,
                        onClearRecents = viewModel::clearRecentSearches,
                        onCategoryPick = viewModel::setCategory,
                        onDateRange = { start, end ->
                            viewModel.setDateRange(start, end)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (state.fulizaLimit > 0.0 && !hasFilters) {
                    item(key = "fuliza_limit_card") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "🔄 Current Fuliza Limit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = PesaTimeConfiguration.formatCurrency(state.fulizaLimit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
                if (hasFilters) {
                    item(key = "filter_pills") {
                        ActiveFilterPills(
                            searchQuery = state.searchQuery,
                            selectedCategory = categoryIndex.resolve(state.selectedCategory),
                            dateRangeStart = state.dateRangeStart,
                            dateRangeEnd = state.dateRangeEnd,
                            onClearSearch = {
                                localQuery = ""
                            },
                            onClearCategory = {
                                viewModel.setCategory(null)
                            },
                            onClearDate = {
                                viewModel.setDateRange(null, null)
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (!hasFilters) {
                    item(key = "upcoming_preview") {
                        UpcomingPreviewSection(
                            upcomingViewModel = upcomingViewModel,
                            allCategories = allCategories,
                            onViewAll = onNavigateToUpcoming
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (pendingCount > 0) {
                    item(key = "unmatched_banner") {
                        ActionBanner(
                            icon = Icons.Rounded.Sms,
                            iconTint = MaterialTheme.colorScheme.error,
                            title = "Review unmatched SMS",
                            subtitle = "$pendingCount transaction${if (pendingCount == 1) "" else "s"} need review",
                            badgeCount = pendingCount,
                            onClick = onNavigateToUnmatched
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (state.filteredTransactions.isEmpty()) {
                    item(key = "empty") {
                        if (!state.hasLoaded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (hasFilters) {
                            EmptyStateMessage(
                                title = "No matching transactions",
                                message = "Try adjusting your search or filters.",
                                emoji = "🔍"
                            )
                        } else {
                            EmptyStateMessage(
                                title = "No history found",
                                message = "Your transactions will show up here once synced.",
                                emoji = "🧾"
                            )
                        }
                    }
                }

                state.groupedTransactions.forEach { weekGroup ->
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
                            items(
                                items = dayGroup.transactions,
                                key = { it.id }
                            ) { tx ->
                                TransactionItem(
                                    tx = tx,
                                    categories = categoryIndex,
                                    categoryViewModel = categoryViewModel,
                                    upcomingViewModel = upcomingViewModel,
                                    hapticFeedback = haptic,
                                    reversalIndex = reversalIndex
                                )
                            }
                        }
                    }
                }
            }
        }

        if (upcomingUiState.showSheet) {
            UpcomingTransactionSheet(
                uiState = upcomingUiState,
                viewModel = upcomingViewModel,
                allCategories = allCategories,
                categoryViewModel = categoryViewModel,
                onDismiss = { upcomingViewModel.closeSheet() }
            )
        }

        PullToRefreshDefaults.Indicator(
            state = pullToRefreshState,
            isRefreshing = isSyncing,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        AnimatedVisibility(
            visible = showBackToTop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = LocalNavigationDockHeight.current + 12.dp)
        ) {
            FloatingQuickActionFab(
                icon = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Back to top",
                onClick = {
                    coroutineScope.launch { listState.scrollToItem(0) }
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }
    }
}
