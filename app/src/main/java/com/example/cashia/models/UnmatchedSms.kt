package com.example.cashia.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The lifecycle state of an unmatched SMS.
 *
 *  PENDING  — not yet acted on; surfaces in the review modal and pending count badge.
 *  SKIPPED  — user dismissed it for now; still visible in the list but not counted as pending.
 *             The learning pipeline will still reprocess it on a parser version bump.
 *  IGNORED  — user explicitly marked it as noise (failed tx, promo, duplicate alert, etc.).
 *             Hidden from the main list by default; never reprocessed; never counted.
 *  SAVED    — user manually categorised it; a real transaction was created from it.
 *             Kept for audit trail; excluded from all active views.
 */
enum class SmsStatus { PENDING, SKIPPED, IGNORED, SAVED }

@Entity(
    tableName = "UnmatchedSms",
    indices = [
        Index(value = ["status"]),
        Index(value = ["contentHash"]),
        Index(value = ["parserVersion"]),
        Index(value = ["confidence"]),
        Index(value = ["smsId"], unique = true)
    ]
)
data class UnmatchedSms(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val smsId: Long = 0L,
    val rawBody: String,
    val receivedAt: Long = System.currentTimeMillis(),
    val status: SmsStatus = SmsStatus.PENDING,
    val contentHash: String = "",
    val parserVersion: Int = 0,
    val confidence: Float = 0f,
    val extractedFields: String? = null,
    val patternGroup: String? = null
)
