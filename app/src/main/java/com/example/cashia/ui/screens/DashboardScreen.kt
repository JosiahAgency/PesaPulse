package com.example.cashia.ui.screens

import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Dashboard") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Welcome, Josiah!", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Here’s a quick overview of your finances.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(title = "Accounts", value = "3", onClick = { onNavigate("accounts") })
                StatCard(title = "Accounts", value = "12", onClick = { onNavigate("accounts") })
            }
            ElevatedCard(
                onClick = { onNavigate("accounts") },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("My Accounts") },
                    leadingContent = {
                        Icon(
                            Icons.Default.AccountBox,
                            contentDescription = "My Account"
                        )
                    }
                )
            }
            ElevatedCard(
                onClick = { onNavigate("transactions") },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("My Transactions") },
                    leadingContent = { Icon(Icons.Default.List, contentDescription = "List") })
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}