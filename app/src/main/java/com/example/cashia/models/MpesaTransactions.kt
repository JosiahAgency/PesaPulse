package com.example.cashia.models

import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val regexSentTo = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*Ksh([\d,]+\.\d{2})\s+sent\s+to\s+(.+?)\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexPaidTo = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*Ksh([\d,]+\.\d{2})\s+paid\s+to\s+(.+?)\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexAirtime = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.?\s*You\s+bought\s+Ksh([\d,]+(?:\.\d{2})?)\s+of\s+airtime\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexReceived = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*You\s+have\s+received\s+Ksh([\d,]+(?:\.\d{2})?)\s+from\s+(.+?)\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexWithdraw = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})\s*Withdraw\s+Ksh([\d,]+\.\d{2})\s+from\s+(.+?)\s+New""",
    RegexOption.IGNORE_CASE
)
private val balanceRegex =
    """(?:balance|account)\s+is\s+Ksh\s*([\d,]+\.\d{2})""".toRegex(RegexOption.IGNORE_CASE)

data class MpesaTransactions(
    val id: String,
    val amount: Double,
    val recipient: String,
    val date: String,
    val type: String,
    val balance: Double,
    val time: String,
)

fun parseMpesaMessage(message: String): MpesaTransactions? {
    try {
        val balance = balanceRegex.find(message)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: 0.0

        fun parseDate(dateStr: String): String {
            val pattern = if (dateStr.length <= 8) "d/M/yy" else "d/M/yyyy"
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern)).toString()
        }

        fun formatTime(timeStr: String): String = timeStr.uppercase().trim()

        return when {
            regexSentTo.find(message) != null -> {
                val match = regexSentTo.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    recipient.trim(),
                    parseDate(date),
                    "expense",
                    balance,
                    formatTime(time)
                )
            }

            regexPaidTo.find(message) != null -> {
                val match = regexPaidTo.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    recipient.trim(),
                    parseDate(date),
                    "expense",
                    balance,
                    formatTime(time)
                )
            }

            regexAirtime.find(message) != null -> {
                val match = regexAirtime.find(message)!!
                val (id, amount, date, time) = match.destructured
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    "Safaricom Airtime",
                    parseDate(date),
                    "expense",
                    balance,
                    formatTime(time)

                )
            }

            regexWithdraw.find(message) != null -> {
                val match = regexWithdraw.find(message)!!
                val (id, date, time, amount, recipient) = match.destructured
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    recipient.trim(),
                    parseDate(date),
                    "expense",
                    balance,
                    formatTime(time)
                )
            }

            regexReceived.find(message) != null -> {
                val match = regexReceived.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    recipient.trim(),
                    parseDate(date),
                    "income",
                    balance,
                    formatTime(time)
                )
            }

            else -> null
        }
    } catch (e: Exception) {
        Log.e("Transactions", "Parsing error: ${e.message}")
        return null
    }
}