package com.example.cashia.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration

/**
 * Weekly nag reminding the user to set a monthly spending cap and/or savings goal.
 * Stops nagging as soon as either target is configured (checked live each run, so it
 * also self-silences if the user sets one shortly after a run) but keeps a lightweight
 * weekly check running indefinitely in case targets are cleared again later.
 */
class MonthlyTargetReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            if (!PesaTimeConfiguration.isMonthlyTargetReminderEnabled) return Result.success()
            if (PesaTimeConfiguration.hasMonthlyTargetSet) return Result.success()

            NotificationHelper.post(
                context,
                id = ID,
                channelId = NotificationHelper.CH_TARGET_NAG,
                title = "🎯 Set a Monthly Target",
                body = "You haven't set a monthly spending limit or savings goal yet. " +
                    "Tap to set one so Cashia can track your progress.",
                deepLinkRoute = AppDestinations.NotificationsSettings.route
            )
            return Result.success()
        } finally {
            // The scheduler respects the latest enabled setting before chaining.
            NotificationScheduler.scheduleMonthlyTargetReminder(context, fromWorker = true)
        }
    }

    companion object {
        const val WORK_NAME = "monthly_target_reminder"
        private const val ID = 1004
    }
}
