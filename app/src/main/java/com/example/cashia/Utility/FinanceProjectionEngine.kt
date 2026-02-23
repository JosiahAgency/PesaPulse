package com.example.cashia.Utility

import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.MpesaTransactions
import java.time.LocalDate

object FinanceProjectionEngine {

    fun project(
        transactions: List<MpesaTransactions>,
        currentBalance: Double,
        daysForward: Int = 30
    ): FinanceProjection {
        if (transactions.isEmpty()) {
            return FinanceProjection(0.0, 0.0, 0.0, currentBalance, emptyList())
        }

        // ── Build daily net map ───────────────────────────────────────────────
        val dailyNet = transactions
            .groupBy {
                try {
                    LocalDate.parse(it.date)
                } catch (e: Exception) {
                    null
                }
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
            return FinanceProjection(0.0, 0.0, 0.0, currentBalance, emptyList())
        }

        // ── Fill any gaps in date range so slope calculation is uniform ───────
        val firstDay = dailyNet.keys.first()
        val lastDay = dailyNet.keys.last()
        val allDays = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(lastDay) }
            .toList()
        val filledValues = allDays.map { dailyNet.getOrDefault(it, 0.0) }

        // ── Linear regression slope ──────────────────────────────────────────
        val slope = calculateSlope(filledValues)

        // ── Weighted 7-day average daily net (recent bias) ───────────────────
        val recentDays = filledValues.takeLast(7)
        val weightedAvgDaily = if (recentDays.isNotEmpty()) {
            val weights = recentDays.indices.map { (it + 1).toDouble() }
            val weightedSum = recentDays.zip(weights).sumOf { (v, w) -> v * w }
            weightedSum / weights.sum()
        } else slope

        // Blend slope with recent weighted average for a more responsive projection
        val projectedDailyNet = slope * 0.4 + weightedAvgDaily * 0.6

        // ── Build future trend ────────────────────────────────────────────────
        var balance = currentBalance
        val futureTrend = mutableListOf<Pair<LocalDate, Double>>()
        var lastDate = lastDay

        repeat(daysForward) {
            lastDate = lastDate.plusDays(1)
            balance += projectedDailyNet
            futureTrend.add(lastDate to balance)
        }

        // ── Predicted income / expenses ───────────────────────────────────────
        val observedDays = filledValues.size.coerceAtLeast(1)

        val dailyAvgIncome = transactions
            .filter { it.type == "income" }
            .sumOf { it.amount } / observedDays

        val dailyAvgExpense = transactions
            .filter { it.type == "expense" }
            .sumOf { it.amount + it.transactionCost } / observedDays

        val predictedIncome = dailyAvgIncome * daysForward
        val predictedExpenses = dailyAvgExpense * daysForward
        val predictedNet = projectedDailyNet * daysForward

        return FinanceProjection(
            predictedIncome = predictedIncome,
            predictedExpenses = predictedExpenses,
            predictedNet = predictedNet,
            predictedBalance = balance,
            dailyTrend = futureTrend
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