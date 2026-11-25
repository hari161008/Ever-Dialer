package com.modiprincekr.thedialer.controller

import android.app.Notification
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.controller.util.WHATSAPP_PACKAGES
import com.modiprincekr.thedialer.controller.util.WhatsAppCallRepository
import com.modiprincekr.thedialer.modal.data.WhatsAppCallRecord
import java.util.concurrent.ConcurrentHashMap

class WhatsAppCallListenerService : NotificationListenerService() {

    private data class CallSession(
        val key: String,
        val callerName: String,
        val isVideo: Boolean,
        val sourcePackage: String,
        val startTime: Long,
        var connectedTime: Long? = null,
        var callType: Int = CallLog.Calls.INCOMING_TYPE,
        var isMissed: Boolean = false
    )

    private data class ContactInfo(
        val contactId: String?,
        val displayName: String,
        val phoneNumber: String?,
        val photoUri: String?
    )

    companion object {
        private val activeSessions = ConcurrentHashMap<String, CallSession>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg !in WHATSAPP_PACKAGES) return

        val prefs = PreferenceManager(applicationContext)
        if (!prefs.getBoolean(PreferenceManager.KEY_SHOW_WHATSAPP_CALLS, false)) return

        val notification = sbn.notification ?: return
        if (!isCallNotification(notification)) return

        val extras = notification.extras ?: return
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        val rawSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim() ?: ""

        val callerName = extractCallerName(rawTitle, rawText)
        if (callerName.isBlank()) return

        val isVideo = detectIsVideo(notification, rawTitle, rawText, rawSubText, sbn.notification.tickerText?.toString())

        val actions = notification.actions ?: emptyArray()
        val actionTitles = actions.mapNotNull { it.title?.toString()?.lowercase() }

        val isMissed = isMissedCall(notification, rawTitle, rawText, actionTitles)

        if (isMissed) {
            // Missed call detected
            val contact = resolveContact(this, callerName)
            val record = WhatsAppCallRecord(
                callerName = contact.displayName.ifBlank { callerName },
                phoneNumber = contact.phoneNumber,
                timestamp = if (sbn.postTime > 0L) sbn.postTime else System.currentTimeMillis(),
                durationSeconds = 0L,
                callType = CallLog.Calls.MISSED_TYPE,
                isVideo = isVideo,
                sourcePackage = pkg,
                photoUri = contact.photoUri,
                contactId = contact.contactId
            )
            WhatsAppCallRepository.addCallRecord(this, record)
            activeSessions.remove(sbn.key)
            activeSessions.remove(callerName)
            return
        }

        // Check if ongoing / active call
        val hasAnswerAction = actionTitles.any { it.contains("answer") }
        val isOngoing = (sbn.isOngoing || (notification.flags and Notification.FLAG_ONGOING_EVENT != 0)) && !hasAnswerAction

        val existing = activeSessions[sbn.key] ?: activeSessions[callerName]
        if (isOngoing) {
            val now = System.currentTimeMillis()
            if (existing != null) {
                if (existing.connectedTime == null) {
                    existing.connectedTime = now
                }
            } else {
                val session = CallSession(
                    key = sbn.key,
                    callerName = callerName,
                    isVideo = isVideo,
                    sourcePackage = pkg,
                    startTime = if (sbn.postTime > 0L) sbn.postTime else now,
                    connectedTime = now,
                    callType = CallLog.Calls.INCOMING_TYPE
                )
                activeSessions[sbn.key] = session
                activeSessions[callerName] = session
            }
        } else {
            // Ringing / incoming call
            val now = System.currentTimeMillis()
            val session = CallSession(
                key = sbn.key,
                callerName = callerName,
                isVideo = isVideo,
                sourcePackage = pkg,
                startTime = if (sbn.postTime > 0L) sbn.postTime else now,
                connectedTime = null,
                callType = CallLog.Calls.INCOMING_TYPE
            )
            activeSessions[sbn.key] = session
            activeSessions[callerName] = session
        }

        // Prune stale sessions older than 4 hours
        pruneStaleSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        if (pkg !in WHATSAPP_PACKAGES) return

        val prefs = PreferenceManager(applicationContext)
        if (!prefs.getBoolean(PreferenceManager.KEY_SHOW_WHATSAPP_CALLS, false)) return

        val session = activeSessions.remove(sbn.key) ?: run {
            // Check if any session has matching key
            val entry = activeSessions.entries.firstOrNull { it.value.key == sbn.key }
            if (entry != null) {
                activeSessions.remove(entry.key)
                entry.value
            } else null
        } ?: return

        activeSessions.remove(session.callerName)

        if (!session.isMissed) {
            val now = System.currentTimeMillis()
            val durationSeconds = if (session.connectedTime != null) {
                ((now - session.connectedTime!!) / 1000L).coerceAtLeast(1L)
            } else {
                0L
            }

            val finalType = if (durationSeconds > 0L) {
                session.callType
            } else {
                CallLog.Calls.MISSED_TYPE
            }

            val contact = resolveContact(this, session.callerName)
            val record = WhatsAppCallRecord(
                callerName = contact.displayName.ifBlank { session.callerName },
                phoneNumber = contact.phoneNumber,
                timestamp = session.startTime,
                durationSeconds = durationSeconds,
                callType = finalType,
                isVideo = session.isVideo,
                sourcePackage = session.sourcePackage,
                photoUri = contact.photoUri,
                contactId = contact.contactId
            )
            WhatsAppCallRepository.addCallRecord(this, record)
        }
    }

    private fun isCallNotification(notification: Notification): Boolean {
        val category = notification.category
        if (category == Notification.CATEGORY_CALL || category == "missed_call") {
            return true
        }

        // Check CallStyle template (Android 12+)
        val template = notification.extras.getString(Notification.EXTRA_TEMPLATE) ?: ""
        if (template.contains("CallStyle", ignoreCase = true)) {
            return true
        }

        // Strictly ignore chat / messaging style notifications
        if (category == Notification.CATEGORY_MESSAGE ||
            notification.extras.containsKey("android.messagingStyleUser") ||
            notification.extras.containsKey(Notification.EXTRA_CONVERSATION_TITLE)
        ) {
            return false
        }

        // Check actions for call-specific action words
        val actions = notification.actions
        if (actions != null && actions.isNotEmpty()) {
            val titles = actions.mapNotNull { it.title?.toString()?.lowercase() }
            val hasCallAction = titles.any {
                it.contains("answer") || it.contains("decline") ||
                it.contains("call back") || it.contains("end call") ||
                it.contains("reject") || it.contains("hang up")
            }
            if (hasCallAction) return true
        }

        // Check text for missed call indicators
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.lowercase() ?: ""
        if (text.contains("missed voice call") || text.contains("missed video call") || text.contains("missed call")) {
            return true
        }

        return false
    }

    private fun extractCallerName(title: String, text: String): String {
        return when {
            title.isNotBlank() && !title.equals("WhatsApp", ignoreCase = true) && !title.equals("WhatsApp Business", ignoreCase = true) -> title
            text.isNotBlank() && !text.contains("call", ignoreCase = true) -> text
            else -> title.ifBlank { text.ifBlank { "" } }
        }
    }

    private fun detectIsVideo(
        notification: Notification,
        title: String,
        text: String,
        subText: String,
        ticker: String?
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (notification.extras.getBoolean(Notification.EXTRA_CALL_IS_VIDEO, false)) {
                return true
            }
        }
        val combined = "$title $text $subText ${ticker ?: ""}".lowercase()
        return combined.contains("video call") || combined.contains("video")
    }

    private fun isMissedCall(
        notification: Notification,
        title: String,
        text: String,
        actionTitles: List<String>
    ): Boolean {
        if (notification.category == "missed_call") return true
        val combined = "$title $text".lowercase()
        if (combined.contains("missed call") || combined.contains("missed voice call") || combined.contains("missed video call") || combined.contains("missed")) {
            return true
        }
        if (actionTitles.any { it.contains("call back") } && actionTitles.none { it.contains("decline") || it.contains("answer") }) {
            return true
        }
        return false
    }

    private fun resolveContact(context: Context, caller: String): ContactInfo {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val cleanDigits = caller.takeIf { it.any { c -> c.isDigit() } }
            return ContactInfo(null, caller, cleanDigits, null)
        }

        try {
            // 1. Try matching by display name
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?",
                arrayOf(caller),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val contactId = cursor.getString(0)
                    val name = cursor.getString(1) ?: caller
                    val number = cursor.getString(2)
                    val photo = cursor.getString(3)
                    return ContactInfo(contactId, name, number, photo)
                }
            }

            // 2. If caller is a phone number, search via PhoneLookup
            val digits = caller.filter { it.isDigit() }
            if (digits.length >= 7) {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(caller))
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        ContactsContract.PhoneLookup._ID,
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.PhoneLookup.NUMBER,
                        ContactsContract.PhoneLookup.PHOTO_URI
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val contactId = cursor.getString(0)
                        val name = cursor.getString(1) ?: caller
                        val number = cursor.getString(2) ?: caller
                        val photo = cursor.getString(3)
                        return ContactInfo(contactId, name, number, photo)
                    }
                }
            }
        } catch (_: Exception) {}

        val cleanDigits = caller.takeIf { it.any { c -> c.isDigit() } }
        return ContactInfo(null, caller, cleanDigits, null)
    }

    private fun pruneStaleSessions() {
        val cutoff = System.currentTimeMillis() - 4 * 60 * 60 * 1000L
        val iterator = activeSessions.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.startTime < cutoff) {
                iterator.remove()
            }
        }
    }
}
