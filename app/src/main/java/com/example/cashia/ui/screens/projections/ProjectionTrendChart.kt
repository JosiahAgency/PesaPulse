package com.example.cashia.ui.screens.projections

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.abs

@SuppressLint("DefaultLocale")
@Composable
fun ProjectionTrendChart(
    projection: FinanceProjection,
    period: ProjectionPeriod,
    currentBalance: Double,
    confidence: Int
) {
    if (projection.dailyTrend.isEmpty()) return

    // [FIX] Removed dead `period.daysRemaining()` call — result was never used.

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    // [PERF] Hoist Paint out of the Canvas draw scope so it's created once,
    // not allocated on every frame. textSize needs density, resolved here.
    val density = LocalDensity.current
    val labelPaint = remember(primary, density) {
        android.graphics.Paint().apply {
            color = primary.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            isFakeBoldText = true
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(projection) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }

    // [PERF] Wrapped in remember — was reconstructed (list + map) on every recomposition.
    val allValues = remember(projection, currentBalance) {
        buildList(projection.dailyTrend.size + 1) {
            add(currentBalance)
            projection.dailyTrend.mapTo(this) { it.second }
        }
    }

    val bandFraction = (1f - confidence / 100f) * 0.25f
    val minVal = (allValues.minOrNull() ?: 0.0) * (1 - bandFraction)
    val maxVal = (allValues.maxOrNull() ?: 1.0) * (1 + bandFraction)
    val range = if (maxVal != minVal) maxVal - minVal else 1.0

    // [FIX] Use absolute band width (fraction of value range) rather than
    // multiplying each value by ±fraction. Multiplying by fraction produces
    // inverted bands for negative balances: -100 * 1.1 = -110 puts "upper"
    // visually below "lower". Absolute band is always symmetric.
    val bandAbsolute = range * bandFraction

    // [PERF] Confidence band Offset lists hoisted into remember — were
    // being allocated inside the Canvas draw scope on every frame.
    val upperBand = remember(allValues, bandAbsolute) {
        allValues.mapIndexed { i, v ->
            val frac = i / (allValues.size - 1).toFloat()
            frac to v + bandAbsolute
        }
    }
    val lowerBand = remember(allValues, bandAbsolute) {
        allValues.mapIndexed { i, v ->
            val frac = i / (allValues.size - 1).toFloat()
            frac to v - bandAbsolute
        }
    }

    // [FIX] crosshairX removed — was set in drag but never read.
    // The Canvas draws using points[crosshairIndex].x directly.
    var crosshairIndex by remember { mutableIntStateOf(-1) }

    // [FIX] chartWidth now driven by onSizeChanged, not set inside the Canvas
    // draw scope. Setting it in the draw scope created a race: pointerInput
    // could fire before the first draw completed, reading the initial value of 1f.
    var chartWidthPx by remember { mutableFloatStateOf(1f) }

    val calloutDate = projection.dailyTrend.getOrNull(crosshairIndex - 1)?.first
    val calloutBalance = if (crosshairIndex == 0) currentBalance
    else projection.dailyTrend.getOrNull(crosshairIndex - 1)?.second

    // [FIX] nearestUpcoming converted to derivedStateOf so it recomputes only
    // when crosshairIndex or upcomingMarkers actually change, not on every
    // recomposition triggered by other state (e.g. progress animation).
    val nearestUpcoming by remember(projection.upcomingMarkers) {
        derivedStateOf {
            if (crosshairIndex < 0) null
            else projection.upcomingMarkers.minByOrNull { (date, _, _) ->
                val idx = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
                abs(idx - crosshairIndex)
            }?.takeIf { (date, _, _) ->
                val idx = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
                abs(idx - crosshairIndex) <= 1
            }
        }
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    SectionLabel("Balance Trajectory")
                    Subtitles("${period.label} · Through ${period.endLabel()}")
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
                        color = primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Callout box ────────────────────────────────────────────────
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
                                    // [FIX] Legend icon matches actual marker (circle + dot).
                                    // Original showed "◆" (diamond) but canvas draws circles.
                                    Canvas(modifier = Modifier.size(10.dp)) {
                                        drawCircle(color = tertiary, radius = size.minDimension / 2)
                                        drawCircle(
                                            color = Color.White,
                                            radius = size.minDimension / 4
                                        )
                                    }
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
                        val changePct =
                            if (currentBalance != 0.0) change / currentBalance * 100 else 0.0
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

            // ── Canvas chart ───────────────────────────────────────────────
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    // [FIX] Capture true canvas width via onSizeChanged instead of
                    // assigning inside the draw scope. Draw scope runs after layout
                    // so pointer input could fire first and read the stale 1f default.
                    .onSizeChanged { chartWidthPx = it.width.toFloat() }
                    // [FIX] Added tap support. Original only handled drag, so a user
                    // tapping a point got no feedback. Now tap + drag both set the crosshair.
                    .pointerInput(allValues.size) {
                        detectTapGestures { offset ->
                            val n = allValues.size
                            if (n < 2) return@detectTapGestures
                            crosshairIndex = ((offset.x / chartWidthPx) * (n - 1))
                                .toInt().coerceIn(0, n - 1)
                        }
                    }
                    .pointerInput(allValues.size) {
                        detectDragGestures { change, _ ->
                            val n = allValues.size
                            if (n < 2) return@detectDragGestures
                            crosshairIndex = ((change.position.x / chartWidthPx) * (n - 1))
                                .toInt().coerceIn(0, n - 1)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                fun valueToY(v: Double) =
                    (h - ((v - minVal) / range * h).toFloat()).coerceIn(0f, h)

                // 1. Build screen-space points
                val points = allValues.mapIndexed { i, v ->
                    Offset(i / (allValues.size - 1).toFloat() * w, valueToY(v))
                }
                val drawCount = (points.size * progress.value).toInt().coerceAtLeast(2)
                val visible = points.take(drawCount)

                // 2. Smooth curve — Catmull-Rom spline
                // [FIX] Replaced midpoint quadratic bezier with Catmull-Rom cubic bezier.
                // The old approach: moveTo first, then quadraticBezierTo(prev, midpoint)
                // produces a curve that passes through midpoints, not the actual data
                // points — it undershoots peaks and overshoots troughs. Catmull-Rom
                // passes through every point while still being smooth, using the
                // neighbouring points as tangent guides.
                val smoothPath = buildCatmullRomPath(visible)

                // 3. Confidence band
                // [PERF] Offset lists were allocated inside draw scope on every frame.
                // Now they're remembered above and we just map fractions → pixels here.
                val upperPts = upperBand.take(drawCount).map { (frac, v) ->
                    Offset(frac * w, valueToY(v))
                }
                val lowerPts = lowerBand.take(drawCount).map { (frac, v) ->
                    Offset(frac * w, valueToY(v))
                }

                drawPath(
                    path = Path().apply {
                        if (upperPts.isEmpty()) return@apply
                        moveTo(upperPts.first().x, upperPts.first().y)
                        upperPts.drop(1).forEach { lineTo(it.x, it.y) }
                        lowerPts.reversed().forEach { lineTo(it.x, it.y) }
                        close()
                    },
                    brush = Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.10f), primary.copy(alpha = 0.02f))
                    )
                )

                // 4. Area fill
                drawPath(
                    path = Path().apply {
                        addPath(smoothPath)
                        lineTo(visible.last().x, h)
                        lineTo(visible.first().x, h)
                        close()
                    },
                    brush = Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.25f), Color.Transparent)
                    )
                )

                // 5. Main line
                drawPath(
                    path = smoothPath,
                    color = primary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // 6. Zero line (only when balance goes negative)
                if (minVal < 0) {
                    val zeroY = valueToY(0.0)
                    drawLine(
                        color = error.copy(alpha = 0.5f),
                        start = Offset(0f, zeroY),
                        end = Offset(w, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                    )
                }

                // 7. Upcoming event markers
                projection.upcomingMarkers.forEach { (date, balance, _) ->
                    val idx = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
                    if (idx in 1 until points.size) {
                        val x = points.getOrNull(idx)?.x ?: return@forEach
                        val y = valueToY(balance)
                        drawCircle(color = tertiary, radius = 5.dp.toPx(), center = Offset(x, y))
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                // 8. Crosshair
                if (crosshairIndex in points.indices) {
                    val cx = points[crosshairIndex].x
                    val cy = points[crosshairIndex].y

                    drawLine(
                        color = onSurface.copy(alpha = 0.15f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, h),
                        strokeWidth = 1.2.dp.toPx()
                    )

                    drawCircle(surfaceColor, 7.dp.toPx(), Offset(cx, cy))
                    drawCircle(primary, 5.dp.toPx(), Offset(cx, cy))
                    drawCircle(Color.White, 2.dp.toPx(), Offset(cx, cy))

                    // Floating label — smart side-aware positioning
                    val label = PesaTimeConfiguration.formatCurrency(
                        if (crosshairIndex == 0) currentBalance
                        else projection.dailyTrend[crosshairIndex - 1].second
                    )
                    val textWidth = labelPaint.measureText(label)
                    val lx = when {
                        cx < w * 0.3f -> cx + 12.dp.toPx()
                        cx > w * 0.7f -> cx - textWidth - 12.dp.toPx()
                        else -> cx - textWidth / 2
                    }
                    val ly = (cy - 14.dp.toPx()).coerceAtLeast(12.dp.toPx())
                    drawContext.canvas.nativeCanvas.drawText(label, lx, ly, labelPaint)
                }
            }

            // ── X-axis labels ──────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Subtitles("Today")
                Subtitles(period.endLabel())
            }

            // ── Marker legend — icon matches actual drawn circles ──────────
            // [FIX] Was drawing a "◆" text character (diamond) but the canvas
            // draws filled circles with a white dot. Now both match.
            if (projection.upcomingMarkers.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = tertiary, radius = size.minDimension / 2)
                        drawCircle(color = Color.White, radius = size.minDimension / 4)
                    }
                    Subtitles("Scheduled Payments")
                }
            }
        }
    }
}

/**
 * Builds a smooth cubic Bézier path through [pts] using Catmull-Rom parameterisation.
 *
 * Unlike midpoint-quadratic bezier (which passes through midpoints, not the actual
 * data), Catmull-Rom passes exactly through every point while remaining C1-continuous.
 * Each pair of adjacent points (p1 → p2) uses the surrounding neighbours (p0, p3) to
 * derive control points, giving a natural tangent at each data point.
 */
private fun buildCatmullRomPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.size < 2) return path
    path.moveTo(pts.first().x, pts.first().y)
    if (pts.size == 2) {
        path.lineTo(pts.last().x, pts.last().y)
        return path
    }
    for (i in 0 until pts.size - 1) {
        val p0 = pts.getOrElse(i - 1) { pts[i] }
        val p1 = pts[i]
        val p2 = pts[i + 1]
        val p3 = pts.getOrElse(i + 2) { pts[i + 1] }

        // Catmull-Rom control points (tension = 1/6 of the chord across neighbours)
        val cp1x = p1.x + (p2.x - p0.x) / 6f
        val cp1y = p1.y + (p2.y - p0.y) / 6f
        val cp2x = p2.x - (p3.x - p1.x) / 6f
        val cp2y = p2.y - (p3.y - p1.y) / 6f

        path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
    }
    return path
}