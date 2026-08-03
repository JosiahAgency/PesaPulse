package com.example.cashia.utility

import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.DayGroup
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dashboardDayFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM")

/**
 * Dashboard-local day-section header — a sibling to [weekBucket] rather than a shared
 * change to it, so History's existing "THIS WEEK"/"LAST WEEK" bucketing is untouched.
 */
fun dashboardDayBucket(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "TODAY"
        today.minusDays(1) -> "YESTERDAY"
        else -> date.format(dashboardDayFormatter)
    }
}

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

/**
 * Derives the transaction's LocalDate from its epoch-millis [MpesaTransactions.timestamp]
 * instead of parsing the `date` string — far cheaper and always populated.
 */
fun MpesaTransactions.localDate(): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

fun MpesaTransactions.toParsedTx(): ParsedTx {
    val d = localDate()
    return ParsedTx(this, d, weekBucket(d))
}

fun buildDayGroups(transactions: List<MpesaTransactions>): List<DayGroup> =
    transactions
        .groupBy { it.localDate() }
        .entries
        .sortedByDescending { it.key }
        .map { (date, dayTxs) ->
            var income = 0.0
            var expense = 0.0
            for (tx in dayTxs) {
                when (tx.type) {
                    "income" -> income += tx.amount
                    "expense" -> expense += tx.amount
                }
            }
            DayGroup(
                date = date,
                transactions = dayTxs.sortedByDescending { it.timestamp },
                dayIncome = income,
                dayExpense = expense,
                dayBalance = dayTxs.lastOrNull { it.balance > 0 }?.balance ?: 0.0
            )
        }

private val amountFormatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
    minimumFractionDigits = 2; maximumFractionDigits = 2
}

fun formatAmount(amount: Double): String = amountFormatter.format(amount)
