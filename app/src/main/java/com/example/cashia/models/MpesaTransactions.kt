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
//
// Coverage map (✓ = was present, ★ = new/improved):
//   ✓ Balance inquiry (account balance was)
//   ★ Balance inquiry alt (balance was, no "account" word)
//   ✓ Sent to person / paybill
//   ✓ Paid to (buy goods / paybill)
//   ★ Explicitly separate Buy Goods (no "for account") for better categorisation
//   ✓ Airtime purchase
//   ✓ Received from person / B2C
//   ✓ Withdraw at agent (compact format)
//   ★ Withdraw at agent (multi-line / spaced format)
//   ★ Agent deposit (cash-in at agent)
//   ★ M-Shwari transfer TO
//   ★ M-Shwari transfer FROM
//   ★ M-Shwari loan received into M-PESA
//   ★ KCB M-PESA transfer TO
//   ★ KCB M-PESA transfer FROM / loan received
//   ★ Hustler Fund loan received
//   ★ Buy Goods / Pay Shop refund
//   ★ Short-form system reversal ("Transaction X has been reversed")
//   ✓ Reversal v1 (in favour of …)
//   ✓ Reversal v2 (Reversal of transaction …)
//   ✓ Fuliza borrow
//   ✓ Fuliza repay
//   ★ Noise: failed airtime purchase
//   ★ Noise: invalid/wrong phone number
//   ✓ Noise: concurrent transaction underway
//   ✓ Noise: Fuliza limit notification
//   ✓ Noise: wrong PIN
//   ✓ Noise: cancelled transaction
//   ✓ Noise: insufficient funds
//   ✓ Noise: invalid input
// ─────────────────────────────────────────────────────────────────────────────

// ── Amount / date / time building blocks (shared across patterns) ─────────────
private const val AMT = """([\d,]+(?:\.\d{2})?)"""
private const val DATE = """(\d{1,2}/\d{1,2}/\d{2,4})"""
private const val TIME = """(\d{1,2}:\d{2}\s*[APM]{2})"""
private const val TXID = """([A-Z0-9]+)"""

// ── Balance & cost helpers ────────────────────────────────────────────────────

/** Matches "New M-PESA [account] balance is KshX" */
private val regexNewBalance = Regex(
    """New\s+M-PESA\s+(?:account\s+)?balance\s+is\s+[Kk][Ss][Hh]\s*([\d,]+\.\d{2})""",
    RegexOption.IGNORE_CASE
)

/** Broader fallback — also catches "M-PESA balance is", "account balance is" etc. */
private val balanceRegex = Regex(
    """(?:New\s+)?(?:M-PESA\s+)?(?:account\s+)?balance\s+is\s+[Kk][Ss][Hh]\s*([\d,]+\.\d{2})""",
    RegexOption.IGNORE_CASE
)

/** "Transaction cost[,] Ksh X" — comma is optional */
private val costRegex = Regex(
    """(?:Transaction\s+cost|cost),?\s+[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)""",
    RegexOption.IGNORE_CASE
)

// ── Balance inquiry ───────────────────────────────────────────────────────────

/** Original: "… Your account balance was: M-PESA Account : KshX on D/M/YY at H:MM AM" */
private val regexBalanceInquiry = Regex(
    """$TXID\s+Confirmed\.\s+Your\s+account\s+balance\s+was:\s+M-PESA\s+Account\s+:\s+[Kk][Ss][Hh]$AMT\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

/** Alternate: "… Your M-PESA balance was KshX on D/M/YY at H:MM AM" */
private val regexBalanceInquiryAlt = Regex(
    """$TXID\s+Confirmed\.\s+Your\s+(?:M-PESA\s+)?balance\s+was\s+[Kk][Ss][Hh]$AMT\s+on\s+$DATE(?:\s+at\s+$TIME)?""",
    RegexOption.IGNORE_CASE
)

// ── Outgoing: Send Money ──────────────────────────────────────────────────────

/**
 * "KshX sent to NAME [PHONE] [for account ACCT] on D/M/YY at H:MM AM"
 * Groups: txId, amount, recipient (name + optional phone), [account], date, time
 */
private val regexSentTo = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]$AMT\s+sent\s+to\s+(.+?)(?:\s+for\s+account\s+\S+)?\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

// ── Outgoing: Pay Bill ────────────────────────────────────────────────────────

/**
 * "KshX paid to NAME for account ACCT on …"  — Pay Bill (has "for account")
 * The account number is captured so we can store it if needed.
 */
private val regexPayBill = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]$AMT\s+paid\s+to\s+(.+?)\s+for\s+account\s+(\S+)\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

/**
 * "KshX paid to NAME on …" — Buy Goods / Lipa na M-PESA (no "for account")
 */
private val regexBuyGoods = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]$AMT\s+paid\s+to\s+(.+?)\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

// ── Outgoing: Airtime ─────────────────────────────────────────────────────────

/** "You bought KshX of airtime [for 07XX…] on D/M/YY at H:MM AM" */
private val regexAirtime = Regex(
    """$TXID\s+[Cc]onfirmed\.?\s*You\s+bought\s+[Kk][Ss][Hh]$AMT\s+of\s+airtime(?:\s+for\s+\S+)?\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

// ── Incoming: Received from person / B2C ─────────────────────────────────────

/** "You have received KshX from NAME [PHONE] on D/M/YY at H:MM AM" */
private val regexReceived = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*You\s+have\s+received\s+[Kk][Ss][Hh]$AMT\s+from\s+(.+?)\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

// ── Outgoing: Withdraw at agent ───────────────────────────────────────────────

/**
 * Original compact format (no space after "Confirmed."):
 * "XConfirmed.on D/M/YY at H:MM AM Withdraw KshX from AGENT New"
 */
private val regexWithdraw = Regex(
    """$TXID\s+[Cc]onfirmed\.on\s+$DATE\s+at\s+$TIME\s*Withdraw\s+[Kk][Ss][Hh]$AMT\s+from\s+(.+?)\s+New""",
    RegexOption.IGNORE_CASE
)

/**
 * ★ Newer / spaced format seen in PesaPi examples:
 * "X Confirmed.\non D/M/YY at H:MM AM\nWithdraw KshX from\nAGENT NAME\nNew M-PESA balance…"
 * Also catches single-line variants produced by SMS aggregators.
 */
private val regexWithdrawV2 = Regex(
    """$TXID\s+[Cc]onfirmed\.\s+on\s+$DATE\s+at\s+$TIME\s+Withdraw\s+[Kk][Ss][Hh]$AMT\s+from\s+(.+?)(?:\s+New\s+M-PESA|\s+New\s+account)""",
    RegexOption.IGNORE_CASE
)

// ── Incoming: Agent deposit (cash-in) ────────────────────────────────────────

/**
 * ★ "X Confirmed.\non D/M/YY at H:MM AM\nGive KshX cash to AGENT NAME\nNew M-PESA balance…"
 * The agent gives the customer's money back as M-PESA credit — this is an INCOME event.
 */
private val regexAgentDeposit = Regex(
    """$TXID\s+[Cc]onfirmed\.\s+on\s+$DATE\s+at\s+$TIME\s+Give\s+[Kk][Ss][Hh]$AMT\s+cash\s+to\s+(.+?)(?:\s+New\s+M-PESA|\s+New\s+account)""",
    RegexOption.IGNORE_CASE
)

/**
 * ★ Modern deposit: "KshX deposited to your M-PESA account by AGENT on D/M/YY at H:MM AM"
 */
private val regexAgentDepositV2 = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]$AMT\s+deposited\s+to\s+your\s+M-PESA\s+account\s+(?:by\s+(.+?)\s+)?on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

// ── M-Shwari transfers ────────────────────────────────────────────────────────

/**
 * ★ Transfer TO M-Shwari savings:
 * "X Confirmed. KshY transferred to M-Shwari account on D/M/YY at H:MM AM.
 *  M-PESA balance is KshZ, new M-Shwari account balance is KshW."
 */
private val regexMshwariTo = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]$AMT\s+transferred\s+to\s+M-Shwari\s+account\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

/**
 * ★ Transfer FROM M-Shwari back to M-PESA:
 * "X Confirmed. You have transferred KshY from your M-Shwari account on D/M/YY at H:MM AM.
 *  M-Shwari balance is KshZ. M-PESA balance is KshW."
 */
private val regexMshwariFrom = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*You\s+have\s+transferred\s+[Kk][Ss][Hh]$AMT\s+from\s+your\s+M-Shwari\s+account\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

/**
 * ★ M-Shwari loan disbursed into M-PESA:
 * "X Confirmed. KshY M-Shwari loan has been sent to your M-PESA account on D/M/YY at H:MM AM"
 * Also matches: "… M-Shwari loan of KshY credited to your M-PESA …"
 */
private val regexMshwariLoan = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]$AMT\s+M-Shwari\s+loan\s+(?:has\s+been\s+sent|credited)\s+to\s+your\s+M-PESA(?:\s+account)?\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

// ── KCB M-PESA transfers ──────────────────────────────────────────────────────

/**
 * ★ Transfer TO KCB M-PESA account:
 * "X Confirmed. KshY transferred to KCB M-PESA account on D/M/YY at H:MM AM"
 */
private val regexKcbTo = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]$AMT\s+transferred\s+to\s+KCB\s+M-PESA\s+account\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

/**
 * ★ Transfer FROM KCB M-PESA / loan received:
 * "X Confirmed. You have transferred KshY from your KCB M-PESA account on D/M/YY at H:MM AM"
 * and: "X Confirmed. KshY KCB M-PESA loan sent to your M-PESA on D/M/YY at H:MM AM"
 */
private val regexKcbFrom = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*(?:You\s+have\s+transferred\s+[Kk][Ss][Hh]$AMT\s+from\s+your\s+KCB\s+M-PESA\s+account|[Kk][Ss][Hh]$AMT\s+KCB\s+M-PESA\s+loan\s+(?:has\s+been\s+)?sent\s+to\s+your\s+M-PESA(?:\s+account)?)\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

// ── Hustler Fund ──────────────────────────────────────────────────────────────

/**
 * ★ Hustler Fund loan disbursed:
 * "X Confirmed. You have received KshY Hustler Fund loan on D/M/YY at H:MM AM"
 * or: "X Confirmed. KshY from Hustler Fund has been credited … on D/M/YY at H:MM AM"
 */
private val regexHustlerFund = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*(?:You\s+have\s+received\s+[Kk][Ss][Hh]$AMT\s+Hustler\s+Fund\s+loan|[Kk][Ss][Hh]$AMT\s+from\s+(?:the\s+)?Hustler\s+Fund\s+has\s+been\s+credited(?:\s+to\s+your\s+M-PESA(?:\s+account)?)?)\s+on\s+$DATE\s+at\s+$TIME""",
    RegexOption.IGNORE_CASE
)

// ── Reversals ─────────────────────────────────────────────────────────────────

private val regexReversal = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*Your\s+original\s+transaction\s+[A-Z0-9]+\s+in\s+favour\s+of\s+(.+?)\s+has\s+been\s+reversed\s+successfully\s+on\s+$DATE\s+at\s+$TIME\s+and\s+[Kk][Ss][Hh]$AMT\s+has\s+been\s+credited""",
    RegexOption.IGNORE_CASE
)

private val regexReversalV2 = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*Reversal\s+of\s+transaction\s+([A-Z0-9]+)\s+has\s+been\s+successfully\s+reversed\s+on\s+$DATE\s+at\s+$TIME\s+and\s+[Kk][Ss][Hh]$AMT\s+is\s+credited""",
    RegexOption.IGNORE_CASE
)

/**
 * ★ Short-form system reversal (incoming, no amount in body, balance updated):
 * "X Confirmed. Transaction Y has been reversed. Your account balance is now KshZ."
 * We treat this as an income event since money is returned; the amount is extracted from
 * the balance-change context (best-effort — stored as 0 if unavailable).
 */
private val regexReversalShort = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*Transaction\s+([A-Z0-9]+)\s+has\s+been\s+reversed\.\s*Your\s+account\s+balance\s+is\s+now\s+[Kk][Ss][Hh]\s*([\d,]+\.\d{2})""",
    RegexOption.IGNORE_CASE
)

// ── Buy Goods / Pay Shop refund ───────────────────────────────────────────────

/**
 * ★ "X confirmed. Your Pay Shop transaction Y of KshZ has been refunded by MERCHANT."
 */
private val regexBuyGoodsRefund = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*Your\s+Pay\s+Shop\s+transaction\s+[A-Z0-9]+\s+of\s+(?:[Kk][Ss][Hh]\s*)?$AMT\s*[Kk][Ss][Hh]?\s*has\s+been\s+refunded\s+by\s+(.+?)[\.\s]*(?:Your\s+account)?""",
    RegexOption.IGNORE_CASE
)

// ── Fuliza ────────────────────────────────────────────────────────────────────

private val regexFulizaBorrow = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*Fuliza\s+M-PESA\s+amount\s+is\s+[Kk][Ss][Hh]\s*$AMT\.\s*(?:Access\s+Fee|Interest)\s+charged\s+[Kk][Ss][Hh]\s*$AMT\.\s*Total\s+Fuliza\s+M-PESA\s+outstanding\s+amount\s+is\s+[Kk][Ss][Hh]\s*$AMT\s+due\s+on\s+$DATE""",
    RegexOption.IGNORE_CASE
)

private val regexFulizaRepay = Regex(
    """$TXID\s+[Cc]onfirmed\.\s*[Kk][Ss][Hh]\s*$AMT\s+from\s+your\s+M-PESA\s+has\s+been\s+used\s+to\s+(?:fully\s+)?pay\s+your\s+(?:outstanding\s+)?Fuliza\s+M-PESA""",
    RegexOption.IGNORE_CASE
)

// ── Noise / system-error patterns ─────────────────────────────────────────────

private val regexConcurrentUnderway = Regex(
    "M-PESA is unable to process your request because a similar transaction is currently underway",
    RegexOption.IGNORE_CASE
)
private val regexFulizaLimit = Regex(
    """Dear\s+.+?,\s+your\s+Fuliza\s+M-PESA\s+limit\s+is\s+KSH\s*([\d,]+(?:\.\d{2})?)""",
    RegexOption.IGNORE_CASE
)
private val regexWrongPin =
    Regex("Failed, you have entered the wrong PIN", RegexOption.IGNORE_CASE)
private val regexCancelled = Regex(
    """You have cancelled the transaction of KSH[\d,.]+\. .* Your M-PESA balance is KSH[\d,.]+""",
    RegexOption.IGNORE_CASE
)
private val regexInsufficientFunds = Regex(
    """Failed\. You do not have enough money in your M-PESA account to pay KSH[\d,.]+ to .* Your M-PESA balance is KSH[\d,.]+""",
    RegexOption.IGNORE_CASE
)
private val regexInvalidInput = Regex(
    """Invalid input\. To complete the transaction of KSH[\d,.]+ please select 1 to SEND.*Your M-PESA balance is KSH[\d,.]+""",
    RegexOption.IGNORE_CASE
)

/** ★ "Failed. M-PESA is temporarily unable to authorise airtime purchase of KshX." */
private val regexFailedAirtime = Regex(
    """Failed\.\s+M-PESA\s+is\s+temporarily\s+unable\s+to\s+authorise\s+airtime\s+purchase""",
    RegexOption.IGNORE_CASE
)

/** ★ "Failed. The entered phone number is incorrect 07XX…" */
private val regexInvalidPhone = Regex(
    """Failed\.\s+The\s+entered\s+phone\s+number\s+is\s+incorrect""",
    RegexOption.IGNORE_CASE
)
/**
 * ★ Fuliza limit notification (already had a pattern but this is the exact real-world format):
 * "Dear GEORGE, your Fuliza M-PESA limit is KSH 1300.00 .Transact normally…"
 * The existing regexFulizaLimit covers this, but we keep this alias for readability/docs.
 * No new Regex needed — regexFulizaLimit already matches.
 */

/**
 * ★ Insufficient funds + Fuliza upsell prompt:
 * "Insufficient funds in your M-PESA account for this transaction, to register for
 *  Fuliza M-PESA service, Dial *334#OK"
 * No money moved — pure informational noise.
 */
private val regexInsufficientFulizaPrompt = Regex(
    """Insufficient\s+funds\s+in\s+your\s+M-PESA\s+account\s+for\s+this\s+transaction""",
    RegexOption.IGNORE_CASE
)

// ── Utility ───────────────────────────────────────────────────────────────────

private val phoneRegex = Regex("""(?:254|\+254|0)7\d{2}[*\d]{3}\d{3}""")

// ─────────────────────────────────────────────────────────────────────────────
// Domain models
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
            message.contains("M-Shwari", ignoreCase = true) ||
            message.contains("Fuliza", ignoreCase = true) ||
            message.contains("HAKIKISHA", ignoreCase = true) ||
            message.contains("Hustler Fund", ignoreCase = true)
    if (!looksLikeMpesa) return ParseResult.Irrelevant

    val isSystemNoise =
        regexConcurrentUnderway.containsMatchIn(message) ||
                regexWrongPin.containsMatchIn(message) ||
                regexCancelled.containsMatchIn(message) ||
                regexInsufficientFunds.containsMatchIn(message) ||
                regexInvalidInput.containsMatchIn(message) ||
                regexFailedAirtime.containsMatchIn(message) ||
                regexInvalidPhone.containsMatchIn(message) ||
                regexInsufficientFulizaPrompt.containsMatchIn(message)    // "Insufficient funds… Dial *334#"
    if (isSystemNoise) return ParseResult.Irrelevant

    return try {
        // Balance and cost extracted once, lazily, shared across whichever branch matches.
        val balance by lazy {
            (regexNewBalance.find(message) ?: balanceRegex.find(message))
                ?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }
        val txCost by lazy {
            costRegex.find(message)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }

        // ── Outgoing ───────────────────────────────────────────────────────

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

        // Pay Bill (has "for account" — checked before generic buyGoods)
        regexPayBill.find(message)?.let { m ->
            val (id, amount, recipient, account, date, time) = m.destructured
            val label = if (account.isNotBlank()) "$recipient (Acc: $account)" else recipient
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, label, date, time, "expense", balance, txCost,
                    forcedCategory = TransactionCategory.PAYBILL.name
                )
            )
        }

        // Buy Goods (no "for account")
        regexBuyGoods.find(message)?.let { m ->
            val (id, amount, recipient, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, recipient, date, time, "expense", balance, txCost,
                    forcedCategory = TransactionCategory.BUY_GOODS.name
                )
            )
        }

        regexAirtime.find(message)?.let { m ->
            val (id, amount, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "Safaricom Airtime", date, time, "expense", balance, txCost,
                    forcedCategory = TransactionCategory.AIRTIME.name
                )
            )
        }

        // Withdraw v1 (compact, no space after "Confirmed.")
        regexWithdraw.find(message)?.let { m ->
            val (id, date, time, amount, recipient) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, recipient, date, time, "expense", balance, txCost,
                    forcedCategory = TransactionCategory.WITHDRAWAL.name
                )
            )
        }

        // ★ Withdraw v2 (spaced / multi-line format)
        regexWithdrawV2.find(message)?.let { m ->
            val (id, date, time, amount, recipient) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, recipient, date, time, "expense", balance, txCost,
                    forcedCategory = TransactionCategory.WITHDRAWAL.name
                )
            )
        }

        // ★ M-Shwari TO (expense — money leaves M-PESA wallet)
        regexMshwariTo.find(message)?.let { m ->
            val (id, amount, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "M-Shwari Account", date, time, "expense", balance, txCost,
                    forcedCategory = TransactionCategory.MSHWARI.name
                )
            )
        }

        // ★ KCB M-PESA TO (expense — money leaves M-PESA wallet)
        regexKcbTo.find(message)?.let { m ->
            val (id, amount, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "KCB M-PESA Account", date, time, "expense", balance, txCost,
                    forcedCategory = TransactionCategory.KCB_MPESA.name
                )
            )
        }

        // ★ Fuliza repay (expense)
        regexFulizaRepay.find(message)?.let { m ->
            val (id, amount) = m.destructured
            val (fallbackDate, fallbackTime) = smsReceivedAt.toDateTimePair()
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "Fuliza M-PESA Repayment",
                    fallbackDate, fallbackTime, "expense", balance, 0.0,
                    forcedCategory = TransactionCategory.FULIZA.name
                )
            )
        }

        // ── Incoming ───────────────────────────────────────────────────────

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

        // ★ Agent deposit v1 (Give cash format)
        regexAgentDeposit.find(message)?.let { m ->
            val (id, date, time, amount, agent) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    "Deposited via $agent",
                    date,
                    time,
                    "income",
                    balance,
                    txCost,
                    forcedCategory = TransactionCategory.AGENT_DEPOSIT.name
                )
            )
        }

        // ★ Agent deposit v2 (deposited to account format)
        regexAgentDepositV2.find(message)?.let { m ->
            val (id, amount, agent, date, time) = m.destructured
            val agentLabel = if (agent.isNotBlank()) "Deposited via $agent" else "Agent Deposit"
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, agentLabel, date, time, "income", balance, txCost,
                    forcedCategory = TransactionCategory.AGENT_DEPOSIT.name
                )
            )
        }

        // ★ M-Shwari FROM (income — money returns to M-PESA wallet)
        regexMshwariFrom.find(message)?.let { m ->
            val (id, amount, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "M-Shwari Withdrawal", date, time, "income", balance, txCost,
                    forcedCategory = TransactionCategory.MSHWARI.name
                )
            )
        }

        // ★ M-Shwari loan disbursed
        regexMshwariLoan.find(message)?.let { m ->
            val (id, amount, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "M-Shwari Loan", date, time, "income", balance, 0.0,
                    forcedCategory = TransactionCategory.MSHWARI.name
                )
            )
        }

        // ★ KCB M-PESA FROM / loan received
        regexKcbFrom.find(message)?.let { m ->
            // Group 2 OR group 3 holds the amount (two alternates in the regex)
            val id = m.groupValues[1]
            val amount = m.groupValues[2].ifBlank { m.groupValues[3] }
            val date = m.groupValues[4]
            val time = m.groupValues[5]
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "KCB M-PESA", date, time, "income", balance, txCost,
                    forcedCategory = TransactionCategory.KCB_MPESA.name
                )
            )
        }

        // ★ Hustler Fund loan
        regexHustlerFund.find(message)?.let { m ->
            val id = m.groupValues[1]
            val amount = m.groupValues[2].ifBlank { m.groupValues[3] }
            val date = m.groupValues[4]
            val time = m.groupValues[5]
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "Hustler Fund Loan", date, time, "income", balance, 0.0,
                    forcedCategory = TransactionCategory.HUSTLER_FUND.name
                )
            )
        }

        // ★ Buy Goods refund
        regexBuyGoodsRefund.find(message)?.let { m ->
            val (id, amount, merchant) = m.destructured
            val (fallbackDate, fallbackTime) = smsReceivedAt.toDateTimePair()
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, amount, "Refund: $merchant",
                    fallbackDate, fallbackTime, "income", balance, 0.0,
                    forcedCategory = TransactionCategory.RECEIVED_MONEY.name
                )
            )
        }

        // ★ Fuliza borrow (neutral — just records overdraft usage)
        regexFulizaBorrow.find(message)?.let { m ->
            val (id, amount, fee, _, dueDate) = m.destructured
            // Use due date as the date, time unknown
            val fallbackTime =
                LocalTime.NOON.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
            return ParseResult.Success(
                buildTransaction(
                    smsId,
                    id,
                    amount,
                    "Fuliza M-PESA (due $dueDate)",
                    dueDate,
                    fallbackTime,
                    "expense",
                    balance,
                    fee.replace(",", "").toDoubleOrNull() ?: 0.0,
                    forcedCategory = TransactionCategory.FULIZA.name
                )
            )
        }

        regexFulizaLimit.find(message)?.let { m ->
            val limitAmt = m.groupValues[1].replace(",", "")
            val (fallbackDate, fallbackTime) = smsReceivedAt.toDateTimePair()

            val generatedId = "FLM-$smsId"

            return ParseResult.Success(
                buildTransaction(
                    smsId = smsId,
                    id = generatedId,
                    amountStr = limitAmt,
                    recipient = "Fuliza Limit Update",
                    dateStr = fallbackDate,
                    timeStr = fallbackTime,
                    type = "neutral", // Classified as neutral so it doesn't skew arithmetic dashboards
                    balance = balance,
                    cost = 0.0,
                    forcedCategory = TransactionCategory.FULIZA.name
                )
            )
        }

        // Reversal v1
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

        // Reversal v2
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

        // ★ Reversal short form
        regexReversalShort.find(message)?.let { m ->
            val (id, reversedTxId, newBalance) = m.destructured
            val parsedBalance = newBalance.replace(",", "").toDoubleOrNull() ?: balance
            val (fallbackDate, fallbackTime) = smsReceivedAt.toDateTimePair()
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, "0.00", "Reversal of $reversedTxId",
                    fallbackDate, fallbackTime, "income", parsedBalance, 0.0,
                    forcedCategory = TransactionCategory.RECEIVED_MONEY.name,
                    reversedTxId = reversedTxId.trim()
                )
            )
        }

        // ── Neutral ────────────────────────────────────────────────────────

        regexBalanceInquiry.find(message)?.let { m ->
            val (id, balAmount, date, time) = m.destructured
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, "0.00", "Balance Inquiry", date, time, "neutral",
                    balance = balAmount.replace(",", "").toDouble(),
                    cost = txCost,
                    forcedCategory = TransactionCategory.BALANCE_INQUIRY.name
                )
            )
        }

        // ★ Balance inquiry alternate format
        regexBalanceInquiryAlt.find(message)?.let { m ->
            val (id, balAmount, date, time) = m.destructured
            val timeOrNow = time.ifBlank {
                LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
            }
            return ParseResult.Success(
                buildTransaction(
                    smsId, id, "0.00", "Balance Inquiry", date, timeOrNow, "neutral",
                    balance = balAmount.replace(",", "").toDouble(),
                    cost = txCost,
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
    val formattedDate = try {
        LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern)).toString()
    } catch (_: Exception) {
        // If dateStr is already ISO (yyyy-MM-dd) — used by fallback paths above
        dateStr
    }
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
        amount = amountStr.replace(",", "").toDoubleOrNull() ?: 0.0,
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

/**
 * Converts a Unix epoch millisecond value (e.g. the SMS received-at timestamp) into the
 * date/time pair expected by [buildTransaction].
 *
 * Used by message types that carry no date/time in their body text (Fuliza repay,
 * Buy Goods refund, short-form reversals). Using the SMS delivery time is always more
 * accurate than falling back to the current wall-clock time at parse time.
 */
private fun Long.toDateTimePair(): Pair<String, String> {
    val ldt = LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(this),
        ZoneId.systemDefault()
    )
    val date = ldt.toLocalDate().toString()                             // yyyy-MM-dd
    val time = ldt.toLocalTime().format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    return date to time
}