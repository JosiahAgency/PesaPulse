package com.example.cashia.utility

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * All notification workers rely on WorkManager's self-chaining OneTimeWorkRequests
 * firing at exact wall-clock times, even when the app hasn't been opened. On most
 * OEM skins (Xiaomi/MIUI, Oppo/ColorOS, Vivo/FuntouchOS, Samsung, etc.) apps that are
 * still subject to battery optimization get their background jobs deferred for hours
 * or dropped entirely once the app leaves the foreground for a while — this is the
 * single most common reason reminders "work in theory" but never actually arrive.
 * Requesting exemption from battery optimizations is the standard fix recommended by
 * Android's own WorkManager docs for time-sensitive background reminders.
 */
object BatteryOptimizationHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Launches the system dialog letting the user exempt this app directly (no need
     * to dig through Settings). Falls back gracefully if the OEM doesn't support it.
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }.onFailure {
            // Some OEMs (or Android versions without this action) reject the direct
            // request dialog — send the user to the general battery settings instead.
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            if (context !is android.app.Activity) fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(fallback) }
        }
    }

    /** True on API levels where battery optimization exemptions even apply. */
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}
