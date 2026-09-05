package com.coolappstore.everdialer.by.svhp.controller

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
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
     * 0.0f = Firm/Hard shake required (~8.0g) — requires deliberate, hard shake when slider is low
     * 0.5f = Moderate shake required (~5.4g)
     * 1.0f = Gentle/Light shake required (~2.8g)
     */
    fun calculateThresholdG(intensity: Float): Float {
        val clamped = intensity.coerceIn(0f, 1f)
        return 8.0f - (clamped * 5.2f)
    }

    /**
     * Reusable detector for Left -> Right -> Left -> Right (or Right -> Left -> Right -> Left)
     * shake gesture with gravity removal and inter-stroke timing validation.
     */
    class ShakePatternDetector(
        var intensity: Float,
        private val onTrigger: () -> Unit
    ) {
        private var gravityX = 0f
        private var hasGravity = false
        private var lastStrokeTime = 0L
        private var lastDirection = 0 // -1 = Left, +1 = Right
        private var strokeCount = 0

        fun reset() {
            hasGravity = false
            gravityX = 0f
            lastStrokeTime = 0L
            lastDirection = 0
            strokeCount = 0
        }

        fun processEvent(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
            val rawAx = event.values[0]
            if (!hasGravity) {
                gravityX = rawAx.coerceIn(-SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH)
                hasGravity = true
            } else {
                val alpha = 0.95f
                gravityX = (alpha * gravityX + (1f - alpha) * rawAx).coerceIn(-SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH)
            }
            val dynamicAx = rawAx - gravityX

            val threshold = calculateThresholdG(intensity)
            val thresholdAccelX = threshold * SensorManager.GRAVITY_EARTH
            val now = System.currentTimeMillis()

            if (strokeCount == 0) {
                if (dynamicAx > thresholdAccelX) {
                    strokeCount = 1
                    lastDirection = 1
                    lastStrokeTime = now
                } else if (dynamicAx < -thresholdAccelX) {
                    strokeCount = 1
                    lastDirection = -1
                    lastStrokeTime = now
                }
            } else {
                val elapsed = now - lastStrokeTime
                if (elapsed > 650L) {
                    // Timeout exceeded between strokes: reset and check if this starts a new sequence
                    if (dynamicAx > thresholdAccelX) {
                        strokeCount = 1
                        lastDirection = 1
                        lastStrokeTime = now
                    } else if (dynamicAx < -thresholdAccelX) {
                        strokeCount = 1
                        lastDirection = -1
                        lastStrokeTime = now
                    } else {
                        strokeCount = 0
                        lastDirection = 0
                    }
                } else if (elapsed >= 110L) {
                    // Must be the opposite direction
                    val expectedDirection = -lastDirection
                    val matchesExpected = (expectedDirection == 1 && dynamicAx > thresholdAccelX) ||
                            (expectedDirection == -1 && dynamicAx < -thresholdAccelX)

                    if (matchesExpected) {
                        strokeCount++
                        lastDirection = expectedDirection
                        lastStrokeTime = now

                        if (strokeCount >= 4) {
                            strokeCount = 0
                            lastDirection = 0
                            lastStrokeTime = 0L
                            onTrigger()
                        }
                    }
                }
            }
        }
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
