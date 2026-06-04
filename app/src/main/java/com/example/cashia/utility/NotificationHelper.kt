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
    const val CH_WEEKLY = "ch_weekly_summary"
    const val CH_PERIODIC = "ch_periodic_summary"
    const val CH_UPCOMING = "ch_upcoming_reminders"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(CH_DAILY, "Daily Digest", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Daily spending & income snapshot" },
            NotificationChannel(CH_WEEKLY, "Weekly Summary", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Weekly financial overview" },
            NotificationChannel(
                CH_PERIODIC,
                "Periodic Summaries",
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .apply { description = "Monthly, quarterly & yearly reports" },
            NotificationChannel(
                CH_UPCOMING,
                "Upcoming Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply { description = "Alerts for upcoming due transactions" },
        ).forEach { nm.createNotificationChannel(it) }
    }

    fun post(context: Context, id: Int, channelId: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val pi = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
                if (channelId == CH_UPCOMING) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun ksh(amount: Double) = "KSh ${"%,.0f".format(amount)}"
}