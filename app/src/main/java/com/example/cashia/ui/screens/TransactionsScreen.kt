package com.example.cashia.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.Query
import com.example.cashia.Utility.SmsRepository
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.parseMpesaMessage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.comparisons.compareByDescending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    val messages = remember {
        try {
            val fetched = SmsRepository.fetchMpesaMessages(context)
            Log.d("AndroidStudio 1Transactions", "Fetched ${fetched.size} SMS messages")
            fetched
        } catch (e: Exception) {
            Log.e("AndroidStudio 2Transactions", "Failed fetching SMS: ${e.message}")
            emptyList<String>()
        }
    }

    val transactions = remember(messages) {
        val parsed = messages.mapNotNull {
            try {
                parseMpesaMessage(it)
            } catch (e: Exception) {
                Log.e("AndroidStudio 3Transactions", "Parse error: ${e.message}")
                null
            }
        }
        Log.d(
            "AndroidStudio 4Transactions",
            "Parsed ${parsed.size} Mpesa transactions from ${messages.size} messages"
        )
        parsed
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showAll by remember { mutableStateOf(false) }

    if (searchQuery.isNotBlank()) {
        Log.d("Transactions", "Search query: $searchQuery")
    }

    val cleanedTransactions = remember(transactions) {
        transactions.mapNotNull { tx ->
            val dateOnly = tx.date.split(" ").firstOrNull() ?: return@mapNotNull null

            val parsedDate = runCatching {
                LocalDate.parse(dateOnly, DateTimeFormatter.ofPattern("dd/MM/yy"))
            }.getOrNull() ?: run {
                Log.e("Transactions", "Failed to parse date: ${tx.date}")
                return@mapNotNull null
            }

            tx.copy(date = parsedDate.toString())
        }.sortedByDescending { it.date }
    }

    val displayedTransactions = cleanedTransactions.filter { tx ->
        val txDate = runCatching { LocalDate.parse(tx.date) }.getOrNull() ?: return@filter false


        val matchesDate = showAll || txDate == selectedDate
        val matchesSearch =
            tx.recipient.contains(searchQuery, ignoreCase = true) ||
                    tx.id.contains(searchQuery, ignoreCase = true) ||
                    tx.amount.toString().contains(searchQuery)

        matchesDate && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Arrow Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            FullMonthCalendar(
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it })

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Button(
                    onClick = { showAll = !showAll },
                    modifier = Modifier
                        .padding(end = 16.dp)
                ) {
                    Text(if (showAll) "Showing All" else "Showing Selected Day")
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        Log.d("AndroidStudio 5Transactions", "Search query: '$query'")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search transactions...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (displayedTransactions.isEmpty()) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No matching transactions"
                    else if (!showAll) "No transactions on this day"
                    else "No transactions available",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedTransactions) { tx ->
                        TransactionItem(tx)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(tx: MpesaTransactions) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Transaction ID: ${tx.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                softWrap = true,
                overflow = TextOverflow.Clip

            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tx.recipient,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    softWrap = true,
                    overflow = TextOverflow.Clip
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Ksh${tx.amount}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

fun filterTransactions(all: List<MpesaTransactions>, query: String): List<MpesaTransactions> {
    val q = query.lowercase().trim()

    return all.filter {
        it.recipient.lowercase().contains(q) ||
                it.id.lowercase().contains(q) ||
                it.amount.toString().contains(q) ||
                it.date.contains(q) // matches "2025-01-17"
    }
}