package com.example.cashia.ui.screens.projections

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.FinanceProjection
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.StatPill
import com.example.cashia.ui.reusedElements.StatPillTone
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.theme.CurrencyAmountStyle
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.ProjectionPeriod


@SuppressLint("DefaultLocale")
@Composable
fun PredictedBalanceCard(
    currentBalance: Double,
    period: ProjectionPeriod,
    projection: FinanceProjection,
    confidence: Int,
    zeroCrossingDay: Int?
) {
    val colorScheme = MaterialTheme.colorScheme

    val balanceColor = when {
        projection.predictedBalance < 0 -> colorScheme.error
        projection.predictedBalance < currentBalance * 0.5 -> Color(0xFFF59E0B)
        else -> colorScheme.tertiary
    }

    val changePercent = if (currentBalance > 0)
        ((projection.predictedBalance - currentBalance) / currentBalance * 100) else 0.0

    val animatedBalance = remember { Animatable(0f) }
    LaunchedEffect(projection.predictedBalance) {
        animatedBalance.animateTo(
            projection.predictedBalance.toFloat(),
            tween(1200, easing = FastOutSlowInEasing)
        )
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Subtitles("Predicted Balance · ${period.label}")
            Text(
                "by ${period.endLabel()}",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                PesaTimeConfiguration.formatCurrency(animatedBalance.value.toDouble()),
                style = MaterialTheme.typography.displayMedium.merge(CurrencyAmountStyle),
                color = balanceColor
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (changePercent >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    null, tint = balanceColor, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Subtitles(
                    "${if (changePercent >= 0) "+" else ""}${
                        String.format("%.1f", changePercent)
                    }% From Today's Balance",
                )
            }
            if (zeroCrossingDay != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.error.copy(alpha = 0.1f),
                    border = BorderStroke(0.5.dp, colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Subtitles(
                            "Balance may reach zero in ~${zeroCrossingDay}d",
                            color = colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPill(
                    label = "Current",
                    amount = PesaTimeConfiguration.formatCurrency(currentBalance),
                    icon = Icons.Rounded.AccountBalanceWallet,
                    tone = StatPillTone.NEUTRAL,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Net Change",
                    amount = "${if (projection.predictedNet >= 0) "+" else ""}${
                        PesaTimeConfiguration.formatCurrency(projection.predictedNet)
                    }",
                    icon = if (projection.predictedNet >= 0) Icons.AutoMirrored.Filled.TrendingUp
                    else Icons.AutoMirrored.Filled.TrendingDown,
                    tone = if (projection.predictedNet >= 0) StatPillTone.INCOME else StatPillTone.EXPENSE,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(Modifier.height(12.dp))
            ConfidenceBar(confidence = confidence)
        }
    }
}

@Composable
private fun ConfidenceBar(confidence: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val barColor = when {
        confidence >= 70 -> colorScheme.tertiary
        confidence >= 45 -> Color(0xFFF59E0B)
        else -> colorScheme.error
    }
    val label = when {
        confidence >= 70 -> "High Confidence"
        confidence >= 45 -> "Moderate Confidence"
        else -> "Low Confidence — Limited Data"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Subtitles(
                label,
                color = barColor,
            )
            Text(
                "${confidence}%",
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { confidence / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f)
        )
    }
}