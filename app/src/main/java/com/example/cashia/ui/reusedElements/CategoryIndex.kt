package com.example.cashia.ui.reusedElements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.example.cashia.models.ResolvedCategory

/**
 * The resolved category list plus an `id -> category` index.
 *
 * Category lookups happen once per rendered transaction row, so scanning [all] with `find`
 * made resolution O(rows x categories). The index is built once per list change via
 * [rememberCategoryIndex] and shared by every row, making each lookup O(1).
 *
 * [all] is retained because pickers still need the ordered list.
 */
@Immutable
data class CategoryIndex(
    val all: List<ResolvedCategory> = emptyList(),
    val byId: Map<String, ResolvedCategory> = emptyMap(),
) {
    fun resolve(id: String?): ResolvedCategory? = id?.let { byId[it] }
}

@Composable
fun rememberCategoryIndex(all: List<ResolvedCategory>): CategoryIndex =
    remember(all) { CategoryIndex(all, all.associateBy { it.id }) }
