package com.coolappstore.everdialer.by.svhp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.controller.util.ContactShortcutUtils

class DirectCallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val edit = sp.edit()
        for (widgetId in appWidgetIds) {
            edit.remove(KEY_NAME_PREFIX + widgetId)
            edit.remove(KEY_NUMBER_PREFIX + widgetId)
            edit.remove(KEY_PHOTO_PREFIX + widgetId)
            edit.remove(KEY_CONTACT_ID_PREFIX + widgetId)
        }
        edit.apply()
    }

    companion object {
        const val PREFS_NAME = "direct_call_widget_prefs"
        const val KEY_NAME_PREFIX = "name_"
        const val KEY_NUMBER_PREFIX = "number_"
        const val KEY_PHOTO_PREFIX = "photo_"
        const val KEY_CONTACT_ID_PREFIX = "contact_id_"

        fun saveWidgetData(
            context: Context,
            widgetId: Int,
            name: String,
            number: String,
            photoUri: String?,
            contactId: String
        ) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putString(KEY_NAME_PREFIX + widgetId, name)
                .putString(KEY_NUMBER_PREFIX + widgetId, number)
                .putString(KEY_PHOTO_PREFIX + widgetId, photoUri)
                .putString(KEY_CONTACT_ID_PREFIX + widgetId, contactId)
                .apply()
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val name = sp.getString(KEY_NAME_PREFIX + widgetId, null) ?: return
            val number = sp.getString(KEY_NUMBER_PREFIX + widgetId, "") ?: ""
            val photoUri = sp.getString(KEY_PHOTO_PREFIX + widgetId, null)
            val contactId = sp.getString(KEY_CONTACT_ID_PREFIX + widgetId, "") ?: ""

            updateWidget(context, appWidgetManager, widgetId, name, number, photoUri, contactId)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            name: String,
            number: String,
            photoUri: String?,
            contactId: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_direct_call)
            views.setTextViewText(R.id.widget_name, name)

            val avatarBitmap = ContactShortcutUtils.buildIconBitmap(context, name, photoUri)
            views.setImageViewBitmap(R.id.widget_avatar, avatarBitmap)

            val dialIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
                setClass(context, MainActivity::class.java)
                putExtra("contact_id", contactId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pi = PendingIntent.getActivity(
                context,
                widgetId,
                dialIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
