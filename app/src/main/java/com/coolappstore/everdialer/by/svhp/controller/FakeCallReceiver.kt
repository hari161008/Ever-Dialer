package com.coolappstore.everdialer.by.svhp.controller

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.controller.util.FakeCallManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.view.screen.FakeIncomingCallActivity
import org.koin.core.context.GlobalContext

/**
 * Receives fake-call alarms and shows an incoming-call style full-screen notification.
 * Also handles BOOT_COMPLETED to re-arm alarms after a reboot.
 */
class FakeCallReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID   = "fake_call_channel"
        private const val CHANNEL_NAME = "Fake Incoming Call"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            FakeCallManager.ACTION_TRIGGER -> handleTrigger(context, intent)
            Intent.ACTION_BOOT_COMPLETED, FakeCallManager.ACTION_BOOT -> handleBoot(context)
            FakeCallManager.ACTION_DECLINE -> handleDecline(context, intent)
        }
    }

    private fun handleTrigger(context: Context, intent: Intent) {
        val id = intent.getStringExtra(FakeCallManager.EXTRA_ID) ?: return
        val prefs = GlobalContext.get().get<PreferenceManager>()
        val entry = FakeCallManager.findEntry(prefs, id) ?: return

        // Wake the screen
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "EverDialer:FakeCallWakeLock"
            )
            wl.acquire(10_000L)
        } catch (_: Exception) {}

        ensureChannel(context)

        // Full-screen intent → launches FakeIncomingCallActivity (uses the same call UI)
        val fullScreenIntent = Intent(context, FakeIncomingCallActivity::class.java).apply {
            putExtra(FakeCallManager.EXTRA_ID, id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, id.hashCode() + 1, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline action PI
        val declineIntent = Intent(context, FakeCallReceiver::class.java).apply {
            action = FakeCallManager.ACTION_DECLINE
            putExtra(FakeCallManager.EXTRA_ID, id)
        }
        val declinePi = PendingIntent.getBroadcast(
            context, id.hashCode() + 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(entry.displayName)
            .setContentText("Incoming call · ${entry.phoneNumber}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPi, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePi)
            .addAction(android.R.drawable.sym_call_incoming, "Answer", fullScreenPi)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(FakeCallManager.notificationId(id), notification)

        // Also launch the activity directly so it shows over lock screen
        try { context.startActivity(fullScreenIntent) } catch (_: Exception) {}

        // Re-schedule if repeating
        if (entry.days.isNotEmpty()) {
            FakeCallManager.rescheduleNext(context, prefs, id)
        }
    }

    private fun handleDecline(context: Context, intent: Intent) {
        val id = intent.getStringExtra(FakeCallManager.EXTRA_ID) ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(FakeCallManager.notificationId(id))
    }

    private fun handleBoot(context: Context) {
        val prefs = GlobalContext.get().get<PreferenceManager>()
        FakeCallManager.rescheduleAll(context, prefs)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Shows simulated incoming calls"
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
        }
    }
}
