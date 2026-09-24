package com.example.cashia.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared layout constants for the app's main scrollable screens. The floating "island" bottom
 * navigation dock renders on top of the four main tabs (see `ui/navigation/IslandNavigationBar.kt`),
 * so any tab with scrollable content needs enough bottom padding
 * to keep its last item from being obscured by it. Previously each screen hardcoded its own
 * value (32dp / 100dp / 120dp / 140dp) with no shared source of truth — use
 * [screenContentPadding] instead of a bespoke `PaddingValues(...)` so every screen clears the
 * nav bar by the same amount.
 */
val ScreenContentHorizontalPadding: Dp = 16.dp
val ScreenContentTopPadding: Dp = 115.dp

/** Clears the floating island nav bar (and its own bottom system-bar padding) on every screen. */
val ScreenContentBottomPadding: Dp = 120.dp

/** Measured dock height, excluding the system navigation inset supplied by its parent. */
val LocalNavigationDockHeight = compositionLocalOf { 102.dp }

@Composable
fun screenContentPadding(
    top: Dp = ScreenContentTopPadding,
    bottom: Dp = ScreenContentBottomPadding +
        (LocalNavigationDockHeight.current - 102.dp).coerceAtLeast(0.dp),
): PaddingValues = PaddingValues(
    start = ScreenContentHorizontalPadding,
    end = ScreenContentHorizontalPadding,
    top = top,
    bottom = bottom,
)
