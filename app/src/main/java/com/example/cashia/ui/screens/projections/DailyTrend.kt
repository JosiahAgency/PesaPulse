package com.example.cashia.ui.screens.projections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.models.FinanceProjection
import com.example.cashia.ui.reusedElements.GlassySurface
import java.time.format.DateTimeFormatter
import kotlin.math.abs


@Composable
fun DailyTrendMiniList(projection: FinanceProjection) {
    if (projection.dailyTrend.isEmpty()) return

    val preview = projection.dailyTrend.take(7)
    val maxAbs = preview.maxOfOrNull { abs(it.second) } ?: 1.0

    // Compute day-over-day delta (use estimatedTodayBalance as day 0 baseline)
    val startBalance = projection.estimatedTodayBalance
    val deltas = preview.mapIndexed { i, (_, bal) ->
        val prev = if (i == 0) startBalance else preview[i - 1].second
        bal - prev
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Next 7 Days — Projected Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Δ daily",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            Spacer(Modifier.height(12.dp))

            preview.forEachIndexed { i, (date, balance) ->
                if (i > 0) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )

                val positive = balance >= 0
                val barColor =
                    if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                val barFraction = (abs(balance) / maxAbs).toFloat().coerceIn(0.02f, 1f)
                val delta = deltas[i]
                val deltaColor =
                    if (delta >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date label
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(100.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor.copy(alpha = 0.7f))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Balance
                    Text(
                        PesaTimeConfiguration.formatCurrency(balance),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor,
                        modifier = Modifier.width(72.dp),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.width(6.dp))
                    // Delta badge
                    Text(
                        "${if (delta >= 0) "+" else ""}${
                            PesaTimeConfiguration.formatCurrency(
                                delta
                            )
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = deltaColor.copy(alpha = 0.75f),
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
            }
        }
    }
}