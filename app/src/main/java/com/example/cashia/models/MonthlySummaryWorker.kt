package com.example.cashia.models

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationRuntimeStore
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.NotificationStream
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Fires on the 1st of each month and summarizes the month that just ended.
 * Tapping the notification deep-links to the in-app report for that month.
 */
class MonthlySummaryWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationRuntimeStore.recordAttempt(context, NotificationStream.MONTHLY)
        try {
            if (!PesaTimeConfiguration.isMonthlySummaryEnabled) return Result.success()

            val monthStart = LocalDate.now()
                .minusMonths(1)
                .with(TemporalAdjusters.firstDayOfMonth())
            val monthEnd = monthStart.with(TemporalAdjusters.lastDayOfMonth())
            val label = monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

            val db = AppDatabase.getDatabase(context)
            val txs = withContext(Dispatchers.IO) {
                db.transactionDao()
                    .getTransactionsForDateRange(monthStart.toString(), monthEnd.toString())
            }

            var income = 0.0
            var expense = 0.0
            val categoryTotals = mutableMapOf<String, Double>()
            txs.forEach {
                when (it.type) {
                    "income" -> income += it.amount
                    "expense" -> {
                        val total = it.amount + it.transactionCost
                        expense += total
                        categoryTotals[it.category] =
                            (categoryTotals[it.category] ?: 0.0) + total
                    }
                }
            }
            val topCategory = categoryTotals.maxByOrNull { it.value }
            val net = income - expense

            val body = buildString {
                append("💰 Income:   ${NotificationHelper.ksh(income)}\n")
                append("💸 Expenses: ${NotificationHelper.ksh(expense)}\n")
                append("📊 Net:      ${if (net >= 0) "+" else ""}${NotificationHelper.ksh(net)}")
                if (txs.isNotEmpty()) append("\n📋 ${txs.size} transactions")
                topCategory?.let {
                    append("\n🏷 Top spend: ${prettyCategory(it.key)} (${NotificationHelper.ksh(it.value)})")
                }
                append("\nTap for the full report")
            }

            NotificationHelper.post(
                context,
                id = ID,
                channelId = NotificationHelper.CH_PERIODIC,
                title = "📊 $label Summary",
                body = body,
                deepLinkRoute = AppDestinations.SummaryReport.buildRoute(
                    "month", monthStart.toString()
                )
            )
            NotificationRuntimeStore.recordSuccess(context, NotificationStream.MONTHLY)
            return Result.success()
        } catch (e: Exception) {
            NotificationRuntimeStore.recordError(context, NotificationStream.MONTHLY, e.localizedMessage)
            return Result.failure()
        } finally {
            // Queue next month's run even when disabled or on failure, so the chain
            // never dies. APPEND keeps this running instance from cancelling itself.
            NotificationScheduler.scheduleMonthly(context, fromWorker = true)
        }
    }

    private fun prettyCategory(raw: String) =
        raw.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

    companion object {
        const val WORK_NAME = "monthly_summary"
        private const val ID = 1003
    }
}
