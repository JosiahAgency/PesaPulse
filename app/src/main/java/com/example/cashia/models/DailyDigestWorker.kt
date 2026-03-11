package com.example.cashia.models

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.MainActivity
import com.example.cashia.Utility.PesaTimeConfiguration
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
            val dueToday = pending.filter { it.dueDate == today }
            val dueTomorrow = pending.filter { it.dueDate == tomorrow }

            // Nothing due — skip notification but still count as success
            if (dueToday.isEmpty() && dueTomorrow.isEmpty()) return Result.success()

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
        private const val CHANNEL_ID = "pesapulse_daily_digest"
        private const val CHANNEL_NAME = "Daily Digest"
        private const val WORK_NAME = "daily_digest"
        private const val NOTIFICATION_ID = 42

        fun schedule(context: Context) {
            try {
                // Ensure channel exists before scheduling so the first notification works
                ensureChannel(context)

                val now = LocalDateTime.now()
                val hour = maxOf(0, minOf(23, PesaTimeConfiguration.reminderHour))
                val minute = maxOf(0, minOf(59, PesaTimeConfiguration.reminderMinute))

                val targetTime = now.with(LocalTime.of(hour, minute)).let {
                    // If we've already passed the target time today, schedule for tomorrow
                    if (now.isAfter(it) || now.isEqual(it)) it.plusDays(1) else it
                }

                val delay = Duration.between(now, targetTime).toMillis()

                val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag(WORK_NAME)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (e: Exception) {
                // WorkManager not initialized yet — safe to ignore; called again on next launch
            }
        }

        private fun ensureChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies you when upcoming payments are due"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        private fun sendNotification(context: Context, lines: List<String>) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return  // Permission not granted; skip notification silently
            }

            ensureChannel(context)

            val body = lines.joinToString("\n")
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.pesapulse.R.mipmap.pesapulse_logo) // ✅ Restored small icon
                .setContentTitle("💳 Upcoming Payments")
                .setContentText(
                    if (lines.size == 1) lines.first() else "${lines.size} payments due soon"
                )
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}