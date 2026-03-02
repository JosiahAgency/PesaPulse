package com.example.cashia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashia.Utility.FinanceProjectionEngine
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.CategoryViewModel
import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.models.UpcomingStatus
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingViewModel
import com.example.cashia.ui.screens.AllTransactions.UpcomingTransactionCard
import com.example.cashia.ui.screens.Analytics.GlassySurface
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Confidence score: 0–100 based on data density, recency & variance
// ─────────────────────────────────────────────────────────────────────────────
private fun computeConfidence(
    txCount: Int,
    lookbackDays: Int,
    horizonDays: Int,
    projection: FinanceProjection
): Int {
    var score = 100
    // Penalise sparse data
    val density = txCount.toFloat() / lookbackDays.coerceAtLeast(1)
    if (density < 0.5f) score -= 20
    else if (density < 1f) score -= 10
    // Penalise long horizon
    if (horizonDays > 30) score -= 15
    else if (horizonDays > 14) score -= 5
    // Penalise few transactions
    if (txCount < 10) score -= 20
    else if (txCount < 30) score -= 10
    // Penalise data quality issues
    if (projection.dataQualityNote.isNotEmpty()) score -= 15
    return score.coerceIn(10, 95)
}

// Days until the running balance first crosses zero (null = never in window)
private fun daysUntilZero(trend: List<Pair<LocalDate, Double>>, currentBalance: Double): Int? {
    if (currentBalance <= 0) return 0
    trend.forEachIndexed { i, (_, bal) ->
        if (bal <= 0) return i + 1
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectionScreen(
    transactionsViewModel: TransactionsViewModel,
    upcomingViewModel: UpcomingViewModel,
    categoryViewModel: CategoryViewModel
) {
    val context = LocalContext.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.provideFactory(context)
    )
    val haptic = LocalHapticFeedback.current
    val state = viewModel.state
    val transactions = state.allTransactions
    val allCategories by categoryViewModel.allCategories.collectAsState()
    val upcomingAll by upcomingViewModel.upcoming.collectAsState()

    var horizonDays by remember { mutableIntStateOf(30) }
    var balanceOverride by remember { mutableStateOf("") }
    var configExpanded by remember { mutableStateOf(false) }

    val effectiveBalance = remember(balanceOverride, state.latestBalance) {
        balanceOverride.toDoubleOrNull()?.takeIf { it >= 0 } ?: state.latestBalance
    }

    val earliestDate = remember(transactions) {
        transactions.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .minOrNull() ?: LocalDate.now().minusDays(365)
    }

    val maxLookback = remember(earliestDate) {
        ChronoUnit.DAYS.between(earliestDate, LocalDate.now()).toInt().coerceIn(30, 365)
    }

    val recentTransactions = remember(transactions, maxLookback) {
        val cutoff = LocalDate.now().minusDays(maxLookback.toLong())
        transactions.filter { runCatching { LocalDate.parse(it.date) >= cutoff }.getOrDefault(false) }
    }

    val pendingUpcoming = remember(upcomingAll) {
        upcomingAll.filter { it.status == UpcomingStatus.PENDING }
    }

    val projection =
        remember(
            recentTransactions,
            effectiveBalance,
            horizonDays,
            state.latestBalanceDateParsed,
            pendingUpcoming
        ) {
            FinanceProjectionEngine.project(
                transactions = recentTransactions,
                currentBalance = effectiveBalance,
                balanceAsOfDate = if (balanceOverride.toDoubleOrNull() != null) LocalDate.now()
                else state.latestBalanceDateParsed,
                horizonDays,
                pendingUpcoming
            )
        }

    val confidence = remember(recentTransactions.size, maxLookback, horizonDays, projection) {
        computeConfidence(recentTransactions.size, maxLookback.toInt(), horizonDays, projection)
    }

    val zeroCrossingDay = remember(projection, effectiveBalance) {
        daysUntilZero(projection.dailyTrend, effectiveBalance)
    }
    val today = LocalDate.now()
    val overdue = remember(pendingUpcoming) {
        pendingUpcoming.filter { LocalDate.parse(it.dueDate).isBefore(today) }
            .sortedBy { it.dueDate }
    }
    val dueSoon = remember(pendingUpcoming) {
        pendingUpcoming.filter {
            val d = LocalDate.parse(it.dueDate)
            !d.isBefore(today) && d.isBefore(today.plusDays(8))
        }.sortedBy { it.dueDate }
    }
    val later = remember(pendingUpcoming) {
        pendingUpcoming.filter {
            LocalDate.parse(it.dueDate) >= today.plusDays(8)
        }.sortedBy { it.dueDate }
    }
    Scaffold(containerColor = Color.Transparent) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProjectionConfigCard(
                    isExpanded = configExpanded,
                    onToggle = { PesaActions.playClick(haptic); configExpanded = !configExpanded },
                    horizonDays = horizonDays,
                    onHorizonChange = { horizonDays = it },
                    balanceOverride = balanceOverride,
                    onBalanceOverrideChange = { balanceOverride = it },
                    autoBalance = state.latestBalance,
                    autoBalanceDate = state.latestBalanceDate,
                    haptic = haptic
                )
            }

            item {
                ProjectionConfigSummary(
                    totalDays = maxLookback,
                    horizonDays = horizonDays,
                    isBalanceOverridden = balanceOverride.toDoubleOrNull() != null,
                    dataPointCount = recentTransactions.size,
                    pendingUpcoming.size
                )
            }

            item {
                PredictedBalanceCard(
                    currentBalance = effectiveBalance,
                    horizonDays = horizonDays,
                    projection = projection,
                    confidence = confidence,
                    zeroCrossingDay = zeroCrossingDay
                )
            }

            item {
                ProjectionTrendChart(
                    projection = projection,
                    horizonDays = horizonDays,
                    currentBalance = effectiveBalance,
                    confidence = confidence
                )
            }

//            item { ProjectionTripleRow(projection = projection) }

            item {
                UpcomingSection(
                    transactionsViewModel,
                    overdue,
                    dueSoon,
                    later,
                    allCategories,
                    upcomingViewModel,
                    haptic
                )
            }

            item {
                ProjectionInsightsCard(
                    projection = projection,
                    transactionCount = recentTransactions.size,
                    horizonDays = horizonDays,
                    confidence = confidence
                )
            }

            item { DailyTrendMiniList(projection = projection) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpcomingSection(
    viewModel: TransactionsViewModel,
    overdue: List<UpcomingTransaction>,
    dueSoon: List<UpcomingTransaction>,
    later: List<UpcomingTransaction>,
    allCategories: List<ResolvedCategory>,
    upcomingViewModel: UpcomingViewModel,
    haptic: HapticFeedback
) {
    val uiState = upcomingViewModel.uiState
    var deleteTarget by remember { mutableStateOf<UpcomingTransaction?>(null) }
    val total = overdue.size + dueSoon.size + later.size


    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Section header + add button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "SCHEDULED PAYMENTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black, letterSpacing = 1.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
                Text(
                    "$total factored into forecast",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var matchRunning by remember { mutableStateOf(false) }
                val allTx = viewModel.state.allTransactions

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (!matchRunning) {
                                PesaActions.playClick(haptic)
                                matchRunning = true
                                upcomingViewModel.matchAllPending(allTx)
                                kotlinx.coroutines.MainScope().launch {
                                    kotlinx.coroutines.delay(2000)
                                    matchRunning = false
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (matchRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Sync, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { PesaActions.playClick(haptic); upcomingViewModel.openCreate() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp)) }
            }
        }

        if (total == 0) {
            GlassySurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📅", fontSize = 32.sp)
                    Text(
                        "No scheduled payments",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Add rent, subscriptions, or salary to make your forecast more accurate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Overdue
            if (overdue.isNotEmpty()) {
                UpcomingSubSection(
                    title = "⚠️ OVERDUE",
                    color = MaterialTheme.colorScheme.error,
                    items = overdue,
                    allCategories = allCategories,
                    onMarkPaid = { upcomingViewModel.markPaid(it) },
                    onSkip = { upcomingViewModel.skip(it) },
                    onEdit = { upcomingViewModel.openEdit(it) },
                    onDelete = { deleteTarget = it }
                )
            }
            // Due soon
            if (dueSoon.isNotEmpty()) {
                UpcomingSubSection(
                    title = "🔔 DUE THIS WEEK",
                    color = Color(0xFFF59E0B),
                    items = dueSoon,
                    allCategories = allCategories,
                    onMarkPaid = { upcomingViewModel.markPaid(it) },
                    onSkip = { upcomingViewModel.skip(it) },
                    onEdit = { upcomingViewModel.openEdit(it) },
                    onDelete = { deleteTarget = it }
                )
            }
            // Later
            if (later.isNotEmpty()) {
                UpcomingSubSection(
                    title = "📆 LATER",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    items = later,
                    allCategories = allCategories,
                    onMarkPaid = { upcomingViewModel.markPaid(it) },
                    onSkip = { upcomingViewModel.skip(it) },
                    onEdit = { upcomingViewModel.openEdit(it) },
                    onDelete = { deleteTarget = it }
                )
            }

        }

        // Editor sheet
        if (uiState.showSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = upcomingViewModel::closeSheet,
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                val scrollState = androidx.compose.foundation.rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .navigationBarsPadding()
                ) {
                    com.example.cashia.ui.screens.AllTransactions.UpcomingEditorSheet(
                        uiState = uiState,
                        allCategories = allCategories,
                        onTitleChange = upcomingViewModel::onTitleChange,
                        onAmountChange = upcomingViewModel::onAmountChange,
                        onCategoryChange = upcomingViewModel::onCategoryChange,
                        onDueDateChange = upcomingViewModel::onDueDateChange,
                        onTypeChange = upcomingViewModel::onTypeChange,
                        onRecurrenceChange = upcomingViewModel::onRecurrenceChange,
                        onNotesChange = upcomingViewModel::onNotesChange,
                        onSave = upcomingViewModel::save,
                        onDismiss = upcomingViewModel::closeSheet
                    )
                }
            }
        }

        deleteTarget?.let { tx ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("Delete \"${tx.title}\"?") },
                text = { Text("This scheduled payment will be permanently removed from your forecast.") },
                confirmButton = {
                    Button(
                        onClick = { upcomingViewModel.delete(tx); deleteTarget = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun UpcomingSubSection(
    title: String,
    color: Color,
    items: List<UpcomingTransaction>,
    allCategories: List<ResolvedCategory>,
    onMarkPaid: (UpcomingTransaction) -> Unit,
    onSkip: (UpcomingTransaction) -> Unit,
    onEdit: (UpcomingTransaction) -> Unit,
    onDelete: (UpcomingTransaction) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black, letterSpacing = 1.2.sp
                ),
                color = color
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
                    Text(
                        "${items.size}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { tx ->
                    UpcomingTransactionCard(
                        tx = tx,
                        allCategories = allCategories,
                        onMarkPaid = { onMarkPaid(tx) },
                        onSkip = { onSkip(tx) },
                        onEdit = { onEdit(tx) },
                        onDelete = { onDelete(tx) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectionConfigCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    horizonDays: Int,
    onHorizonChange: (Int) -> Unit,
    balanceOverride: String,
    onBalanceOverrideChange: (String) -> Unit,
    autoBalance: Double,
    autoBalanceDate: String,
    haptic: HapticFeedback
) {
    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f
    val overrideValue = balanceOverride.toDoubleOrNull()
    val overrideIsNegative = overrideValue != null && overrideValue < 0
    val overrideIsUnreasonable =
        overrideValue != null && autoBalance > 0 && overrideValue > autoBalance * 10

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Tune,
                                null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            "Projection Basis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            if (isExpanded) "Tap to collapse" else "Tap to configure",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null, tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(300)) + fadeIn(),
                exit = shrinkVertically(tween(250)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))

                    ConfigSection(
                        label = "FORECAST WINDOW",
                        sublabel = "Projecting through ${
                            LocalDate.now().plusDays(horizonDays.toLong())
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        } (${horizonDays} days)\nAccuracy peaks within 30 days."
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                7 to "7d",
                                14 to "14d",
                                30 to "30d",
                                60 to "60d"
                            ).forEach { (days, label) ->
                                ConfigChip(
                                    label,
                                    horizonDays == days,
                                    Modifier.weight(1f)
                                ) { PesaActions.playClick(haptic); onHorizonChange(days) }
                            }
                        }
                    }

                    ConfigSection(
                        label = "STARTING BALANCE",
                        sublabel = buildString {
                            append("Detected from your M-Pesa SMS")
                            if (autoBalanceDate.isNotEmpty()) append(" on $autoBalanceDate")
                            append(". Override if your balance has changed.")
                        }
                    ) {
                        OutlinedTextField(
                            value = balanceOverride,
                            onValueChange = onBalanceOverrideChange,
                            placeholder = {
                                Text(
                                    autoBalance.toLong().toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            },
                            prefix = {
                                Text(
                                    "KES ",
                                    color = if (overrideIsNegative) colorScheme.error else colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            trailingIcon = {
                                if (balanceOverride.isNotEmpty()) {
                                    IconButton(onClick = { onBalanceOverrideChange("") }) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            "Clear",
                                            modifier = Modifier.size(16.dp),
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            isError = overrideIsNegative || overrideIsUnreasonable,
                            supportingText = when {
                                overrideIsNegative -> {
                                    {
                                        Text(
                                            "Balance can't be negative",
                                            color = colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                overrideIsUnreasonable -> {
                                    {
                                        Text(
                                            "Seems unusually high — double check",
                                            color = colorScheme.error,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                overrideValue != null -> {
                                    {
                                        Text(
                                            "Using custom balance",
                                            color = colorScheme.primary.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }

                                else -> null
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (overrideIsNegative || overrideIsUnreasonable) colorScheme.error else colorScheme.primary,
                                unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.15f),
                                focusedContainerColor = colorScheme.primary.copy(alpha = 0.04f),
                                unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                errorBorderColor = colorScheme.error,
                                errorContainerColor = colorScheme.error.copy(alpha = 0.04f)
                            )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Config Summary Pills
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectionConfigSummary(
    totalDays: Int,
    horizonDays: Int,
    isBalanceOverridden: Boolean,
    dataPointCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill(
            Modifier.weight(1f),
            "All ${totalDays}d data",
            Icons.Default.History,
            isCustom = false
        )
        SummaryPill(
            Modifier.weight(1f),
            "${horizonDays}d forecast",
            Icons.Default.AutoGraph,
            horizonDays != 30
        )
        SummaryPill(
            Modifier.weight(1f),
            "$dataPointCount txns",
            Icons.Default.DataUsage,
            isBalanceOverridden,
            if (isBalanceOverridden) "Custom bal." else null
        )
    }
}

@Composable
private fun SummaryPill(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    isCustom: Boolean,
    overrideLabel: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val color =
        if (isCustom) colorScheme.primary else colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = color.copy(alpha = if (isCustom) 0.08f else 0.04f),
        border = BorderStroke(0.5.dp, color.copy(alpha = if (isCustom) 0.25f else 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(
                overrideLabel ?: label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConfigSection(label: String, sublabel: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
            Text(
                sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
        content()
    }
}

@Composable
private fun ConfigChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50.dp),
        color = if (selected) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) null else BorderStroke(
            0.5.dp,
            colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Text(
            label,
            modifier = Modifier.padding(vertical = 9.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Balance Card — FIXED colour logic + confidence + zero-crossing alert
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PredictedBalanceCard(
    currentBalance: Double,
    horizonDays: Int,
    projection: FinanceProjection,
    confidence: Int,
    zeroCrossingDay: Int?
) {
    val colorScheme = MaterialTheme.colorScheme

    val balanceColor = when {
        projection.predictedBalance < 0 -> colorScheme.error
        projection.predictedBalance < currentBalance * 0.5 -> Color(0xFFF59E0B) // amber
        else -> colorScheme.tertiary
    }

    val changePercent = if (currentBalance > 0)
        ((projection.predictedBalance - currentBalance) / currentBalance * 100) else 0.0

    val animatedBalance = remember { Animatable(0f) }
    LaunchedEffect(projection.predictedBalance) {
        animatedBalance.animateTo(
            projection.predictedBalance.toFloat(),
            tween(1200, easing = FastOutSlowInEasing)
        )
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Predicted Balance in $horizonDays days",
                style = MaterialTheme.typography.labelLarge,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Text(
                PesaTimeConfiguration.formatCurrency(animatedBalance.value.toDouble()),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = balanceColor
            )

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (changePercent >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    null, tint = balanceColor, modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${if (changePercent >= 0) "+" else ""}${
                        String.format(
                            "%.1f",
                            changePercent
                        )
                    }% from today's balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            // ── Zero-crossing warning ─────────────────────────────────────────
            if (zeroCrossingDay != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.error.copy(alpha = 0.1f),
                    border = BorderStroke(0.5.dp, colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Balance may reach zero in ~${zeroCrossingDay}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))

            // ── Current / Net Change row ──────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat(
                    "Current",
                    PesaTimeConfiguration.formatCurrency(currentBalance),
                    colorScheme.primary
                )
                VerticalDivider(
                    modifier = Modifier.height(36.dp),
                    color = colorScheme.onSurface.copy(alpha = 0.1f)
                )
                MiniStat(
                    "Net Change",
                    "${if (projection.predictedNet >= 0) "+" else ""}${
                        PesaTimeConfiguration.formatCurrency(
                            projection.predictedNet
                        )
                    }",
                    if (projection.predictedNet >= 0) colorScheme.tertiary else colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(Modifier.height(12.dp))

            // ── Confidence bar ────────────────────────────────────────────────
            ConfidenceBar(confidence = confidence)
        }
    }
}

@Composable
private fun ConfidenceBar(confidence: Int) {
    val colorScheme = MaterialTheme.colorScheme
    val barColor = when {
        confidence >= 70 -> colorScheme.tertiary
        confidence >= 45 -> Color(0xFFF59E0B)
        else -> colorScheme.error
    }
    val label = when {
        confidence >= 70 -> "High confidence"
        confidence >= 45 -> "Moderate confidence"
        else -> "Low confidence — limited data"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${confidence}%",
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { confidence / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.15f)
        )
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Trend Chart — now includes a confidence band shading
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectionTrendChart(
    projection: FinanceProjection,
    horizonDays: Int,
    currentBalance: Double,
    confidence: Int
) {
    if (projection.dailyTrend.isEmpty()) return

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error

    val progress = remember { Animatable(0f) }
    LaunchedEffect(projection) {
        progress.animateTo(
            1f,
            tween(1000, easing = FastOutSlowInEasing)
        )
    }

    val allValues = listOf(currentBalance) + projection.dailyTrend.map { it.second }
    val bandFraction = (1f - confidence / 100f) * 0.25f
    val minVal = (allValues.minOrNull() ?: 0.0) * (1 - bandFraction)
    val maxVal = (allValues.maxOrNull() ?: 1.0) * (1 + bandFraction)
    val range = if (maxVal != minVal) maxVal - minVal else 1.0

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Balance Trajectory",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Projected path over next $horizonDays days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                // Confidence badge
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

            Spacer(Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val w = size.width
                val h = size.height

                fun valueToY(v: Double) =
                    (h - ((v - minVal) / range * h).toFloat()).coerceIn(0f, h)

                val points = allValues.mapIndexed { i, v ->
                    Offset(i / (allValues.size - 1).toFloat() * w, valueToY(v))
                }
                val upperPoints = allValues.mapIndexed { i, v ->
                    Offset(
                        i / (allValues.size - 1).toFloat() * w,
                        valueToY(v * (1 + bandFraction))
                    )
                }
                val lowerPoints = allValues.mapIndexed { i, v ->
                    Offset(
                        i / (allValues.size - 1).toFloat() * w,
                        valueToY(v * (1 - bandFraction))
                    )
                }

                val drawCount = (points.size * progress.value).toInt().coerceAtLeast(2)
                val vis = points.take(drawCount)
                val visUpper = upperPoints.take(drawCount)
                val visLower = lowerPoints.take(drawCount)

                // ── Confidence band fill ──────────────────────────────────────
                if (vis.size >= 2) {
                    val bandPath = Path().apply {
                        moveTo(visUpper.first().x, visUpper.first().y)
                        visUpper.drop(1).forEach { lineTo(it.x, it.y) }
                        visLower.reversed().forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(bandPath, color = primary.copy(alpha = 0.06f))

                    // ── Gradient fill under main line ─────────────────────────
                    val fillPath = Path().apply {
                        moveTo(vis.first().x, h)
                        vis.forEach { lineTo(it.x, it.y) }
                        lineTo(vis.last().x, h)
                        close()
                    }
                    drawPath(
                        fillPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                primary.copy(alpha = 0.22f),
                                Color.Transparent
                            ), 0f, h
                        )
                    )

                    // ── Main line ─────────────────────────────────────────────
                    val linePath = Path().apply {
                        moveTo(vis.first().x, vis.first().y)
                        vis.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        linePath,
                        color = primary,
                        style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // ── Zero baseline (if balance dips negative) ──────────────────
                if (minVal < 0) {
                    val zeroY = valueToY(0.0)
                    drawLine(
                        error.copy(alpha = 0.5f),
                        Offset(0f, zeroY),
                        Offset(w, zeroY),
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                // ── Today dot ────────────────────────────────────────────────
                if (vis.isNotEmpty()) {
                    drawCircle(tertiary, 6.dp.toPx(), points.first())
                    drawCircle(Color.White, 3.dp.toPx(), points.first())
                }
                // ── End dot ───────────────────────────────────────────────────
                if (drawCount >= points.size) {
                    drawCircle(primary, 6.dp.toPx(), points.last())
                    drawCircle(Color.White, 3.dp.toPx(), points.last())
                }
                projection.upcomingMarkers.forEach { (date, balance, _) ->
                    val dayIndex = ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
                    if (dayIndex in 1..horizonDays) {
                        val xFrac = dayIndex.toFloat() / horizonDays.toFloat()
                        val x = xFrac * w
                        val y = valueToY(balance)
                        val r = 6.dp.toPx()
                        // Diamond shape
                        val diamond = Path().apply {
                            moveTo(x, y - r)
                            lineTo(x + r, y)
                            lineTo(x, y + r)
                            lineTo(x - r, y)
                            close()
                        }
                        drawPath(diamond, color = tertiary.copy(alpha = 0.9f))
                        drawPath(
                            diamond, color = Color.White,
                            style = Stroke(1.5.dp.toPx())
                        )
                    }
                }
                // Today dot
                if (vis.isNotEmpty()) {
                    drawCircle(tertiary, 6.dp.toPx(), points.first())
                    drawCircle(Color.White, 3.dp.toPx(), points.first())
                }
                // End dot
                if (drawCount >= points.size) {
                    drawCircle(primary, 6.dp.toPx(), points.last())
                    drawCircle(Color.White, 3.dp.toPx(), points.last())
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    LocalDate.now().plusDays(horizonDays.toLong())
                        .format(DateTimeFormatter.ofPattern("dd MMM")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (projection.upcomingMarkers.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        size.minDimension / 2f
                        val diamond = Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(size.width.toFloat(), size.height / 2f)
                            lineTo(size.width / 2f, size.height.toFloat())
                            lineTo(0f, size.height / 2f)
                            close()
                        }
                        drawPath(diamond, color = tertiary)
                    }
                    Text(
                        "Scheduled payment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// Update the function signature to accept upcomingCount:
@Composable
private fun ProjectionConfigSummary(
    totalDays: Int,
    horizonDays: Int,
    isBalanceOverridden: Boolean,
    dataPointCount: Int,
    upcomingCount: Int   // ← add
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryPill(Modifier.weight(1f), "All ${totalDays}d data", Icons.Default.History, false)
        SummaryPill(
            Modifier.weight(1f),
            "${horizonDays}d forecast",
            Icons.Default.AutoGraph,
            horizonDays != 30
        )
        SummaryPill(
            Modifier.weight(1f),
            "$upcomingCount scheduled",
            Icons.Default.CalendarToday,
            upcomingCount > 0,
            if (isBalanceOverridden) "Custom bal." else null
        )
    }
}

@Composable
private fun ProjectionTripleRow(projection: FinanceProjection) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProjectionStatCard(
            Modifier.weight(1f),
            "Income",
            PesaTimeConfiguration.formatCurrency(projection.predictedIncome),
            Icons.Default.ArrowDownward,
            MaterialTheme.colorScheme.tertiary
        )
        ProjectionStatCard(
            Modifier.weight(1f),
            "Expenses",
            PesaTimeConfiguration.formatCurrency(projection.predictedExpenses),
            Icons.Default.ArrowUpward,
            MaterialTheme.colorScheme.error
        )
        ProjectionStatCard(
            Modifier.weight(1f),
            "Net",
            "${if (projection.predictedNet >= 0) "+" else ""}${
                PesaTimeConfiguration.formatCurrency(
                    abs(projection.predictedNet)
                )
            }",
            if (projection.predictedNet >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
            if (projection.predictedNet >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ProjectionStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    GlassySurface(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Insights Card — now includes confidence score
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProjectionInsightsCard(
    projection: FinanceProjection,
    transactionCount: Int,
    horizonDays: Int,
    confidence: Int
) {
    val savingsRate =
        if (projection.predictedIncome > 0) (projection.predictedNet / projection.predictedIncome * 100) else 0.0
    val dailyAvgExpense = projection.predictedExpenses / horizonDays.toDouble()

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Projection Insights",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            ) {
                Text(
                    "Based on a blend of long-term trend (40%) and recent ${
                        minOf(
                            14,
                            transactionCount
                        )
                    }-day activity (60%). " +
                            "Projected daily net: ${if (projection.projectedDailyNet >= 0) "+" else ""}${
                                PesaTimeConfiguration.formatCurrency(
                                    projection.projectedDailyNet
                                )
                            }.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 16.sp
                )
            }

            if (projection.dataQualityNote.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.06f),
                    border = BorderStroke(0.5.dp, Color.Red.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 1.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            projection.dataQualityNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            Spacer(Modifier.height(12.dp))

            val insights = buildList {
                add(
                    Triple(
                        Icons.Default.Savings,
                        "Savings Rate",
                        "${String.format("%.1f", savingsRate)}% of projected income"
                    )
                )
                add(
                    Triple(
                        Icons.Default.CalendarToday,
                        "Daily Avg Spend",
                        PesaTimeConfiguration.formatCurrency(dailyAvgExpense)
                    )
                )
                add(
                    Triple(
                        Icons.Default.DataUsage,
                        "Data Points",
                        "$transactionCount transactions across lookback window"
                    )
                )
                if (projection.predictedBalance < 0) add(
                    Triple(
                        Icons.Default.Warning,
                        "Alert",
                        "Balance may go negative — reduce spending"
                    )
                )
            }

            insights.forEachIndexed { i, (icon, label, desc) ->
                if (i > 0) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
                val isAlert = label == "Alert"
                val tint =
                    if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = tint.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                null,
                                tint = tint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Daily Breakdown — now shows Δ delta alongside balance
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyTrendMiniList(projection: FinanceProjection) {
    if (projection.dailyTrend.isEmpty()) return

    val preview = projection.dailyTrend.take(7)
    val maxAbs = preview.maxOfOrNull { abs(it.second) } ?: 1.0

    // Compute day-over-day delta (use estimatedTodayBalance as day 0 baseline)
    val startBalance = projection.estimatedTodayBalance
    val deltas = preview.mapIndexed { i, (_, bal) ->
        val prev = if (i == 0) startBalance else preview[i - 1].second
        bal - prev
    }

    GlassySurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Next 7 Days — Projected Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Δ daily",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            Spacer(Modifier.height(12.dp))

            preview.forEachIndexed { i, (date, balance) ->
                if (i > 0) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )

                val positive = balance >= 0
                val barColor =
                    if (positive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                val barFraction = (abs(balance) / maxAbs).toFloat().coerceIn(0.02f, 1f)
                val delta = deltas[i]
                val deltaColor =
                    if (delta >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date label
                    Text(
                        date.format(DateTimeFormatter.ofPattern("EEE, dd MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(100.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor.copy(alpha = 0.7f))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Balance
                    Text(
                        PesaTimeConfiguration.formatCurrency(balance),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor,
                        modifier = Modifier.width(72.dp),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.width(6.dp))
                    // Delta badge
                    Text(
                        "${if (delta >= 0) "+" else ""}${
                            PesaTimeConfiguration.formatCurrency(
                                delta
                            )
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = deltaColor.copy(alpha = 0.75f),
                        modifier = Modifier.width(64.dp),
                        textAlign = TextAlign.End,
                        maxLines = 1
                    )
                }
            }
        }
    }
}