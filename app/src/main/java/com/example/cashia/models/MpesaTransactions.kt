package com.example.cashia.models

import android.util.Log
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cashia.utility.PesaTimeConfiguration
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

// Handles: "KSH1.00 sent to Daraja-Sandbox for account S00002 on 6/3/26 at 9:35 AM"
// "for account <ref>" is optional — consumed before the date so it doesn't pollute recipient.
private val regexSentTo = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+sent\s+to\s+(.+?)(?:\s+for\s+account\s+\S+)?\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexPaidTo = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+paid\s+to\s+(.+?)(?:\s+for\s+account\s+\S+)?\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexAirtime = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.?\s*You\s+bought\s+[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+of\s+airtime\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexReceived = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*You\s+have\s+received\s+[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+from\s+(.+?)\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexWithdraw = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})\s*Withdraw\s+[Kk][Ss][Hh]([\d,]+\.\d{2})\s+from\s+(.+?)\s+New""",
    RegexOption.IGNORE_CASE
)

// Matches any of:
//   "New M-PESA balance is KSH12,673.66"       ← no space before amount
//   "New M-PESA account balance is Ksh 12,673.66"
//   "M-PESA account balance is Ksh12,673.66"
// \s* makes the space between KSH and digits optional.
private val balanceRegex = Regex(
    """(?:New\s+)?M-PESA\s+(?:account\s+)?balance\s+is\s+[Kk][Ss][Hh]\s*([\d,]+\.\d{2})""",
    RegexOption.IGNORE_CASE
)

private val costRegex =
    """(?:Transaction\s+cost|cost),?\s+[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)""".toRegex(RegexOption.IGNORE_CASE)

// ── Reversal v1 ───────────────────────────────────────────────────────────────
// "XYZ Confirmed. Your original transaction ABC in favour of John Doe has been
//  reversed successfully on 6/3/26 at 10:00 AM and Ksh1.00 has been credited"
private val regexReversal = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*Your\s+original\s+transaction\s+[A-Z0-9]+\s+in\s+favour\s+of\s+(.+?)\s+has\s+been\s+reversed\s+successfully\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})\s+and\s+[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+has\s+been\s+credited""",
    RegexOption.IGNORE_CASE
)

// ── Reversal v2 ───────────────────────────────────────────────────────────────
// "XYZ confirmed. Reversal of transaction ABC has been successfully reversed
//  on 6/3/26 at 10:00 AM and Ksh1.00 is credited to your M-PESA account.
//  New M-PESA account balance is Ksh12,674.66."
private val regexReversalV2 = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*Reversal\s+of\s+transaction\s+([A-Z0-9]+)\s+has\s+been\s+successfully\s+reversed\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})\s+and\s+[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+is\s+credited""",
    RegexOption.IGNORE_CASE
)

// Dedicated extractor for the new-balance line present in most modern M-Pesa messages
// Handles both "New M-PESA balance is KSH..." and "New M-PESA account balance is Ksh..."
private val regexNewBalance = Regex(
    """New\s+M-PESA\s+(?:account\s+)?balance\s+is\s+[Kk][Ss][Hh]\s*([\d,]+\.\d{2})""",
    RegexOption.IGNORE_CASE
)

@Entity(
    tableName = "MpesaTransactions", indices = [
        Index(value = ["timestamp"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["category"])
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
        // Use the dedicated new-balance extractor first (most reliable in modern messages),
        // then fall back to the generic balance regex for older formats.
        val balance = (regexNewBalance.find(message) ?: balanceRegex.find(message))
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        val transactionCost = costRegex.find(message)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        return when {
            regexSentTo.find(message) != null -> {
                val (id, amount, recipient, date, time) = regexSentTo.find(message)!!.destructured
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
                val (id, amount, recipient, date, time) = regexPaidTo.find(message)!!.destructured
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
                val (id, amount, date, time) = regexAirtime.find(message)!!.destructured
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
                val (id, date, time, amount, recipient) = regexWithdraw.find(message)!!.destructured
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
                val (id, amount, recipient, date, time) = regexReceived.find(message)!!.destructured
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

            // ── Reversal v1: "in favour of <name>" ────────────────────────
            regexReversal.find(message) != null -> {
                val (id, recipient, date, time, amount) = regexReversal.find(message)!!.destructured
                buildTransaction(
                    id,
                    amount,
                    "Reversal: $recipient",
                    date,
                    time,
                    "income",
                    balance,
                    transactionCost,
                    forcedCategory = TransactionCategory.RECEIVED.name
                )
            }

            // ── Reversal v2: "Reversal of transaction <txId>" ─────────────
            regexReversalV2.find(message) != null -> {
                val (id, reversedTxId, date, time, amount) = regexReversalV2.find(message)!!.destructured
                buildTransaction(
                    id,
                    amount,
                    "Reversal of $reversedTxId",
                    date,
                    time,
                    "income",
                    balance,
                    transactionCost,
                    forcedCategory = TransactionCategory.RECEIVED.name
                )
            }

            // ── Balance inquiry ────────────────────────────────────────────
            regexBalanceInquiry.find(message) != null -> {
                val (id, balAmount, date, time) = regexBalanceInquiry.find(message)!!.destructured
                buildTransaction(
                    id = id,
                    amountStr = "0.00",
                    recipient = "Balance Inquiry",
                    dateStr = date,
                    timeStr = time,
                    type = "neutral",
                    balance = balAmount.replace(",", "").toDouble(),
                    cost = transactionCost,
                    forcedCategory = TransactionCategory.BALANCE_INQUIRY.name
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
    cost: Double,
    forcedCategory: String? = null
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
        category = forcedCategory ?: guessCategory(cleanRecipient, type)
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