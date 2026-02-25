package com.example.cashia.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cashia.Utility.BiometricAuthenticator
import com.example.cashia.Utility.PesaActions
import com.example.cashia.Utility.PesaTimeConfiguration
import com.example.cashia.models.SettingsViewModel
import com.example.cashia.ui.theme.primaryGradient
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onFirstDayOfWeekChange: (DayOfWeek) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val appVersion = remember { getAppVersion(context) }
    var showDayPicker by remember { mutableStateOf(false) }
    val settingsViewModel: SettingsViewModel = viewModel()

    val biometricAuthenticator = remember { BiometricAuthenticator(context) }


    if (showDayPicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDayPicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(32.dp), // More rounded for modern feel
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CalendarViewWeek,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Week Starts On",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        "Transactions will be grouped starting from this day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    val sortedDays = remember(PesaTimeConfiguration.firstDayOfWeek) {
                        val allDays = DayOfWeek.entries.toMutableList()
                        val selected = PesaTimeConfiguration.firstDayOfWeek

                        allDays.remove(selected)
                        allDays.add(0, selected)
                        allDays
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedDays) { day ->
                            val isSelected = PesaTimeConfiguration.firstDayOfWeek == day

                            val backgroundColor = if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else
                                Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(backgroundColor)
                                    .clickable {
                                        PesaActions.playClick(haptic) // Add haptic feedback
                                        onFirstDayOfWeekChange(day)
                                        showDayPicker = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = day.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        brush = if (isSelected) primaryGradient() else null,
                                    ),
                                    color = if (!isSelected) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                                )

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle, // Using a filled check for more weight
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Appearance Section
        item {
            SettingsSection(title = "Appearance") {
                SettingsToggleItem(
                    label = "App Theme",
                    subtitle = "Switch between Light & Dark Modes.",
                    icon = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    checked = isDarkTheme,
                    onCheckedChange = {
                        PesaActions.playClick(haptic)
                        onToggleTheme(it)
                    }
                )
//                SettingsToggleItem(
//                    label = "Use 24-Hour Clock",
//                    subtitle = "Toggle between 12h (AM/PM) and 24h Time Format",
//                    icon = Icons.Rounded.Schedule,
//                    checked = use24HourFormat,
//                    onCheckedChange = {
//                        PesaActions.playClick(haptic)
//                        onToggle24Hour(it)
//                    }
//                )
//                SettingsActionItem(
//                    label = "Week Start",
//                    subtitle = PesaTimeConfiguration.firstDayOfWeek.name.lowercase()
//                        .replaceFirstChar { it.uppercase() },
//                    icon = Icons.Rounded.CalendarMonth,
//                    onClick = {
//                        PesaActions.playClick(haptic)
//                        showDayPicker = true
//                    },
//                    isExpanded = false
//                )
            }
        }

//         Security Section
        item {
            SettingsSection(title = "Security") {
                // Inside SettingsScreen.composable
                SettingsToggleItem(
                    label = "App Lock",
                    subtitle = "Secure your financial data with biometrics or device PIN.",
                    icon = Icons.Rounded.Fingerprint,
                    checked = PesaTimeConfiguration.isBiometricEnabled,
                    onCheckedChange = { enable ->
                        PesaActions.playClick(haptic)
                        if (enable) {
                            val activity = context as? FragmentActivity
                            activity?.let {
                                biometricAuthenticator.promptAuthenticator(
                                    activity = it,
                                    onSuccess = { settingsViewModel.setBiometric(true) },
                                    onError = { msg ->
                                        Toast.makeText(
                                            context,
                                            msg,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        } else {
                            settingsViewModel.setBiometric(false)
                        }
                    }
                )
//                SettingsToggleItem(
//                    label = "Auto-Lock",
//                    subtitle = "Require authentication after the app has been in the background. ",
//                    icon = Icons.Rounded.Fingerprint,
//                    checked = biometricEnabled,
//                    onCheckedChange = {
//                        PesaActions.playClick(haptic)
//                        Toast.makeText(
//                            context,
//                            "Feature coming soon! Josiah is still polishing the pulse. ✨",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                )
            }
        }
//
//        // Data Management
//        item {
//            SettingsSection(title = "Data & Storage") {
//                SettingsActionItem(
//                    label = "Sync SMS Transactions",
//                    subtitle = "Manually refresh from inbox",
//                    icon = Icons.Rounded.Sync,
//                    onClick = {
//                        PesaActions.playSuccess(haptic)
//                        Toast.makeText(
//                            context,
//                            "Feature coming soon! Josiah is still polishing the pulse. ✨",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    },
//                    isExpanded = false
//                )
//                SettingsActionItem(
//                    label = "Export Data",
//                    subtitle = "Save transactions as CSV",
//                    icon = Icons.Rounded.IosShare,
//                    onClick = {
//                        PesaActions.playClick(haptic)
//                        Toast.makeText(
//                            context,
//                            "Feature coming soon! Josiah is still polishing the pulse. ✨",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    },
//                    isExpanded = false
//                )
//            }
//        }

        // About
        item {
            var showSummary by remember { mutableStateOf(false) }
            SettingsSection(title = "About") {
                SettingsActionItem(
                    label = "PesaPulse Version",
                    subtitle = appVersion,
                    icon = Icons.Rounded.Info,
                    onClick = {
                        PesaActions.playClick(haptic)
                        showSummary = !showSummary
                    },
                    isExpanded = showSummary
                )
                AnimatedVisibility(visible = showSummary) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "PesaPulse is your intelligent financial heartbeat. By securely transforming M-Pesa SMS alerts into real-time visual insights, we turn your transaction history into a roadmap for wealth.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FeatureSmallBadge("Zero Manual Entry", Icons.Rounded.AutoAwesome)
                            FeatureSmallBadge("100% Offline", Icons.Rounded.Shield)
                        }
                        Text(
                            "By: George Josiah",
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable { PesaActions.playClick(haptic) },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                brush = primaryGradient(),
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureSmallBadge(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            letterSpacing = 1.5.sp
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    label: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsIconBox(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun SettingsActionItem(
    label: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isExpanded: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsIconBox(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Light
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant

        )
    }
}

@Composable
fun SettingsIconBox(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        "v${packageInfo.versionName}"
    } catch (e: Exception) {
        "v1.0.0" // Fallback
    }
}