package com.example.cashia.utility

import android.content.Context
import com.example.cashia.AppDatabase
import java.time.LocalDate

/**
 * Event-driven low-balance alert: checked right after each SMS sync (whenever new
 * transactions are parsed), not on a fixed schedule — a balance can only change when
 * a transaction happens, so there is no point polling for it on a timer.
 */
object BalanceAlertService {
    private const val ID = 4001

    suspend fun checkAndNotify(context: Context, db: AppDatabase) {
        if (!PesaTimeConfiguration.isLowBalanceAlertEnabled) return

        val balance = db.transactionDao().getLatestBalance() ?: return
        val threshold = PesaTimeConfiguration.lowBalanceThreshold

        if (balance > threshold) {
            // Balance recovered above the threshold — clear so a future dip alerts again.
            PesaTimeConfiguration.lastLowBalanceAlertDate = null
            return
        }

        // Avoid re-notifying on every single sync while the balance stays low; once per day is enough.
        val today = LocalDate.now().toString()
        if (PesaTimeConfiguration.lastLowBalanceAlertDate == today) return
        PesaTimeConfiguration.lastLowBalanceAlertDate = today

        NotificationHelper.post(
            context,
            id = ID,
            channelId = NotificationHelper.CH_LOW_BALANCE,
            title = "⚠️ Low Balance",
            body = "Your M-Pesa balance is ${NotificationHelper.ksh(balance)}, at or below your " +
                "${NotificationHelper.ksh(threshold)} alert threshold."
        )
    }
}
