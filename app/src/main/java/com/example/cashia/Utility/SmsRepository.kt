package com.example.cashia.Utility

import android.content.Context
import androidx.core.net.toUri
import com.example.cashia.AppDatabase
import com.example.cashia.Utility.SmsRepository.fetchMpesaMessages
import com.example.cashia.models.parseMpesaMessage

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

    val lastTs = dao.getLastTransactionTimestamp() ?: 0L
    val messages = fetchMpesaMessages(context, lastTs)
    if (messages.isEmpty()) return

    val parsed = messages.mapNotNull { parseMpesaMessage(it) }
    if (parsed.isNotEmpty()) {
        dao.insertTransactions(parsed)
    }
}