package com.example.cashia.models

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
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

    private val _linkedGoogleEmail = MutableStateFlow<String?>(
        PesaTimeConfiguration.linkedGoogleEmail
    )

    val linkedGoogleEmail: StateFlow<String?> = _linkedGoogleEmail.asStateFlow()
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

    fun updateReminderTime(hour: Int, minute: Int) {
        PesaTimeConfiguration.saveReminderTime(hour, minute)
        if (PesaTimeConfiguration.isDailyDigestEnabled) {
            DailyDigestWorker.schedule(context)
        }
    }

    fun setDailyDigestEnabled(enabled: Boolean) {
        PesaTimeConfiguration.saveDailyDigest(enabled)

        if (enabled) {
            DailyDigestWorker.schedule(context)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("daily_digest")
        }
    }
}