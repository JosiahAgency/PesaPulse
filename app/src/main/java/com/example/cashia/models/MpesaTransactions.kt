package com.example.cashia.models

import android.util.Log
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cashia.Utility.PesaTimeConfiguration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val regexBalanceInquiry = Regex(
    """([A-Z0-9]+)\s+Confirmed\.\s+Your\s+account\s+balance\s+was:\s+M-PESA\s+Account\s+:\s+Ksh([\d,]+\.\d{2})\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
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

private val regexReversal = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*Your\s+original\s+transaction\s+[A-Z0-9]+\s+in\s+favour\s+of\s+(.+?)\s+has\s+been\s+reversed\s+successfully\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})\s+and\s+Ksh([\d,]+\.\d{2})\s+has\s+been\s+credited""",
    RegexOption.IGNORE_CASE
)

@Entity(
    tableName = "MpesaTransactions", indices = [
        Index(value = ["timestamp"]),   // used by getLastTransactionTimestamp + sync diff
        Index(value = ["date"]),        // used by calendar day filter + month filter
        Index(value = ["type"]),        // used by income/expense splits everywhere
        Index(value = ["category"])     // used by category filter in TransactionScreen
    ]
)
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
    val category: String = TransactionCategory.OTHER.name,
) {
    @get:androidx.room.Ignore
    val displayTime: String by lazy {
        try {
            val storedTime = LocalTime.parse(
                time, DateTimeFormatter.ofPattern("h:mm a", Locale.US)
            )
            storedTime.format(PesaTimeConfiguration.getTimeFormatter())
        } catch (e: Exception) {
            time
        }
    }

    @get:androidx.room.Ignore
    val displayDate: String by lazy {
        try {
            LocalDate.parse(date).format(PesaTimeConfiguration.getDateFormatter())
        } catch (e: Exception) {
            date
        }
    }
}

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
                buildTransaction(
                    id,
                    amount,
                    "Safaricom Airtime",
                    date,
                    time,
                    "expense",
                    balance,
                    transactionCost
                )
            }

            regexWithdraw.find(message) != null -> {
                val match = regexWithdraw.find(message)!!
                val (id, date, time, amount, recipient) = match.destructured
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

            regexReceived.find(message) != null -> {
                val match = regexReceived.find(message)!!
                val (id, amount, recipient, date, time) = match.destructured
                buildTransaction(
                    id,
                    amount,
                    recipient,
                    date,
                    time,
                    "income",
                    balance,
                    transactionCost
                )
            }

            regexReversal.find(message) != null -> {
                val match = regexReversal.find(message)!!
                val (id, recipient, date, time, amount) = match.destructured
                buildTransaction(
                    id,
                    amount,
                    "Reversal: $recipient",
                    date,
                    time,
                    "income",
                    balance,
                    transactionCost
                )
            }

            regexBalanceInquiry.find(message) != null -> {
                val match = regexBalanceInquiry.find(message)!!
                val (id, balAmount, date, time) = match.destructured
                buildTransaction(
                    id = id,
                    amountStr = "0.00", // No money moved in a balance check
                    recipient = "Balance Inquiry",
                    dateStr = date,
                    timeStr = time,
                    type = "neutral", // Use "neutral" so it doesn't affect Income/Expense totals
                    balance = balAmount.replace(",", "").toDouble(),
                    cost = transactionCost
                )
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
    val cleanRecipient = recipient.trim().removeSuffix(".")
    val cleanTime = timeStr.uppercase().trim().replace(".", "")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val standardizedTime = try {
        LocalTime.parse(cleanTime, timeFormatter).format(timeFormatter)
    } catch (e: Exception) {
        cleanTime
    }
    return MpesaTransactions(
        id = id.trim(),
        amount = amountStr.replace(",", "").toDouble(),
        transactionCost = cost,
        recipient = cleanRecipient,
        date = formattedDate,
        type = type,
        balance = balance,
        time = standardizedTime,
        timestamp = convertToTimestamp(formattedDate, standardizedTime),
        category = guessCategory(cleanRecipient, type)
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