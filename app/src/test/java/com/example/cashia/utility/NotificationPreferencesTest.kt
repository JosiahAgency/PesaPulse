package com.example.cashia.utility

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NotificationPreferencesTest {
    private val context = mockk<Context>()
    private val prefs = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val values = mutableMapOf<String, Int>()

    @Before
    fun setUp() {
        every { context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getBoolean(any(), any()) } answers { secondArg() }
        every { prefs.getBoolean("notif_prefs_migrated", false) } returns true
        every { prefs.getBoolean("monthly_budget_migrated", false) } returns true
        every { prefs.registerOnSharedPreferenceChangeListener(any()) } returns Unit
        every { prefs.unregisterOnSharedPreferenceChangeListener(any()) } returns Unit
        every { prefs.getString(any(), any()) } answers { secondArg() }
        every { prefs.getInt(any(), any()) } answers { values[firstArg<String>()] ?: secondArg() }
        every { prefs.edit() } returns editor
        every { editor.putInt(any(), any()) } answers {
            values[firstArg()] = secondArg()
            editor
        }
        PesaTimeConfiguration.initialize(context)
    }

    @Test
    fun reminderTimePersistsBothFieldsAcrossInitialization() {
        PesaTimeConfiguration.saveReminderTime(7, 45)
        PesaTimeConfiguration.initialize(context)

        assertEquals(7, PesaTimeConfiguration.reminderHour)
        assertEquals(45, PesaTimeConfiguration.reminderMinute)
        verify(exactly = 1) { editor.apply() }
    }

    @Test
    fun reminderTimeSupportsMidnightAndEndOfDay() {
        for ((hour, minute) in listOf(0 to 0, 23 to 59)) {
            PesaTimeConfiguration.saveReminderTime(hour, minute)
            assertEquals(hour, PesaTimeConfiguration.reminderHour)
            assertEquals(minute, PesaTimeConfiguration.reminderMinute)
        }
    }

    @Test
    fun reminderWindowPersistsIncludingBoundaries() {
        for (days in listOf(0, 3, 30)) {
            PesaTimeConfiguration.saveUpcomingReminderDays(days)
            PesaTimeConfiguration.initialize(context)
            assertEquals(days, PesaTimeConfiguration.upcomingReminderDaysBefore)
        }
    }

    @Test
    fun invalidSettingsAreRejectedBeforeWriting() {
        for ((hour, minute) in listOf(-1 to 0, 24 to 0, 12 to -1, 12 to 60)) {
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                PesaTimeConfiguration.saveReminderTime(hour, minute)
            }
        }
        for (days in listOf(-1, 31)) {
            org.junit.Assert.assertThrows(IllegalArgumentException::class.java) {
                PesaTimeConfiguration.saveUpcomingReminderDays(days)
            }
        }
        verify(exactly = 0) { prefs.edit() }
    }
}
