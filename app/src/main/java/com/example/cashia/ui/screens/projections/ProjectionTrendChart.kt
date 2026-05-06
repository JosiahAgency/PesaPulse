package com.example.cashia.ui.screens.projections

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.FinanceProjection
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.ProjectionPeriod
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@SuppressLint("DefaultLocale")
@Composable
fun ProjectionTrendChart(
    projection: FinanceProjection,
    period: ProjectionPeriod,
    currentBalance: Double,
    confidence: Int
) {
    if (projection.dailyTrend.isEmpty()) return

    period.daysRemaining()

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    val progress = remember { Animatable(0f) }
    LaunchedEffect(projection) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }

    val allValues = listOf(currentBalance) + projection.dailyTrend.map { it.second }
    val bandFraction = (1f - confidence / 100f) * 0.25f
    val minVal = (allValues.minOrNull() ?: 0.0) * (1 - bandFraction)
    val maxVal = (allValues.maxOrNull() ?: 1.0) * (1 + bandFraction)
    val range = if (maxVal != minVal) maxVal - minVal else 1.0

    var crosshairIndex by remember { mutableIntStateOf(-1) }
    var crosshairX by remember { mutableFloatStateOf(-1f) }
    var chartWidth by remember { mutableFloatStateOf(1f) }

    val calloutDate = projection.dailyTrend.getOrNull(crosshairIndex - 1)?.first
    val calloutBalance = if (crosshairIndex == 0) currentBalance
    else projection.dailyTrend.getOrNull(crosshairIndex - 1)?.second

    val nearestUpcoming = remember(crosshairIndex, projection.upcomingMarkers) {
        if (crosshairIndex < 0) null
        else projection.upcomingMarkers.minByOrNull { (date, _, _) ->
            val idx = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
            kotlin.math.abs(idx - crosshairIndex)
        }?.takeIf { (date, _, _) ->
            val idx = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
            kotlin.math.abs(idx - crosshairIndex) <= 1
        }
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("Balance Trajectory")
                    Subtitles(
                        "${period.label} · Through ${period.endLabel()}",
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = primary.copy(alpha = 0.08f),
                    border = BorderStroke(0.5.dp, primary.copy(alpha = 0.2f))
                ) {
                    Text(
                        "${confidence}% conf.",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = primary, fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Callout box ───────────────────────────────────────────────
            if (crosshairIndex >= 0 && calloutBalance != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                calloutDate?.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))
                                    ?: "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                PesaTimeConfiguration.formatCurrency(calloutBalance),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    calloutBalance < 0 -> error
                                    calloutBalance < currentBalance * 0.5 -> Color(0xFFF59E0B)
                                    else -> primary
                                }
                            )
                        }
                        nearestUpcoming?.let { (_, _, title) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tertiary.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 6.dp
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("◆", fontSize = 9.sp, color = tertiary)
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = tertiary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        val change = calloutBalance - currentBalance
                        val changePct = if (currentBalance != 0.0)
                            change / currentBalance * 100 else 0.0
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "vs Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                "${if (change >= 0) "+" else ""}${
                                    PesaTimeConfiguration.formatCurrency(change)
                                } (${String.format("%.1f", changePct)}%)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (change >= 0) tertiary else error
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(64.dp))
            }

// Only showing the Canvas section (this is where all improvements are)
// Replace your Canvas {} block with this

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(allValues.size) {
                        detectDragGestures { change, _ ->
                            val n = allValues.size
                            if (n < 2) return@detectDragGestures
                            val idx = ((change.position.x / chartWidth) * (n - 1))
                                .toInt().coerceIn(0, n - 1)

                            crosshairIndex = idx
                            crosshairX = (idx.toFloat() / (n - 1)) * chartWidth
                        }
                    }
            ) {
                chartWidth = size.width
                val w = size.width
                val h = size.height

                fun valueToY(v: Double) =
                    (h - ((v - minVal) / range * h).toFloat()).coerceIn(0f, h)

                // ─────────────────────────────────────────────
                // 1. Build points
                // ─────────────────────────────────────────────
                val points = allValues.mapIndexed { i, v ->
                    Offset(i / (allValues.size - 1).toFloat() * w, valueToY(v))
                }

                val drawCount = (points.size * progress.value).toInt().coerceAtLeast(2)
                val visible = points.take(drawCount)

                // ─────────────────────────────────────────────
                // 2. Smooth curve (BEZIER)
                // ─────────────────────────────────────────────
                fun buildSmoothPath(pts: List<Offset>): Path {
                    val path = Path()
                    if (pts.size < 2) return path

                    path.moveTo(pts.first().x, pts.first().y)

                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val curr = pts[i]

                        val midX = (prev.x + curr.x) / 2
                        val midY = (prev.y + curr.y) / 2

                        path.quadraticBezierTo(prev.x, prev.y, midX, midY)
                    }

                    path.lineTo(pts.last().x, pts.last().y)
                    return path
                }

                val smoothPath = buildSmoothPath(visible)

                // ─────────────────────────────────────────────
                // 3. Confidence band (cleaner)
                // ─────────────────────────────────────────────
                val upper = visible.mapIndexed { i, p ->
                    Offset(p.x, valueToY(allValues[i] * (1 + bandFraction)))
                }
                val lower = visible.mapIndexed { i, p ->
                    Offset(p.x, valueToY(allValues[i] * (1 - bandFraction)))
                }

                drawPath(
                    path = Path().apply {
                        moveTo(upper.first().x, upper.first().y)
                        upper.drop(1).forEach { lineTo(it.x, it.y) }
                        lower.reversed().forEach { lineTo(it.x, it.y) }
                        close()
                    },
                    brush = Brush.verticalGradient(
                        listOf(
                            primary.copy(alpha = 0.10f),
                            primary.copy(alpha = 0.02f)
                        )
                    )
                )

                // ─────────────────────────────────────────────
                // 4. Area fill (stronger depth)
                // ─────────────────────────────────────────────
                drawPath(
                    path = Path().apply {
                        addPath(smoothPath)
                        lineTo(visible.last().x, h)
                        lineTo(visible.first().x, h)
                        close()
                    },
                    brush = Brush.verticalGradient(
                        listOf(
                            primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )

                // ─────────────────────────────────────────────
                // 5. Main line (thicker + premium)
                // ─────────────────────────────────────────────
                drawPath(
                    path = smoothPath,
                    color = primary,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // ─────────────────────────────────────────────
                // 6. Zero line
                // ─────────────────────────────────────────────
                if (minVal < 0) {
                    val zeroY = valueToY(0.0)
                    drawLine(
                        error.copy(alpha = 0.5f),
                        Offset(0f, zeroY),
                        Offset(w, zeroY),
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                    )
                }

                // ─────────────────────────────────────────────
                // 7. Upcoming markers (cleaner + aligned)
                // ─────────────────────────────────────────────
                projection.upcomingMarkers.forEach { (date, balance, _) ->
                    val idx = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
                    if (idx in 1 until points.size) {
                        val x = points.getOrNull(idx)?.x ?: return@forEach
                        val y = valueToY(balance)

                        drawCircle(
                            color = tertiary,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                // ─────────────────────────────────────────────
                // 8. Crosshair (much cleaner)
                // ─────────────────────────────────────────────
                if (crosshairIndex in points.indices) {
                    val cx = points[crosshairIndex].x
                    val cy = points[crosshairIndex].y

                    drawLine(
                        onSurface.copy(alpha = 0.15f),
                        Offset(cx, 0f),
                        Offset(cx, h),
                        1.2.dp.toPx()
                    )

                    drawCircle(surfaceColor, 7.dp.toPx(), Offset(cx, cy))
                    drawCircle(primary, 5.dp.toPx(), Offset(cx, cy))
                    drawCircle(Color.White, 2.dp.toPx(), Offset(cx, cy))

                    // Floating label (smarter positioning)
                    val label = PesaTimeConfiguration.formatCurrency(
                        if (crosshairIndex == 0) currentBalance
                        else projection.dailyTrend[crosshairIndex - 1].second
                    )

                    val paint = android.graphics.Paint().apply {
                        color = primary.toArgb()
                        textSize = 10.sp.toPx()
                        isFakeBoldText = true
                    }

                    val textWidth = paint.measureText(label)

                    val lx = when {
                        cx < w * 0.3f -> cx + 12.dp.toPx()
                        cx > w * 0.7f -> cx - textWidth - 12.dp.toPx()
                        else -> cx - textWidth / 2
                    }

                    val ly = (cy - 14.dp.toPx()).coerceAtLeast(12.dp.toPx())

                    drawContext.canvas.nativeCanvas.drawText(label, lx, ly, paint)
                }
            }

            // ── X-axis labels — "Today" → period end date ─────────────────
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Subtitles("Today")
                Subtitles(period.endLabel())
            }

            if (projection.upcomingMarkers.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        val diamond = Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width, size.height / 2f)
                            lineTo(size.width / 2f, size.height)
                            lineTo(0f, size.height / 2f); close()
                        }
                        drawPath(diamond, color = tertiary)
                    }
                    Subtitles("Scheduled Payments")
                }
            }
        }
    }
}