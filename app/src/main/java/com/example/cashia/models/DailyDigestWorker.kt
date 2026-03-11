package com.example.cashia.models

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.pesapulse.R
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class DailyDigestWorker(private val ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {

            val dao = AppDatabase.getDatabase(ctx).upcomingTransactionDao()
            val today = LocalDate.now().toString()
            val tomorrow = LocalDate.now().plusDays(1).toString()

            val pending = dao.getPendingOnce()
            val dueToday = pending.filter { it.dueDate == today }
            val dueTomorrow = pending.filter { it.dueDate == tomorrow }

            if (dueToday.isEmpty() && dueTomorrow.isEmpty()) return Result.success()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ctx.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) return Result.success()
            }

            val lines = buildList {
                dueToday.forEach { add("• ${it.title} — KES ${"%.0f".format(it.amount)} due TODAY") }
                dueTomorrow.forEach { add("• ${it.title} — KES ${"%.0f".format(it.amount)} due tomorrow") }
            }

            sendNotification(ctx, lines)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val CHANNEL_ID = "pesa_digest"
        private const val WORK_NAME = "daily_digest"
        private const val NOTIFICATION_ID = 42

        fun schedule(context: Context) {
            try {
                ensureChannel(context)
                val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(1, TimeUnit.DAYS)
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (e: Exception) {
                // WorkManager not initialized yet — safe to ignore, called again on next launch
            }
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Payment Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifies you when upcoming payments are due"
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }

        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        private fun sendNotification(context: Context, lines: List<String>) {
            ensureChannel(context)
            val body = lines.joinToString("\n")
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.pesapulse_logo)
                .setContentTitle("💳 Upcoming Payments")
                .setContentText(if (lines.size == 1) lines.first() else "${lines.size} payments due soon")
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}