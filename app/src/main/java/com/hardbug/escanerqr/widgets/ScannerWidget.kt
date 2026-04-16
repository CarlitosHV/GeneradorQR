package com.hardbug.escanerqr.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hardbug.escanerqr.HomeActivity
import com.hardbug.escanerqr.R

class ScannerWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int) {

        val views = RemoteViews(context.packageName, R.layout.widget_scanner)

        val scanIntent = Intent(context, HomeActivity::class.java).apply {
            action = "ACTION_SCAN"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val scanPendingIntent = PendingIntent.getActivity(
            context, 0, scanIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_scan, scanPendingIntent)

        val createIntent = Intent(context, HomeActivity::class.java).apply {
            action = "ACTION_CREATE"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val createPendingIntent = PendingIntent.getActivity(
            context, 1, createIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_create, createPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}