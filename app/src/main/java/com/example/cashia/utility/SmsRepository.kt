package com.example.cashia.utility

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.example.cashia.AppDatabase
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.ParseResult
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.models.parseMpesaMessage
import com.example.cashia.utility.SmsRepository.fetchMpesaMessages

object SmsRepository {
    fun fetchMpesaMessages(context: Context, lastDBTimestamp: Long): List<Pair<String, Long>> {
        val smsList = mutableListOf<Pair<String, Long>>()
        val uri = "content://sms/inbox".toUri()
        context.contentResolver.query(
            uri,
            arrayOf("body", "date"),
            "address=? AND date > ?",
            arrayOf("MPESA", lastDBTimestamp.toString()),
            "date ASC"
        )?.use { cursor ->
            val bodyIndex = cursor.getColumnIndex("body")
            val dateIndex = cursor.getColumnIndex("date")
            if (bodyIndex < 0) return@use
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyIndex)
                val date =
                    if (dateIndex >= 0) cursor.getLong(dateIndex) else System.currentTimeMillis()
                smsList.add(body to date)
            }
        }
        return smsList
    }

}

suspend fun syncTransactions(context: Context, database: AppDatabase) {
    val dao = database.transactionDao()
    val unmatchedDao = database.unmatchedSmsDao()

    val lastTxTs = dao.getLastTransactionTimestamp() ?: 0L
    val lastUnmatchedTs = unmatchedDao.getLastReceivedAt() ?: 0L
    val lastTs = maxOf(lastTxTs, lastUnmatchedTs)

    val messages = fetchMpesaMessages(context, lastTs)
    if (messages.isEmpty()) return

    val parsed = mutableListOf<MpesaTransactions>()
    val unmatched = mutableListOf<UnmatchedSms>()

    for ((body, receivedAt) in messages) {
        when (val result = parseMpesaMessage(body, receivedAt)) {
            is ParseResult.Success -> parsed.add(result.transactions)
            is ParseResult.Unmatched -> unmatched.add(UnmatchedSms(rawBody = result.rawBody))
            is ParseResult.Irrelevant -> Unit
        }
    }
    Log.d("SyncDebug", "Parsed: ${parsed.size}, Unmatched: ${unmatched.size}")

    if (parsed.isNotEmpty()) dao.insertTransactions(parsed)
    if (unmatched.isNotEmpty()) unmatchedDao.insertAll(unmatched)
}