package com.example.cashia.utility

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth
import kotlin.time.Duration.Companion.milliseconds

class ObservableBudgetTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val prefs =
        context.getSharedPreferences("reliability-preferences-test", Context.MODE_PRIVATE)
    private val isolated = object : ContextWrapper(context) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = prefs
    }

    @Before
    fun setUp() {
        prefs.edit().clear().putBoolean("notif_prefs_migrated", true).commit()
    }

    @After
    fun tearDown() {
        PesaTimeConfiguration.initialize(context)
        prefs.edit().clear().commit()
    }

    @Test
    fun legacyBudgetMigrationPreservesMonthlyTargetAndDoesNotRepeatIntoNextMonth() {
        val month = YearMonth.now()
        prefs.edit().putString("monthly_target_$month", "12500.25")
            .putFloat("monthly_spending_cap", 10000f).putFloat("monthly_savings_goal", 3000f)
            .commit()
        PesaTimeConfiguration.initialize(isolated)
        assertEquals(
            MonthlyBudget(month, 12500.25, 3000.0),
            PesaTimeConfiguration.getMonthlyBudget()
        )
        assertTrue(PesaTimeConfiguration.hasMonthlyTargetSet)
        assertNull(PesaTimeConfiguration.getMonthlyBudget(month.plusMonths(1)).spendingCap)
        PesaTimeConfiguration.saveMonthlyTargets(null, null)
        PesaTimeConfiguration.initialize(isolated)
        assertFalse(PesaTimeConfiguration.hasMonthlyTargetSet)
    }

    @Test
    fun settingsAndDashboardWritesUseTheSameObservableMonthlyBudget() = runBlocking {
        PesaTimeConfiguration.initialize(isolated)
        val changed = async(Dispatchers.Unconfined) {
            withTimeout(5000.milliseconds) {
                PesaTimeConfiguration.observe { PesaTimeConfiguration.getMonthlyBudget() }.drop(1)
                    .first()
            }
        }
        instrumentation.runOnMainSync { PesaTimeConfiguration.saveMonthlyTargets(15000.75, 2000.0) }
        assertEquals(15000.75, changed.await().spendingCap!!, 0.0)
        assertEquals(
            15000.75,
            PesaTimeConfiguration.getMonthlySpendingTarget(YearMonth.now())!!,
            0.0
        )
        instrumentation.runOnMainSync {
            PesaTimeConfiguration.saveMonthlySpendingTarget(
                YearMonth.now(),
                16000.0
            )
        }
        assertEquals(16000.0, PesaTimeConfiguration.monthlySpendingCap!!, 0.0)
        assertEquals(2000.0, PesaTimeConfiguration.monthlySavingsGoal!!, 0.0)
    }

    @Test
    fun preferenceListenerObservesExternalSwitchAndTimeChangesAfterReinitialization() =
        runBlocking {
            PesaTimeConfiguration.initialize(isolated)
            PesaTimeConfiguration.initialize(isolated)
            val change = async(Dispatchers.Unconfined) {
                withTimeout(5000.milliseconds) {
                    PesaTimeConfiguration.observe { PesaTimeConfiguration.isDailyDigestEnabled to PesaTimeConfiguration.reminderHour }
                        .drop(1).first()
                }
            }
            instrumentation.runOnMainSync {
                prefs.edit().putBoolean("daily_digest_enabled", false).putInt("reminder_hour", 17)
                    .commit()
            }
            assertEquals(false to 17, change.await())
        }
}
