/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.services.call

import android.telecom.Call
import android.telecom.InCallService
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.utils.AppLogger

/**
 * Optional, opt-in call detection path using Android's Telecom [InCallService] API instead of
 * (or alongside) the PHONE_STATE broadcast used by [PhoneStateReceiver].
 *
 * This is registered as a non-UI, secondary InCallService (see the manifest metadata), meaning it
 * does not make this app a dialer and does not interfere with whatever the default dialer/InCallService
 * UI is. It simply gets notified of ongoing calls (including self-managed/VoIP calls from apps like
 * WhatsApp or Telegram, via INCLUDE_SELF_MANAGED_CALLS) directly through Telecom, without waiting on
 * a broadcast.
 *
 * Entirely inert unless [AppPreferences.getCallDetectionMode] is [AppPreferences.CallDetectionMode.IN_CALL_SERVICE] —
 * every callback below checks this first, so simply having this service declared in the manifest has
 * no effect on the default (PHONE_STATE) detection method.
 */
class AppInCallService : InCallService() {

    private companion object {
        const val TAG = "SCR:AppInCallService"
    }

    /** Tracks the [Call.Callback] registered for each currently tracked call, so it can be unregistered later. */
    private val registeredCallbacks = mutableMapOf<Call, Call.Callback>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)

        val preferences = AppPreferences(applicationContext)
        if (!preferences.isCallRecordingEnabled()) return
        if (preferences.getCallDetectionMode() != AppPreferences.CallDetectionMode.IN_CALL_SERVICE) return

        AppLogger.d(TAG, "onCallAdded: initial state=${call.state}")

        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                forwardState(call, state)
            }
        }
        call.registerCallback(callback)
        registeredCallbacks[call] = callback

        // The call may already be past its initial state (e.g. RINGING) by the time we're bound
        // and this callback is registered, so forward the current state immediately too.
        forwardState(call, call.state)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        registeredCallbacks.remove(call)?.let { call.unregisterCallback(it) }
    }

    private fun forwardState(call: Call, state: Int) {
        val number = call.details?.handle?.schemeSpecificPart
        try {
            CallSessionManager.getInstance(applicationContext).handleInCallServiceState(state, number)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to forward InCallService state to CallSessionManager", e)
        }
    }
}
