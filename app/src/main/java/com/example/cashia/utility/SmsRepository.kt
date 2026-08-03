package com.example.cashia.utility

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.example.cashia.AppDatabase
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.SyncState
import com.example.cashia.models.UnmatchedSms
import com.example.cashia.models.guessCategory
import com.example.cashia.widget.WidgetUpdateManager
import com.example.cashia.utility.SmsRepository.fetchMpesaMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

object SmsRepository {
    data class SmsMessage(
        val id: Long,
        val body: String,
        val date: Long
    )

    private val SMS_PROJECTION = arrayOf(
        "_id",
        "body",
        "date"
    )

    fun fetchMpesaMessages(context: Context, lastSmsId: Long): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = "content://sms/inbox".toUri()

        context.contentResolver.query(
            uri,
            SMS_PROJECTION,
            "address=? AND _id > ?",
            arrayOf("MPESA", lastSmsId.toString()),
            "_id ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex("_id")
            val bodyIndex = cursor.getColumnIndex("body")
            val dateIndex = cursor.getColumnIndex("date")

            if (idIndex < 0 || bodyIndex < 0 || dateIndex < 0) return emptyList()

            while (cursor.moveToNext()) {
                messages.add(
                    SmsMessage(
                        cursor.getLong(idIndex),
                        cursor.getString(bodyIndex),
                        cursor.getLong(dateIndex)
                    )
                )
            }
        }
        return messages
    }

}

sealed class ProcessedResult {
    data class Parsed(val transaction: MpesaTransactions) : ProcessedResult()
    data class Unmatched(val unmatchedSms: UnmatchedSms) : ProcessedResult()
    object Ignored : ProcessedResult()
}

/** Syncs new M-Pesa SMS into the DB. Returns the newly parsed transactions. */
suspend fun syncTransactions(context: Context, database: AppDatabase): List<MpesaTransactions> {
    val startTime = System.currentTimeMillis()
    val dao = database.transactionDao()
    val unmatchedDao = database.unmatchedSmsDao()
    val syncDao = database.syncStateDao()
    val categoryDao = database.customCategoryDao()
    val correctionDao = database.userCorrectionDao()

    val lastSmsId = syncDao.getLastSmsId("mpesa_sync") ?: 0L

    // 1. Fetch Phase
    val fetchStart = System.currentTimeMillis()
    val messages = fetchMpesaMessages(context, lastSmsId)
    val fetchEnd = System.currentTimeMillis()

    if (messages.isEmpty()) return emptyList()

    Log.d("SyncPerf", "Fetched ${messages.size} messages in ${fetchEnd - fetchStart}ms")

    val newestSmsId = messages.last().id
    val customCategories = categoryDao.getAllCustomCategoriesOnce()
    val userCorrections = correctionDao.getAllCorrectionsOnce()
        .associate { it.recipient to it.categoryId }

    // 2. Processing Phase (Parsing & Category Guessing)
    val processStart = System.currentTimeMillis()
    val processedResults = withContext(Dispatchers.Default) {
        messages.map { sms ->
            async {
                when (val result = parseMpesaMessageRich(sms.body, sms.id, sms.date)) {
                    is RichParseResult.FullMatch -> {
                        val tx = result.transaction.copy(smsId = sms.id)
                        val betterCategory = guessCategory(
                            recipient = tx.recipient,
                            type = tx.type,
                            customCategories = customCategories,
                            userCorrections = userCorrections
                        )
                        if (tx.type != "neutral" && tx.amount <= 0.0) {
                            Log.w(
                                "SyncParse",
                                "Parsed non-neutral tx has non-positive amount. smsId=${sms.id}, txId=${tx.id}, type=${tx.type}"
                            )
                        }
                        ProcessedResult.Parsed(
                            if (betterCategory != tx.category) tx.copy(category = betterCategory) else tx
                        )
                    }

                    is RichParseResult.LowConfidence,
                    is RichParseResult.Unmatched -> {
                        val unmatched = result.toUnmatchedSms(
                            rawBody = sms.body,
                            smsId = sms.id,
                            smsReceivedAt = sms.date
                        ) ?: UnmatchedSms(smsId = sms.id, rawBody = sms.body, receivedAt = sms.date)
                        ProcessedResult.Unmatched(unmatched)
                    }

                    is RichParseResult.Irrelevant -> ProcessedResult.Ignored
                }
            }
        }.awaitAll()
    }
    val processEnd = System.currentTimeMillis()
    Log.d("SyncPerf", "Processed ${messages.size} items in ${processEnd - processStart}ms")

    // Sort results
    val parsed = mutableListOf<MpesaTransactions>()
    val unmatched = mutableListOf<UnmatchedSms>()
    for (result in processedResults) {
        when (result) {
            is ProcessedResult.Parsed -> parsed.add(result.transaction)
            is ProcessedResult.Unmatched -> unmatched.add(result.unmatchedSms)
            ProcessedResult.Ignored -> Unit
        }
    }
    val lowConfidenceCount = unmatched.count { it.confidence > 0f }
    val ignoredCount = messages.size - parsed.size - unmatched.size
    Log.i(
        "SyncParseStats",
        "full=${parsed.size}, lowConfidence=$lowConfidenceCount, unmatched=${unmatched.size - lowConfidenceCount}, ignored=$ignoredCount"
    )

    // 3. Database Insertion Phase
    val dbStart = System.currentTimeMillis()
    database.withTransaction {
        if (parsed.isNotEmpty()) dao.insertTransactions(parsed)
        if (unmatched.isNotEmpty()) unmatchedDao.insertAll(unmatched)
        syncDao.upsert(SyncState(key = "mpesa_sync", lastSmsId = newestSmsId))
    }
    val dbEnd = System.currentTimeMillis()

    Log.d("SyncPerf", "Database transaction finished in ${dbEnd - dbStart}ms")
    if (parsed.isNotEmpty()) WidgetUpdateManager.refreshCashiaWidgets(context)
    Log.d("SyncPerf", "Total sync cycle completed in ${dbEnd - startTime}ms")
    return parsed
}
