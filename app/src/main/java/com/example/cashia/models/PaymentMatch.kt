package com.example.cashia.models

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "PaymentMatches",
    primaryKeys = ["upcomingId", "transactionId"],
    indices = [Index("transactionId")]
)
data class PaymentMatch(
    val upcomingId: String,
    val transactionId: String,
    val reason: String,
    val dismissed: Boolean = false
)

data class PaymentMatchReview(
    val upcomingId: String,
    val transactionId: String,
    val title: String,
    val expectedAmount: Double,
    val dueDate: String,
    val recipient: String,
    val amount: Double,
    val date: String,
    val reference: String?,
    val reason: String
)
