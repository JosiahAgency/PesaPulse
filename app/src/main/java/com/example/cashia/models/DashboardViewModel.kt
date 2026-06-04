package com.example.cashia.models

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.cashia.utility.TimeScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class DashboardViewModel(private val context: Context) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate = _selectedDate.asStateFlow()

    private val _scope = MutableStateFlow(TimeScope.DAY)
    val scope = _scope.asStateFlow()

    // Owned here so it survives tab switches
    private val _isListExpanded = MutableStateFlow(true)
    val isListExpanded = _isListExpanded.asStateFlow()

    fun setDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setScope(scope: TimeScope) {
        _scope.value = scope
    }

    fun toggleListExpanded() {
        _isListExpanded.value = !_isListExpanded.value
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DashboardViewModel::class.java))
                        return DashboardViewModel(context) as T
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}