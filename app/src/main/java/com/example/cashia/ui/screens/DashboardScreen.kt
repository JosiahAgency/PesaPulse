package com.example.cashia.ui.screens

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.Utility.SmsRepository
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.parseMpesaMessage
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val transactions = remember {
        SmsRepository.fetchMpesaMessages(context).mapNotNull { parseMpesaMessage(it) }
    }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    val transactionsForSelectedDate = transactions.filter {
        LocalDate.parse(it.date, formatter) == selectedDate
    }

    Scaffold() { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp, 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            DailySummaryCard(transactionsForSelectedDate)
            WeeklySpendingChart(transactions, selectedDate, { selectedDate = it })
            Spacer(Modifier.height(6.dp))
            DailyTransactions(transactions, selectedDate, { selectedDate = it })
        }
    }
}

@Composable
fun DailySummaryCard(transactions: List<MpesaTransactions>) {
    val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
    val expenses = transactions.filter { it.type == "expense" }.sumOf { it.amount }
    val net = income - expenses

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Daily Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(
                    "Income",
                    income,
                    MaterialTheme.colorScheme.primary,
                    Icons.Default.ArrowCircleDown
                )
                SummaryItem(
                    "Expenses",
                    expenses,
                    MaterialTheme.colorScheme.error,
                    Icons.Default.ArrowCircleUp
                )
                SummaryItem(
                    "Net",
                    net,
                    Color.Cyan,
                    Icons.Default.AccountBalance
                )
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, amount: Double, color: Color, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                "Ksh ${formatAmount(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
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
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    val last7Days = (0..6).map { today.minusDays(it.toLong()) }.reversed()

    val dailyTotals = last7Days.map { date ->
        transactions.filter { tx ->
            LocalDate.parse(tx.date, formatter) == date && tx.type == "expense"
        }.sumOf { it.amount }
    }

    val maxValue = dailyTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0

    Column {
        Text("Last 7 Days Spending", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            last7Days.forEachIndexed { index, date ->
                val value = dailyTotals[index]
                val heightRatio = value / maxValue
                val isSelected = date == selectedDate

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable() { onDaySelected(date) }
                ) {
                    Text(
                        text = if (value > 0) "-Ksh ${formatAmount(value)}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height((100 * heightRatio).dp)
                            .width(if (isSelected) 28.dp else 22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) {
                                    Color.Red
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = Color.Red,
                                shape = RoundedCornerShape(6.dp)
                            ),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = date.dayOfWeek.name.take(3).lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = selectedDate.format(
                DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalPager(
            count = days.size,
            state = pagerState,
            modifier = Modifier.fillMaxHeight()
        ) { page ->
            val date = days[page]
            val transactionsForDay = allTransactions.filter {
                LocalDate.parse(it.date, formatter) == date
            }
            if (transactionsForDay.isEmpty()) {
                Text(
                    "No Transactions On This Day",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(transactionsForDay) { tx ->
                        TransactionItem(tx)
                    }
                }

            }
        }
    }
}