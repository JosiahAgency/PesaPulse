package com.example.cashia.ui.screens

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.screens.AllTransactions.EmptyStateMessage
import com.example.cashia.ui.screens.AllTransactions.TransactionDetailDialog
import com.example.cashia.ui.screens.AllTransactions.TransactionItem
import com.example.cashia.ui.screens.AllTransactions.resolveCategory
import com.example.cashia.ui.screens.Analytics.FinancialOverviewCards
import com.example.cashia.ui.screens.Analytics.GlassySurface
import com.example.cashia.ui.theme.primaryGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: TransactionsViewModel, categoryViewModel: CategoryViewModel, upcomingViewModel: UpcomingViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val state = viewModel.state
    var selectedTransaction by remember { mutableStateOf<MpesaTransactions?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val upcomingAll by upcomingViewModel.upcoming.collectAsState()

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

    val upcomingForDay = remember(upcomingAll, selectedDate) {
        upcomingAll.filter { it.dueDate == selectedDate.toString() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
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
                categoryViewModel = categoryViewModel,
                onDismiss = { selectedTransaction = null },
                onEdit = { /* Handle Edit logic */ },
                onDelete = {
                }
            )
        }
        PullToRefreshBox(
            isRefreshing = state.isLoading, onRefresh = {
                viewModel.refreshFromSms()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FinancialOverviewCards(state.allTransactions, selectedDate) {
                        selectedDate = it
                    }
                }
                item {
                    FullMonthCalendar(state.allTransactions, selectedDate, upcomingAll) { selectedDate = it }
                }
                item {
                    TransactionFrequencyCard(state.allTransactions, selectedDate) {
                        selectedDate = it
                    }
                }
                item {
                    DayHeader(selectedDate = selectedDate)
                }
                val transactionsForDay = state.allTransactions.filter {
                    LocalDate.parse(it.date) == selectedDate
                }
                if (transactionsForDay.isNotEmpty() || upcomingForDay.isNotEmpty()) {
                    item {
                        DaySpendingBar(
                            transactions = transactionsForDay,
                            upcomingForDay = upcomingForDay
                        )
                    }
                }
                if (upcomingForDay.isNotEmpty()) {
                    item {
                        UpcomingDaySection(
                            upcoming = upcomingForDay,
                            allCategories = allCategories,
                            onMarkPaid = { upcomingViewModel.markPaid(it) }
                        )
                    }
                }

                if (transactionsForDay.isEmpty() && upcomingForDay.isEmpty()) {
                    item { EmptyStateMessage("No Transactions On This Day", hapticFeedback) }
                } else {
                    items(transactionsForDay, key = { it.id }) { tx ->
                        TransactionItem(
                            tx,
                            categoryLabel = resolveCategory(tx.category, allCategories),
                            onClick = { selectedTransaction = tx },
                            hapticFeedback
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(selectedDate: LocalDate) {
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
            Text(
                text = selectedDate
                    .format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
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
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

// ── Day spending summary bar ──────────────────────────────────────────────────

@Composable
private fun DaySpendingBar(
    transactions: List<MpesaTransactions>,
    upcomingForDay: List<UpcomingTransaction>
) {
    val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
    val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
    val upcomingExpense = upcomingForDay
        .filter { it.type == com.example.cashia.models.UpcomingType.EXPENSE }
        .sumOf { it.amount }
    val upcomingIncome = upcomingForDay
        .filter { it.type != com.example.cashia.models.UpcomingType.EXPENSE }
        .sumOf { it.amount }

    val totalInflow = income + upcomingIncome
    val totalOutflow = expense + upcomingExpense
    val total = totalInflow + totalOutflow
    val incomeRatio = if (total > 0) (totalInflow / total).toFloat() else 0.5f

    // Animate bar fill
    val animatedRatio by animateFloatAsState(
        targetValue = incomeRatio,
        animationSpec = tween(600),
        label = "bar"
    )

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DAY SUMMARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
                if (upcomingForDay.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "${upcomingForDay.size} scheduled",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Income / Expense split bar ────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRatio.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(brush = primaryGradient())
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Stat row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Income",
                    amount = income,
                    pendingAmount = upcomingIncome,
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.TrendingUp
                )
                DayStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Expenses",
                    amount = expense,
                    pendingAmount = upcomingExpense,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.TrendingDown
                )
                // Net
                val net = income - expense
                DayStatPill(
                    modifier = Modifier.weight(1f),
                    label = "Net",
                    amount = net,
                    pendingAmount = 0.0,
                    color = if (net >= 0) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.error,
                    icon = if (net >= 0) Icons.Default.TrendingUp
                    else Icons.Default.TrendingDown
                )
            }
        }
    }
}

@Composable
private fun DayStatPill(
    modifier: Modifier = Modifier,
    label: String,
    amount: Double,
    pendingAmount: Double,
    color: Color,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, null,
                    modifier = Modifier.size(12.dp),
                    tint = color.copy(alpha = 0.7f))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Text(
                com.example.cashia.ui.screens.Analytics.formatCompactAmount(amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = if (amount == 0.0) color.copy(alpha = 0.35f) else color
            )
            // Show pending upcoming amount if present
            if (pendingAmount > 0) {
                Text(
                    "+${com.example.cashia.ui.screens.Analytics.formatCompactAmount(pendingAmount)} scheduled",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ── Upcoming items on selected day ────────────────────────────────────────────

@Composable
private fun UpcomingDaySection(
    upcoming: List<UpcomingTransaction>,
    allCategories: List<com.example.cashia.models.ResolvedCategory>,
    onMarkPaid: (UpcomingTransaction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "SCHEDULED TODAY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 4.dp)
        )
        upcoming.forEach { tx ->
            val resolved = allCategories.find { it.id == tx.categoryId }
            GlassySurface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category emoji
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(resolved?.emoji ?: "📦", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            tx.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            resolved?.label ?: "Other",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            com.example.cashia.Utility.PesaTimeConfiguration.formatCurrency(tx.amount),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = if (tx.type == com.example.cashia.models.UpcomingType.INCOME)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = { onMarkPaid(tx) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                "Mark Paid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
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

    val (topIncome, topExpense, _) = frequencyData

    selectedDate.toString()
    monthlyTransactions.count {
        it.second == selectedDate && it.first.type == "income"
    }
    monthlyTransactions.count {
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
