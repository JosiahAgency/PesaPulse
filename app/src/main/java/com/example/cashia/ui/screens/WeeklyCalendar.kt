package com.example.cashia.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.CalendarActivity
import com.example.cashia.models.MonthPaceState
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TimeScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private val accessibleDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")

/**
 * The pager is virtually endless: pages map to `anchorWeek + (page - anchor)` weeks, so a
 * ~1M-page window centred on today covers roughly ±9,600 years of swiping in either
 * direction — far beyond `LocalDate`'s useful range, unlike the previous ±500-week window.
 */
private const val WEEK_PAGER_PAGE_COUNT = 1_000_001
private const val WEEK_PAGER_ANCHOR_PAGE = WEEK_PAGER_PAGE_COUNT / 2

/**
 * Week strip for day/week scopes, and a period stepper for month/year scopes — the strip can
 * only express a 7-day window, so leaving it on screen while the dashboard is scoped to a
 * month or a year made day taps look unrelated to the period they actually changed.
 */
@Composable
fun WeeklyCalendar(
    hapticFeedback: HapticFeedback,
    activity: CalendarActivity,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onWeekVisible: (LocalDate) -> Unit = {},
    pace: MonthPaceState = MonthPaceState(),
    onEditMonthTarget: () -> Unit = {},
    scope: TimeScope = TimeScope.DAY,
    periodLabel: String = "",
) {
    val today = remember { LocalDate.now() }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = WEEK_PAGER_ANCHOR_PAGE,
        pageCount = { WEEK_PAGER_PAGE_COUNT }
    )
    val firstDayOfWeek = remember { PesaTimeConfiguration.firstDayOfWeek }
    val anchorWeekStart = remember(today, firstDayOfWeek) {
        today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate, firstDayOfWeek) {
        val targetWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        val weeksDiff = java.time.temporal.ChronoUnit.WEEKS
            .between(anchorWeekStart, targetWeekStart).toInt()
        val targetPage = (WEEK_PAGER_ANCHOR_PAGE + weeksDiff)
            .coerceIn(0, WEEK_PAGER_PAGE_COUNT - 1)
        if (pagerState.currentPage != targetPage) {
            isProgrammaticScroll = true
            pagerState.animateScrollToPage(targetPage)
            isProgrammaticScroll = false
        }
    }

    // Reports the week actually on screen back to the caller so a bounded query can stay
    // centered on it — without this, free-swiping away from `selectedDate`'s week (which
    // doesn't itself change on a swipe, only on a day tap) would show stale/missing dots
    // once the caller stops handing in the full transaction table.
    LaunchedEffect(Unit) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (isProgrammaticScroll) return@collect
            onWeekVisible(anchorWeekStart.plusWeeks((page - WEEK_PAGER_ANCHOR_PAGE).toLong()))
        }
    }

    val incomeDates = activity.incomeDates
    val expenseDates = activity.expenseDates

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            if (scope == TimeScope.MONTH || scope == TimeScope.YEAR) {
                PeriodStepperRow(
                    scope = scope,
                    periodLabel = periodLabel,
                    hapticFeedback = hapticFeedback,
                    onStep = { steps ->
                        onDateSelected(
                            if (scope == TimeScope.MONTH) selectedDate.plusMonths(steps)
                            else selectedDate.plusYears(steps)
                        )
                    }
                )
            } else {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val weekOffset = (page - WEEK_PAGER_ANCHOR_PAGE).toLong()
                    val weekStart = anchorWeekStart.plusWeeks(weekOffset)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (dayOffset in 0..6) {
                            val date = weekStart.plusDays(dayOffset.toLong())
                            val dateStr = date.toString()
                            WeeklyDayItem(
                                date = date,
                                isSelected = if (scope == TimeScope.WEEK) {
                                    date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)) ==
                                            selectedDate.with(
                                                TemporalAdjusters.previousOrSame(firstDayOfWeek)
                                            )
                                } else {
                                    date == selectedDate
                                },
                                isToday = date == today,
                                hasIncome = dateStr in incomeDates,
                                hasExpense = dateStr in expenseDates,
                                onDateSelected = onDateSelected,
                                hapticFeedback = hapticFeedback
                            )
                        }
                    }
                }
            }
            MonthPaceRow(
                pace = pace,
                onEditTarget = onEditMonthTarget,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/** Month/year equivalent of the week strip: steps the selected date a whole period at a time. */
@Composable
private fun PeriodStepperRow(
    scope: TimeScope,
    periodLabel: String,
    hapticFeedback: HapticFeedback,
    onStep: (Long) -> Unit,
) {
    val unit = if (scope == TimeScope.MONTH) "month" else "year"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            PesaActions.playClick(hapticFeedback)
            onStep(-1)
        }) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Previous $unit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = periodLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = {
            PesaActions.playClick(hapticFeedback)
            onStep(1)
        }) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Next $unit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "Pace this month" row — see [MonthPaceState] for what this reuses/represents. */
@Composable
private fun MonthPaceRow(
    pace: MonthPaceState,
    onEditTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progressText = if (pace.hasTarget) "${pace.percentLabel}%" else "Set target"
    val trailingText = when {
        !pace.hasTarget -> "Add this month target"
        pace.remainingLabel > 0.0 -> "Left ${PesaTimeConfiguration.formatCurrency(pace.remainingLabel)}"
        else -> "Over ${PesaTimeConfiguration.formatCurrency(pace.spentAmount - pace.targetAmount)}"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onEditTarget)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pace this month",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                progressText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                trailingText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (pace.hasTarget && pace.remainingLabel <= 0.0)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.tertiary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(pace.progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(primaryGradient())
            )
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
    val dayName = remember(date) {
        date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    }
    val dayNumber = remember(date) { date.dayOfMonth.toString() }
    // The activity dots are the only cue that a day has transactions, and they differ by
    // colour alone — so the shapes differ too (circle = money in, square = money out) and the
    // whole cell carries one merged description for screen readers.
    val cellDescription = remember(date, isToday, hasIncome, hasExpense) {
        buildString {
            append(date.format(accessibleDateFormatter))
            if (isToday) append(", today")
            when {
                hasIncome && hasExpense -> append(", money in and money out")
                hasIncome -> append(", money in")
                hasExpense -> append(", money out")
                else -> append(", no transactions")
            }
        }
    }

    Column(
        modifier = Modifier
            .width(45.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                onDateSelected(date)
                PesaActions.playClick(hapticFeedback)
            }
            .semantics(mergeDescendants = true) {
                contentDescription = cellDescription
                if (isSelected) selected = true
            }
            .then(
                when {
                    isSelected -> Modifier.background(primaryGradient())
                    isToday -> Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )

                    else -> Modifier
                }
            )
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White.copy(alpha = 0.8f)
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = dayNumber,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = when {
                isSelected -> Color.White
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            if (hasExpense) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.error
                        )
                )
            }
        }
    }
}