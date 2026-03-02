package com.example.cashia.ui.screens.Analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.ResolvedCategory

// ── Slice colours — enough for all system + custom categories ─────────────────
private val SLICE_COLORS = listOf(
    Color(0xFF6C63FF), Color(0xFFFF6584), Color(0xFF43E97B),
    Color(0xFFFA8231), Color(0xFF00B4D8), Color(0xFFFFD166),
    Color(0xFFEF476F), Color(0xFF06D6A0), Color(0xFFFFB347),
    Color(0xFF9B5DE5), Color(0xFF00BBF9), Color(0xFFF15BB5),
    Color(0xFF3A86FF), Color(0xFFFF006E), Color(0xFFFB5607),
    Color(0xFF8338EC), Color(0xFF06A77D), Color(0xFFD62246)
)

data class PieSlice(
    val categoryId: String,
    val label: String,
    val emoji: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun CategoryPieChart(
    filteredTransactions: List<MpesaTransactions>,
    allCategories: List<ResolvedCategory>,
    selectedType: TransactionType
) {
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }

    val slices = remember(filteredTransactions, allCategories) {
        val total = filteredTransactions.sumOf { it.amount }
        if (total == 0.0) return@remember emptyList()

        filteredTransactions
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry ->
                val resolved = categoryMap[entry.key]
                PieSlice(
                    categoryId = entry.key,
                    label = resolved?.label ?: entry.key,
                    emoji = resolved?.emoji ?: "📦",
                    amount = entry.value,
                    percentage = (entry.value / total * 100).toFloat(),
                    color = SLICE_COLORS[index % SLICE_COLORS.size]
                )
            }
    }

    if (slices.isEmpty()) return

    var selectedIndex by remember(filteredTransactions) { mutableIntStateOf(-1) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(filteredTransactions) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    if (selectedType == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            Text(
                text = "CATEGORY BREAKDOWN",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )

            // ── Donut chart ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                val strokeWidth = 52f
                val gapDegrees = 2f

                Canvas(modifier = Modifier.size(200.dp)) {
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        (size.width - diameter) / 2f,
                        (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)
                    var startAngle = -90f

                    slices.forEachIndexed { index, slice ->
                        val sweep = (slice.percentage / 100f) * 360f * animatedProgress.value
                        val isSelected = index == selectedIndex
                        val sliceStroke = if (isSelected) strokeWidth + 12f else strokeWidth

                        drawArc(
                            color = slice.color.copy(
                                alpha = when {
                                    selectedIndex == -1 -> 0.85f
                                    isSelected -> 1f
                                    else -> 0.3f
                                }
                            ),
                            startAngle = startAngle + gapDegrees / 2f,
                            sweepAngle = sweep - gapDegrees,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = sliceStroke, cap = StrokeCap.Butt)
                        )
                        startAngle += sweep
                    }
                }

                // ── Centre label ──────────────────────────────────────────
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (selectedIndex >= 0 && selectedIndex < slices.size) {
                        val sel = slices[selectedIndex]
                        Text(sel.emoji, fontSize = 26.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "%.1f%%".format(sel.percentage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = sel.color
                        )
                        Text(
                            text = PesaTimeConfiguration.formatCurrency(sel.amount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            if (selectedType == TransactionType.INCOME) "💰" else "💸",
                            fontSize = 30.sp
                        )
                        Text(
                            text = "${slices.size} categories",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Legend ────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slices.forEachIndexed { index, slice ->
                    val isSelected = index == selectedIndex
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedIndex = if (selectedIndex == index) -1 else index
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected)
                            slice.color.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Colour dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color.copy(alpha = if (isSelected) 1f else 0.7f))
                            )
                            // Emoji + label
                            Text(slice.emoji, fontSize = 14.sp)
                            Text(
                                text = slice.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = if (isSelected) 1f else 0.75f
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            // Percentage chip
                            Surface(
                                shape = CircleShape,
                                color = slice.color.copy(alpha = if (isSelected) 0.2f else 0.1f)
                            ) {
                                Text(
                                    text = "%.1f%%".format(slice.percentage),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = slice.color
                                )
                            }
                            // Amount
                            Text(
                                text = PesaTimeConfiguration.formatCurrency(slice.amount),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}