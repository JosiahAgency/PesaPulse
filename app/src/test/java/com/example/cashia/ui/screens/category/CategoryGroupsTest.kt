package com.example.cashia.ui.screens.category

import com.example.cashia.models.ResolvedCategory
import com.example.cashia.models.TransactionCategory
import com.example.cashia.models.toResolved
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryGroupsTest {
    private val categories = TransactionCategory.entries.map { it.toResolved() }

    @Test
    fun emptySearchIncludesEveryBuiltInExactlyOnce() {
        val groups = categoryGroups(categories, "")
        val ids = groups.flatMap { listOf(it.parent.id) + it.children.map { child -> child.id } }
        assertEquals(categories.map { it.id }.toSet(), ids.toSet())
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun childMatchKeepsParentContextWithoutUnrelatedChildren() {
        val groups = categoryGroups(categories, "  gRoCeRiEs  ")
        assertEquals(1, groups.size)
        assertEquals("FOOD", groups.single().parent.id)
        assertEquals(listOf("GROCERIES"), groups.single().children.map { it.id })
    }

    @Test
    fun parentMatchIncludesAllItsChildren() {
        val group = categoryGroups(categories, "Food & Drinks").single()
        assertEquals(categories.filter { it.parentId == "FOOD" }, group.children)
    }

    @Test
    fun searchesCustomisedLabelsAndExcludesCustomCategoriesFromLibrary() {
        val changed = categories.map { if (it.id == "FOOD") it.copy(label = "My meals") else it } +
            ResolvedCategory("custom_1", "My meals", "X", false)
        assertEquals(listOf("FOOD"), categoryGroups(changed, "My meals").map { it.parent.id })
        assertTrue(categoryGroups(changed, "does not exist").isEmpty())
    }
}
