package com.example.cashia.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.utility.NotificationHelper
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class MonthlySummaryWorker(private val ctx: Context, param: WorkerParameters) :
    CoroutineWorker(ctx, param) {
    override suspend fun doWork(): Result {
        return try {
            val dao = AppDatabase.getDatabase(ctx).transactionDao()

            val lastMonth = LocalDate.now().minusMonths(1)
            val monthStart = lastMonth.withDayOfMonth(1)
            val monthEnd = lastMonth.with(TemporalAdjusters.lastDayOfMonth())
            val monthLabel = lastMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

            val monthTxs = dao.getTransactionsForDateRange(
                monthStart.toString(),
                monthEnd.toString()
            )

            if (monthTxs.isEmpty()) return Result.success()

            var income = 0.0
            var expenses = 0.0
            monthTxs.forEach {
                when (it.type) {
                    "income" -> income += it.amount
                    "expense" -> expenses += it.amount + it.transactionCost
                }
            }

            val net = income - expenses
            val body = buildString {
                append("$monthLabel\n")
                append("💰 Income:   ${NotificationHelper.ksh(income)}\n")
                append("💸 Expenses: ${NotificationHelper.ksh(expenses)}\n")
                append("📊 Net:      ${if (net >= 0) "+" else ""}${NotificationHelper.ksh(net)}\n")
                append("📋 ${monthTxs.size} transactions")
            }

            NotificationHelper.post(
                ctx,
                id = 1003,
                channelId = NotificationHelper.CH_PERIODIC,
                title = "📅 $monthLabel Summary",
                body = body
            )

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "monthly_summary"

        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            val target = now.with(TemporalAdjusters.firstDayOfNextMonth())
                .with(LocalTime.of(9, 0))
            val delay  = Duration.between(now, target).toMillis()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<MonthlySummaryWorker>(30, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(WORK_NAME)
                    .build()
            )

        }
    }
}