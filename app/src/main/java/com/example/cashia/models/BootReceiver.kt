package com.example.cashia.models

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        PesaTimeConfiguration.initialize(context)
        NotificationHelper.createChannels(context)
        NotificationScheduler.scheduleAll(context)
        Log.i("BootReceiver", "Rescheduled notification work after action=$action")
    }
}