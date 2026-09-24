package com.example.cashia.utility

import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Notification extras are external input; only registered destinations are accepted. */
fun validatedNotificationRoute(route: String): String? {
    if (route in setOf("dashboard", "transactions", "upcoming_payments", "notifications_settings")) return route
    val parts = route.split('/')
    if (parts.size != 3 || parts[0] != "summary_report" || parts[1] !in setOf("day", "month")) return null
    return try {
        val date = LocalDate.parse(parts[2])
        if (date.toString() == parts[2]) route else null
    } catch (_: DateTimeParseException) { null }
}
