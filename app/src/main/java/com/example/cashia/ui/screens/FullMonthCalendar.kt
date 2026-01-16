package com.example.cashia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.MpesaTransactions
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun FullMonthCalendar(
    allTransactions: List<MpesaTransactions>,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    LaunchedEffect(selectedDate) {
        val selectedYearMonth = YearMonth.from(selectedDate)
        if (currentMonth != selectedYearMonth) {
            currentMonth = selectedYearMonth
        }
    }

    val monthGrid = remember(currentMonth) { generateMonthGrid(currentMonth) }
    val monthTitle = currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } +
            " " + currentMonth.year

    val incomeDates = remember(allTransactions) {
        allTransactions.filter { it.type == "income" }.map { it.date }.toSet()
    }
    val expenseDates = remember(allTransactions) {
        allTransactions.filter { it.type == "expense" }.map { it.date }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentMonth = currentMonth.minusMonths(1)
            }) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowLeft,
                    "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = {
                currentMonth = currentMonth.plusMonths(1)
            }) {
                Icon(
                    Icons.Default.KeyboardDoubleArrowRight,
                    "Next Month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Text(
                        day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

            }
        }
        Spacer(Modifier.height(4.dp))
//
//        val transactionsByDate = remember(allTransactions) {
//            allTransactions.groupBy { it.date }
//        }

        monthGrid.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.size(40.dp))
                    } else {
                        val isSelected = day == selectedDate
                        val dayIsoString = day.toString()
                        val hasIncome = incomeDates.contains(dayIsoString)
                        val hasExpense = expenseDates.contains(dayIsoString)

                        val backgroundColor = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            hasIncome || hasExpense -> MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.3f
                            )
                            else -> Color.Transparent
                        }

                        val textColor = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(backgroundColor)
                                .clickable {
                                    onDateSelected(day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor
                                )

                                if (hasIncome || hasExpense) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (hasExpense) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) Color.White.copy(alpha = 0.7f)
                                                        else MaterialTheme.colorScheme.error
                                                    )
                                            )
                                        }
                                        if (hasIncome) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) Color.White
                                                        else MaterialTheme.colorScheme.tertiary
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

fun generateMonthGrid(month: YearMonth): List<List<LocalDate?>> {
    val firstDay = month.atDay(1)
    val lastDay = month.atEndOfMonth()

    val start = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    val end = lastDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))

    val days = mutableListOf<List<LocalDate?>>()
    var current = start

    while (!current.isAfter(end)) {
        val week = (0..6).map {
            val day = current.plusDays(it.toLong())
            if (day.monthValue == month.monthValue) day else null
        }
        days.add(week)
        current = current.plusWeeks(1)
    }
    return days
}