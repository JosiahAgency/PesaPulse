package com.example.cashia.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

class WeeklySummaryWorker(private val ctx: Context, param: WorkerParameters) :
    CoroutineWorker(ctx, param) {

    override suspend fun doWork(): Result {
        return try {
            val dao = AppDatabase.Companion.getDatabase(ctx).transactionDao()
            val weekStart = LocalDate.now().minusDays(7)
            val weekEnd = LocalDate.now().minusDays(1) // yesterday = end of last week

            val all = dao.getAllTransactionsOnce()  // add this query if not present
            val weekTxs = all.filter {
                runCatching { LocalDate.parse(it.date) }.getOrNull()
                    ?.let { d -> !d.isBefore(weekStart) && !d.isAfter(weekEnd) } == true
            }

            if (weekTxs.isEmpty()) return Result.success()

            val income = weekTxs.filter { it.type == "income" }.sumOf { it.amount }
            val expenses = weekTxs.filter { it.type == "expense" }.sumOf { it.amount }

            NotificationHelper.sendWeeklySummary(
                ctx, income, expenses, income - expenses, weekTxs.size
            )
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "weekly_summary"

        fun schedule(context: Context) {
            try {
                val now = LocalDateTime.now()
                val target = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY))
                    .with(LocalTime.of(8, 0))
                    .let { if (!now.isBefore(it)) it.plusWeeks(1) else it }

                val delay = Duration.between(now, target).toMillis()

                val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(WORK_NAME)
                    .build()

                WorkManager.Companion.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (_: Exception) {
            }
        }
    }

}