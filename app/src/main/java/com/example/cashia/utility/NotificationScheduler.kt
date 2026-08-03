package com.example.cashia.utility

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.cashia.models.DailyDigestWorker
import com.example.cashia.models.MonthlySummaryWorker
import com.example.cashia.models.UpcomingReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val delayMinutes = minutesUntil(
            PesaTimeConfiguration.dailyDigestHour,
            PesaTimeConfiguration.dailyDigestMinute
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            DailyDigestWorker.WORK_NAME,
            if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DailyDigestWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()
        )
        NotificationRuntimeStore.recordNextRun(
            context,
            NotificationStream.DAILY,
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes)
        )
        Log.i(
            "NotificationScheduler",
            "Scheduled daily digest in ${delayMinutes}m, fromWorker=$fromWorker"
        )
    }

    fun scheduleMonthly(context: Context, fromWorker: Boolean = false) {
        val delayMinutes = minutesUntilMonthlyRun(
            PesaTimeConfiguration.monthlySummaryDayOfMonth,
            PesaTimeConfiguration.monthlySummaryHour,
            PesaTimeConfiguration.monthlySummaryMinute
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            MonthlySummaryWorker.WORK_NAME,
            if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MonthlySummaryWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()
        )
        NotificationRuntimeStore.recordNextRun(
            context,
            NotificationStream.MONTHLY,
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes)
        )
        Log.i(
            "NotificationScheduler",
            "Scheduled monthly summary in ${delayMinutes}m, fromWorker=$fromWorker"
        )
    }

    fun scheduleUpcomingReminders(context: Context, fromWorker: Boolean = false) {
        val delayMinutes = minutesUntil(
            PesaTimeConfiguration.upcomingReminderHour,
            PesaTimeConfiguration.upcomingReminderMinute
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            UpcomingReminderWorker.WORK_NAME,
            if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<UpcomingReminderWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()
        )
        NotificationRuntimeStore.recordNextRun(
            context,
            NotificationStream.UPCOMING,
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(delayMinutes)
        )
        Log.i(
            "NotificationScheduler",
            "Scheduled upcoming reminders in ${delayMinutes}m, fromWorker=$fromWorker"
        )
    }

    suspend fun ensureHealthySchedules(context: Context) = withContext(Dispatchers.IO) {
        val wm = WorkManager.getInstance(context)
        ensureOne(
            wm,
            DailyDigestWorker.WORK_NAME,
            PesaTimeConfiguration.isDailyDigestEnabled
        ) { scheduleDailyDigest(context) }
        ensureOne(
            wm,
            MonthlySummaryWorker.WORK_NAME,
            PesaTimeConfiguration.isMonthlySummaryEnabled
        ) { scheduleMonthly(context) }
        ensureOne(
            wm,
            UpcomingReminderWorker.WORK_NAME,
            PesaTimeConfiguration.isUpcomingReminderEnabled
        ) { scheduleUpcomingReminders(context) }
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

    private fun minutesUntilMonthlyRun(dayOfMonth: Int, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val thisMonthDay = dayOfMonth.coerceIn(1, today.lengthOfMonth())
        var target = today.withDayOfMonth(thisMonthDay)
            .atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        if (!target.isAfter(now)) {
            val nextMonth = today.plusMonths(1)
            val nextMonthDay = dayOfMonth.coerceIn(1, nextMonth.lengthOfMonth())
            target = nextMonth.withDayOfMonth(nextMonthDay)
                .atTime(LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
        }
        return Duration.between(now, target).toMinutes().coerceAtLeast(1)
    }

    private fun ensureOne(
        wm: WorkManager,
        workName: String,
        enabled: Boolean,
        schedule: () -> Unit
    ) {
        if (!enabled) return
        val infos = wm.getWorkInfosForUniqueWork(workName).get()
        val hasActive = infos.any {
            it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.RUNNING ||
                    it.state == WorkInfo.State.BLOCKED
        }
        if (!hasActive) {
            Log.w("NotificationScheduler", "Repairing missing work for $workName")
            schedule()
        } else {
            Log.i(
                "NotificationScheduler",
                "Health check $workName ok: ${infos.joinToString { it.state.name }}"
            )
        }
    }
}
