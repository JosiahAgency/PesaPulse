package com.example.cashia.utility

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.cashia.models.DailyDigestWorker
import com.example.cashia.models.PeriodSummaryWorker
import com.example.cashia.models.UpcomingReminderWorker
import com.example.cashia.models.WeeklySummaryWorker
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    /** Call on app launch and after each settings toggle */
    fun scheduleAll(context: Context) {
        if (PesaTimeConfiguration.isDailyDigestEnabled) scheduleDailyDigest(context)
        if (PesaTimeConfiguration.isWeeklySummaryEnabled) scheduleWeeklySummary(context)
        if (PesaTimeConfiguration.isMonthlySummaryEnabled) scheduleMonthly(context)
        if (PesaTimeConfiguration.isQuarterlySummaryEnabled) scheduleQuarterly(context)
        if (PesaTimeConfiguration.isYearlySummaryEnabled) scheduleYearly(context)
        if (PesaTimeConfiguration.isUpcomingReminderEnabled) scheduleUpcomingReminders(context)
    }

    fun scheduleDailyDigest(context: Context) {
        val delay = minutesUntil(
            PesaTimeConfiguration.reminderHour,
            PesaTimeConfiguration.reminderMinute
        )
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyDigestWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailyDigestWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()
        )
    }

    fun scheduleWeeklySummary(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeeklySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
                .setInitialDelay(minutesUntilNextWeek(), TimeUnit.MINUTES)
                .build()
        )
    }

    fun scheduleMonthly(context: Context) = enqueuePeriodOnce(
        context,
        PeriodSummaryWorker.WORK_MONTHLY,
        "monthly",
        minutesUntilFirstOfNextMonth()
    )

    fun scheduleQuarterly(context: Context) = enqueuePeriodOnce(
        context,
        PeriodSummaryWorker.WORK_QUARTERLY,
        "quarterly",
        minutesUntilNextQuarter()
    )

    fun scheduleYearly(context: Context) = enqueuePeriodOnce(
        context,
        PeriodSummaryWorker.WORK_YEARLY,
        "yearly",
        minutesUntilNextYear()
    )

    fun scheduleUpcomingReminders(context: Context) {
        // Run 1 hour before the main digest so reminders land first
        val h = (PesaTimeConfiguration.reminderHour - 1).coerceAtLeast(0)
        val delay = minutesUntil(h, PesaTimeConfiguration.reminderMinute)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UpcomingReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<UpcomingReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .build()
        )
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        listOf(
            DailyDigestWorker.WORK_NAME,
            WeeklySummaryWorker.WORK_NAME,
            PeriodSummaryWorker.WORK_MONTHLY,
            PeriodSummaryWorker.WORK_QUARTERLY,
            PeriodSummaryWorker.WORK_YEARLY,
            UpcomingReminderWorker.WORK_NAME,
        ).forEach { wm.cancelUniqueWork(it) }
    }

    // ── Scheduling helpers ─────────────────────────────────────────────────

    private fun enqueuePeriodOnce(context: Context, name: String, period: String, delay: Long) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            name,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<PeriodSummaryWorker>()
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setInputData(workDataOf(PeriodSummaryWorker.KEY_PERIOD to period))
                .build()
        )
    }

    /** Minutes from now until clock hits hour:minute (next occurrence). */
    private fun minutesUntil(hour: Int, minute: Int): Long {
        val now = LocalTime.now()
        val target = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val minutes = java.time.Duration.between(now, target).toMinutes()
        return if (minutes > 0) minutes else minutes + 24 * 60
    }

    private fun minutesUntilNextWeek(): Long {
        val today = LocalDate.now()
        val first = PesaTimeConfiguration.firstDayOfWeek
        val daysUntil = ((first.value - today.dayOfWeek.value + 7) % 7).let {
            if (it == 0) 7 else it
        }
        val target = today.plusDays(daysUntil.toLong()).atTime(9, 0)
        return java.time.Duration.between(today.atTime(LocalTime.now()), target)
            .toMinutes().coerceAtLeast(1)
    }

    private fun minutesUntilFirstOfNextMonth(): Long {
        val today = LocalDate.now()
        val target = today.withDayOfMonth(1).plusMonths(1).atTime(9, 0)
        return java.time.Duration.between(today.atTime(LocalTime.now()), target)
            .toMinutes().coerceAtLeast(1)
    }

    private fun minutesUntilNextQuarter(): Long {
        val today = LocalDate.now()
        val currentQStart = ((today.monthValue - 1) / 3) * 3 + 1
        val target = LocalDate.of(today.year, currentQStart, 1).plusMonths(3).atTime(9, 0)
        return java.time.Duration.between(today.atTime(LocalTime.now()), target)
            .toMinutes().coerceAtLeast(1)
    }

    private fun minutesUntilNextYear(): Long {
        val today = LocalDate.now()
        val target = LocalDate.of(today.year + 1, Month.JANUARY, 1).atTime(9, 0)
        return java.time.Duration.between(today.atTime(LocalTime.now()), target)
            .toMinutes().coerceAtLeast(1)
    }

}