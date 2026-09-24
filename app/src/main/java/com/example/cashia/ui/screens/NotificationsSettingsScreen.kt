package com.example.cashia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cashia.models.SettingsViewModel
import com.example.cashia.ui.reusedElements.PesaDialog
import com.example.cashia.ui.reusedElements.SectionCard
import com.example.cashia.ui.theme.screenContentPadding
import com.example.cashia.utility.BatteryOptimizationHelper
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * One home for every reminder/alert Cashia can send. Split out of [SettingsScreen] so the
 * growing list of notification types (digest, monthly summary, upcoming/overdue payments,
 * low balance, monthly target) doesn't overwhelm the main Settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(settingsViewModel: SettingsViewModel) {
    val is24Hour by settingsViewModel.use24Hour.collectAsStateWithLifecycle()
    val isDailyDigestOn by settingsViewModel.isDailyDigestEnabled.collectAsStateWithLifecycle()
    val isMonthlySummaryOn by settingsViewModel.isMonthlySummaryEnabled.collectAsStateWithLifecycle()
    val isUpcomingReminderOn by settingsViewModel.isUpcomingReminderEnabled.collectAsStateWithLifecycle()
    val upcomingReminderDays by settingsViewModel.upcomingReminderDays.collectAsStateWithLifecycle()
    val isLowBalanceOn by settingsViewModel.isLowBalanceAlertEnabled.collectAsStateWithLifecycle()
    val lowBalanceThreshold by settingsViewModel.lowBalanceThreshold.collectAsStateWithLifecycle()
    val isTargetReminderOn by settingsViewModel.isMonthlyTargetReminderEnabled.collectAsStateWithLifecycle()
    val monthlySpendingCap by settingsViewModel.monthlySpendingCap.collectAsStateWithLifecycle()
    val monthlySavingsGoal by settingsViewModel.monthlySavingsGoal.collectAsStateWithLifecycle()
    val reminderTime by settingsViewModel.reminderTime.collectAsStateWithLifecycle()

    var showTimePicker by remember { mutableStateOf(false) }
    var showDaysDialog by remember { mutableStateOf(false) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showTargetsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var isBatteryUnrestricted by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    // Re-check whenever the screen resumes: the user may have just come back from the
    // system battery-optimization dialog/settings, so the banner should clear immediately.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentContext by rememberUpdatedState(context)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryUnrestricted =
                    BatteryOptimizationHelper.isIgnoringBatteryOptimizations(currentContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val reminderTimeText = remember(
        reminderTime, is24Hour
    ) {
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        LocalTime.of(reminderTime.first, reminderTime.second)
            .format(DateTimeFormatter.ofPattern(pattern))
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderTime.first,
            initialMinute = reminderTime.second,
            is24Hour = is24Hour
        )
        PesaDialog(onDismissRequest = { showTimePicker = false }) {
            TimePicker(state = timePickerState)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                TextButton(onClick = {
                    settingsViewModel.updateReminderTime(
                        timePickerState.hour, timePickerState.minute
                    )
                    showTimePicker = false
                }) { Text("Confirm") }
            }
        }
    }

    if (showDaysDialog) {
        var text by remember { mutableStateOf(upcomingReminderDays.toString()) }
        PesaDialog(
            onDismissRequest = { showDaysDialog = false }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Remind Me Before",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "How many days ahead of a due date should upcoming reminders start?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text("Days before") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showDaysDialog = false }) { Text("Cancel") }
                TextButton(onClick = {
                    text.toIntOrNull()
                        ?.let { settingsViewModel.setUpcomingReminderDays(it.coerceIn(0, 30)) }
                    showDaysDialog = false
                }) { Text("Save") }
            }
        }
    }

    if (showThresholdDialog) {
        var text by remember { mutableStateOf(lowBalanceThreshold.toInt().toString()) }
        PesaDialog(
            onDismissRequest = { showThresholdDialog = false }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Low Balance Threshold",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Get alerted when your M-Pesa balance drops to or below this amount.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text("KSh") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showThresholdDialog = false }) { Text("Cancel") }
                TextButton(onClick = {
                    text.toDoubleOrNull()?.let { settingsViewModel.setLowBalanceThreshold(it) }
                    showThresholdDialog = false
                }) { Text("Save") }
            }
        }
    }

    if (showTargetsDialog) {
        var capText by remember { mutableStateOf(monthlySpendingCap?.toString() ?: "") }
        var goalText by remember { mutableStateOf(monthlySavingsGoal?.toString() ?: "") }
        PesaDialog(
            onDismissRequest = { showTargetsDialog = false }, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Monthly Target - ${java.time.YearMonth.now()}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Shared with Home and Insights for this month. Leave a field blank to clear it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = capText,
                onValueChange = { capText = it },
                label = { Text("Spending cap (KSh)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                label = { Text("Savings goal (KSh)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { showTargetsDialog = false }) { Text("Cancel") }
                TextButton(onClick = {
                    settingsViewModel.setMonthlyTargets(
                        capText.toDoubleOrNull(), goalText.toDoubleOrNull()
                    )
                    showTargetsDialog = false
                }, enabled = listOf(capText, goalText).all { text ->
                    text.isBlank() || text.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true
                }) { Text("Save") }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = screenContentPadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            if (BatteryOptimizationHelper.isSupported && !isBatteryUnrestricted) {
                SectionCard("Reliability") {
                    SettingsActionItem(
                        label = "Fix Missed Reminders",
                        subtitle = "Battery optimization is ON for Cashia — this is the #1 reason " + "reminders arrive late or not at all. Tap to allow background delivery.",
                        icon = Icons.Rounded.BatteryAlert,
                        onClick = {
                            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(
                                context
                            )
                        },
                        isExpanded = false
                    )
                }
            }
        }

        item {
            SectionCard("Daily & Monthly Reports") {
                SettingsToggleItem(
                    label = "Daily Payment Digest",
                    subtitle = "Evening recap of the day's spending & income.",
                    icon = Icons.Rounded.NotificationsActive,
                    checked = isDailyDigestOn,
                    onCheckedChange = settingsViewModel::setDailyDigestEnabled
                )
                SettingsActionItem(
                    label = "Reminder Time",
                    subtitle = "Currently set for $reminderTimeText",
                    icon = Icons.Rounded.Schedule,
                    onClick = { showTimePicker = true },
                    isExpanded = false
                )
                SettingsToggleItem(
                    label = "Monthly Summary",
                    subtitle = "Report for the month that just ended.",
                    icon = Icons.Rounded.NotificationsActive,
                    checked = isMonthlySummaryOn,
                    onCheckedChange = settingsViewModel::setMonthlySummaryEnabled
                )
            }
        }

        item {
            SectionCard("Upcoming & Overdue Payments") {
                SettingsToggleItem(
                    label = "Upcoming Payment Reminders",
                    subtitle = "Alerts as a scheduled bill or income nears its due date.",
                    icon = Icons.Rounded.Schedule,
                    checked = isUpcomingReminderOn,
                    onCheckedChange = settingsViewModel::setUpcomingReminderEnabled
                )
                SettingsActionItem(
                    label = "Remind Me Before",
                    subtitle = "$upcomingReminderDays day(s) ahead of the due date",
                    icon = Icons.Rounded.Schedule,
                    onClick = { showDaysDialog = true },
                    isExpanded = false
                )
                SettingsActionItem(
                    label = "Overdue Escalation",
                    subtitle = "Once something is overdue, you're re-alerted every 7 days until it's resolved.",
                    icon = Icons.Rounded.EventBusy,
                    onClick = {},
                    isExpanded = false
                )
            }
        }

        item {
            SectionCard("Low Balance Alerts") {
                SettingsToggleItem(
                    label = "Low Balance Alerts",
                    subtitle = "Warn me when my M-Pesa balance drops too low.",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    checked = isLowBalanceOn,
                    onCheckedChange = settingsViewModel::setLowBalanceAlertEnabled
                )
                SettingsActionItem(
                    label = "Alert Threshold",
                    subtitle = "KSh ${lowBalanceThreshold.toInt()}",
                    icon = Icons.Rounded.AccountBalanceWallet,
                    onClick = { showThresholdDialog = true },
                    isExpanded = false
                )
            }
        }

        item {
            SectionCard("Monthly Target") {
                SettingsToggleItem(
                    label = "Target Reminder",
                    subtitle = "Nag me weekly until I set a monthly target.",
                    icon = Icons.Rounded.Flag,
                    checked = isTargetReminderOn,
                    onCheckedChange = settingsViewModel::setMonthlyTargetReminderEnabled
                )
                SettingsActionItem(
                    label = "Spending Cap & Savings Goal",
                    subtitle = when {
                        monthlySpendingCap == null && monthlySavingsGoal == null -> "Not set yet"
                        else -> buildString {
                            monthlySpendingCap?.let { append("Cap KSh ${it.toInt()}") }
                            if (monthlySpendingCap != null && monthlySavingsGoal != null) append(" · ")
                            monthlySavingsGoal?.let { append("Goal KSh ${it.toInt()}") }
                        }
                    },
                    icon = Icons.Rounded.Savings,
                    onClick = { showTargetsDialog = true },
                    isExpanded = false
                )
            }
        }
    }
}
