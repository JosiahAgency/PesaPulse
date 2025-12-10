package com.example.cashia.models

import android.content.Context
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

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        fetchTransactions()
    }

    private fun fetchTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = SmsRepository.fetchMpesaMessages(context)
                val parsedTransactions = messages.mapNotNull { parseMpesaMessage(it) }

                val now = LocalDate.now()
                val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                val year = now.year

                val currentMonthTransactions = parsedTransactions.filter { tx ->
                    val date = try {
                        LocalDate.parse(tx.date, formatter)
                    } catch (e: Exception) {
                        null
                    }
                    date != null && date.year == now.year && date.monthValue == now.monthValue
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
                    currentMonth = "$monthName $year",
                    isLoading = false
                )
            } catch (e: Exception) {
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
        val transactions = state.allTransactions
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

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

        val scopedTransactions = transactions.filter { tx ->
            val date = try {
                LocalDate.parse(tx.date, formatter)
            } catch (e: Exception) {
                null
            }
            date != null && !date.isBefore(startDate) && !date.isAfter(endDate)
        }

        val income = scopedTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val expenses = scopedTransactions.filter { it.type == "expense" }.sumOf { it.amount }

        return SummaryData(
            income = income,
            expenses = expenses,
            startDate = startDate,
            endDate = endDate
        )
    }
}
