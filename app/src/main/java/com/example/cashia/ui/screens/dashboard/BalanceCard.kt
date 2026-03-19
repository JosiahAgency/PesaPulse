package com.example.cashia.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleVisibility() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isVisible)
                        PesaTimeConfiguration.formatCurrency(balance)
                    else
                        "••••••",
                    style = MaterialTheme.typography.displaySmall.copy(
                        brush = primaryGradient()
                    ),
                    fontWeight = FontWeight.Black,
                )
                // Live indicator dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            brush = primaryGradient(),
                            shape = CircleShape
                        )
                )
            }
            Spacer(Modifier.height(4.dp))
            Subtitles("Balance as of Latest Transaction Dated: $latestBalanceDate")
        }
    }
}