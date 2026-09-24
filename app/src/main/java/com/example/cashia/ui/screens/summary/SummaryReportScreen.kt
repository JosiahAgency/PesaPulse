package com.example.cashia.ui.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.ReportPeriod
import com.example.cashia.models.SummaryReportUiState
import com.example.cashia.models.SummaryReportViewModel
import com.example.cashia.ui.theme.screenContentPadding
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import kotlin.math.abs

/**
 * Detailed view of one daily or monthly summary — the screen that summary
 * notifications deep-link into. Prev/next chevrons and the Day/Month chips
 * make past periods browsable in-app.
 */
@Composable
fun SummaryReportScreen(
    viewModel: SummaryReportViewModel,
    categoryViewModel: CategoryViewModel,
) {
    val haptic = LocalHapticFeedback.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allCategories by categoryViewModel.allCategories.collectAsStateWithLifecycle()

    val categoryDisplay = remember(allCategories) {
        allCategories.associate { it.id to "${it.emoji} ${it.label}" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "picker") {
            PeriodPicker(
                state = state,
                onPeriodChange = { PesaActions.playClick(haptic); viewModel.setPeriod(it) },
                onPrevious = { PesaActions.playClick(haptic); viewModel.previous() },
                onNext = { PesaActions.playClick(haptic); viewModel.next() },
            )
        }

        item(key = "totals") { TotalsCard(state) }
        if (state.transactions.any { it.type == "expense" && it.transactionCost == null }) {
            item(key = "unknown_fees") {
                Text("Totals exclude unknown fees and may understate spending.", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (state.prevIncome != 0.0 || state.prevExpenses != 0.0) {
            item(key = "comparison") { ComparisonCard(state) }
        }

        if (state.categorySpends.isNotEmpty()) {
            item(key = "categories") { CategoryBreakdownCard(state, categoryDisplay) }
        }

        if (state.topMerchants.isNotEmpty()) {
            item(key = "merchants") { TopMerchantsCard(state) }
        }

        if (state.txCount == 0) {
            item(key = "empty") {
                Text(
                    text = "🌿 No M-Pesa activity in this period",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }
        }
    }
}

// ── Sections ─────────────────────────────────────────────────────────────────

@Composable
private fun PeriodPicker(
    state: SummaryReportUiState,
    onPeriodChange: (ReportPeriod) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.period == ReportPeriod.DAY,
                onClick = { onPeriodChange(ReportPeriod.DAY) },
                label = { Text("Daily") },
            )
            FilterChip(
                selected = state.period == ReportPeriod.MONTH,
                onClick = { onPeriodChange(ReportPeriod.MONTH) },
                label = { Text("Monthly") },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous period")
            }
            Text(
                text = state.label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNext, enabled = state.canGoNext) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next period")
            }
        }
    }
}

@Composable
private fun TotalsCard(state: SummaryReportUiState) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Net",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = "${if (state.net >= 0) "+" else "−"}${
                    PesaTimeConfiguration.formatCurrency(abs(state.net))
                }",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = if (state.net >= 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            Text(
                text = "${state.txCount} transaction${if (state.txCount == 1) "" else "s"}" +
                        " · ${PesaTimeConfiguration.formatCurrency(state.transactionCost)} in known fees",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                AmountColumn(
                    label = "Income",
                    amount = state.income,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                AmountColumn(
                    label = "Expenses",
                    amount = state.expenses,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AmountColumn(
    label: String,
    amount: Double,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = PesaTimeConfiguration.formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
private fun ComparisonCard(state: SummaryReportUiState) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "vs ${state.prevLabel}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            DeltaRow("Income", state.income, state.prevIncome, higherIsBetter = true)
            DeltaRow("Expenses", state.expenses, state.prevExpenses, higherIsBetter = false)
        }
    }
}

@Composable
private fun DeltaRow(label: String, current: Double, previous: Double, higherIsBetter: Boolean) {
    val delta = current - previous
    val pct = if (previous > 0) (delta / previous * 100) else null
    val improved = if (higherIsBetter) delta >= 0 else delta <= 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = buildString {
                append(if (delta >= 0) "▲ " else "▼ ")
                append(PesaTimeConfiguration.formatCurrency(abs(delta)))
                pct?.let { append(" (${"%.0f".format(abs(it))}%)") }
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (improved) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun CategoryBreakdownCard(
    state: SummaryReportUiState,
    categoryDisplay: Map<String, String>,
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Where it went",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            val max = state.categorySpends.maxOf { it.amount }
            state.categorySpends.forEach { spend ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = categoryDisplay[spend.categoryId]
                                ?: prettify(spend.categoryId),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = PesaTimeConfiguration.formatCurrency(spend.amount),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((spend.amount / max).toFloat().coerceIn(0.02f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopMerchantsCard(state: SummaryReportUiState) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Top recipients",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            state.topMerchants.forEach { merchant ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = merchant.recipient,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${merchant.count} payment${if (merchant.count == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        text = PesaTimeConfiguration.formatCurrency(merchant.amount),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
            state.biggestExpense?.let { tx ->
                Text(
                    text = "🔝 Biggest: ${tx.recipient} (${PesaTimeConfiguration.formatCurrency(tx.amount)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun prettify(raw: String) =
    raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
