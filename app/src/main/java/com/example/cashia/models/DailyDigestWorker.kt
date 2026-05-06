package com.example.cashia.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class DailyDigestWorker(private val ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val dao = AppDatabase.getDatabase(ctx).upcomingTransactionDao()
            val today = LocalDate.now().toString()
            val tomorrow = LocalDate.now().plusDays(1).toString()
            val pending = dao.getPendingOnce()

            val lines = buildList {
                pending.filter { it.dueDate == today }
                    .forEach { add("• ${it.title} — KES ${"%.0f".format(it.amount)} due TODAY") }
                pending.filter { it.dueDate == tomorrow }
                    .forEach { add("• ${it.title} — KES ${"%.0f".format(it.amount)} due tomorrow") }
            }
            if (lines.isNotEmpty()) {
                NotificationHelper.sendDailyBills(ctx, lines)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }


    companion object {
        private const val WORK_NAME = "daily_digest"

        fun schedule(context: Context) {
            try {
                val now = LocalDateTime.now()
                val hour = PesaTimeConfiguration.reminderHour.coerceIn(0, 23)
                val min = PesaTimeConfiguration.reminderMinute.coerceIn(0, 59)
                val target = now.with(LocalTime.of(hour, min)).let {
                    if (!now.isBefore(it)) it.plusDays(1) else it
                }
                val delay = Duration.between(now, target).toMillis()

                val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
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