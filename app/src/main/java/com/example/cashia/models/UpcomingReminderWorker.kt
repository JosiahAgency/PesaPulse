package com.example.cashia.models

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class UpcomingReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result = try {
        run()
    } finally {
        // Queue tomorrow's run so the one-time chain never dies.
        NotificationScheduler.scheduleUpcomingReminders(context, fromWorker = true)
    }

    private suspend fun run(): Result {
        if (!PesaTimeConfiguration.isUpcomingReminderEnabled) return Result.success()

        val db = AppDatabase.getDatabase(context)
        val pending = withContext(Dispatchers.IO) { db.upcomingTransactionDao().getPendingOnce() }
        val today = LocalDate.now()
        val window = PesaTimeConfiguration.upcomingReminderDaysBefore.toLong()

        pending.forEach { tx ->
            val dueDate = runCatching { LocalDate.parse(tx.dueDate) }.getOrNull() ?: return@forEach
            val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
            val icon = if (tx.type == UpcomingType.INCOME) "💰" else "💸"

            when {
                // Upcoming (due today through the user's reminder window)
                daysUntil in 0..window -> {
                    val whenText = when (daysUntil) {
                        0L -> "Due TODAY"
                        1L -> "Due TOMORROW"
                        else -> "Due in $daysUntil days"
                    }
                    NotificationHelper.post(
                        context,
                        id = 2000 + (tx.id.hashCode() and 0x3FF),
                        channelId = NotificationHelper.CH_UPCOMING,
                        title = "⏰ ${tx.title}",
                        body = "$icon ${NotificationHelper.ksh(tx.amount)} • $whenText"
                    )
                }

                // Overdue: alert the day it becomes overdue, then re-alert weekly
                // until the user marks it paid/cancelled — no longer capped at 3 days.
                daysUntil < 0 && (daysUntil == -1L || abs(daysUntil) % 7 == 0L) -> {
                    val whenText = "⚠️ OVERDUE by ${abs(daysUntil)} day(s)"
                    NotificationHelper.post(
                        context,
                        id = 3000 + (tx.id.hashCode() and 0x3FF),
                        channelId = NotificationHelper.CH_OVERDUE,
                        title = "⚠️ ${tx.title}",
                        body = "$icon ${NotificationHelper.ksh(tx.amount)} • $whenText"
                    )
                }
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "upcoming_reminders"
    }
}