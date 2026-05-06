package com.example.cashia.utility

import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.RecurrenceType
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

object FinanceProjectionEngine {

    fun project(
        transactions: List<MpesaTransactions>,
        currentBalance: Double,
        balanceAsOfDate: LocalDate,
        daysForward: Int = 30,
        upcomingTransactions: List<UpcomingTransaction> = emptyList()
    ): FinanceProjection {

        val today = LocalDate.now()

        if (transactions.isEmpty()) {
            return emptyProjection(currentBalance, "No transaction history.")
        }

        data class ParsedTx(val date: LocalDate, val net: Double)

        // ─────────────────────────────────────────────
        // 1. Parse once
        // ─────────────────────────────────────────────
        val parsedTx = transactions.mapNotNull {
            val date = runCatching { LocalDate.parse(it.date) }.getOrNull()
                ?: return@mapNotNull null

            if (date > balanceAsOfDate) return@mapNotNull null

            val net = if (it.type == "income") it.amount
            else -(it.amount + it.transactionCost)

            ParsedTx(date, net)
        }

        if (parsedTx.isEmpty()) {
            return emptyProjection(currentBalance, "No valid transaction data.")
        }

        // ─────────────────────────────────────────────
        // 2. Daily aggregation
        // ─────────────────────────────────────────────
        val dailyNet = mutableMapOf<LocalDate, Double>()
        parsedTx.forEach {
            dailyNet[it.date] = (dailyNet[it.date] ?: 0.0) + it.net
        }

        val firstDay = dailyNet.keys.minOrNull()!!
        val filledValues = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(balanceAsOfDate) }
            .map { dailyNet[it] ?: 0.0 }
            .toList()

        val observedDays = filledValues.size.coerceAtLeast(1)

        // ─────────────────────────────────────────────
        // 3. IQR Outliers
        // ─────────────────────────────────────────────
        val (regularValues, outliers) = filterOutliersIQR(filledValues)

        // ─────────────────────────────────────────────
        // 4. Split income / expense modeling
        // ─────────────────────────────────────────────
        val incomeSeries = parsedTx.filter { it.net > 0 }.map { it.net }
        val expenseSeries = parsedTx.filter { it.net < 0 }.map { abs(it.net) }

        val smoothedIncome = exponentialSmoothing(incomeSeries)
        val smoothedExpense = exponentialSmoothing(expenseSeries)

        val incomeSlope = calculateSlope(incomeSeries)
        val expenseSlope = calculateSlope(expenseSeries)

        val incomeTrend = smoothedIncome * 0.7 + incomeSlope * 0.3
        val expenseTrend = smoothedExpense * 0.7 + expenseSlope * 0.3

        var baseDailyNet = incomeTrend - expenseTrend

        // ─────────────────────────────────────────────
        // 5. Outlier reintegration (dampened)
        // ─────────────────────────────────────────────
        val outlierImpact = outliers.sum() / observedDays
        baseDailyNet += outlierImpact * 0.3

        // ─────────────────────────────────────────────
        // 6. Weekday modeling
        // ─────────────────────────────────────────────
        val weekdayAverages = parsedTx
            .groupBy { it.date.dayOfWeek }
            .mapValues { (_, list) -> list.map { it.net }.average() }

        // ─────────────────────────────────────────────
        // 7. Monthly seasonality
        // ─────────────────────────────────────────────
        val monthlyPattern = parsedTx
            .groupBy { it.date.dayOfMonth }
            .mapValues { (_, list) -> list.map { it.net }.average() }

        // ─────────────────────────────────────────────
        // 8. Volatility (for confidence + bands)
        // ─────────────────────────────────────────────
        val volatility = if (regularValues.isNotEmpty()) {
            val avg = regularValues.average()
            regularValues.map { abs(it - avg) }.average()
        } else 0.0

        // ─────────────────────────────────────────────
        // 9. Gap bridging (safe)
        // ─────────────────────────────────────────────
        val gapDays = ChronoUnit.DAYS.between(balanceAsOfDate, today).toInt().coerceAtLeast(0)

        val cappedDrift = baseDailyNet.coerceIn(
            -abs(currentBalance * 0.1),
            abs(currentBalance * 0.1)
        )

        val estimatedTodayBalance = currentBalance + (cappedDrift * gapDays)

        // ─────────────────────────────────────────────
        // 10. Upcoming transactions
        // ─────────────────────────────────────────────
        val upcomingByDate = mutableMapOf<LocalDate, Double>()

        upcomingTransactions.forEach { u ->
            val start = runCatching { LocalDate.parse(u.dueDate) }.getOrNull() ?: return@forEach

            val impact = when (u.type) {
                UpcomingType.INCOME, UpcomingType.SAVING -> u.amount
                UpcomingType.EXPENSE -> -u.amount
            }

            var date = fastForward(start, u.recurrence, today)
            val end = today.plusDays(daysForward.toLong())

            while (!date.isAfter(end)) {
                if (!date.isBefore(today)) {
                    upcomingByDate[date] = (upcomingByDate[date] ?: 0.0) + impact
                }
                if (u.recurrence == RecurrenceType.NONE) break
                date = nextDueDate(date, u.recurrence)
            }
        }

        // ─────────────────────────────────────────────
        // 11. Projection loop (FINAL UPGRADE)
        // ─────────────────────────────────────────────
        var runningBalance = estimatedTodayBalance
        val trend = mutableListOf<Pair<LocalDate, Double>>()
        val markers = mutableListOf<Triple<LocalDate, Double, String>>()

        repeat(daysForward) { i ->
            val date = today.plusDays((i + 1).toLong())

            val weekday = weekdayAverages[date.dayOfWeek] ?: baseDailyNet
            val seasonal = monthlyPattern[date.dayOfMonth] ?: 0.0

            val decay = 1 - (i.toDouble() / (daysForward * 1.5))
            val drift = (weekday + seasonal) * decay

            runningBalance += drift

            val upcomingImpact = upcomingByDate[date] ?: 0.0
            if (upcomingImpact != 0.0) {
                runningBalance += upcomingImpact
                markers.add(Triple(date, runningBalance, "Scheduled"))
            }

            trend.add(date to runningBalance)
        }

        // ─────────────────────────────────────────────
        // 12. Totals
        // ─────────────────────────────────────────────
        val predictedNet =
            trend.lastOrNull()?.second?.minus(estimatedTodayBalance) ?: 0.0

        val predictedIncome =
            incomeTrend * daysForward +
                    upcomingTransactions.filter { it.type != UpcomingType.EXPENSE }
                        .sumOf { it.amount }

        val predictedExpenses =
            expenseTrend * daysForward +
                    upcomingTransactions.filter { it.type == UpcomingType.EXPENSE }
                        .sumOf { it.amount }

        // ─────────────────────────────────────────────
        // 13. Confidence scoring
        // ─────────────────────────────────────────────
        val confidence = when {
            observedDays > 90 && volatility < 500 -> "High"
            observedDays > 45 -> "Medium"
            else -> "Low"
        }

        val note = buildString {
            append("Confidence: $confidence. ")
            if (gapDays > 3) append("Balance is ${gapDays}d old. ")
            if (outliers.isNotEmpty()) append("Outliers adjusted. ")
            if (upcomingTransactions.isNotEmpty()) append("Upcoming included. ")
        }

        return FinanceProjection(
            predictedIncome = predictedIncome.coerceAtLeast(0.0),
            predictedExpenses = predictedExpenses.coerceAtLeast(0.0),
            predictedNet = predictedNet,
            predictedBalance = trend.lastOrNull()?.second ?: estimatedTodayBalance,
            dailyTrend = trend,
            projectedDailyNet = baseDailyNet,
            estimatedTodayBalance = estimatedTodayBalance,
            dataQualityNote = note,
            upcomingMarkers = markers
        )
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private fun filterOutliersIQR(values: List<Double>): Pair<List<Double>, List<Double>> {
        if (values.size < 5) return values to emptyList()

        val sorted = values.sorted()
        val q1 = sorted[(sorted.size * 0.25).toInt()]
        val q3 = sorted[(sorted.size * 0.75).toInt()]
        val iqr = q3 - q1

        val lower = q1 - 1.5 * iqr
        val upper = q3 + 1.5 * iqr

        val regular = values.filter { it in lower..upper }
        val outliers = values.filter { it !in lower..upper }

        return regular to outliers
    }

    private fun exponentialSmoothing(values: List<Double>, alpha: Double = 0.3): Double {
        if (values.isEmpty()) return 0.0
        var result = values.first()
        for (i in 1 until values.size) {
            result = alpha * values[i] + (1 - alpha) * result
        }
        return result
    }

    private fun fastForward(
        date: LocalDate,
        recurrence: RecurrenceType,
        today: LocalDate
    ): LocalDate {
        return when (recurrence) {
            RecurrenceType.WEEKLY -> date.plusWeeks(
                ChronoUnit.WEEKS.between(date, today).coerceAtLeast(0)
            )

            RecurrenceType.MONTHLY -> date.plusMonths(
                ChronoUnit.MONTHS.between(date, today).coerceAtLeast(0)
            )

            RecurrenceType.YEARLY -> date.plusYears(
                ChronoUnit.YEARS.between(date, today).coerceAtLeast(0)
            )

            RecurrenceType.NONE -> date
        }
    }

    private fun nextDueDate(from: LocalDate, recurrence: RecurrenceType) = when (recurrence) {
        RecurrenceType.WEEKLY -> from.plusWeeks(1)
        RecurrenceType.MONTHLY -> from.plusMonths(1)
        RecurrenceType.YEARLY -> from.plusYears(1)
        RecurrenceType.NONE -> from
    }

    private fun emptyProjection(balance: Double, note: String) = FinanceProjection(
        predictedIncome = 0.0,
        predictedExpenses = 0.0,
        predictedNet = 0.0,
        predictedBalance = balance,
        dailyTrend = emptyList(),
        projectedDailyNet = 0.0,
        dataQualityNote = note
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
