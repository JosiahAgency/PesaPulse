package com.example.cashia.ui.screens.allTransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.EnhancedSearchBar
import com.example.cashia.ui.reusedElements.PendingReviewBadge
import com.example.cashia.ui.screens.category.CategoryPickerSheet
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionsSection
import com.example.cashia.ui.theme.primaryGradient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel
) {
    val state by viewModel.state.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val allCategories by categoryViewModel.allCategories.collectAsState()
    val reversalIndex by viewModel.reversalIndex.collectAsState()
    val upcomingUiState by upcomingViewModel.uiState.collectAsStateWithLifecycle()

    var searchExpanded by remember { mutableStateOf(false) }
    var recentSearches by remember { mutableStateOf(listOf<String>()) }

//    val searchQuery = state.searchQuery
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var dateRangeStart by remember { mutableStateOf<LocalDate?>(null) }
    var dateRangeEnd by remember { mutableStateOf<LocalDate?>(null) }

    var collapsedBuckets by remember { mutableStateOf(setOf<String>()) }

    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }
    val showBackToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }

    LaunchedEffect(
        state.searchQuery,
        selectedCategoryFilter,
        dateRangeStart,
        dateRangeEnd,
    ) {
        viewModel.updateFilters(
            query = state.searchQuery,
            categoryFilter = selectedCategoryFilter,
            start = dateRangeStart,
            end = dateRangeEnd,
            categoryMap = allCategories.associate { it.id to it.label }
        )
    }

    val hasFilters by remember {
        derivedStateOf {
            state.selectedCategory != null || state.searchQuery.isNotBlank()
        }
    }

    var swipeCategoryTarget by remember { mutableStateOf<MpesaTransactions?>(null) }

    val pendingCount by viewModel.pendingReviewCount.collectAsState()
    val nextPending by viewModel.nextPending.collectAsState()
    val allUnmatched by viewModel.allUnmatched.collectAsState()

    var showUnmatchedModal by remember { mutableStateOf(false) }
    var modalTargetSms by remember { mutableStateOf<UnmatchedSms?>(null) }
    var unmatchedExpanded by remember { mutableStateOf(false) }

    var hasAutoOpened by remember { mutableStateOf(false) }

    LaunchedEffect(pendingCount) {
        if (pendingCount > 0 && !hasAutoOpened) {
            hasAutoOpened = true
            modalTargetSms = nextPending
        }
    }

    LaunchedEffect(nextPending) {
        if (showUnmatchedModal && nextPending == null) {
            showUnmatchedModal = false
            modalTargetSms = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = Color.Transparent) { padding ->
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

            if (showUnmatchedModal) {
                modalTargetSms?.let { target ->
                    val advance = { id: Long ->
                        val next = nextPending?.takeIf { it.id != id }
                        if (next != null) modalTargetSms = next
                        else {
                            showUnmatchedModal = false; modalTargetSms = null
                        }
                    }
                    UnmatchedSmsModal(
                        sms = target,
                        pendingCount = pendingCount,
                        allCategories = allCategories,
                        categoryViewModel = categoryViewModel,
                        onSave = { type, category ->
                            viewModel.saveTransactionFromSms(target, type, category)
                            advance(target.id)
                        },
                        onSkip = {
                            viewModel.markSkipped(target.id)
                            advance(target.id)
                        },
                        onIgnore = {
                            viewModel.markIgnored(target)
                            advance(target.id)
                        },
                        onDismiss = { showUnmatchedModal = false }
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                item(key = "search") {
                    EnhancedSearchBar(
                        query = state.searchQuery,
                        expanded = searchExpanded,
                        recentSearches = recentSearches,
                        allCategories = allCategories,
                        selectedCategory = state.selectedCategory,
                        dateRangeStart = null,
                        dateRangeEnd = null,
                        onQueryChange = viewModel::setQuery,
                        onExpandChange = { searchExpanded = it },
                        onRecentPick = {
                            viewModel.setQuery(it)
                            searchExpanded = false
                        },
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
                                    text = "Ksh ${
                                        String.format(
                                            Locale.US,
                                            "%,.2f",
                                            state.fulizaLimit
                                        )
                                    }",
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
                            selectedCategory = selectedCategoryFilter?.let { categoryMap[it] },
                            dateRangeStart = dateRangeStart,
                            dateRangeEnd = dateRangeEnd,
                            onClearSearch = {
                                viewModel.setQuery("")
                            },
                            onClearCategory = {
                                selectedCategoryFilter = null
                                viewModel.setCategory(null)
                            },
                            onClearDate = {
                                dateRangeStart = null
                                dateRangeEnd = null
                                viewModel.setDateRange(null, null)
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (!hasFilters) {
                    item(key = "upcoming_section") {
                        UpcomingTransactionsSection(
                            upcomingViewModel = upcomingViewModel,
                            categoryViewModel = categoryViewModel,
                            allCategories = allCategories
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }

                item(key = "pending_badge") {
                    PendingReviewBadge(
                        count = pendingCount,
                        onClick = {
                            modalTargetSms = nextPending
                            showUnmatchedModal = true
                        },
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

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
                                    horizontal = 14.dp, vertical = 10.dp
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
                                { clicked -> viewModel.markIgnored(clicked) }
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

                if (state.filteredTransactions.isEmpty()) {
                    item(key = "empty") {
                        EmptyStateMessage(
                            if (hasFilters) "No matching transactions" else "No History Found"
                        )
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
                                    allCategories = allCategories,
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

        AnimatedVisibility(
            visible = showBackToTop,
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