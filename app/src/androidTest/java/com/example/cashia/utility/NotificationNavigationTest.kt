package com.example.cashia.utility

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.cashia.MainActivity
import org.junit.Rule
import org.junit.Test

class NotificationNavigationTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS
    )

    @Test fun coldAndWarmNotificationTapsOpenRequestedReportAndSurviveRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("pesa_prefs", Context.MODE_PRIVATE).edit().putBoolean("biometric_on", false).commit()
        context.getSharedPreferences("cashia_prefs", Context.MODE_PRIVATE).edit().putBoolean("first_run_notif", false).commit()
        fun reportIntent(date: String) = Intent(context, MainActivity::class.java)
            .putExtra(NotificationHelper.EXTRA_ROUTE, "summary_report/month/$date")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        fun awaitLabel(label: String) {
            compose.waitUntil(10000) { compose.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
        }
        ActivityScenario.launch<MainActivity>(reportIntent("2024-02-01")).use { scenario ->
            awaitLabel("February 2024")
            context.startActivity(reportIntent("2024-03-01"))
            awaitLabel("March 2024")
            compose.onNodeWithContentDescription("Previous period").performClick()
            awaitLabel("February 2024")
            context.startActivity(reportIntent("2024-03-01"))
            awaitLabel("March 2024")
            scenario.recreate()
            awaitLabel("March 2024")
        }
    }
}
