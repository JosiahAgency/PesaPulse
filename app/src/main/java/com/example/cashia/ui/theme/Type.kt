package com.example.cashia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Reserved for currency amounts (balances, totals, predicted figures) — tabular figures so
 * digits align across rows/cards, merged onto each site's existing type style rather than
 * replacing it (e.g. `MaterialTheme.typography.displaySmall.merge(CurrencyAmountStyle)`).
 */
val CurrencyAmountStyle = TextStyle(
    fontWeight = FontWeight.Black,
    fontFeatureSettings = "tnum"
)