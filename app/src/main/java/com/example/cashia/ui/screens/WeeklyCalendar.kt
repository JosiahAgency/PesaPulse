package com.example.cashia.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.theme.primaryGradient
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun WeeklyCalendar(
    hapticFeedback: HapticFeedback,
    allTransactions: List<MpesaTransactions>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    rememberCoroutineScope()

    val initialPage = 500
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPage,
        pageCount = { 1000 }
    )
    val firstDay = PesaTimeConfiguration.firstDayOfWeek
    val startOfWeek = selectedDate.with(
        TemporalAdjusters.previousOrSame(firstDay)
    )
    startOfWeek.plusDays(6)

    LaunchedEffect(selectedDate) {
        LocalDate.now()

        val weeksDiff =
            java.time.temporal.ChronoUnit.WEEKS.between(startOfWeek, startOfWeek)
                .toInt()
        val targetPage = initialPage + weeksDiff

        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    val incomeDates = remember(allTransactions) {
        allTransactions.filter { it.type == "income" }.map { it.date }.toSet()
    }
    val expenseDates = remember(allTransactions) {
        allTransactions.filter { it.type == "expense" }.map { it.date }.toSet()
    }

    // Glassy Container
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
            ) { page ->
                val weekOffset = (page - initialPage).toLong()
                val startOfWeek = startOfWeek

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    (0..6).forEach { dayOffset ->
                        val date = startOfWeek.plusDays(dayOffset.toLong())
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()

                        WeeklyDayItem(
                            date = date,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasIncome = incomeDates.contains(date.toString()),
                            hasExpense = expenseDates.contains(date.toString()),
                            onDateSelected = onDateSelected,
                            hapticFeedback
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyDayItem(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasIncome: Boolean,
    hasExpense: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    hapticFeedback: HapticFeedback
) {
    val dayName = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }

    Column(
        modifier = Modifier
            .width(45.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onDateSelected(date) }
            .then(
                if (isSelected) Modifier
                    .background(primaryGradient())
                    .clickable { PesaActions.playClick(hapticFeedback) }
                else if (isToday) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(16.dp)
                )
                else Modifier
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )

        // Compact Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasIncome) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else MaterialTheme.colorScheme.tertiary)
                )
            }
            if (hasExpense) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}