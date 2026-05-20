package com.example.cashia.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.detectRecurringMerchants
import com.example.cashia.utility.formatCompactAmount
import com.example.cashia.utility.hourlyPattern
import com.example.cashia.utility.weekdaySpendPattern
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

// ── 1. Weekday spend heatmap ──────────────────────────────────────────────

@Composable
fun WeekdaySpendCard(
    transactions: List<MpesaTransactions>,
    type: String
) {
    val pattern = remember(transactions, type) { weekdaySpendPattern(transactions, type) }
    val max = remember(pattern) { pattern.maxOfOrNull { it.avgAmount }?.coerceAtLeast(1.0) ?: 1.0 }
    var expanded by remember { mutableStateOf(true) }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("SPEND BY DAY OF WEEK")
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(180)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val peakDay = pattern.maxByOrNull { it.avgAmount }
                    pattern.forEach { p ->
                        val ratio = (p.avgAmount / max).toFloat()
                        val isPeak = p.day == peakDay?.day
                        val barColor = if (isPeak)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                p.day.getDisplayName(
                                    TextStyle.SHORT,
                                    LocalLocale.current.platformLocale
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                                color = if (isPeak)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.width(32.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                            ) {
                                val animRatio = remember { Animatable(0f) }
                                LaunchedEffect(ratio) {
                                    animRatio.animateTo(
                                        ratio,
                                        tween(600, easing = FastOutSlowInEasing)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animRatio.value)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(barColor)
                                )
                                if (isPeak) {
                                    Text(
                                        "peak",
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .padding(end = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.5f
                                        ),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Text(
                                formatCompactAmount(p.avgAmount),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isPeak) FontWeight.Black else FontWeight.Normal,
                                color = if (isPeak)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(40.dp)
                            )
                        }
                    }

                    peakDay?.let {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "💡 You spend most on ${
                                    it.day.getDisplayName(
                                        TextStyle.FULL,
                                        LocalLocale.current.platformLocale
                                    )
                                }s — avg ${PesaTimeConfiguration.formatCurrency(it.avgAmount)} per ${
                                    it.day.getDisplayName(
                                        TextStyle.SHORT,
                                        LocalLocale.current.platformLocale
                                    )
                                }",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 2. Recurring merchants ────────────────────────────────────────────────

@Composable
fun RecurringMerchantsCard(
    transactions: List<MpesaTransactions>,
    type: String
) {
    val merchants = remember(transactions, type) {
        detectRecurringMerchants(transactions, type).take(8)
    }
    if (merchants.isEmpty()) return

    var expanded by remember { mutableStateOf(true) }
    val accentColor = if (type == "expense") MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.tertiary

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("RECURRING PAYMENTS")
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(180)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Subtitles("Merchants you pay in multiple months")
                    merchants.forEach { m ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.06f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 10.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        m.recipient.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = accentColor
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        m.recipient,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Subtitles(
                                        "${m.occurrences}× · last ${
                                            m.lastDate.format(
                                                DateTimeFormatter.ofPattern("dd MMM yy")
                                            )
                                        }"
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        PesaTimeConfiguration.formatCurrency(m.avgAmount),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = accentColor
                                    )
                                    Subtitles("avg / occurrence")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 3. Time-of-day activity heatmap ──────────────────────────────────────

@Composable
fun TimeOfDayCard(transactions: List<MpesaTransactions>, type: String) {
    val pattern = remember(transactions, type) { hourlyPattern(transactions, type) }
    val hasData = pattern.any { it.txCount > 0 }
    if (!hasData) return

    val maxCount = remember(pattern) {
        pattern.maxOfOrNull { it.txCount }?.coerceAtLeast(1) ?: 1
    }
    var expanded by remember { mutableStateOf(true) }

    // Group into sessions
    val sessions = listOf(
        "Dawn 🌙" to (0..5),
        "Morning ☀️" to (6..11),
        "Afternoon 🌤" to (12..16),
        "Evening 🌆" to (17..20),
        "Night 🌛" to (21..23)
    )

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("ACTIVITY BY TIME OF DAY")
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(180)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 24 mini bars across the full width
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        pattern.forEach { hp ->
                            val ratio = hp.txCount.toFloat() / maxCount
                            val animH = remember { Animatable(0f) }
                            LaunchedEffect(ratio) {
                                animH.animateTo(ratio, tween(500, easing = FastOutSlowInEasing))
                            }
                            val barColor = when (hp.hour) {
                                in 6..11 -> Color(0xFFFFD166)
                                in 12..16 -> Color(0xFF06D6A0)
                                in 17..20 -> Color(0xFF118AB2)
                                in 21..23 -> Color(0xFF6C63FF)
                                else -> Color(0xFF9B5DE5)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height((animH.value * 48 + 4).dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(
                                        if (hp.txCount > 0) barColor.copy(alpha = 0.75f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }
                    // Hour labels at quarters
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("12am", "6am", "12pm", "6pm", "11pm").forEachIndexed { i, label ->
                            if (i > 0) Spacer(Modifier.weight(1f))
                            Text(
                                label, fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    // Session summary chips
                    sessions.forEach { (label, range) ->
                        val sessionTxs = pattern.filter { it.hour in range }
                        val count = sessionTxs.sumOf { it.txCount }
                        val total = sessionTxs.sumOf { it.totalAmount }
                        if (count == 0) return@forEach

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        "$count txn",
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    PesaTimeConfiguration.formatCurrency(total),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 4. Budget burn rate ───────────────────────────────────────────────────

@Composable
fun BurnRateCard(
    transactions: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val today = LocalDate.now()
    val totalDays = remember(startDate, endDate) {
        (ChronoUnit.DAYS.between(startDate, endDate) + 1).toInt()
    }
    val elapsedDays = remember(startDate, today, endDate) {
        (ChronoUnit.DAYS.between(startDate, today.coerceAtMost(endDate)) + 1).toInt()
            .coerceAtLeast(1)
    }
    val totalSpend = remember(transactions, startDate, endDate) {
        transactions
            .filter {
                it.type == "expense" &&
                        it.reversedTxId == null &&
                        !LocalDate.parse(it.date).isBefore(startDate) &&
                        !LocalDate.parse(it.date).isAfter(endDate)
            }
            .sumOf { it.amount }
    }

    if (totalSpend == 0.0) return

    val dailyRate = totalSpend / elapsedDays
    val projectedTotal = dailyRate * totalDays
    val remainingDays = (totalDays - elapsedDays).coerceAtLeast(0)
    val burnProgress = (elapsedDays.toFloat() / totalDays).coerceIn(0f, 1f)
    val spendProgress = (totalSpend / projectedTotal).toFloat().coerceIn(0f, 1f)
    val colorScheme = MaterialTheme.colorScheme

    val burnColor = when {
        spendProgress > 0.85f -> colorScheme.error
        spendProgress > 0.6f -> Color(0xFFFA8231)
        else -> colorScheme.tertiary
    }

    var expanded by remember { mutableStateOf(true) }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("BURN RATE")
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(180)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Period progress bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Subtitles("Period progress")
                            Subtitles("Day $elapsedDays of $totalDays")
                        }
                        val animBurn = remember { Animatable(0f) }
                        LaunchedEffect(burnProgress) {
                            animBurn.animateTo(
                                burnProgress,
                                tween(700, easing = FastOutSlowInEasing)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animBurn.value)
                                    .clip(CircleShape)
                                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            )
                        }
                    }

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BurnStatChip(
                            label = "Daily avg",
                            value = PesaTimeConfiguration.formatCurrency(dailyRate),
                            color = burnColor,
                            modifier = Modifier.weight(1f)
                        )
                        BurnStatChip(
                            label = "Projected total",
                            value = PesaTimeConfiguration.formatCurrency(projectedTotal),
                            color = burnColor,
                            modifier = Modifier.weight(1f)
                        )
                        BurnStatChip(
                            label = "Days left",
                            value = "$remainingDays",
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Insight callout
                    val insight = when {
                        remainingDays == 0 -> "Period complete — you spent ${
                            PesaTimeConfiguration.formatCurrency(
                                totalSpend
                            )
                        } total."

                        spendProgress > 0.85f ->
                            "⚠️ High burn rate — at this pace you'll spend ${
                                PesaTimeConfiguration.formatCurrency(
                                    projectedTotal
                                )
                            } this period."

                        else ->
                            "At ${PesaTimeConfiguration.formatCurrency(dailyRate)}/day you'll spend ~${
                                PesaTimeConfiguration.formatCurrency(
                                    projectedTotal
                                )
                            } by end of period."
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = burnColor.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            insight,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = burnColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BurnStatChip(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ── 5. Balance trend card ─────────────────────────────────────────────────

@Composable
fun BalanceTrendCard(
    transactions: List<MpesaTransactions>,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val balancePoints = remember(transactions, startDate, endDate) {
        transactions
            .filter {
                it.balance > 0 &&
                        !LocalDate.parse(it.date).isBefore(startDate) &&
                        !LocalDate.parse(it.date).isAfter(endDate)
            }
            .sortedBy { it.timestamp }
    }
    if (balancePoints.size < 2) return

    val minBal = balancePoints.minOf { it.balance }
    val maxBal = balancePoints.maxOf { it.balance }
    val latestBal = balancePoints.last().balance
    val earliestBal = balancePoints.first().balance
    val change = latestBal - earliestBal
    val changePercent = if (earliestBal > 0) (change / earliestBal * 100) else 0.0

    val colorScheme = MaterialTheme.colorScheme
    val lineColor = if (change >= 0) colorScheme.tertiary else colorScheme.error
    var expanded by remember { mutableStateOf(true) }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("M-PESA BALANCE TREND")
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(200)) + fadeIn(),
                exit = shrinkVertically(tween(180)) + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Subtitles("Current balance")
                            Text(
                                PesaTimeConfiguration.formatCurrency(latestBal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = lineColor
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = lineColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                "${if (change >= 0) "+" else ""}${
                                    PesaTimeConfiguration.formatCurrency(change)
                                } (${
                                    "%.1f".format(changePercent)
                                }%)",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = lineColor
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BurnStatChip(
                            "Lowest",
                            PesaTimeConfiguration.formatCurrency(minBal),
                            colorScheme.error,
                            Modifier.weight(1f)
                        )
                        BurnStatChip(
                            "Highest",
                            PesaTimeConfiguration.formatCurrency(maxBal),
                            colorScheme.tertiary,
                            Modifier.weight(1f)
                        )
                        BurnStatChip(
                            "Data points",
                            "${balancePoints.size}",
                            colorScheme.onSurfaceVariant,
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}