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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.reusedElements.EmptyStateMessage
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.screens.allTransactions.TransactionItem
import com.example.cashia.ui.screens.analytics.FinancialOverviewCards
import com.example.cashia.ui.screens.upcomingTransactions.UpcomingTransactionSheet
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: TransactionsViewModel,
    categoryViewModel: CategoryViewModel,
    upcomingViewModel: UpcomingViewModel,
) {
    val today = remember { LocalDate.now() }
    val reversalIndex by viewModel.reversalIndex.collectAsState()
    var selectedDate by remember { mutableStateOf(today) }
    var filterByDay by remember { mutableStateOf(false) }
    val state = viewModel.state
    val hapticFeedback = LocalHapticFeedback.current
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val upcomingAll by upcomingViewModel.upcoming.collectAsState()
    var selectedFilter by remember { mutableStateOf(TransactionType.ALL) }
    val upcomingUiState = upcomingViewModel.uiState

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onPermissionGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val upcomingForDay by remember(upcomingAll, selectedDate) {
        derivedStateOf {
            val dateStr = selectedDate.toString()
            upcomingAll.filter { it.dueDate == dateStr }
        }
    }

    val filteredTransctions by remember(
        state.allTransactions,
        selectedDate,
        selectedFilter,
        filterByDay
    ) {
        derivedStateOf {
            val monthlyPrefix =
                "${selectedDate.year}-${selectedDate.monthValue.toString().padStart(2, '0')}"
            val dayString = selectedDate.toString()

            state.allTransactions.filter { tx ->
                if (filterByDay) {
                    if (tx.date != dayString) return@filter false
                } else {
                    if (!tx.date.startsWith(monthlyPrefix)) return@filter false
                }
                when (selectedFilter) {
                    TransactionType.ALL -> true
                    TransactionType.INCOME -> tx.type.equals("income", ignoreCase = true)
                    TransactionType.EXPENSE -> tx.type.equals("expense", ignoreCase = true)
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedDate != today || !filterByDay) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedDate = today
                        filterByDay = true
                    },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    text = { Text("Today", fontWeight = FontWeight.SemiBold) },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                    ),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(brush = primaryGradient())
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.refreshFromSms() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding()
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        FinancialOverviewCards(
                            state.allTransactions,
                            selectedDate,
                            selectedFilter,
                            { selectedFilter = it })
                    }
                    item {
                        FullMonthCalendar(
                            allTransactions = state.allTransactions,
                            selectedDate = selectedDate,
                            upcomingTransactions = upcomingAll
                        ) { clickedDate ->
                            filterByDay =
                                !(clickedDate.month != selectedDate.month || clickedDate.year != selectedDate.year)
                            selectedDate = clickedDate
                        }
                    }
                    item {
                        ListHeader(
                            selectedFilter = selectedFilter,
                            filterByDay = filterByDay,
                            selectedDate = selectedDate,
                            onClearDayFilter = { filterByDay = false }
                        )
                    }

                    if (filteredTransctions.isEmpty() && (!filterByDay || upcomingForDay.isEmpty())) {
                        item { EmptyStateMessage("No Transactions Found") }
                    } else {
                        // Render upcoming scheduled liabilities when inspecting specific date nodes
                        if (filterByDay && upcomingForDay.isNotEmpty()) {
                            items(
                                upcomingForDay,
                                key = { "upcoming-${it.dueDate}-${filteredTransctions.size}" }) { upcomingTx ->
                                CalendarUpcomingItem(
                                    title = "Scheduled Commitment",
                                    dateStr = upcomingTx.dueDate
                                )
                            }
                        }
                        items(filteredTransctions, key = { it.id }) { tx ->
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
            if (upcomingUiState.showSheet) {
                UpcomingTransactionSheet(
                    uiState = upcomingUiState,
                    viewModel = upcomingViewModel,
                    allCategories = allCategories,
                    categoryViewModel = categoryViewModel,
                    onDismiss = { upcomingViewModel.closeSheet() }
                )
            }

        }
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
private fun CalendarUpcomingItem(title: String, dateStr: String) {
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
                title,
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