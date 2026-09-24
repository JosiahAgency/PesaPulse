package com.example.cashia.models

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.flow.SharingStarted
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
    val reminderTime = PesaTimeConfiguration.observe {
        PesaTimeConfiguration.reminderHour to PesaTimeConfiguration.reminderMinute
    }.stateIn(
        viewModelScope, SharingStarted.Eagerly,
        PesaTimeConfiguration.reminderHour to PesaTimeConfiguration.reminderMinute
    )

    val darkTheme = PesaTimeConfiguration.observe { PesaTimeConfiguration.isDarkTheme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.isDarkTheme)

    val use24Hour = PesaTimeConfiguration.observe { PesaTimeConfiguration.use24HrsFormat }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.use24HrsFormat)

//    val firstDayOfWeek = PesaTimeConfiguration.observe { PesaTimeConfiguration.firstDayOfWeek }
//        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.firstDayOfWeek)
//
//    val isBiometricEnabled =
//        PesaTimeConfiguration.observe { PesaTimeConfiguration.isBiometricEnabled }
//            .stateIn(
//                viewModelScope,
//                SharingStarted.Eagerly,
//                PesaTimeConfiguration.isBiometricEnabled
//            )

    val isDailyDigestEnabled =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.isDailyDigestEnabled }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.isDailyDigestEnabled
            )

    val isMonthlySummaryEnabled =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.isMonthlySummaryEnabled }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.isMonthlySummaryEnabled
            )

    val isUpcomingReminderEnabled =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.isUpcomingReminderEnabled }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.isUpcomingReminderEnabled
            )

    val upcomingReminderDays =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.upcomingReminderDaysBefore }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.upcomingReminderDaysBefore
            )

    val isLowBalanceAlertEnabled =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.isLowBalanceAlertEnabled }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.isLowBalanceAlertEnabled
            )

    val lowBalanceThreshold =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.lowBalanceThreshold }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.lowBalanceThreshold
            )

    val isMonthlyTargetReminderEnabled =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.isMonthlyTargetReminderEnabled }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.isMonthlyTargetReminderEnabled
            )

    val monthlySpendingCap =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.monthlySpendingCap }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.monthlySpendingCap
            )

    val monthlySavingsGoal =
        PesaTimeConfiguration.observe { PesaTimeConfiguration.monthlySavingsGoal }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                PesaTimeConfiguration.monthlySavingsGoal
            )

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
        if (PesaTimeConfiguration.isDailyDigestEnabled) NotificationScheduler.scheduleDailyDigest(
            context
        )
        if (PesaTimeConfiguration.isUpcomingReminderEnabled) NotificationScheduler.scheduleUpcomingReminders(
            context
        )
    }

    fun setLowBalanceAlertEnabled(enabled: Boolean) {
        PesaTimeConfiguration.saveLowBalanceAlert(enabled)
    }

    fun setLowBalanceThreshold(amount: Double) {
        PesaTimeConfiguration.saveLowBalanceThreshold(amount)
    }

    fun setMonthlyTargetReminderEnabled(enabled: Boolean) {
        PesaTimeConfiguration.saveMonthlyTargetReminder(enabled)
        if (enabled) NotificationScheduler.scheduleMonthlyTargetReminder(context)
        else WorkManager.getInstance(context)
            .cancelUniqueWork(MonthlyTargetReminderWorker.WORK_NAME)
    }

    fun setMonthlyTargets(spendingCap: Double?, savingsGoal: Double?) {
        PesaTimeConfiguration.saveMonthlyTargets(spendingCap, savingsGoal)
    }
}