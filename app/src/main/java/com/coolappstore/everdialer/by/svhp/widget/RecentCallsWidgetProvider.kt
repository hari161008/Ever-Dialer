package com.coolappstore.everdialer.by.svhp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.R

class RecentCallsWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_DIAL = "com.coolappstore.everdialer.widget.ACTION_DIAL"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, RecentCallsWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val provider = RecentCallsWidgetProvider()
                provider.onUpdate(context, manager, ids)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_DIAL) {
            val number = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
            val callUri = Uri.parse("tel:$number")
            try {
                context.startActivity(
                    Intent(Intent.ACTION_CALL, callUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            } catch (_: Exception) {
                context.startActivity(
                    Intent(Intent.ACTION_DIAL, callUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_recent_calls)

        // Tapping header opens the app
        val openAppPi = PendingIntent.getActivity(
            context,
            widgetId * 10,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_open_app, openAppPi)
        views.setOnClickPendingIntent(R.id.widget_root, openAppPi)

        // RemoteViews adapter — unique data URI per widget
        val serviceIntent = Intent(context, RecentCallsWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.fromParts("widget", widgetId.toString(), null)
        }
        views.setRemoteAdapter(R.id.widget_call_list, serviceIntent)

        // Template PendingIntent for row taps — MUST use a unique data URI so Android
        // doesn't cache-collide with the open-app intent above
        val dialTemplatePi = PendingIntent.getBroadcast(
            context,
            widgetId * 10 + 1,
            Intent(context, RecentCallsWidgetProvider::class.java).apply {
                action = ACTION_DIAL
                // Unique URI per widget so PendingIntent extras are not shared
                data = Uri.fromParts("dial", widgetId.toString(), null)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_call_list, dialTemplatePi)

        appWidgetManager.updateAppWidget(widgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_call_list)
    }
}
