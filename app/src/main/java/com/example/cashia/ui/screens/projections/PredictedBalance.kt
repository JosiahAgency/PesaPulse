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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.ProjectionPeriod
import com.example.cashia.models.FinanceProjection
import com.example.cashia.ui.reusedElements.GlassySurface


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

            // Period label — "Predicted Balance · This Month"
            Text(
                "Predicted Balance · ${period.label}",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                "by ${period.endLabel()}",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                PesaTimeConfiguration.formatCurrency(animatedBalance.value.toDouble()),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
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
                Text(
                    "${if (changePercent >= 0) "+" else ""}${
                        String.format("%.1f", changePercent)
                    }% from today's balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            // ── Zero-crossing warning ─────────────────────────────────────────
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
                        Text(
                            "Balance may reach zero in ~${zeroCrossingDay}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))

            // ── Current / Net Change row ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(
                    "Current",
                    PesaTimeConfiguration.formatCurrency(currentBalance),
                    colorScheme.primary
                )
                VerticalDivider(
                    modifier = Modifier.height(36.dp),
                    color = colorScheme.onSurface.copy(alpha = 0.1f)
                )
                MiniStat(
                    "Net Change",
                    "${if (projection.predictedNet >= 0) "+" else ""}${
                        PesaTimeConfiguration.formatCurrency(projection.predictedNet)
                    }",
                    if (projection.predictedNet >= 0) colorScheme.tertiary else colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(Modifier.height(12.dp))

            // ── Confidence bar ────────────────────────────────────────────────
            ConfidenceBar(confidence = confidence)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
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
        confidence >= 70 -> "High confidence"
        confidence >= 45 -> "Moderate confidence"
        else -> "Low confidence — limited data"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
                fontWeight = FontWeight.SemiBold
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