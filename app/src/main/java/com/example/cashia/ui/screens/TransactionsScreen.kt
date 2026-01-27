package com.example.cashia.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.TransactionsViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(viewModel: TransactionsViewModel, onNavigateBack: @Composable () -> Unit) {
    val incomeColor = MaterialTheme.colorScheme.tertiary
    val expenseColor = MaterialTheme.colorScheme.error
    val balanceColor = MaterialTheme.colorScheme.primary
    val state = viewModel.state
    var selectedTransaction by remember { mutableStateOf<MpesaTransactions?>(null) }

    val totalIncome = state.totalIncome
    val totalExpenses = state.totalExpenses
    val latestBalance = state.latestBalance
    val totalFees = remember(state.allTransactions) {
        state.allTransactions.sumOf { it.transactionCost }
    }

    var searchQuery by remember { mutableStateOf("") }

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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
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
            isRefreshing = state.isLoading,
            onRefresh = {
                viewModel.refreshFromSms()
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Your Transactions",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = state.currentMonth,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = balanceColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Running Balance",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "Ksh ${formatAmount(latestBalance)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = balanceColor
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = expenseColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Total Transactions Cost",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Ksh ${formatAmount(totalFees)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = expenseColor
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                    alpha = 0.2f
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = expenseColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Expenses",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Ksh ${formatAmount(totalExpenses)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = expenseColor
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Income",
                                        tint = incomeColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Income",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Ksh ${formatAmount(totalIncome)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = incomeColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Transactions...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
                if (state.allTransactions.isEmpty() && !state.isLoading) {
                    item {
                        EmptyStateMessage("No transactions available. Swipe down to refresh.")
                    }
                } else if (groupedTransactions.isEmpty() && searchQuery.isNotBlank()) {
                    item {
                        EmptyStateMessage("No matching transactions found.")
                    }
                } else {
                    groupedTransactions.forEach { (date, dailyTransactions) ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shadowElevation = 4.dp,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)

                                )
                            }
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
fun EmptyStateMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TransactionItem(tx: MpesaTransactions, onClick: () -> Unit) {
    val isIncome = tx.type == "income"
    val dataColor = if (isIncome) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp, 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.recipient,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = dataColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = tx.type.replaceFirstChar { it.uppercase() } + " • ID: ${tx.id}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isIncome) "+ Ksh ${formatAmount(tx.amount)}" else "- Ksh ${
                            formatAmount(
                                tx.amount
                            )
                        }",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = dataColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "${tx.date.format(DateTimeFormatter.ofPattern(" dd MMM yyyy "))} • ${tx.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val isIncome = tx.type == "income"
    val typeColor =
        if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val typeIcon = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = typeColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = if (isIncome) "Income Received" else "Payment Sent",
                style = MaterialTheme.typography.headlineSmall,
                color = typeColor
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Transaction ID", tx.id)
                DetailRow(if (isIncome) "Sender" else "Recepient", tx.recipient)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DetailRow(
                    label = "Amount",
                    value = "${if (isIncome) "+" else "-"} Ksh ${formatAmount(tx.amount)}",
                    isBold = true,
                    valueColor = typeColor
                )
                if (!isIncome) {
                    DetailRow("Transaction Cost", " - Ksh ${formatAmount(tx.transactionCost)}")
                }
                val total = if (tx.type == "expense") tx.amount + tx.transactionCost else tx.amount
                DetailRow(
                    label = if (isIncome) "Net Gain" else "Total Cost",
                    value = "${if (isIncome) "+" else "-"} Ksh ${formatAmount(total)}",
                    isBold = true,
                    valueColor = typeColor
                )
                HorizontalDivider()
                DetailRow("Date", tx.date)
                DetailRow("Time", tx.time)
                DetailRow("Balance After ", "Ksh ${formatAmount(tx.balance)}")
            }
        },
        confirmButton = {
//            TextButton(onClick = { onEdit(tx) }) {
//                Text("Edit")
//            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Normal,
                color = valueColor,
                modifier = Modifier.weight(1.2f, fill = false)
            )
        }
    }
}

fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "KE")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return formatter.format(amount)
}
