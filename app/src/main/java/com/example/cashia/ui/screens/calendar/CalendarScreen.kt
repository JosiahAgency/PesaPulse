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
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.TransactionType
import java.time.LocalDate


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
    val state = viewModel.state
    val hapticFeedback = LocalHapticFeedback.current
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val upcomingAll by upcomingViewModel.upcoming.collectAsState()
    var selectedFilter by remember { mutableStateOf(TransactionType.ALL) }

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

    val filteredTransctions by remember(state.allTransactions, selectedDate, selectedFilter) {
        derivedStateOf {
            val monthlyPrefix =
                "${selectedDate.year}-${selectedDate.monthValue.toString().padStart(2, '0')}"
            state.allTransactions.filter { tx ->
                if (!tx.date.startsWith(monthlyPrefix)) return@filter false
                when (selectedFilter) {
                    TransactionType.ALL -> true
                    TransactionType.INCOME -> tx.type == "income"
                    TransactionType.EXPENSE -> tx.type == "expense"
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedDate != today) {
                ExtendedFloatingActionButton(
                    onClick = { selectedDate = today },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    text = { Text("Today", fontWeight = FontWeight.SemiBold) },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(brush = primaryGradient())
                )
            }
        }
    ) { padding ->
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
                        state.allTransactions,
                        selectedDate,
                        upcomingAll
                    ) { selectedDate = it }
                }
                item {
                    ListHeader(selectedFilter)
                }
                if (filteredTransctions.isEmpty() && upcomingForDay.isEmpty()) {
                    item { EmptyStateMessage("No Transactions") }
                } else {
                    items(filteredTransctions, key = { it.id }) { tx ->
                        TransactionItem(
                            tx,
                            allCategories,
                            categoryViewModel,
                            hapticFeedback,
                            reversalIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListHeader(selectedFilter: TransactionType) {
    val title = when (selectedFilter) {
        TransactionType.ALL -> "All Transactions"
        TransactionType.INCOME -> "Income Transactions Only"
        TransactionType.EXPENSE -> "Expense Transactions Only"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
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