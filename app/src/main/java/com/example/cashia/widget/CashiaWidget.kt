package com.example.cashia.widget

import android.app.KeyguardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.cashia.AppDatabase
import com.example.cashia.MainActivity
import com.example.cashia.ui.theme.WidgetBackground
import com.example.cashia.ui.theme.WidgetCard
import com.example.cashia.ui.theme.WidgetExpense
import com.example.cashia.ui.theme.WidgetIncome
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CashiaWidget : GlanceAppWidget() {

    companion object {
        private val COMPACT = DpSize(140.dp, 110.dp)
        private val WIDE = DpSize(260.dp, 110.dp)
        private val TALL = DpSize(140.dp, 220.dp)
        private val LARGE = DpSize(260.dp, 220.dp)

        private val uiDateFmt = DateTimeFormatter.ofPattern("d MMM")
        private const val WIDGET_PREFS = "cashia_widget_prefs"
        private const val SHOW_BALANCE_KEY = "widget_show_balance"

        fun ksh(amount: Double): String =
            "${PesaTimeConfiguration.CURRENCY} ${compactAmount(amount)}"

        /** Bare number (no currency prefix) for compact rows that supply their own sign/prefix. */
        fun compactAmount(amount: Double): String = "%,.0f".format(amount)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT, WIDE, TALL, LARGE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = fetchWidgetData(context)
        provideContent {
            WidgetRoot(data)
        }
    }

    private suspend fun fetchWidgetData(context: Context): WidgetData =
        withContext(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).transactionDao()
                val today = LocalDate.now()
                val todayStr = today.toString()
                val dayTxs = dao.getTransactionsForDateRange(todayStr, todayStr)
                val weekStart = today.minusDays(today.dayOfWeek.value.toLong() - 1L).toString()
                val weekTxs = dao.getTransactionsForDateRange(weekStart, todayStr)
                val latestTx = dao.getLatestTransactionOnce()
                val isLocked =
                    (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
                        ?.isKeyguardLocked == true
                val revealRequested = context
                    .getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                    .getBoolean(SHOW_BALANCE_KEY, false)

                WidgetData(
                    balance = latestTx?.balance ?: 0.0,
                    today = metrics(dayTxs),
                    week = metrics(weekTxs),
                    updatedAt = buildUpdatedLabel(latestTx?.date, latestTx?.time),
                    isLocked = isLocked,
                    revealRequested = revealRequested
                )
            } catch (_: Exception) {
                WidgetData()
            }
        }

    private fun buildUpdatedLabel(date: String?, time: String?): String {
        if (date.isNullOrBlank()) return "No synced transactions"
        val dateLabel = runCatching { LocalDate.parse(date).format(uiDateFmt) }.getOrElse { date }
        return if (time.isNullOrBlank()) "Updated $dateLabel" else "Updated $dateLabel • $time"
    }

    private fun metrics(txs: List<com.example.cashia.models.MpesaTransactions>): Metrics {
        var income = 0.0
        var expenses = 0.0
        for (tx in txs) {
            when (tx.type) {
                "income" -> income += tx.amount
                "expense" -> expenses += tx.amount + tx.transactionCost
            }
        }
        return Metrics(income, expenses)
    }

    @Composable
    private fun WidgetRoot(data: WidgetData) {
        val size = LocalSize.current
        val showBalance = data.revealRequested && !data.isLocked
        val isWide = size.width >= WIDE.width
        val isTall = size.height >= TALL.height
        val isLarge = isWide && isTall

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBackground)
                .cornerRadius(20.dp)
                .padding(if (isLarge) 14.dp else 12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Header(data.isLocked, showBalance)
            Spacer(GlanceModifier.height(if (isTall) 8.dp else 6.dp))

            BalanceCard(
                balance = data.balance,
                showBalance = showBalance,
                isLocked = data.isLocked
            )
            Spacer(GlanceModifier.height(if (isTall) 8.dp else 6.dp))

            when {
                isLarge -> {
                    MetricsRow(title = "Today", metrics = data.today)
                    Spacer(GlanceModifier.height(6.dp))
                    MetricsRow(title = "Week", metrics = data.week)
                }

                isWide -> {
                    MetricsInline(metrics = data.today)
                }

                isTall -> {
                    MetricsRow(title = "Today", metrics = data.today)
                }

                else -> {
                    TodayNetCard(data.today)
                }
            }

            Spacer(GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.height(6.dp))
            Footer(updatedAt = data.updatedAt)
            Spacer(GlanceModifier.height(6.dp))
            ActionRow(showBalance = showBalance, isLocked = data.isLocked)
        }
    }

    @Composable
    private fun Header(isLocked: Boolean, showBalance: Boolean) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "Balance Snapshot",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(12f, TextUnitType.Sp)
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            val badgeText = when {
                isLocked -> "Locked"
                showBalance -> "Visible"
                else -> "Hidden"
            }
            Box(
                modifier = GlanceModifier
                    .background(WidgetCard)
                    .cornerRadius(6.dp)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = TextStyle(
                        fontSize = TextUnit(8f, TextUnitType.Sp),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    @Composable
    private fun BalanceCard(
        balance: Double,
        showBalance: Boolean,
        isLocked: Boolean
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Text(
                text = "CURRENT BALANCE",
                style = TextStyle(
                    fontSize = TextUnit(9f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = if (showBalance) ksh(balance) else "••••••",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            )
            Text(
                text = when {
                    isLocked -> "Unlock device to reveal"
                    showBalance -> "Tap Hide to mask balance"
                    else -> "Tap Reveal to view balance"
                },
                style = TextStyle(
                    fontSize = TextUnit(9f, TextUnitType.Sp)
                )
            )
        }
    }

    @Composable
    private fun MetricsInline(metrics: Metrics) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            MetricPill("Income", metrics.income, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            MetricPill("Expense", metrics.expenses, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            val netColor = if (metrics.net >= 0) WidgetIncome else WidgetExpense
            MetricPill(
                label = "Net",
                amount = kotlin.math.abs(metrics.net),
                modifier = GlanceModifier.defaultWeight(),
                sign = if (metrics.net >= 0) "+" else "−"
            )
        }
    }

    @Composable
    private fun MetricsRow(title: String, metrics: Metrics) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetCard)
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = TextStyle(
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(4.dp))
            MetricsInline(metrics = metrics)
        }
    }

    @Composable
    private fun MetricPill(
        label: String,
        amount: Double,
        modifier: GlanceModifier,
        sign: String = ""
    ) {
        Column(
            modifier = modifier.background(WidgetCard).cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = TextStyle(
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = "$sign${compactAmount(amount)}",
                style = TextStyle(
                    fontSize = TextUnit(10f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun TodayNetCard(today: Metrics) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(34.dp)
                .background(WidgetCard)
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Today net: ${if (today.net >= 0) "+" else "−"}${
                    compactAmount(kotlin.math.abs(today.net))
                }",
                style = TextStyle(
                    fontSize = TextUnit(10f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun Footer(updatedAt: String) {
        Text(
            text = updatedAt,
            style = TextStyle(
                fontSize = TextUnit(9f, TextUnitType.Sp)
            )
        )
    }

    @Composable
    private fun ActionRow(showBalance: Boolean, isLocked: Boolean) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(48.dp)
                    .background(WidgetCard)
                    .cornerRadius(8.dp)
                    .clickable(actionRunCallback<ToggleBalanceVisibilityAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        isLocked -> "Locked"
                        showBalance -> "Hide"
                        else -> "Reveal"
                    },
                    style = TextStyle(
                        fontSize = TextUnit(10f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(GlanceModifier.width(6.dp))
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(48.dp)
                    .background(WidgetCard)
                    .cornerRadius(8.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open",
                    style = TextStyle(
                        fontSize = TextUnit(10f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }

    data class WidgetData(
        val balance: Double = 0.0,
        val today: Metrics = Metrics(),
        val week: Metrics = Metrics(),
        val updatedAt: String = "No synced transactions",
        val isLocked: Boolean = false,
        val revealRequested: Boolean = false
    )

    data class Metrics(
        val income: Double = 0.0,
        val expenses: Double = 0.0
    ) {
        val net: Double get() = income - expenses
    }

}

class ToggleBalanceVisibilityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences("cashia_widget_prefs", Context.MODE_PRIVATE)
        val current = prefs.getBoolean("widget_show_balance", false)
        prefs.edit().putBoolean("widget_show_balance", !current).apply()
        CashiaWidget().update(context, glanceId)
    }
}