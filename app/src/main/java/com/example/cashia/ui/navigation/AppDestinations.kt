package com.example.cashia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestinations(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : AppDestinations("dashboard", "Home", Icons.Default.Home)
    object Transactions : AppDestinations("transactions", "History", Icons.Default.Receipt)
    object Calendar : AppDestinations("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Analytics : AppDestinations("analytics", "Insights", Icons.Default.BarChart)
    object Projections : AppDestinations(
        "projections", "Forecast",
        Icons.AutoMirrored.Filled.TrendingUp
    )

    object Settings : AppDestinations("settings", "Settings", Icons.Default.Settings)

    object Categories : AppDestinations("categories", "categories", Icons.Default.Category)

}