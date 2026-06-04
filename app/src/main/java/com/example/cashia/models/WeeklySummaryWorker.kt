package com.example.cashia.models

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cashia.AppDatabase
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class WeeklySummaryWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        if (!PesaTimeConfiguration.isWeeklySummaryEnabled) return Result.success()

        val today = LocalDate.now()
        val firstDay = PesaTimeConfiguration.firstDayOfWeek
        // Summarise the week that just ended
        val weekEnd = today.with(TemporalAdjusters.previousOrSame(firstDay)).minusDays(1)
        val weekStart = weekEnd.minusDays(6)

        val db = AppDatabase.getDatabase(context)
        val txs = withContext(Dispatchers.IO) {
            db.transactionDao()
                .getTransactionsForDateRange(weekStart.toString(), weekEnd.toString())
        }

        var income = 0.0
        var expense = 0.0
        var topExpense = 0.0
        var topRecipient = ""

        txs.forEach {
            when (it.type) {
                "income" -> income += it.amount
                "expense" -> {
                    val total = it.amount + it.transactionCost
                    expense += total
                    if (it.amount > topExpense) {
                        topExpense = it.amount; topRecipient = it.recipient
                    }
                }
            }
        }

        val fmt = DateTimeFormatter.ofPattern("dd MMM")
        val body = buildString {
            append("${weekStart.format(fmt)} – ${weekEnd.format(fmt)}\n")
            append("💰 Income:   ${NotificationHelper.ksh(income)}\n")
            append("💸 Expenses: ${NotificationHelper.ksh(expense)}\n")
            append("📊 Net:      ${NotificationHelper.ksh(income - expense)}")
            if (txs.isNotEmpty()) append("\n📋 ${txs.size} transactions")
            if (topRecipient.isNotBlank()) append(
                "\n🔝 Biggest: $topRecipient (${
                    NotificationHelper.ksh(
                        topExpense
                    )
                })"
            )
        }

        NotificationHelper.post(context, ID, NotificationHelper.CH_WEEKLY, "📆 Weekly Summary", body)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "weekly_summary"
        private const val ID = 1002
    }
}