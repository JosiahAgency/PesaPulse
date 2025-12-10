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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun DashboardScreen() {
    val context = LocalContext.current

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()

    val viewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    val transactions = viewModel.state.allTransactions
    var selectedScope by remember { mutableStateOf(TimeScope.DAY) }

    val summaryData by remember(selectedDate, selectedScope, transactions) {
        derivedStateOf {
            viewModel.calculateSummary(selectedDate, selectedScope)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedDate != today) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedDate = today
                    }, icon = {
                        Icon(Icons.Default.Today, contentDescription = "Today")

                    }, text = {
                        Text("Today", fontWeight = FontWeight.SemiBold)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TimeScopedSummaryCard(selectedScope, { selectedScope = it }, summaryData)
            }
            item {
                WeeklySpendingChart(transactions, selectedDate, { selectedDate = it })
            }
            item {
                DailyTransactions(transactions, selectedDate, { selectedDate = it })
            }
        }
    }
}

@Composable
fun TimeScopedSummaryCard(
    scope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    summaryData: SummaryData
) {
    val income = summaryData.income
    val expenses = summaryData.expenses
    val startDate = summaryData.startDate
    val endDate = summaryData.endDate

    val monthDayFormatter = DateTimeFormatter.ofPattern("dd MMM")
    val fullDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    val dateDisplay = when (scope) {
        TimeScope.DAY -> {
            "Summary for ${startDate.format(fullDateFormatter)}"
        }

        TimeScope.WEEK -> {
            val start = startDate.format(monthDayFormatter)
            val end = endDate.format(fullDateFormatter)
            "Weekly Summary: $start - $end"
        }

        TimeScope.MONTH -> {
            val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
            "Monthly Summary: ${startDate.format(monthYearFormatter)}"
        }

        TimeScope.CUSTOM -> {
            "Summary for ${startDate.format(fullDateFormatter)}"
        }
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
                TimeScope.entries.forEach { currentScope ->
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
                            .padding(vertical = 8.dp)
                    )
                }
            }

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

@Composable
fun WeeklySpendingChart(
    transactions: List<MpesaTransactions>,
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit
) {
    FullMonthCalendar(transactions, selectedDate, onDaySelected)
}

@Composable
fun DailyTransactions(
    allTransactions: List<MpesaTransactions>,
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val today = LocalDate.now()

    val days = (0 until 30)
        .map { today.minusDays(it.toLong()) }.reversed()

    val selectedPageIndex = days.indexOf(selectedDate).coerceIn(0, days.lastIndex)

    val pagerState = rememberPagerState(initialPage = selectedPageIndex)

    LaunchedEffect(selectedDate) {
        val page = days.indexOf(selectedDate)
        if (page != pagerState.currentPage && page != -1) {
            pagerState.animateScrollToPage(page)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = selectedDate.format(
                DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
            ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        val transactionsForDay = allTransactions.filter {
            LocalDate.parse(it.date, formatter) == selectedDate
        }
        if (transactionsForDay.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No Transactions On This Day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(transactionsForDay) { tx ->
                    TransactionItem(tx)
                }
            }

        }
    }
}