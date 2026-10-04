package com.coolappstore.everdialer.by.svhp.controller.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
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

                var threadId = try {
                    Telephony.Threads.getOrCreateThreadId(context, address)
                } catch (_: Throwable) {
                    -1L
                }

                var insertedMessageId = -1L
                if (!alreadyExists) {
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
                    val insertedUri = try {
                        context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                    } catch (_: Throwable) {
                        null
                    }
                    insertedMessageId = try {
                        insertedUri?.lastPathSegment?.toLongOrNull() ?: -1L
                    } catch (_: Throwable) {
                        -1L
                    }
                }

                // Explicitly notify content resolver so all observers pick it up
                try {
                    context.contentResolver.notifyChange(Telephony.Sms.CONTENT_URI, null)
                    context.contentResolver.notifyChange(Telephony.Sms.Inbox.CONTENT_URI, null)
                    context.contentResolver.notifyChange(Uri.parse("content://mms-sms/conversations"), null)
                    if (threadId > 0) {
                        context.contentResolver.notifyChange(Uri.parse("content://mms-sms/conversations/$threadId"), null)
                    }
                } catch (_: Throwable) {}

                // Broadcast in-process event to instantly update active screens
                SmsEventBus.notifyNewSms(if (threadId > 0) threadId else null)

                // Resolve contact name and photo for notification
                val prefs = PreferenceManager(context)
                val isBlocked = com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager.isBlocked(context, prefs, address)
                val isMuted = prefs.isSmsMuted(address)

                if (!isBlocked && !isMuted) {
                    val (contactName, photoUri) = resolveContact(context, address)
                    showSmsNotification(
                        context = context,
                        sender = address,
                        contactName = contactName,
                        photoUri = photoUri,
                        message = fullBody,
                        threadId = threadId,
                        messageId = insertedMessageId,
                        timestamp = timestamp
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun resolveContact(context: Context, phoneNumber: String): Pair<String?, String?> {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI),
                null,
                null,
                null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0)
                    val photo = c.getString(1)
                    Pair(name, photo)
                } else Pair(null, null)
            } ?: Pair(null, null)
        } catch (_: Throwable) {
            Pair(null, null)
        }
    }

    private fun getCircularBitmap(src: Bitmap): Bitmap {
        val size = Math.min(src.width, src.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val left = (size - src.width) / 2f
        val top = (size - src.height) / 2f
        canvas.drawBitmap(src, left, top, paint)
        return output
    }

    private fun createAvatarBitmap(context: Context, displayName: String, autoColor: Boolean): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = (64 * density).toInt().coerceAtLeast(128)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = if (autoColor) {
            val colors = intArrayOf(
                0xFFC62828.toInt(), 0xFFAD1457.toInt(), 0xFF6A1B9A.toInt(), 0xFF4527A0.toInt(),
                0xFF283593.toInt(), 0xFF1565C0.toInt(), 0xFF0277BD.toInt(), 0xFF00838F.toInt(),
                0xFF00695C.toInt(), 0xFF2E7D32.toInt(), 0xFF558B2F.toInt(), 0xFF9E9D24.toInt(),
                0xFFF9A825.toInt(), 0xFFFF8F00.toInt(), 0xFFE65100.toInt(), 0xFFBF360C.toInt()
            )
            val key = displayName.ifBlank { "unknown" }
            colors[Math.abs(key.hashCode()) % colors.size]
        } else {
            0xFF5C5B60.toInt()
        }

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

        val firstChar = displayName.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: ""
        if (firstChar.isNotEmpty()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = size * 0.45f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(firstChar, size / 2f, yPos, textPaint)
        }
        return bitmap
    }

    private fun showSmsNotification(
        context: Context,
        sender: String,
        contactName: String?,
        photoUri: String?,
        message: String,
        threadId: Long,
        messageId: Long,
        timestamp: Long
    ) {
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

        val notifId = if (threadId > 0) threadId.toInt() else sender.hashCode()
        val displayName = contactName ?: sender

        // 1. Content PendingIntent -> Opens conversation in Ever Dialer
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("sms_address", sender)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // 2. Avatar bitmap (circular, displayed on the left side)
        val avatarBitmap = try {
            if (!photoUri.isNullOrBlank()) {
                val uri = Uri.parse(photoUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val raw = BitmapFactory.decodeStream(stream)
                    if (raw != null) getCircularBitmap(raw) else null
                }
            } else null
        } catch (_: Throwable) {
            null
        } ?: run {
            val prefs = PreferenceManager(context)
            val autoColor = prefs.getBoolean(PreferenceManager.KEY_SMS_AUTO_COLOR_AVATARS, true)
            createAvatarBitmap(context, displayName, autoColor)
        }

        // 3. Pending intents for actions: Reply, Mark Read, Delete
        // Mark Read Action
        val markReadIntent = Intent(context, SmsActionReceiver::class.java).apply {
            action = SmsActionReceiver.ACTION_MARK_READ
            putExtra(SmsActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(SmsActionReceiver.EXTRA_THREAD_ID, threadId)
            putExtra(SmsActionReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(SmsActionReceiver.EXTRA_ADDRESS, sender)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            context,
            (sender + "_read").hashCode(),
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Delete Action
        val deleteIntent = Intent(context, SmsActionReceiver::class.java).apply {
            action = SmsActionReceiver.ACTION_DELETE
            putExtra(SmsActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(SmsActionReceiver.EXTRA_THREAD_ID, threadId)
            putExtra(SmsActionReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(SmsActionReceiver.EXTRA_ADDRESS, sender)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            (sender + "_delete").hashCode(),
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Inline Reply Action (RemoteInput)
        val replyRemoteInput = RemoteInput.Builder(SmsActionReceiver.KEY_TEXT_REPLY)
            .setLabel("Reply to $displayName...")
            .build()

        val replyBroadcastIntent = Intent(context, SmsActionReceiver::class.java).apply {
            action = SmsActionReceiver.ACTION_REPLY
            putExtra(SmsActionReceiver.EXTRA_NOTIFICATION_ID, notifId)
            putExtra(SmsActionReceiver.EXTRA_THREAD_ID, threadId)
            putExtra(SmsActionReceiver.EXTRA_MESSAGE_ID, messageId)
            putExtra(SmsActionReceiver.EXTRA_ADDRESS, sender)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            (sender + "_reply").hashCode(),
            replyBroadcastIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT)
        )

        // Action buttons for NotificationCompat
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_reply,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(replyRemoteInput).build()

        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_mark_read,
            "Mark Read",
            markReadPendingIntent
        ).build()

        val deleteAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notif_delete,
            "Delete",
            deletePendingIntent
        ).build()

        // 5. Native Material You MessagingStyle
        val userPerson = Person.Builder()
            .setName("Me")
            .build()

        val senderPersonBuilder = Person.Builder()
            .setName(displayName)
            .setKey(sender)
        if (avatarBitmap != null) {
            senderPersonBuilder.setIcon(IconCompat.createWithBitmap(avatarBitmap))
        }
        val senderPerson = senderPersonBuilder.build()

        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(null)
            .addMessage(
                NotificationCompat.MessagingStyle.Message(
                    message,
                    timestamp,
                    senderPerson
                )
            )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notif_sms)
            .setStyle(messagingStyle)
            .setContentTitle(displayName)
            .setContentText(message)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(replyAction)
            .addAction(markReadAction)
            .addAction(deleteAction)

        if (avatarBitmap != null) {
            notificationBuilder.setLargeIcon(avatarBitmap)
        }

        nm.notify(notifId, notificationBuilder.build())
    }
}
