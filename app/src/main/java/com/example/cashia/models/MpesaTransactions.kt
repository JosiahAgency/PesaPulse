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

// ─────────────────────────────────────────────────────────────────────────────
// Regex patterns — compiled once at class load, reused for every message.
// ─────────────────────────────────────────────────────────────────────────────

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
private val costRegex = Regex(
    """(?:Transaction\s+cost|cost),?\s+[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)""",
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

// ── Noise / system-error patterns ─────────────────────────────────────────────
private val regexConcurrentUnderway = Regex(
    "M-PESA is unable to process your request because a similar transaction is currently underway",
    RegexOption.IGNORE_CASE
)
private val regexFulizaLimit =
    Regex("Dear .*, your Fuliza M-PESA limit is KSH[\\d,.]+", RegexOption.IGNORE_CASE)
private val regexWrongPin = Regex("Failed, you have entered the wrong PIN", RegexOption.IGNORE_CASE)
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

// ─────────────────────────────────────────────────────────────────────────────

data class DayGroup(
    val date: LocalDate,
    val transactions: List<MpesaTransactions>,
    val dayIncome: Double,
    val dayExpense: Double,
    val dayBalance: Double
)

data class WeekGroup(val bucket: String, val days: List<DayGroup>)

@Entity(
    tableName = "MpesaTransactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["date"]),
        Index(value = ["type"]),
        Index(value = ["category"]),
        Index(value = ["smsId"], unique = true),
        Index(value = ["reversedTxId"])
    ]
)
data class MpesaTransactions(
    @PrimaryKey
    val id: String,
    val smsId: Long = 0L,
    val amount: Double,
    val transactionCost: Double,
    val recipient: String,
    val date: String,
    val type: String,
    val balance: Double,
    val time: String,
    val timestamp: Long,
    val category: String = TransactionCategory.OTHER.name,
    val reversedTxId: String? = null,
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
    val displayTime: String
        get() = try {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("h:mm a", Locale.US))
                .format(PesaTimeConfiguration.getTimeFormatter())
        } catch (_: Exception) {
            time
        }

    @get:androidx.room.Ignore
    val displayDate: String by lazy {
        try {
            LocalDate.parse(date).format(PesaTimeConfiguration.getDateFormatter())
        } catch (_: Exception) {
            date
        }
    }

    @get:androidx.room.Ignore
    val isIncome: Boolean get() = type == "income"

    @get:androidx.room.Ignore
    val isExpense: Boolean get() = type == "expense"

    /** Net impact on balance: positive for income, negative for expenses (incl. cost). */
    @get:androidx.room.Ignore
    val netAmount: Double get() = if (isIncome) amount else -(amount + transactionCost)
}

sealed class ParseResult {
    data class Success(val transactions: MpesaTransactions) : ParseResult()
    data class Unmatched(val rawBody: String) : ParseResult()
    object Irrelevant : ParseResult()
}

// ─────────────────────────────────────────────────────────────────────────────
// Parser
// ─────────────────────────────────────────────────────────────────────────────

fun parseMpesaMessage(
    message: String,
    smsId: Long,
    @Suppress("UNUSED_PARAMETER") smsReceivedAt: Long = System.currentTimeMillis()
): ParseResult {

    val looksLikeMpesa = message.contains("Confirmed", ignoreCase = true) ||
            message.contains("M-PESA", ignoreCase = true) ||
            message.contains("HAKIKISHA", ignoreCase = true)
    if (!looksLikeMpesa) return ParseResult.Irrelevant

    val isSystemNoise = regexConcurrentUnderway.containsMatchIn(message) ||
            regexFulizaLimit.containsMatchIn(message) ||
            regexWrongPin.containsMatchIn(message) ||
            regexCancelled.containsMatchIn(message) ||
            regexInsufficientFunds.containsMatchIn(message) ||
            regexInvalidInput.containsMatchIn(message)
    if (isSystemNoise) return ParseResult.Irrelevant

    return try {
        // [PERF] Balance and cost are extracted once, lazily, and shared across
        // whichever branch matches. The original code re-called find() twice per
        // match (once for the null-check, once for destructuring) and rebuilt the
        // extractBalanceAndCost lambda on every call.
        val balance by lazy {
            (regexNewBalance.find(message) ?: balanceRegex.find(message))
                ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }
        val txCost by lazy {
            costRegex.find(message)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }

        // [FIX] Store each match result in a local val so find() is called only
        // once per pattern. Previously: `regexX.find(msg) != null` then
        // `regexX.find(msg)!!.destructured` → 2× regex evaluation per branch.
        regexSentTo.find(message)?.let { m ->
            val (id, amount, recipient, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    recipient,
                    date,
                    time,
                    "expense",
                    balance,
                    txCost
                )
            )
        }

        regexPaidTo.find(message)?.let { m ->
            val (id, amount, recipient, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    recipient,
                    date,
                    time,
                    "expense",
                    balance,
                    txCost
                )
            )
        }

        regexAirtime.find(message)?.let { m ->
            val (id, amount, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    "Safaricom Airtime",
                    date,
                    time,
                    "expense",
                    balance,
                    txCost
                )
            )
        }

        regexWithdraw.find(message)?.let { m ->
            val (id, date, time, amount, recipient) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    recipient,
                    date,
                    time,
                    "expense",
                    balance,
                    txCost
                )
            )
        }

        regexReceived.find(message)?.let { m ->
            val (id, amount, recipient, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    recipient,
                    date,
                    time,
                    "income",
                    balance,
                    txCost
                )
            )
        }

        regexReversal.find(message)?.let { m ->
            val (id, recipient, date, time, amount) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    "Reversal: $recipient",
                    date,
                    time,
                    "income",
                    balance,
                    txCost,
                    forcedCategory = TransactionCategory.RECEIVED_MONEY.name
                )
            )
        }

        regexReversalV2.find(message)?.let { m ->
            val (id, reversedTxId, date, time, amount) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    "Reversal of $reversedTxId",
                    date,
                    time,
                    "income",
                    balance,
                    txCost,
                    forcedCategory = TransactionCategory.RECEIVED_MONEY.name,
                    reversedTxId = reversedTxId.trim()
                )
            )
        }

        regexBalanceInquiry.find(message)?.let { m ->
            val (id, balAmount, date, time) = m.destructured
            val inquiryCost = costRegex.find(message)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, "0.00", "Balance Inquiry", date, time, "neutral",
                    balance = balAmount.replace(",", "").toDouble(),
                    cost = inquiryCost,
                    forcedCategory = TransactionCategory.BALANCE_INQUIRY.name
                )
            )
        }

        ParseResult.Unmatched(message)

    } catch (e: Exception) {
        Log.e("Transactions", "Parsing error: ${e.message}")
        ParseResult.Unmatched(message)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Builder
// ─────────────────────────────────────────────────────────────────────────────

fun buildTransaction(
    smsId: Long,
    id: String,
    amountStr: String,
    recipient: String,
    dateStr: String,
    timeStr: String,
    type: String,
    balance: Double,
    cost: Double,
    forcedCategory: String? = null,
    reversedTxId: String? = null
): MpesaTransactions {
    val pattern = if (dateStr.length <= 8) "d/M/yy" else "d/M/yyyy"
    val formattedDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern)).toString()
    val cleanRecipient = recipient.trim().removeSuffix(".")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    val cleanTime = timeStr.uppercase().trim().replace(".", "")
    val standardizedTime = try {
        LocalTime.parse(cleanTime, timeFormatter).format(timeFormatter)
    } catch (_: Exception) {
        cleanTime
    }

    return MpesaTransactions(
        id = id.trim(),
        smsId = smsId,
        amount = amountStr.replace(",", "").toDouble(),
        transactionCost = cost,
        recipient = cleanRecipient,
        date = formattedDate,
        type = type,
        balance = balance,
        time = standardizedTime,
        timestamp = convertToTimestamp(formattedDate, standardizedTime),
        category = forcedCategory ?: guessCategory(cleanRecipient, type),
        reversedTxId = reversedTxId
    )
}

fun convertToTimestamp(dateStr: String, timeStr: String): Long = try {
    LocalDateTime.parse(
        "$dateStr $timeStr",
        DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm a", Locale.US)
    ).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
} catch (_: Exception) {
    System.currentTimeMillis()
}