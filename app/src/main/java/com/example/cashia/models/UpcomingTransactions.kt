package com.example.cashia.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class RecurrenceType { NONE, WEEKLY, MONTHLY, YEARLY }
enum class UpcomingType { EXPENSE, INCOME, SAVING }
enum class UpcomingStatus { PENDING, PAID, SKIPPED }

@Entity(tableName = "UpcomingTransactions")
data class UpcomingTransaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val categoryId: String,
    val dueDate: String,
    val type: UpcomingType,
    val recurrence: RecurrenceType,
    val notes: String = "",
    val status: UpcomingStatus = UpcomingStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val matchedTransactionId: String? = null
)