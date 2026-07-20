package com.supernova.networkswitch.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.supernova.networkswitch.util.AutomationSwitchStore
import com.supernova.networkswitch.util.MasterSwitchStore

/**
 * Starts or stops [AutomationService] so that it is running if and only if the master 4G/5G
 * Switcher toggle is on AND at least one of the three automation rules is enabled. Call
 * [sync] after any change to the master switch or to any automation rule's enabled flag.
 */
object AutomationServiceController {

    fun sync(context: Context) {
        MasterSwitchStore.init(context)
        AutomationSwitchStore.init(context)

        val shouldRun = MasterSwitchStore.isEnabled() && AutomationSwitchStore.isAnyEnabled(context)

        if (shouldRun) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, AutomationService::class.java))
            } catch (e: Exception) {
                // Background start restrictions on some OEMs/states — the service will be
                // (re)started next time the app is foregrounded and sync() runs again.
            }
        } else {
            context.stopService(Intent(context, AutomationService::class.java))
        }
    }
}
