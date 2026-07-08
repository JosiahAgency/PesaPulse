package com.example.cashia.utility

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cashia.models.DailyDigestWorker
import com.example.cashia.models.MonthlySummaryWorker
import com.example.cashia.models.UpcomingReminderWorker
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * All summary notifications are self-chaining one-time work: each run enqueues
 * the next occurrence at an exact wall-clock time. (PeriodicWorkRequest drifts
 * after doze deferrals, so it isn't used here.)
 */
object NotificationScheduler {

    fun scheduleAll(context: Context) {
        if (PesaTimeConfiguration.isDailyDigestEnabled) scheduleDailyDigest(context)
        if (PesaTimeConfiguration.isMonthlySummaryEnabled) scheduleMonthly(context)
        if (PesaTimeConfiguration.isUpcomingReminderEnabled) scheduleUpcomingReminders(context)
    }

    /**
     * [fromWorker] must be true when called from inside the worker itself:
     * REPLACE would cancel the running instance, APPEND_OR_REPLACE chains after it.
     */
    fun scheduleDailyDigest(context: Context, fromWorker: Boolean = false) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            DailyDigestWorker.WORK_NAME,
            if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DailyDigestWorker>()
                .setInitialDelay(
                    minutesUntil(
                        PesaTimeConfiguration.reminderHour,
                        PesaTimeConfiguration.reminderMinute
                    ),
                    TimeUnit.MINUTES
                )
                .build()
        )
    }

    fun scheduleMonthly(context: Context, fromWorker: Boolean = false) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            MonthlySummaryWorker.WORK_NAME,
            if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MonthlySummaryWorker>()
                .setInitialDelay(minutesUntilFirstOfNextMonth(), TimeUnit.MINUTES)
                .build()
        )
    }

    fun scheduleUpcomingReminders(context: Context, fromWorker: Boolean = false) {
        // Run 1 hour before the main digest so reminders land first
        val h = (PesaTimeConfiguration.reminderHour - 1).coerceAtLeast(0)
        WorkManager.getInstance(context).enqueueUniqueWork(
            UpcomingReminderWorker.WORK_NAME,
            if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<UpcomingReminderWorker>()
                .setInitialDelay(
                    minutesUntil(h, PesaTimeConfiguration.reminderMinute),
                    TimeUnit.MINUTES
                )
                .build()
        )
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        listOf(
            DailyDigestWorker.WORK_NAME,
            MonthlySummaryWorker.WORK_NAME,
            UpcomingReminderWorker.WORK_NAME,
        ).forEach { wm.cancelUniqueWork(it) }
    }

    // ── Scheduling helpers ─────────────────────────────────────────────────

    /** Minutes from now until the clock next hits hour:minute (min 1). */
    private fun minutesUntil(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.toLocalDate()
            .atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMinutes().coerceAtLeast(1)
    }

    private fun minutesUntilFirstOfNextMonth(): Long {
        val now = LocalDateTime.now()
        val target = LocalDate.now().withDayOfMonth(1).plusMonths(1).atTime(9, 0)
        return Duration.between(now, target).toMinutes().coerceAtLeast(1)
    }
}
