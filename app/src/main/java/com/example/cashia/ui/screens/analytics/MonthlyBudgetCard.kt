package com.example.cashia.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.BudgetProgress
import com.example.cashia.ui.reusedElements.GlassySurface
import com.example.cashia.ui.reusedElements.SectionLabel
import com.example.cashia.ui.reusedElements.Subtitles
import com.example.cashia.ui.theme.CurrencyAmountStyle
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.format.DateTimeFormatter

@Composable
fun MonthlyBudgetCard(progress: BudgetProgress) {
    val colors = MaterialTheme.colorScheme
    val cap = progress.budget.spendingCap
    val overBudget = cap != null && progress.spent > cap

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Monthly budget")
                Subtitles("${progress.budget.month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))} · Full month")
            }

            BudgetMetric(
                label = "Spending",
                amount = progress.spent,
                targetLabel = "Spending cap",
                target = cap,
                accent = if (overBudget) colors.error else colors.primary
            )
            if (overBudget) {
                Text(
                    text = "${PesaTimeConfiguration.formatCurrency(progress.spent - requireNotNull(cap))} over budget",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.error
                )
            }

            HorizontalDivider(color = colors.onSurface.copy(alpha = 0.08f))

            BudgetMetric(
                label = "Net saved",
                amount = progress.netSaved,
                targetLabel = "Savings goal",
                target = progress.budget.savingsGoal,
                accent = if (progress.netSaved < 0) colors.error else colors.tertiary
            )
            Subtitles("Income minus spending")

            if (progress.unknownFeeCount > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = colors.errorContainer.copy(alpha = 0.35f),
                    contentColor = colors.onSurface
                ) {
                    val count = progress.unknownFeeCount
                    Text(
                        text = "Fees missing on $count ${if (count == 1) "transaction" else "transactions"}. Totals are incomplete.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetMetric(
    label: String,
    amount: Double,
    targetLabel: String,
    target: Double?,
    accent: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Subtitles(label)
        Text(
            text = PesaTimeConfiguration.formatCurrency(amount),
            style = MaterialTheme.typography.headlineSmall.merge(CurrencyAmountStyle),
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = "$targetLabel: ${target?.let { PesaTimeConfiguration.formatCurrency(it) } ?: "Not set"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (target != null && target > 0) {
            LinearProgressIndicator(
                progress = { (amount / target).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = accent,
                trackColor = accent.copy(alpha = 0.12f),
                drawStopIndicator = {}
            )
        }
    }
}
