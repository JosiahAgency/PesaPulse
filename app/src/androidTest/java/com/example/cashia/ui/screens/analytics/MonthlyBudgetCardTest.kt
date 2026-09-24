package com.example.cashia.ui.screens.analytics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.cashia.models.BudgetProgress
import com.example.cashia.ui.theme.DarkColorScheme
import com.example.cashia.ui.theme.LightColorScheme
import com.example.cashia.utility.MonthlyBudget
import com.example.cashia.utility.PesaTimeConfiguration
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

class MonthlyBudgetCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun darkCardKeepsOverspendingAndMissingFeeWarningVisibleAtLargeText() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 1.8f)) {
                MaterialTheme(colorScheme = DarkColorScheme) {
                    Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                        MonthlyBudgetCard(
                            BudgetProgress(MonthlyBudget(YearMonth.of(2026, 9), 1000.0, 500.0), 1200.0, -200.0, 1)
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("MONTHLY BUDGET").assertIsDisplayed()
        compose.onNodeWithText("${PesaTimeConfiguration.formatCurrency(200.0)} over budget")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Savings goal: ${PesaTimeConfiguration.formatCurrency(500.0)}")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fees missing on 1 transaction. Totals are incomplete.")
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun lightCardPreservesUnsetTargetsAndFullMonthContext() {
        compose.setContent {
            MaterialTheme(colorScheme = LightColorScheme) {
                Column(Modifier.width(320.dp).verticalScroll(rememberScrollState())) {
                    MonthlyBudgetCard(
                        BudgetProgress(MonthlyBudget(YearMonth.of(2026, 9), null, null), 0.0, 0.0, 0)
                    )
                }
            }
        }
        compose.onNodeWithText("September 2026 · Full month").assertIsDisplayed()
        compose.onNodeWithText("Spending cap: Not set").assertIsDisplayed()
        compose.onNodeWithText("Savings goal: Not set").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Income minus spending").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Totals are incomplete.", substring = true).assertDoesNotExist()
    }
}
