package com.example.cashia.models

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@SuppressLint("StaticFieldLeak")
class SettingsViewModel(app: Application) : AndroidViewModel(app) {
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

    fun setDarkTheme(enabled: Boolean) {
        PesaTimeConfiguration.saveTheme(context, enabled)
    }

    fun setUse24Hour(enabled: Boolean) {
        PesaTimeConfiguration.saveTimeFormat(context, enabled)
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        PesaTimeConfiguration.saveFirstDayOfWeek(context, day)
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            PesaTimeConfiguration.saveBiometric(context, enabled)
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        PesaTimeConfiguration.saveReminderTime(context, hour, minute)
        if (PesaTimeConfiguration.isDailyDigestEnabled) {
            DailyDigestWorker.schedule(context)
        }
    }

    fun setDailyDigestEnabled(enabled: Boolean) {
        PesaTimeConfiguration.saveDailyDigest(context, enabled)

        try {
            if (enabled) {
                DailyDigestWorker.schedule(context)
            } else {
                WorkManager.getInstance(context).cancelUniqueWork("daily_digest")
            }
        } catch (e: IllegalStateException) {
            // WorkManager not yet initialized — preference is saved, worker will be
            // scheduled on next app launch via MainActivity's DailyDigestWorker.schedule() call.
        }
    }
}