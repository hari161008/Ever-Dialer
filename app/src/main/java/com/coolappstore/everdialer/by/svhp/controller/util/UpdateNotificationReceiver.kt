package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = PreferenceManager(context)
        if (!prefs.isNotifyUpdatesEnabled()) {
            UpdateNotificationManager.cancelUpdateCheck(context)
            return
        }

        val action = intent?.action ?: return
        when (action) {
            UpdateNotificationManager.ACTION_UPDATE_CHECK_TRIGGER -> {
                UpdateNotificationManager.checkUpdatesAndNotify(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                UpdateNotificationManager.scheduleUpdateCheck(context)
            }
        }
    }
}
