package com.example.cashia.models

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class PeriodSummaryWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val period = inputData.getString(KEY_PERIOD) ?: return Result.failure()
        val today = LocalDate.now()

        data class Cfg(
            val start: LocalDate, val end: LocalDate,
            val label: String, val id: Int,
            val enabled: Boolean
        )

        val cfg = when (period) {
            "monthly" -> {
                val s = today.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth())
                val e = s.with(TemporalAdjusters.lastDayOfMonth())
                Cfg(
                    s, e, s.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    1003, PesaTimeConfiguration.isMonthlySummaryEnabled
                )
            }

            "quarterly" -> {
                val prevQStart = today.minusMonths(3).let {
                    LocalDate.of(it.year, ((it.monthValue - 1) / 3) * 3 + 1, 1)
                }
                val q = ((prevQStart.monthValue - 1) / 3) + 1
                Cfg(
                    prevQStart, prevQStart.plusMonths(3).minusDays(1),
                    "Q$q ${prevQStart.year}", 1004,
                    PesaTimeConfiguration.isQuarterlySummaryEnabled
                )
            }

            "yearly" -> {
                val s = LocalDate.of(today.year - 1, 1, 1)
                Cfg(
                    s, LocalDate.of(today.year - 1, 12, 31),
                    "${today.year - 1} Year in Review", 1005,
                    PesaTimeConfiguration.isYearlySummaryEnabled
                )
            }

            else -> return Result.failure()
        }

        // Always reschedule (so next occurrence is queued) then bail if toggled off
        reschedule(period)
        if (!cfg.enabled) return Result.success()

        val db = AppDatabase.getDatabase(context)
        val txs = withContext(Dispatchers.IO) {
            db.transactionDao()
                .getTransactionsForDateRange(cfg.start.toString(), cfg.end.toString())
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
                    val cat = it.category ?: "other"
                    categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + total
                }
            }
        }

        val topCategory = categoryTotals.maxByOrNull { it.value }
        val net = income - expense

        val body = buildString {
            append("${cfg.label}\n")
            append("💰 Income:   ${NotificationHelper.ksh(income)}\n")
            append("💸 Expenses: ${NotificationHelper.ksh(expense)}\n")
            append("📊 Net:      ${if (net >= 0) "+" else ""}${NotificationHelper.ksh(net)}")
            if (txs.isNotEmpty()) append("\n📋 ${txs.size} transactions")
            topCategory?.let {
                append(
                    "\n🏷 Top spend: ${it.key.replaceFirstChar { c -> c.uppercase() }} (${
                        NotificationHelper.ksh(
                            it.value
                        )
                    })"
                )
            }
        }

        NotificationHelper.post(
            context,
            cfg.id,
            NotificationHelper.CH_PERIODIC,
            "📊 ${cfg.label}",
            body
        )
        return Result.success()
    }

    private fun reschedule(period: String) = when (period) {
        "monthly" -> NotificationScheduler.scheduleMonthly(context)
        "quarterly" -> NotificationScheduler.scheduleQuarterly(context)
        "yearly" -> NotificationScheduler.scheduleYearly(context)
        else -> Unit
    }

    companion object {
        const val KEY_PERIOD = "period"
        const val WORK_MONTHLY = "monthly_summary"
        const val WORK_QUARTERLY = "quarterly_summary"
        const val WORK_YEARLY = "yearly_summary"
    }
}