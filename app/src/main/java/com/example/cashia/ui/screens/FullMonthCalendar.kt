package com.example.cashia.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.ui.theme.primaryGradient
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

@Composable
fun FullMonthCalendar(
    allTransactions: List<MpesaTransactions>,
    selectedDate: LocalDate,
    upcomingTransactions: List<UpcomingTransaction> = emptyList(),
    onDateSelected: (LocalDate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val initialPage = 500
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { 1000 }
    )
    val currentMonth = remember(pagerState.currentPage) {
        YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())
    }
    val firstDay = PesaTimeConfiguration.firstDayOfWeek
    val dayLabels = remember(firstDay) {
        (0..6).map { firstDay.plus(it.toLong()) }
            .map { it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() } }
    }

    LaunchedEffect(pagerState.currentPage) {
        androidx.compose.runtime.snapshotFlow { pagerState.currentPage }.collect { page ->
            val newMonth = YearMonth.now().plusMonths((page - initialPage).toLong())
            if (selectedDate.year != newMonth.year || selectedDate.month != newMonth.month) {
                val targetDate = if (newMonth == YearMonth.now()) {
                    LocalDate.now()
                } else {
                    newMonth.atDay(1)
                }
                onDateSelected(targetDate)
            }
        }
    }
    LaunchedEffect(selectedDate) {
        val targetMonth = YearMonth.from(selectedDate)
        val currentPagerMonth =
            YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())

        if (targetMonth != currentPagerMonth) {
            val targetPage = initialPage +
                    ((targetMonth.year - YearMonth.now().year) * 12) +
                    (targetMonth.monthValue - YearMonth.now().monthValue)
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f

    val monthTitle = currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } +
            " " + currentMonth.year

    val incomeDates = remember(allTransactions) {
        allTransactions.filter { it.type == "income" }.map { it.date }.toSet()
    }
    val expenseDates = remember(allTransactions) {
        allTransactions.filter { it.type == "expense" }.map { it.date }.toSet()
    }
    val upcomingDates = remember(upcomingTransactions) {
        upcomingTransactions.map { it.dueDate }.toSet()
    }
    val overdueUpcomingDates = remember(upcomingTransactions) {
        val today = LocalDate.now().toString()
        upcomingTransactions.filter { it.dueDate < today }.map { it.dueDate }.toSet()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f)),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                }) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        "Prev",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val upcomingThisMonth = remember(upcomingTransactions, currentMonth) {
                        upcomingTransactions.count {
                            runCatching {
                                val d = LocalDate.parse(it.dueDate)
                                d.year == currentMonth.year && d.month == currentMonth.month
                            }.getOrDefault(false)
                        }
                    }
                    if (upcomingThisMonth > 0) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "$upcomingThisMonth scheduled",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IconButton(onClick = {
                    coroutineScope.launch {
                        if (pagerState.currentPage < pagerState.pageCount - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }) {
                    Icon(
                        Icons.Default.ChevronRight,
                        "Next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayLabels.forEach { day ->
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            day,
                            modifier = Modifier.width(40.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val monthOfPage = YearMonth.now().plusMonths((page - initialPage).toLong())
                val monthGrid = remember(monthOfPage) { generateMonthGrid(monthOfPage, firstDay) }

                Column(modifier = Modifier.fillMaxWidth()) {
                    monthGrid.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEach { day ->
                                CalendarDayItem(
                                    day,
                                    isSelected = day == selectedDate,
                                    isCurrentMonth = day?.month == monthOfPage.month,
                                    hasIncome = day != null && incomeDates.contains(day.toString()),
                                    hasExpense = day != null && expenseDates.contains(day.toString()),
                                    hasUpcoming = day != null && upcomingDates.contains(day.toString()),
                                    isOverdueUpcoming = day != null && overdueUpcomingDates.contains(day.toString()),
                                    onDateSelected
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(colorScheme.tertiary, "Income")
                Spacer(Modifier.width(16.dp))
                LegendItem(colorScheme.error, "Expense")
                Spacer(Modifier.width(16.dp))
                LegendDiamond(colorScheme.primary, "Scheduled")
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier.size(6.dp).clip(CircleShape).background(color)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun LegendDiamond(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Rotated square = diamond
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
                .then(Modifier) // rotation handled visually by the square + small size
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
    }
}

@Composable
fun CalendarDayItem(
    day: LocalDate?,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    hasIncome: Boolean,
    hasExpense: Boolean,
    hasUpcoming: Boolean = false,
    isOverdueUpcoming: Boolean = false,
    onDateSelected: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val today = LocalDate.now()
    val isToday = day == today
    val isFuture = day != null && day.isAfter(today)

    Box(
        modifier = Modifier
            .size(44.dp) // Slightly larger touch target
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            val selectionModifier = if (isSelected) {
                Modifier.background(brush = primaryGradient(), shape = CircleShape)
            }             else if (isToday)
                Modifier.background(
                    color = colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                )
            else Modifier
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .then(selectionModifier)
                    .clickable { onDateSelected(day) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = when {
                        isSelected -> FontWeight.ExtraBold
                        isToday -> FontWeight.Bold
                        else -> FontWeight.Medium
                    },
                    color = when {
                        isSelected -> Color.White
                        isToday -> colorScheme.primary
                        !isCurrentMonth -> colorScheme.onSurface.copy(alpha = 0.25f)
                        isFuture && hasUpcoming -> colorScheme.onSurface
                        else -> colorScheme.onSurface
                    }
                )

                // Status Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.height(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasIncome) IndicatorDot(if (isSelected) Color.White else MaterialTheme.colorScheme.tertiary)
                    if (hasExpense) IndicatorDot(if (isSelected) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error)
                    if (hasUpcoming && !hasIncome && !hasExpense) {
                        val upcomingColor = when {
                            isSelected -> Color.White
                            isOverdueUpcoming -> colorScheme.error
                            else -> colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(upcomingColor)
                        )
                    } else if (hasUpcoming) {
                        // Has both transaction history AND upcoming — show a tiny primary dot
                        val upcomingColor = when {
                            isSelected -> Color.White.copy(alpha = 0.8f)
                            isOverdueUpcoming -> colorScheme.error
                            else -> colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(upcomingColor)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun IndicatorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(color)
    )
}

fun generateMonthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
    val firstOfMonth = month.atDay(1)
    val lastOfMonth = month.atEndOfMonth()

    val gridStart = firstOfMonth.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    val lastDayWeekStart = lastOfMonth.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val gridEnd = lastDayWeekStart.plusDays(6)

    val weeks = mutableListOf<List<LocalDate?>>()
    var current = gridStart

    while (!current.isAfter(gridEnd)) {
        val week = (0..6).map {
            val day = current.plusDays(it.toLong())
            if (day.monthValue == month.monthValue) day else null
        }
        weeks.add(week)
        current = current.plusWeeks(1)
    }
    return weeks
}