package com.example.cashia.ui.screens.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.utility.formatCompactAmount
import java.time.LocalDate


@Composable
fun FinancialOverviewCards(
    transactions: List<MpesaTransactions>, selectedDate: LocalDate
) {
    val selectedMonth = selectedDate.month
    val selectedYear = selectedDate.year
    val monthPrefix = remember(selectedMonth, selectedYear) {
        "$selectedYear-${selectedMonth.value.toString().padStart(2, '0')}"
    }
    val (monthlyIncome, monthlyExpense, hasTransactions) = remember(transactions, monthPrefix) {
        var income = 0.0
        var expense = 0.0
        var count = 0
        for (tx in transactions) {
            if (!tx.date.startsWith(monthPrefix)) continue
            count++
            when (tx.type) {
                "income" -> income += tx.amount
                "expense" -> expense += tx.amount
            }
        }
        Triple(income, expense, count > 0)
    }

    val monthlyNetFlow = monthlyIncome - monthlyExpense

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("${selectedDate.month.name} ${selectedDate.year} SUMMARY")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income Card
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "Income",
                amount = monthlyIncome,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = MaterialTheme.colorScheme.tertiary,
                isSelected = monthlyIncome > 0
            )

            // Expense Card
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "Expenses",
                amount = monthlyExpense,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                color = MaterialTheme.colorScheme.error,
                isSelected = monthlyExpense > 0
            )

            // Net Flow Card
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "Net",
                amount = monthlyNetFlow,
                icon = if (monthlyNetFlow >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                color = if (monthlyNetFlow >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                hasTransactions
            )
        }
    }
}

@Composable
fun SmallMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val backgroundAlpha by animateFloatAsState(if (isSelected) 0.12f else 0.04f, label = "bg")

    GlassySurface(
        modifier = modifier.height(100.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color.copy(alpha = backgroundAlpha))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
                Column {
                    Subtitles(title)
                    Text(
                        text = formatCompactAmount(amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (amount == 0.0) color.copy(alpha = 0.4f) else color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}