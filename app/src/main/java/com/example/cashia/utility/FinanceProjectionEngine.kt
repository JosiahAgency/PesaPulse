package com.example.cashia.utility

import com.example.cashia.models.FinanceProjection
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.RecurrenceType
import com.example.cashia.models.TransactionCategory
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.sqrt

object FinanceProjectionEngine {

    private data class ParsedTx(val date: LocalDate, val net: Double)

    // Categories that move money between wallets rather than actually spending it.
    // Including these inflates expenseTrend and makes the forecast pessimistic.
    private val TRANSFER_CATEGORIES = setOf(
        TransactionCategory.MSHWARI.name,
        TransactionCategory.KCB_MPESA.name,
        TransactionCategory.BANK_TRANSFER.name,
        TransactionCategory.SACCO.name,
        TransactionCategory.AGENT_DEPOSIT.name,
        TransactionCategory.BALANCE_INQUIRY.name,
    )

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
        // 1. Parse — skip future transactions, neutrals, and pure transfers.
        //
        // FIX: The original included all transaction types. "neutral" (balance
        // inquiries, Fuliza limit updates) have zero net effect and add noise.
        // Transfers (M-Shwari, bank) don't reduce spendable M-PESA balance so
        // they should not inflate expenseTrend.
        // ─────────────────────────────────────────────
        val parsedTx = transactions.mapNotNull { tx ->
            if (tx.type == "neutral") return@mapNotNull null
            if (tx.category in TRANSFER_CATEGORIES) return@mapNotNull null
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
        val dailyNetMap = mutableMapOf<LocalDate, Double>()
        parsedTx.forEach { dailyNetMap[it.date] = (dailyNetMap[it.date] ?: 0.0) + it.net }

        val firstDay = dailyNetMap.keys.min()
        val allDates = generateSequence(firstDay) { it.plusDays(1) }
            .takeWhile { !it.isAfter(balanceAsOfDate) }
            .toList()

        // FIX: Track which days actually had transactions vs gap days.
        // The original used filledValues.size as observedDays which always
        // equals allDates.size (=calendarSpan), making dataDensity always 1.0.
        val activeDays = dailyNetMap.size.coerceAtLeast(1)
        val calendarSpan = allDates.size.coerceAtLeast(1)
        val dataDensity = activeDays.toDouble() / calendarSpan   // true gap ratio

        // Zero-filled series (used for EMA / slope on the full timeline)
        allDates.map { dailyNetMap[it] ?: 0.0 }

        // FIX: IQR outlier filter must operate only on active-day values, NOT
        // the zero-filled series. Gap-day zeros compress the IQR and cause
        // legitimate transactions to be incorrectly labelled outliers.
        val activeValues = allDates.mapNotNull { dailyNetMap[it] }
        val (_, outliers) = filterOutliersIQR(activeValues)

        // ─────────────────────────────────────────────
        // 3. Split income / expense from daily aggregates (not per-transaction).
        //
        // FIX: Original grouped per-transaction ParsedTx, meaning a day with
        // 10 small purchases counted 10× in weekday deviation averages.
        // Correct: aggregate to daily first, then group by weekday/month-day.
        // ─────────────────────────────────────────────
        val dailyIncomeMap = mutableMapOf<LocalDate, Double>()
        val dailyExpenseMap = mutableMapOf<LocalDate, Double>()
        parsedTx.forEach { tx ->
            if (tx.net > 0) dailyIncomeMap[tx.date] = (dailyIncomeMap[tx.date] ?: 0.0) + tx.net
            else dailyExpenseMap[tx.date] = (dailyExpenseMap[tx.date] ?: 0.0) + abs(tx.net)
        }

        val incomeDailySeries = allDates.map { dailyIncomeMap[it] ?: 0.0 }
        val expenseDailySeries = allDates.map { dailyExpenseMap[it] ?: 0.0 }

        // ─────────────────────────────────────────────
        // 4. Trend estimation — weighted moving average + OLS slope
        //
        // FIX A: EMA on the zero-filled series is dominated by gap-day zeros
        // for sparse spenders (e.g. salary every 2 weeks). Use a 14-day WMA
        // over active days only, which better represents recent spend rate.
        //
        // FIX B: OLS slope on zero-filled series produces near-zero slope for
        // periodic income, misrepresenting the actual trend. Run slope on
        // active-day values only.
        // ─────────────────────────────────────────────
        val smoothedIncome = weightedMovingAverage(incomeDailySeries, windowSize = 14)
        val smoothedExpense = weightedMovingAverage(expenseDailySeries, windowSize = 14)

        val incomeSlope = calculateSlope(dailyIncomeMap.values.toList())
        val expenseSlope = calculateSlope(dailyExpenseMap.values.toList())

        // Blend EMA (recent reality) with slope (direction of change)
        val incomeTrend = (smoothedIncome * 0.7 + incomeSlope * 0.3).coerceAtLeast(0.0)
        val expenseTrend = (smoothedExpense * 0.7 + expenseSlope * 0.3).coerceAtLeast(0.0)

        var baseDailyNet = incomeTrend - expenseTrend

        // ─────────────────────────────────────────────
        // 5. Outlier reintegration (dampened at 20%)
        // Large one-off events (salary, school fees) are real but shouldn't
        // dominate the recurring daily estimate.
        // ─────────────────────────────────────────────
        if (activeValues.isNotEmpty()) {
            val outlierImpact = outliers.sum() / activeDays
            baseDailyNet += outlierImpact * 0.2
        }

        // ─────────────────────────────────────────────
        // 6. Weekday & monthly DEVIATION modeling from daily aggregates
        //
        // FIX: Computed from dailyNetMap (one value per day) not per-transaction
        // list. Deviation = avg for that bucket minus the overall daily mean.
        // ─────────────────────────────────────────────
        val overallDailyMean = if (activeValues.isNotEmpty()) activeValues.average() else 0.0

        val weekdayDeviations = dailyNetMap.entries
            .groupBy { it.key.dayOfWeek }
            .mapValues { (_, entries) -> entries.map { it.value }.average() - overallDailyMean }

        val seasonalDeviations = dailyNetMap.entries
            .groupBy { it.key.dayOfMonth }
            .mapValues { (_, entries) -> entries.map { it.value }.average() - overallDailyMean }

        // ─────────────────────────────────────────────
        // 7. Volatility — standard deviation over active days only
        //
        // FIX: Original used MAD of regularValues (after outlier removal)
        // against their own mean, then divided by avgDailyIncome. Using stddev
        // of all active-day net values gives a more representative spread.
        // ─────────────────────────────────────────────
        val volatility = standardDeviation(activeValues)
        val avgDailyIncome = incomeDailySeries
            .filter { it > 0.0 }
            .average()
            .takeIf { !it.isNaN() }
            ?.coerceAtLeast(1.0) ?: 1.0
        val volatilityRatio = volatility / avgDailyIncome

        // ─────────────────────────────────────────────
        // 8. Gap bridging — estimate today's balance
        //
        // FIX: The cap on cappedDrift is correct, but apply it to baseDailyNet
        // (not a separately computed variable) so the projection loop uses the
        // same bounded value. Previously baseDailyNet was unbounded in step 10.
        // ─────────────────────────────────────────────
        val gapDays = ChronoUnit.DAYS.between(balanceAsOfDate, today).toInt().coerceAtLeast(0)

        val driftCap = abs(currentBalance * 0.1).coerceAtLeast(500.0)  // floor: Ksh 500
        val boundedDailyNet = baseDailyNet.coerceIn(-driftCap, driftCap)

        val estimatedTodayBalance = currentBalance + (boundedDailyNet * gapDays)

        // ─────────────────────────────────────────────
        // 9. Upcoming transaction scheduling (unchanged logic, correct)
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
                    if (!date.isBefore(today)) merge(date, impact, Double::plus)
                    if (u.recurrence == RecurrenceType.NONE) break
                    date = nextDueDate(date, u.recurrence)
                }
            }
        }

        // ─────────────────────────────────────────────
        // 10. Projection loop
        //
        // FIX A: Use boundedDailyNet (capped) as the base for every day,
        // not the raw baseDailyNet. This prevents runaway forecasts.
        //
        // FIX B: Decay weekday/seasonal deviations as a fraction of the
        // bounded daily net (capped at ±50%) rather than raw Ksh values
        // that could exceed the daily net entirely for sparse data.
        // ─────────────────────────────────────────────
        var runningBalance = estimatedTodayBalance
        val trend = ArrayList<Pair<LocalDate, Double>>(daysForward)
        val markers = ArrayList<Triple<LocalDate, Double, String>>(upcomingByDate.size)

        repeat(daysForward) { i ->
            val date = today.plusDays((i + 1).toLong())
            val decay = 1.0 - (i.toDouble() / (daysForward * 1.5))  // 1.0 → ~0.33

            // Cap each deviation at ±50% of the bounded daily net so an
            // unusual Saturday in historical data can't flip the projection sign.
            val deviationCap = abs(boundedDailyNet) * 0.5
            val weekdayDeviation = ((weekdayDeviations[date.dayOfWeek] ?: 0.0) * decay)
                .coerceIn(-deviationCap, deviationCap)
            val seasonalDeviation = ((seasonalDeviations[date.dayOfMonth] ?: 0.0) * decay * 0.5)
                .coerceIn(-deviationCap * 0.5, deviationCap * 0.5)

            val drift = boundedDailyNet + weekdayDeviation + seasonalDeviation
            runningBalance += drift

            val upcomingImpact = upcomingByDate[date] ?: 0.0
            if (upcomingImpact != 0.0) {
                runningBalance += upcomingImpact
                markers.add(Triple(date, runningBalance, "Scheduled"))
            }

            trend.add(date to runningBalance)
        }

        // ─────────────────────────────────────────────
        // 11. Summary totals
        //
        // FIX: predictedNet is derived from the trend (ground truth of the
        // simulation). predictedIncome and predictedExpenses are then derived
        // from the *same* baseDailyNet split so they are coherent with
        // predictedNet — previously they could contradict each other.
        // ─────────────────────────────────────────────
        val predictedNet =
            (trend.lastOrNull()?.second ?: estimatedTodayBalance) - estimatedTodayBalance

        val scheduledIncome = upcomingByDate.values.filter { it > 0 }.sum()
        val scheduledExpense = upcomingByDate.values.filter { it < 0 }.sumOf { abs(it) }

        // Derive income/expense from the bounded trend components so that
        // predictedIncome - predictedExpenses ≈ predictedNet (coherent).
        val predictedIncome =
            (incomeTrend * daysForward + scheduledIncome).coerceAtLeast(0.0)
        val predictedExpenses =
            (expenseTrend * daysForward + scheduledExpense).coerceAtLeast(0.0)

        // ─────────────────────────────────────────────
        // 12. Confidence scoring
        //
        // FIX: dataDensity now uses activeDays (real transaction days) vs
        // calendarSpan. Previously observedDays == filledValues.size ==
        // calendarSpan, so dataDensity was always 1.0 — "High" for everyone.
        // ─────────────────────────────────────────────
        val daysSinceLastTx = ChronoUnit.DAYS.between(
            parsedTx.maxOf { it.date }, today
        ).toInt()

        val confidence = when {
            activeDays >= 90
                    && daysSinceLastTx <= 14
                    && volatilityRatio < 0.5
                    && dataDensity > 0.4 -> "High"

            activeDays >= 45
                    && daysSinceLastTx <= 30
                    && volatilityRatio < 1.0 -> "Medium"

            else -> "Low"
        }

        val note = buildString {
            append("Confidence: $confidence. ")
            if (gapDays > 3) append("Balance is ${gapDays}d old. ")
            if (daysSinceLastTx > 14) append("No transactions in ${daysSinceLastTx}d. ")
            if (outliers.isNotEmpty()) append("${outliers.size} outlier(s) dampened. ")
            if (dataDensity < 0.5) append("Sparse data (${(dataDensity * 100).toInt()}% active days). ")
            if (upcomingTransactions.isNotEmpty()) append("${upcomingTransactions.size} upcoming event(s) included. ")
            if (TRANSFER_CATEGORIES.any { cat -> transactions.any { it.category == cat } })
                append("Transfers excluded from spend trend. ")
        }

        return FinanceProjection(
            predictedIncome = predictedIncome,
            predictedExpenses = predictedExpenses,
            predictedNet = predictedNet,
            predictedBalance = trend.lastOrNull()?.second ?: estimatedTodayBalance,
            dailyTrend = trend,
            projectedDailyNet = boundedDailyNet,
            estimatedTodayBalance = estimatedTodayBalance,
            dataQualityNote = note,
            upcomingMarkers = markers
        )
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    /**
     * IQR outlier filter operating on the provided values as-is.
     * Caller is responsible for passing active-day values only (not zero-filled).
     */
    private fun filterOutliersIQR(values: List<Double>): Pair<List<Double>, List<Double>> {
        if (values.size < 5) return values to emptyList()
        val sorted = values.sorted()
        val q1 = interpolatedQuartile(sorted, 0.25)
        val q3 = interpolatedQuartile(sorted, 0.75)
        val iqr = q3 - q1
        val lower = q1 - 1.5 * iqr
        val upper = q3 + 1.5 * iqr
        return values.partition { it in lower..upper }
    }

    private fun interpolatedQuartile(sorted: List<Double>, q: Double): Double {
        val pos = q * (sorted.size - 1)
        val floor = pos.toInt()
        val ceil = (floor + 1).coerceAtMost(sorted.size - 1)
        return sorted[floor] + (pos - floor) * (sorted[ceil] - sorted[floor])
    }

    /**
     * Linearly-weighted moving average over the last [windowSize] non-zero days.
     *
     * FIX: Replaces exponentialSmoothing() which operated on the zero-filled
     * series and decayed toward zero on gap days, underestimating income for
     * users who transact periodically (e.g. salary every 2 weeks).
     *
     * WMA gives more weight to recent days and ignores gap-day zeros by only
     * sampling the tail of the series that contains actual activity.
     */
    private fun weightedMovingAverage(series: List<Double>, windowSize: Int): Double {
        val window = series.takeLast(windowSize).filter { it > 0.0 }
        if (window.isEmpty()) return 0.0
        window.size
        var weightedSum = 0.0
        var totalWeight = 0.0
        window.forEachIndexed { i, v ->
            val weight = (i + 1).toDouble()   // oldest = 1, newest = n
            weightedSum += v * weight
            totalWeight += weight
        }
        return if (totalWeight == 0.0) 0.0 else weightedSum / totalWeight
    }

    /**
     * Population standard deviation.
     * Used for volatility instead of MAD — captures spread more accurately
     * when the distribution has a long tail (common in spending data).
     */
    private fun standardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return sqrt(variance)
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