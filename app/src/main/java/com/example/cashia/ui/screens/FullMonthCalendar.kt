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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun FullMonthCalendar(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val monthGrid = generateMonthGrid(currentMonth)
    val monthTitle = currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } +
            " " + currentMonth.year

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = {
                currentMonth = currentMonth.minusMonths(1)
            }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = {
                currentMonth = currentMonth.plusMonths(1)
            }) {
                Icon(Icons.Default.ArrowForward, null)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Mon", "Tue", "Wed", "Thur", "Fri", "Sat", "Sun").forEach {
                Text(it, style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(8.dp))

        monthGrid.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Box(modifier = Modifier.size(38.dp))
                    } else {
                        val isSelected = day == selectedDate
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    onDateSelected(day)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

fun generateMonthGrid(month: YearMonth): List<List<LocalDate?>> {
    val firstDay = month.atDay(1)
    val lastDay = month.atEndOfMonth()

    val start = firstDay.with(DayOfWeek.MONDAY)
    val end = lastDay.with(DayOfWeek.SUNDAY)

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

