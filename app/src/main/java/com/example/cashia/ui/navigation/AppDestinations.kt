package com.example.cashia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
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

    /** Full CRUD list of upcoming payments — moved out of History into its own screen. */
    object UpcomingPayments :
        AppDestinations("upcoming_payments", "Upcoming Payments", Icons.Outlined.Schedule)

    /** Full list of unmatched SMS for review — moved out of History into its own screen. */
    object UnmatchedReview :
        AppDestinations("unmatched_review", "Review Unmatched SMS", Icons.Outlined.Sms)

    /** Detail view for a daily/monthly summary. Not in the bottom nav. */
    object SummaryReport : AppDestinations(
        "summary_report/{period}/{date}", "Summary", Icons.Outlined.BarChart
    ) {

        /** [period] is "day" or "month"; [isoDate] an ISO yyyy-MM-dd anchor date. */
        fun buildRoute(period: String, isoDate: String) = "summary_report/$period/$isoDate"
    }
}