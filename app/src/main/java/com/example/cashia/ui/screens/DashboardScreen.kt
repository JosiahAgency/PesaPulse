package com.example.cashia.ui.screens

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cashia.Utility.TimeScope
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.SummaryData
import com.example.cashia.models.TransactionsViewModel
import com.google.accompanist.pager.rememberPagerState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TransactionsViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val state = viewModel.state
    var selectedTransaction by remember { mutableStateOf<MpesaTransactions?>(null) }


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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }) { padding ->
        selectedTransaction?.let { tx ->
            TransactionDetailDialog(
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
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TimeScopedSummaryCard(
                        selectedScope,
                        { selectedScope = it },
                        summaryData,
                        state.latestBalance
                    )
                }
                item {
                    FullMonthCalendar(state.allTransactions, selectedDate, { selectedDate = it })
                }
                val transactionsForDay = state.allTransactions.filter {
                    LocalDate.parse(it.date) == selectedDate
                }
                item {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (transactionsForDay.isEmpty()) {
                    item { EmptyStateMessage("No Transactions On This Day") }
                } else {
                    items(transactionsForDay, key = { it.id }) { tx ->
                        TransactionItem(tx, onClick = { selectedTransaction = tx })
                    }
                }
            }
        }
    }
}

@Composable
fun TimeScopedSummaryCard(
    scope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    summaryData: SummaryData,
    currentBalance: Double
) {
    val income = summaryData.income
    val expenses = summaryData.expenses
    val startDate = summaryData.startDate
    val endDate = summaryData.endDate

    val monthDayFormatter = DateTimeFormatter.ofPattern("dd MMM")
    val fullDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    val dateDisplay = if (scope == TimeScope.DAY) {
        "Summary for ${startDate.format(fullDateFormatter)}"
    } else if (scope == TimeScope.WEEK) {
        val start = startDate.format(monthDayFormatter)
        val end = endDate.format(fullDateFormatter)
        "Weekly Summary: $start - $end"
    } else if (scope == TimeScope.MONTH) {
        val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        "Monthly Summary: ${startDate.format(monthYearFormatter)}"
    } else {
        val start = startDate.format(monthDayFormatter)
        val end = endDate.format(fullDateFormatter)
        "Weekly Summary: $start - $end"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.background),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimeScope.entries.filter { it != TimeScope.CUSTOM }.forEach { currentScope ->
                    val isSelected = currentScope == scope
                    Text(
                        text = currentScope.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onScopeChange(currentScope) }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .padding(vertical = 8.dp))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {

                Column {
                    Text(
                        "${scope.name.lowercase().replaceFirstChar { it.uppercase() }} Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dateDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Current Balance", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Ksh ${formatAmount(currentBalance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val isCompact = maxWidth < 300.dp

                val content = @Composable {
                    SummaryItem(
                        "Amount Received",
                        income,
                        MaterialTheme.colorScheme.tertiary,
                        Icons.Default.ArrowCircleDown
                    )
                    SummaryItem(
                        "Amount Spent",
                        expenses,
                        MaterialTheme.colorScheme.error,
                        Icons.Default.ArrowCircleUp
                    )
                }

                if (isCompact) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        content()
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.1f),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Ksh ${formatAmount(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}