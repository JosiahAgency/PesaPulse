package com.example.cashia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestinations(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : AppDestinations("dashboard", "Dashboard", Icons.Default.Home)
    object Analytics : AppDestinations("analytics", "Analytics", Icons.Default.AutoGraph)
    object Transactions : AppDestinations("transactions", "Transactions", Icons.Default.MonetizationOn)
}