package com.example.cashia.utility

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cashia.MainActivity
import com.pesapulse.R

object NotificationHelper {
    const val CH_DAILY = "ch_daily_digest"
    const val CH_PERIODIC = "ch_periodic_summary"
    const val CH_UPCOMING = "ch_upcoming_reminders"
    const val CH_OVERDUE = "ch_overdue_payments"
    const val CH_LOW_BALANCE = "ch_low_balance"
    const val CH_TARGET_NAG = "ch_monthly_target"

    /** Intent extra carrying an in-app navigation route for the tapped notification. */
    const val EXTRA_ROUTE = "com.example.cashia.EXTRA_ROUTE"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Channel from the removed weekly summary; harmless to call when absent.
        nm.deleteNotificationChannel("ch_weekly_summary")
        listOf(
            NotificationChannel(CH_DAILY, "Daily Recap", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Evening snapshot of the day's spending & income" },
            NotificationChannel(
                CH_PERIODIC,
                "Monthly Summaries",
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .apply { description = "Report for the month that just ended" },
            NotificationChannel(
                CH_UPCOMING,
                "Upcoming Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply { description = "Alerts for upcoming due transactions" },
            NotificationChannel(
                CH_OVERDUE,
                "Overdue Payments",
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply { description = "Recurring alerts for payments past their due date" },
            NotificationChannel(
                CH_LOW_BALANCE,
                "Low Balance Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply { description = "Warns when your M-Pesa balance drops below your threshold" },
            NotificationChannel(
                CH_TARGET_NAG,
                "Monthly Target",
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .apply { description = "Reminders to set a monthly spending/savings target" },
        ).forEach { nm.createNotificationChannel(it) }
    }

    fun post(
        context: Context,
        id: Int,
        channelId: String,
        title: String,
        body: String,
        deepLinkRoute: String? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val pi = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                deepLinkRoute?.let { putExtra(EXTRA_ROUTE, it) }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.pesapulse_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(
                if (channelId == CH_UPCOMING || channelId == CH_OVERDUE || channelId == CH_LOW_BALANCE)
                    NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun ksh(amount: Double) = "KSh ${"%,.0f".format(amount)}"
}