package com.example.cashia.Utility

import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.MpesaTransactions
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object FinanceProjectionEngine {

    fun project(
        transactions: List<MpesaTransactions>,
        currentBalance: Double,       // balance from most recent SMS — post-transaction balance
        balanceAsOfDate: LocalDate,   // ← NEW: date of that SMS, not necessarily today
        daysForward: Int = 30
    ): FinanceProjection {

        val today = LocalDate.now()

        if (transactions.isEmpty()) {
            return FinanceProjection(
                predictedIncome = 0.0,
                predictedExpenses = 0.0,
                predictedNet = 0.0,
                predictedBalance = currentBalance,
                dailyTrend = emptyList(),
                projectedDailyNet = 0.0,
                dataQualityNote = "No transaction history to base projection on."
            )
        }

        // ── 1. Build daily net from transaction history ───────────────────────
        // Only use transactions up to and including balanceAsOfDate so we don't
        // double-count anything that happened after the last known balance snapshot
        val relevantTx = transactions.filter {
            try { LocalDate.parse(it.date) <= balanceAsOfDate } catch (e: Exception) { false }
        }

        val dailyNet = relevantTx
            .groupBy {
                try { LocalDate.parse(it.date) } catch (e: Exception) { null }
            }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .mapValues { (_, list) ->
                list.sumOf {
                    if (it.type == "income") it.amount
                    else -(it.amount + it.transactionCost)
                }
            }
            .toSortedMap()

        if (dailyNet.isEmpty()) {
            return FinanceProjection(
                predictedIncome = 0.0,
                predictedExpenses = 0.0,
                predictedNet = 0.0,
                predictedBalance = currentBalance,
                dailyTrend = emptyList(),
                projectedDailyNet = 0.0,
                dataQualityNote = "Could not parse transaction dates."
            )
        }

        // ── 2. Fill date gaps so regression isn't skewed by sparse data ───────
        val firstDay = dailyNet.keys.first()
        val filledValues = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(balanceAsOfDate) }
            .map { dailyNet.getOrDefault(it, 0.0) }
            .toList()

        val observedDays = filledValues.size.coerceAtLeast(1)

        // ── 3. Slope from linear regression on filled daily nets ──────────────
        val slope = calculateSlope(filledValues)

        val recentActiveDays = filledValues
            .takeLast(14)
            .filter { it != 0.0 }

        val weightedAvgDaily = if (recentActiveDays.isNotEmpty()) {
            val weights = recentActiveDays.indices.map { (it + 1).toDouble() }
            val weightedSum = recentActiveDays.zip(weights).sumOf { (v, w) -> v * w }
            weightedSum / weights.sum()
        } else {
            slope
        }

        val projectedDailyNet = slope * 0.4 + weightedAvgDaily * 0.6

        val gapDays = ChronoUnit.DAYS.between(balanceAsOfDate, today).coerceAtLeast(0)
        val estimatedTodayBalance = currentBalance + (projectedDailyNet * gapDays)

        var runningBalance = estimatedTodayBalance
        val futureTrend = mutableListOf<Pair<LocalDate, Double>>()

        repeat(daysForward) { dayOffset ->
            val date = today.plusDays(dayOffset.toLong() + 1)
            runningBalance += projectedDailyNet
            futureTrend.add(date to runningBalance)
        }

        val dailyAvgIncome = relevantTx
            .filter { it.type == "income" }
            .sumOf { it.amount } / observedDays

        val dailyAvgExpense = relevantTx
            .filter { it.type == "expense" }
            .sumOf { it.amount + it.transactionCost } / observedDays

        // Scale so income - expenses == predictedNet
        val rawNet = dailyAvgIncome - dailyAvgExpense
        val scaleFactor = if (rawNet != 0.0) projectedDailyNet / rawNet else 1.0
        val scaledIncome = dailyAvgIncome * scaleFactor
        val scaledExpense = dailyAvgExpense * scaleFactor

        val predictedIncome = scaledIncome * daysForward
        val predictedExpenses = scaledExpense * daysForward
        val predictedNet = projectedDailyNet * daysForward

        // ── 8. Data quality note ───────────────────────────────────────────────
        val dataQualityNote = buildString {
            if (gapDays > 3) append("Balance is ${gapDays}d old — estimated today's balance before projecting. ")
            if (observedDays < 14) append("Low data: only ${observedDays} days of history. ")
            if (recentActiveDays.size < 3) append("Very few recent transactions — treat forecast as rough estimate.")
        }.trim()

        return FinanceProjection(
            predictedIncome = predictedIncome,
            predictedExpenses = predictedExpenses,
            predictedNet = predictedNet,
            predictedBalance = futureTrend.lastOrNull()?.second ?: estimatedTodayBalance,
            dailyTrend = futureTrend,
            projectedDailyNet = projectedDailyNet,
            estimatedTodayBalance = estimatedTodayBalance,
            dataQualityNote = dataQualityNote
        )
    }

    private fun calculateSlope(values: List<Double>): Double {
        val n = values.size
        if (n < 2) return 0.0
        val xMean = (0 until n).average()
        val yMean = values.average()
        var numerator = 0.0
        var denominator = 0.0
        for (i in values.indices) {
            numerator += (i - xMean) * (values[i] - yMean)
            denominator += (i - xMean) * (i - xMean)
        }
        return if (denominator == 0.0) 0.0 else numerator / denominator
    }
}