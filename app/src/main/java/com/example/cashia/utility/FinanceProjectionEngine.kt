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
    private data class ParsedTx(val date: LocalDate, val net: Double)

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

        // ─────────────────────────────────────────────
        // 1. Parse once — skip anything beyond balance date
        // ─────────────────────────────────────────────
        val parsedTx = transactions.mapNotNull { tx ->
            val date = runCatching { LocalDate.parse(tx.date) }.getOrNull()
                ?: return@mapNotNull null
            if (date > balanceAsOfDate) return@mapNotNull null
            val net = if (tx.type == "income") tx.amount
            else -(tx.amount + tx.transactionCost)
            ParsedTx(date, net)
        }

        if (parsedTx.isEmpty()) {
            return emptyProjection(currentBalance, "No valid transaction data.")
        }

        // ─────────────────────────────────────────────
        // 2. Daily aggregation
        // ─────────────────────────────────────────────
        val dailyNet = mutableMapOf<LocalDate, Double>()
        parsedTx.forEach { dailyNet[it.date] = (dailyNet[it.date] ?: 0.0) + it.net }

        val firstDay = dailyNet.keys.min()                // safe: parsedTx non-empty
        val allDates = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(balanceAsOfDate) }
            .toList()

        val filledValues = allDates.map { dailyNet[it] ?: 0.0 }
        val observedDays = filledValues.size.coerceAtLeast(1)

        // ─────────────────────────────────────────────
        // 3. IQR Outlier filter — interpolated quartiles
        // ─────────────────────────────────────────────
        val (regularValues, outliers) = filterOutliersIQR(filledValues)

        // ─────────────────────────────────────────────
        // 4. Split income / expense — single partition pass
        // [PERF] Replaced two separate filter() calls with one partition().
        // ─────────────────────────────────────────────
        val (incomeList, expenseList) = parsedTx.partition { it.net > 0 }

        val dailyIncomeMap =
            incomeList.groupBy { it.date }.mapValues { (_, l) -> l.sumOf { it.net } }
        val dailyExpenseMap =
            expenseList.groupBy { it.date }.mapValues { (_, l) -> l.sumOf { abs(it.net) } }

        val incomeDailySeries = allDates.map { dailyIncomeMap[it] ?: 0.0 }
        val expenseDailySeries = allDates.map { dailyExpenseMap[it] ?: 0.0 }

        val smoothedIncome = exponentialSmoothing(incomeDailySeries)
        val smoothedExpense = exponentialSmoothing(expenseDailySeries)

        val incomeSlope = calculateSlope(incomeDailySeries)
        val expenseSlope = calculateSlope(expenseDailySeries)

        val incomeTrend = smoothedIncome * 0.7 + incomeSlope * 0.3
        val expenseTrend = expenseTrend(smoothedExpense, expenseSlope)  // see helper below

        var baseDailyNet = incomeTrend - expenseTrend

        // ─────────────────────────────────────────────
        // 5. Outlier reintegration (dampened)
        // ─────────────────────────────────────────────
        val outlierImpact = outliers.sum() / observedDays
        baseDailyNet += outlierImpact * 0.3

        // ─────────────────────────────────────────────
        // 6. Weekday & monthly DEVIATION modeling
        // [FIX] Original code used raw weekday/seasonal averages in the
        // projection loop and added them together, double-counting the mean.
        // E.g. if the overall mean is +300 and Monday avg is +500, adding a
        // seasonal average of +400 would produce +900 drift — wildly off.
        //
        // Correct approach: compute how much each weekday / day-of-month
        // DEVIATES from the overall mean, then layer only the deviation on
        // top of baseDailyNet.
        // ─────────────────────────────────────────────
        val overallMean = if (filledValues.isNotEmpty()) filledValues.average() else 0.0

        val weekdayDeviations = parsedTx
            .groupBy { it.date.dayOfWeek }
            .mapValues { (_, list) -> list.map { it.net }.average() - overallMean }

        // Monthly deviation is dampened more (noisier signal, fewer samples
        // per bucket — each day-of-month appears ~once per month).
        val seasonalDeviations = parsedTx
            .groupBy { it.date.dayOfMonth }
            .mapValues { (_, list) -> list.map { it.net }.average() - overallMean }

        // ─────────────────────────────────────────────
        // 7. Volatility (for confidence bands)
        // ─────────────────────────────────────────────
        val volatility = if (regularValues.isNotEmpty()) {
            val avg = regularValues.average()
            regularValues.map { abs(it - avg) }.average()
        } else 0.0

        // Coefficient of variation — volatility relative to income scale.
        // Raw volatility alone is misleading: Ksh 500 noise means something
        // very different if average daily income is Ksh 200 vs Ksh 50,000.
        val avgDailyIncome = incomeDailySeries.average().coerceAtLeast(1.0)
        val volatilityRatio = volatility / avgDailyIncome

        // ─────────────────────────────────────────────
        // 8. Gap bridging — estimate today's balance
        // ─────────────────────────────────────────────
        val gapDays = ChronoUnit.DAYS.between(balanceAsOfDate, today).toInt().coerceAtLeast(0)

        val cappedDrift = baseDailyNet.coerceIn(
            -abs(currentBalance * 0.1),
            abs(currentBalance * 0.1)
        )
        val estimatedTodayBalance = currentBalance + (cappedDrift * gapDays)

        // ─────────────────────────────────────────────
        // 9. Upcoming transaction scheduling
        // ─────────────────────────────────────────────
        val upcomingByDate = buildMap<LocalDate, Double> {
            val windowEnd = today.plusDays(daysForward.toLong())
            upcomingTransactions.forEach { u ->
                val start = runCatching { LocalDate.parse(u.dueDate) }.getOrNull()
                    ?: return@forEach
                val impact = when (u.type) {
                    UpcomingType.INCOME, UpcomingType.SAVING -> u.amount
                    UpcomingType.EXPENSE -> -u.amount
                }
                var date = fastForward(start, u.recurrence, today)
                while (!date.isAfter(windowEnd)) {
                    if (!date.isBefore(today)) {
                        merge(date, impact, Double::plus)
                    }
                    if (u.recurrence == RecurrenceType.NONE) break
                    date = nextDueDate(date, u.recurrence)
                }
            }
        }

        // ─────────────────────────────────────────────
        // 10. Projection loop
        // [FIX] Drift is now: baseDailyNet + weekdayDeviation + seasonalDeviation
        // Each deviation term decays over time (we're less certain about
        // patterns further out). Seasonal deviation is half-weighted because
        // day-of-month data is sparser (fewer samples per bucket).
        // ─────────────────────────────────────────────
        var runningBalance = estimatedTodayBalance
        val trend = ArrayList<Pair<LocalDate, Double>>(daysForward)
        val markers = ArrayList<Triple<LocalDate, Double, String>>(upcomingByDate.size)

        repeat(daysForward) { i ->
            val date = today.plusDays((i + 1).toLong())
            val decay = 1.0 - (i.toDouble() / (daysForward * 1.5))  // 1.0 → ~0.33

            val weekdayDeviation = (weekdayDeviations[date.dayOfWeek] ?: 0.0) * decay
            val seasonalDeviation = (seasonalDeviations[date.dayOfMonth] ?: 0.0) * decay * 0.5

            val drift = baseDailyNet + weekdayDeviation + seasonalDeviation
            runningBalance += drift

            val upcomingImpact = upcomingByDate[date] ?: 0.0
            if (upcomingImpact != 0.0) {
                runningBalance += upcomingImpact
                markers.add(Triple(date, runningBalance, "Scheduled"))
            }

            trend.add(date to runningBalance)
        }

        // ─────────────────────────────────────────────
        // 11. Totals
        // [FIX] predictedIncome/Expenses now uses avg daily flow * window,
        // plus only the scheduled amounts that actually fall in the window.
        // Previous version multiplied a per-transaction-smoothed value by
        // daysForward — wrong units — and counted all upcoming transactions
        // regardless of whether they fall in the projection window.
        // ─────────────────────────────────────────────
        val predictedNet =
            (trend.lastOrNull()?.second ?: estimatedTodayBalance) - estimatedTodayBalance

        val scheduledIncome = upcomingByDate.values.filter { it > 0 }.sum()
        val scheduledExpense = upcomingByDate.values.filter { it < 0 }.sumOf { abs(it) }

        val predictedIncome = (incomeTrend * daysForward + scheduledIncome).coerceAtLeast(0.0)
        val predictedExpenses = (expenseTrend * daysForward + scheduledExpense).coerceAtLeast(0.0)

        // ─────────────────────────────────────────────
        // 12. Confidence scoring
        // [FIX] Original: ignored data recency and relative volatility.
        //   91 days of data from 2 years ago would score "High".
        // New: factors in recency (days since last transaction),
        //   data density (observed days vs calendar span), and
        //   volatility ratio (noise relative to income scale).
        // ─────────────────────────────────────────────
        val daysSinceLastTx = ChronoUnit.DAYS.between(
            parsedTx.maxOf { it.date }, today
        ).toInt()
        val calendarSpan =
            ChronoUnit.DAYS.between(firstDay, balanceAsOfDate).toInt().coerceAtLeast(1)
        val dataDensity = observedDays.toDouble() / calendarSpan  // 1.0 = no gaps

        val confidence = when {
            observedDays >= 90 && daysSinceLastTx <= 14 && volatilityRatio < 0.5 && dataDensity > 0.6 -> "High"
            observedDays >= 45 && daysSinceLastTx <= 30 && volatilityRatio < 1.0 -> "Medium"
            else -> "Low"
        }

        val note = buildString {
            append("Confidence: $confidence. ")
            if (gapDays > 3) append("Balance is ${gapDays}d old. ")
            if (daysSinceLastTx > 14) append("No transactions in ${daysSinceLastTx}d. ")
            if (outliers.isNotEmpty()) append("${outliers.size} outlier(s) adjusted. ")
            if (dataDensity < 0.5) append("Sparse data (${(dataDensity * 100).toInt()}% density). ")
            if (upcomingTransactions.isNotEmpty()) append("${upcomingTransactions.size} upcoming event(s) included. ")
        }

        return FinanceProjection(
            predictedIncome = predictedIncome,
            predictedExpenses = predictedExpenses,
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

    // [FIX] Proper linear interpolation for quartiles instead of integer
    // index truncation. Truncation underestimates Q1 and overestimates Q3,
    // which narrows the IQR and over-flags values as outliers.
    private fun filterOutliersIQR(values: List<Double>): Pair<List<Double>, List<Double>> {
        if (values.size < 5) return values to emptyList()
        val sorted = values.sorted()
        val q1 = interpolatedQuartile(sorted, 0.25)
        val q3 = interpolatedQuartile(sorted, 0.75)
        val iqr = q3 - q1
        val lower = q1 - 1.5 * iqr
        val upper = q3 + 1.5 * iqr
        val regular = values.filter { it in lower..upper }
        val outliers = values.filter { it !in lower..upper }
        return regular to outliers
    }

    private fun interpolatedQuartile(sorted: List<Double>, q: Double): Double {
        val pos = q * (sorted.size - 1)
        val floor = pos.toInt()
        val ceil = (floor + 1).coerceAtMost(sorted.size - 1)
        return sorted[floor] + (pos - floor) * (sorted[ceil] - sorted[floor])
    }

    private fun exponentialSmoothing(values: List<Double>, alpha: Double = 0.3): Double {
        if (values.isEmpty()) return 0.0
        var result = values.first()
        for (i in 1 until values.size) {
            result = alpha * values[i] + (1 - alpha) * result
        }
        return result
    }

    // Tiny helper so the main function reads cleanly.
    private fun expenseTrend(smoothedExpense: Double, expenseSlope: Double) =
        smoothedExpense * 0.7 + expenseSlope * 0.3

    // [FIX] fastForward could land before today.
    // Example: due date is Thursday, today is Tuesday, weeksApart = 0,
    // so result = Thursday of a past week → still before today.
    // Fix: advance one more period if we haven't cleared today yet.
    private fun fastForward(
        date: LocalDate,
        recurrence: RecurrenceType,
        today: LocalDate
    ): LocalDate {
        return when (recurrence) {
            RecurrenceType.WEEKLY -> {
                val weeks = ChronoUnit.WEEKS.between(date, today).coerceAtLeast(0)
                var result = date.plusWeeks(weeks)
                if (result.isBefore(today)) result = result.plusWeeks(1)
                result
            }

            RecurrenceType.MONTHLY -> {
                val months = ChronoUnit.MONTHS.between(date, today).coerceAtLeast(0)
                var result = date.plusMonths(months)
                if (result.isBefore(today)) result = result.plusMonths(1)
                result
            }

            RecurrenceType.YEARLY -> {
                val years = ChronoUnit.YEARS.between(date, today).coerceAtLeast(0)
                var result = date.plusYears(years)
                if (result.isBefore(today)) result = result.plusYears(1)
                result
            }

            RecurrenceType.NONE -> date
        }
    }

    private fun nextDueDate(from: LocalDate, recurrence: RecurrenceType) = when (recurrence) {
        RecurrenceType.WEEKLY -> from.plusWeeks(1)
        RecurrenceType.MONTHLY -> from.plusMonths(1)
        RecurrenceType.YEARLY -> from.plusYears(1)
        RecurrenceType.NONE -> from
    }

    private fun calculateSlope(values: List<Double>): Double {
        val n = values.size
        if (n < 2) return 0.0
        val xMean = (0 until n).average()
        val yMean = values.average()
        var num = 0.0
        var den = 0.0
        for (i in values.indices) {
            val dx = i - xMean
            num += dx * (values[i] - yMean)
            den += dx * dx
        }
        return if (den == 0.0) 0.0 else num / den
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
}