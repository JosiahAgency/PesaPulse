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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),       // Lighter Blue for contrast on dark surface
    secondary = Color(0xFFCFD8DC),     // Light Gray/Slate
    tertiary = Color(0xFFA5D6A7),      // Muted Green for subtle positive accent

    background = Color(0xFF121212),    // Near Black
    surface = Color(0xFF1E1E1E),       // Dark Gray/Surface background

    onPrimary = Color(0xFF1565C0),     // Deep Blue text/icons on primary
    onSecondary = Color(0xFF263238),   // Dark Blue-Gray text/icons
    onTertiary = Color(0xFF1B5E20),    // Dark Green text/icons

    onBackground = Color(0xFFE0E0E0),  // Light Gray text on background
    onSurface = Color(0xFFE0E0E0),     // Light Gray text on surface

    surfaceVariant = Color(0xFF424242),   // Medium-Dark Gray
    onSurfaceVariant = Color(0xFFBDBDBD)  // Medium Gray text
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),       // Deep Professional Blue (Trustworthy)
    secondary = Color(0xFF546E7A),     // Muted Blue-Gray (Subtle secondary info)
    tertiary = Color(0xFF4CAF50),      // Standard Green (For success/positive actions)

    background = Color(0xFFFFFFFF),    // Pure White
    surface = Color(0xFFF9F9F9),       // Slight Off-White/Light Gray for surface distinction

    onPrimary = Color.White,           // White text on deep blue
    onSecondary = Color.White,         // White text on blue-gray
    onTertiary = Color.White,          // White text on green

    onBackground = Color(0xFF1C1B1F),  // Near Black text on background
    onSurface = Color(0xFF1C1B1F),     // Near Black text on surface

    surfaceVariant = Color(0xFFE0E0E0),   // Light Gray for elements/dividers
    onSurfaceVariant = Color(0xFF424242)  // Dark Gray text on surface variant
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


    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}