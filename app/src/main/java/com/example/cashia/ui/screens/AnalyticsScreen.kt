package com.example.cashia.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashia.Utility.TimeScope
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
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
            val (start, end) = when (selectedScope) {
                TimeScope.DAY -> today to today
                TimeScope.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) to today.with(
                    TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)
                )

                TimeScope.MONTH -> today.withDayOfMonth(1) to today.with(TemporalAdjusters.lastDayOfMonth())
                else -> startDate to endDate
            }
            startDate = start
            endDate = end
        }
    }

    val startTs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endTs =
        endDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val filteredTransactions = remember(transactions, selectedType, startTs, endTs) {
        transactions.filter { tx ->
            val isDateInRange = tx.timestamp in startTs..endTs
            val isTypeMatch = tx.type == selectedType.name.lowercase()
            isDateInRange && isTypeMatch

        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refreshFromSms() },
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TransactionTypeSelector(
                                selectedType,
                                onTypeChange = { selectedType = it })
                            VerticalDivider(
                                modifier = Modifier
                                    .height(32.dp)
                                    .padding(horizontal = 4.dp)
                            )
                            AnalysisScopeSelector(
                                selectedScope,
                                onScopeChange = { selectedScope = it })
                        }
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
                    AnalyticsSummaryHeader(
                        totalAmount = filteredTransactions.sumOf { it.amount },
                        transactionCount = filteredTransactions.size,
                        type = selectedType
                    )
                }
//                item {
//                    TrendChart(transactions = filteredTransactions, type = selectedType)
//                }

                if (filteredTransactions.isNotEmpty()) {
                    item {
                        QuickInsightCard(filteredTransactions)
                    }
                }

                item {
                    val breakdownTitle = if (selectedType == TransactionType.INCOME)
                        "Top Income Sources" else "Top Expenditure"

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
fun AnalyticsSummaryHeader(totalAmount: Double, transactionCount: Int, type: TransactionType) {
    val color =
        if (type == TransactionType.INCOME) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total ${type.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
                Text(
                    text = "Ksh ${formatAmount(totalAmount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.2f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = transactionCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun QuickInsightCard(transactions: List<MpesaTransactions>) {
    val largest = transactions.maxByOrNull { it.amount } ?: return
    val isIncome = largest.type == "income"
    val label = if (isIncome) "Largest amount received" else "Largest single spend"
    val iconColor =
        if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Default.ArrowCircleDown else Icons.Default.ArrowCircleUp,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.padding(5.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ksh ${formatAmount(largest.amount)} to ${largest.recipient}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                val percentageRaw = if (totalAmount > 0) (entry.value / totalAmount) else 0.0
                val percentageDisplay = (percentageRaw * 100).roundToInt()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {}
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

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = if (type == TransactionType.INCOME)
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    ), contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

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
                                    text = "$percentageDisplay% of Total",
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
                        progress = percentageRaw.toFloat(),
                        color = if (type == TransactionType.INCOME)
                            MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
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

@Composable
fun TrendChart(
    transactions: List<MpesaTransactions>,
    type: TransactionType,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    // Use slightly transparent base colors so the selected one pops
    val baseColor = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val unselectedColor = baseColor.copy(alpha = 0.5f)

    var selectedKey by remember { mutableStateOf<String?>(null) }

    // --- Data Preparation (Same as before) ---
    val groupedData = remember(transactions) {
        val daily = transactions.groupBy { it.date }
            .mapValues { it.value.sumOf { tx -> tx.amount } }

        if (daily.size > 31) {
            transactions.groupBy { it.date.substring(0, 7) }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toSortedMap()
        } else {
            daily.toSortedMap()
        }
    }

    val keysList = remember(groupedData) { groupedData.keys.toList() }
    val maxAmount = remember(groupedData) { (groupedData.values.maxOrNull() ?: 1.0).toFloat() }
    val yAxisLabels = listOf(maxAmount, maxAmount / 2, 0f)

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Financial Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Tap a bar to see details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp)) // More space for tooltip at top

            Row(modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()) {
                // --- Y-AXIS LABELS ---
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    yAxisLabels.forEach {
                        Text(formatCompactAmount(it.toDouble()), style = MaterialTheme.typography.labelSmall)
                    }
                }

                // --- MAIN CHART AREA ---
                // We use BoxWithConstraints to define the boundaries for alignment
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp)
                ) {
                    // Layer 1: Grid Lines (Background)
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        repeat(3) { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
                    }

                    // Layer 2: The Bars (Middle)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        groupedData.forEach { (key, amount) ->
                            val isSelected = selectedKey == key
                            // Reserve top 20% space so tooltip doesn't overlap top grid line too much
                            val barHeightRatio = ((amount.toFloat() / maxAmount) * 0.8f).coerceAtLeast(0.05f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(barHeightRatio)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (isSelected) baseColor else unselectedColor)
                                    .clickable {
                                        if (selectedKey != key) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedKey = key
                                        } else {
                                            selectedKey = null
                                        }
                                    }
                            )
                        }
                    }

                    // Layer 3: The Tooltip Overlay (Top)
                    // This sits on top of everything and is not constrained by bar width.
                    selectedKey?.let { key ->
                        val selectedIndex = keysList.indexOf(key)
                        val amount = groupedData[key] ?: 0.0

                        // Calculate horizontal position (-1 is start, 0 is center, 1 is end)
                        val totalItems = keysList.size
                        val horizontalBias = if (totalItems > 1) {
                            (selectedIndex.toFloat() / (totalItems - 1)) * 2f - 1f
                        } else 0f

                        Surface(
                            color = MaterialTheme.colorScheme.inverseSurface, // Darker background for contrast
                            shape = RoundedCornerShape(4.dp),
                            shadowElevation = 4.dp,
                            // Align horizontally based on index, vertically at the top (-1f)
                            modifier = Modifier
                                .align(BiasAlignment(horizontalBias, -1f))
                                .zIndex(1f) // Ensure it's on top
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Small arrow pointing down (optional aesthetic)
                                // Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.inverseSurface).rotate(45f))

                                Text(
                                    text = "Ksh ${formatCompactAmount(amount)}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            // --- X-AXIS LABELS (Bottom) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (keysList.isNotEmpty()) {
                    Text(formatXLabel(keysList.first()), style = MaterialTheme.typography.labelSmall)
                    if (keysList.size > 2) {
                        Text(formatXLabel(keysList[keysList.size / 2]), style = MaterialTheme.typography.labelSmall)
                    }
                    Text(formatXLabel(keysList.last()), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

fun formatCompactAmount(amount: Double): String {
    return when {
        amount >= 1_000_000 -> "${(amount / 1_000_000).toInt()}M"
        amount >= 1_000 -> "${(amount / 1_000).toInt()}k"
        else -> amount.toInt().toString()
    }
}

fun formatXLabel(key: String): String {
    return if (key.length > 7) {
        val date = LocalDate.parse(key)
        date.format(DateTimeFormatter.ofPattern("dd MMM"))
    } else {
        val yearMonth = java.time.YearMonth.parse(key)
        yearMonth.format(DateTimeFormatter.ofPattern("MMM yy"))
    }
}
