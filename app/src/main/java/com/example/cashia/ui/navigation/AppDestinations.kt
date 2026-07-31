package com.example.cashia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestinations(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : AppDestinations("dashboard", "Home", Icons.Outlined.Home)
    object Transactions : AppDestinations("transactions", "History", Icons.Outlined.Receipt)
    object Calendar : AppDestinations("calendar", "Calendar", Icons.Outlined.CalendarMonth)
    object Analytics : AppDestinations("analytics", "Insights", Icons.Outlined.BarChart)
    object Projections : AppDestinations(
        "projections", "Forecast",
        Icons.AutoMirrored.Outlined.TrendingUp
    )

    object Settings : AppDestinations("settings", "Settings", Icons.Outlined.Settings)
    object Categories : AppDestinations("categories", "categories", Icons.Outlined.Category)

    /** Detail view for a daily/monthly summary. Not in the bottom nav. */
    object SummaryReport : AppDestinations(
        "summary_report/{period}/{date}", "Summary", Icons.Outlined.BarChart
    ) {
        const val ARG_PERIOD = "period"
        const val ARG_DATE = "date"

        /** [period] is "day" or "month"; [isoDate] an ISO yyyy-MM-dd anchor date. */
        fun buildRoute(period: String, isoDate: String) = "summary_report/$period/$isoDate"
    }
}