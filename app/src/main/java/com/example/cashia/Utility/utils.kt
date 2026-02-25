package com.example.cashia.Utility

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.cashia.ui.screens.AllTransactions.formatAmount
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TimeScope {
    DAY, WEEK, MONTH, CUSTOM
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

    fun getGreeting(): String {
        return when (LocalTime.now().hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}

object PesaTimeConfiguration {
    var use24HrsFormat by mutableStateOf(false)
    var datePattern: String = "dd MMM yyyy"
    var firstDayOfWeek by mutableStateOf(DayOfWeek.SUNDAY)
    var isDarkTheme by mutableStateOf(false)
    const val CURRENCY = "KES"
    var isBiometricEnabled by mutableStateOf(false)
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
        isDarkTheme = prefs.getBoolean("dark_theme", false)
        val dayName = prefs.getString("start_day", DayOfWeek.SUNDAY.name)
        firstDayOfWeek = DayOfWeek.valueOf(dayName ?: DayOfWeek.SUNDAY.name)
    }

    // Individual save methods to be called by ViewModel
    fun saveTimeFormat(context: Context, enabled: Boolean) {
        use24HrsFormat = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("use_24h", enabled).apply()
    }

    fun saveTheme(context: Context, isDark: Boolean) {
        isDarkTheme = isDark
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("dark_theme", isDark).apply()
    }

    fun saveBiometric(context: Context, enabled: Boolean) {
        isBiometricEnabled = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("biometric_on", enabled).apply()
    }

    fun saveFirstDayOfWeek(context: Context, day: DayOfWeek) {
        firstDayOfWeek = day
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("start_day", day.name).apply()
    }
}

