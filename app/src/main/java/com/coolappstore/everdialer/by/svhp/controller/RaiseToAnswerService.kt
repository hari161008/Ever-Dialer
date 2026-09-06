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
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.coolappstore.everdialer.by.svhp.MainActivity
import java.util.Timer
import java.util.TimerTask
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Foreground service that listens to the proximity, accelerometer and (when available)
 * magnetometer sensors while a call is ringing, and automatically answers the call once
 * the device is raised to the user's ear — ported from the open-source "Raise To Answer"
 * project and adapted to drive Ever Dialer's own [CallService] instead of TelecomManager.
 *
 * The service is started and stopped exclusively by [RaiseToAnswerManager], which is the
 * single source of truth for whether the feature is enabled and supported on this device.
 */
class RaiseToAnswerService : Service(), SensorEventListener {

    private var featureAnswerEnabled = true
    private var featureAnswerAllAnglesEnabled = false
    private var featureDeclineEnabled = false
    private var behaviourBeepEnabled = true
    private var behaviourVibrateEnabled = false

    private var proximitySensorRange = 0f
    private var proximitySensorThreshold = 0f

    private var mAccelerometerValues: FloatArray = FloatArray(3)
    private var mMagnetometerValues: FloatArray = FloatArray(3)
    private var mProximityValue: Float? = null
    private var mInclinationValue: Int? = null

    private var mToneGenerator: ToneGenerator? = null
    private var mVibrator: Vibrator? = null

    // First 2 ticks: confirm a sane starting state (proximity not near).
    // 3 more matching ticks in a row: trigger answer / decline.
    private var resetTicksDone = 0
    private var answerTicksDone = 0
    private var declineTicksDone = 0
    private var mTimer: Timer? = null

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        featureAnswerEnabled = intent?.getBooleanExtra(EXTRA_ANSWER_ENABLED, true) ?: true
        featureAnswerAllAnglesEnabled = intent?.getBooleanExtra(EXTRA_ANSWER_ALL_ANGLES, false) ?: false
        featureDeclineEnabled = intent?.getBooleanExtra(EXTRA_DECLINE_ENABLED, false) ?: false
        behaviourBeepEnabled = intent?.getBooleanExtra(EXTRA_BEEP_ENABLED, true) ?: true
        behaviourVibrateEnabled = intent?.getBooleanExtra(EXTRA_VIBRATE_ENABLED, false) ?: false

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // Required sensors are validated by RaiseToAnswerManager before this service is
        // ever started, but bail out defensively rather than crash if they vanish.
        val proximity = proximitySensor
        val accel = accelerometer
        if (proximity == null || accel == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startAsForeground()

        mToneGenerator = try { ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100) } catch (_: Exception) { null }
        mVibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        proximitySensorRange = proximity.maximumRange
        proximitySensorThreshold = proximitySensorRange / 2

        val hasMagnetometer = magnetometer != null
        registerSensors(hasMagnetometer)
        startDetectionTimer(hasMagnetometer)

        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val channelId = "raise_to_answer_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Raise to Answer", NotificationManager.IMPORTANCE_MIN).apply {
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
            .setContentTitle("Raise to Answer")
            .setContentText("Listening for gesture…")
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

    private fun registerSensors(hasMagnetometer: Boolean) {
        sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        if (hasMagnetometer) {
            sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun startDetectionTimer(hasMagnetometer: Boolean) {
        mTimer = Timer()
        mTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                evaluateTick(hasMagnetometer)
            }
        }, 400, 400)
    }

    private fun evaluateTick(hasMagnetometer: Boolean) {
        // Only act if Ever Dialer still has an actual ringing call — guards against the
        // brief window between the call leaving the ringing state and this service
        // receiving its stop command.
        if (CallService.currentCallSession.value?.call?.state != android.telecom.Call.STATE_RINGING) {
            return
        }

        val proximityValue = mProximityValue
        val isCovered = proximityValue != null && (proximityValue <= proximitySensorThreshold || (proximitySensorRange > 0f && proximityValue < proximitySensorRange) || proximityValue == 0.0f)

        // ── 1. Decline by flipping check ──────────────────────────────────────────
        // Lying on a flat surface with screen facing downwards:
        // Gravity is in the negative Z direction (az < -6.5 m/s²), while X and Y are nearly 0.
        val az = if (mAccelerometerValues.isNotEmpty()) mAccelerometerValues[2] else 0f
        val ax = if (mAccelerometerValues.isNotEmpty()) mAccelerometerValues[0] else 0f
        val ay = if (mAccelerometerValues.isNotEmpty()) mAccelerometerValues[1] else 0f
        val xyTilt = sqrt(ax * ax + ay * ay)
        val isScreenFacingDownFlat = az < -6.5f && xyTilt < 5.5f

        if (featureDeclineEnabled && isCovered && isScreenFacingDownFlat) {
            feedback(ToneGenerator.TONE_PROP_NACK, 100)
            declineTicksDone += 1
            if (declineTicksDone >= 2) {
                declineDetected()
                return
            }
        } else {
            declineTicksDone = 0
        }

        // ── 2. Raise to answer check ──────────────────────────────────────────────
        if (!featureAnswerEnabled) {
            return
        }

        val inclinationValue = mInclinationValue

        val orientation = FloatArray(3)
        if (hasMagnetometer && !featureAnswerAllAnglesEnabled) {
            if (mAccelerometerValues.isNotEmpty() && mMagnetometerValues.isNotEmpty()) {
                val rotationMatrix = FloatArray(9)
                if (!SensorManager.getRotationMatrix(rotationMatrix, null, mAccelerometerValues, mMagnetometerValues)) {
                    return
                }
                SensorManager.getOrientation(rotationMatrix, orientation)
            } else {
                return
            }
        }

        // Phase 1: wait until we've seen a couple of consecutive "not near ear" readings,
        // so we have a clean baseline before watching for the raise gesture.
        if (resetTicksDone < 2) {
            if (proximityValue == null || proximityValue >= proximitySensorThreshold) {
                feedback(ToneGenerator.TONE_CDMA_ANSWER, 200)
                resetTicksDone += 1
            } else {
                resetTicksDone = 0
            }
            return
        }

        if (hasMagnetometer && !featureAnswerAllAnglesEnabled) {
            val roll = Math.toDegrees(orientation[2].toDouble()) + 180.0

            if (inclinationValue != null && inclinationValue in -90..90 &&
                proximityValue != null && proximityValue <= proximitySensorThreshold &&
                roll in 45.0..315.0
            ) {
                feedback(ToneGenerator.TONE_CDMA_PIP, 100)
                answerTicksDone += 1
                if (answerTicksDone >= 3) {
                    answerDetected()
                }
            } else {
                answerTicksDone = 0
            }
        } else {
            // Simpler algorithm when there's no magnetometer (or all-angles mode is on):
            // any tilt where the proximity sensor reads "near" counts as a raise.
            if (inclinationValue != null && inclinationValue in -90..90 &&
                proximityValue != null && proximityValue <= proximitySensorThreshold
            ) {
                feedback(ToneGenerator.TONE_CDMA_PIP, 100)
                answerTicksDone += 1
                if (answerTicksDone >= 3) {
                    answerDetected()
                }
            } else {
                answerTicksDone = 0
            }
        }
    }

    private fun feedback(tone: Int, vibrateMs: Long) {
        if (behaviourBeepEnabled) {
            try { mToneGenerator?.startTone(tone, 100) } catch (_: Exception) {}
        }
        if (behaviourVibrateEnabled) {
            try { mVibrator?.vibrate(VibrationEffect.createOneShot(vibrateMs, VibrationEffect.DEFAULT_AMPLITUDE)) } catch (_: Exception) {}
        }
    }

    private fun answerDetected() {
        Log.d(TAG, "Raise-to-answer gesture detected — answering call")
        val handler = Handler(Looper.getMainLooper())
        handler.post { CallService.answerCall() }
        stopSelf()
    }

    private fun declineDetected() {
        Log.d(TAG, "Flip-to-decline gesture detected — declining call")
        val handler = Handler(Looper.getMainLooper())
        handler.post { CallService.declineCall() }
        stopSelf()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                mProximityValue = event.values[0]
            }
            Sensor.TYPE_ACCELEROMETER -> {
                mAccelerometerValues = event.values.copyOf()

                // https://stackoverflow.com/a/15149421
                val normOfG = sqrt(
                    event.values[0] * event.values[0] +
                        event.values[1] * event.values[1] +
                        event.values[2] * event.values[2]
                )
                if (normOfG > 0f) {
                    val nx = event.values[0] / normOfG
                    val ny = event.values[1] / normOfG
                    mInclinationValue = Math.toDegrees(atan2(nx, ny).toDouble()).roundToInt()
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                mMagnetometerValues = event.values.copyOf()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        try { mTimer?.cancel() } catch (_: IllegalStateException) {}
        mTimer = null

        sensorManager?.unregisterListener(this)
        resetTicksDone = 0
        answerTicksDone = 0
        declineTicksDone = 0

        try { mToneGenerator?.release() } catch (_: Exception) {}
        mToneGenerator = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)

        super.onDestroy()
    }

    companion object {
        private const val TAG = "RaiseToAnswer"
        private const val NOTIFICATION_ID = 9421

        const val EXTRA_ANSWER_ENABLED = "extra_answer_enabled"
        const val EXTRA_ANSWER_ALL_ANGLES = "extra_answer_all_angles"
        const val EXTRA_DECLINE_ENABLED = "extra_decline_enabled"
        const val EXTRA_BEEP_ENABLED = "extra_beep_enabled"
        const val EXTRA_VIBRATE_ENABLED = "extra_vibrate_enabled"
    }
}
