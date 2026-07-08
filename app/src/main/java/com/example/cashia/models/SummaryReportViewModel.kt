package com.example.cashia.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.daos.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class ReportPeriod { DAY, MONTH }

data class CategorySpend(val categoryId: String, val amount: Double)

data class MerchantSpend(val recipient: String, val amount: Double, val count: Int)

data class SummaryReportUiState(
    val period: ReportPeriod = ReportPeriod.DAY,
    val anchor: LocalDate = LocalDate.now(),
    val label: String = "",
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val transactionCost: Double = 0.0,
    val txCount: Int = 0,
    val prevLabel: String = "",
    val prevIncome: Double = 0.0,
    val prevExpenses: Double = 0.0,
    val categorySpends: List<CategorySpend> = emptyList(),
    val topMerchants: List<MerchantSpend> = emptyList(),
    val biggestExpense: MpesaTransactions? = null,
    val transactions: List<MpesaTransactions> = emptyList(),
    val canGoNext: Boolean = false,
) {
    val net: Double get() = income - expenses
}

/**
 * Backs the in-app detail view for daily/monthly summary notifications.
 * The anchor date + period are navigable (prev/next, day/month switch), so past
 * reports are browsable; data comes straight from Room and stays live.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SummaryReportViewModel(
    private val dao: TransactionDao,
    initialPeriod: ReportPeriod,
    initialAnchor: LocalDate,
) : ViewModel() {

    private val _period = MutableStateFlow(initialPeriod)
    private val _anchor = MutableStateFlow(initialAnchor)

    val uiState: StateFlow<SummaryReportUiState> = combine(_period, _anchor) { p, a -> p to a }
        .flatMapLatest { (period, anchor) ->
            val (start, end) = rangeOf(period, anchor)
            val (prevStart, prevEnd) = rangeOf(period, prevAnchor(period, anchor))

            combine(
                dao.getTransactionsInRange(start.toString(), end.toString()),
                dao.getTransactionsInRange(prevStart.toString(), prevEnd.toString()),
            ) { txs, prevTxs ->
                build(period, anchor, txs, prevTxs, prevStart)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SummaryReportUiState())

    // ── Actions ──────────────────────────────────────────────────────────────

    fun setPeriod(period: ReportPeriod) {
        _period.value = period
        // A month anchor (e.g. the 1st) can point at a stale day; clamp to today.
        if (period == ReportPeriod.DAY && _anchor.value.isAfter(LocalDate.now())) {
            _anchor.value = LocalDate.now()
        }
    }

    fun previous() {
        _anchor.value = prevAnchor(_period.value, _anchor.value)
    }

    fun next() {
        val candidate = when (_period.value) {
            ReportPeriod.DAY -> _anchor.value.plusDays(1)
            ReportPeriod.MONTH -> _anchor.value.plusMonths(1)
        }
        if (!rangeOf(_period.value, candidate).first.isAfter(LocalDate.now())) {
            _anchor.value = candidate
        }
    }

    // ── Computation ──────────────────────────────────────────────────────────

    private fun rangeOf(period: ReportPeriod, anchor: LocalDate): Pair<LocalDate, LocalDate> =
        when (period) {
            ReportPeriod.DAY -> anchor to anchor
            ReportPeriod.MONTH ->
                anchor.with(TemporalAdjusters.firstDayOfMonth()) to
                        anchor.with(TemporalAdjusters.lastDayOfMonth())
        }

    private fun prevAnchor(period: ReportPeriod, anchor: LocalDate): LocalDate = when (period) {
        ReportPeriod.DAY -> anchor.minusDays(1)
        ReportPeriod.MONTH -> anchor.minusMonths(1)
    }

    private fun label(period: ReportPeriod, anchor: LocalDate): String = when (period) {
        ReportPeriod.DAY -> when (anchor) {
            LocalDate.now() -> "Today"
            LocalDate.now().minusDays(1) -> "Yesterday"
            else -> anchor.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy"))
        }

        ReportPeriod.MONTH -> anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    private fun build(
        period: ReportPeriod,
        anchor: LocalDate,
        txs: List<MpesaTransactions>,
        prevTxs: List<MpesaTransactions>,
        prevStart: LocalDate,
    ): SummaryReportUiState {
        var income = 0.0
        var expenses = 0.0
        var cost = 0.0
        val categoryTotals = mutableMapOf<String, Double>()
        val merchantTotals = mutableMapOf<String, Pair<Double, Int>>()
        var biggest: MpesaTransactions? = null

        for (tx in txs) {
            cost += tx.transactionCost
            when (tx.type) {
                "income" -> income += tx.amount

                "expense" -> {
                    val total = tx.amount + tx.transactionCost
                    expenses += total
                    categoryTotals.merge(tx.category, total, Double::plus)
                    merchantTotals.merge(tx.recipient, total to 1) { (a, n), (b, m) ->
                        (a + b) to (n + m)
                    }
                    if (tx.amount > (biggest?.amount ?: 0.0)) biggest = tx
                }
            }
        }

        var prevIncome = 0.0
        var prevExpenses = 0.0
        for (tx in prevTxs) {
            when (tx.type) {
                "income" -> prevIncome += tx.amount
                "expense" -> prevExpenses += tx.amount + tx.transactionCost
            }
        }

        return SummaryReportUiState(
            period = period,
            anchor = anchor,
            label = label(period, anchor),
            income = income,
            expenses = expenses,
            transactionCost = cost,
            txCount = txs.size,
            prevLabel = label(period, prevStart),
            prevIncome = prevIncome,
            prevExpenses = prevExpenses,
            categorySpends = categoryTotals.entries
                .sortedByDescending { it.value }
                .take(6)
                .map { CategorySpend(it.key, it.value) },
            topMerchants = merchantTotals.entries
                .sortedByDescending { it.value.first }
                .take(5)
                .map { MerchantSpend(it.key, it.value.first, it.value.second) },
            biggestExpense = biggest,
            transactions = txs,
            canGoNext = rangeOf(period, anchor).second < LocalDate.now(),
        )
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    companion object {
        fun provideFactory(
            dao: TransactionDao,
            period: String,
            isoDate: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val p = if (period.equals("month", ignoreCase = true)) ReportPeriod.MONTH
                else ReportPeriod.DAY
                val anchor = runCatching { LocalDate.parse(isoDate) }
                    .getOrDefault(LocalDate.now())
                return SummaryReportViewModel(dao, p, anchor) as T
            }
        }
    }
}
