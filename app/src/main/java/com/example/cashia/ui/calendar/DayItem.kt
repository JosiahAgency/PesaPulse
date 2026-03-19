package com.example.cashia.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.ui.theme.primaryGradient
import java.time.LocalDate


@Composable
fun CalendarDayItem(
    day: LocalDate?,
    today: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    hasIncome: Boolean,
    hasExpense: Boolean,
    hasUpcoming: Boolean = false,
    isOverdueUpcoming: Boolean = false,
    onDateSelected: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isToday = day == today
    val isFuture = day != null && day.isAfter(today)

    Box(
        modifier = Modifier
            .size(44.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            val selectionModifier = when {
                isSelected -> Modifier.background(brush = primaryGradient(), shape = CircleShape)
                isToday -> Modifier.background(
                    color = colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                )

                else -> Modifier
            }
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.height(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasIncome) IndicatorDot(if (isSelected) Color.White else MaterialTheme.colorScheme.tertiary)
                    if (hasExpense) IndicatorDot(if (isSelected) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error)
                    if (hasUpcoming) {
                        val dotSize = if (!hasIncome && !hasExpense) 4.dp else 3.dp
                        val upcomingColor = when {
                            isSelected -> Color.White.copy(alpha = if (!hasIncome && !hasExpense) 1f else 0.8f)
                            isOverdueUpcoming -> colorScheme.error
                            else -> colorScheme.primary
                        }
                        Box(
                            modifier = Modifier
                                .size(dotSize)
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