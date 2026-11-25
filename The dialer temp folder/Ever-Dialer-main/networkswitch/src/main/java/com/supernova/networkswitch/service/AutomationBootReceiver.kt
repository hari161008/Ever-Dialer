package com.supernova.networkswitch.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the automation background service after a reboot, if it should be running (master
 * switch on and at least one automation rule enabled) — mirroring how [AutomationServiceController]
 * decides during normal app usage.
 */
class AutomationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        AutomationServiceController.sync(context.applicationContext)
    }
}
