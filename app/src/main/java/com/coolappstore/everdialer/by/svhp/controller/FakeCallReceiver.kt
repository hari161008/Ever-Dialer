package com.coolappstore.everdialer.by.svhp.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import org.koin.core.context.GlobalContext

/**
 * Receives fake-call alarms and places a genuine Telecom incoming call via
 * [FakeCallConnectionService]. From this point on, the call is a real
 * [android.telecom.Call] handled by the app's existing [CallService] / [CallActivity] —
 * the same incoming-call screen, system call notification, and ongoing-call screen used
 * for real calls, with no custom UI involved anywhere.
 */
class FakeCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            FakeCallManager.ACTION_TRIGGER -> handleTrigger(context, intent)
            Intent.ACTION_BOOT_COMPLETED, FakeCallManager.ACTION_BOOT -> handleBoot(context)
        }
    }

    private fun handleTrigger(context: Context, intent: Intent) {
        val id = intent.getStringExtra(FakeCallManager.EXTRA_ID) ?: return
        val prefs = GlobalContext.get().get<PreferenceManager>()
        val entry = FakeCallManager.findEntry(prefs, id) ?: return
        if (!entry.enabled) return

        FakeCallConnectionService.placeFakeIncomingCall(
            context = context,
            id = entry.id,
            displayName = entry.displayName,
            number = entry.phoneNumber
        )

        // Repeating fake calls re-arm for the next matching day; one-time fake calls (no days
        // selected) have served their purpose, so flip their toggle off in Settings.
        if (entry.days.isNotEmpty()) {
            FakeCallManager.rescheduleNext(context, prefs, id)
        } else {
            FakeCallManager.setEnabled(context, prefs, id, false)
        }
    }

    private fun handleBoot(context: Context) {
        FakeCallConnectionService.ensureRegistered(context)
        val prefs = GlobalContext.get().get<PreferenceManager>()
        FakeCallManager.rescheduleAll(context, prefs)
    }
}
