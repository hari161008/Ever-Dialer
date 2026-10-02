package com.coolappstore.everdialer.by.svhp.controller.sms

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Service required by Android to qualify as the default SMS app.
 * Handles the ACTION_RESPOND_VIA_MESSAGE intent for quick responses (e.g. from lock screen, Android Auto, or Bluetooth).
 */
class HeadlessSmsSendService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action != TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val dataUri = intent.data

        if (text.isNullOrBlank() || dataUri == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val recipients = extractRecipients(dataUri)
        if (recipients.isEmpty()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        scope.launch {
            try {
                @Suppress("DEPRECATION")
                val smsManager = SmsManager.getDefault()
                for (recipient in recipients) {
                    val trimmed = recipient.trim()
                    if (trimmed.isBlank()) continue

                    val parts = smsManager.divideMessage(text)
                    if (parts.size > 1) {
                        smsManager.sendMultipartTextMessage(trimmed, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(trimmed, null, text, null, null)
                    }

                    // Save to provider
                    val threadId = try {
                        Telephony.Threads.getOrCreateThreadId(applicationContext, trimmed)
                    } catch (_: Throwable) { null }

                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, trimmed)
                        put(Telephony.Sms.BODY, text)
                        put(Telephony.Sms.DATE, System.currentTimeMillis())
                        put(Telephony.Sms.READ, 1)
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                        if (threadId != null && threadId > 0) {
                            put(Telephony.Sms.THREAD_ID, threadId)
                        }
                    }
                    contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
                }
            } catch (_: Throwable) {
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    private fun extractRecipients(uri: Uri): List<String> {
        val base = uri.schemeSpecificPart ?: return emptyList()
        val pos = base.indexOf('?')
        val clean = if (pos >= 0) base.substring(0, pos) else base
        return clean.split(';', ',').map { it.trim() }.filter { it.isNotBlank() }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
