package com.example.cashia.ui.reusedElements

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.cashia.ui.theme.primaryGradient

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 1.5.sp,
            brush = primaryGradient()
        ),
        fontWeight = FontWeight.Black,
    )
}