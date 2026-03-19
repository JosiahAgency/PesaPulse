package com.example.cashia.utility

import com.example.cashia.models.FinanceProjection
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class ProjectionPeriod(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_QUARTER("This Quarter"),
    THIS_YEAR("This Year");

    fun daysRemaining(): Int {
        val today = LocalDate.now()
        val end = when (this) {
            THIS_WEEK -> today.with(DayOfWeek.SUNDAY)
            THIS_MONTH -> today.withDayOfMonth(today.month.length(today.isLeapYear))
            THIS_QUARTER -> {
                val qEndMonth = ((today.monthValue - 1) / 3 + 1) * 3
                today.withMonth(qEndMonth)
                    .withDayOfMonth(today.withMonth(qEndMonth).month.length(today.isLeapYear))
            }

            THIS_YEAR -> today.withDayOfYear(if (today.isLeapYear) 366 else 365)
        }
        return ChronoUnit.DAYS.between(today, end).toInt().coerceAtLeast(1)
    }

    fun endLabel(): String =
        LocalDate.now().plusDays(daysRemaining().toLong())
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}

fun computeConfidence(
    txCount: Int,
    lookbackDays: Int,
    horizonDays: Int,
    projection: FinanceProjection
): Int {
    var score = 100
    val density = txCount.toFloat() / lookbackDays.coerceAtLeast(1)
    if (density < 0.5f) score -= 20
    else if (density < 1f) score -= 10
    if (horizonDays > 30) score -= 15
    else if (horizonDays > 14) score -= 5
    if (txCount < 10) score -= 20
    else if (txCount < 30) score -= 10
    if (projection.dataQualityNote.isNotEmpty()) score -= 15
    return score.coerceIn(10, 95)
}

fun daysUntilZero(trend: List<Pair<LocalDate, Double>>, currentBalance: Double): Int? {
    if (currentBalance <= 0) return 0
    trend.forEachIndexed { i, (_, bal) ->
        if (bal <= 0) return i + 1
    }
    return null
}