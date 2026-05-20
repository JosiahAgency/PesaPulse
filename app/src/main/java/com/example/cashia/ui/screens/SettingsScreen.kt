package com.example.cashia.ui.screens

import android.app.Application
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
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.navigation.NavController
import com.example.cashia.models.SettingsViewModel
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.BiometricAuthenticator
import com.example.cashia.utility.GoogleAuthHelper
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onFirstDayOfWeekChange: (DayOfWeek) -> Unit,
    navController: NavController,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appVersion = remember { PesaTimeConfiguration.getAppVersion(context) }
    var showDayPicker by remember { mutableStateOf(false) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            (LocalContext.current.applicationContext as Application)
        )
    )

    // Collect all state flows from ViewModel — this is the single source of truth
    val isDailyDigestOn by settingsViewModel.isDailyDigestEnabled.collectAsState()
    val is24Hour by settingsViewModel.use24Hour.collectAsState()
    val linkedGoogleEmail by settingsViewModel.linkedGoogleEmail.collectAsState()

    val biometricAuthenticator = remember { BiometricAuthenticator(context) }
    val googleAuthHelper = remember { GoogleAuthHelper(context) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Day picker dialog
    if (showDayPicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDayPicker = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(32.dp),
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

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        PesaActions.playClick(haptic)
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
                                        imageVector = Icons.Rounded.CheckCircle,
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

    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = PesaTimeConfiguration.reminderHour,
            initialMinute = PesaTimeConfiguration.reminderMinute,
            is24Hour = is24Hour  // Respect the user's time format preference
        )
        androidx.compose.ui.window.Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(
                            onClick = {
                                settingsViewModel.updateReminderTime(
                                    timePickerState.hour,
                                    timePickerState.minute
                                )
                                showTimePicker = false
                            }
                        ) { Text("Confirm") }
                    }
                }
            }
        }
    }

    // Format reminder time display based on current 24h/12h preference
    val reminderTimeText = remember(
        PesaTimeConfiguration.reminderHour,
        PesaTimeConfiguration.reminderMinute,
        is24Hour
    ) {
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        LocalTime.of(PesaTimeConfiguration.reminderHour, PesaTimeConfiguration.reminderMinute)
            .format(DateTimeFormatter.ofPattern(pattern))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
//        // Account
//        item {
//            SettingsSection(title = "Account") {
//                if (linkedGoogleEmail != null) {
//                    LinkedAccountItem(
//                        email = linkedGoogleEmail!!,
//                        onUnlink = {
//                            PesaActions.playClick(haptic)
//                            scope.launch {
//                                googleAuthHelper.signOut()
//                                settingsViewModel.unlinkGoogleAccount()
//                            }
//                        }
//                    )
//                } else {
//                    SettingsActionItem(
//                        label = "Link Google Account",
//                        subtitle = "Enable cloud sync and data backup",
//                        icon = Icons.Rounded.Shield, // Changed from Info for better context
//                        onClick = {
//                            PesaActions.playClick(haptic)
//                            scope.launch {
//                                val email = googleAuthHelper.signInWithGoogle()
//                                if (email != null) {
//                                    settingsViewModel.linkGoogleAccount(email)
//                                }
//                            }
//                        },
//                        isExpanded = false
//                    )
//                }
//            }
//        }

        // Categories
        item {
            SettingsSection(title = "Categories") {
                SettingsActionItem(
                    label = "Manage Categories",
                    subtitle = "Add or update categories",
                    icon = Icons.Rounded.Category,
                    onClick = {
                        PesaActions.playSuccess(haptic)
                        navController.navigate("categories")
                    },
                    isExpanded = false
                )
            }
        }

        // Appearance
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
                SettingsActionItem(
                    label = "Week Starts On",
                    subtitle = PesaTimeConfiguration.firstDayOfWeek.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    icon = Icons.Rounded.CalendarViewWeek,
                    onClick = {
                        PesaActions.playClick(haptic)
                        showDayPicker = true
                    },
                    isExpanded = false
                )
                // ✅ NEW: 24-hour clock toggle
                SettingsToggleItem(
                    label = "24-Hour Clock",
                    subtitle = if (is24Hour) "Time shown in 24-hour format." else "Time shown in 12-hour format.",
                    icon = Icons.Rounded.Schedule,
                    checked = is24Hour,
                    onCheckedChange = {
                        PesaActions.playClick(haptic)
                        settingsViewModel.setUse24Hour(it)
                    }
                )
            }
        }

        // Security
        item {
            SettingsSection(title = "Security") {
                SettingsToggleItem(
                    label = "App Lock",
                    subtitle = "Secure your financial data with biometrics or device PIN.",
                    icon = Icons.Rounded.Fingerprint,
                    // ✅ FIX: Read directly from PesaTimeConfiguration (backed by mutableStateOf,
                    //    so Compose will recompose when it changes — no crash from stale state)
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
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        } else {
                            settingsViewModel.setBiometric(false)
                        }
                    }
                )
            }
        }

        // Notifications
        item {
            SettingsSection("Notifications") {
                SettingsToggleItem(
                    label = "Daily Payment Digest",
                    subtitle = "Get a morning summary of upcoming bills and dues.",
                    icon = Icons.Rounded.NotificationsActive,
                    checked = isDailyDigestOn,
                    onCheckedChange = { enable ->
                        PesaActions.playClick(haptic)
                        // ✅ FIX: setDailyDigestEnabled now safely handles WorkManager internally
                        settingsViewModel.setDailyDigestEnabled(enable)
                        if (enable) {
                            Toast.makeText(
                                context,
                                "Reminders set for $reminderTimeText",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                SettingsActionItem(
                    label = "Reminder Time",
                    subtitle = "Currently set for $reminderTimeText",
                    icon = Icons.Rounded.Schedule,
                    onClick = { showTimePicker = true },
                    isExpanded = false
                )
            }
        }

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
            Column { content() }
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

@Composable
fun LinkedAccountItem(email: String, onUnlink: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Google avatar placeholder with first letter
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = email.first().uppercaseChar().toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Google Account",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                email,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }

        // Unlink button
        Surface(
            onClick = onUnlink,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            border = BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            )
        ) {
            Text(
                "Unlink",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

