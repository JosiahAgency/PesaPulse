package com.example.cashia.models

import android.util.Log
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

data class MpesaTransactions(
    val id: String,
    val amount: Double,
    val recipient: String,
    val date: String,
)

fun parseMpesaMessage(message: String): MpesaTransactions? {
    try {
        val regex = Regex("""([A-Z0-9]+)\s+Confirmed\. Ksh([\d,]+\.\d{2}) sent to (.+?) on (\d{1,2}/\d{1,2}/\d{2}) at""")
        val match = regex.find(message) ?: return null
        val (id, amountStr, recipient, dateStr) = match.destructured

        val parsedDate = runCatching {
            LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("d/M/yy"))
        }.getOrElse {
            Log.e("Transactions", "Failed to parse date '$dateStr': ${it.message}")
            return null
        }

        return MpesaTransactions(
            id = id.trim(),
            amount = amountStr.replace(",", "").toDouble(),
            recipient = recipient.trim(),
            date = parsedDate.toString()
        )
    } catch (e: Exception) {
        Log.e("Transactions", "parseMpesaMessage exception: ${e.message}")
        return null
    }
}

//fun parseMpesaMessage(message: String): MpesaTransactions? {
//    return runCatching {
//
//        val regex =
//            Regex("""([A-Z0-9]+) Confirmed\. Ksh([\d,]+(?:\.\d{2})?) sent to (.+?) on (\d{1,2}/\d{1,2}/\d{2}) at .+""")
//        val match = regex.find(message) ?: throw Exception("Regex no match")
//        val (id, amountStr, recipient, dateStr) = match.destructured
//
//        val amount = amountStr.replace(",", "").toDoubleOrNull()
//            ?: throw Exception("Invalid amount '$amountStr'")
//
//        val parsedDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("d/M/yy"))
//
//        MpesaTransactions(
//            id = id,
//            amount = amount,
//            recipient = recipient.trim(),
//            date = parsedDate.toString(),
//        )
//
//    }.getOrElse {
//        Log.e(
//            "Transactions",
//            "Failed to parse message: '${message.take(50)}...' | Reason: ${it.message}"
//        )
//        null
//    }
//}