package com.example.cashia.models

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cashia.AppDatabase
import com.example.cashia.utility.ExportColumn
import com.example.cashia.utility.buildTransactionsCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Powers the "Export Transactions" screen: column + period selection, CSV generation, SAF write. */
class ExportViewModel(private val context: Context) : ViewModel() {

    private val dao = AppDatabase.getDatabase(context).transactionDao()

    /** Quick period presets shown as chips; [CUSTOM] defers to [customStart]/[customEnd]. */
    enum class PeriodPreset(val label: String) {
        THIS_MONTH("This Month"),
        LAST_MONTH("Last Month"),
        LAST_3_MONTHS("Last 3 Months"),
        THIS_YEAR("This Year"),
        ALL_TIME("All Time"),
        CUSTOM("Custom Range"),
    }

    sealed class ExportEvent {
        data class Success(val count: Int, val fileName: String) : ExportEvent()
        data class Error(val message: String) : ExportEvent()
    }

    var selectedPreset by mutableStateOf(PeriodPreset.THIS_MONTH)
        private set
    var customStart by mutableStateOf<LocalDate?>(null)
        private set
    var customEnd by mutableStateOf<LocalDate?>(null)
        private set
    var selectedColumns by mutableStateOf(ExportColumn.entries.toSet())
        private set
    var isExporting by mutableStateOf(false)
        private set

    /** Count of transactions matching the current period — refreshed on any period change. */
    var matchingCount by mutableStateOf<Int?>(null)
        private set

    private val _exportEvent = MutableSharedFlow<ExportEvent>(extraBufferCapacity = 1)
    val exportEvent: SharedFlow<ExportEvent> = _exportEvent.asSharedFlow()

    init {
        refreshCount()
    }

    fun selectPreset(preset: PeriodPreset) {
        selectedPreset = preset
        if (preset != PeriodPreset.CUSTOM) {
            customStart = null
            customEnd = null
        }
        refreshCount()
    }

    fun setCustomRange(start: LocalDate?, end: LocalDate?) {
        selectedPreset = PeriodPreset.CUSTOM
        customStart = start
        customEnd = end
        refreshCount()
    }

    fun toggleColumn(column: ExportColumn) {
        selectedColumns = if (column in selectedColumns) {
            // Keep at least one column selected — an empty CSV isn't useful.
            if (selectedColumns.size > 1) selectedColumns - column else selectedColumns
        } else {
            selectedColumns + column
        }
    }

    /** Resolves the active preset into an inclusive [start, end] ISO date range (null = unbounded). */
    private fun resolvedRange(): Pair<LocalDate?, LocalDate?> {
        val today = LocalDate.now()
        return when (selectedPreset) {
            PeriodPreset.THIS_MONTH ->
                today.with(TemporalAdjusters.firstDayOfMonth()) to today

            PeriodPreset.LAST_MONTH -> {
                val lastMonth = today.minusMonths(1)
                lastMonth.with(TemporalAdjusters.firstDayOfMonth()) to
                        lastMonth.with(TemporalAdjusters.lastDayOfMonth())
            }

            PeriodPreset.LAST_3_MONTHS -> today.minusMonths(3) to today
            PeriodPreset.THIS_YEAR ->
                today.with(TemporalAdjusters.firstDayOfYear()) to today

            PeriodPreset.ALL_TIME -> null to null
            PeriodPreset.CUSTOM -> customStart to customEnd
        }
    }

    private suspend fun fetchTransactions(): List<MpesaTransactions> {
        val (start, end) = resolvedRange()
        return if (start == null || end == null) {
            dao.getAllTransactionsOnce()
        } else {
            dao.getTransactionsForDateRange(start.toString(), end.toString())
        }
    }

    fun refreshCount() {
        viewModelScope.launch(Dispatchers.IO) {
            matchingCount = fetchTransactions().size
        }
    }

    /** Generates the CSV and writes it to the SAF-provided [uri] (from a CreateDocument launcher). */
    fun exportTo(uri: Uri, categoryLabelMap: Map<String, String>, fileName: String) {
        if (selectedColumns.isEmpty() || isExporting) return
        viewModelScope.launch(Dispatchers.IO) {
            isExporting = true
            try {
                val transactions = fetchTransactions()
                val csv = buildTransactionsCsv(
                    transactions = transactions,
                    columns = selectedColumns.toList(),
                    categoryLabelMap = categoryLabelMap,
                )
                val written = withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                        true
                    } ?: false
                }
                if (written) {
                    _exportEvent.tryEmit(ExportEvent.Success(transactions.size, fileName))
                } else {
                    _exportEvent.tryEmit(ExportEvent.Error("Couldn't open the selected file for writing."))
                }
            } catch (e: Exception) {
                Log.e("ExportViewModel", "CSV export failed", e)
                _exportEvent.tryEmit(ExportEvent.Error(e.message ?: "Export failed."))
            } finally {
                isExporting = false
            }
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ExportViewModel::class.java)) {
                        return ExportViewModel(context) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
