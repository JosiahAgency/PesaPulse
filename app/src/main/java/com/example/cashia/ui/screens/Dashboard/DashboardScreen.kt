package com.example.cashia.ui.screens.Dashboard

import android.os.Build
import androidx.annotation.RequiresExtension
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TimeScope
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.screens.AllTransactions.EmptyStateMessage
import com.example.cashia.ui.screens.AllTransactions.TransactionDetailDialog
import com.example.cashia.ui.screens.AllTransactions.TransactionItem
import com.example.cashia.ui.screens.WeeklyCalendar
import com.example.cashia.ui.theme.primaryGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TransactionsViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val state = viewModel.state
    var selectedTransaction by remember { mutableStateOf<MpesaTransactions?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    var isListExpanded by remember { mutableStateOf(true) }

    val firstDay = PesaTimeConfiguration.firstDayOfWeek
    val startOfWeek = selectedDate.with(
        TemporalAdjusters.previousOrSame(firstDay)
    )
    val endOfWeek = startOfWeek.plusDays(6)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedScope by remember { mutableStateOf(TimeScope.WEEK) }
    val summaryData by remember(selectedDate, selectedScope, state.allTransactions) {
        derivedStateOf {
            viewModel.calculateSummary(selectedDate, selectedScope)
        }
    }

    val transactionsForScope = remember(state.allTransactions, selectedDate, selectedScope) {
        state.allTransactions.filter { transaction ->
            val txDate = LocalDate.parse(transaction.date)

            when (selectedScope) {
                TimeScope.DAY -> {
                    txDate == selectedDate
                }

                TimeScope.WEEK -> {
                    !txDate.isBefore(startOfWeek) && !txDate.isAfter(endOfWeek)
                }

                TimeScope.MONTH -> {
                    txDate.month == selectedDate.month && txDate.year == selectedDate.year
                }

                else -> txDate == selectedDate
            }
        }.sortedByDescending { it.date } // Keep newest at the top
    }

    val scopeLabel = remember(selectedDate, selectedScope) {
        when (selectedScope) {
            TimeScope.DAY -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
            TimeScope.WEEK -> {
                "${startOfWeek.format(DateTimeFormatter.ofPattern("dd MMM"))} - ${
                    endOfWeek.format(
                        DateTimeFormatter.ofPattern("dd MMM yyyy")
                    )
                }"
            }

            TimeScope.MONTH -> selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            else -> ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, floatingActionButton = {
            if (selectedDate != today) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedDate = today
                    },
                    icon = {
                        Icon(Icons.Default.Today, contentDescription = "Today")

                    },
                    text = {
                        Text("Today", fontWeight = FontWeight.SemiBold)
                    },
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
                        .background(
                            brush = primaryGradient()
                        )
                )
            }
        }) { padding ->
        selectedTransaction?.let { tx ->
            TransactionDetailDialog(
                hapticFeedback,
                tx = tx,
                onDismiss = { selectedTransaction = null },
                onEdit = { /* Handle Edit logic */ },
                onDelete = {
//                    viewModel.deleteTransaction(it)
//                    selectedTransaction = null
                }
            )
        }
        PullToRefreshBox(
            isRefreshing = state.isLoading, onRefresh = {
                viewModel.refreshFromSms()
            }, modifier = Modifier
                .fillMaxSize()
                .padding()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
//                item {
//                    Row() {
//                        Text(
//                            text = "${PesaActions.getGreeting()},",
//                            style = MaterialTheme.typography.bodyLarge,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
//                        )
////                        Text(
////                            text = "Josiah",
////                            style = MaterialTheme.typography.bodyLarge.copy(
////                                brush = primaryGradient() // Use your brand gradient for the name
////                            )
////                        )
//                    }
//
//                }

                item {
                    TimeScopedSummaryCard(
                        selectedScope,
                        { selectedScope = it },
                        summaryData,
                        state.latestBalance,
                        viewModel.isBalanceVisible,
                        viewModel::toggleBalanceVisibility,
                        hapticFeedback
                    )
                }
                item {
                    WeeklyCalendar(
                        hapticFeedback,
                        state.allTransactions,
                        selectedDate
                    ) { selectedDate = it }
                }
                item {
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
                                .clickable {
                                    PesaActions.playClick(hapticFeedback)
                                    isListExpanded = !isListExpanded
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(brush = primaryGradient(), shape = CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = scopeLabel.uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isListExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (isListExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(2.dp)
                                .padding(top = 4.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
                if (transactionsForScope.isNotEmpty() && isListExpanded) {
                    items(transactionsForScope, key = { it.id }) { tx ->
                        TransactionItem(tx, onClick = { selectedTransaction = tx }, hapticFeedback)
                    }
                } else if (transactionsForScope.isEmpty() && isListExpanded) {
                    item { EmptyStateMessage("No Transactions On This Day", hapticFeedback) }
                } else {
                    item {
                    }
                }

            }
        }
    }
}