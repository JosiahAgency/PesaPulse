package com.example.cashia.ui.screens.Dashboard

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.Utility.TimeScope
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.screens.AllTransactions.EmptyStateMessage
import com.example.cashia.ui.screens.AllTransactions.TransactionDetailDialog
import com.example.cashia.ui.screens.AllTransactions.TransactionItem
import com.example.cashia.ui.screens.Analytics.GlassySurface
import com.example.cashia.ui.screens.WeeklyCalendar
import com.example.cashia.ui.theme.primaryGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TransactionsViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val state = viewModel.state
    var selectedTransaction by remember { mutableStateOf<MpesaTransactions?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    var isListExpanded by remember { mutableStateOf(true) }

    val firstDay = PesaTimeConfiguration.firstDayOfWeek
    val startOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(firstDay))
    val endOfWeek = startOfWeek.plusDays(6)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onPermissionGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedScope by remember { mutableStateOf(TimeScope.WEEK) }
    val summaryData by remember(selectedDate, selectedScope, state.allTransactions) {
        derivedStateOf { viewModel.calculateSummary(selectedDate, selectedScope) }
    }

    val transactionsForScope = remember(state.allTransactions, selectedDate, selectedScope) {
        state.allTransactions.filter { tx ->
            val txDate = LocalDate.parse(tx.date)
            when (selectedScope) {
                TimeScope.DAY -> txDate == selectedDate
                TimeScope.WEEK -> !txDate.isBefore(startOfWeek) && !txDate.isAfter(endOfWeek)
                TimeScope.MONTH -> txDate.month == selectedDate.month && txDate.year == selectedDate.year
                else -> txDate == selectedDate
            }
        }.sortedByDescending { it.date }
    }

    // Human-readable period label used everywhere so stats are always contextualised
    val periodLabel = remember(selectedDate, selectedScope) {
        when (selectedScope) {
            TimeScope.DAY -> selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM"))
            TimeScope.WEEK -> "${startOfWeek.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${
                endOfWeek.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                )
            }"

            TimeScope.MONTH -> selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            else -> ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedDate != today) {
                ExtendedFloatingActionButton(
                    onClick = { selectedDate = today },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    text = { Text("Back to Today", fontWeight = FontWeight.SemiBold) },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(brush = primaryGradient())
                )
            }
        }
    ) { padding ->
        selectedTransaction?.let { tx ->
            TransactionDetailDialog(
                hapticFeedback, tx = tx,
                onDismiss = { selectedTransaction = null },
                onEdit = {},
                onDelete = {}
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refreshFromSms() },
            modifier = Modifier
                .fillMaxSize()
                .padding()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── 1. BALANCE HERO — always shows "right now" balance ──────────────
                item {
                    BalanceHeroCard(
                        balance = state.latestBalance,
                        isVisible = viewModel.isBalanceVisible,
                        onToggleVisibility = viewModel::toggleBalanceVisibility
                    )
                }

                // ── 2. PERIOD SCOPE SELECTOR + SUMMARY — clearly labelled ───────────
                item {
                    PeriodSummaryCard(
                        selectedScope = selectedScope,
                        onScopeChange = { selectedScope = it },
                        periodLabel = periodLabel,
                        summaryData = summaryData,
                        hapticFeedback = hapticFeedback
                    )
                }

                // ── 3. WEEKLY CALENDAR ────────────────────────────────────────────
                item {
                    WeeklyCalendar(
                        hapticFeedback,
                        state.allTransactions,
                        selectedDate
                    ) { selectedDate = it }
                }

                // ── 4. TRANSACTION LIST HEADER — period label repeated so ──────────
                //    user knows exactly which transactions they're seeing
                item {
                    TransactionListHeader(
                        periodLabel = periodLabel,
                        count = transactionsForScope.size,
                        isExpanded = isListExpanded,
                        onToggle = {
                            PesaActions.playClick(hapticFeedback)
                            isListExpanded = !isListExpanded
                        }
                    )
                }

                // ── 5. TRANSACTIONS ───────────────────────────────────────────────
                if (transactionsForScope.isNotEmpty() && isListExpanded) {
                    items(transactionsForScope, key = { it.id }) { tx ->
                        TransactionItem(tx, onClick = { selectedTransaction = tx }, hapticFeedback)
                    }
                } else if (transactionsForScope.isEmpty() && isListExpanded) {
                    item { EmptyStateMessage("No transactions in this period", hapticFeedback) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Balance Hero — unambiguous "right now" framing
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BalanceHeroCard(
    balance: Double,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Text(
                text = "CURRENT BALANCE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleVisibility() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isVisible)
                        PesaTimeConfiguration.formatCurrency(balance)
                    else
                        "••••••",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Live indicator dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            brush = primaryGradient(),
                            shape = CircleShape
                        )
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap to ${if (isVisible) "hide" else "show"} • as of right now",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Period Summary — every number is labelled with what period it's for
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PeriodSummaryCard(
    selectedScope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    periodLabel: String,
    summaryData: com.example.cashia.models.SummaryData,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period scope tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(TimeScope.DAY, TimeScope.WEEK, TimeScope.MONTH).forEach { scope ->
                    val isSelected = selectedScope == scope
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                PesaActions.playClick(hapticFeedback)
                                onScopeChange(scope)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = when (scope) {
                                    TimeScope.DAY -> "Day"
                                    TimeScope.WEEK -> "Week"
                                    TimeScope.MONTH -> "Month"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Period label — the key missing piece: user always knows what they're looking at
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

            // Stats row — each stat has its own label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PeriodStat(
                    label = "Money In",
                    value = PesaTimeConfiguration.formatCurrency(summaryData.income),
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.TrendingUp
                )
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(52.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )
                PeriodStat(
                    label = "Money Out",
                    value = PesaTimeConfiguration.formatCurrency(summaryData.expenses),
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.TrendingDown,
                    alignEnd = true
                )
            }

            // Net for the period
            val net = summaryData.income - summaryData.expenses
            val netPositive = net >= 0
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = (if (netPositive) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error).copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net this ${
                            when (selectedScope) {
                                TimeScope.DAY -> "day"
                                TimeScope.WEEK -> "week"
                                else -> "month"
                            }
                        }",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${if (netPositive) "+" else ""}${
                            PesaTimeConfiguration.formatCurrency(
                                net
                            )
                        }",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (netPositive) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodStat(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    alignEnd: Boolean = false
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!alignEnd) {
                Icon(icon, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            if (alignEnd) {
                Icon(icon, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Transaction list header — restates the period so user is never confused
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TransactionListHeader(
    periodLabel: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "TRANSACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            // Period label beneath so there's zero ambiguity
            Text(
                text = "$count result${if (count != 1) "s" else ""} for $periodLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Transaction count badge
            if (count > 0) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}