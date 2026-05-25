package com.example.cashia.models

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.cashia.utility.NotificationHelper
import com.example.cashia.utility.NotificationScheduler
import com.example.cashia.utility.PesaTimeConfiguration

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        PesaTimeConfiguration.initialize(context)
        NotificationHelper.createChannels(context)
        NotificationScheduler.scheduleAll(context)
    }
}