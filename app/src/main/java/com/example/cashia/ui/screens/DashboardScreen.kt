package com.example.cashia.ui.screens

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.cashia.Utility.TimeScope
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.SummaryData
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.screens.AllTransactions.EmptyStateMessage
import com.example.cashia.ui.screens.AllTransactions.TransactionDetailDialog
import com.example.cashia.ui.screens.AllTransactions.TransactionItem
import com.example.cashia.ui.screens.AllTransactions.formatAmount
import com.example.cashia.ui.theme.primaryGradient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: TransactionsViewModel) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val state = viewModel.state
    var selectedTransaction by remember { mutableStateOf<MpesaTransactions?>(null) }


    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedScope by remember { mutableStateOf(TimeScope.WEEK) }
    val summaryData by remember(selectedDate, selectedScope, state.allTransactions) {
        derivedStateOf {
            viewModel.calculateSummary(selectedDate, selectedScope)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, floatingActionButton = {
            if (selectedDate != today) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedDate = today
                    },
                    icon = {
                        Icon(Icons.Default.Today, contentDescription = "Today")

                    },
                    text = {
                        Text("Today", fontWeight = FontWeight.SemiBold)
                    },
                    containerColor = Color.Transparent,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp
                    ),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            brush = primaryGradient()
                        )
                )
            }
        }) { padding ->
        selectedTransaction?.let { tx ->
            TransactionDetailDialog(
                tx = tx,
                onDismiss = { selectedTransaction = null },
                onEdit = { /* Handle Edit logic */ },
                onDelete = {
//                    viewModel.deleteTransaction(it)
//                    selectedTransaction = null
                }
            )
        }
        PullToRefreshBox(
            isRefreshing = state.isLoading, onRefresh = {
                viewModel.refreshFromSms()
            }, modifier = Modifier
                .fillMaxSize()
                .padding()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TimeScopedSummaryCard(
                        selectedScope,
                        { selectedScope = it },
                        summaryData,
                        state.latestBalance
                    )
                }
                item {
                    WeeklyCalendar(state.allTransactions, selectedDate) { selectedDate = it }
                }
                val transactionsForDay = state.allTransactions.filter {
                    LocalDate.parse(it.date) == selectedDate
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            // A small "Pulse" dot to signify this is the active day
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(brush = primaryGradient(), shape = CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))
                                    .uppercase(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // A subtle divider that fades out to the right
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(2.dp)
                                .padding(top = 4.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }
                if (transactionsForDay.isEmpty()) {
                    item { EmptyStateMessage("No Transactions On This Day") }
                } else {
                    items(transactionsForDay, key = { it.id }) { tx ->
                        TransactionItem(tx, onClick = { selectedTransaction = tx })
                    }
                }
            }
        }
    }
}

@Composable
fun TimeScopedSummaryCard(
    scope: TimeScope,
    onScopeChange: (TimeScope) -> Unit,
    summaryData: SummaryData,
    currentBalance: Double
) {
    val colorScheme = MaterialTheme.colorScheme

    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f
    val glassColor = colorScheme.surface.copy(alpha = glassAlpha)
    val glassBorder = colorScheme.onSurface.copy(alpha = 0.12f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        color = glassColor,
        border = BorderStroke(
            0.6.dp, glassBorder
        ),
        shadowElevation = 10.dp,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            ModernSegmentedControl(scope, onScopeChange)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Total Balance",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                val balanceText = "Ksh ${formatAmount(currentBalance)}"
                Text(
                    balanceText,
                    style =
                        when {
                            balanceText.length > 15 -> MaterialTheme.typography.headlineSmall
                            balanceText.length > 12 -> MaterialTheme.typography.headlineMedium
                            else -> MaterialTheme.typography.displaySmall
                        }.copy(
                            brush = primaryGradient(),
                        ),
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape) // Glass outer rim
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernSummaryItem(
                    "Amount Received",
                    summaryData.income,
                    MaterialTheme.colorScheme.tertiary,
                    Icons.Rounded.TrendingUp
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                )
                ModernSummaryItem(
                    "Amount Spent",
                    summaryData.expenses,
                    MaterialTheme.colorScheme.error,
                    Icons.Rounded.TrendingDown
                )
            }
        }
    }
}

@Composable
fun ModernSummaryItem(label: String, amount: Double, color: Color, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "Ksh ${formatAmount(amount)}",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ModernSegmentedControl(
    selectedScope: TimeScope,
    onScopeChange: (TimeScope) -> Unit
) {
    val items = remember { TimeScope.entries.filter { it != TimeScope.CUSTOM } }
    val selectedIndex = items.indexOf(selectedScope)

    val animateIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pillSelection"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp) // Slightly taller for better touch targets
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape) // Glass outer rim
            .padding(4.dp)
    ) {
        val maxWidth = maxWidth
        val itemWidth = maxWidth / items.size

        // The Gradient Sliding Pill
        Box(
            modifier = Modifier
                .offset(x = itemWidth * animateIndex)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary // Adds that "pulse" color transition
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape) // Inner highlight
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, scope ->
                val isSelected = index == selectedIndex

                val textColor by animateColorAsState(
                    targetValue = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "textColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onScopeChange(scope) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = scope.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium.copy(
                            shadow = if (isSelected) Shadow(
                                color = Color.Black.copy(alpha = 0.2f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            ) else Shadow.None
                        ),
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}