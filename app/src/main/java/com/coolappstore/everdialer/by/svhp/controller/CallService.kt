package com.coolappstore.everdialer.by.svhp.controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.core.app.NotificationCompat
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.view.screen.CallActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject

data class CallSession(
    val call: Call,
    val state: Int,
    val updateTime: Long = System.currentTimeMillis()
)

class CallService : InCallService() {

    private val contactsRepository: IContactsRepository by inject()
    private val prefs: PreferenceManager by inject()

    companion object {
        private const val CHANNEL_ID = "call_channel"
        private const val NOTIFICATION_ID = 101

        private val _currentCallSession = MutableStateFlow<CallSession?>(null)
        val currentCallSession = _currentCallSession.asStateFlow()

        private val _audioState = MutableStateFlow<CallAudioState?>(null)
        val audioState = _audioState.asStateFlow()

        private var instance: CallService? = null

        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }

        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }

        fun answerCall() {
            _currentCallSession.value?.call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        }

        fun declineCall() {
            _currentCallSession.value?.call?.disconnect()
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            _currentCallSession.value = CallSession(call, state)

            if (state == Call.STATE_DISCONNECTED) {
                removeForeground()
                cancelNotification()
            } else {
                updateNotification(call)
            }
        }
    }

    private fun removeForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun isNumberBlocked(number: String): Boolean {
        // Check block unknown callers
        val blockUnknown = prefs.getBoolean(PreferenceManager.KEY_BLOCK_UNKNOWN, false)
        if (blockUnknown && number.isBlank()) return true

        // Check block hidden numbers
        val blockHidden = prefs.getBoolean(PreferenceManager.KEY_BLOCK_HIDDEN, false)
        if (blockHidden && number.isBlank()) return true

        // Check blocked contacts list
        val blockedList = prefs.getString(PreferenceManager.KEY_BLOCKED_CONTACTS, "")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        if (blockedList.any { blocked ->
            val cleanBlocked = blocked.replace(" ", "").replace("-", "")
            val cleanNumber = number.replace(" ", "").replace("-", "")
            cleanNumber.endsWith(cleanBlocked) || cleanBlocked.endsWith(cleanNumber)
        }) return true

        return false
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        instance = this

        val handle = call.details.handle
        val number = handle?.schemeSpecificPart ?: ""

        // Block unknown callers
        val blockUnknown = prefs.getBoolean(PreferenceManager.KEY_BLOCK_UNKNOWN, false)
        if (blockUnknown && number.isBlank()) {
            call.disconnect()
            return
        }

        // Block hidden numbers
        val blockHidden = prefs.getBoolean(PreferenceManager.KEY_BLOCK_HIDDEN, false)
        if (blockHidden && (number.isBlank() || handle == null)) {
            call.disconnect()
            return
        }

        // Block specific contacts
        if (number.isNotBlank() && isNumberBlocked(number)) {
            call.disconnect()
            return
        }

        call.registerCallback(callCallback)
        _currentCallSession.value = CallSession(call, call.state)
        updateNotification(call)

        if (call.state != Call.STATE_RINGING) {
            val intent = Intent(this, CallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        if (_currentCallSession.value?.call == call) {
            _currentCallSession.value = null
        }
        instance = null
        removeForeground()
        cancelNotification()
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        _audioState.value = audioState
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ANSWER_CALL" -> answerCall()
            "DECLINE_CALL" -> declineCall()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotification(call: Call) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val handle = call.details.handle
        val number = handle?.schemeSpecificPart ?: ""

        val contact = if (number.isNotEmpty()) {
            try {
                contactsRepository.getContactByNumber(number)
            } catch (e: Exception) { null }
        } else null

        val contactName = when {
            contact != null -> contact.name
            number.isNotEmpty() -> number
            else -> "Unknown Number"
        }

        val fullScreenIntent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val answerIntent = Intent(this, CallService::class.java).apply { action = "ANSWER_CALL" }
        val answerPendingIntent = PendingIntent.getService(this, 1, answerIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val declineIntent = Intent(this, CallService::class.java).apply { action = "DECLINE_CALL" }
        val declinePendingIntent = PendingIntent.getService(this, 2, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val person = androidx.core.app.Person.Builder()
            .setName(contactName)
            .setImportant(true)
            .build()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(contactName)
            .setContentText(if (call.state == Call.STATE_RINGING) "Incoming call" else "Active call")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setSilent(call.state != Call.STATE_RINGING)
            .setDefaults(if (call.state == Call.STATE_RINGING) NotificationCompat.DEFAULT_ALL else 0)
            .setStyle(
                if (call.state == Call.STATE_RINGING) {
                    NotificationCompat.CallStyle.forIncomingCall(person, declinePendingIntent, answerPendingIntent)
                } else {
                    NotificationCompat.CallStyle.forOngoingCall(person, declinePendingIntent)
                }
            )
            .setColorized(false)

        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
