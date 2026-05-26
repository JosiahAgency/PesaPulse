package com.example.cashia.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.unit.ColorProvider
import com.example.cashia.AppDatabase
import com.example.cashia.MainActivity
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.ui.theme.WidgetBackground
import com.example.cashia.ui.theme.WidgetCard
import com.example.cashia.ui.theme.WidgetExpense
import com.example.cashia.ui.theme.WidgetIncome
import com.example.cashia.ui.theme.WidgetPrimary
import com.example.cashia.ui.theme.WidgetSubtle
import com.example.cashia.ui.theme.WidgetText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

class CashiaWidget : GlanceAppWidget() {

    // ── Size breakpoints ──────────────────────────────────────────────────────
    // Glance Responsive picks the largest DpSize where BOTH dimensions fit
    // within the actual widget allocation on-screen.
    //
    //   COMPACT  (140×110) → 3×2 cells:  balance + today card
    //   WIDE     (260×110) → 4×2 cells:  balance + today/week/month row
    //   TALL     (140×260) → 3×4 cells:  balance + stacked period cards + recent txs
    //   FULL     (260×260) → 4×4 cells:  balance + period row + recent txs

    companion object {
        private val COMPACT = DpSize(140.dp, 110.dp)
        private val WIDE = DpSize(260.dp, 110.dp)
        private val TALL = DpSize(140.dp, 260.dp)
        private val FULL = DpSize(260.dp, 260.dp)

        private val shortDateFmt = DateTimeFormatter.ofPattern("d MMM")
        private val monthFmt = DateTimeFormatter.ofPattern("MMM yyyy")

        // Ksh 12,450 — used for balance (full precision matters)
        fun ksh(amount: Double): String = "Ksh ${"%,.0f".format(amount)}"

        // 12.5K / 1.2M — used for compact period cards
        fun shortKsh(amount: Double): String = when {
            amount >= 1_000_000 -> "${"%.1f".format(amount / 1_000_000)}M"
            amount >= 1_000 -> "${"%.1f".format(amount / 1_000)}K"
            else -> "%,.0f".format(amount)
        }

        private fun txDateLabel(dateStr: String, today: LocalDate): String {
            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return ""
            return when (date) {
                today -> "Today"
                today.minusDays(1) -> "Yest."
                else -> date.format(shortDateFmt)
            }
        }
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(COMPACT, WIDE, TALL, FULL)
    )

    // ── Glance entry point ────────────────────────────────────────────────────

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = fetchWidgetData(context)
        provideContent {
            WidgetRoot(data)
        }
    }

    // ── Data fetching ─────────────────────────────────────────────────────────

    private suspend fun fetchWidgetData(context: Context): WidgetData =
        withContext(Dispatchers.IO) {
            try {
                val dao = AppDatabase.getDatabase(context).transactionDao()
                val today = LocalDate.now()

                val todayStr = today.toString()
                val weekStart =
                    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toString()
                val monthStart = today.with(TemporalAdjusters.firstDayOfMonth()).toString()
                val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth()).toString()

                val dayTxs = dao.getTransactionsForDateRange(todayStr, todayStr)
                val weekTxs = dao.getTransactionsForDateRange(weekStart, weekEnd)
                val monthTxs = dao.getTransactionsForDateRange(monthStart, monthEnd)
                val allTxs = dao.getAllTransactionsOnce()

                val latestTx = allTxs.firstOrNull()

                WidgetData(
                    balance = latestTx?.balance ?: 0.0,
                    balanceDate = latestTx?.date?.let {
                        runCatching { LocalDate.parse(it).format(shortDateFmt) }.getOrNull()
                    }.orEmpty(),
                    day = metrics(dayTxs),
                    week = metrics(weekTxs),
                    month = metrics(monthTxs),
                    recentTxs = allTxs.take(3).map { tx ->
                        TxSummary(
                            label = tx.recipient.take(16).trimEnd(),
                            amount = tx.amount,
                            type = tx.type,
                            date = txDateLabel(tx.date, today)
                        )
                    },
                    monthLabel = today.format(monthFmt)
                )
            } catch (e: Exception) {
                WidgetData()
            }
        }

    private fun metrics(txs: List<MpesaTransactions>): Metrics {
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

    // ── Root composable ───────────────────────────────────────────────────────

    @Composable
    private fun WidgetRoot(data: WidgetData) {
        val size = LocalSize.current
        val isWide = size.width >= WIDE.width
        val isTall = size.height >= TALL.height

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetBackground)
                .cornerRadius(20.dp)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            Header(data.monthLabel)
            Spacer(GlanceModifier.height(8.dp))

            BalanceSection(data.balance, data.balanceDate)
            Spacer(GlanceModifier.height(10.dp))

            when {
                // ── Full (4×4): period row + recent list ──────────────────────
                isTall && isWide -> {
                    PeriodRow(data)
                    Spacer(GlanceModifier.height(8.dp))
                    ThinDivider()
                    Spacer(GlanceModifier.height(8.dp))
                    RecentSection(data.recentTxs)
                }
                // ── Tall (3×4): stacked period cards + recent list ────────────
                isTall -> {
                    PeriodStack(data)
                    Spacer(GlanceModifier.height(8.dp))
                    ThinDivider()
                    Spacer(GlanceModifier.height(8.dp))
                    RecentSection(data.recentTxs)
                }
                // ── Wide (4×2): period row only ───────────────────────────────
                isWide -> {
                    PeriodRow(data)
                }
                // ── Compact (3×2): today only ─────────────────────────────────
                else -> {
                    PeriodCard("Today", data.day, GlanceModifier.fillMaxWidth())
                }
            }

            Spacer(GlanceModifier.defaultWeight())
            OpenButton()
        }
    }

    // ── Layout blocks ─────────────────────────────────────────────────────────

    @Composable
    private fun PeriodRow(data: WidgetData) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            PeriodCard("Today", data.day, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            PeriodCard("This Week", data.week, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            PeriodCard("This Month", data.month, GlanceModifier.defaultWeight())
        }
    }

    @Composable
    private fun PeriodStack(data: WidgetData) {
        PeriodCard("Today", data.day, GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(5.dp))
        PeriodCard("This Week", data.week, GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(5.dp))
        PeriodCard("This Month", data.month, GlanceModifier.fillMaxWidth())
    }

    // ── Section composables ───────────────────────────────────────────────────

    @Composable
    private fun Header(monthLabel: String) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "💰 Cashia",
                style = TextStyle(
                    color = ColorProvider(WidgetPrimary),
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(13f, TextUnitType.Sp)
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = monthLabel,
                style = TextStyle(
                    color = ColorProvider(WidgetSubtle),
                    fontSize = TextUnit(10f, TextUnitType.Sp)
                )
            )
        }
    }

    @Composable
    private fun BalanceSection(balance: Double, balanceDate: String) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "M-PESA BALANCE",
                style = TextStyle(
                    color = ColorProvider(WidgetSubtle),
                    fontSize = TextUnit(9f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = ksh(balance),
                style = TextStyle(
                    color = ColorProvider(WidgetText),
                    fontWeight = FontWeight.Bold,
                    fontSize = TextUnit(22f, TextUnitType.Sp)
                )
            )
            if (balanceDate.isNotBlank()) {
                Text(
                    text = "as of $balanceDate",
                    style = TextStyle(
                        color = ColorProvider(WidgetSubtle),
                        fontSize = TextUnit(9f, TextUnitType.Sp)
                    )
                )
            }
        }
    }

    @Composable
    private fun PeriodCard(label: String, metrics: Metrics, modifier: GlanceModifier) {
        val net = metrics.net
        val isPositiveNet = net >= 0

        Column(
            modifier = modifier
                .background(WidgetCard)
                .cornerRadius(10.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // Period label
            Text(
                text = label.uppercase(),
                style = TextStyle(
                    color = ColorProvider(WidgetSubtle),
                    fontSize = TextUnit(8f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(4.dp))

            // Income row
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "↑",
                    style = TextStyle(
                        color = ColorProvider(WidgetIncome),
                        fontSize = TextUnit(10f, TextUnitType.Sp)
                    )
                )
                Spacer(GlanceModifier.width(3.dp))
                Text(
                    text = shortKsh(metrics.income),
                    style = TextStyle(
                        color = ColorProvider(WidgetIncome),
                        fontSize = TextUnit(10f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(GlanceModifier.height(2.dp))

            // Expense row
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "↓",
                    style = TextStyle(
                        color = ColorProvider(WidgetExpense),
                        fontSize = TextUnit(10f, TextUnitType.Sp)
                    )
                )
                Spacer(GlanceModifier.width(3.dp))
                Text(
                    text = shortKsh(metrics.expenses),
                    style = TextStyle(
                        color = ColorProvider(WidgetExpense),
                        fontSize = TextUnit(10f, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(GlanceModifier.height(4.dp))

            // Net row — coloured by sign
            Text(
                text = "${if (isPositiveNet) "+" else "−"}${shortKsh(abs(net))}",
                style = TextStyle(
                    color = ColorProvider(if (isPositiveNet) WidgetIncome else WidgetExpense),
                    fontSize = TextUnit(9f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun RecentSection(txs: List<TxSummary>) {
        Text(
            text = "RECENT",
            style = TextStyle(
                color = ColorProvider(WidgetSubtle),
                fontSize = TextUnit(9f, TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(5.dp))

        if (txs.isEmpty()) {
            Text(
                text = "No transactions yet",
                style = TextStyle(
                    color = ColorProvider(WidgetSubtle),
                    fontSize = TextUnit(10f, TextUnitType.Sp)
                )
            )
        } else {
            txs.forEachIndexed { index, tx ->
                TxRow(tx)
                if (index < txs.lastIndex) Spacer(GlanceModifier.height(5.dp))
            }
        }
    }

    @Composable
    private fun TxRow(tx: TxSummary) {
        val isIncome = tx.type == "income"
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // Arrow indicator
            Text(
                text = if (isIncome) "↑" else "↓",
                style = TextStyle(
                    color = ColorProvider(if (isIncome) WidgetIncome else WidgetExpense),
                    fontSize = TextUnit(11f, TextUnitType.Sp)
                )
            )
            Spacer(GlanceModifier.width(4.dp))

            // Recipient — fills available space
            Box(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = tx.label,
                    style = TextStyle(
                        color = ColorProvider(WidgetText),
                        fontSize = TextUnit(10f, TextUnitType.Sp)
                    )
                )
            }

            Spacer(GlanceModifier.width(4.dp))

            // Amount
            Text(
                text = "${if (isIncome) "+" else "−"}${shortKsh(tx.amount)}",
                style = TextStyle(
                    color = ColorProvider(if (isIncome) WidgetIncome else WidgetExpense),
                    fontSize = TextUnit(10f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.width(6.dp))

            // Date badge
            Box(
                modifier = GlanceModifier
                    .background(WidgetCard)
                    .cornerRadius(4.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tx.date,
                    style = TextStyle(
                        color = ColorProvider(WidgetSubtle),
                        fontSize = TextUnit(8f, TextUnitType.Sp)
                    )
                )
            }
        }
    }

    @Composable
    private fun ThinDivider() {
        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(WidgetCard)
        )
    }

    @Composable
    private fun OpenButton() {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(30.dp)
                .background(WidgetCard)
                .cornerRadius(8.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Open Cashia  →",
                style = TextStyle(
                    color = ColorProvider(WidgetPrimary),
                    fontSize = TextUnit(11f, TextUnitType.Sp),
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    // ── Data models ───────────────────────────────────────────────────────────

    data class WidgetData(
        val balance: Double = 0.0,
        val balanceDate: String = "",
        val day: Metrics = Metrics(),
        val week: Metrics = Metrics(),
        val month: Metrics = Metrics(),
        val recentTxs: List<TxSummary> = emptyList(),
        val monthLabel: String = "Cashia"
    )

    data class Metrics(
        val income: Double = 0.0,
        val expenses: Double = 0.0
    ) {
        val net: Double get() = income - expenses
    }

    data class TxSummary(
        val label: String,
        val amount: Double,
        val type: String,
        val date: String
    )
}