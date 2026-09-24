package com.example.cashia.utility

import com.example.cashia.models.MpesaTransactions
import java.util.Locale

enum class ExportColumn(val header: String) {
    DATE("Date"),
    TIME("Time"),
    TYPE("Type"),
    CATEGORY("Category"),
    RECIPIENT("Recipient"),
    AMOUNT("Amount"),
    TRANSACTION_COST("Transaction Cost"),
    BALANCE("Balance After"),
    TRANSACTION_ID("Transaction ID");

    fun extract(tx: MpesaTransactions, categoryLabelMap: Map<String, String>): String =
        when (this) {
            DATE -> tx.date
            TIME -> tx.time.orEmpty()
            TYPE -> tx.type.replaceFirstChar { it.uppercase() }
            CATEGORY -> categoryLabelMap[tx.category] ?: tx.category
            RECIPIENT -> tx.recipient
            AMOUNT -> String.format(Locale.US, "%.2f", tx.amount)
            TRANSACTION_COST -> tx.transactionCost?.let { String.format(Locale.US, "%.2f", it) }
                .orEmpty()

            BALANCE -> tx.balance?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()
            TRANSACTION_ID -> tx.reference.orEmpty()
        }
}

/** Escapes a single CSV field per RFC 4180: quote if it contains a comma, quote, or newline. */
private fun String.toCsvField(): String =
    if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"${replace("\"", "\"\"")}\""
    } else {
        this
    }

/**
 * Builds a CSV document (header row + one row per transaction) from the given [transactions],
 * restricted to [columns] (kept in [ExportColumn] declaration order), resolving categories via
 * [categoryLabelMap].
 */
fun buildTransactionsCsv(
    transactions: List<MpesaTransactions>,
    columns: List<ExportColumn>,
    categoryLabelMap: Map<String, String>,
): String {
    val orderedColumns = ExportColumn.entries.filter { it in columns }
    val sb = StringBuilder()
    sb.append(orderedColumns.joinToString(",") { it.header.toCsvField() }).append("\r\n")
    for (tx in transactions) {
        sb.append(
            orderedColumns.joinToString(",") { column ->
                column.extract(tx, categoryLabelMap).toCsvField()
            }
        ).append("\r\n")
    }
    return sb.toString()
}
