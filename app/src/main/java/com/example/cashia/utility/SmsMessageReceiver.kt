package com.example.cashia.utility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.cashia.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsMessageReceiver : BroadcastReceiver() {
    // An application-scoped lifecycle fallback to handle asynchronous parsing safe from execution cuts
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // Extract the raw SMS PDUs natively from the intent wrapper
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        // Fast filtering: Verify if any message segment originated from our target alphanumeric address
        val hasMpesaSms = messages.any {
            it.displayOriginatingAddress?.equals(
                "MPESA",
                ignoreCase = true
            ) == true
        }
        if (!hasMpesaSms) return

        // Take a PendingResult to signal the OS that we are doing quick async work
        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                Log.d(
                    "SmsMessageReceiver",
                    "M-PESA SMS event intercepted. Initializing parallel engine."
                )

                val database = AppDatabase.getDatabase(context.applicationContext)

                // Fire your parallel execution pipeline
                syncTransactions(context.applicationContext, database)

            } catch (e: Exception) {
                Log.e(
                    "SmsMessageReceiver",
                    "Pipeline synchronization fault: ${e.localizedMessage}",
                    e
                )
            } finally {
                // Must always finish the asynchronous lifecycle back to the operating system pool
                pendingResult.finish()
            }
        }
    }
}