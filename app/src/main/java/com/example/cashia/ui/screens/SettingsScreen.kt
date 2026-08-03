package com.example.cashia.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.cashia.models.SettingsViewModel
import com.example.cashia.ui.reusedElements.PesaDialog
import com.example.cashia.ui.theme.primaryGradient
import com.example.cashia.utility.BiometricAuthenticator
import com.example.cashia.utility.PesaActions
import com.example.cashia.utility.PesaTimeConfiguration
import java.time.DayOfWeek

private enum class NotificationTimeTarget { DAILY, MONTHLY, UPCOMING }

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
    val appVersion = remember { PesaTimeConfiguration.getAppVersion(context) }
    var showDayPicker by remember { mutableStateOf(false) }
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(
            (LocalContext.current.applicationContext as Application)
        )
    )

    // Collect all state flows from ViewModel — this is the single source of truth
    val isDailyDigestOn by settingsViewModel.isDailyDigestEnabled.collectAsStateWithLifecycle()
    val isMonthlySummaryOn by settingsViewModel.isMonthlySummaryEnabled.collectAsStateWithLifecycle()
    val isUpcomingReminderOn by settingsViewModel.isUpcomingReminderEnabled.collectAsStateWithLifecycle()
    val notificationDiagnostics by settingsViewModel.notificationDiagnostics.collectAsStateWithLifecycle()
    val notificationSchedule by settingsViewModel.notificationSchedule.collectAsStateWithLifecycle()
    val is24Hour by settingsViewModel.use24Hour.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        settingsViewModel.refreshNotificationSettings()
        settingsViewModel.refreshNotificationSchedule()
        settingsViewModel.refreshNotificationDiagnostics()
    }

    val biometricAuthenticator = remember { BiometricAuthenticator(context) }
    var timePickerTarget by remember { mutableStateOf<NotificationTimeTarget?>(null) }
    var showMonthlyDayPicker by remember { mutableStateOf(false) }
    var monthlyDayDraft by remember { mutableStateOf(PesaTimeConfiguration.monthlySummaryDayOfMonth) }

    // Day picker dialog
    if (showDayPicker) {
        PesaDialog(
            onDismissRequest = { showDayPicker = false },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
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

    // Notification time picker dialog
    if (timePickerTarget != null) {
        val initialHour = when (timePickerTarget) {
            NotificationTimeTarget.DAILY -> PesaTimeConfiguration.dailyDigestHour
            NotificationTimeTarget.MONTHLY -> PesaTimeConfiguration.monthlySummaryHour
            NotificationTimeTarget.UPCOMING -> PesaTimeConfiguration.upcomingReminderHour
            else -> {
                0
            }
        }
        val initialMinute = when (timePickerTarget) {
            NotificationTimeTarget.DAILY -> PesaTimeConfiguration.dailyDigestMinute
            NotificationTimeTarget.MONTHLY -> PesaTimeConfiguration.monthlySummaryMinute
            NotificationTimeTarget.UPCOMING -> PesaTimeConfiguration.upcomingReminderMinute
            null -> 0
        }
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = is24Hour  // Respect the user's time format preference
        )
        PesaDialog(onDismissRequest = { timePickerTarget = null }) {
            TimePicker(state = timePickerState)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { timePickerTarget = null }) { Text("Cancel") }
                TextButton(
                    onClick = {
                        when (timePickerTarget) {
                            NotificationTimeTarget.DAILY -> settingsViewModel.updateReminderTime(
                                timePickerState.hour,
                                timePickerState.minute
                            )

                            NotificationTimeTarget.MONTHLY -> settingsViewModel.updateMonthlySummarySchedule(
                                dayOfMonth = PesaTimeConfiguration.monthlySummaryDayOfMonth,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute
                            )

                            NotificationTimeTarget.UPCOMING -> settingsViewModel.updateUpcomingReminderTime(
                                timePickerState.hour,
                                timePickerState.minute
                            )

                            null -> Unit
                        }
                        timePickerTarget = null
                    }
                ) { Text("Confirm") }
            }
        }
    }

    if (showMonthlyDayPicker) {
        PesaDialog(onDismissRequest = { showMonthlyDayPicker = false }) {
            Text(
                "Monthly Summary Day",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choose which day of month to send your summary.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items((1..28).toList()) { day ->
                    val selected = day == monthlyDayDraft
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .clickable { monthlyDayDraft = day }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Day $day",
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showMonthlyDayPicker = false }) { Text("Cancel") }
                TextButton(onClick = {
                    settingsViewModel.updateMonthlySummarySchedule(
                        dayOfMonth = monthlyDayDraft,
                        hour = PesaTimeConfiguration.monthlySummaryHour,
                        minute = PesaTimeConfiguration.monthlySummaryMinute
                    )
                    showMonthlyDayPicker = false
                }) { Text("Confirm") }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 115.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
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
                NotificationStreamCard {
                    SettingsToggleItem(
                        label = "Daily Payment Digest",
                        subtitle = "Get a morning summary of upcoming bills and dues.",
                        icon = Icons.Rounded.NotificationsActive,
                        checked = isDailyDigestOn,
                        onCheckedChange = { enable ->
                            PesaActions.playClick(haptic)
                            settingsViewModel.setDailyDigestEnabled(enable)
                            if (enable) {
                                Toast.makeText(
                                    context,
                                    "Daily digest set for ${notificationSchedule.dailyTimeText}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    SettingsActionItem(
                        label = "Digest Time",
                        subtitle = "Daily at ${notificationSchedule.dailyTimeText}",
                        icon = Icons.Rounded.Schedule,
                        onClick = {
                            PesaActions.playClick(haptic)
                            timePickerTarget = NotificationTimeTarget.DAILY
                        },
                        isExpanded = false
                    )
                }
                NotificationStreamCard {
                    SettingsToggleItem(
                        label = "Monthly Summary",
                        subtitle = "Get a recap on your selected date and time.",
                        icon = Icons.Rounded.NotificationsActive,
                        checked = isMonthlySummaryOn,
                        onCheckedChange = {
                            PesaActions.playClick(haptic)
                            settingsViewModel.setMonthlySummaryEnabled(it)
                        }
                    )
                    SettingsActionItem(
                        label = "Summary Date",
                        subtitle = "Day ${notificationSchedule.monthlyDayOfMonth} of each month",
                        icon = Icons.Rounded.CalendarViewWeek,
                        onClick = {
                            PesaActions.playClick(haptic)
                            monthlyDayDraft = notificationSchedule.monthlyDayOfMonth
                            showMonthlyDayPicker = true
                        },
                        isExpanded = false
                    )
                    SettingsActionItem(
                        label = "Summary Time",
                        subtitle = "At ${notificationSchedule.monthlyTimeText}",
                        icon = Icons.Rounded.Schedule,
                        onClick = {
                            PesaActions.playClick(haptic)
                            timePickerTarget = NotificationTimeTarget.MONTHLY
                        },
                        isExpanded = false
                    )
                }
                NotificationStreamCard {
                    SettingsToggleItem(
                        label = "Upcoming Reminders",
                        subtitle = "Get alerts for upcoming and overdue commitments.",
                        icon = Icons.Rounded.NotificationsActive,
                        checked = isUpcomingReminderOn,
                        onCheckedChange = {
                            PesaActions.playClick(haptic)
                            settingsViewModel.setUpcomingReminderEnabled(it)
                        }
                    )
                    SettingsActionItem(
                        label = "Reminder Time",
                        subtitle = "Daily at ${notificationSchedule.upcomingTimeText}",
                        icon = Icons.Rounded.Schedule,
                        onClick = {
                            PesaActions.playClick(haptic)
                            timePickerTarget = NotificationTimeTarget.UPCOMING
                        },
                        isExpanded = false
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Diagnostics",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Last refresh: ${notificationDiagnostics.lastRefreshedAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Permission: ${if (notificationDiagnostics.permissionGranted) "Granted" else "Denied"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (notificationDiagnostics.permissionGranted)
                            MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
                    )
                    notificationDiagnostics.streams.forEach { stream ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    "${stream.label} • ${if (stream.enabled) "Enabled" else "Disabled"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Work: ${stream.workState}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Next: ${stream.nextRunAt} (${stream.nextRunIn})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Last attempt: ${stream.lastAttemptAt}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Last success: ${stream.lastSuccessAt}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Last error: ${stream.lastErrorAt}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Error detail: ${stream.lastErrorMessage}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "Channel: ${if (stream.channelHealthy) "Healthy" else "Missing"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (stream.channelHealthy)
                                        MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = {
                            PesaActions.playClick(haptic)
                            settingsViewModel.refreshNotificationDiagnostics()
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Refresh diagnostics") }
                    TextButton(
                        onClick = {
                            PesaActions.playClick(haptic)
                            settingsViewModel.repairNotificationSchedules()
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Repair schedules now") }
                }
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
fun NotificationStreamCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    ) {
        Column { content() }
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
