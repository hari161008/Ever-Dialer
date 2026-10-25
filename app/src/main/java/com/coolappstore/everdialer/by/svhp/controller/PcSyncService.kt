package com.coolappstore.everdialer.by.svhp.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.controller.util.PcBluetoothSyncManager
import com.coolappstore.everdialer.by.svhp.controller.util.PcSyncStatus
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Foreground service that keeps Ever Dialer continuously connected to the PC app via Bluetooth
 * even when the mobile app is in the background or closed.
 */
class PcSyncService : Service() {

    companion object {
        private const val TAG = "PcSyncService"
        const val CHANNEL_ID = "everdialer_pc_connect_channel"
        const val NOTIFICATION_ID = 9102

        const val ACTION_UPDATE_NOTIFICATION = "com.coolappstore.everdialer.action.UPDATE_PC_SYNC_NOTIFICATION"
        const val EXTRA_SHOW_NOTIFICATION = "extra_show_notification"

        fun start(context: Context) {
            val intent = Intent(context, PcSyncService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start PcSyncService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PcSyncService::class.java)
            try {
                context.stopService(intent)
            } catch (_: Exception) {}
        }

        fun updateNotificationVisibility(context: Context, show: Boolean) {
            val intent = Intent(context, PcSyncService::class.java).apply {
                action = ACTION_UPDATE_NOTIFICATION
                putExtra(EXTRA_SHOW_NOTIFICATION, show)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send notification visibility update: ${e.message}")
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var syncManager: PcBluetoothSyncManager? = null
    private var prefs: PreferenceManager? = null
    private var lastNotificationTitle: String? = null
    private var lastNotificationText: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "PcSyncService created")
        createNotificationChannel()

        try {
            syncManager = GlobalContext.get().getOrNull()
            prefs = GlobalContext.get().getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving Koin dependencies: ${e.message}")
        }

        val showNotification = prefs?.getBoolean(PreferenceManager.KEY_PC_SYNC_SHOW_NOTIFICATION, true) ?: true
        val notification = buildNotification(
            title = "Ever Dialer PC Connect",
            content = "Waiting for Ever Dialer PC..."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        if (!showNotification) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping foreground notification: ${e.message}")
            }
        }

        observeSyncStatus()
        ensureSyncStarted()
    }

    private fun observeSyncStatus() {
        val manager = syncManager ?: return
        scope.launch {
            manager.status.collectLatest { status ->
                val device = manager.connectedDeviceName.value
                val title: String
                val text: String

                when (status) {
                    PcSyncStatus.CONNECTED -> {
                        title = "Connected to PC"
                        text = if (!device.isNullOrBlank()) "Linked with $device • Real-time sync" else "Real-time sync active"
                    }
                    PcSyncStatus.SYNCING -> {
                        title = "Syncing with PC"
                        text = "Transferring latest updates..."
                    }
                    PcSyncStatus.CONNECTING -> {
                        title = "Connecting to PC"
                        text = "Searching for Ever Dialer PC..."
                    }
                    else -> {
                        title = "Ever Dialer PC Connect"
                        text = "Waiting for Ever Dialer PC..."
                    }
                }
                updateNotification(title, text)
            }
        }
    }

    private fun ensureSyncStarted() {
        val p = prefs
        val isEnabled = p?.getBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, false) ?: false
        if (isEnabled) {
            syncManager?.start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val p = prefs ?: try { GlobalContext.get().getOrNull<PreferenceManager>() } catch (_: Throwable) { null }
        val isEnabled = p?.getBoolean(PreferenceManager.KEY_PC_SYNC_BLUETOOTH_ENABLED, false) ?: false
        if (!isEnabled) {
            Log.i(TAG, "Bluetooth PC Sync disabled in prefs, stopping PcSyncService")
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_UPDATE_NOTIFICATION) {
            val show = intent.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true)
            if (!show) {
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping foreground notification: ${e.message}")
                }
            } else {
                val title = lastNotificationTitle ?: "Ever Dialer PC Connect"
                val text = lastNotificationText ?: "Waiting for Ever Dialer PC..."
                val notif = buildNotification(title, text)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
                    } else {
                        startForeground(NOTIFICATION_ID, notif)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error restoring foreground notification: ${e.message}")
                }
            }
            return START_STICKY
        }

        ensureSyncStarted()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PC Connect Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Ever Dialer connected with Ever Dialer PC in the background"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        lastNotificationTitle = title
        lastNotificationText = content
        val showNotification = prefs?.getBoolean(PreferenceManager.KEY_PC_SYNC_SHOW_NOTIFICATION, true) ?: true
        if (!showNotification) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {}
            return
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            nm.notify(NOTIFICATION_ID, buildNotification(title, content))
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        Log.i(TAG, "PcSyncService destroyed")
        scope.cancel()
        super.onDestroy()
    }
}
