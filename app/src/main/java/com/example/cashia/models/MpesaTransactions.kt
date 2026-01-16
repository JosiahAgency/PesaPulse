package com.example.cashia.models

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

@Entity(tableName = "MpesaTransactions")
data class MpesaTransactions(
    @PrimaryKey val id: String,
    val amount: Double,
    val recipient: String,
    val date: String,
    val type: String,
    val balance: Double,
    val time: String,
    val timestamp: Long,
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
                val formattedDate = parseDate(date)
                val formattedTime = formatTime(time)
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    recipient.trim(),
                    parseDate(date),
                    "expense",
                    balance,
                    formatTime(time),
                    timestamp = convertToTimestamp(formattedDate, formattedTime)
                )
            }

            regexPaidTo.find(message) != null -> {
                val match = regexPaidTo.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                val formattedDate = parseDate(date)
                val formattedTime = formatTime(time)
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    recipient.trim(),
                    parseDate(date),
                    "expense",
                    balance,
                    formatTime(time),
                    timestamp = convertToTimestamp(formattedDate, formattedTime)

                )
            }

            regexAirtime.find(message) != null -> {
                val match = regexAirtime.find(message)!!
                val (id, amount, date, time) = match.destructured
                val formattedDate = parseDate(date)
                val formattedTime = formatTime(time)
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    "Safaricom Airtime",
                    parseDate(date),
                    "expense",
                    balance,
                    formatTime(time),
                    timestamp = convertToTimestamp(formattedDate, formattedTime)

                )
            }

            regexWithdraw.find(message) != null -> {
                val match = regexWithdraw.find(message)!!
                val (id, date, time, amount, recipient) = match.destructured
                val formattedDate = parseDate(date)
                val formattedTime = formatTime(time)
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    recipient.trim(),
                    parseDate(date),
                    "expense",
                    balance,
                    formatTime(time),
                    timestamp = convertToTimestamp(formattedDate, formattedTime)

                )
            }

            regexReceived.find(message) != null -> {
                val match = regexReceived.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                val formattedDate = parseDate(date)
                val formattedTime = formatTime(time)
                MpesaTransactions(
                    id.trim(),
                    amount.replace(",", "").toDouble(),
                    recipient.trim(),
                    parseDate(date),
                    "income",
                    balance,
                    formatTime(time),
                    timestamp = convertToTimestamp(formattedDate, formattedTime)

                )
            }

            else -> null
        }
    } catch (e: Exception) {
        Log.e("Transactions", "Parsing error: ${e.message}")
        return null
    }
}

fun convertToTimestamp(dateStr: String, timeStr: String): Long {
    return try {
        // pattern: 2026-01-16 + 11:24 AM
        val dateTimeStr = "$dateStr $timeStr"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a", Locale.US)
        LocalDateTime.parse(dateTimeStr, formatter)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}