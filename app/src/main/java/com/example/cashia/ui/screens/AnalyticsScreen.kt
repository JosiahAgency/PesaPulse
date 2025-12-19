package com.example.cashia.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashia.Utility.TimeScope
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )

    val state = viewModel.state
    val transactions = viewModel.state.allTransactions

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedScope by remember { mutableStateOf(TimeScope.MONTH) }

    val today = LocalDate.now()

    var startDate by remember { mutableStateOf(LocalDate.now().minusMonths(1).withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var topNCount by remember { mutableIntStateOf(5) }

    LaunchedEffect(selectedScope) {
        if (selectedScope != TimeScope.CUSTOM) {
            val start = when (selectedScope) {
                TimeScope.DAY -> today
                TimeScope.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                TimeScope.MONTH -> today.withDayOfMonth(1)
                else -> today // Should not happen but handles exhaustive check
            }
            val end = when (selectedScope) {
                TimeScope.DAY -> today
                TimeScope.WEEK -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                TimeScope.MONTH -> today.with(TemporalAdjusters.lastDayOfMonth())
                else -> today // Should not happen but handles exhaustive check
            }
            startDate = start
            endDate = end
        }
    }

    val filteredTransactions = remember(transactions, selectedType, startDate, endDate) {
        transactions.filter { tx ->
            val date = LocalDate.parse(tx.date, DateTimeFormatter.ISO_LOCAL_DATE)
            val isDateInRange = !date.isBefore(startDate) && !date.isAfter(endDate)
            val isTypeMatch = when (selectedType) {
                TransactionType.INCOME -> tx.type == "income"
                TransactionType.EXPENSE -> tx.type == "expense"
            }
            isDateInRange && isTypeMatch
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.fetchTransactions() },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        AnalysisScopeSelector(selectedScope, onScopeChange = { selectedScope = it })
                        TransactionTypeSelector(selectedType, onTypeChange = { selectedType = it })
                        AdjustableDateRangeDisplay(
                            startDate = startDate,
                            endDate = endDate,
                            onStartDateSelected = { newDate ->
                                startDate = newDate
                                selectedScope = TimeScope.CUSTOM
                            },
                            onEndDateSelected = { newDate ->
                                endDate = newDate
                                selectedScope = TimeScope.CUSTOM
                            }
                        )
                    }
                }
                item {
                    val breakdownTitle = if (selectedType == TransactionType.INCOME)
                        "Top Income Sources" else "Top Expenditure Categories"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                breakdownTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Relative to total ${selectedType.name.lowercase()} in Period",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        ItemCountSelector(
                            currentCount = topNCount,
                            onCountChange = { topNCount = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (filteredTransactions.isEmpty()) {
                        EmptyAnalyticsState("No transactions to break down")
                    } else {
                        TopBreakdownList(filteredTransactions, selectedType, n = topNCount)
                    }
                }
            }
        }
    }
}


@Composable
fun EmptyAnalyticsState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TopBreakdownList(transactions: List<MpesaTransactions>, type: TransactionType, n: Int) {
    val amountColor = when (type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
    }

    val breakdown = remember(transactions) {
        transactions.groupBy { it.recipient }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }
    val totalAmount = remember(breakdown) {
        breakdown.sumOf { it.value }
    }
    val topBreakdown = breakdown.take(n)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            if (topBreakdown.isEmpty()) {
                EmptyAnalyticsState("No data to display in this breakdown.")
                return@Card
            }

            topBreakdown.forEachIndexed { index, entry ->
                val percentage = (entry.value / totalAmount).toFloat()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = entry.key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${(percentage * 100).roundToInt()}% of total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "Ksh ${formatAmount(entry.value)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    AnimatedProgressBar(
                        progress = percentage,
                        color = amountColor
                    )
                }
                if (index < breakdown.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun ItemCountSelector(
    currentCount: Int,
    onCountChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(3, 5, 10, 20, 40, 60, 100)

    Box(
        modifier = Modifier.wrapContentSize(Alignment.TopEnd)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Show: $currentCount",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { number ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "$number items",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onCountChange(number)
                        expanded = false
                    },
                    trailingIcon = {
                        if (number == currentCount) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AnimatedProgressBar(progress: Float, color: Color) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        )
    }

    LinearProgressIndicator(
        progress = { animatedProgress.value },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round // Gives the bar rounded ends
    )
}

@Composable
fun AnalysisScopeSelector(selectedScope: TimeScope, onScopeChange: (TimeScope) -> Unit) {
    val selectedColor = MaterialTheme.colorScheme.primary
    Column {
        Text("Select Period", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeScope.entries.forEach { scope ->
                val isSelected = selectedScope == scope
                FilterChip(
                    selected = isSelected,
                    onClick = { onScopeChange(scope) },
                    label = {
                        Text(
                            scope.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            null,
                            Modifier.size(18.dp),
                            tint = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = selectedColor.copy(alpha = 0.1f),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        }
    }
}

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit
) {
    Column {
        Text("Transaction Type", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionType.entries.forEach { type ->
                val typeColor = when (type) {
                    TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                }
                val isSelected = selectedType == type

                FilterChip(
                    selected = isSelected,
                    onClick = { onTypeChange(type) },
                    label = {
                        Text(
                            type.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingIcon = {
                        if (isSelected) Icon(
                            Icons.Default.Category,
                            null,
                            Modifier.size(16.dp),
                            tint = typeColor
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = typeColor.copy(alpha = 0.1f),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        }
    }
}

@Composable
fun AdjustableDateRangeDisplay(
    startDate: LocalDate, endDate: LocalDate, onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun showDatePicker(
        initialDate: LocalDate,
        onDateSelected: (LocalDate) -> Unit,
        isStart: Boolean
    ) {
        val calendar = Calendar.getInstance().apply {
            set(initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth)
        }

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _: DatePicker, year: Int, month: Int, day: Int ->
                val newDate = LocalDate.of(year, month + 1, day)

                if (isStart && newDate.isAfter(endDate)) {
                    onDateSelected(endDate) // If start date is after end date, set to end date
                } else if (!isStart && newDate.isBefore(startDate)) {
                    onDateSelected(startDate) // If end date is before start date, set to start date
                } else {
                    onDateSelected(newDate)
                }
            }

        val dialog = DatePickerDialog(
            context,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .clickable { showDatePicker(startDate, onStartDateSelected, true) },
            ) {
                Text(
                    "Start Date:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    startDate.format(formatter),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(1.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .clickable { showDatePicker(endDate, onEndDateSelected, false) },
            ) {
                Text(
                    "End Date:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    endDate.format(formatter),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
