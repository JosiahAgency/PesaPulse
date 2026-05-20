package com.example.cashia.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SyncState")
data class SyncState(
    @PrimaryKey
    val key: String,
    val lastSmsId: Long
)