package com.coolappstore.everdialer.by.svhp.controller

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.telecom.Call
import android.util.Log
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager

/**
 * Single source of truth for the "Rain Mode" shake-to-answer/reject feature:
 * decides — based on user preference and on-device sensor availability — whether
 * [RainModeService] should be running, and starts/stops it in lockstep with the call state,
 * operating identically to [RaiseToAnswerManager].
 */
object RainModeManager {

    private const val TAG = "RainModeManager"

    @Volatile
    private var isRunning = false

    @Volatile
    private var currentIsIncomingRinging = false

    /**
     * Converts a 0.0f..1.0f intensity setting into an acceleration g-force threshold.
     * 0.0f = Firm shake required (~4.0g)
     * 0.5f = Moderate shake required (~2.8g)
     * 1.0f = Gentle shake required (~1.6g)
     */
    fun calculateThresholdG(intensity: Float): Float {
        val clamped = intensity.coerceIn(0f, 1f)
        return 4.0f - (clamped * 2.4f)
    }

    /** True if this device has an accelerometer sensor. */
    fun hasRequiredSensors(context: Context): Boolean {
        val sm = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        return sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    /** Called from CallService whenever a tracked call's state changes. */
    fun onCallStateChanged(context: Context, call: Call) {
        val appContext = context.applicationContext
        val prefs = PreferenceManager(appContext)

        if (!prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, false) || !hasRequiredSensors(appContext)) {
            stop(context)
            return
        }

        when (call.state) {
            Call.STATE_RINGING -> {
                start(context, isIncomingRinging = true)
            }
            Call.STATE_ACTIVE, Call.STATE_DIALING -> {
                val allowEndActive = prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_END_ACTIVE_CALL, true)
                if (allowEndActive) {
                    start(context, isIncomingRinging = false)
                } else {
                    stop(context)
                }
            }
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> {
                val incoming = CallService.incomingCallSession.value?.call
                val current = CallService.currentCallSession.value?.call
                val held = CallService.heldCallSession.value?.call

                val hasRinging = incoming != null && incoming.state == Call.STATE_RINGING
                val hasActive = (current != null && current.state == Call.STATE_ACTIVE) ||
                        (held != null && held.state == Call.STATE_ACTIVE)

                if (hasRinging) {
                    start(context, isIncomingRinging = true)
                } else if (hasActive && prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_END_ACTIVE_CALL, true)) {
                    start(context, isIncomingRinging = false)
                } else {
                    stop(context)
                }
            }
        }
    }

    private fun start(context: Context, isIncomingRinging: Boolean) {
        val appContext = context.applicationContext
        val prefs = PreferenceManager(appContext)

        if (!prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_ENABLED, false)) return
        if (!hasRequiredSensors(appContext)) return

        if (isRunning && currentIsIncomingRinging == isIncomingRinging) return

        val intent = Intent(appContext, RainModeService::class.java).apply {
            putExtra(
                RainModeService.EXTRA_SHAKE_INTENSITY,
                prefs.getFloat(
                    PreferenceManager.KEY_RAIN_MODE_SHAKE_INTENSITY,
                    PreferenceManager.DEFAULT_RAIN_MODE_SHAKE_INTENSITY
                )
            )
            putExtra(
                RainModeService.EXTRA_INCOMING_ACTION,
                prefs.getString(
                    PreferenceManager.KEY_RAIN_MODE_INCOMING_ACTION,
                    PreferenceManager.DEFAULT_RAIN_MODE_INCOMING_ACTION
                )
            )
            putExtra(
                RainModeService.EXTRA_VIBRATE_ENABLED,
                prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_VIBRATE, true)
            )
            putExtra(
                RainModeService.EXTRA_END_ACTIVE_CALL,
                prefs.getBoolean(PreferenceManager.KEY_RAIN_MODE_END_ACTIVE_CALL, true)
            )
            putExtra(RainModeService.EXTRA_IS_INCOMING_RINGING, isIncomingRinging)
        }

        try {
            appContext.startForegroundService(intent)
            isRunning = true
            currentIsIncomingRinging = isIncomingRinging
            Log.d(TAG, "RainModeService started via startForegroundService")
        } catch (e: Exception) {
            Log.w(TAG, "Could not start RainModeService", e)
            isRunning = false
        }
    }

    fun stop(context: Context) {
        if (!isRunning) return
        try {
            context.applicationContext.stopService(Intent(context.applicationContext, RainModeService::class.java))
        } catch (_: Exception) {
        } finally {
            isRunning = false
            currentIsIncomingRinging = false
            Log.d(TAG, "RainModeService stopped")
        }
    }
}
