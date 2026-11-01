package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutoBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        when (action) {
            AutoBackupManager.ACTION_AUTO_BACKUP_TRIGGER -> {
                AutoBackupManager.performAutoBackup(context)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                AutoBackupManager.scheduleAutoBackup(context)
            }
        }
    }
}
