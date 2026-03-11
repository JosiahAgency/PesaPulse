package com.example.cashia.models

import android.app.Application
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cashia.Utility.PesaTimeConfiguration
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

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
}