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
    var use24HrsFormat: Boolean = false
    var datePattern: String = "dd MMM yyyy"

    var firstDayOfWeek by mutableStateOf(DayOfWeek.SUNDAY)

    const val CURRENCY = "KES"

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
}