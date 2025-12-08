package com.example.cashia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.cashia.models.AccountsModel
import com.example.cashia.ui.navigation.AppDestinations
import com.example.cashia.ui.screens.AccountsScreen
import com.example.cashia.ui.screens.AddAccountScreen
import com.example.cashia.ui.screens.DashboardScreen
import com.example.cashia.ui.screens.TransactionScreen
import com.example.cashia.ui.theme.CashiaTheme

class MainActivity : ComponentActivity() {
    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_SMS
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_SMS), 100
            )
        }
        enableEdgeToEdge()
        setContent {
            CashiaTheme() {
                CashiaApp()
            }
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@Composable
fun CashiaApp() {
    val navController = rememberNavController()
    val paths = listOf(
        AppDestinations.Transactions,
        AppDestinations.Dashboard,
        AppDestinations.Accounts,
    )
    Scaffold(bottomBar = {
        NavigationBar {
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            paths.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        androidx.compose.material3.Icon(
                            screen.icon,
                            contentDescription = screen.label
                        )
                    }, label = { Text(screen.label) },
                    selected = currentDestination?.route == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
            }
        }
    }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.Dashboard.route,
            modifier = Modifier.padding(innerPadding)

        ) {
            composable(AppDestinations.Transactions.route) {
                TransactionScreen { navController.popBackStack() }
            }
            composable(AppDestinations.Dashboard.route) {
                DashboardScreen { route -> navController.navigate(route) }
            }
            composable(AppDestinations.Accounts.route) {
                val accounts = listOf(
                    AccountsModel(1, "Cash", 1200.0),
                    AccountsModel(2, "Bank", 5400.0)
                )

                AccountsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    accounts = accounts,
                    onAddAccount = { navController.navigate(AppDestinations.AddAccount.route) }
                )
            }
            composable(AppDestinations.AddAccount.route) {
                AddAccountScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
