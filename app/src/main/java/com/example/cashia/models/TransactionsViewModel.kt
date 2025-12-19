package com.example.cashia.models

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.Utility.SmsRepository
import com.example.cashia.Utility.TimeScope
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
    var state by mutableStateOf(TransactionState())
        private set

    private var transactionsByDate: Map<LocalDate, List<MpesaTransactions>> = emptyMap()
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE


    init {
        fetchTransactions()
    }

    fun onPermissionGranted() {
        if (state.allTransactions.isEmpty()) {
            fetchTransactions()
        }
    }

    fun fetchTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            state = state.copy(isLoading = true)
            try {
                val messages = SmsRepository.fetchMpesaMessages(context)
                val parsedTransactions = messages.mapNotNull { parseMpesaMessage(it) }

                transactionsByDate = parsedTransactions.groupBy {
                    LocalDate.parse(it.date, formatter)
                }

                val now = LocalDate.now()
                val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

                val currentMonthTransactions = parsedTransactions.filter { tx ->
                    val date = LocalDate.parse(tx.date, formatter)
                    date.year == now.year && date.monthValue == now.monthValue
                }

                val latestBalance = currentMonthTransactions
                    .maxByOrNull { LocalDate.parse(it.date, formatter) }
                    ?.balance ?: 0.0

                val totalIncome = currentMonthTransactions
                    .filter { it.type == "income" }
                    .sumOf { it.amount }

                val totalExpenses = currentMonthTransactions
                    .filter { it.type == "expense" }
                    .sumOf { it.amount }

                state = state.copy(
                    allTransactions = parsedTransactions,
                    latestBalance = latestBalance,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    currentMonth = "$monthName ${now.year}",
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("PesaPulse Error", "Error fetching", e)
            } finally {
                state = state.copy(isLoading = false)
            }
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

    fun calculateSummary(selectedDate: LocalDate, scope: TimeScope): SummaryData {
        val startDate = when (scope) {
            TimeScope.DAY -> selectedDate
            TimeScope.WEEK -> selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
            TimeScope.MONTH -> selectedDate.withDayOfMonth(1)
            TimeScope.CUSTOM -> selectedDate
        }

        val endDate = when (scope) {
            TimeScope.DAY -> selectedDate
            TimeScope.WEEK -> selectedDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            TimeScope.MONTH -> selectedDate.with(TemporalAdjusters.lastDayOfMonth())
            TimeScope.CUSTOM -> selectedDate
        }

        var totalIncome = 0.0
        var totalExpenses = 0.0

        var current = startDate
        while (!current.isAfter(endDate)) {
            transactionsByDate[current]?.forEach { tx ->
                if (tx.type == "income") totalIncome += tx.amount
                else if (tx.type == "expense") totalExpenses += tx.amount
            }
            current = current.plusDays(1)
        }
        return SummaryData(totalIncome, totalExpenses, startDate, endDate)
    }
}
