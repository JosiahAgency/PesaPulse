package com.example.cashia.ui.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.cashia.daos.DayTotalsRow
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.formatAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter


private val PREVIEW_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * A single cell of the month grid.
 *
 * [totals] carries the day's money movement. It drives the indicator dots and a long-press
 * preview of the actual amounts, so checking "how much went out on the 14th?" no longer
 * requires tapping through to the ledger and back.
 */
@Composable
fun CalendarDayItem(
    day: LocalDate?,
    today: LocalDate,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    totals: DayTotalsRow? = null,
    hasUpcoming: Boolean = false,
    isOverdueUpcoming: Boolean = false,
    onDateSelected: (LocalDate) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val isToday = day == today
    val isFuture = day != null && day.isAfter(today)
    val hasIncome = (totals?.income ?: 0.0) > 0.0
    val hasExpense = (totals?.expense ?: 0.0) > 0.0
    var showPreview by remember(day) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(44.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            // The dots are colour-coded only, which says nothing to a screen reader and
            // little to a colourblind user, so the cell announces its activity in words.
            val description = buildString {
                append(day.format(PREVIEW_DATE_FORMAT))
                if (isToday) append(", today")
                if (hasIncome) append(", money in ${formatAmount(totals?.income ?: 0.0)}")
                if (hasExpense) append(", money out ${formatAmount(totals?.expense ?: 0.0)}")
                if (hasUpcoming) append(if (isOverdueUpcoming) ", overdue payment" else ", scheduled payment")
                if (isSelected) append(", selected")
            }
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
                    .combinedClickable(
                        onClick = { onDateSelected(day) },
                        onLongClick = {
                            if (totals != null) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showPreview = true
                            }
                        }
                    )
                    .semantics(mergeDescendants = true) { contentDescription = description },
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
                                .rotate(45f)
                                .clip(RoundedCornerShape(1.dp))
                                .background(upcomingColor)
                        )
                    }
                }
            }

            if (showPreview && totals != null) {
                DayTotalsPreview(
                    day = day,
                    totals = totals,
                    onDismiss = { showPreview = false }
                )
            }
        }
    }
}

/** Long-press bubble showing a day's in/out totals without leaving the grid. */
@Composable
private fun DayTotalsPreview(
    day: LocalDate,
    totals: DayTotalsRow,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val verticalOffset = with(LocalDensity.current) { (-8).dp.roundToPx() }

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, verticalOffset),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colorScheme.surface,
            border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f)),
            tonalElevation = 3.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = day.format(PREVIEW_DATE_FORMAT),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
                if (totals.income > 0.0) {
                    PreviewAmountRow("In", totals.income, colorScheme.tertiary)
                }
                if (totals.expense > 0.0) {
                    PreviewAmountRow("Out", totals.expense, colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PreviewAmountRow(label: String, amount: Double, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label ${formatAmount(amount)}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
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