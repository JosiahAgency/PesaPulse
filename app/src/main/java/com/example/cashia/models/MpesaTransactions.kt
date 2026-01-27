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

private val costRegex =
    """(?:Transaction\s+cost|cost),?\s+Ksh\s*([\d,]+\.\d{2})""".toRegex(RegexOption.IGNORE_CASE)

@Entity(tableName = "MpesaTransactions")
data class MpesaTransactions(
    @PrimaryKey val id: String,
    val amount: Double,
    val transactionCost: Double,
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

        val transactionCost = costRegex.find(message)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        return when {
            regexSentTo.find(message) != null -> {
                val match = regexSentTo.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                buildTransaction(
                    id,
                    amount,
                    recipient,
                    date,
                    time,
                    "expense",
                    balance,
                    transactionCost
                )
            }

            regexPaidTo.find(message) != null -> {
                val match = regexPaidTo.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                buildTransaction(
                    id,
                    amount,
                    recipient,
                    date,
                    time,
                    "expense",
                    balance,
                    transactionCost
                )
            }

            regexAirtime.find(message) != null -> {
                val match = regexAirtime.find(message)!!
                val (id, amount, date, time) = match.destructured
                buildTransaction(id, amount, "Safaricom Airtime", date, time, "expense", balance, transactionCost)
            }

            regexWithdraw.find(message) != null -> {
                val match = regexWithdraw.find(message)!!
                val (id, date, time, amount, recipient) = match.destructured
                buildTransaction(id, amount, recipient, date, time, "expense", balance, transactionCost)
            }

            regexReceived.find(message) != null -> {
                val match = regexReceived.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                buildTransaction(id, amount, recipient, date, time, "income", balance, transactionCost)
            }

            else -> null
        }
    } catch (e: Exception) {
        Log.e("Transactions", "Parsing error: ${e.message}")
        return null
    }
}

private fun buildTransaction(
    id: String,
    amountStr: String,
    recipient: String,
    dateStr: String,
    timeStr: String,
    type: String,
    balance: Double,
    cost: Double
): MpesaTransactions {
    val pattern = if (dateStr.length <= 8) "d/M/yy" else "d/M/yyyy"
    val formattedDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern)).toString()
    val formattedTime = timeStr.uppercase().trim()
    val cleanRecipient = recipient.trim().removeSuffix(".")
    return MpesaTransactions(
        id = id.trim(),
        amount = amountStr.replace(",", "").toDouble(),
        transactionCost = cost,
        recipient = cleanRecipient,
        date = formattedDate,
        type = type,
        balance = balance,
        time = formattedTime,
        timestamp = convertToTimestamp(formattedDate, formattedTime)
    )
}

fun convertToTimestamp(dateStr: String, timeStr: String): Long {
    return try {
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