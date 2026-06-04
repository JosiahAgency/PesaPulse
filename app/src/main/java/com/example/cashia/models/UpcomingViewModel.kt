package com.example.cashia.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UpcomingUiState(
    val showSheet: Boolean = false,
    val editing: UpcomingTransaction? = null,
    val draftTitle: String = "",
    val draftAmount: String = "",
    val draftCategory: String = TransactionCategory.OTHER.name,
    val draftDueDate: LocalDate = LocalDate.now().plusDays(1),
    val draftType: UpcomingType = UpcomingType.EXPENSE,
    val draftRecurrence: RecurrenceType = RecurrenceType.NONE,
    val draftNotes: String = "",
    val error: String? = null
)

class UpcomingViewModel(private val context: Context) : ViewModel() {

    private val dao = AppDatabase.getDatabase(context).upcomingTransactionDao()

    val upcoming: StateFlow<List<UpcomingTransaction>> = dao.getPending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val all: StateFlow<List<UpcomingTransaction>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

//    var uiState by mutableStateOf(UpcomingUiState())
//        private set

    private val _uiState = MutableStateFlow(UpcomingUiState())
    val uiState: StateFlow<UpcomingUiState> = _uiState.asStateFlow()
    var currentState = _uiState.value

    fun openCreate() {
        currentState = UpcomingUiState(showSheet = true)
    }

    fun openEdit(tx: UpcomingTransaction) {
        currentState = UpcomingUiState(
            showSheet = true,
            editing = tx,
            draftTitle = tx.title,
            draftAmount = tx.amount.toString(),
            draftCategory = tx.categoryId,
            draftDueDate = LocalDate.parse(tx.dueDate),
            draftType = tx.type,
            draftRecurrence = tx.recurrence,
            draftNotes = tx.notes
        )
    }

    fun closeSheet() {
        currentState = UpcomingUiState()
    }

    fun onTitleChange(v: String) {
        _uiState.update {
            it.copy(draftTitle = v, error = null)
        }
    }

    fun onAmountChange(v: String) {
        _uiState.update {
            it.copy(draftAmount = v, error = null)
        }
    }

    fun onCategoryChange(v: String) {
        _uiState.update {
            it.copy(draftCategory = v)
        }
    }

    fun onDueDateChange(v: LocalDate) {
        _uiState.update {
            it.copy(draftDueDate = v)
        }
    }

    fun onTypeChange(v: UpcomingType) {
        _uiState.update {
            it.copy(draftType = v)
        }
    }

    fun onRecurrenceChange(v: RecurrenceType) {
        _uiState.update {
            it.copy(draftRecurrence = v)
        }
    }

    fun onNotesChange(v: String) {
        _uiState.update {
            it.copy(draftNotes = v)
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────
    fun save() {
        val title = currentState.draftTitle.trim()
        val amount = currentState.draftAmount.toDouble()

        if (title.isBlank()) {
            _uiState.update {
                it.copy(error = "Title cannot be empty"); return
            }
        }
        if (amount == null || amount <= 0) {
            _uiState.update {
                it.copy(error = "Enter a valid amount"); return
            }
        }

        val txToSave = UpcomingTransaction(
            id = currentState.editing?.id ?: java.util.UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            categoryId = currentState.draftCategory,
            dueDate = currentState.draftDueDate.toString(),
            type = currentState.draftType,
            recurrence = currentState.draftRecurrence,
            notes = currentState.draftNotes.trim(),
            status = currentState.editing?.status ?: UpcomingStatus.PENDING,
            createdAt = currentState.editing?.createdAt ?: System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            dao.upsert(txToSave)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                currentState = UpcomingUiState()
            }
        }
    }

    // ── Mark paid / skip ──────────────────────────────────────────────────

    fun markPaid(tx: UpcomingTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateStatus(tx.id, UpcomingStatus.PAID.name)
            // If recurring, spawn the next occurrence
            if (tx.recurrence != RecurrenceType.NONE) {
                val nextDate = nextDueDate(LocalDate.parse(tx.dueDate), tx.recurrence)
                dao.upsert(
                    tx.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        dueDate = nextDate.toString(),
                        status = UpcomingStatus.PENDING,
                        matchedTransactionId = null
                    )
                )
            }
        }
    }

    fun skip(tx: UpcomingTransaction) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateStatus(tx.id, UpcomingStatus.SKIPPED.name)
            if (tx.recurrence != RecurrenceType.NONE) {
                val nextDate = nextDueDate(LocalDate.parse(tx.dueDate), tx.recurrence)
                dao.upsert(
                    tx.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        dueDate = nextDate.toString(),
                        status = UpcomingStatus.PENDING,
                        matchedTransactionId = null
                    )
                )
            }
        }
    }

    fun delete(tx: UpcomingTransaction) {
        viewModelScope.launch(Dispatchers.IO) { dao.deleteById(tx.id) }
    }

// Replace tryAutoMatch and add the new helpers in UpcomingViewModel:

    /**
     * Called automatically after new M-Pesa transactions are synced.
     * Runs the matcher against each new transaction.
     */
    fun tryAutoMatch(mpesaTx: MpesaTransactions) {
        viewModelScope.launch(Dispatchers.IO) {
            matchTransaction(mpesaTx)
        }
    }

    /**
     * Manual trigger — scans ALL recent M-Pesa transactions against
     * ALL pending upcoming items. Useful after adding a new upcoming item.
     */
    fun matchAllPending(allMpesaTx: List<MpesaTransactions>) {
        viewModelScope.launch(Dispatchers.IO) {
            // Only look at transactions from the last 14 days to avoid false positives
            val cutoff = LocalDate.now().minusDays(14).toString()
            val recent = allMpesaTx.filter { it.date >= cutoff }
            recent.forEach { matchTransaction(it) }
        }
    }

    fun openCreateFromHistory(
        txId: String,
        recipient: String,
        amount: Double,
        category: String,
        type: String
    ) {
        val scheduledType = if (type.equals("income", ignoreCase = true)) {
            UpcomingType.INCOME
        } else {
            UpcomingType.EXPENSE
        }
        val nextDate = LocalDate.now().plusMonths(1)

        currentState = UpcomingUiState(
            showSheet = true,
            draftTitle = recipient,
            draftAmount = amount.toString(),
            draftCategory = category,
            draftDueDate = nextDate,
            draftType = scheduledType,
            draftRecurrence = RecurrenceType.MONTHLY,
            draftNotes = "Created automatically from transaction reference: $txId"
        )
    }

    private suspend fun matchTransaction(mpesaTx: MpesaTransactions) {
        val pending = dao.getPendingOnce()
        if (pending.isEmpty()) return

        val txDate = runCatching { LocalDate.parse(mpesaTx.date) }.getOrNull() ?: return

        // Score every pending item against this M-Pesa transaction
        data class Candidate(val upcoming: UpcomingTransaction, val score: Int)

        val candidates = pending.mapNotNull { upcoming ->
            val score = computeMatchScore(upcoming, mpesaTx, txDate)
            if (score >= MATCH_THRESHOLD) Candidate(upcoming, score) else null
        }

        // Take the single best match only — never double-match
        val best = candidates.maxByOrNull { it.score } ?: return

        dao.updateStatus(best.upcoming.id, UpcomingStatus.PAID.name, mpesaTx.id)

        if (best.upcoming.recurrence != RecurrenceType.NONE) {
            val nextDate = nextDueDate(
                LocalDate.parse(best.upcoming.dueDate),
                best.upcoming.recurrence
            )
            dao.upsert(
                best.upcoming.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    dueDate = nextDate.toString(),
                    status = UpcomingStatus.PENDING,
                    matchedTransactionId = null
                )
            )
        }
    }

    /**
     * Multi-signal scoring. Returns 0–100.
     * MATCH_THRESHOLD = 40 means we need at least two medium signals to match.
     *
     * Signals:
     *  - Amount closeness      0–35 pts  (exact=35, within 5%=25, within 15%=10)
     *  - Date proximity        0–30 pts  (same day=30, ±1=25, ±3=15, ±7=5)
     *  - Keyword overlap       0–25 pts  (each matching word = 8pts, capped at 25)
     *  - Transaction type      0–10 pts  (income→INCOME/SAVING match, expense→EXPENSE)
     */
    private fun computeMatchScore(
        upcoming: UpcomingTransaction,
        mpesaTx: MpesaTransactions,
        txDate: LocalDate
    ): Int {
        var score = 0

        // ── Amount signal ─────────────────────────────────────────────────────
        val amountDiff = kotlin.math.abs(upcoming.amount - mpesaTx.amount)
        val amountRatio = if (upcoming.amount > 0) amountDiff / upcoming.amount else 1.0
        score += when {
            amountRatio < 0.01 -> 35   // within 1% — almost certainly the same payment
            amountRatio < 0.05 -> 25   // within 5%
            amountRatio < 0.15 -> 10   // within 15% — weak signal only
            else -> 0
        }

        // ── Date proximity signal ─────────────────────────────────────────────
        val dueDate = runCatching { LocalDate.parse(upcoming.dueDate) }.getOrNull() ?: return 0
        val daysDiff = kotlin.math.abs(
            java.time.temporal.ChronoUnit.DAYS.between(dueDate, txDate)
        )
        score += when {
            daysDiff == 0L -> 30
            daysDiff <= 1 -> 25
            daysDiff <= 3 -> 15
            daysDiff <= 7 -> 5
            else -> 0
        }

        // ── Keyword overlap signal ────────────────────────────────────────────
        // Tokenise both title and notes, filter short/common words
        val upcomingWords = (upcoming.title + " " + upcoming.notes)
            .lowercase()
            .split(TOKEN_REGEX)
            .filter { it.length >= 3 && it !in STOP_WORDS }
            .toSet()

        val recipientWords = mpesaTx.recipient
            .lowercase()
            .split(TOKEN_REGEX)
            .filter { it.length >= 3 && it !in STOP_WORDS }
            .toSet()

        // Also check if any upcoming word is a substring of the recipient or vice versa
        val exactMatches = upcomingWords.intersect(recipientWords).size
        val substringMatches = upcomingWords.count { uw ->
            recipientWords.any { rw -> rw.contains(uw) || uw.contains(rw) }
        }
        val keywordScore = ((exactMatches * 10) + (substringMatches * 6)).coerceAtMost(25)
        score += keywordScore

        // ── Transaction type signal ───────────────────────────────────────────
        val typeMatch = when {
            mpesaTx.type == "income" && upcoming.type in
                    listOf(UpcomingType.INCOME, UpcomingType.SAVING) -> true

            mpesaTx.type == "expense" && upcoming.type == UpcomingType.EXPENSE -> true
            else -> false
        }
        if (typeMatch) score += 10

        return score
    }

    private fun nextDueDate(from: LocalDate, recurrence: RecurrenceType) = when (recurrence) {
        RecurrenceType.WEEKLY -> from.plusWeeks(1)
        RecurrenceType.MONTHLY -> from.plusMonths(1)
        RecurrenceType.YEARLY -> from.plusYears(1)
        RecurrenceType.NONE -> from
    }

    companion object {
        private const val MATCH_THRESHOLD = 40
        private val TOKEN_REGEX = Regex("[\\s,.-]+")
        private val STOP_WORDS =
            setOf("the", "and", "for", "ltd", "inc", "plc", "pay", "ksh", "mpesa")

        fun provideFactory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                UpcomingViewModel(context) as T
        }
    }
}