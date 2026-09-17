package com.example.cashia.utility

import com.example.cashia.models.MpesaTransactions
import java.util.Locale

/**
 * One selectable CSV column for the transaction export feature. [header] is the CSV column
 * title, [extract] pulls the raw value for a row. [categoryLabelMap] resolves the stored
 * category id (system enum name or custom category id) to its human-readable label.
 */
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
            TIME -> tx.time
            TYPE -> tx.type.replaceFirstChar { it.uppercase() }
            CATEGORY -> categoryLabelMap[tx.category] ?: tx.category
            RECIPIENT -> tx.recipient
            AMOUNT -> String.format(Locale.US, "%.2f", tx.amount)
            TRANSACTION_COST -> String.format(Locale.US, "%.2f", tx.transactionCost)
            BALANCE -> String.format(Locale.US, "%.2f", tx.balance)
            TRANSACTION_ID -> tx.id
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
