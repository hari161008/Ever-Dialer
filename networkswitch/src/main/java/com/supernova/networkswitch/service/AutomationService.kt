package com.supernova.networkswitch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.telephony.SubscriptionManager
import androidx.core.app.NotificationCompat
import com.supernova.networkswitch.R
import com.supernova.networkswitch.di.NetworkSwitchGraph
import com.supernova.networkswitch.domain.model.AppAutomationMode
import com.supernova.networkswitch.domain.model.AppLaunchAutomationConfig
import com.supernova.networkswitch.domain.model.AutomationMode
import com.supernova.networkswitch.domain.model.BatterySaverAutomationConfig
import com.supernova.networkswitch.domain.model.ScreenStateAutomationConfig
import com.supernova.networkswitch.presentation.ui.activity.MainActivity
import com.supernova.networkswitch.util.MasterSwitchStore
import com.supernova.networkswitch.util.UsageAccessHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Background foreground service that implements all three automation rules:
 *  - "Switch Based On Screen State": listens for ACTION_SCREEN_ON / ACTION_SCREEN_OFF.
 *  - "Switch based on Battery Saver state": listens for ACTION_POWER_SAVE_MODE_CHANGED.
 *  - "Switch Based On App Launched": polls the foreground app via UsageStatsManager (requires
 *    the user-granted "Usage Access" special permission).
 *
 * Started/stopped by [AutomationServiceController] whenever the combination of the master
 * 4G/5G Switcher toggle and these three rule toggles changes. When multiple rules are enabled at
 * once and conditions overlap, priority is: Battery Saver > App Launched > Screen State — battery
 * state is the most safety-relevant, and a per-app rule is more specific than a blanket
 * screen-on/off rule.
 */
class AutomationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    private var screenStateConfig = ScreenStateAutomationConfig()
    private var batterySaverConfig = BatterySaverAutomationConfig()
    private var appLaunchConfig = AppLaunchAutomationConfig()

    private var isScreenOn = true
    private var lastAppliedMode: AutomationMode? = null
    private var lastForegroundPackage: String? = null

    // Tracks whether the Battery Saver rule is currently the one driving the mode, so we know
    // when it turns off that we should switch to the opposite mode.
    private var batterySaverOverrideActive = false

    // Tracks whether an app-launch rule is currently driving the mode, and which app/mode it was,
    // so that when the app is closed we can switch to the opposite mode.
    private var appLaunchOverrideActive = false
    private var activeAppLaunchPackage: String? = null

    private var receiverRegistered = false
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    evaluateAndApply()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    evaluateAndApply()
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    evaluateAndApply()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NetworkSwitchGraph.init(applicationContext)
        MasterSwitchStore.init(applicationContext)

        startForegroundWithNotification()
        registerReceivers()
        observeConfigs()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        val channelId = "network_switch_automation"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                channelId,
                "Network Mode Automation",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps automatic network mode switching rules running in the background"
                setShowBadge(false)
            }
            manager?.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("4G/5G Switcher automation active")
            .setContentText("Watching for screen, battery saver, or app changes")
            .setSmallIcon(R.drawable.ic_5g_big)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(openAppIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            // Android requires a notification to start a foreground service, but the automation
            // rules running in the background aren't something the user needs to be reminded
            // about on every screen/app change, so hide it from the notification shade right
            // after satisfying that requirement. The service keeps running in the foreground.
            androidx.core.app.NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // If the platform refuses the foreground start (e.g. background start restrictions),
            // fall back to running as a plain background service rather than crashing.
        }
    }

    private fun registerReceivers() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        registerReceiver(stateReceiver, filter)
        receiverRegistered = true

        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        isScreenOn = powerManager?.isInteractive ?: true
    }

    private fun observeConfigs() {
        serviceScope.launch {
            NetworkSwitchGraph.preferencesRepository.observeScreenStateConfig().collectLatest {
                screenStateConfig = it
                syncPollingState()
                evaluateAndApply()
            }
        }
        serviceScope.launch {
            NetworkSwitchGraph.preferencesRepository.observeBatterySaverConfig().collectLatest {
                batterySaverConfig = it
                evaluateAndApply()
            }
        }
        serviceScope.launch {
            NetworkSwitchGraph.preferencesRepository.observeAppLaunchConfig().collectLatest {
                appLaunchConfig = it
                syncPollingState()
                evaluateAndApply()
            }
        }
    }

    private fun syncPollingState() {
        val shouldPoll = appLaunchConfig.enabled && UsageAccessHelper.hasUsageAccess(applicationContext)
        if (shouldPoll && pollingJob == null) {
            pollingJob = serviceScope.launch { pollForegroundApp() }
        } else if (!shouldPoll && pollingJob != null) {
            pollingJob?.cancel()
            pollingJob = null
            lastForegroundPackage = null
        }
    }

    private suspend fun pollForegroundApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        while (true) {
            try {
                val foregroundPackage = queryForegroundPackage(usageStatsManager)
                if (foregroundPackage != null && foregroundPackage != lastForegroundPackage) {
                    lastForegroundPackage = foregroundPackage
                    evaluateAndApply()
                }
            } catch (e: Exception) {
                // Ignore transient UsageStatsManager errors and keep polling.
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun queryForegroundPackage(usageStatsManager: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - USAGE_EVENTS_WINDOW_MS
        val events = usageStatsManager.queryEvents(startTime, endTime)
        var lastResumedPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                lastResumedPackage = event.packageName
            }
        }
        return lastResumedPackage
    }

    /**
     * Re-evaluates all enabled rules against current state and applies the highest-priority
     * target mode, if it differs from what was last applied.
     *
     * Both overriding rules (Battery Saver and App Launched) toggle to the opposite mode when
     * the condition that triggered them goes away — battery saver turns back off, or the
     * configured app is closed/exited — instead of falling straight through to a lower-priority
     * rule.
     */
    private fun evaluateAndApply() {
        if (!MasterSwitchStore.isEnabled()) return

        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        val batterySaverOn = powerManager?.isPowerSaveMode ?: false

        // --- Rule 1: Battery Saver (highest priority) ---
        if (batterySaverConfig.enabled && batterySaverOn) {
            batterySaverOverrideActive = true
            applyIfNeeded(batterySaverConfig.mode)
            return
        } else if (batterySaverOverrideActive) {
            // Battery Saver was just turned off (or the rule got disabled): switch to the
            // opposite of the mode Battery Saver had set.
            batterySaverOverrideActive = false
            applyIfNeeded(opposite(batterySaverConfig.mode))
            return
        }

        // --- Rule 2: App Launched ---
        val configuredMode: AutomationMode? = if (appLaunchConfig.enabled) {
            lastForegroundPackage?.let { pkg ->
                when (appLaunchConfig.appModes[pkg]) {
                    AppAutomationMode.MODE_A -> AutomationMode.MODE_A
                    AppAutomationMode.MODE_B -> AutomationMode.MODE_B
                    else -> null
                }
            }
        } else null

        if (configuredMode != null) {
            appLaunchOverrideActive = true
            activeAppLaunchPackage = lastForegroundPackage
            applyIfNeeded(configuredMode)
            return
        } else if (appLaunchOverrideActive) {
            // The configured app was closed/exited (foreground moved to an app with no mode
            // configured, or the rule got disabled): switch to the opposite of the mode that
            // app had set.
            val appMode = activeAppLaunchPackage?.let { appLaunchConfig.appModes[it] }
            appLaunchOverrideActive = false
            activeAppLaunchPackage = null
            val target = when (appMode) {
                AppAutomationMode.MODE_A -> AutomationMode.MODE_B
                AppAutomationMode.MODE_B -> AutomationMode.MODE_A
                else -> null
            }
            if (target != null) {
                applyIfNeeded(target)
                return
            }
            // Nothing to switch to — fall through to the screen-state fallback.
        }

        // --- Rule 3: Screen State (fallback) ---
        screenStateFallback()?.let { applyIfNeeded(it) }
    }

    private fun screenStateFallback(): AutomationMode? {
        if (!screenStateConfig.enabled) return null
        return if (isScreenOn) screenStateConfig.screenOnMode else screenStateConfig.screenOffMode
    }

    private fun opposite(mode: AutomationMode): AutomationMode =
        if (mode == AutomationMode.MODE_A) AutomationMode.MODE_B else AutomationMode.MODE_A

    private fun applyIfNeeded(mode: AutomationMode) {
        if (mode != lastAppliedMode) {
            lastAppliedMode = mode
            applyMode(mode)
        }
    }

    private fun applyMode(mode: AutomationMode) {
        serviceScope.launch {
            try {
                val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                NetworkSwitchGraph.applyAutomationModeUseCase(subId, mode)
            } catch (e: Exception) {
                // Swallow: next state change will simply retry.
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (receiverRegistered) {
            try {
                unregisterReceiver(stateReceiver)
            } catch (_: Exception) {
            }
            receiverRegistered = false
        }
        pollingJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 8421
        private const val POLL_INTERVAL_MS = 2000L
        private const val USAGE_EVENTS_WINDOW_MS = 10_000L
    }
}
