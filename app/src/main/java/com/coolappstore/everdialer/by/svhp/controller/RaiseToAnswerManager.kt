package com.coolappstore.everdialer.by.svhp.controller

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.telecom.Call
import android.util.Log
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager

/**
 * Single source of truth for the "Raise to Answer" feature: decides — based on the user's
 * preference and on-device sensor availability — whether [RaiseToAnswerService] should be
 * running, and starts/stops it in lockstep with the ringing state of the active call.
 *
 * This intentionally does NOT use a PHONE_STATE broadcast receiver or TelecomManager:
 * Ever Dialer's [CallService] (the app's own InCallService) already reports ringing/answered/
 * declined state changes directly and can answer/decline the call itself, so no extra
 * telephony broadcast permission or call-control permission is needed for this feature.
 */
object RaiseToAnswerManager {

    private const val TAG = "RaiseToAnswerManager"

    @Volatile
    private var isRunning = false

    private fun sensorManager(context: Context): SensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** True if this device has the minimum sensors (proximity + accelerometer) required. */
    fun hasRequiredSensors(context: Context): Boolean {
        val sm = sensorManager(context)
        return sm.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null &&
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }

    /** True if this device additionally has a magnetometer, enabling the more precise mode. */
    fun hasMagnetometer(context: Context): Boolean =
        sensorManager(context).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null

    /** Call from CallService whenever a tracked call's Telecom state changes. */
    fun onCallStateChanged(context: Context, call: Call) {
        if (call.state == Call.STATE_RINGING) {
            start(context)
        } else {
            stop(context)
        }
    }

    private fun start(context: Context) {
        val appContext = context.applicationContext
        val prefs = PreferenceManager(appContext)

        val raiseEnabled = prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ENABLED, false)
        val flipDeclineEnabled = prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_DECLINE_FLIP, false)

        if (!raiseEnabled && !flipDeclineEnabled) return
        if (!hasRequiredSensors(appContext)) return
        if (isRunning) return

        val intent = Intent(appContext, RaiseToAnswerService::class.java).apply {
            val forcedAnyAngle = !hasMagnetometer(appContext)
            putExtra(RaiseToAnswerService.EXTRA_ANSWER_ENABLED, raiseEnabled)
            putExtra(RaiseToAnswerService.EXTRA_ANSWER_ALL_ANGLES, forcedAnyAngle || prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_ANY_ANGLE, false))
            putExtra(RaiseToAnswerService.EXTRA_DECLINE_ENABLED, flipDeclineEnabled)
            putExtra(RaiseToAnswerService.EXTRA_BEEP_ENABLED, prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_BEEP, true))
            putExtra(RaiseToAnswerService.EXTRA_VIBRATE_ENABLED, prefs.getBoolean(PreferenceManager.KEY_RAISE_TO_ANSWER_VIBRATE, false))
        }

        try {
            appContext.startForegroundService(intent)
            isRunning = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not start RaiseToAnswerService", e)
            isRunning = false
        }
    }

    fun stop(context: Context) {
        if (!isRunning) return
        try {
            context.applicationContext.stopService(Intent(context.applicationContext, RaiseToAnswerService::class.java))
        } catch (_: Exception) {
        } finally {
            isRunning = false
        }
    }
}
