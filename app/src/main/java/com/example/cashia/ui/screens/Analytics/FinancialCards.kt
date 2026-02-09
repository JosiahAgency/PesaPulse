package com.example.cashia.ui.screens.Analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.unit.sp
import com.example.cashia.models.MpesaTransactions
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Composable
fun FinancialOverviewCards(
    transactions: List<MpesaTransactions>, selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val selectedDateStr = selectedDate.toString()
    val dailyTransactions = remember(transactions, selectedDateStr) {
        transactions.filter { it.date == selectedDateStr }
    }

    val income = dailyTransactions.filter { it.type == "income" }.sumOf { it.amount }
    val expense = dailyTransactions.filter { it.type == "expense" }.sumOf { it.amount }
    val netFlow = income - expense

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "DAILY TOTALS: ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM"))}",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Income Card
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "Income",
                amount = income,
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.tertiary,
                isSelected = income > 0
            )

            // Expense Card
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "Expenses",
                amount = expense,
                icon = Icons.Default.TrendingDown,
                color = MaterialTheme.colorScheme.error,
                isSelected = expense > 0
            )

            // Net Flow Card
            SmallMetricCard(
                modifier = Modifier.weight(1f),
                title = "Net",
                amount = netFlow,
                icon = if (netFlow >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                color = if (netFlow >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                isSelected = dailyTransactions.isNotEmpty()
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

    val borderAlpha by animateFloatAsState(if (isSelected) 0.4f else 0.05f, label = "border")
    val backgroundAlpha by animateFloatAsState(if (isSelected) 0.12f else 0.04f, label = "bg")

    GlassySurface(
        modifier = modifier.height(100.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color.copy(alpha = backgroundAlpha))
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    // Optional: Add logic to filter main list by this type
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
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
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
//@Composable
//fun SmallMetricCard(
//    modifier: Modifier = Modifier,
//    title: String,
//    amount: Double,
//    icon: ImageVector,
//    color: Color,
//    isSelected: Boolean = false
//) {
//    val haptic = LocalHapticFeedback.current
//
//    // Dynamic border to highlight if there is data for this date
//    val borderAlpha by animateFloatAsState(if (isSelected) 0.4f else 0.05f, label = "border")
//    val backgroundAlpha by animateFloatAsState(if (isSelected) 0.12f else 0.04f, label = "bg")
//    GlassySurface(modifier = modifier) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(color.copy(alpha = backgroundAlpha))
//                .border(
//                    width = 1.dp,
//                    color = color.copy(alpha = borderAlpha),
//                    shape = RoundedCornerShape(24.dp)
//                )
//                .clickable {
//                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                    // Optional: Add logic to filter main list by this type
//                }
//                .padding(12.dp)
//        ) {
//        Column(
//            modifier = Modifier.padding(12.dp),
//            horizontalAlignment = Alignment.Start
//        ) {
//            Icon(
//                imageVector = icon,
//                contentDescription = null,
//                tint = color,
//                modifier = Modifier.size(20.dp)
//            )
//            Spacer(Modifier.height(8.dp))
//            Text(
//                text = title,
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(
//                text = formatAmount(amount),
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//                color = color,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//        }
//    }
//}