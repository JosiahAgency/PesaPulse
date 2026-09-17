package com.example.cashia.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.utility.BalanceAlertService
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationRuntimeStore
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.NotificationStream
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Evening recap of the current day, fired at the user's reminder time
 * (default 20:00). Tapping it deep-links to the in-app report for that day.
 */
class DailyDigestWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationRuntimeStore.recordAttempt(context, NotificationStream.DAILY)
        try {
            val db = AppDatabase.getDatabase(context)
            // Daily safety-net: catches balance dips even on days with no new SMS sync.
            BalanceAlertService.checkAndNotify(context, db)

            if (!PesaTimeConfiguration.isDailyDigestEnabled) return Result.success()

            val today = LocalDate.now().toString()
            val txs = withContext(Dispatchers.IO) {
                db.transactionDao().getTransactionsForDateRange(today, today)
            }
            val balance =
                withContext(Dispatchers.IO) { db.transactionDao().getLatestBalance() } ?: 0.0

            var income = 0.0
            var expense = 0.0
            txs.forEach {
                when (it.type) {
                    "income" -> income += it.amount
                    "expense" -> expense += it.amount + it.transactionCost
                }
            }
            val net = income - expense

            val body = buildString {
                append("💰 Income:   ${NotificationHelper.ksh(income)}\n")
                append("💸 Spent:    ${NotificationHelper.ksh(expense)}\n")
                append("📊 Net:      ${if (net >= 0) "+" else ""}${NotificationHelper.ksh(net)}\n")
                append("🏦 Balance:  ${NotificationHelper.ksh(balance)}")
                if (txs.isNotEmpty()) append("\n📋 ${txs.size} transaction(s) today · tap for details")
                else append("\n🌿 No M-Pesa activity today")
            }

            NotificationHelper.post(
                context,
                id = ID,
                channelId = NotificationHelper.CH_DAILY,
                title = "📅 Today's Recap",
                body = body,
                deepLinkRoute = AppDestinations.SummaryReport.buildRoute("day", today)
            )
            NotificationRuntimeStore.recordSuccess(context, NotificationStream.DAILY)
            return Result.success()
        } catch (e: Exception) {
            NotificationRuntimeStore.recordError(context, NotificationStream.DAILY, e.localizedMessage)
            return Result.failure()
        } finally {
            // Queue tomorrow's run even when disabled or on failure, so the chain
            // never dies. APPEND keeps this running instance from cancelling itself.
            NotificationScheduler.scheduleDailyDigest(context, fromWorker = true)
        }
    }

    companion object {
        const val WORK_NAME = "daily_digest"
        private const val ID = 1001
    }
}
