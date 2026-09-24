package com.example.cashia.utility

import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.UnmatchedSms
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

data class TransactionCorrection(
    val amount: String = "",
    val recipient: String = "",
    val date: String = "",
    val time: String = "",
    val fees: String = "",
    val balance: String = "",
    val reference: String = "",
    val type: String = "",
    val category: String = "OTHER"
) {
    fun toTransaction(source: UnmatchedSms): MpesaTransactions {
        fun money(value: String, label: String, required: Boolean = false): Double? {
            if (value.isBlank() && !required) return null
            require(Regex("""(?:\d+|\d{1,3}(?:,\d{3})+)(?:\.\d{1,2})?""").matches(value.trim())) {
                "Enter a valid $label with at most two decimal places."
            }
            val number = value.trim().replace(",", "").toDouble()
            require(number.isFinite() && number >= 0 && (!required || number > 0)) {
                "$label must ${if (required) "be greater than" else "not be less than"} zero."
            }
            return number
        }
        val amountValue = requireNotNull(money(amount, "amount", required = true))
        require(recipient.isNotBlank()) { "Recipient is required." }
        require(type in setOf("income", "expense", "neutral")) { "Choose the transaction direction." }
        require(category.isNotBlank()) { "Choose a category." }
        val parsedDate = try {
            LocalDate.parse(date.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("Enter a real date in YYYY-MM-DD format.")
        }
        val parsedTime = if (time.isBlank()) null else try {
            LocalTime.parse(time.trim(), DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT))
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("Enter time as HH:mm (24-hour), or leave it blank.")
        }
        val ref = reference.trim().uppercase(Locale.ROOT).takeIf { it.isNotEmpty() }
        require(ref == null || Regex("[A-Z0-9]{10}").matches(ref)) {
            "An M-PESA reference must contain 10 letters or digits, or be left blank."
        }
        return MpesaTransactions(
            id = ref ?: "sms-correction:${source.smsId}", smsId = source.smsId,
            amount = amountValue, recipient = recipient.trim(), date = parsedDate.toString(),
            time = parsedTime?.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US)),
            transactionCost = money(fees, "fees"), balance = money(balance, "balance"),
            type = type, category = category, reference = ref,
            // A date-only row sorts at the start of its day; this is not an observed payment time.
            timestamp = (parsedTime?.let { parsedDate.atTime(it) } ?: parsedDate.atStartOfDay())
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    companion object {
        fun fromSms(source: UnmatchedSms): TransactionCorrection {
            val fields = partialFieldsFromJson(source.extractedFields) ?: return TransactionCorrection()
            val date = fields.date?.let { raw ->
                try {
                    LocalDate.parse(raw, DateTimeFormatter.ofPattern(
                        if (raw.substringAfterLast('/').length == 2) "d/M/uu" else "d/M/uuuu"
                    ).withResolverStyle(ResolverStyle.STRICT)).toString()
                } catch (_: DateTimeParseException) { raw }
            }.orEmpty()
            val time = fields.time?.let { raw ->
                try {
                    LocalTime.parse(raw.uppercase(Locale.US), DateTimeFormatter.ofPattern("h:mm a", Locale.US))
                        .format(DateTimeFormatter.ofPattern("HH:mm"))
                } catch (_: DateTimeParseException) { raw }
            }.orEmpty()
            return TransactionCorrection(
                amount = fields.amount.orEmpty(), recipient = fields.recipient.orEmpty(),
                date = date, time = time, fees = fields.transactionCost.orEmpty(),
                balance = fields.balance.orEmpty(), reference = fields.transactionId.orEmpty(),
                type = fields.likelyType.orEmpty()
            )
        }
    }
}
