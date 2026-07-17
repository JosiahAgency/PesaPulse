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

    /** Detail view for a daily/monthly summary. Not in the bottom nav. */
    object SummaryReport : AppDestinations(
        "summary_report/{period}/{date}", "Summary", Icons.Default.BarChart
    ) {
        const val ARG_PERIOD = "period"
        const val ARG_DATE = "date"

        /** [period] is "day" or "month"; [isoDate] an ISO yyyy-MM-dd anchor date. */
        fun buildRoute(period: String, isoDate: String) = "summary_report/$period/$isoDate"
    }
}