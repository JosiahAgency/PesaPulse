package com.example.cashia.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

object WidgetUpdateManager {
    suspend fun refreshCashiaWidgets(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(CashiaWidget::class.java)
        glanceIds.forEach { id ->
            CashiaWidget().update(context, id)
        }
    }
}