package com.example.cashia.models

import android.util.Log
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.Instant
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
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+sent\s+to\s+(.+?)(?:\s+for\s+account\s+\S+)?\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexPaidTo = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+paid\s+to\s+(.+?)(?:\s+for\s+account\s+\S+)?\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
    RegexOption.IGNORE_CASE
)
private val regexAirtime = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.?\s*You\s+bought\s+[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+of\s+airtime(?:\s+for\s+\S+)?\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})""",
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
private val costRegex =
    """(?:Transaction\s+cost|cost),?\s+[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)""".toRegex(RegexOption.IGNORE_CASE)
private val regexReversal = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*Your\s+original\s+transaction\s+[A-Z0-9]+\s+in\s+favour\s+of\s+(.+?)\s+has\s+been\s+reversed\s+successfully\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})\s+and\s+[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+has\s+been\s+credited""",
    RegexOption.IGNORE_CASE
)
private val regexReversalV2 = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*Reversal\s+of\s+transaction\s+([A-Z0-9]+)\s+has\s+been\s+successfully\s+reversed\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APM]{2})\s+and\s+[Kk][Ss][Hh]([\d,]+(?:\.\d{2})?)\s+is\s+credited""",
    RegexOption.IGNORE_CASE
)
private val regexNewBalance = Regex(
    """New\s+M-PESA\s+(?:account\s+)?balance\s+is\s+[Kk][Ss][Hh]\s*([\d,]+\.\d{2})""",
    RegexOption.IGNORE_CASE
)
private val balanceRegex = Regex(
    """(?:New\s+)?(?:M-PESA\s+)?(?:account\s+)?balance\s+is\s+[Kk][Ss][Hh]\s*([\d,]+\.\d{2})""",
    RegexOption.IGNORE_CASE
)
private val regexFulizaBorrow = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*Fuliza\s+M-PESA\s+amount\s+is\s+[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)\.\s*(?:Access\s+Fee|Interest)\s+charged\s+[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)\.\s*Total\s+Fuliza\s+M-PESA\s+outstanding\s+amount\s+is\s+[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)\s+due\s+on\s+(\d{1,2}/\d{1,2}/\d{2,4})""",
    RegexOption.IGNORE_CASE
)

private val regexFulizaRepay = Regex(
    """([A-Z0-9]+)\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)\s+from\s+your\s+M-PESA\s+has\s+been\s+used\s+to\s+(?:fully\s+)?pay\s+your\s+(?:outstanding\s+)?Fuliza\s+M-PESA""",
    RegexOption.IGNORE_CASE
)
private val regexConcurrentUnderway = Regex(
    "M-PESA is unable to process your request because a similar transaction is currently underway",
    RegexOption.IGNORE_CASE
)

private val regexFulizaLimit = Regex(
    "Dear .*, your Fuliza M-PESA limit is KSH [\\d,.]+",
    RegexOption.IGNORE_CASE
)

private val regexWrongPin = Regex(
    "Failed, you have entered the wrong PIN",
    RegexOption.IGNORE_CASE
)
private val regexCancelled = Regex(
    "You have cancelled the transaction of KSH[\\d,.]+\\. .* Your M-PESA balance is KSH[\\d,.]+",
    RegexOption.IGNORE_CASE
)
private val regexInsufficientFunds = Regex(
    "Failed\\. You do not have enough money in your M-PESA account to pay KSH[\\d,.]+ to .* Your M-PESA balance is KSH[\\d,.]+",
    RegexOption.IGNORE_CASE
)
private val regexInvalidInput = Regex(
    "Invalid input\\. To complete the transaction of KSH[\\d,.]+ please select 1 to SEND.*Your M-PESA balance is KSH[\\d,.]+",
    RegexOption.IGNORE_CASE
)
private val phoneRegex = Regex("""07\d{2}[*\d]{3}\d{3}""")

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
    val recipientName: String by lazy {
        recipient.replace(phoneRegex, "").trim()
    }

    @get:androidx.room.Ignore
    val recipientPhone: String? by lazy {
        phoneRegex.find(recipient)?.value?.trim()
    }

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

sealed class ParseResult {
    data class Success(val transactions: MpesaTransactions) : ParseResult()
    data class Unmatched(val rawBody: String) : ParseResult()
    object Irrelevant : ParseResult()
}

fun parseMpesaMessage(
    message: String,
    smsReceivedAt: Long = System.currentTimeMillis()
): ParseResult {
    val looksLikeMpesa = message.contains("Confirmed", ignoreCase = true) ||
            message.contains("M-PESA", ignoreCase = true) ||
            message.contains("HAKIKISHA", ignoreCase = true)
    if (!looksLikeMpesa) return ParseResult.Irrelevant

    val isSystemNoise = regexConcurrentUnderway.containsMatchIn(message) ||
            regexFulizaLimit.containsMatchIn(message) ||
            regexWrongPin.containsMatchIn(message) ||
            regexCancelled.containsMatchIn(message) ||      // New: Cancellation
            regexInsufficientFunds.containsMatchIn(message) ||
            regexInvalidInput.containsMatchIn(message)
    if (isSystemNoise) return ParseResult.Irrelevant

    return try {
        val balance = (regexNewBalance.find(message) ?: balanceRegex.find(message))
            ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val transactionCost = costRegex.find(message)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        when {
            regexSentTo.find(message) != null -> {
                val (id, amount, recipient, date, time) = regexSentTo.find(message)!!.destructured
                ParseResult.Success(
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
                )
            }

            regexPaidTo.find(message) != null -> {
                val (id, amount, recipient, date, time) = regexPaidTo.find(message)!!.destructured
                ParseResult.Success(
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
                )
            }

            regexAirtime.find(message) != null -> {
                val (id, amount, date, time) = regexAirtime.find(message)!!.destructured
                ParseResult.Success(
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
                )
            }

            regexWithdraw.find(message) != null -> {
                val (id, date, time, amount, recipient) = regexWithdraw.find(message)!!.destructured
                ParseResult.Success(
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
                )
            }

            regexReceived.find(message) != null -> {
                val (id, amount, recipient, date, time) = regexReceived.find(message)!!.destructured
                ParseResult.Success(
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
                )
            }

            // ── Reversal v1: "in favour of <name>" ────────────────────────
            regexReversal.find(message) != null -> {
                val (id, recipient, date, time, amount) = regexReversal.find(message)!!.destructured
                ParseResult.Success(
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
                )
            }

            // ── Reversal v2: "Reversal of transaction <txId>" ─────────────
            regexReversalV2.find(message) != null -> {
                val (id, reversedTxId, date, time, amount) = regexReversalV2.find(message)!!.destructured
                ParseResult.Success(
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
                )
            }

            // ── Balance inquiry ────────────────────────────────────────────
            regexBalanceInquiry.find(message) != null -> {
                val (id, balAmount, date, time) = regexBalanceInquiry.find(message)!!.destructured
                ParseResult.Success(
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
                )
            }

            regexFulizaBorrow.find(message) != null -> {
                val match = regexFulizaBorrow.find(message)!!
                val (id, amount, fee, _, dueDate) = match.destructured

                val txDate = Instant.ofEpochMilli(smsReceivedAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate().toString()
                val txTime = Instant.ofEpochMilli(smsReceivedAt)
                    .atZone(ZoneId.systemDefault()).toLocalTime()
                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

                val datePattern = if (dueDate.length <= 8) "d/M/yy" else "d/M/yyyy"
                val dueDateFormatted = try {
                    LocalDate.parse(dueDate, DateTimeFormatter.ofPattern(datePattern))
                        .format(DateTimeFormatter.ofPattern("d MMM yy"))
                } catch (e: Exception) {
                    dueDate
                }

                ParseResult.Success(
                    MpesaTransactions(
                        id = id.trim(),
                        amount = amount.replace(",", "").toDouble(),
                        transactionCost = fee.replace(",", "").toDouble(),
                        recipient = "Fuliza M-PESA (due $dueDateFormatted)",
                        date = txDate,
                        type = "expense",       // ← was "income", corrected: it's debt taken on, money OUT eventually
                        balance = 0.0,          // ← don't store debt here; it would corrupt latestBalance
                        time = txTime,
                        timestamp = smsReceivedAt,
                        category = TransactionCategory.FULIZA_LOAN.name
                    )
                )
            }

            regexFulizaRepay.find(message) != null -> {
                val match = regexFulizaRepay.find(message)!!
                val (id, amount) = match.destructured

                // Same fix: use actual SMS received time, not LocalDate.now()
                val txDate = Instant.ofEpochMilli(smsReceivedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
                val txTime = Instant.ofEpochMilli(smsReceivedAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime()
                    .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

                ParseResult.Success(
                    MpesaTransactions(
                        id = id.trim(),
                        amount = amount.replace(",", "").toDouble(),
                        transactionCost = transactionCost,
                        recipient = "Fuliza Repayment",
                        date = txDate,
                        type = "expense",
                        balance = balance,
                        time = txTime,
                        timestamp = smsReceivedAt,
                        category = TransactionCategory.FULIZA_REPAYMENT.name
                    )
                )
            }

            else -> ParseResult.Unmatched(message)
        }
    } catch (e: Exception) {
        Log.e("Transactions", "Parsing error: ${e.message}")
        ParseResult.Unmatched(message)
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