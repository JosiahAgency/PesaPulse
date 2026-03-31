package com.example.cashia.models

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cashia.AppDatabase
import com.example.cashia.utility.PesaTimeConfiguration
import com.example.cashia.utility.TimeScope
import com.example.cashia.utility.syncTransactions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class TransactionState(
    val allTransactions: List<MpesaTransactions> = emptyList(),
    val latestBalance: Double = 0.0,
    val latestBalanceDate: String = "",
    val latestBalanceDateParsed: LocalDate = LocalDate.now(),
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val totalTransactionCost: Double = 0.0,
    val currentMonth: String = "",
    val isLoading: Boolean = true,
    val selectedCategory: String? = null,
)

data class SummaryData(
    val income: Double,
    val expenses: Double,
    val startDate: LocalDate,
    val endDate: LocalDate
)

@Entity(tableName = "UnmatchedSms")
data class UnmatchedSms(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawBody: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val reviewed: Boolean = false
)


@OptIn(FlowPreview::class)
class TransactionsViewModel(private val context: Context) : ViewModel() {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.transactionDao()

    var state by mutableStateOf(TransactionState())
        private set

    var isBalanceVisible by mutableStateOf(false)
        private set

    var onNewTransactionsSynced: ((List<MpesaTransactions>) -> Unit)? = null
    val pendingReviewCount: StateFlow<Int> = database.unmatchedSmsDao()
        .pendingCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val nextPending: StateFlow<UnmatchedSms?> = database.unmatchedSmsDao()
        .nextPendingFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val allUnmatched: StateFlow<List<UnmatchedSms>> = database.unmatchedSmsDao()
        .allUnmatchedFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        observeDatabase()
        refreshFromSms()
        reCategorizeExisting()
    }

    fun toggleBalanceVisibility() {
        isBalanceVisible = !isBalanceVisible
    }

    fun onPermissionGranted() {
        refreshFromSms()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            dao.getAllTransactions()
                .debounce(150)
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect { transactions ->
                    val newState = computeState(transactions)
                    withContext(Dispatchers.Main) {
                        state = newState
                    }
                }
        }
    }

    fun refreshFromSms() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { state = state.copy(isLoading = true) }
            try {
                val beforeIds = dao.getAllTransactionIdsOnce()
                syncTransactions(context, database)
                val afterIds = dao.getAllTransactionIdsOnce()
                val newIds = afterIds - beforeIds.toSet()
                if (newIds.isNotEmpty()) {
                    val newTxs = dao.getTransactionsByIds(newIds.toList())
                    withContext(Dispatchers.Main) {
                        onNewTransactionsSynced?.invoke(newTxs)
                    }
                }

            } catch (e: Exception) {
                Log.e("PesaPulse", "Sync failed", e)
            } finally {
                withContext(Dispatchers.Main) { state = state.copy(isLoading = false) }
            }
        }
    }

    private fun computeState(transactions: List<MpesaTransactions>): TransactionState {
        val now = LocalDate.now()

        val latestBalanceTx = transactions
            .filter { it.balance > 0.0 }
            .maxByOrNull { it.timestamp }

        val latestBalance = latestBalanceTx?.balance ?: 0.0
        val latestBalanceDateParsed = latestBalanceTx?.let {
            runCatching { LocalDate.parse(it.date) }.getOrDefault(now)
        } ?: now
        val latestBalanceDate = latestBalanceDateParsed
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

        // Only iterate once for current month totals
        var monthIncome = 0.0
        var monthExpenses = 0.0
        transactions.forEach { tx ->
            val date = runCatching { LocalDate.parse(tx.date) }.getOrNull() ?: return@forEach
            if (date.year == now.year && date.monthValue == now.monthValue) {
                when (tx.type) {
                    "income" -> monthIncome += tx.amount + tx.transactionCost
                    "expense" -> monthExpenses += tx.amount + tx.transactionCost
                }
            }
        }

        return state.copy(
            allTransactions = transactions,
            latestBalance = latestBalance,
            latestBalanceDate = latestBalanceDate,
            latestBalanceDateParsed = latestBalanceDateParsed,
            totalIncome = monthIncome,
            totalExpenses = monthExpenses,
            currentMonth = "${
                now.month.getDisplayName(
                    TextStyle.FULL,
                    Locale.getDefault()
                )
            } ${now.year}",
            isLoading = false
        )
    }

    fun filterByCategory(category: String?) {
        state = state.copy(selectedCategory = category)
    }

    fun updateCategory(txId: String, newCategory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCategory(txId, newCategory)
        }
    }

    fun reCategorizeExisting() {
        viewModelScope.launch(Dispatchers.IO) {
            val uncategorized = dao.getUncategorizedTransactions()
            uncategorized.forEach { tx ->
                val guessed = guessCategory(tx.recipient, tx.type)
                if (guessed != TransactionCategory.OTHER.name) {
                    dao.updateCategory(tx.id, guessed)
                }
            }
        }
    }

    fun calculateSummary(selectedDate: LocalDate, scope: TimeScope): SummaryData {
        val (startDate, enDate) = when (scope) {
            TimeScope.DAY -> selectedDate to selectedDate
            TimeScope.WEEK -> {
                val firstDayOfWeek = PesaTimeConfiguration.firstDayOfWeek
                val startOfWeek =
                    selectedDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                val end = startOfWeek.plusDays(6)
                startOfWeek to end
            }

            TimeScope.MONTH -> selectedDate.withDayOfMonth(1) to selectedDate.with(TemporalAdjusters.lastDayOfMonth())
            TimeScope.YEAR -> selectedDate.withDayOfYear(1) to selectedDate.withDayOfYear(
                selectedDate.lengthOfYear()
            )

            TimeScope.CUSTOM -> selectedDate to selectedDate
        }

        val filtered = state.allTransactions.filter {
            val d = LocalDate.parse(it.date)
            !d.isBefore(startDate) && !d.isAfter(enDate)
        }
        return SummaryData(
            income = filtered.filter { it.type == "income" }
                .sumOf { it.amount + it.transactionCost },
            expenses = filtered.filter { it.type == "expense" }
                .sumOf { it.amount + it.transactionCost },
            startDate = startDate,
            endDate = enDate
        )
    }

    fun saveManualTransaction(source: UnmatchedSms, type: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val transaction = when (val r = parseMpesaMessage(source.rawBody, source.receivedAt)) {
                is ParseResult.Success -> r.transactions.copy(type = type, category = category)
                else -> MpesaTransactions(
                    id = "MANUAL_${source.id}_${source.receivedAt}",
                    amount = 0.0,
                    transactionCost = 0.0,
                    recipient = "Unknown (manual entry)",
                    date = Instant.ofEpochMilli(source.receivedAt)  // ← use receivedAt, not LocalDate.now()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .toString(),
                    time = java.time.LocalTime.now()
                        .format(
                            DateTimeFormatter.ofPattern(
                                "h:mm a",
                                Locale.US
                            )
                        ),
                    timestamp = source.receivedAt,
                    type = type,
                    balance = 0.0,
                    category = category
                )
            }
            database.transactionDao().insertTransactions(listOf(transaction))
            database.unmatchedSmsDao().delete(source)
        }
    }

    fun markSkipped(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            database.unmatchedSmsDao().markReviewed(id)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
                        return TransactionsViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
