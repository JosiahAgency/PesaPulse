package com.example.cashia.utility

import android.content.Context
import androidx.core.content.edit

enum class NotificationStream(val key: String) {
    DAILY("daily"),
    MONTHLY("monthly"),
    UPCOMING("upcoming")
}

data class NotificationStreamStatus(
    val lastAttemptAt: Long = 0L,
    val lastSuccessAt: Long = 0L,
    val lastErrorAt: Long = 0L,
    val lastErrorMessage: String? = null,
    val nextRunAt: Long = 0L,
)

object NotificationRuntimeStore {
    private const val PREFS = "notification_runtime"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(stream: NotificationStream, suffix: String) = "${stream.key}_$suffix"

    fun recordAttempt(context: Context, stream: NotificationStream) {
        prefs(context).edit { putLong(key(stream, "last_attempt"), System.currentTimeMillis()) }
    }

    fun recordSuccess(context: Context, stream: NotificationStream) {
        prefs(context).edit { putLong(key(stream, "last_success"), System.currentTimeMillis()) }
    }

    fun recordError(context: Context, stream: NotificationStream, message: String?) {
        prefs(context).edit {
            putLong(key(stream, "last_error"), System.currentTimeMillis())
                .putString(key(stream, "last_error_message"), message ?: "Unknown error")
        }
    }

    fun recordNextRun(context: Context, stream: NotificationStream, epochMillis: Long) {
        prefs(context).edit { putLong(key(stream, "next_run"), epochMillis) }
    }

    fun getStatus(context: Context, stream: NotificationStream): NotificationStreamStatus {
        val p = prefs(context)
        return NotificationStreamStatus(
            lastAttemptAt = p.getLong(key(stream, "last_attempt"), 0L),
            lastSuccessAt = p.getLong(key(stream, "last_success"), 0L),
            lastErrorAt = p.getLong(key(stream, "last_error"), 0L),
            lastErrorMessage = p.getString(key(stream, "last_error_message"), null),
            nextRunAt = p.getLong(key(stream, "next_run"), 0L),
        )
    }
}
