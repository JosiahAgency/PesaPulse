package com.example.cashia

import android.app.Application
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Guarantees notification setup runs for *every* process start, not just when
 * [MainActivity] is opened or the device just booted.
 *
 * WorkManager, [com.example.cashia.utility.SmsMessageReceiver], and other background
 * components can revive this app's process with no Activity ever created. Previously
 * [PesaTimeConfiguration] was only initialized from `MainActivity.onCreate`/`BootReceiver`,
 * so a fresh process crashed the moment a background Worker tried to read a setting
 * (`lateinit property prefs has not been initialized`) — silently killing the
 * self-chaining reminder loop until the user next opened the app. `Application.onCreate()`
 * is guaranteed to run before any other component in the process, so initializing here
 * fixes reminders "going quiet" after a day of not opening the app.
 */
class CashiaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        PesaTimeConfiguration.initialize(this)
        NotificationHelper.createChannels(this)
        NotificationScheduler.scheduleAll(this)
        warmUpDatabase()
    }

    /**
     * Opens the Room database and runs a trivial query on a background thread right at
     * process start. Without this, the SQLite file-open / WAL setup / first query-plan
     * compile cost is paid lazily on whichever screen the user happens to open first
     * (Calendar, Analytics, Export, Summary each build their own DAO/ViewModel on first
     * navigation) — surfacing as a one-time stutter the first time each screen is visited
     * in a session. Paying that cost here, before any UI is shown, removes it from the
     * navigation path entirely.
     */
    private fun warmUpDatabase() {
        applicationScope.launch {
            runCatching {
                AppDatabase.getDatabase(this@CashiaApplication)
                    .transactionDao()
                    .getLastTransactionTimestamp()
            }
        }
    }
}

