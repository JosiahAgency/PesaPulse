package com.example.cashia.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var startDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(selectedScope) {
        if (selectedScope != TimeScope.CUSTOM) {
            val (start, end) = when (selectedScope) {
                TimeScope.DAY -> today to today
                TimeScope.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) to
                        today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

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

    val allFilteredTransactions = remember(transactions, startTs, endTs) {
        transactions.filter { tx -> tx.timestamp in startTs..endTs }
    }
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f
    val glassColor = colorScheme.surface.copy(alpha = glassAlpha)
    val glassBorder = colorScheme.onSurface.copy(alpha = 0.12f)

    Scaffold(
        containerColor = Color.Transparent,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refreshFromSms() },
            modifier = Modifier
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = glassColor,
                        border = BorderStroke(
                            0.6.dp, glassBorder
                        ),
                        shadowElevation = 10.dp,
                    ) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            GlassyPeriodSelector(
                                selectedScope = selectedScope,
                                onScopeChange = { selectedScope = it },
                                startDate = startDate,
                                endDate = endDate,
                                onStartDateSelected = {
                                    startDate = it
                                    selectedScope = TimeScope.CUSTOM
                                },
                                onEndDateSelected = {
                                    endDate = it
                                    selectedScope = TimeScope.CUSTOM
                                }
                            )
                            ModernTypeSelector(
                                selectedType = selectedType,
                                onTypeChange = { selectedType = it }
                            )
                        }
                    }
                }
                item {
                    FinancialOverviewCards(allFilteredTransactions)
                }
                item {
                    GlassyAmountHeader(
                        totalAmount = filteredTransactions.sumOf { it.amount },
                        transactionCount = filteredTransactions.size,
                        type = selectedType,
                        previousPeriodAmount = calculatePreviousPeriod(
                            transactions,
                            startDate,
                            endDate,
                            selectedType
                        )
                    )
                }
                if (filteredTransactions.isNotEmpty()) {
                    item {
                        GlassyTrendChart(
                            transactions = filteredTransactions,
                            type = selectedType
                        )
                    }
                }
                if (filteredTransactions.isNotEmpty()) {
                    item {
                        QuickInsightsRow(filteredTransactions, selectedType)
                    }
                }
                item {
                    TopBreakdownSection(
                        filteredTransactions = filteredTransactions,
                        selectedType = selectedType
                    )
                }
                if (filteredTransactions.isNotEmpty()) {
                    item {
                        TransactionFrequencyCard(filteredTransactions, selectedType)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassyPeriodSelector(
    selectedScope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModernSegmentedControl2(
            selectedScope = selectedScope,
            onScopeChange = onScopeChange
        )
        GlassySurface {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateColumn(
                    label = "From",
                    date = startDate,
                    onClick = {
                        showDatePicker(
                            context,
                            startDate,
                            endDate,
                            true,
                            onStartDateSelected
                        )
                    }
                )

                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                DateColumn(
                    label = "To",
                    date = endDate,
                    onClick = {
                        showDatePicker(
                            context,
                            startDate,
                            endDate,
                            false,
                            onEndDateSelected
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun ModernSegmentedControl2(
    selectedScope: TimeScope,
    onScopeChange: (TimeScope) -> Unit
) {
    val items = remember { TimeScope.entries.filter { it != TimeScope.WEEK } }
    val selectedIndex = items.indexOf(selectedScope)

    val animateIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillSelection"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val itemWidth = maxWidth / items.size

        // The Gradient Sliding Pill
        Box(
            modifier = Modifier
                .offset(x = itemWidth * animateIndex)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, scope ->
                val isSelected = index == selectedIndex

                val textColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onScopeChange(scope) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scope.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium.copy(
                            shadow = if (isSelected) Shadow(
                                color = Color.Black.copy(alpha = 0.2f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            ) else Shadow.None
                        ),
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ModernTypeSelector(
    selectedType: TransactionType,
    onTypeChange: (TransactionType) -> Unit
) {
    val items = remember { TransactionType.entries }
    val selectedIndex = items.indexOf(selectedType)

    val animateIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "typePillSelection"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val itemWidth = maxWidth / items.size
        val currentType = items[selectedIndex.toInt().coerceIn(items.indices)]
        val pillColor = when (currentType) {
            TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
            TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        }

        Box(
            modifier = Modifier
                .offset(x = itemWidth * animateIndex)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            pillColor.copy(alpha = 0.8f),
                            pillColor
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, type ->
                val isSelected = index == selectedIndex
                val typeColor = when (type) {
                    TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
                    TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
                }

                val textColor by animateColorAsState(
                    targetValue = if (isSelected)
                        Color.White
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "typeTextColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTypeChange(type) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = if (type == TransactionType.INCOME)
                                    Icons.Default.TrendingUp
                                else
                                    Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium.copy(
                                shadow = if (isSelected) Shadow(
                                    color = Color.Black.copy(alpha = 0.3f),
                                    offset = Offset(0f, 2f),
                                    blurRadius = 4f
                                ) else Shadow.None
                            ),
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateColumn(label: String, date: LocalDate, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun FinancialOverviewCards(transactions: List<MpesaTransactions>) {
    val income = transactions.filter { it.type == "income" }.sumOf { it.amount }
    val expense = transactions.filter { it.type == "expense" }.sumOf { it.amount }
    val netFlow = income - expense

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Income Card
        SmallMetricCard(
            modifier = Modifier.weight(1f),
            title = "Income",
            amount = income,
            icon = Icons.Default.TrendingUp,
            color = MaterialTheme.colorScheme.tertiary
        )

        // Expense Card
        SmallMetricCard(
            modifier = Modifier.weight(1f),
            title = "Expenses",
            amount = expense,
            icon = Icons.Default.TrendingDown,
            color = MaterialTheme.colorScheme.error
        )

        // Net Flow Card
        SmallMetricCard(
            modifier = Modifier.weight(1f),
            title = "Net",
            amount = netFlow,
            icon = if (netFlow >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            color = if (netFlow >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun SmallMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color
) {
    GlassySurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatAmount(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassyAmountHeader(
    totalAmount: Double,
    transactionCount: Int,
    type: TransactionType,
    previousPeriodAmount: Double
) {
    val color = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val percentageChange = if (previousPeriodAmount > 0) {
        ((totalAmount - previousPeriodAmount) / previousPeriodAmount * 100)
    } else 0.0

    GlassySurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total ${
                            type.name.lowercase().replaceFirstChar { it.uppercase() }
                        }",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Ksh ${formatAmount(totalAmount)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    )

                    if (percentageChange != 0.0) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (percentageChange > 0)
                                    Icons.Default.TrendingUp
                                else
                                    Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (percentageChange > 0) color else color.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${if (percentageChange > 0) "+" else ""}${percentageChange.roundToInt()}% vs last period",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Transaction count badge
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = transactionCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = "txns",
                            style = MaterialTheme.typography.labelSmall,
                            color = color.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassyTrendChart(
    transactions: List<MpesaTransactions>,
    type: TransactionType
) {
    val haptic = LocalHapticFeedback.current
    val baseColor = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    var selectedKey by remember { mutableStateOf<String?>(null) }

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

    GlassySurface {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Trend Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tap bars for details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = baseColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            ) {
                // Y-axis
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    yAxisLabels.forEach {
                        Text(
                            formatCompactAmount(it.toDouble()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chart area
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Grid lines
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(3) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }

                    // Bars
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        groupedData.forEach { (key, amount) ->
                            val isSelected = selectedKey == key
                            val barHeightRatio =
                                ((amount.toFloat() / maxAmount) * 0.8f).coerceAtLeast(0.05f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(barHeightRatio)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isSelected)
                                            baseColor
                                        else
                                            baseColor.copy(alpha = 0.4f)
                                    )
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

                    // Tooltip
                    selectedKey?.let { key ->
                        val selectedIndex = keysList.indexOf(key)
                        val amount = groupedData[key] ?: 0.0
                        val totalItems = keysList.size
                        val horizontalBias = if (totalItems > 1) {
                            (selectedIndex.toFloat() / (totalItems - 1)) * 2f - 1f
                        } else 0f

                        Surface(
                            color = baseColor,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(BiasAlignment(horizontalBias, -1f))
                                .zIndex(1f)
                        ) {
                            Text(
                                text = "Ksh ${formatCompactAmount(amount)}",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // X-axis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (keysList.isNotEmpty()) {
                    Text(
                        formatXLabel(keysList.first()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (keysList.size > 2) {
                        Text(
                            formatXLabel(keysList[keysList.size / 2]),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatXLabel(keysList.last()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuickInsightsRow(transactions: List<MpesaTransactions>, type: TransactionType) {
    val largest = transactions.maxByOrNull { it.amount }
    val average =
        if (transactions.isNotEmpty()) transactions.sumOf { it.amount } / transactions.size else 0.0

    val color = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Quick Insights",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Largest
            largest?.let {
                InsightCard(
                    modifier = Modifier.weight(1f),
                    title = "Largest",
                    amount = it.amount,
                    subtitle = it.recipient,
                    color = color
                )
            }

            // Average
            InsightCard(
                modifier = Modifier.weight(1f),
                title = "Average",
                amount = average,
                subtitle = "per transaction",
                color = color
            )
        }
    }
}

@Composable
fun InsightCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    subtitle: String,
    color: Color
) {
    GlassySurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ksh ${formatAmount(amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TopBreakdownSection(
    filteredTransactions: List<MpesaTransactions>,
    selectedType: TransactionType
) {
    var topNCount by remember { mutableIntStateOf(5) }
    val breakdownData = remember(filteredTransactions, topNCount) {
        filteredTransactions
            .groupBy { it.recipient }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(topNCount)
    }

    val totalAmountForType = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.amount }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = if (selectedType == TransactionType.INCOME) "TOP SOURCES" else "TOP RECIPIENTS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Contribution to total ${selectedType.name.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // High-fidelity count switcher
            ItemCountSelector(
                currentCount = topNCount,
                onCountChange = { topNCount = it }
            )
        }

        // --- Content Area ---
        if (filteredTransactions.isEmpty()) {
            GlassySurface(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No data for this period", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            GlassySurface {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    breakdownData.forEach { entry ->
                        RecipientProgressRow(
                            name = entry.key,
                            amount = entry.value,
                            total = totalAmountForType,
                            type = selectedType
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipientProgressRow(
    name: String,
    amount: Double,
    total: Double,
    type: TransactionType
) {
    val progress = if (total > 0) (amount / total).toFloat() else 0f
    val color = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Ksh ${formatAmount(amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
        }

        // Custom Progress Bar with Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }
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

    val totalAmount = remember(breakdown) { breakdown.sumOf { it.value } }
    val topBreakdown = breakdown.take(n)

    GlassySurface {
        Column(Modifier.padding(vertical = 8.dp)) {
            topBreakdown.forEachIndexed { index, entry ->
                val percentageRaw = if (totalAmount > 0) (entry.value / totalAmount) else 0.0
                val percentageDisplay = (percentageRaw * 100).roundToInt()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rank badge
                            Surface(
                                shape = CircleShape,
                                color = amountColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        (index + 1).toString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = amountColor
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
                                    text = "$percentageDisplay% of total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = formatAmount(entry.value),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = amountColor
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    AnimatedProgressBar(
                        progress = percentageRaw.toFloat(),
                        color = amountColor
                    )
                }

                if (index < topBreakdown.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionFrequencyCard(transactions: List<MpesaTransactions>, type: TransactionType) {
    val color = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    val byDay = transactions.groupBy {
        LocalDate.parse(it.date).dayOfWeek.name.lowercase()
            .replaceFirstChar { char -> char.uppercase() }
    }

    val mostFrequentDay = byDay.maxByOrNull { it.value.size }?.key ?: "N/A"
    val mostFrequentCount = byDay.maxByOrNull { it.value.size }?.value?.size ?: 0

    GlassySurface {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Most Active Day",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    mostFrequentDay,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    "$mostFrequentCount transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = color.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

// Helper Composables
@Composable
fun GlassySurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.95f else 0.8f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.5.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
        shadowElevation = 2.dp
    ) {
        content()
    }
}

@Composable
fun ItemCountSelector(
    currentCount: Int,
    onCountChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(3, 5, 10, 15, 20)

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            onClick = { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Top $currentCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { number ->
                DropdownMenuItem(
                    text = { Text("Top $number") },
                    onClick = {
                        onCountChange(number)
                        expanded = false
                    },
                    trailingIcon = {
                        if (number == currentCount) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary
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
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
        )
    }

    LinearProgressIndicator(
        progress = { animatedProgress.value },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = color,
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        strokeCap = StrokeCap.Round
    )
}

@Composable
fun EmptyAnalyticsState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper Functions
fun calculatePreviousPeriod(
    transactions: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate,
    type: TransactionType
): Double {
    val periodLength = java.time.Period.between(startDate, endDate).days + 1
    val previousStart = startDate.minusDays(periodLength.toLong())
    val previousEnd = startDate.minusDays(1)

    val previousStartTs =
        previousStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val previousEndTs =
        previousEnd.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
            .toEpochMilli()

    return transactions.filter { tx ->
        tx.timestamp in previousStartTs..previousEndTs && tx.type == type.name.lowercase()
    }.sumOf { it.amount }
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

fun showDatePicker(
    context: android.content.Context,
    startDate: LocalDate,
    endDate: LocalDate,
    isStart: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val initialDate = if (isStart) startDate else endDate
    val calendar = Calendar.getInstance().apply {
        set(initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth)
    }

    val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
        val newDate = LocalDate.of(year, month + 1, day)

        if (isStart && newDate.isAfter(endDate)) {
            onDateSelected(endDate)
        } else if (!isStart && newDate.isBefore(startDate)) {
            onDateSelected(startDate)
        } else {
            onDateSelected(newDate)
        }
    }

    DatePickerDialog(
        context,
        dateSetListener,
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}