package com.example.cashia.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration
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
    val pagerState = rememberPagerState(
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
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (isProgrammaticScroll) return@collect   // ← skip intermediate pages
            val newMonth = YearMonth.now().plusMonths((page - initialPage).toLong())
            if (selectedDate.year != newMonth.year || selectedDate.month != newMonth.month) {
                val targetDate = if (newMonth == YearMonth.now()) LocalDate.now()
                else newMonth.atDay(1)
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
            isProgrammaticScroll = true
            pagerState.animateScrollToPage(targetPage)
            isProgrammaticScroll = false
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f

    val monthTitle = currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } +
            " " + currentMonth.year

    val (incomeDates, expenseDates) = remember(allTransactions) {
        val inc = mutableSetOf<String>()
        val exp = mutableSetOf<String>()
        for (tx in allTransactions) {
            when (tx.type) {
                "income" -> inc.add(tx.date)
                "expense" -> exp.add(tx.date)
            }
        }
        inc to exp
    }

    val upcomingDates = remember(upcomingTransactions) {
        upcomingTransactions.map { it.dueDate }.toHashSet()
    }
    val todayStr = remember { LocalDate.now().toString() }
    val overdueUpcomingDates = remember(upcomingTransactions, todayStr) {
        upcomingTransactions.filter { it.dueDate < todayStr }.map { it.dueDate }.toHashSet()
    }
    val today = remember { LocalDate.now() }

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
                    SectionLabel(monthTitle)
                    val currentMonthPrefix = remember(currentMonth) {
                        "${currentMonth.year}-${
                            currentMonth.monthValue.toString().padStart(2, '0')
                        }"
                    }
                    val upcomingThisMonth = remember(upcomingTransactions, currentMonthPrefix) {
                        upcomingTransactions.count { it.dueDate.startsWith(currentMonthPrefix) }
                    }
                    if (upcomingThisMonth > 0) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "$upcomingThisMonth Scheduled",
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
                        tint = MaterialTheme.colorScheme.primary
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
            HorizontalPager(
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
                                    today,
                                    isSelected = day == selectedDate,
                                    isCurrentMonth = day?.month == monthOfPage.month,
                                    hasIncome = day != null && incomeDates.contains(day.toString()),
                                    hasExpense = day != null && expenseDates.contains(day.toString()),
                                    hasUpcoming = day != null && upcomingDates.contains(day.toString()),
                                    isOverdueUpcoming = day != null && overdueUpcomingDates.contains(
                                        day.toString()
                                    ),
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
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Subtitles(label)
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
                .then(Modifier)
        )
        Subtitles(label)
    }
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
        val week = ArrayList<LocalDate?>(7)
        for (i in 0..6) {
            val day = current.plusDays(i.toLong())
            week.add(if (day.monthValue == month.monthValue) day else null)
        }
        weeks.add(week)
        current = current.plusWeeks(1)
    }
    return weeks
}