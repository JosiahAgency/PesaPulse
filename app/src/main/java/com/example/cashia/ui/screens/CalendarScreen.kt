package com.example.cashia.ui.screens

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
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cashia.Utility.TimeScope
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.screens.AllTransactions.EmptyStateMessage
import com.example.cashia.ui.screens.AllTransactions.TransactionDetailDialog
import com.example.cashia.ui.screens.AllTransactions.TransactionItem
import com.example.cashia.ui.screens.Analytics.FinancialOverviewCards
import com.example.cashia.ui.screens.Analytics.GlassySurface
import com.example.cashia.ui.theme.primaryGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: TransactionsViewModel) {
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
                item {
                    FinancialOverviewCards(state.allTransactions, selectedDate) { selectedDate = it }
                }
                item {
                    FullMonthCalendar(state.allTransactions, selectedDate) { selectedDate = it }
                }
                item {
                    TransactionFrequencyCard(state.allTransactions, selectedDate) {
                        selectedDate = it
                    }
                }

                val transactionsForDay = state.allTransactions.filter {
                    LocalDate.parse(it.date) == selectedDate
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
                        ) {
                            // A small "Pulse" dot to signify this is the active day
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(brush = primaryGradient(), shape = CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
                                    .uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
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
fun TransactionFrequencyCard(
    allTransactions: List<MpesaTransactions>, selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val selectedMonth = selectedDate.month
    val selectedYear = selectedDate.year

    val monthlyTransactions = remember(allTransactions, selectedDate) {
        allTransactions.mapNotNull { txn ->
            runCatching {
                txn to LocalDate.parse(txn.date)
            }.getOrNull()
        }.filter { (_, date) ->
            date.month == selectedMonth && date.year == selectedYear
        }
    }

    val frequencyData = remember(monthlyTransactions) {
        val incomeMap = monthlyTransactions
            .filter { it.first.type == "income" }
            .groupBy { it.second }

        val expenseMap = monthlyTransactions
            .filter { it.first.type == "expense" }
            .groupBy { it.second }

        val topIncome = incomeMap.maxByOrNull { it.value.size }
        val topExpense = expenseMap.maxByOrNull { it.value.size }

        Triple(topIncome, topExpense, monthlyTransactions.size)
    }

    val (topIncome, topExpense, totalCount) = frequencyData

    val selectedDateStr = selectedDate.toString()
    val activeDayIncome = monthlyTransactions.count {
        it.second == selectedDate && it.first.type == "income"
    }
    val activeDayExpense = monthlyTransactions.count {
        it.second == selectedDate && it.first.type == "expense"
    }


    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "MONTHLY ACTIVITY PEAKS",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Black
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        GlassySurface {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FrequencyMetric(
                        modifier = Modifier.weight(1f),
                        label = "Income Peak",
                        dateString = topIncome?.key?.toString(),
                        count = topIncome?.value?.size ?: 0,
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Default.TrendingUp,
                        onClick = {
                            topIncome?.key?.let(onDateSelected)
                        }
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            .align(Alignment.CenterVertically)
                    )

                    // Expense Frequency Side
                    FrequencyMetric(
                        modifier = Modifier.weight(1f),
                        label = "Expense Peak",
                        dateString = topExpense?.key?.toString(),
                        count = topExpense?.value?.size ?: 0,
                        color = MaterialTheme.colorScheme.error,
                        icon = Icons.Default.TrendingDown,
                        onClick = {
                            topExpense?.key?.let(onDateSelected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyMetric(
    modifier: Modifier,
    label: String,
    dateString: String?,
    count: Int,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val formattedDate = remember(dateString) {
        if (dateString != null) {
            LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("dd MMM, yyyy"))
        } else "No Data"
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun SelectionBadge(count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}
