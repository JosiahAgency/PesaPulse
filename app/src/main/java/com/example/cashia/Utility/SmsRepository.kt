package com.example.cashia.Utility

import android.content.Context
import androidx.annotation.UiContext
import androidx.core.net.toUri
import com.example.cashia.AppDatabase
import com.example.cashia.Utility.SmsRepository.fetchMpesaMessages
import com.example.cashia.models.parseMpesaMessage
import java.time.format.DateTimeFormatter

object SmsRepository {
    fun fetchMpesaMessages(context: Context, lastDBTimestamp: Long): List<String> {
        val smsList = mutableListOf<String>()
        val uri = "content://sms/inbox".toUri()
        val selection = "address=? AND date > ?"
        val selectionArgs = arrayOf("MPESA", lastDBTimestamp.toString())
        val cursor = context.contentResolver.query(
            uri, arrayOf("body"), selection, selectionArgs, "date ASC"
        )
        cursor?.use {
            val bodyIndex = it.getColumnIndex("body")
            while (it.moveToNext()) {
                val body = it.getString(bodyIndex)
                smsList.add(body)
            }
        }
        return smsList
    }

}

suspend fun syncTransactions(context: Context, database: AppDatabase) {
    val dao = database.transactionDao()
    val lastDBTimestamp = dao.getLastTransactionTimestamp() ?: 0L
    val newMessages = fetchMpesaMessages(context, lastDBTimestamp)
    val parsedEntities = newMessages.mapNotNull { body ->
        parseMpesaMessage(body)
    }
    if (parsedEntities.isNotEmpty()) {
        dao.insertTransactions(parsedEntities)
    }

}