package com.example.cashia.ui.screens.Analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.cashia.Utility.TransactionType
import com.example.cashia.models.MpesaTransactions


@Composable
fun GlassyTrendChart(
    transactions: List<MpesaTransactions>,
    type: TransactionType
) {
    val haptic = LocalHapticFeedback.current
    val baseColor = if (type == TransactionType.INCOME)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.error

    var selectedKey by remember { mutableStateOf<String?>(null) }

    val groupedData = remember(transactions) {
        val daily = transactions.groupBy { it.date }
            .mapValues { it.value.sumOf { tx -> tx.amount } }

        if (daily.size > 31) {
            transactions.groupBy { it.date.substring(0, 7) }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toSortedMap()
        } else {
            daily.toSortedMap()
        }
    }

    val keysList = remember(groupedData) { groupedData.keys.toList() }
    val maxAmount = remember(groupedData) { (groupedData.values.maxOrNull() ?: 1.0).toFloat() }
    val yAxisLabels = listOf(maxAmount, maxAmount / 2, 0f)

    GlassySurface {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Trend Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap bars for details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = baseColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            ) {
                // Y-axis
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    yAxisLabels.forEach {
                        Text(
                            formatCompactAmount(it.toDouble()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chart area
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Grid lines
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(3) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }

                    // Bars
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        groupedData.forEach { (key, amount) ->
                            val isSelected = selectedKey == key
                            val barHeightRatio =
                                ((amount.toFloat() / maxAmount) * 0.8f).coerceAtLeast(0.05f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(barHeightRatio)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isSelected)
                                            baseColor
                                        else
                                            baseColor.copy(alpha = 0.4f)
                                    )
                                    .clickable {
                                        if (selectedKey != key) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedKey = key
                                        } else {
                                            selectedKey = null
                                        }
                                    }
                            )
                        }
                    }

                    // Tooltip
                    selectedKey?.let { key ->
                        val selectedIndex = keysList.indexOf(key)
                        val amount = groupedData[key] ?: 0.0
                        val totalItems = keysList.size
                        val horizontalBias = if (totalItems > 1) {
                            (selectedIndex.toFloat() / (totalItems - 1)) * 2f - 1f
                        } else 0f

                        Surface(
                            color = baseColor,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .align(BiasAlignment(horizontalBias, -1f))
                                .zIndex(1f)
                        ) {
                            Text(
                                text = "Ksh ${formatCompactAmount(amount)}",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // X-axis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (keysList.isNotEmpty()) {
                    Text(
                        formatXLabel(keysList.first()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (keysList.size > 2) {
                        Text(
                            formatXLabel(keysList[keysList.size / 2]),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        formatXLabel(keysList.last()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}