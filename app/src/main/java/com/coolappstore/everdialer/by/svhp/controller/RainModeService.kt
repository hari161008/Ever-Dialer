package com.coolappstore.everdialer.by.svhp.controller

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.Call
import android.util.Log
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import kotlin.math.sqrt

/**
 * Foreground sensor-listening service for Rain Mode.
 * Runs during incoming ringing or ongoing calls, detecting shake gestures using [Sensor.TYPE_ACCELEROMETER]
 * and ensuring that during incoming calls the gesture is ignored if [Sensor.TYPE_PROXIMITY] is covered
 * (e.g. while in a pocket or face down).
 */
class RainModeService : Service(), SensorEventListener {

    private var shakeIntensity = PreferenceManager.DEFAULT_RAIN_MODE_SHAKE_INTENSITY
    private var incomingAction = PreferenceManager.DEFAULT_RAIN_MODE_INCOMING_ACTION
    private var vibrateEnabled = true
    private var endActiveCallEnabled = true
    private var isIncomingRinging = false

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var proximitySensor: Sensor? = null

    @Volatile
    private var isProximityNear = false
    private var proximityThreshold = 0f

    // Horizontal "chop-chop" (X-axis) shake tracking
    private var lastDirectionChangeTimestamp = 0L
    private var lastDirection = 0 // -1 = Left, +1 = Right
    private var reversalCount = 0
    private var lastTriggerTimestamp = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        shakeIntensity = intent?.getFloatExtra(EXTRA_SHAKE_INTENSITY, PreferenceManager.DEFAULT_RAIN_MODE_SHAKE_INTENSITY)
            ?: PreferenceManager.DEFAULT_RAIN_MODE_SHAKE_INTENSITY
        incomingAction = intent?.getStringExtra(EXTRA_INCOMING_ACTION) ?: PreferenceManager.DEFAULT_RAIN_MODE_INCOMING_ACTION
        vibrateEnabled = intent?.getBooleanExtra(EXTRA_VIBRATE_ENABLED, true) ?: true
        endActiveCallEnabled = intent?.getBooleanExtra(EXTRA_END_ACTIVE_CALL, true) ?: true
        isIncomingRinging = intent?.getBooleanExtra(EXTRA_IS_INCOMING_RINGING, false) ?: false

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()

        reversalCount = 0
        lastDirection = 0
        lastDirectionChangeTimestamp = 0L
        lastTriggerTimestamp = 0L
        isProximityNear = false

        // Register accelerometer
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        // Register proximity sensor ONLY during incoming ringing calls
        if (isIncomingRinging) {
            proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            if (proximitySensor != null) {
                proximityThreshold = proximitySensor!!.maximumRange / 2f
                sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        Log.d(TAG, "RainModeService started (incomingRinging=$isIncomingRinging, intensity=$shakeIntensity)")
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val channelId = "rain_mode_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Rain Mode", NotificationManager.IMPORTANCE_MIN).apply {
                    setShowBadge(false)
                }
            )
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Rain Mode")
            .setContentText("Listening for shake gesture…")
            .setContentIntent(contentIntent)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val dist = event.values[0]
            isProximityNear = dist <= proximityThreshold || dist == 0f
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // If incoming call is ringing and proximity is covered, do nothing
            if (isIncomingRinging && isProximityNear) {
                return
            }

            // We specifically detect horizontal side-to-side (X-axis) "chop chop" motion:
            // e.g. Left -> Right -> Left or Right -> Left -> Right.
            val ax = event.values[0]
            val threshold = RainModeManager.calculateThresholdG(shakeIntensity)
            // Convert threshold g to m/s^2 along X axis (e.g. 1.1g ~ 10.8 m/s^2, 1.75g ~ 17.1 m/s^2, 2.4g ~ 23.5 m/s^2)
            val thresholdAccelX = threshold * SensorManager.GRAVITY_EARTH
            val now = System.currentTimeMillis()

            if (now - lastTriggerTimestamp < 1500L) {
                return
            }

            // Check if current X acceleration exceeds the directional threshold
            val currentDirection = when {
                ax > thresholdAccelX -> 1   // Moving right / tilted sharply right
                ax < -thresholdAccelX -> -1 // Moving left / tilted sharply left
                else -> 0
            }

            if (currentDirection != 0) {
                // If it's been too long since the last directional reversal (>600ms), start fresh
                if (now - lastDirectionChangeTimestamp > 600L) {
                    reversalCount = 1
                    lastDirection = currentDirection
                    lastDirectionChangeTimestamp = now
                } else if (currentDirection != lastDirection && (now - lastDirectionChangeTimestamp) > 70L) {
                    // Direction flipped (e.g. Left -> Right or Right -> Left)
                    reversalCount++
                    lastDirection = currentDirection
                    lastDirectionChangeTimestamp = now

                    // 3 directional movements (e.g. Left -> Right -> Left or Right -> Left -> Right)
                    // constitutes a deliberate "chop chop" shake gesture!
                    if (reversalCount >= 3) {
                        reversalCount = 0
                        lastDirection = 0
                        lastTriggerTimestamp = now
                        handleShakeAction()
                    }
                }
            }
        }
    }

    private fun handleShakeAction() {
        Log.d(TAG, "Shake gesture detected in RainModeService")
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val incoming = CallService.incomingCallSession.value?.call
            val current = CallService.currentCallSession.value?.call ?: CallService.heldCallSession.value?.call

            val isRingingNow = (incoming != null && incoming.state == Call.STATE_RINGING) ||
                    (current != null && current.state == Call.STATE_RINGING) ||
                    isIncomingRinging

            if (isRingingNow) {
                if (isProximityNear) {
                    Log.d(TAG, "Shake ignored because proximity sensor is covered")
                    return@post
                }
                if (incomingAction == "decline") {
                    Log.d(TAG, "Rain Mode declining call")
                    CallService.declineCall()
                    if (vibrateEnabled) {
                        VolumeDndAccessibilityService.performVibration(this, longArrayOf(0, 180, 80, 180))
                    }
                } else {
                    Log.d(TAG, "Rain Mode answering call")
                    CallService.answerCall()
                    if (vibrateEnabled) {
                        VolumeDndAccessibilityService.performVibration(this, longArrayOf(0, 120, 80, 120))
                    }
                }
                stopSelf()
            } else if (endActiveCallEnabled && current != null) {
                Log.d(TAG, "Rain Mode ending active call")
                CallService.declineCall()
                if (vibrateEnabled) {
                    VolumeDndAccessibilityService.performVibration(this, longArrayOf(0, 180, 80, 180))
                }
                stopSelf()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
        sensorManager = null
        accelerometer = null
        proximitySensor = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)

        Log.d(TAG, "RainModeService destroyed")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RainModeService"
        private const val NOTIFICATION_ID = 9422

        const val EXTRA_SHAKE_INTENSITY = "extra_shake_intensity"
        const val EXTRA_INCOMING_ACTION = "extra_incoming_action"
        const val EXTRA_VIBRATE_ENABLED = "extra_vibrate_enabled"
        const val EXTRA_END_ACTIVE_CALL = "extra_end_active_call"
        const val EXTRA_IS_INCOMING_RINGING = "extra_is_incoming_ringing"
    }
}
