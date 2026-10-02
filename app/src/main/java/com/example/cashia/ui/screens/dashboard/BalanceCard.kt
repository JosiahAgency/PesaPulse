package com.example.cashia.ui.screens.dashboard

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.StatPill
import com.example.cashia.ui.reusedElements.StatPillTone
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.reusedElements.UnknownFeesNotice
import com.example.cashia.ui.theme.CurrencyAmountStyle
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.PesaTimeConfiguration

@Composable
fun BalanceHeroCard(
    balance: Double?,
    latestBalanceDate: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    income: Double,
    expenses: Double,
    isLoading: Boolean = false,
    unknownFeeCount: Int = 0,
) {
    GlassySurface(modifier = Modifier.fillMaxWidth(), glow = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            SectionLabel("current Balance")
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Crossfade(
                    targetState = if (isLoading) BalanceDisplay.LOADING
                    else if (isVisible) BalanceDisplay.AMOUNT else BalanceDisplay.HIDDEN,
                    label = "balance_visibility"
                ) { display ->
                    when (display) {
                        // Matches the spinner the transaction list below shows while Room's
                        // first emission is still pending — "Unknown" now means "loaded, but
                        // no balance in the SMS", never "still loading".
                        BalanceDisplay.LOADING -> CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )

                        else -> Text(
                            text = if (display == BalanceDisplay.AMOUNT)
                                balance?.let { PesaTimeConfiguration.formatCurrency(it) }
                                    ?: "Unknown"
                            else
                                "••••••",
                            style = MaterialTheme.typography.displaySmall.copy(
                                brush = primaryGradient(),
                                letterSpacing = 0.5.sp
                            ).merge(CurrencyAmountStyle)
                        )
                    }
                }
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle balance visibility",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Subtitles(
                if (isLoading) "Loading latest balance…"
                else "Latest Updated: $latestBalanceDate"
            )
            UnknownFeesNotice(unknownFeeCount)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPill(
                    label = "Income",
                    amount = pillAmount(income, isVisible, isLoading),
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    tone = StatPillTone.INCOME,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Expenses",
                    amount = pillAmount(expenses, isVisible, isLoading),
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    tone = StatPillTone.EXPENSE,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Private mode hides the balance; period totals are just as sensitive, so they mask too.
 * A zero total is only shown once the period has actually loaded, matching the list below.
 */
private fun pillAmount(amount: Double, isVisible: Boolean, isLoading: Boolean): String = when {
    isLoading -> "—"
    !isVisible -> "••••"
    else -> PesaTimeConfiguration.formatCurrency(amount)
}

private enum class BalanceDisplay { LOADING, AMOUNT, HIDDEN }
