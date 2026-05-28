package com.example.cashia.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class RecurrenceType { NONE, WEEKLY, MONTHLY, YEARLY }
enum class UpcomingType { EXPENSE, INCOME, SAVING }
enum class UpcomingStatus { PENDING, PAID, SKIPPED }

// [FIX] Added indexes for the three columns most likely to appear in WHERE
// clauses and ORDER BY: dueDate (sort/filter by date), status (filter PENDING),
// and type (filter EXPENSE vs INCOME). Without these, Room does a full table
// scan for every query that filters on these columns.
@Entity(
    tableName = "UpcomingTransactions",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["status"]),
        Index(value = ["type"]),
        Index(value = ["categoryId"])
    ]
)
data class UpcomingTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val categoryId: String,
    /** ISO-8601 date string (yyyy-MM-dd) of the first / next due date. */
    val dueDate: String,
    val type: UpcomingType,
    val recurrence: RecurrenceType,
    val notes: String = "",
    val status: UpcomingStatus = UpcomingStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val matchedTransactionId: String? = null
) {

    // ─────────────────────────────────────────────
    // Parsed date — computed once, used by everything below.
    // ─────────────────────────────────────────────

    @get:Ignore
    val dueDateParsed: LocalDate? by lazy {
        runCatching { LocalDate.parse(dueDate) }.getOrNull()
    }

    // ─────────────────────────────────────────────
    // Display helpers
    // ─────────────────────────────────────────────

    @get:Ignore
    val displayDueDate: String by lazy {
        dueDateParsed?.format(PesaTimeConfiguration.getDateFormatter()) ?: dueDate
    }

    /** Human-readable label: "Today", "Tomorrow", "In 3 days", "3 days ago", etc. */
    @get:Ignore
    val relativeDueLabel: String
        get() {
            val date = dueDateParsed ?: return displayDueDate
            val today = LocalDate.now()
            val days = ChronoUnit.DAYS.between(today, date)
            return when {
                days == 0L -> "Today"
                days == 1L -> "Tomorrow"
                days == -1L -> "Yesterday"
                days > 1 -> "In $days days"
                else -> "${-days} days ago"
            }
        }

    // ─────────────────────────────────────────────
    // State predicates
    // ─────────────────────────────────────────────

    @get:Ignore
    val isPending: Boolean get() = status == UpcomingStatus.PENDING

    @get:Ignore
    val isPaid: Boolean get() = status == UpcomingStatus.PAID

    @get:Ignore
    val isSkipped: Boolean get() = status == UpcomingStatus.SKIPPED

    @get:Ignore
    val isOverdue: Boolean
        get() = isPending && (dueDateParsed?.isBefore(LocalDate.now()) == true)

    @get:Ignore
    val isDueToday: Boolean
        get() = dueDateParsed == LocalDate.now()

    /** True if due within the next [withinDays] days (inclusive of today). */
    fun isDueWithin(withinDays: Int): Boolean {
        val date = dueDateParsed ?: return false
        val today = LocalDate.now()
        return !date.isBefore(today) && !date.isAfter(today.plusDays(withinDays.toLong()))
    }

    // ─────────────────────────────────────────────
    // Recurrence helpers
    // ─────────────────────────────────────────────

    @get:Ignore
    val isRecurring: Boolean get() = recurrence != RecurrenceType.NONE

    /** The next occurrence after [from] (defaults to today). */
    fun nextOccurrence(from: LocalDate = LocalDate.now()): LocalDate? {
        if (!isRecurring) return null
        val base = dueDateParsed ?: return null
        return when (recurrence) {
            RecurrenceType.WEEKLY -> base.plusWeeks(
                ChronoUnit.WEEKS.between(base, from).coerceAtLeast(0) + 1
            )

            RecurrenceType.MONTHLY -> base.plusMonths(
                ChronoUnit.MONTHS.between(base, from).coerceAtLeast(0) + 1
            )

            RecurrenceType.YEARLY -> base.plusYears(
                ChronoUnit.YEARS.between(base, from).coerceAtLeast(0) + 1
            )

            RecurrenceType.NONE -> null
        }
    }

    /** Human-readable recurrence label, e.g. "Weekly", "Every month". */
    @get:Ignore
    val recurrenceLabel: String
        get() = when (recurrence) {
            RecurrenceType.NONE -> "One-time"
            RecurrenceType.WEEKLY -> "Weekly"
            RecurrenceType.MONTHLY -> "Monthly"
            RecurrenceType.YEARLY -> "Yearly"
        }

    // ─────────────────────────────────────────────
    // Financial helpers
    // ─────────────────────────────────────────────

    /** Signed amount: positive for income/savings, negative for expenses. */
    @get:Ignore
    val signedAmount: Double
        get() = when (type) {
            UpcomingType.EXPENSE -> -amount
            UpcomingType.INCOME, UpcomingType.SAVING -> amount
        }

    @get:Ignore
    val isExpense: Boolean get() = type == UpcomingType.EXPENSE

    @get:Ignore
    val isIncome: Boolean get() = type == UpcomingType.INCOME

    @get:Ignore
    val isSaving: Boolean get() = type == UpcomingType.SAVING
}