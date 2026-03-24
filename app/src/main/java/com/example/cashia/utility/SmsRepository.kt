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
    fun fetchMpesaMessages(context: Context, lastDBTimestamp: Long): List<String> {
        val smsList = mutableListOf<String>()
        val uri = "content://sms/inbox".toUri()
        context.contentResolver.query(
            uri,
            arrayOf("body"),
            "address=? AND date > ?",
            arrayOf("MPESA", lastDBTimestamp.toString()),
            "date ASC"
        )?.use { cursor ->
            val bodyIndex = cursor.getColumnIndex("body")
            if (bodyIndex < 0) return@use
            while (cursor.moveToNext()) {
                smsList.add(cursor.getString(bodyIndex))
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
    val lastTs = minOf(lastTxTs, lastUnmatchedTs).takeIf { it>0L }?:0L

    val messages = fetchMpesaMessages(context, lastTs)
    if (messages.isEmpty()) return

    val parsed = mutableListOf<MpesaTransactions>()
    val unmatched = mutableListOf<UnmatchedSms>()

    for (message in messages) {
        when (val result = parseMpesaMessage(message)) {
            is ParseResult.Success -> parsed.add(result.transactions)
            is ParseResult.Unmatched -> unmatched.add(UnmatchedSms(rawBody = result.rawBody))
            is ParseResult.Irrelevant -> Unit
        }
    }
    Log.d("SyncDebug", "Parsed: ${parsed.size}, Unmatched: ${unmatched.size}")

    if (parsed.isNotEmpty()) dao.insertTransactions(parsed)
    if (unmatched.isNotEmpty()) unmatchedDao.insertAll(unmatched)
}