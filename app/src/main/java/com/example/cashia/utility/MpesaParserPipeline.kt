package com.example.cashia.utility

import android.util.Log
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.ParseResult
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.models.parseMpesaMessage
import org.json.JSONObject
import java.security.MessageDigest

const val PARSER_VERSION = 3
const val CONFIDENCE_FULL = 0.85f
const val LOW_CONFIDENCE_THRESHOLD = 0.35f

fun normalizeSmsBody(raw: String): String = raw
    // Collapse all whitespace variants to a single space
    .replace(Regex("""[\u00A0\u200B\u200C\u200D\uFEFF\t\r\n]+"""), " ")
    // Convert full-width digits → ASCII (e.g. ０→0, seen in some aggregators)
    .map { c ->
        if (c in '\uFF10'..'\uFF19') '0' + (c - '\uFF10') else c
    }.joinToString("")
    // Remove stray dot-space combos that break amount parsing ("Ksh1, 200.00")
    .replace(Regex(""",\s+(?=\d)"""), ",")
    .trim()

/**
 * Compute a stable content hash used to group structurally identical messages.
 *
 * We hash the *normalised* body so that minor whitespace differences between
 * otherwise identical messages land in the same bucket.
 *
 * The hash is truncated to 16 hex chars — collision-resistant enough for our use.
 */
fun contentHash(normalizedBody: String): String {
    val bytes = MessageDigest.getInstance("SHA-256")
        .digest(normalizedBody.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }.take(16)
}

// ────────────────────────────────────────────────────────────────────────────
// Partial / fallback extraction
// ────────────────────────────────────────────────────────────────────────────

/**
 * Fields that can be extracted from a message even when full regex doesn't match.
 * All fields are nullable — we fill whatever we can find.
 */
data class PartialFields(
    val transactionId: String? = null,
    val amount: String? = null,
    val recipient: String? = null,
    val date: String? = null,
    val time: String? = null,
    val balance: String? = null,
    val transactionCost: String? = null,
    val likelyType: String? = null   // "income" | "expense" | null
)

// Broad patterns used only in fallback mode — deliberately loose
private val fallbackIdRegex = Regex("""^([A-Z]{2,3}[A-Z0-9]{6,12})\b""")
private val fallbackAmountRegex = Regex("""[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)""")
private val fallbackBalanceRegex = Regex(
    """(?:balance|bal)\s+(?:is\s+)?[Kk][Ss][Hh]\s*([\d,]+\.\d{2})""",
    RegexOption.IGNORE_CASE
)
private val fallbackDateRegex = Regex("""(\d{1,2}/\d{1,2}/\d{2,4})""")
private val fallbackTimeRegex = Regex("""(\d{1,2}:\d{2}\s*[APap][Mm])""")
private val fallbackCostRegex = Regex(
    """(?:Transaction\s+cost|cost)[,.]?\s*[Kk][Ss][Hh]\s*([\d,]+(?:\.\d{2})?)""",
    RegexOption.IGNORE_CASE
)

/** Keywords that suggest the money direction */
private val incomeKeywords = listOf("received", "credited", "reversal", "refund", "deposit")
private val expenseKeywords = listOf("sent", "paid", "withdraw", "bought", "payment", "deducted")

/**
 * Confidence scoring:
 *   +0.25  transaction ID found
 *   +0.30  at least one amount found
 *   +0.20  date found
 *   +0.10  time found
 *   +0.10  balance found
 *   +0.05  transaction cost found
 */
fun partialExtract(normalized: String): Pair<PartialFields, Float> {
    var score = 0f

    val id = fallbackIdRegex.find(normalized)?.groupValues?.get(1)
    if (id != null) score += 0.25f

    val amounts = fallbackAmountRegex.findAll(normalized)
        .map { it.groupValues[1].replace(",", "") }
        .toList()
    val primaryAmount = amounts.firstOrNull()
    if (primaryAmount != null) score += 0.30f

    val balance = fallbackBalanceRegex.find(normalized)?.groupValues?.get(1)?.replace(",", "")
    if (balance != null) score += 0.10f

    val date = fallbackDateRegex.find(normalized)?.groupValues?.get(1)
    if (date != null) score += 0.20f

    val time = fallbackTimeRegex.find(normalized)?.groupValues?.get(1)
    if (time != null) score += 0.10f

    val cost = fallbackCostRegex.find(normalized)?.groupValues?.get(1)?.replace(",", "")
    if (cost != null) score += 0.05f

    val lowerNorm = normalized.lowercase()
    val likelyType = when {
        incomeKeywords.any { it in lowerNorm } -> "income"
        expenseKeywords.any { it in lowerNorm } -> "expense"
        else -> null
    }

    return PartialFields(
        transactionId = id,
        amount = primaryAmount,
        date = date,
        time = time,
        balance = balance,
        transactionCost = cost,
        likelyType = likelyType
    ) to score.coerceIn(0f, 1f)
}

/**
 * Serialise [PartialFields] to a compact JSON string for storage in
 * [UnmatchedSms.extractedFields].  Fields with null values are omitted.
 */
fun PartialFields.toJson(): String {
    val obj = JSONObject()
    transactionId?.let { obj.put("id", it) }
    amount?.let { obj.put("amount", it) }
    recipient?.let { obj.put("recipient", it) }
    date?.let { obj.put("date", it) }
    time?.let { obj.put("time", it) }
    balance?.let { obj.put("balance", it) }
    transactionCost?.let { obj.put("cost", it) }
    likelyType?.let { obj.put("type", it) }
    return obj.toString()
}

/**
 * Deserialize a stored JSON string back into a [PartialFields] for pre-filling the UI.
 * Returns null if parsing fails (corrupt or missing data).
 */
fun partialFieldsFromJson(json: String?): PartialFields? {
    if (json.isNullOrBlank()) return null
    return try {
        val obj = JSONObject(json)
        PartialFields(
            transactionId = obj.optString("id").takeIf { it.isNotBlank() },
            amount = obj.optString("amount").takeIf { it.isNotBlank() },
            recipient = obj.optString("recipient").takeIf { it.isNotBlank() },
            date = obj.optString("date").takeIf { it.isNotBlank() },
            time = obj.optString("time").takeIf { it.isNotBlank() },
            balance = obj.optString("balance").takeIf { it.isNotBlank() },
            transactionCost = obj.optString("cost").takeIf { it.isNotBlank() },
            likelyType = obj.optString("type").takeIf { it.isNotBlank() }
        )
    } catch (_: Exception) {
        null
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Sealed result that includes confidence and partial data
// ────────────────────────────────────────────────────────────────────────────

/**
 * Extended parse result that carries confidence and partial fields alongside
 * the traditional Success / Unmatched / Irrelevant outcomes.
 */
sealed class RichParseResult {
    /** Full match — confidence 1.0, ready to store as a transaction. */
    data class FullMatch(val transaction: MpesaTransactions) : RichParseResult()

    /**
     * Partial match — some fields were extracted but confidence < [CONFIDENCE_FULL].
     * The transaction has been built with best-effort values; the caller should
     * store it as [UnmatchedSms] with [partial] and [confidence] attached so the
     * user can review and correct it.
     */
    data class LowConfidence(
        val partial: PartialFields,
        val confidence: Float,
        val normalizedBody: String
    ) : RichParseResult()

    /** The message looks like M-PESA but nothing could be extracted. */
    data class Unmatched(val normalizedBody: String) : RichParseResult()

    /** Not an M-PESA message — discard silently. */
    object Irrelevant : RichParseResult()
}

// ────────────────────────────────────────────────────────────────────────────
// Main pipeline entry point
// ────────────────────────────────────────────────────────────────────────────

/**
 * Parse a raw SMS body through the full learning pipeline:
 *
 * 1. Normalize the body (whitespace, encoding).
 * 2. Quick-reject non-M-PESA and known noise messages (same as before).
 * 3. Try all primary regexes (your existing patterns).  On success → [RichParseResult.FullMatch].
 * 4. Run [partialExtract] on everything that fell through.
 *    - confidence ≥ [CONFIDENCE_FULL]        → [RichParseResult.FullMatch] (shouldn't happen but guard)
 *    - [LOW_CONFIDENCE_THRESHOLD] ≤ confidence < [CONFIDENCE_FULL] → [RichParseResult.LowConfidence]
 *    - confidence < [LOW_CONFIDENCE_THRESHOLD] → [RichParseResult.Unmatched]
 *
 * Callers should convert [RichParseResult] to an [UnmatchedSms] row using
 * [RichParseResult.toUnmatchedSms] and store it via the DAO.
 */
fun parseMpesaMessageRich(
    rawBody: String,
    smsId: Long,
    smsReceivedAt: Long = System.currentTimeMillis()
): RichParseResult {
    val normalized = normalizeSmsBody(rawBody)

    // ── Step 1: Quick-reject ──────────────────────────────────────────────
    val looksLikeMpesa = normalized.contains("Confirmed", ignoreCase = true) ||
            normalized.contains("M-PESA", ignoreCase = true) ||
            normalized.contains("HAKIKISHA", ignoreCase = true)
    if (!looksLikeMpesa) return RichParseResult.Irrelevant

    // Check for system noise (failed transactions, balance queries, etc.)
    if (isSystemNoise(normalized)) return RichParseResult.Irrelevant

    // ── Step 2: Primary regex pass (full confidence) ──────────────────────
    val legacyResult = parseMpesaMessage(rawBody, smsId, smsReceivedAt)
    if (legacyResult is ParseResult.Success) {
        return RichParseResult.FullMatch(legacyResult.transactions)
    }
    if (legacyResult is ParseResult.Irrelevant) return RichParseResult.Irrelevant

    // ── Step 3: Fallback partial extraction ───────────────────────────────
    val (partial, confidence) = partialExtract(normalized)

    return when {
        confidence >= CONFIDENCE_FULL -> {
            Log.w(
                "MpesaParser",
                "Partial extractor hit CONFIDENCE_FULL — unexpected. Storing as LowConfidence to be safe."
            )
            RichParseResult.LowConfidence(partial, confidence, normalized)
        }

        confidence >= LOW_CONFIDENCE_THRESHOLD ->
            RichParseResult.LowConfidence(partial, confidence, normalized)

        else ->
            RichParseResult.Unmatched(normalized)
    }
}

fun RichParseResult.toUnmatchedSms(
    rawBody: String,
    smsId: Long,
    smsReceivedAt: Long
): UnmatchedSms? {
    val normalized = when (this) {
        is RichParseResult.LowConfidence -> normalizedBody
        is RichParseResult.Unmatched -> normalizedBody
        else -> return null  // FullMatch / Irrelevant — don't store
    }
    return UnmatchedSms(
        smsId = smsId,
        rawBody = rawBody,
        receivedAt = smsReceivedAt,

        contentHash = contentHash(normalized),
        parserVersion = PARSER_VERSION,
        confidence = when (this) {
            is RichParseResult.LowConfidence -> confidence
            else -> 0f
        },
        extractedFields = when (this) {
            is RichParseResult.LowConfidence -> partial.toJson()
            else -> null
        }
    )
}

// ── Noise detection (extracted for reuse) ─────────────────────────────────

private val regexConcurrentUnderway = Regex(
    "M-PESA is unable to process your request because a similar transaction is currently underway",
    RegexOption.IGNORE_CASE
)
private val regexFulizaLimit = Regex(
    "Dear .*, your Fuliza M-PESA limit is KSH [\\d,.]+",
    RegexOption.IGNORE_CASE
)
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

private fun isSystemNoise(normalized: String) =
    regexConcurrentUnderway.containsMatchIn(normalized) ||
            regexFulizaLimit.containsMatchIn(normalized) ||
            regexWrongPin.containsMatchIn(normalized) ||
            regexCancelled.containsMatchIn(normalized) ||
            regexInsufficientFunds.containsMatchIn(normalized) ||
            regexInvalidInput.containsMatchIn(normalized)