package com.example.cashia.Utility

import android.content.Context
import androidx.core.net.toUri

object SmsRepository {
    fun fetchMpesaMessages(context: Context): List<String> {
        val smsList = mutableListOf<String>()
        val uri = "content://sms/inbox".toUri()
        val cursor = context.contentResolver.query(
            uri, arrayOf("address", "body"), "address=?", arrayOf("MPESA"), "date DESC"
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