package com.example.cashia.utility

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.edit
import com.example.cashia.ui.navigation.AppDestinations
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeScope {
    DAY, WEEK, MONTH, CUSTOM, YEAR
}

enum class TransactionType {
    INCOME, EXPENSE, ALL
}

object PesaActions {
    fun playClick(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun playSuccess(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun copyToClipboard(
        context: Context,
        clipboard: ClipboardManager,
        haptic: HapticFeedback,
        label: String,
        value: String
    ) {
        clipboard.setText(AnnotatedString(value))
        playSuccess(haptic)
        Toast.makeText(context, "$label Copied", Toast.LENGTH_SHORT).show()
    }
}

object PesaTimeConfiguration {
    var use24HrsFormat by mutableStateOf(false)
    var datePattern: String = "dd MMM yyyy"
    var firstDayOfWeek by mutableStateOf(DayOfWeek.SUNDAY)
    var isDarkTheme by mutableStateOf(true)
    const val CURRENCY = "KES"
    var isBiometricEnabled by mutableStateOf(false)
    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)       // 8 PM default
        set(v) {
            prefs.edit { putInt("reminder_hour", v) }
        }
    var reminderMinute: Int
        get() = prefs.getInt("reminder_minute", 0)
        set(v) {
            prefs.edit { putInt("reminder_minute", v) }
        }

    var dailyDigestHour: Int
        get() = prefs.getInt("daily_digest_hour", reminderHour)
        set(v) {
            prefs.edit { putInt("daily_digest_hour", v) }
        }

    var dailyDigestMinute: Int
        get() = prefs.getInt("daily_digest_minute", reminderMinute)
        set(v) {
            prefs.edit { putInt("daily_digest_minute", v) }
        }

    fun saveDailyDigestTime(hour: Int, minute: Int) {
        dailyDigestHour = hour
        dailyDigestMinute = minute
    }

    var monthlySummaryDayOfMonth: Int
        get() = prefs.getInt("monthly_summary_day", 1).coerceIn(1, 28)
        set(v) {
            prefs.edit { putInt("monthly_summary_day", v.coerceIn(1, 28)) }
        }

    var monthlySummaryHour: Int
        get() = prefs.getInt("monthly_summary_hour", 9)
        set(v) {
            prefs.edit { putInt("monthly_summary_hour", v) }
        }

    var monthlySummaryMinute: Int
        get() = prefs.getInt("monthly_summary_minute", 0)
        set(v) {
            prefs.edit { putInt("monthly_summary_minute", v) }
        }

    fun saveMonthlySummarySchedule(dayOfMonth: Int, hour: Int, minute: Int) {
        monthlySummaryDayOfMonth = dayOfMonth
        monthlySummaryHour = hour
        monthlySummaryMinute = minute
    }

    var upcomingReminderHour: Int
        get() = prefs.getInt("upcoming_reminder_hour", (reminderHour - 1).coerceAtLeast(0))
        set(v) {
            prefs.edit { putInt("upcoming_reminder_hour", v) }
        }

    var upcomingReminderMinute: Int
        get() = prefs.getInt("upcoming_reminder_minute", reminderMinute)
        set(v) {
            prefs.edit { putInt("upcoming_reminder_minute", v) }
        }

    fun saveUpcomingReminderTime(hour: Int, minute: Int) {
        upcomingReminderHour = hour
        upcomingReminderMinute = minute
    }

    var isDailyDigestEnabled: Boolean
        get() = prefs.getBoolean("daily_digest_enabled", true)
        set(v) {
            prefs.edit { putBoolean("daily_digest_enabled", v) }
        }

    fun saveDailyDigest(enabled: Boolean) {
        isDailyDigestEnabled = enabled
    }

    var isMonthlySummaryEnabled: Boolean
        get() = prefs.getBoolean("monthly_summary_enabled", true)
        set(v) {
            prefs.edit { putBoolean("monthly_summary_enabled", v) }
        }

    fun saveMonthlySummary(enabled: Boolean) {
        isMonthlySummaryEnabled = enabled
    }

    var isUpcomingReminderEnabled: Boolean
        get() = prefs.getBoolean("upcoming_reminder_enabled", true)
        set(v) {
            prefs.edit { putBoolean("upcoming_reminder_enabled", v) }
        }

    fun saveUpcomingReminder(enabled: Boolean) {
        isUpcomingReminderEnabled = enabled
    }

    var upcomingReminderDaysBefore: Int
        get() = prefs.getInt("upcoming_reminder_days", 3)
        set(v) {
            prefs.edit { putInt("upcoming_reminder_days", v) }
        }

    fun saveUpcomingReminderDays(days: Int) {
        upcomingReminderDaysBefore = days
    }

    private fun monthlyTargetKey(month: YearMonth): String {
        return "$MONTHLY_TARGET_KEY_PREFIX${month}"
    }

    fun getMonthlySpendingTarget(month: YearMonth): Double? {
        return prefs.getString(monthlyTargetKey(month), null)?.toDoubleOrNull()
    }

    fun hasMonthlySpendingTarget(month: YearMonth): Boolean {
        return getMonthlySpendingTarget(month) != null
    }

    fun saveMonthlySpendingTarget(month: YearMonth, amount: Double) {
        require(amount > 0.0) { "Monthly spending target must be greater than zero." }
        prefs.edit { putString(monthlyTargetKey(month), amount.toString()) }
    }

    private const val PREFS_NAME = "pesa_prefs"
    private const val MONTHLY_TARGET_KEY_PREFIX = "monthly_target_"

    fun formatCurrency(amount: Double): String {
        return "$CURRENCY ${formatAmount(amount)}"
    }

    fun getTimeFormatter(): DateTimeFormatter {
        val pattern = if (use24HrsFormat) "HH:mm" else "h:mm a"
        return DateTimeFormatter.ofPattern(pattern, Locale.US)
    }

    fun getDateFormatter(): DateTimeFormatter {
        return DateTimeFormatter.ofPattern(datePattern, Locale.US)
    }

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        use24HrsFormat = prefs.getBoolean("use_24h", false)
        isBiometricEnabled = prefs.getBoolean("biometric_on", false)
        isDarkTheme = prefs.getBoolean("dark_theme", true)
        val dayName = prefs.getString("start_day", DayOfWeek.SUNDAY.name)
        firstDayOfWeek = DayOfWeek.valueOf(dayName ?: DayOfWeek.SUNDAY.name)
        // Notification prefs (daily digest, reminder time) are read lazily by their
        // getters — do NOT write them back here: an earlier version round-tripped
        // them through setters with mismatched keys, force-disabling the digest and
        // resetting the time to 08:00 on every launch. Clear those bug-written
        // values once so the intended defaults (enabled, evening) apply again.
        if (!prefs.getBoolean("notif_prefs_migrated", false)) {
            prefs.edit {
                remove("daily_digest_enabled")
                if (prefs.getInt("reminder_hour", -1) == 8 &&
                    prefs.getInt("reminder_minute", -1) == 0
                ) {
                    remove("reminder_hour")
                    remove("reminder_minute")
                }
                putBoolean("notif_prefs_migrated", true)
            }
        }
    }

    var linkedGoogleEmail: String?
        get() = prefs.getString("linked_google_email", null)
        set(value) {
            prefs.edit {
                if (value != null) putString(
                    "linked_google_email",
                    value
                ) else remove("linked_google_email")
            }
        }

    fun saveTimeFormat(enabled: Boolean) {
        use24HrsFormat = enabled
        prefs.edit { putBoolean("use_24h", enabled) }
    }

    fun saveTheme(isDark: Boolean) {
        isDarkTheme = isDark
        prefs.edit { putBoolean("dark_theme", isDark) }
    }

    fun saveBiometric(enabled: Boolean) {
        isBiometricEnabled = enabled
        prefs.edit { putBoolean("biometric_on", enabled) }
    }

    fun saveFirstDayOfWeek(day: DayOfWeek) {
        firstDayOfWeek = day
        prefs.edit { putString("start_day", day.name) }
    }

    fun screenSubtitle(route: String?): String = when (route) {
        AppDestinations.Dashboard.route -> "Your financial pulse, today"
        AppDestinations.Transactions.route -> "Full transaction history"
        AppDestinations.Calendar.route -> "Activity across the month"
        AppDestinations.Analytics.route -> "Breakdown by period & category"
        AppDestinations.SummaryReport.route -> "Daily & monthly reports"
        AppDestinations.UpcomingPayments.route -> "Manage your scheduled bills and income"
        AppDestinations.UnmatchedReview.route -> "Confirm or ignore unrecognised messages"
        else -> ""
    }

    fun screenTitle(route: String?): String = when (route) {
        AppDestinations.Dashboard.route -> "Home"
        AppDestinations.Transactions.route -> "History"
        AppDestinations.Calendar.route -> "Calendar"
        AppDestinations.Analytics.route -> "Insights"
        AppDestinations.Settings.route -> "Settings"
        AppDestinations.Categories.route -> "Categories"
        AppDestinations.SummaryReport.route -> "Summary"
        AppDestinations.UpcomingPayments.route -> "Upcoming Payments"
        AppDestinations.UnmatchedReview.route -> "Review Unmatched SMS"
        else -> "PesaPulse"
    }

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
            "v${packageInfo.versionName}"
        } catch (_: Exception) {
            "v1.0.0"
        }
    }
}
