package com.example.cashia.utility

import android.app.DatePickerDialog
import android.content.Context
import com.example.cashia.models.MpesaTransactions
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

fun calculatePreviousPeriod(
    transactions: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate,
    type: TransactionType
): Double {
    val periodDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
    val previousStart = startDate.minusDays(periodDays)
    val previousEnd = startDate.minusDays(1)
    val prevStartTs = previousStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val prevEndTs =
        previousEnd.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val typeLower = type.name.lowercase()
    return transactions
        .filter { it.timestamp in prevStartTs..prevEndTs && it.type == typeLower && it.reversedTxId == null }
        .sumOf { it.amount }
}

fun formatCompactAmount(amount: Double): String = when {
    amount >= 1_000_000 -> "${(amount / 1_000_000).toInt()}M"
    amount >= 1_000 -> "${(amount / 1_000).toInt()}k"
    else -> amount.toInt().toString()
}

fun formatXLabel(key: String): String = if (key.length > 7)
    LocalDate.parse(key).format(DateTimeFormatter.ofPattern("dd MMM"))
else
    YearMonth.parse(key).format(DateTimeFormatter.ofPattern("MMM yy"))

fun showDatePicker(
    context: Context,
    startDate: LocalDate,
    endDate: LocalDate,
    isStart: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val initialDate = if (isStart) startDate else endDate
    val calendar = Calendar.getInstance().apply {
        set(initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth)
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            val newDate = LocalDate.of(year, month + 1, day)
            when {
                isStart && newDate.isAfter(endDate) -> onDateSelected(endDate)
                !isStart && newDate.isBefore(startDate) -> onDateSelected(startDate)
                else -> onDateSelected(newDate)
            }
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}


data class DailySpend(val date: LocalDate, val amount: Double)
data class WeekdayPattern(val day: DayOfWeek, val avgAmount: Double, val txCount: Int)
data class HourlyPattern(val hour: Int, val txCount: Int, val totalAmount: Double)
data class SpendingStreak(val days: Int, val startDate: LocalDate, val endDate: LocalDate)
data class RecurringMerchant(
    val recipient: String,
    val avgAmount: Double,
    val occurrences: Int,
    val lastDate: LocalDate,
    val totalAmount: Double
)

/** Daily spend series for the given window, zeroed for days with no transactions. */
fun dailySpendSeries(
    transactions: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate,
    type: String
): List<DailySpend> {
    val byDay = transactions
        .filter { it.type == type && it.reversedTxId == null }
        .groupBy { LocalDate.parse(it.date) }
        .mapValues { (_, txs) -> txs.sumOf { it.amount } }

    val result = mutableListOf<DailySpend>()
    var cursor = startDate
    while (!cursor.isAfter(endDate)) {
        result.add(DailySpend(cursor, byDay[cursor] ?: 0.0))
        cursor = cursor.plusDays(1)
    }
    return result
}

/** Average spend by day-of-week (useful for "you spend most on Fridays" insight). */
fun weekdaySpendPattern(
    transactions: List<MpesaTransactions>,
    type: String
): List<WeekdayPattern> {
    val grouped = transactions
        .filter { it.type == type && it.reversedTxId == null }
        .groupBy {
            runCatching { LocalDate.parse(it.date).dayOfWeek }.getOrNull() ?: return@groupBy null
        }
        .filterKeys { it != null }

    return DayOfWeek.entries.map { dow ->
        val txs = grouped[dow] ?: emptyList()
        val days = txs.map { LocalDate.parse(it.date) }.distinct().size.coerceAtLeast(1)
        WeekdayPattern(
            day = dow,
            avgAmount = if (txs.isEmpty()) 0.0 else txs.sumOf { it.amount } / days,
            txCount = txs.size
        )
    }
}

/** Tx count + total by hour-of-day (0–23). */
fun hourlyPattern(transactions: List<MpesaTransactions>, type: String): List<HourlyPattern> {
    val grouped = transactions
        .filter { it.type == type && it.reversedTxId == null }
        .groupBy {
            runCatching {
                LocalTime.parse(
                    it.time,
                    DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.US)
                ).hour
            }.getOrNull() ?: -1
        }
        .filterKeys { it >= 0 }

    return (0..23).map { h ->
        val txs = grouped[h] ?: emptyList()
        HourlyPattern(h, txs.size, txs.sumOf { it.amount })
    }
}

/**
 * Merchants that appear in ≥2 different calendar months — likely recurring bills.
 * Sorted by occurrence count descending.
 */
fun detectRecurringMerchants(
    transactions: List<MpesaTransactions>,
    type: String,
    minMonths: Int = 2
): List<RecurringMerchant> {
    return transactions
        .filter { it.type == type && it.reversedTxId == null }
        .groupBy { it.recipient.trim().lowercase() }
        .mapNotNull { (_, txs) ->
            val months = txs.map {
                runCatching { YearMonth.from(LocalDate.parse(it.date)) }.getOrNull()
            }.filterNotNull().distinct()
            if (months.size < minMonths) return@mapNotNull null

            val latest = txs.maxOf { LocalDate.parse(it.date) }
            RecurringMerchant(
                recipient = txs.first().recipient.trim(),
                avgAmount = txs.sumOf { it.amount } / txs.size,
                occurrences = txs.size,
                lastDate = latest,
                totalAmount = txs.sumOf { it.amount }
            )
        }
        .sortedByDescending { it.occurrences }
}

/**
 * Longest consecutive-day streak where at least one expense was recorded.
 */
fun longestSpendingStreak(transactions: List<MpesaTransactions>): SpendingStreak? {
    val days = transactions
        .filter { it.type == "expense" && it.reversedTxId == null }
        .mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        .toSortedSet()

    if (days.isEmpty()) return null

    var bestStart = days.first()
    var bestLen = 1
    var curStart = days.first()
    var curLen = 1

    val dayList = days.toList()
    for (i in 1 until dayList.size) {
        if (dayList[i] == dayList[i - 1].plusDays(1)) {
            curLen++
            if (curLen > bestLen) {
                bestLen = curLen; bestStart = curStart
            }
        } else {
            curStart = dayList[i]
            curLen = 1
        }
    }
    return SpendingStreak(bestLen, bestStart, bestStart.plusDays((bestLen - 1).toLong()))
}

/** Month-over-month change as a percentage. Null if previous period is zero. */
fun periodChangePercent(current: Double, previous: Double): Float? {
    if (previous == 0.0) return null
    return ((current - previous) / previous * 100).toFloat()
}