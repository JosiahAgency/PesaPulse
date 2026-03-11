package com.example.cashia.Utility

import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.RecurrenceType
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.sqrt

object FinanceProjectionEngine {

    fun project(
        transactions: List<MpesaTransactions>,
        currentBalance: Double,
        balanceAsOfDate: LocalDate,
        daysForward: Int = 30,
        upcomingTransactions: List<UpcomingTransaction> = emptyList()  // ← new
    ): FinanceProjection {
        val today = LocalDate.now()

        if (transactions.isEmpty()) return emptyProjection(
            currentBalance,
            "No transaction history to base projection on."
        )

        val relevantTx = transactions.filter {
            try {
                LocalDate.parse(it.date) <= balanceAsOfDate
            } catch (e: Exception) {
                false
            }
        }

        // ── 1. Build filled daily nets ────────────────────────────────────
        val dailyNet = relevantTx
            .groupBy { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .filterKeys { it != null }.mapKeys { it.key!! }
            .mapValues { (_, list) ->
                list.sumOf {
                    if (it.type == "income") it.amount else -(it.amount + it.transactionCost)
                }
            }.toSortedMap()

        if (dailyNet.isEmpty()) return emptyProjection(
            currentBalance,
            "Could not parse transaction dates."
        )

        val firstDay = dailyNet.keys.first()
        val filledValues = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(balanceAsOfDate) }
            .map { dailyNet.getOrDefault(it, 0.0) }
            .toList()

        val observedDays = filledValues.size.coerceAtLeast(1)

        // ── 2. Outlier detection ──────────────────────────────────────────
        val nonZero = filledValues.filter { it != 0.0 }
        val mean = if (nonZero.isNotEmpty()) nonZero.average() else 0.0
        val stddev = if (nonZero.size > 1)
            sqrt(nonZero.sumOf { (it - mean) * (it - mean) } / (nonZero.size - 1))
        else 0.0
        val outlierThreshold = abs(mean) + 2.5 * stddev

        val regularValues = filledValues.filter { abs(it) <= outlierThreshold || stddev == 0.0 }
        val outlierValues = filledValues.filter { abs(it) > outlierThreshold && stddev > 0.0 }

        // ── 3. Daily rate from regular + amortised outliers ───────────────
        val recentRegular = regularValues.takeLast(21).filter { it != 0.0 }
        val regularDailyRate = if (recentRegular.isNotEmpty()) {
            val weights = recentRegular.indices.map { (it + 1).toDouble() }
            recentRegular.zip(weights).sumOf { (v, w) -> v * w } / weights.sum()
        } else calculateSlope(regularValues)

        val totalOutlierNet = outlierValues.sum()
        val outlierDailyRate = totalOutlierNet / observedDays.toDouble()
        val outlierWeight = when {
            observedDays >= 60 -> 1.0
            observedDays >= 30 -> 0.7
            else -> 0.4
        }
        val projectedDailyNet = regularDailyRate + (outlierDailyRate * outlierWeight)
        val slope = calculateSlope(regularValues)
        val baseDailyNet = projectedDailyNet * 0.85 + slope * 0.15

        // ── 4. Bridge gap to today ────────────────────────────────────────
        val gapDays = ChronoUnit.DAYS.between(balanceAsOfDate, today).coerceAtLeast(0)
        val estimatedTodayBalance = currentBalance + (baseDailyNet * gapDays)

        // ── 5. Build upcoming cash flow map ───────────────────────────────
        // Map each upcoming transaction to its impact on a specific future date.
        // Recurring ones are expanded across the forecast window.
        val upcomingByDate = mutableMapOf<LocalDate, Double>()

        upcomingTransactions.forEach { upcoming ->
            val dueDate =
                runCatching { LocalDate.parse(upcoming.dueDate) }.getOrNull() ?: return@forEach
            val impact = when (upcoming.type) {
                UpcomingType.INCOME, UpcomingType.SAVING -> upcoming.amount
                UpcomingType.EXPENSE -> -upcoming.amount
            }

            // Expand recurrence across the forecast window
            var date = dueDate
            val endDate = today.plusDays(daysForward.toLong())
            // For recurring items that started before today, fast-forward to next future occurrence
            if (upcoming.recurrence != RecurrenceType.NONE) {
                while (date.isBefore(today)) {
                    date = nextDueDate(date, upcoming.recurrence)
                }
            }
            while (!date.isAfter(endDate)) {
                if (!date.isBefore(today)) {
                    upcomingByDate[date] = (upcomingByDate[date] ?: 0.0) + impact
                }
                if (upcoming.recurrence == RecurrenceType.NONE) break
                date = nextDueDate(date, upcoming.recurrence)
            }
        }

        // ── 6. Project forward, injecting upcoming on their dates ─────────
        var runningBalance = estimatedTodayBalance
        val futureTrend = mutableListOf<Pair<LocalDate, Double>>()
        // Track which dates have upcoming injections for the chart markers
        val upcomingMarkers =
            mutableListOf<Triple<LocalDate, Double, String>>() // date, balance after, title

        repeat(daysForward) { dayOffset ->
            val date = today.plusDays(dayOffset.toLong() + 1)
            // Base algorithmic drift
            runningBalance += baseDailyNet
            // Layer in known upcoming transactions on top
            val upcomingImpact = upcomingByDate[date] ?: 0.0
            if (upcomingImpact != 0.0) {
                runningBalance += upcomingImpact
                // Find title for marker
                val title = upcomingTransactions
                    .firstOrNull { runCatching { LocalDate.parse(it.dueDate) }.getOrNull() == date }
                    ?.title ?: "Scheduled"
                upcomingMarkers.add(Triple(date, runningBalance, title))
            }
            futureTrend.add(date to runningBalance)
        }

        // ── 7. Summary totals ─────────────────────────────────────────────
        val dailyAvgIncome = relevantTx.filter { it.type == "income" }
            .sumOf { it.amount } / observedDays
        val dailyAvgExpense = relevantTx.filter { it.type == "expense" }
            .sumOf { it.amount + it.transactionCost } / observedDays
        val rawNet = dailyAvgIncome - dailyAvgExpense
        val scaleFactor = if (rawNet != 0.0) baseDailyNet / rawNet else 1.0

        // Add upcoming totals to predicted income/expense
        val upcomingIncome = upcomingTransactions
            .filter { it.type == UpcomingType.INCOME || it.type == UpcomingType.SAVING }
            .sumOf { it.amount }
        val upcomingExpense = upcomingTransactions
            .filter { it.type == UpcomingType.EXPENSE }
            .sumOf { it.amount }

        val predictedIncome = ((dailyAvgIncome * scaleFactor) * daysForward) + upcomingIncome
        val predictedExpenses = ((dailyAvgExpense * scaleFactor) * daysForward) + upcomingExpense
        val predictedNet = baseDailyNet * daysForward + upcomingByDate.values.sum()

        // ── 8. Data quality notes ─────────────────────────────────────────
        val dataQualityNote = buildString {
            if (gapDays > 3) append("Balance is ${gapDays}d old — estimated from last known. ")
            if (observedDays < 14) append("Only ${observedDays}d of history — treat as rough estimate. ")
            if (outlierValues.isNotEmpty())
                append("${outlierValues.size} large transaction(s) amortised across window. ")
            if (upcomingTransactions.isNotEmpty())
                append("${upcomingTransactions.size} scheduled payment(s) factored in. ")
            if (recentRegular.size < 3) append("Very few regular transactions — low confidence.")
        }.trim()

        return FinanceProjection(
            predictedIncome = predictedIncome.coerceAtLeast(0.0),
            predictedExpenses = predictedExpenses.coerceAtLeast(0.0),
            predictedNet = predictedNet,
            predictedBalance = futureTrend.lastOrNull()?.second ?: estimatedTodayBalance,
            dailyTrend = futureTrend,
            projectedDailyNet = baseDailyNet,
            estimatedTodayBalance = estimatedTodayBalance,
            dataQualityNote = dataQualityNote,
            upcomingMarkers = upcomingMarkers  // ← new field
        )
    }

    private fun nextDueDate(from: LocalDate, recurrence: RecurrenceType) = when (recurrence) {
        RecurrenceType.WEEKLY -> from.plusWeeks(1)
        RecurrenceType.MONTHLY -> from.plusMonths(1)
        RecurrenceType.YEARLY -> from.plusYears(1)
        RecurrenceType.NONE -> from
    }

    private fun emptyProjection(balance: Double, note: String) = FinanceProjection(
        predictedIncome = 0.0, predictedExpenses = 0.0, predictedNet = 0.0,
        predictedBalance = balance, dailyTrend = emptyList(),
        projectedDailyNet = 0.0, dataQualityNote = note
    )

    private fun calculateSlope(values: List<Double>): Double {
        val n = values.size
        if (n < 2) return 0.0
        val xMean = (0 until n).average()
        val yMean = values.average()
        var num = 0.0
        var den = 0.0
        for (i in values.indices) {
            num += (i - xMean) * (values[i] - yMean)
            den += (i - xMean) * (i - xMean)
        }
        return if (den == 0.0) 0.0 else num / den
    }
}