package com.example.cashia.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "SystemCategoryOverrides")
data class SystemCategoryOverride(
    @PrimaryKey val id: String,
    val label: String,
    val emoji: String
)