package com.example.cashia.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cashia.ui.theme.DarkColorScheme
import com.example.cashia.ui.theme.LightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class IslandNavigationBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val paths = listOf(
        AppDestinations.Dashboard, AppDestinations.Transactions,
        AppDestinations.Calendar, AppDestinations.Analytics
    )

    @Test
    fun allTabsStayLabelledAndSelectionFollowsTaps() {
        compose.setContent {
            var route by rememberSaveable { mutableStateOf(paths.first().route) }
            MaterialTheme(colorScheme = DarkColorScheme) {
                NavigationDock(paths, route, { route = it.route })
            }
        }
        paths.forEach { screen ->
            compose.onNodeWithText(screen.label)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .assertWidthIsAtLeast(48.dp)
                .performClick()
                .assertIsSelected()
            paths.filter { it != screen }.forEach {
                compose.onNodeWithText(it.label).assertIsDisplayed().assertIsNotSelected()
            }
        }
    }

    @Test
    fun narrowDockSupportsLargeTextAndRtlWithoutClippingLabels() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                MaterialTheme(colorScheme = LightColorScheme) {
                    Box(Modifier.width(320.dp)) {
                        NavigationDock(paths, AppDestinations.Calendar.route, {})
                    }
                }
            }
        }
        paths.forEach { screen ->
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(screen.label).assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            assertEquals(1, results.size)
            assertFalse("${screen.label} should not be clipped", results.single().hasVisualOverflow)
        }
        compose.onNodeWithText("Calendar").assertIsSelected()
    }

    @Test
    fun switchingTabsRestoresStateAndReselectionDoesNotNavigateOrVibrate() {
        lateinit var nav: NavHostController
        var hapticCount = 0
        val haptic = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                hapticCount++
            }
        }
        compose.setContent {
            nav = rememberNavController()
            MaterialTheme(colorScheme = DarkColorScheme) {
                Surface(Modifier.fillMaxSize()) {
                    Column {
                        NavHost(nav, startDestination = paths.first().route, modifier = Modifier.weight(1f)) {
                            paths.forEach { screen ->
                                composable(screen.route) {
                                    var count by rememberSaveable { mutableIntStateOf(0) }
                                    Button(onClick = { count++ }) { Text("${screen.label} count: $count") }
                                }
                            }
                        }
                        IslandNavigationBar(haptic, nav, paths)
                    }
                }
            }
        }
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("History count: 0").performClick()
        compose.onNodeWithText("History count: 1").assertIsDisplayed()
        val entry = compose.runOnIdle { nav.currentBackStackEntry }
        compose.onNodeWithText("History").performClick()
        compose.runOnIdle {
            assertEquals(entry, nav.currentBackStackEntry)
            assertEquals(1, hapticCount)
        }
        compose.onNodeWithText("Calendar").performClick()
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("History count: 1").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(3, hapticCount)
            nav.popBackStack()
        }
        compose.onNodeWithText("Home").assertIsSelected()
    }
}
