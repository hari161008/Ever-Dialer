/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.services.call

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.data.recordings.RecordingDirection
import com.coolappstore.evercallrecorder.by.svhp.data.recordings.RecordingMetadata
import com.coolappstore.evercallrecorder.by.svhp.services.recording.RecordingForegroundService
import com.coolappstore.evercallrecorder.by.svhp.utils.AppLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Detects ongoing voice/video calls inside WhatsApp and Telegram, and drives [RecordingForegroundService]
 * the same way [PhoneStateReceiver]/[CallSessionManager] do for normal telephony calls.
 *
 * **Why a notification listener?** Unlike telephony calls, VoIP calls placed inside a third-party app never
 * raise [android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED]; Android has no public broadcast for
 * "a call is happening inside app X". The one reliable, system-documented signal every call app is expected
 * to expose is the *ongoing call notification* — the persistent notification shown for the whole duration of
 * the call, marked with [Notification.CATEGORY_CALL] (this is also the category Android 12+'s CallStyle API
 * requires). We watch for that notification appearing/disappearing for the packages in [AppCallTarget].
 *
 * **What gets recorded?** Once a call is detected, this service forwards to [RecordingForegroundService]
 * exactly like a normal call would, except the resulting [RecordingMetadata.sourceApp] is set. That flag makes
 * [com.coolappstore.evercallrecorder.by.svhp.services.recording.AudioRecordingEngine] capture with
 * [com.coolappstore.evercallrecorder.by.svhp.integrations.scrcpy.ScrcpyAudioSource.OUTPUT] instead of the
 * telephony-only VOICE_CALL source, relayed through the same privileged Shizuku/scrcpy-server pipeline. PLAYBACK
 * and MIC-class sources were tried first and both produced silent recordings: PLAYBACK hard-excludes audio
 * tagged `USAGE_VOICE_COMMUNICATION` (how these apps tag call audio); MIC-class sources compete with the app's
 * own concurrent microphone session and get silenced by Android's privacy protections. OUTPUT is a privileged,
 * CAPTURE_AUDIO_OUTPUT-gated system mix tap — the same permission class as VOICE_CALL — so it hits neither limit.
 *
 * **Requires the user to grant "Notification access"** to this app (Settings ➜ Notification access), the same
 * system permission any notification-reading app needs. See "Record calls from apps" in Settings, which links
 * to that screen when needed.
 */
class AppCallNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "SCR:AppCallNotifListener"
    }

    /**
     * Notification key → [AppCallTarget] for every call we are currently treating as active.
     *
     * Both WhatsApp and Telegram keep updating their ongoing-call notification every second (to tick the call
     * duration shown to the user), which re-triggers [onNotificationPosted] for the *same* notification key
     * many times over a single call. We only want to start a recording session once per call, so we track
     * which keys we've already reacted to here, and only stop when that key is actually removed.
     */
    private val activeCalls = ConcurrentHashMap<String, AppCallTarget>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        AppLogger.d(TAG, "Notification listener connected. Scanning currently active notifications for in-progress calls...")
        // Covers the case where the listener (re)binds while a call is already ongoing (e.g. app update,
        // Shizuku/Settings churn, or the system rebinding us), so we don't miss the rest of that call.
        try {
            activeNotifications?.forEach(::handlePosted)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to scan existing notifications on connect: ${e.message}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        AppLogger.w(TAG, "Notification listener disconnected.")
        activeCalls.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = handlePosted(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val target = activeCalls.remove(sbn.key) ?: return
        AppLogger.i(TAG, "${target.key} call notification ended (key=${sbn.key}). Stopping recording session.")
        sendServiceCommand(RecordingForegroundService.ACTION_STOP_RECORDING)
    }

    // -- Private helpers

    private fun handlePosted(sbn: StatusBarNotification) {
        val target = AppCallTarget.fromPackageName(sbn.packageName) ?: return
        if (!isTargetEnabled(target)) return
        if (!looksLikeOngoingCallNotification(sbn.notification)) return

        // putIfAbsent: if this key is already tracked, this is just the call-duration ticking the
        // notification text every second, not a new call. Ignore it to avoid sending duplicate START intents.
        if (activeCalls.putIfAbsent(sbn.key, target) != null) return

        val metadata = buildMetadata(target, sbn.notification)
        AppLogger.i(TAG, "Detected ongoing ${target.key} call (notification key=${sbn.key}, direction=${metadata.direction}). Starting recording session.")
        sendServiceCommand(RecordingForegroundService.ACTION_START_RECORDING, metadata)
    }

    private fun isTargetEnabled(target: AppCallTarget): Boolean {
        val preferences = AppPreferences(applicationContext)
        return when (target) {
            AppCallTarget.WHATSAPP -> preferences.isRecordWhatsAppCallsEnabled()
            AppCallTarget.TELEGRAM -> preferences.isRecordTelegramCallsEnabled()
        }
    }

    /**
     * Decides whether [notification] represents an in-progress call rather than some other notification
     * from the same app (a new-message ping, a missed-call notice, an incoming-ringing alert that the user
     * hasn't answered yet, etc).
     *
     * We require both:
     *  - [Notification.CATEGORY_CALL]: the category every well-behaved call app sets on its in-call notification
     *    (mandatory for Android 12+'s CallStyle, and what WhatsApp/Telegram use on versions that support it).
     *  - [Notification.FLAG_ONGOING_EVENT]: sets it apart from a *missed*-call notification, which uses the same
     *    category but is dismissible and not ongoing.
     */
    private fun looksLikeOngoingCallNotification(notification: Notification): Boolean {
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        return isOngoing && notification.category == Notification.CATEGORY_CALL
    }

    /**
     * Builds the [RecordingMetadata] for a newly detected app call.
     *
     * There is no phone number for a VoIP call, so the notification's title (usually the contact's name, as
     * shown by the messaging app itself) is stored in [RecordingMetadata.rawPhoneNumber] purely so it still
     * flows through to filenames/fallbacks. [RecordingMetadata.sourceApp] is what actually drives the special
     * PLAYBACK-capture + filename-prefix handling.
     */
    private fun buildMetadata(target: AppCallTarget, notification: Notification): RecordingMetadata {
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val body = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val callerLabel = title?.takeIf { it.isNotBlank() }

        return RecordingMetadata(
            rawPhoneNumber = callerLabel,
            direction = guessDirection(body),
            isEnriched = true, // Skip phone-number enrichment entirely; callerLabel is not a real number.
            isCrossCountry = false,
            sourceApp = getString(target.displayNameResId)
        )
    }

    /**
     * Best-effort guess at call direction from the notification body text.
     *
     * By the time a VoIP call's *ongoing* notification appears, incoming and outgoing calls usually look
     * identical (Android exposes no reliable API for the original direction of a third-party app's VoIP call),
     * so this only catches the rare case where the notification text still hints at it. It defaults to
     * [RecordingDirection.INCOMING] when ambiguous — this only affects the direction label/icon shown for the
     * recording, never whether the call gets recorded.
     */
    private fun guessDirection(notificationText: String?): RecordingDirection {
        val lower = notificationText?.lowercase().orEmpty()
        return if (lower.contains("outgoing") || lower.contains("calling")) {
            RecordingDirection.OUTGOING
        } else {
            RecordingDirection.INCOMING
        }
    }

    /** Mirrors [CallSessionManager]'s service-command helper so both pipelines drive [RecordingForegroundService] identically. */
    private fun sendServiceCommand(action: String, metadata: RecordingMetadata? = null) {
        val intent = Intent(applicationContext, RecordingForegroundService::class.java).apply {
            this.action = action
            if (metadata != null) {
                putExtra(RecordingMetadata.EXTRA_METADATA, metadata)
            }
        }
        if (action == RecordingForegroundService.ACTION_STOP_RECORDING) {
            applicationContext.startService(intent)
        } else {
            applicationContext.startForegroundService(intent)
        }
    }
}
