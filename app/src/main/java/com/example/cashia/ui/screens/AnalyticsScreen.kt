package com.example.cashia.ui.screens

import android.app.DatePickerDialog
import android.graphics.Paint.Align
import android.widget.DatePicker
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.collections.mapOf
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    val transactions = viewModel.state.allTransactions

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedScope by remember { mutableStateOf(TimeScope.MONTH) }

    val today = LocalDate.now()

    var startDate by remember { mutableStateOf(LocalDate.now().minusMonths(1).withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }

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
                TransactionType.ALL -> true
                TransactionType.INCOME -> tx.type == "income"
                TransactionType.EXPENSE -> tx.type == "expense"
            }
            isDateInRange && isTypeMatch
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
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
                Text(
                    "Trends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredTransactions.isEmpty()) {
                    EmptyAnalyticsState("No data for this period")
                } else {
                    SimpleBarChart(filteredTransactions, selectedType)
                }
            }
            item {
                Text(
                    "Top Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredTransactions.isEmpty()) {
                    EmptyAnalyticsState("No transactions to break down")
                } else {
                    TopBreakdownList(filteredTransactions, selectedType)
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(transactions: List<MpesaTransactions>, type: TransactionType) {
    val primaryBarColor = when (type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.ALL -> MaterialTheme.colorScheme.primary
    }
    val secondaryBarColor = MaterialTheme.colorScheme.error

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val groupedData = remember(transactions, type) {
        val dailyGroups = transactions.groupBy {
            LocalDate.parse(
                it.date,
                DateTimeFormatter.ISO_LOCAL_DATE
            )
        }

        dailyGroups.mapValues { (_, txList) ->
            if (type == TransactionType.ALL) {
                mapOf(
                    "income" to txList.filter { it.type == "income" }.sumOf { it.amount },
                    "expense" to txList.filter { it.type == "expense" }.sumOf { it.amount }
                )
            } else {
                mapOf(type.name.lowercase() to txList.sumOf { it.amount })
            }
        }.toSortedMap()
    }

    val maxValue = remember(groupedData) {
        groupedData.values.flatMap { it.values }.maxOrNull() ?: 1.0
    }
    val density = LocalDensity.current
    val textPaint = remember(density, labelColor) {
        android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = density.run { 10.sp.toPx() }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Total: Ksh ${formatAmount(transactions.sumOf { it.amount })}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val xAxisHeight = 30.dp.toPx()
                    val yAxisWidth = 50.dp.toPx()
                    val chartWidth = size.width - yAxisWidth
                    val chartHeight = size.height - xAxisHeight
                    val barCountPerGroup = if (type == TransactionType.ALL) 2 else 1
                    val totalGroups = groupedData.size

                    // Total available space for one day (including bars and internal space)
                    val groupWidth = chartWidth / totalGroups.coerceAtLeast(1)

                    // Dynamic bar width
                    val barWidth = (groupWidth / (barCountPerGroup + 1f)).coerceAtMost(40.dp.toPx())
                    val innerSpace =
                        (groupWidth - (barWidth * barCountPerGroup)) / (barCountPerGroup + 1f)
                    val ySteps = 3
                    drawIntoCanvas { canvas ->
                        textPaint.textAlign = Align.RIGHT
                        for (i in 0 until ySteps) {
                            val ratio = i.toFloat() / (ySteps - 1)
                            val yPos = chartHeight * (1 - ratio)
                            val labelValue = (maxValue * ratio)

                            drawLine(
                                color = gridColor,
                                start = Offset(yAxisWidth, yPos),
                                end = Offset(size.width, yPos),
                                strokeWidth = 1.dp.toPx()
                            )

                            val labelText =
                                if (i == 0) "0" else "KES ${abbreviateNumber(labelValue)}"

                            canvas.nativeCanvas.drawText(
                                labelText,
                                yAxisWidth - 10.dp.toPx(),
                                yPos + 10f,
                                textPaint
                            )
                        }
                    }

                    var xOffset = yAxisWidth + innerSpace
                    val skipLabel = if (totalGroups > 7) 2 else 1
                    var index = 0

                    drawIntoCanvas { canvas ->
                        textPaint.textAlign = Align.CENTER
                        groupedData.forEach { (date, amount) ->
                            if (type == TransactionType.ALL) {
                                val expenseAmount = amount["expense"] ?: 0.0
                                val expenseBarHeight =
                                    (expenseAmount / maxValue * chartHeight).toFloat()
                                drawRect(
                                    color = secondaryBarColor,
                                    topLeft = Offset(xOffset, chartHeight - expenseBarHeight),
                                    size = Size(barWidth, expenseBarHeight)
                                )
                                xOffset += barWidth
                            } else {
                                val amount = amount.values.firstOrNull() ?: 0.0
                                val barHeight = (amount / maxValue * chartHeight).toFloat()

                                drawRect(
                                    color = primaryBarColor,
                                    topLeft = Offset(xOffset, chartHeight - barHeight),
                                    size = Size(barWidth, barHeight)
                                )
                                xOffset += barWidth
                            }

                            if (index % skipLabel == 0) {
                                val dateLabel = date.format(DateTimeFormatter.ofPattern("dd/MM"))

                                val labelX = xOffset - (groupWidth / 2)
                                canvas.nativeCanvas.drawText(
                                    dateLabel,
                                    labelX,
                                    chartHeight + 20.dp.toPx(),
                                    textPaint
                                )
                            }

                            xOffset += innerSpace
                            index++
                        }
                    }
                }
            }
            if (type == TransactionType.ALL) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem(color = secondaryBarColor, label = "Expenses")
                    LegendItem(color = primaryBarColor, label = "Income")
                }
            }
        }
    }
}

fun abbreviateNumber(value: Double): String {
    val suffixes = arrayOf(" ", "k", "M", "B", "T")
    var v = value
    var i = 0
    while (v >= 1000 && i < suffixes.size - 1) {
        v /= 1000.0
        i++
    }
    return if (v % 1.0 != 0.0) {
        String.format("%.1f%s", v, suffixes[i])
    } else {
        String.format("%.0f%s", v, suffixes[i])
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
fun TopBreakdownList(transactions: List<MpesaTransactions>, type: TransactionType) {
    val amountColor = when (type) {
        TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.ALL -> MaterialTheme.colorScheme.primary
    }

    val breakdown = remember(transactions) {
        transactions.groupBy { it.recipient }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
    }
    val totalAmount = remember(breakdown) {
        breakdown.sumOf { it.value }
    }
    val topBreakdown = breakdown.take(5)

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
    Column {
        Text("Analyze Period", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeScope.entries.forEach { scope ->
                FilterChip(
                    selected = selectedScope == scope,
                    onClick = { onScopeChange(scope) },
                    label = {
                        Text(
                            scope.name.lowercase().replaceFirstChar { it.uppercase() })
                    },
                    leadingIcon = { Icon(Icons.Default.DateRange, null, Modifier.size(18.dp)) }
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
                    TransactionType.ALL -> MaterialTheme.colorScheme.primary
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
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showDatePicker(startDate, onStartDateSelected, true) },
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "Start Date:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }

            Text(
                startDate.format(formatter),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = " - ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable { showDatePicker(endDate, onEndDateSelected, false) },
            horizontalAlignment = Alignment.End
        ) {
            Text(
                "End Date:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                endDate.format(formatter),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
