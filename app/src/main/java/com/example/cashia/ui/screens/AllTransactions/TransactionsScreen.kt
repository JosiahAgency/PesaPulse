package com.example.cashia.ui.screens.AllTransactions

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import com.example.cashia.ui.screens.Analytics.formatCompactAmount
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(viewModel: TransactionsViewModel, onNavigateBack: @Composable () -> Unit) {
    val state = viewModel.state
    var selectedTransaction by remember { mutableStateOf<MpesaTransactions?>(null) }
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }

    val colorScheme = MaterialTheme.colorScheme
    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.92f else 0.78f

    val filteredTransactions by remember(searchQuery, state.allTransactions) {
        derivedStateOf {
            state.allTransactions.filter {
                it.recipient.contains(searchQuery, ignoreCase = true) ||
                        it.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val groupedTransactions by remember(filteredTransactions) {
        derivedStateOf {
            filteredTransactions
                .groupBy { LocalDate.parse(it.date) }
                .toSortedMap(compareByDescending { it })
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
    ) { padding ->
        selectedTransaction?.let { tx ->
            TransactionDetailDialog(
                tx = tx,
                onDismiss = { selectedTransaction = null },
                onEdit = { /* Handle Edit logic */ },
                onDelete = {

                }
            )
        }
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = {
                viewModel.refreshFromSms()
            },
            modifier = Modifier
                .padding()
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CompactTransactionSummary(
                        balance = state.latestBalance,
                        income = state.totalIncome,
                        expenses = state.totalExpenses
                    )
                }
                item {
                    ModernSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (state.allTransactions.isEmpty() && !state.isLoading) {
                    item {
                        EmptyStateMessage("No History Found")
                    }
                } else {
                    groupedTransactions.forEach { (date, dailyTransactions) ->
                        stickyHeader {
                            ModernStickyHeader(date)
                        }

                        items(items = dailyTransactions, key = { it.id }) { tx ->
                            TransactionItem(tx, onClick = { selectedTransaction = tx })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactTransactionSummary(balance: Double, income: Double, expenses: Double) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surface.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummarySmallItem(
                "Income",
                income,
                MaterialTheme.colorScheme.tertiary,
                icon = Icons.Default.TrendingUp
            )
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorScheme.primary.copy(alpha = 0.08f))
                    .border(
                        0.5.dp,
                        colorScheme.primary.copy(alpha = 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Ksh ${formatAmount(balance)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black, // Consistent with dashboard headers
                    color = colorScheme.onSurface
                )
            }
            SummarySmallItem(
                "Expenses", expenses, colorScheme.error, icon = Icons.Default.TrendingDown,
                alignEnd = true
            )
        }
    }
}

@Composable
fun SummarySmallItem(
    label: String, amount: Double, color: Color, icon: ImageVector,
    alignEnd: Boolean = false
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!alignEnd) Icon(
                icon,
                null,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
            if (alignEnd) Icon(
                icon,
                null,
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = formatCompactAmount(amount), // Use compact for small items
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun ModernSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape, // Fully rounded for modern look
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ModernStickyHeader(date: LocalDate) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM")).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TransactionItem(tx: MpesaTransactions, onClick: () -> Unit) {
    val isIncome = tx.type.lowercase() == "income"
    val colorScheme = MaterialTheme.colorScheme

    val glassAlpha = if (colorScheme.background.luminance() > 0.5f) 0.6f else 0.4f
    val accentColor = if (isIncome) colorScheme.tertiary else colorScheme.error

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surface.copy(alpha = glassAlpha),
        border = BorderStroke(0.5.dp, colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center, modifier = Modifier
                    .size(48.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.recipient,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${tx.time} • ${tx.type.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isIncome) "+${formatAmount(tx.amount)}" else "-${formatAmount(tx.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum" // Tabular figures for alignment
                    ),
                    color = accentColor
                )
                Text(
                    text = "Ksh",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TransactionDetailDialog(
    tx: MpesaTransactions,
    onDismiss: () -> Unit,
    onEdit: (MpesaTransactions) -> Unit,
    onDelete: (MpesaTransactions) -> Unit
) {
    val context = LocalContext.current
    val isIncome = tx.type.lowercase() == "income"
    val colorScheme = MaterialTheme.colorScheme
    val accentColor =
        if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val headerBg = accentColor.copy(alpha = 0.1f)
    val total = if (!isIncome) tx.amount + tx.transactionCost else tx.amount

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            shape = RoundedCornerShape(28.dp),
            color = colorScheme.surface.copy(alpha = 1f),
            border = BorderStroke(0.6.dp, colorScheme.onSurface.copy(alpha = 0.12f)),
            tonalElevation = 10.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp, 4.dp)
                        .background(headerBg, CircleShape)
                        .border(0.dp, accentColor.copy(alpha = 0.2f), CircleShape)
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = tx.recipient,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isIncome) "Transaction Received" else "Outgoing Payment",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(15.dp))

                Text(
                    text = "${if (isIncome) "+" else "-"} Ksh ${formatAmount(total)}",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                    ),
                    color = if (isIncome) colorScheme.tertiary else colorScheme.error
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.04f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailRow("REFERENCE", tx.id)
                    DetailRow("TIMESTAMP", "${tx.time}, ${tx.date}")
                    if (!isIncome) {
                        DetailRow("SUBTOTAL", "Ksh ${formatAmount(tx.amount)}")
                        DetailRow("FEES", "Ksh ${formatAmount(tx.transactionCost)}")
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colorScheme.onSurface.copy(alpha = 0.06f))
                    )

                    DetailRow(
                        "BALANCE AFTER TRANSACTION",
                        "Ksh ${formatAmount(tx.balance)}",
                        isBold = true
                    )
                }

                Spacer(Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Close", color = colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            Toast.makeText(
                                context,
                                "Feature coming soon! Josiah is still polishing the pulse. ✨",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.error.copy(alpha = 0.1f),
                            contentColor = colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = null
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontFamily = if (value.any { it.isDigit() }) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (isBold) FontWeight.Black else FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(2f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(amount)
}
