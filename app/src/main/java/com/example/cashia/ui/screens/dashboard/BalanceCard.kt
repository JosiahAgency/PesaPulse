package com.example.cashia.ui.screens.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.PesaTimeConfiguration

@Composable
fun BalanceHeroCard(
    balance: Double,
    latestBalanceDate: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            SectionLabel("current Balance")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleVisibility() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Crossfade(targetState = isVisible) { visible ->
                    Text(
                        text = if (visible)
                            PesaTimeConfiguration.formatCurrency(balance)
                        else
                            "••••••",
                        style = MaterialTheme.typography.displaySmall.copy(
                            brush = primaryGradient(),
                            letterSpacing = 0.5.sp
                        ),
                        fontWeight = FontWeight.Black,
                    )
                }

                Icon(
                    if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    "Toggle Visibility",
                    Modifier.clickable { onToggleVisibility() }
                )
            }
            Spacer(Modifier.height(4.dp))
            Subtitles("Latest Updated: $latestBalanceDate")
        }
    }
}