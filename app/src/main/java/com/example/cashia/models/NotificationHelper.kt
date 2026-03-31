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
import com.example.cashia.MainActivity

object NotificationHelper {

    const val CHANNEL_DAILY = "cashia_daily_digest"
    const val CHANNEL_WEEKLY = "cashia_weekly_summary"
    const val CHANNEL_MONTHLY = "cashia_monthly_summary"

    private const val ID_DAILY_BILLS = 100
    private const val ID_WEEKLY = 101
    private const val ID_MONTHLY = 102

    fun createAllChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            Triple(CHANNEL_DAILY, "Daily Digest", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_WEEKLY, "Weekly Summary", NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_MONTHLY, "Monthly Summary", NotificationManager.IMPORTANCE_DEFAULT),
        ).forEach { (id, name, importance) ->
            nm.createNotificationChannel(NotificationChannel(id, name, importance))
        }
    }

    fun send(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        lines: List<String> = emptyList()
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi =
            PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)

        if (lines.size > 1) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun sendDailyBills(context: Context, lines: List<String>) {
        val body = if (lines.size == 1) lines.first() else "${lines.size} payments due soon"
        send(context, CHANNEL_DAILY, ID_DAILY_BILLS, "💳 Upcoming Payments", body, lines)
    }

    fun sendWeeklySummary(
        context: Context,
        income: Double,
        expenses: Double,
        net: Double,
        txCount: Int
    ) {
        val sign = if (net >= 0) "+" else ""
        val body =
            "Income: KES ${"%.0f".format(income)}  •  Expenses: KES ${"%.0f".format(expenses)}"
        val lines = listOf(
            "Income:    KES ${"%.0f".format(income)}",
            "Expenses:  KES ${"%.0f".format(expenses)}",
            "Net:       ${sign}KES ${"%.0f".format(net)}",
            "$txCount transactions this week"
        )
        send(context, CHANNEL_WEEKLY, ID_WEEKLY, "📊 Weekly Summary", body, lines)
    }

    fun sendMonthlySummary(
        context: Context,
        month: String,
        income: Double,
        expenses: Double,
        net: Double,
        txCount: Int
    ) {
        val sign = if (net >= 0) "+" else ""
        val body = "$month • Net ${sign}KES ${"%.0f".format(net)}"
        val lines = listOf(
            "Income:    KES ${"%.0f".format(income)}",
            "Expenses:  KES ${"%.0f".format(expenses)}",
            "Net:       ${sign}KES ${"%.0f".format(net)}",
            "$txCount transactions"
        )
        send(context, CHANNEL_MONTHLY, ID_MONTHLY, "📅 $month Summary", body, lines)
    }
}