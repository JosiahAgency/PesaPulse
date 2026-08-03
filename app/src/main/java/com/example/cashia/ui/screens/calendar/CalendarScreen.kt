package com.example.cashia.ui.screens.calendar

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CalendarViewModel
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.DataSyncBanner
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.standardScreenPadding
import com.example.cashia.ui.screens.allTransactions.TransactionItem
import com.example.cashia.ui.screens.analytics.FinancialOverviewCards
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.TransactionType
import com.example.cashia.utility.dashboardDayBucket
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel,
    calendarViewModel: CalendarViewModel
) {
    val windowedTransactions by calendarViewModel.windowedTransactionsFlow.collectAsStateWithLifecycle()
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()
    val reversalIndex by viewModel.reversalIndex.collectAsStateWithLifecycle()
    val upcomingUiState by upcomingViewModel.uiState.collectAsStateWithLifecycle()
    val upcomingAll by upcomingViewModel.upcoming.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current
    val calendarState by calendarViewModel.uiState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onPermissionGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (upcomingUiState.showSheet) {
        UpcomingTransactionSheet(
            uiState = upcomingUiState,
            viewModel = upcomingViewModel,
            allCategories = allCategories,
            categoryViewModel = categoryViewModel,
            onDismiss = upcomingViewModel::closeSheet
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = standardScreenPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (isSyncing) {
                item(key = "calendar_sync_banner") {
                    DataSyncBanner()
                }
            }
            item {
                FinancialOverviewCards(
                    windowedTransactions, calendarState.selectedDate, calendarState.selectedFilter,
                    calendarViewModel::setFilter
                )
            }
            item {
                FullMonthCalendar(
                    allTransactions = windowedTransactions,
                    selectedDate = calendarState.selectedDate,
                    upcomingTransactions = upcomingAll,
                    calendarViewModel::handleDayClick
                )
            }
            item {
                ListHeader(
                    selectedFilter = calendarState.selectedFilter,
                    filterByDay = calendarState.filterByDay,
                    selectedDate = calendarState.selectedDate,
                    calendarViewModel::clearDayFilter
                )
            }

            val noTransactions = calendarState.filteredTransactions.isEmpty()
            val noUpcoming = calendarState.upcomingForDay.isEmpty()

            if (noTransactions && (!calendarState.filterByDay || noUpcoming)) {
                item {
                    EmptyStateMessage(
                        title = "No transactions found",
                        message = "Try a different day or month.",
                        emoji = "📅"
                    )
                }
            } else {
                if (calendarState.filterByDay && !noUpcoming) {
                    items(
                        items = calendarState.upcomingForDay,
                        key = { it.id }
                    ) { upcomingTx ->
                        CalendarUpcomingItem(
                            dateStr = upcomingTx.dueDate
                        )
                    }
                }
                calendarState.dayGroups.forEach { dayGroup ->
                    item(key = "calendar_day_header_${dayGroup.date}") {
                        SectionLabel(dashboardDayBucket(dayGroup.date))
                    }
                    items(dayGroup.transactions, key = { it.id }) { tx ->
                        TransactionItem(
                            tx,
                            allCategories,
                            categoryViewModel,
                            upcomingViewModel,
                            hapticFeedback,
                            reversalIndex
                        )
                    }
                }
            }
        }

//        val showFab = YearMonth.from(calendarState.selectedDate) != YearMonth.from(today) ||
//                !calendarState.filterByDay
//
//        AnimatedVisibility(
//            visible = showFab,
//            enter = fadeIn(tween(200)),
//            exit = fadeOut(tween(150)),
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .padding(horizontal = 24.dp)
//                .navigationBarsPadding()
//                .padding(bottom = 98.dp)
//        ) {
//            HarmonizedActionFab(
//                onClick = calendarViewModel::resetToToday,
//                icon = Icons.Default.Today,
//                text = "Today",
//                contentDescription = "Today",
//                fullWidth = true
//            )
//        }
    }
}

@Composable
private fun ListHeader(
    selectedFilter: TransactionType,
    filterByDay: Boolean,
    selectedDate: LocalDate,
    onClearDayFilter: () -> Unit
) {

    val title = remember(selectedFilter, filterByDay, selectedDate) {
        val dateFormater = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

        when (selectedFilter) {
            TransactionType.ALL -> if (filterByDay) "Transactions for ${
                selectedDate.format(
                    dateFormater
                )
            }" else "${selectedDate.format(monthFormatter)} LEDGER"

            TransactionType.INCOME -> if (filterByDay) "Income for ${
                selectedDate.format(
                    dateFormater
                )
            }" else "Income Receipts (${selectedDate.format(monthFormatter)})"

            TransactionType.EXPENSE -> if (filterByDay) "Expenses for ${
                selectedDate.format(
                    dateFormater
                )
            }" else "Expense Debits (${selectedDate.format(monthFormatter)})"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(brush = primaryGradient(), shape = CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                SectionLabel(title)
            }
            if (filterByDay) {
                TextButton(
                    onClick = onClearDayFilter,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Show Month's Ledger",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(2.dp)
                .padding(top = 4.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun CalendarUpcomingItem(dateStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Today,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                "Scheduled Commitment",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Due: $dateStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}