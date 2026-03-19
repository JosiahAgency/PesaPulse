package com.example.cashia.utility

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.edit
import com.example.cashia.ui.navigation.AppDestinations
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeScope {
    DAY, WEEK, MONTH, CUSTOM, YEAR
}

enum class TransactionType {
    INCOME, EXPENSE
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
    var isDailyDigestEnabled by mutableStateOf(false)
    var reminderHour by mutableIntStateOf(8)
    var reminderMinute by mutableIntStateOf(0)
    private const val PREFS_NAME = "pesa_prefs"

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

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        use24HrsFormat = prefs.getBoolean("use_24h", false)
        isBiometricEnabled = prefs.getBoolean("biometric_on", false)
        isDarkTheme = prefs.getBoolean("dark_theme", true)
        val dayName = prefs.getString("start_day", DayOfWeek.SUNDAY.name)
        firstDayOfWeek = DayOfWeek.valueOf(dayName ?: DayOfWeek.SUNDAY.name)
        isDailyDigestEnabled = prefs.getBoolean("daily_digest_on", false)
        reminderHour = prefs.getInt("reminder_hour", 8)
        reminderMinute = prefs.getInt("reminder_minute", 0)
    }

    // Individual save methods to be called by ViewModel
    fun saveTimeFormat(context: Context, enabled: Boolean) {
        use24HrsFormat = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean("use_24h", enabled) }
    }

    fun saveTheme(context: Context, isDark: Boolean) {
        isDarkTheme = isDark
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean("dark_theme", isDark) }
    }

    fun saveBiometric(context: Context, enabled: Boolean) {
        isBiometricEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean("biometric_on", enabled) }
    }

    fun saveFirstDayOfWeek(context: Context, day: DayOfWeek) {
        firstDayOfWeek = day
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString("start_day", day.name) }
    }

    fun saveDailyDigest(context: Context, enabled: Boolean) {
        isDailyDigestEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean("daily_digest_on", enabled) }
    }

    fun saveReminderTime(context: Context, hour: Int, minute: Int) {
        reminderHour = hour
        reminderMinute = minute
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt("reminder_hour", hour)
                putInt("reminder_minute", minute)
            }
    }


    fun screenSubtitle(route: String?): String = when (route) {
        AppDestinations.Dashboard.route -> "Your financial pulse, today"
        AppDestinations.Transactions.route -> "Full transaction history"
        AppDestinations.Calendar.route -> "Activity across the month"
        AppDestinations.Analytics.route -> "Breakdown by period & category"
        AppDestinations.Projections.route -> "30-day balance forecast"
        else -> ""
    }

    fun screenTitle(route: String?): String = when (route) {
        AppDestinations.Dashboard.route -> "Home"
        AppDestinations.Transactions.route -> "History"
        AppDestinations.Calendar.route -> "Calendar"
        AppDestinations.Analytics.route -> "Insights"
        AppDestinations.Projections.route -> "Forecast"
        AppDestinations.Settings.route -> "Settings"
        AppDestinations.Categories.route -> "Categories"
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
        } catch (e: Exception) {
            "v1.0.0"
        }
    }

}

