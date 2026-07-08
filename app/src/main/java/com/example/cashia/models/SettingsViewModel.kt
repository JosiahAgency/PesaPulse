package com.example.cashia.models

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@SuppressLint("StaticFieldLeak")
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        fun provideFactory(app: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.AndroidViewModelFactory(app) {}
        }
    }

    private val context = getApplication<Application>().applicationContext

    val darkTheme = snapshotFlow { PesaTimeConfiguration.isDarkTheme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.isDarkTheme)

    val use24Hour = snapshotFlow { PesaTimeConfiguration.use24HrsFormat }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.use24HrsFormat)

    val firstDayOfWeek = snapshotFlow { PesaTimeConfiguration.firstDayOfWeek }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.firstDayOfWeek)

    val isBiometricEnabled = snapshotFlow { PesaTimeConfiguration.isBiometricEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.isBiometricEnabled)

    val isDailyDigestEnabled = snapshotFlow { PesaTimeConfiguration.isDailyDigestEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.isDailyDigestEnabled)

    val isMonthlySummaryEnabled = snapshotFlow { PesaTimeConfiguration.isMonthlySummaryEnabled }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PesaTimeConfiguration.isMonthlySummaryEnabled
        )

    val isUpcomingReminderEnabled = snapshotFlow { PesaTimeConfiguration.isUpcomingReminderEnabled }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PesaTimeConfiguration.isUpcomingReminderEnabled
        )

    val upcomingReminderDays = snapshotFlow { PesaTimeConfiguration.upcomingReminderDaysBefore }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PesaTimeConfiguration.upcomingReminderDaysBefore
        )


    private val _linkedGoogleEmail = MutableStateFlow<String?>(
        PesaTimeConfiguration.linkedGoogleEmail
    )

    val linkedGoogleEmail: StateFlow<String?> = _linkedGoogleEmail.asStateFlow()

    fun linkGoogleAccount(email: String) {
        setLinkedGoogleEmail(email)
    }

    fun unlinkGoogleAccount() {
        setLinkedGoogleEmail(null)
    }

    fun setLinkedGoogleEmail(email: String?) {
        _linkedGoogleEmail.value = email
        PesaTimeConfiguration.linkedGoogleEmail = email
    }

    fun setDarkTheme(enabled: Boolean) {
        PesaTimeConfiguration.saveTheme(enabled)
    }

    fun setUse24Hour(enabled: Boolean) {
        PesaTimeConfiguration.saveTimeFormat(enabled)
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        PesaTimeConfiguration.saveFirstDayOfWeek(day)
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            PesaTimeConfiguration.saveBiometric(enabled)
        }
    }

    fun setDailyDigestEnabled(enabled: Boolean) {
        PesaTimeConfiguration.saveDailyDigest(enabled)
        if (enabled) NotificationScheduler.scheduleDailyDigest(context)
        else WorkManager.getInstance(context).cancelUniqueWork(DailyDigestWorker.WORK_NAME)
    }

    fun setMonthlySummaryEnabled(enabled: Boolean) {
        PesaTimeConfiguration.saveMonthlySummary(enabled)
        if (enabled) NotificationScheduler.scheduleMonthly(context)
        else WorkManager.getInstance(context).cancelUniqueWork(MonthlySummaryWorker.WORK_NAME)
    }

    fun setUpcomingReminderEnabled(enabled: Boolean) {
        PesaTimeConfiguration.saveUpcomingReminder(enabled)
        if (enabled) NotificationScheduler.scheduleUpcomingReminders(context)
        else WorkManager.getInstance(context).cancelUniqueWork(UpcomingReminderWorker.WORK_NAME)
    }

    fun setUpcomingReminderDays(days: Int) {
        PesaTimeConfiguration.saveUpcomingReminderDays(days)
        // Re-schedule with new window if active
        if (PesaTimeConfiguration.isUpcomingReminderEnabled)
            NotificationScheduler.scheduleUpcomingReminders(context)
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        PesaTimeConfiguration.saveReminderTime(hour, minute)
        if (PesaTimeConfiguration.isDailyDigestEnabled)      NotificationScheduler.scheduleDailyDigest(context)
        if (PesaTimeConfiguration.isUpcomingReminderEnabled) NotificationScheduler.scheduleUpcomingReminders(context)
    }
}