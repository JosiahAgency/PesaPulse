package com.example.cashia.utility

import androidx.room.withTransaction
import com.example.cashia.AppDatabase
import com.example.cashia.models.MpesaTransactions
import com.example.cashia.models.PaymentMatch
import com.example.cashia.models.RecurrenceType
import com.example.cashia.models.UpcomingStatus
import com.example.cashia.models.UpcomingTransaction
import com.example.cashia.models.UpcomingType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.abs

/** Returns evidence for a suggestion, never permission to automatically mark a payment paid. */
fun paymentMatchReason(upcoming: UpcomingTransaction, transaction: MpesaTransactions): String? {
    val expectedType = when (upcoming.type) {
        UpcomingType.EXPENSE -> "expense"
        UpcomingType.INCOME -> "income"
        UpcomingType.SAVING -> return null // A savings label alone does not establish wallet direction.
    }
    if (transaction.type != expectedType || transaction.reversedTxId != null) return null
    if (upcoming.status != UpcomingStatus.PENDING || upcoming.amount <= 0 || !upcoming.amount.isFinite()) return null
    val difference = abs(transaction.amount - upcoming.amount) / upcoming.amount
    if (!difference.isFinite() || difference > 0.05) return null
    val days = abs(ChronoUnit.DAYS.between(LocalDate.parse(upcoming.dueDate), LocalDate.parse(transaction.date)))
    if (days > 7) return null
    val stopWords = setOf("the", "and", "for", "ltd", "limited", "payment", "mpesa", "ksh", "kes")
    fun tokens(value: String) = value.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 3 && it !in stopWords }.toSet()
    val titleTokens = tokens(upcoming.title)
    val recipientTokens = tokens(transaction.recipient)
    val shared = titleTokens.intersect(recipientTokens)
    val exactName = titleTokens.isNotEmpty() && titleTokens == recipientTokens
    val fullName = shared.size >= 2 &&
        (shared == titleTokens || shared == recipientTokens)
    val phoneMatch = shared.any { it.all(Char::isDigit) && it.length in 9..12 }
    if (!exactName && !fullName && !phoneMatch) return null
    return "Recipient matches; amount within 5%; payment within 7 days of due date."
}

fun nextRecurringDate(dueDate: LocalDate, recurrence: RecurrenceType, anchor: LocalDate): LocalDate =
    when (recurrence) {
        RecurrenceType.NONE -> dueDate
        RecurrenceType.WEEKLY -> dueDate.plusWeeks(1)
        RecurrenceType.MONTHLY -> YearMonth.from(dueDate).plusMonths(1).let { month ->
            month.atDay(anchor.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
        }
        RecurrenceType.YEARLY -> YearMonth.of(dueDate.year + 1, anchor.month).let { month ->
            month.atDay(anchor.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
        }
    }

class UpcomingPaymentService(private val db: AppDatabase) {
    private val upcomingDao = db.upcomingTransactionDao()
    private val transactionDao = db.transactionDao()
    private val matchDao = db.paymentMatchDao()

    suspend fun suggest(transactions: List<MpesaTransactions>) = db.withTransaction {
        val pending = upcomingDao.getPendingOnce()
        for (transaction in transactions) {
            if (upcomingDao.isTransactionMatched(transaction.id) ||
                transactionDao.getReversalFor(transaction.id) != null
            ) continue
            val suggestions = pending.mapNotNull { upcoming ->
                paymentMatchReason(upcoming, transaction)?.let {
                    PaymentMatch(upcoming.id, transaction.id, it)
                }
            }
            if (suggestions.isNotEmpty()) matchDao.insert(suggestions)
        }
    }

    /** Also repairs matching missed by older versions or by an interrupted UI session. */
    suspend fun reconcilePending() = db.withTransaction {
        val pending = upcomingDao.getPendingOnce()
        if (pending.isEmpty()) return@withTransaction
        val start = pending.minOf { LocalDate.parse(it.dueDate) }.minusDays(7)
        val end = pending.maxOf { LocalDate.parse(it.dueDate) }.plusDays(7)
        suggest(transactionDao.getTransactionsForDateRange(start.toString(), end.toString()))
    }

    suspend fun confirm(upcomingId: String, transactionId: String): Boolean = db.withTransaction {
        val upcoming = upcomingDao.getById(upcomingId) ?: return@withTransaction false
        val transaction = transactionDao.getTransactionById(transactionId) ?: return@withTransaction false
        if (matchDao.get(upcomingId, transactionId) == null ||
            paymentMatchReason(upcoming, transaction) == null ||
            transactionDao.getReversalFor(transactionId) != null ||
            upcomingDao.isTransactionMatched(transactionId)
        ) return@withTransaction false
        complete(upcomingId, UpcomingStatus.PAID, transactionId)
    }

    suspend fun complete(
        id: String,
        status: UpcomingStatus,
        transactionId: String? = null
    ): Boolean = db.withTransaction {
        require(status == UpcomingStatus.PAID || status == UpcomingStatus.SKIPPED)
        val current = upcomingDao.getById(id) ?: return@withTransaction false
        if (transactionId != null && upcomingDao.isTransactionMatched(transactionId)) return@withTransaction false
        if (upcomingDao.completePending(id, status.name, transactionId) != 1) return@withTransaction false
        if (current.isRecurring) {
            val next = nextRecurringDate(
                LocalDate.parse(current.dueDate), current.recurrence, LocalDate.parse(current.recurrenceAnchor)
            )
            upcomingDao.upsert(
                current.copy(
                    id = UUID.nameUUIDFromBytes("$id:next".toByteArray(Charsets.UTF_8)).toString(),
                    dueDate = next.toString(),
                    status = UpcomingStatus.PENDING,
                    matchedTransactionId = null,
                    createdAt = System.currentTimeMillis()
                )
            )
            reconcilePending()
        }
        true
    }
}
