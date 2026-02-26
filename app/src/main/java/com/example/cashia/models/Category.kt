package com.example.cashia.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Categories")
data class Category(
	   @PrimaryKey val id: String,          // UUID
	   val name: String,
	   val emoji: String = "📦",            // visual identifier
	   val keywords: String = "",           // comma-separated keywords for auto-categorization
	   val isDefault: Boolean = false       // true for "Fuel" and "Uncategorized"
)