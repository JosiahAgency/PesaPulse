package com.example.cashia.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cashia.data.SettingsDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class SettingsViewModel (app: Application): AndroidViewModel(app){
    private val store = SettingsDataStore(app)

    val darkTheme = store.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val use24Hour = store.use24HourFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val firstDayOfWeek = store.firstDayOfWeekFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, DayOfWeek.SUNDAY)

    fun setDarkTheme(value: Boolean) {
        viewModelScope.launch { store.setDarkTheme(value) }
    }

    fun setUse24Hour(value: Boolean) {
        viewModelScope.launch { store.setUse24Hour(value) }
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        viewModelScope.launch { store.setFirstDayOfWeek(day) }
    }
}