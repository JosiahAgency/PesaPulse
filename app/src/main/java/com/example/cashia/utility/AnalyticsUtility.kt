package com.example.cashia.utility

import android.app.DatePickerDialog
import android.content.Context
import com.example.cashia.models.MpesaTransactions
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
        .filter { it.timestamp in prevStartTs..prevEndTs && it.type == typeLower }
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