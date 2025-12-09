package com.example.cashia.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestinations(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : AppDestinations("dashboard", "Dashboard", Icons.Default.Home)
    object Accounts : AppDestinations("accounts", "Accounts", Icons.Default.AccountBox)
    object Transactions : AppDestinations("transactions", "Transactions", Icons.Default.MonetizationOn)
    object AddTransaction :
        AppDestinations("add_transaction", "Add Transactions", Icons.Default.AddCircle)

    object AddAccount : AppDestinations("add_account", "Add Account", Icons.Default.AddCircle)
}