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
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class UpcomingReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        if (!PesaTimeConfiguration.isUpcomingReminderEnabled) return Result.success()

        val db = AppDatabase.getDatabase(context)
        val pending = withContext(Dispatchers.IO) { db.upcomingTransactionDao().getPendingOnce() }
        val today = LocalDate.now()
        val window = PesaTimeConfiguration.upcomingReminderDaysBefore.toLong()

        pending.forEach { tx ->
            val dueDate = runCatching { LocalDate.parse(tx.dueDate) }.getOrNull() ?: return@forEach
            val daysUntil = ChronoUnit.DAYS.between(today, dueDate)

            // Fire for overdue (< 0, up to -3) and upcoming within the user's window
            if (daysUntil in -3..window) {
                val icon = if (tx.type == UpcomingType.INCOME) "💰" else "💸"
                val whenText = when {
                    daysUntil < 0 -> "⚠️ OVERDUE by ${abs(daysUntil)} day(s)"
                    daysUntil == 0L -> "Due TODAY"
                    daysUntil == 1L -> "Due TOMORROW"
                    else -> "Due in $daysUntil days"
                }
                val body = "$icon ${NotificationHelper.ksh(tx.amount)} • $whenText"
                // Stable ID per upcoming item so repeated runs update rather than stack
                val notifId = 2000 + (tx.id.hashCode() and 0x3FF)

                NotificationHelper.post(
                    context, notifId,
                    NotificationHelper.CH_UPCOMING,
                    "⏰ ${tx.title}",
                    body
                )
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "upcoming_reminders"
    }
}