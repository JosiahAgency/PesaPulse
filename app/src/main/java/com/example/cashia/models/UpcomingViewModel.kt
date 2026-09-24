package com.example.cashia.models

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.example.cashia.AppDatabase
import com.example.cashia.utility.UpcomingPaymentService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

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

class UpcomingViewModel(
    context: Context,
    private val db: AppDatabase = AppDatabase.getDatabase(context.applicationContext)
) : ViewModel() {
    private val dao = db.upcomingTransactionDao()
    private val service = UpcomingPaymentService(db)
    val upcoming = dao.getPending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val all = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val possibleMatches = db.paymentMatchDao().pendingReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _uiState = MutableStateFlow(UpcomingUiState())
    val uiState = _uiState.asStateFlow()
    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage = _operationMessage.asStateFlow()
    private val _isWorking = MutableStateFlow(false)
    val isWorking = _isWorking.asStateFlow()

    init {
        runOperation("Could not check for possible payment matches.") {
            service.reconcilePending()
            null
        }
    }

    fun clearOperationMessage(message: String) { _operationMessage.compareAndSet(message, null) }
    fun openCreate() { _uiState.value = UpcomingUiState(showSheet = true) }
    fun closeSheet() { if (!_isWorking.value) _uiState.value = UpcomingUiState() }
    fun onTitleChange(v: String) { _uiState.update { it.copy(draftTitle = v, error = null) } }
    fun onAmountChange(v: String) { _uiState.update { it.copy(draftAmount = v, error = null) } }
    fun onCategoryChange(v: String) { _uiState.update { it.copy(draftCategory = v) } }
    fun onDueDateChange(v: LocalDate) { _uiState.update { it.copy(draftDueDate = v) } }
    fun onTypeChange(v: UpcomingType) { _uiState.update { it.copy(draftType = v) } }
    fun onRecurrenceChange(v: RecurrenceType) { _uiState.update { it.copy(draftRecurrence = v) } }
    fun onNotesChange(v: String) { _uiState.update { it.copy(draftNotes = v) } }

    fun openEdit(tx: UpcomingTransaction) {
        _uiState.value = UpcomingUiState(
            showSheet = true, editing = tx, draftTitle = tx.title,
            draftAmount = tx.amount.toString(), draftCategory = tx.categoryId,
            draftDueDate = LocalDate.parse(tx.dueDate), draftType = tx.type,
            draftRecurrence = tx.recurrence, draftNotes = tx.notes
        )
    }

    fun save() {
        val draft = _uiState.value
        val title = draft.draftTitle.trim()
        val amount = draft.draftAmount.trim().replace(",", "").toDoubleOrNull()
        if (title.isBlank()) {
            _uiState.update { it.copy(error = "Title cannot be empty") }
            return
        }
        if (amount == null || !amount.isFinite() || amount <= 0.0) {
            _uiState.update { it.copy(error = "Enter a valid amount") }
            return
        }
        val existing = draft.editing
        val dueDate = draft.draftDueDate.toString()
        val anchor = if (existing != null && existing.dueDate == dueDate &&
            existing.recurrence == draft.draftRecurrence) existing.recurrenceAnchor else dueDate
        val tx = UpcomingTransaction(
            id = existing?.id ?: UUID.randomUUID().toString(),
            title = title, amount = amount, categoryId = draft.draftCategory,
            dueDate = dueDate, type = draft.draftType, recurrence = draft.draftRecurrence,
            notes = draft.draftNotes.trim(), status = existing?.status ?: UpcomingStatus.PENDING,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            matchedTransactionId = existing?.matchedTransactionId, recurrenceAnchor = anchor
        )
        runOperation("Could not save this payment. Please try again.") {
            val saved = db.withTransaction {
                val current = existing?.let { dao.getById(it.id) }
                if (existing != null && current?.status != existing.status) false
                else {
                    dao.upsert(tx)
                    db.paymentMatchDao().clearActive(tx.id)
                    service.reconcilePending()
                    true
                }
            }
            if (saved) {
                withContext(Dispatchers.Main) { _uiState.value = UpcomingUiState() }
                "Payment saved."
            } else "This payment changed while you were editing. Close the editor and reopen it."
        }
    }

    fun markPaid(tx: UpcomingTransaction) {
        runOperation("Could not mark this payment paid. Please try again.") {
            if (service.complete(tx.id, UpcomingStatus.PAID)) "Payment marked paid."
            else "This payment has already been completed or removed."
        }
    }

    fun skip(tx: UpcomingTransaction) {
        runOperation("Could not skip this payment. Please try again.") {
            if (service.complete(tx.id, UpcomingStatus.SKIPPED)) "Payment skipped."
            else "This payment has already been completed or removed."
        }
    }

    fun delete(tx: UpcomingTransaction) {
        runOperation("Could not delete this payment. Please try again.") {
            dao.deleteById(tx.id)
            "Payment deleted."
        }
    }

    fun confirmMatch(match: PaymentMatchReview) {
        runOperation("Could not confirm this payment. Please try again.") {
            if (service.confirm(match.upcomingId, match.transactionId)) "Payment confirmed."
            else "This match is no longer available. Please review the payment again."
        }
    }

    fun dismissMatch(match: PaymentMatchReview) {
        runOperation("Could not dismiss this match. Please try again.") {
            db.paymentMatchDao().dismiss(match.upcomingId, match.transactionId)
            "Suggestion dismissed. The payment remains pending."
        }
    }

    private fun runOperation(errorMessage: String, block: suspend () -> String?) {
        if (_isWorking.value) {
            _operationMessage.value = "Please wait for the current operation to finish."
            return
        }
        _isWorking.value = true
        viewModelScope.launch {
            try {
                _operationMessage.value = withContext(Dispatchers.IO) { block() }
            } catch (e: SQLiteException) {
                Log.e("UpcomingViewModel", errorMessage, e)
                _operationMessage.value = errorMessage
                if (_uiState.value.showSheet) _uiState.update { it.copy(error = errorMessage) }
            } finally {
                _isWorking.value = false
            }
        }
    }

    fun openCreateFromHistory(
        txId: String, recipient: String, amount: Double, category: String, type: String, txDate: String
    ) {
        val transactionDate = LocalDate.parse(txDate)
        val today = LocalDate.now()
        var dueDate = today.withDayOfMonth(transactionDate.dayOfMonth.coerceAtMost(today.lengthOfMonth()))
        if (dueDate.isBefore(today)) {
            val nextMonth = today.plusMonths(1)
            dueDate = nextMonth.withDayOfMonth(transactionDate.dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
        }
        _uiState.value = UpcomingUiState(
            showSheet = true, draftTitle = recipient, draftAmount = amount.toString(),
            draftCategory = category, draftDueDate = dueDate,
            draftType = if (type.equals("income", true)) UpcomingType.INCOME else UpcomingType.EXPENSE,
            draftRecurrence = RecurrenceType.MONTHLY,
            draftNotes = "Created from transaction: $txId"
        )
    }

    companion object {
        fun provideFactory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = UpcomingViewModel(context) as T
        }
    }
}
