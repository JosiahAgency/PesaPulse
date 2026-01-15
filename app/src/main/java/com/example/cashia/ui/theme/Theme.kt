package com.example.cashia.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    secondary = Color(0xFF455A64),
    onSecondary = Color.White,
    tertiary = IncomeGreen,             // For Income
    onTertiary = Color.White,
    error = ExpenseRed,                 // For Expenses
    onError = Color.White,

    background = Color.White,
    onBackground = Color(0xFF1A1C1E),
    surface = OffWhite,
    onSurface = Color(0xFF1A1C1E),

    surfaceVariant = Color(0xFFEBEEF0), // Lighter, cleaner card background
    onSurfaceVariant = Color(0xFF333333) // Much darker gray for better legibility
)

private val DarkColorScheme = darkColorScheme(
    primary = NavyLight,
    onPrimary = Color(0xFF003366),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF263238),
    tertiary = IncomeGreenDark,         // Accessible green on black
    onTertiary = Color(0xFF003300),
    error = ExpenseRedDark,             // Accessible red on black
    onError = Color(0xFF330000),

    background = Charcoal,
    onBackground = Color(0xFFE2E2E2),
    surface = CardGrayDark,
    onSurface = Color(0xFFE2E2E2),

    surfaceVariant = Color(0xFF37393B),
    onSurfaceVariant = Color(0xFFCFD1D3)
)


@Composable
fun PesaPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colors = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = Color.Transparent.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}