package com.example.cashia.models

import androidx.lifecycle.ViewModelStore
import com.example.cashia.daos.TransactionDao
import com.example.cashia.utility.TransactionType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val store = ViewModelStore()
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val dao = mockk<TransactionDao>()
        every { dao.getTransactionsInRange(any(), any()) } returns MutableStateFlow(emptyList())
        viewModel = CalendarViewModel(dao, MutableStateFlow(emptyList()))
        store.put("calendar", viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun resetToTodaySelectsTodayFromAnotherMonthAndPreservesTypeFilter() = runTest {
        val otherMonth = LocalDate.now().minusMonths(2)
        viewModel.handleDayClick(otherMonth)
        viewModel.setFilter(TransactionType.EXPENSE)
        viewModel.uiState.first {
            it.selectedDate == otherMonth && it.selectedFilter == TransactionType.EXPENSE
        }

        viewModel.resetToToday()

        val state = viewModel.uiState.first {
            it.selectedDate == LocalDate.now() && it.filterByDay
        }
        assertEquals(TransactionType.EXPENSE, state.selectedFilter)
    }

    @Test
    fun resetToTodayEnablesDayFilterEvenWhenAlreadyInCurrentMonth() = runTest {
        viewModel.clearDayFilter()
        viewModel.resetToToday()

        val state = viewModel.uiState.first { it.filterByDay }
        assertEquals(LocalDate.now(), state.selectedDate)
    }
}
