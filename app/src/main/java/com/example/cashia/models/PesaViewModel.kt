package com.example.cashia.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.cashia.Utility.PesaTimeConfiguration


class PesaViewModel : ViewModel() {
    var use24HourFormat by mutableStateOf(false)

    fun toggleTimeFormat() {
        use24HourFormat = !use24HourFormat
        PesaTimeConfiguration.use24HrsFormat = use24HourFormat
    }
}
