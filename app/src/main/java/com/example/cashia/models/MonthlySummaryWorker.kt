package com.example.cashia.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
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

            // Summarise the month that just ended
            val lastMonth = LocalDate.now().minusMonths(1)
            val monthStart = lastMonth.withDayOfMonth(1)
            val monthEnd = lastMonth.with(TemporalAdjusters.lastDayOfMonth())
            val monthLabel = lastMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

            val all = dao.getAllTransactionsOnce()
            val monthTxs = all.filter {
                runCatching { LocalDate.parse(it.date) }.getOrNull()
                    ?.let { d -> !d.isBefore(monthStart) && !d.isAfter(monthEnd) } == true
            }

            if (monthTxs.isEmpty()) return Result.success()

            val income = monthTxs.filter { it.type == "income" }.sumOf { it.amount }
            val expenses = monthTxs.filter { it.type == "expense" }.sumOf { it.amount }

            NotificationHelper.sendMonthlySummary(
                ctx, monthLabel, income, expenses, income - expenses, monthTxs.size
            )
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "monthly_summary"

        fun schedule(context: Context) {
            try {
                // Fire on the 1st of each month at 9 AM
                val now = LocalDateTime.now()
                val target = now.with(TemporalAdjusters.firstDayOfNextMonth())
                    .with(LocalTime.of(9, 0))

                val daysUntil = Duration.between(now, target).toMillis()
                TimeUnit.DAYS.toMillis(30)

                val request = PeriodicWorkRequestBuilder<MonthlySummaryWorker>(30, TimeUnit.DAYS)
                    .setInitialDelay(daysUntil, TimeUnit.MILLISECONDS)
                    .addTag(WORK_NAME)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (_: Exception) {
            }
        }
    }
}