package com.example.cashia.models

import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter


data class MpesaTransactions(
    val id: String,
    val amount: Double,
    val recipient: String,
    val date: String,
    val type: String,
    val balance: Double
)

fun parseMpesaMessage(message: String): MpesaTransactions? {
    try {
        val regexSentTo =
            Regex("""([A-Z0-9]+)\s+Confirmed\. Ksh([\d,]+\.\d{2}) sent to (.+?) on (\d{1,2}/\d{1,2}/\d{2}) at""")
        val regexPaidTo =
            Regex("""([A-Z0-9]+)\s+Confirmed\. Ksh([\d,]+\.\d{2}) paid to (.+?) on (\d{1,2}/\d{1,2}/\d{2}) at""")
        val regexReceived =
            Regex("""([A-Z0-9]+)\s+Confirmed\.You have received Ksh([\d,]+\.\d{2}) from (.+?) on (\d{1,2}/\d{1,2}/\d{2}) at""")
        val balanceRegex = "balance is Ksh([0-9,]+\\.?[0-9]*)".toRegex(RegexOption.IGNORE_CASE)
        val balance = balanceRegex.find(message)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull() ?: 0.0


        val sentMatch = regexSentTo.find(message)
        if (sentMatch != null) {
            val (id, amountStr, recipient, dateStr) = sentMatch.destructured
            val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d/M/yy"))
            return MpesaTransactions(
                id = id.trim(),
                amount = amountStr.replace(",", "").toDouble(),
                recipient = recipient.trim(),
                date = date.toString(),
                type = "expense",
                balance = balance
            )
        }

        val paidMatch = regexPaidTo.find(message)
        if (paidMatch != null) {
            val (id, amountStr, recipient, dateStr) = paidMatch.destructured
            val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d/M/yy"))
            return MpesaTransactions(
                id = id.trim(),
                amount = amountStr.replace(",", "").toDouble(),
                recipient = recipient.trim(),
                date = date.toString(),
                type = "expense",
                balance = balance
            )
        }

        val receivedMatch = regexReceived.find(message)
        if (receivedMatch != null) {
            val (id, amountStr, sender, dateStr) = receivedMatch.destructured
            val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d/M/yy"))
            return MpesaTransactions(
                id = id.trim(),
                amount = amountStr.replace(",", "").toDouble(),
                recipient = sender.trim(),
                date = date.toString(),
                type = "income",
                balance = balance
            )
        }
        return null

    } catch (e: Exception) {
        Log.e("Transactions", "parseMpesaMessage exception: ${e.message}")
        return null
    }
}