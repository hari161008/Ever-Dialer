package com.coolappstore.everdialer.by.svhp.controller.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            return
        }

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (_: Throwable) {
            return
        }

        if (messages.isNullOrEmpty()) return

        val address = messages[0].displayOriginatingAddress ?: messages[0].originatingAddress ?: "Unknown"
        val timestamp = messages[0].timestampMillis
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            bodyBuilder.append(msg.displayMessageBody ?: msg.messageBody ?: "")
        }
        val fullBody = bodyBuilder.toString()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if this identical message has already been written (deduplication)
                var alreadyExists = false
                try {
                    context.contentResolver.query(
                        Telephony.Sms.Inbox.CONTENT_URI,
                        arrayOf(Telephony.Sms._ID),
                        "${Telephony.Sms.ADDRESS} = ? AND ${Telephony.Sms.BODY} = ? AND ${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?",
                        arrayOf(address, fullBody, (timestamp - 4000L).toString(), (timestamp + 4000L).toString()),
                        null
                    )?.use { cursor ->
                        if (cursor.count > 0) {
                            alreadyExists = true
                        }
                    }
                } catch (_: Throwable) {}

                if (!alreadyExists) {
                    val threadId = try {
                        Telephony.Threads.getOrCreateThreadId(context, address)
                    } catch (_: Throwable) {
                        -1L
                    }

                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, address)
                        put(Telephony.Sms.BODY, fullBody)
                        put(Telephony.Sms.DATE, timestamp)
                        put(Telephony.Sms.READ, 0)
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                        if (threadId > 0) {
                            put(Telephony.Sms.THREAD_ID, threadId)
                        }
                    }
                    try {
                        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                    } catch (_: Throwable) {}
                }

                // Resolve contact name for notification
                val contactName = resolveContactName(context, address)
                showSmsNotification(context, address, contactName, fullBody)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun resolveContactName(context: Context, phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    c.getString(0)
                } else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun showSmsNotification(context: Context, sender: String, contactName: String?, message: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channelId = "ever_sms_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SMS Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming SMS messages"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("sms_address", sender)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            sender.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val displayName = contactName ?: sender
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notif_sms)
            .setContentTitle(displayName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        nm.notify(sender.hashCode(), notification)
    }
}
