package com.coolappstore.everdialer.by.svhp.controller.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.ScheduledSmsManager
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class ScheduledSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ScheduledSmsManager.ACTION_TRIGGER -> handleTrigger(context, intent)
            Intent.ACTION_BOOT_COMPLETED, ScheduledSmsManager.ACTION_BOOT -> handleBoot(context)
        }
    }

    private fun handleTrigger(context: Context, intent: Intent) {
        val id = intent.getStringExtra(ScheduledSmsManager.EXTRA_ID) ?: return
        val prefs = try {
            GlobalContext.get().get<PreferenceManager>()
        } catch (_: Throwable) {
            PreferenceManager(context)
        }
        val entry = ScheduledSmsManager.findEntry(prefs, id) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val smsRepo = try {
                    GlobalContext.get().get<ISmsRepository>()
                } catch (_: Throwable) {
                    null
                }
                val success = smsRepo?.sendSms(entry.address, entry.body, entry.subId) ?: false
                ScheduledSmsManager.removeEntry(context, prefs, id)
                Handler(Looper.getMainLooper()).post {
                    if (success) {
                        Toast.makeText(context, "Scheduled SMS sent to ${entry.address}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to send scheduled SMS", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleBoot(context: Context) {
        val prefs = try {
            GlobalContext.get().get<PreferenceManager>()
        } catch (_: Throwable) {
            PreferenceManager(context)
        }
        ScheduledSmsManager.rescheduleAll(context, prefs)
    }
}
