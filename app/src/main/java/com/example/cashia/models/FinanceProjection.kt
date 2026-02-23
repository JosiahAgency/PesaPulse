package com.example.cashia.models

import java.time.LocalDate

data class FinanceProjection(
    val predictedIncome: Double,
    val predictedExpenses: Double,
    val predictedNet: Double,
    val predictedBalance: Double,
    val dailyTrend: List<Pair<LocalDate, Double>>
)