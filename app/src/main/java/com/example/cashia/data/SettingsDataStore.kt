package com.example.cashia.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

private val Context.dataStore by preferencesDataStore("settings")

class SettingsDataStore(private val context: Context) {
    companion object {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val USE_24HOUR = booleanPreferencesKey("use_24_hour")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_week")
    }

    val darkThemeFlow: Flow<Boolean> =
        context.dataStore.data.map { it[DARK_THEME] ?: false }

    val use24HourFlow: Flow<Boolean> =
        context.dataStore.data.map { it[USE_24HOUR] ?: false }

    val firstDayOfWeekFlow: Flow<DayOfWeek> =
        context.dataStore.data.map {
            DayOfWeek.valueOf(it[FIRST_DAY_OF_WEEK] ?: DayOfWeek.SUNDAY.name)
        }

    suspend fun setDarkTheme(value: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = value }
    }

    suspend fun setUse24Hour(value: Boolean) {
        context.dataStore.edit { it[USE_24HOUR] = value }
    }

    suspend fun setFirstDayOfWeek(day: DayOfWeek) {
        context.dataStore.edit { it[FIRST_DAY_OF_WEEK] = day.name }
    }
}
