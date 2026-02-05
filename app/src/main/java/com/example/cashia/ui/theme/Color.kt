package com.example.cashia.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),          // Indigo Glow (More vibrant)
    onPrimary = Color.White,
    secondary = Color(0xFF94A3B8),        // Slate Blue (For glassy borders/subtitles)
    onSecondary = Color.White,
    tertiary = Color(0xFF10B981),         // Emerald Green (Crisp Income)
    onTertiary = Color.White,
    error = Color(0xFFF43F5E),            // Rose Pink (Sophisticated Expense)
    onError = Color.White,

    background = Color(0xFFF8FAFC),       // Clean Slate White
    onBackground = Color(0xFF0F172A),     // Deep Navy Text
    surface = Color.White.copy(alpha = 0.9f), // Base for glass cards
    onSurface = Color(0xFF0F172A),

    surfaceVariant = Color(0xFFE2E8F0),   // Glass Stroke color
    onSurfaceVariant = Color(0xFF64748B)
)
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),          // Soft Electric Indigo
    onPrimary = Color(0xFF1E1B4B),
    secondary = Color(0xFF475569),        // Muted Slate
    onSecondary = Color(0xFFF1F5F9),
    tertiary = Color(0xFF34D399),         // Neon Mint Glow
    onTertiary = Color(0xFF064E3B),
    error = Color(0xFFFB7185),            // Soft Watermelon
    onError = Color(0xFF4C0519),

    background = Color(0xFF0F172A),       // Deep Navy (Better than obsidian for glass)
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),          // Elevated Slate
    onSurface = Color(0xFFF1F5F9),

    surfaceVariant = Color(0xFF334155),   // Border for dark glass
    onSurfaceVariant = Color(0xFF94A3B8)
)
