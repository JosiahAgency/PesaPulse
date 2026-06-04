package com.example.cashia.models

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DailyDigestWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        if (!PesaTimeConfiguration.isDailyDigestEnabled) return Result.success()

        val today = LocalDate.now().toString()
        val db = AppDatabase.getDatabase(context)
        val txs = withContext(Dispatchers.IO) {
            db.transactionDao().getTransactionsForDateRange(today, today)
        }
        val balance = withContext(Dispatchers.IO) { db.transactionDao().getLatestBalance() } ?: 0.0

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
            if (txs.isNotEmpty()) append("\n📋 ${txs.size} transaction(s) today")
            if (txs.isEmpty()) append("\n🌿 No M-Pesa activity today")
        }

        NotificationHelper.post(context, ID, NotificationHelper.CH_DAILY, "📅 Daily Digest", body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_digest"
        private const val ID = 1001
    }
}