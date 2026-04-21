package com.example.cashia.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TransactionFrequencyCard(
    allTransactions: List<MpesaTransactions>, selectedDate: LocalDate) {
    val selectedMonth = selectedDate.month
    val selectedYear = selectedDate.year

    val monthPrefix = remember(selectedMonth, selectedYear) {
        "$selectedYear-${selectedMonth.value.toString().padStart(2, '0')}"
    }

    val monthlyPairs = remember(allTransactions, monthPrefix) {
        allTransactions.mapNotNull { txn ->
            if (!txn.date.startsWith(monthPrefix)) return@mapNotNull null
            val date =
                runCatching { LocalDate.parse(txn.date) }.getOrNull() ?: return@mapNotNull null
            txn to date
        }
    }

    val (topIncome, topExpense) = remember(monthlyPairs) {
        val incomeMap = mutableMapOf<LocalDate, MutableList<MpesaTransactions>>()
        val expenseMap = mutableMapOf<LocalDate, MutableList<MpesaTransactions>>()
        for ((txn, date) in monthlyPairs) {
            when (txn.type) {
                "income" -> incomeMap.getOrPut(date) { mutableListOf() }.add(txn)
                "expense" -> expenseMap.getOrPut(date) { mutableListOf() }.add(txn)
            }
        }
        incomeMap.maxByOrNull { it.value.size } to expenseMap.maxByOrNull { it.value.size }
    }


    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("monthly activity peaks")

        GlassySurface {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FrequencyMetric(
                        modifier = Modifier.weight(1f),
                        label = "Income Peak",
                        dateString = topIncome?.key?.toString(),
                        count = topIncome?.value?.size ?: 0,
                        color = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(60.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            .align(Alignment.CenterVertically)
                    )

                    FrequencyMetric(
                        modifier = Modifier.weight(1f),
                        label = "Expense Peak",
                        dateString = topExpense?.key?.toString(),
                        count = topExpense?.value?.size ?: 0,
                        color = MaterialTheme.colorScheme.error,
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyMetric(
    modifier: Modifier,
    label: String,
    dateString: String?,
    count: Int,
    color: Color,
    icon: ImageVector,
) {
    val formattedDate = remember(dateString) {
        if (dateString != null) {
            LocalDate.parse(dateString).format(DateTimeFormatter.ofPattern("dd MMM, yyyy"))
        } else "No Data"
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            Subtitles(label)
        }

        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}