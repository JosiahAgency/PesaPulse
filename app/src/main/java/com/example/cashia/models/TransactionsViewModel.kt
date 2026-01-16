package com.example.cashia.models

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.AppDatabase
import com.example.cashia.Utility.SmsRepository
import com.example.cashia.Utility.TimeScope
import com.example.cashia.Utility.syncTransactions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class TransactionState(
    val allTransactions: List<MpesaTransactions> = emptyList(),
    val latestBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val currentMonth: String = "",
    val isLoading: Boolean = true
)

data class SummaryData(
    val income: Double,
    val expenses: Double,
    val startDate: LocalDate,
    val endDate: LocalDate
)

class TransactionsViewModel(private val context: Context) : ViewModel() {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.transactionDao()

    var state by mutableStateOf(TransactionState())
        private set

    private var transactionsByDate: Map<LocalDate, List<MpesaTransactions>> = emptyMap()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE


    init {
        observeDatabase()
        refreshFromSms()
    }

    fun onPermissionGranted() {
        refreshFromSms()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            dao.getAllTransactions().collect { transactions ->
                updateStateWith(transactions)
            }
        }
    }

    fun refreshFromSms() {
        viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(isLoading = true)
            try {
                syncTransactions(context, database)
            } catch (e: Exception) {
                Log.e("PesaPulse", "Sync failed", e)
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    private fun updateStateWith(transactions: List<MpesaTransactions>) {
        val now = LocalDate.now()
        val currentMonthTransactions = transactions.filter {
            val date = LocalDate.parse(it.date)
            date.year == now.year && date.monthValue == now.monthValue
        }
        transactionsByDate = transactions.groupBy { LocalDate.parse(it.date) }

        state = state.copy(
            allTransactions = transactions,
            latestBalance = currentMonthTransactions.firstOrNull()?.balance ?: 0.0,
            totalIncome = currentMonthTransactions.filter { it.type == "income" }
                .sumOf { it.amount },
            totalExpenses = currentMonthTransactions.filter { it.type == "expense" }
                .sumOf { it.amount },
            currentMonth = "${
                now.month.getDisplayName(
                    TextStyle.FULL,
                    Locale.getDefault()
                )
            } ${now.year}",
            isLoading = false

        )
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

    fun calculateSummary(selectedDate: LocalDate, scope: TimeScope): SummaryData {
        val (startDate, enDate) = when (scope) {
            TimeScope.DAY -> selectedDate to selectedDate
            TimeScope.WEEK -> selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) to selectedDate.with(
                TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY)
            )

            TimeScope.MONTH -> selectedDate.withDayOfMonth(1) to selectedDate.with(TemporalAdjusters.lastDayOfMonth())
            TimeScope.CUSTOM -> selectedDate to selectedDate
        }

        val filtered = state.allTransactions.filter {
            val d = LocalDate.parse(it.date)
            !d.isBefore(startDate) && !d.isAfter(enDate)
        }

        return SummaryData(
            income = filtered.filter { it.type == "income" }.sumOf { it.amount },
            expenses = filtered.filter { it.type == "expense" }.sumOf { it.amount },
            startDate = startDate,
            endDate = enDate
        )
    }
}
