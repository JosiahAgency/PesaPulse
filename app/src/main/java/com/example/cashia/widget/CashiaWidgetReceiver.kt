package com.example.cashia.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class CashiaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CashiaWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }
}