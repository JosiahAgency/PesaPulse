package com.example.cashia.models

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationRuntimeStore
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.NotificationStream
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class NotificationStreamDiagnosticsUi(
    val label: String,
    val enabled: Boolean,
    val workState: String,
    val nextRunAt: String,
    val nextRunIn: String,
    val lastAttemptAt: String,
    val lastSuccessAt: String,
    val lastErrorAt: String,
    val lastErrorMessage: String,
    val channelHealthy: Boolean,
)

data class NotificationDiagnosticsUi(
    val permissionGranted: Boolean = true,
    val lastRefreshedAt: String = "N/A",
    val streams: List<NotificationStreamDiagnosticsUi> = emptyList(),
)

data class NotificationScheduleUi(
    val dailyTimeText: String = "N/A",
    val monthlyDayOfMonth: Int = 1,
    val monthlyTimeText: String = "N/A",
    val upcomingTimeText: String = "N/A",
)

@SuppressLint("StaticFieldLeak")
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        fun provideFactory(app: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.AndroidViewModelFactory(app) {}
        }
    }

    private val context = getApplication<Application>().applicationContext

    val darkTheme = snapshotFlow { PesaTimeConfiguration.isDarkTheme }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.isDarkTheme)

    val use24Hour = snapshotFlow { PesaTimeConfiguration.use24HrsFormat }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PesaTimeConfiguration.use24HrsFormat)

    private val _isDailyDigestEnabled =
        MutableStateFlow(PesaTimeConfiguration.isDailyDigestEnabled)
    val isDailyDigestEnabled: StateFlow<Boolean> = _isDailyDigestEnabled.asStateFlow()

    private val _isMonthlySummaryEnabled =
        MutableStateFlow(PesaTimeConfiguration.isMonthlySummaryEnabled)
    val isMonthlySummaryEnabled: StateFlow<Boolean> = _isMonthlySummaryEnabled.asStateFlow()

    private val _isUpcomingReminderEnabled =
        MutableStateFlow(PesaTimeConfiguration.isUpcomingReminderEnabled)
    val isUpcomingReminderEnabled: StateFlow<Boolean> = _isUpcomingReminderEnabled.asStateFlow()

    private val _upcomingReminderDays =
        MutableStateFlow(PesaTimeConfiguration.upcomingReminderDaysBefore)

    private val _notificationDiagnostics = MutableStateFlow(NotificationDiagnosticsUi())
    val notificationDiagnostics: StateFlow<NotificationDiagnosticsUi> =
        _notificationDiagnostics.asStateFlow()
    private val _notificationSchedule = MutableStateFlow(NotificationScheduleUi())
    val notificationSchedule: StateFlow<NotificationScheduleUi> =
        _notificationSchedule.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            NotificationScheduler.ensureHealthySchedules(context)
            refreshNotificationSettings()
            refreshNotificationSchedule()
            refreshNotificationDiagnostics()
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        PesaTimeConfiguration.saveTheme(enabled)
    }

    fun setUse24Hour(enabled: Boolean) {
        PesaTimeConfiguration.saveTimeFormat(enabled)
        refreshNotificationSchedule()
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        PesaTimeConfiguration.saveFirstDayOfWeek(day)
    }

    fun setBiometric(enabled: Boolean) {
        viewModelScope.launch {
            PesaTimeConfiguration.saveBiometric(enabled)
        }
    }

    fun setDailyDigestEnabled(enabled: Boolean) {
        _isDailyDigestEnabled.value = enabled
        PesaTimeConfiguration.saveDailyDigest(enabled)
        if (enabled) NotificationScheduler.scheduleDailyDigest(context)
        else WorkManager.getInstance(context).cancelUniqueWork(DailyDigestWorker.WORK_NAME)
        logWorkState(DailyDigestWorker.WORK_NAME)
        refreshNotificationDiagnostics()
    }

    fun setMonthlySummaryEnabled(enabled: Boolean) {
        _isMonthlySummaryEnabled.value = enabled
        PesaTimeConfiguration.saveMonthlySummary(enabled)
        if (enabled) NotificationScheduler.scheduleMonthly(context)
        else WorkManager.getInstance(context).cancelUniqueWork(MonthlySummaryWorker.WORK_NAME)
        logWorkState(MonthlySummaryWorker.WORK_NAME)
        refreshNotificationDiagnostics()
    }

    fun setUpcomingReminderEnabled(enabled: Boolean) {
        _isUpcomingReminderEnabled.value = enabled
        PesaTimeConfiguration.saveUpcomingReminder(enabled)
        if (enabled) NotificationScheduler.scheduleUpcomingReminders(context)
        else WorkManager.getInstance(context).cancelUniqueWork(UpcomingReminderWorker.WORK_NAME)
        logWorkState(UpcomingReminderWorker.WORK_NAME)
        refreshNotificationDiagnostics()
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        PesaTimeConfiguration.saveDailyDigestTime(hour, minute)
        if (PesaTimeConfiguration.isDailyDigestEnabled) {
            NotificationScheduler.scheduleDailyDigest(context)
        }
        logWorkState(DailyDigestWorker.WORK_NAME)
        refreshNotificationSchedule()
        refreshNotificationDiagnostics()
    }

    fun updateMonthlySummarySchedule(dayOfMonth: Int, hour: Int, minute: Int) {
        PesaTimeConfiguration.saveMonthlySummarySchedule(dayOfMonth, hour, minute)
        if (PesaTimeConfiguration.isMonthlySummaryEnabled) {
            NotificationScheduler.scheduleMonthly(context)
        }
        logWorkState(MonthlySummaryWorker.WORK_NAME)
        refreshNotificationSchedule()
        refreshNotificationDiagnostics()
    }

    fun updateUpcomingReminderTime(hour: Int, minute: Int) {
        PesaTimeConfiguration.saveUpcomingReminderTime(hour, minute)
        if (PesaTimeConfiguration.isUpcomingReminderEnabled) {
            NotificationScheduler.scheduleUpcomingReminders(context)
        }
        logWorkState(UpcomingReminderWorker.WORK_NAME)
        refreshNotificationSchedule()
        refreshNotificationDiagnostics()
    }

    fun refreshNotificationSettings() {
        _isDailyDigestEnabled.value = PesaTimeConfiguration.isDailyDigestEnabled
        _isMonthlySummaryEnabled.value = PesaTimeConfiguration.isMonthlySummaryEnabled
        _isUpcomingReminderEnabled.value = PesaTimeConfiguration.isUpcomingReminderEnabled
        _upcomingReminderDays.value = PesaTimeConfiguration.upcomingReminderDaysBefore
    }

    fun refreshNotificationSchedule() {
        _notificationSchedule.value = NotificationScheduleUi(
            dailyTimeText = formatTime(
                PesaTimeConfiguration.dailyDigestHour,
                PesaTimeConfiguration.dailyDigestMinute
            ),
            monthlyDayOfMonth = PesaTimeConfiguration.monthlySummaryDayOfMonth,
            monthlyTimeText = formatTime(
                PesaTimeConfiguration.monthlySummaryHour,
                PesaTimeConfiguration.monthlySummaryMinute
            ),
            upcomingTimeText = formatTime(
                PesaTimeConfiguration.upcomingReminderHour,
                PesaTimeConfiguration.upcomingReminderMinute
            ),
        )
    }

    fun repairNotificationSchedules() {
        viewModelScope.launch(Dispatchers.IO) {
            NotificationScheduler.ensureHealthySchedules(context)
            refreshNotificationDiagnostics()
        }
    }

    fun refreshNotificationDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            val permissionGranted = isNotificationPermissionGranted()
            _notificationDiagnostics.value = NotificationDiagnosticsUi(
                permissionGranted = permissionGranted,
                lastRefreshedAt = formatEpoch(System.currentTimeMillis()),
                streams = listOf(
                    buildStreamDiagnostics(
                        stream = NotificationStream.DAILY,
                        label = "Daily Digest",
                        enabled = _isDailyDigestEnabled.value,
                        workName = DailyDigestWorker.WORK_NAME,
                        channelId = NotificationHelper.CH_DAILY
                    ),
                    buildStreamDiagnostics(
                        stream = NotificationStream.MONTHLY,
                        label = "Monthly Summary",
                        enabled = _isMonthlySummaryEnabled.value,
                        workName = MonthlySummaryWorker.WORK_NAME,
                        channelId = NotificationHelper.CH_PERIODIC
                    ),
                    buildStreamDiagnostics(
                        stream = NotificationStream.UPCOMING,
                        label = "Upcoming Reminders",
                        enabled = _isUpcomingReminderEnabled.value,
                        workName = UpcomingReminderWorker.WORK_NAME,
                        channelId = NotificationHelper.CH_UPCOMING
                    )
                )
            )
        }
    }

    private fun buildStreamDiagnostics(
        stream: NotificationStream,
        label: String,
        enabled: Boolean,
        workName: String,
        channelId: String
    ): NotificationStreamDiagnosticsUi {
        val status = NotificationRuntimeStore.getStatus(context, stream)
        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName).get()
        val workStates = if (infos.isEmpty()) "NONE"
        else infos.joinToString(" | ") { it.state.name }
        return NotificationStreamDiagnosticsUi(
            label = label,
            enabled = enabled,
            workState = workStates,
            nextRunAt = formatEpoch(status.nextRunAt),
            nextRunIn = formatCountdown(status.nextRunAt),
            lastAttemptAt = formatEpoch(status.lastAttemptAt),
            lastSuccessAt = formatEpoch(status.lastSuccessAt),
            lastErrorAt = formatEpoch(status.lastErrorAt),
            lastErrorMessage = status.lastErrorMessage ?: "None",
            channelHealthy = isChannelHealthy(channelId)
        )
    }

    private fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isChannelHealthy(channelId: String): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.getNotificationChannel(channelId) != null
    }

    private fun logWorkState(workName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(workName).get()
            val states = infos.joinToString { it.state.name }
            Log.i("SettingsNotifications", "work=$workName states=[$states]")
        }
    }

    private fun formatEpoch(value: Long): String {
        if (value <= 0L) return "N/A"
        return DateTimeFormatter.ofPattern("dd MMM HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(value))
    }

    private fun formatCountdown(epochMillis: Long): String {
        if (epochMillis <= 0L) return "N/A"
        val now = System.currentTimeMillis()
        val diff = epochMillis - now
        if (diff <= 0) return "due"
        val d = Duration.ofMillis(diff)
        val hours = d.toHours()
        val minutes = d.minusHours(hours).toMinutes()
        return "${hours}h ${minutes}m"
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val pattern = if (PesaTimeConfiguration.use24HrsFormat) "HH:mm" else "hh:mm a"
        return LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .format(DateTimeFormatter.ofPattern(pattern))
    }
}
