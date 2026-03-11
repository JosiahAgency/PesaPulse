package com.example.cashia.utility

import com.example.cashia.models.MpesaTransactions
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

fun weekBucket(date: LocalDate): String {
    val today = LocalDate.now()
    return when {
        !date.isBefore(today) -> "TODAY"
        date.isAfter(today.minusWeeks(1)) -> "THIS WEEK"
        date.isAfter(today.minusWeeks(2)) -> "LAST WEEK"
        date.isAfter(today.minusMonths(1)) -> "EARLIER THIS MONTH"
        date.isAfter(today.minusMonths(3)) -> "${
            date.month.name.take(3).replaceFirstChar { it.uppercase() }
        } ${date.year}"

        else -> date.year.toString()
    }
}

data class ParsedTx(
    val tx: MpesaTransactions,
    val date: LocalDate,
    val bucket: String
)

private val amountFormatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}

fun formatAmount(amount: Double): String = amountFormatter.format(amount)

