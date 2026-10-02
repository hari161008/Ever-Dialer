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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import androidx.core.app.NotificationCompat
import com.coolappstore.everdialer.by.svhp.MainActivity
import com.coolappstore.everdialer.by.svhp.R
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
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

                // Resolve contact name and photo for notification
                val (contactName, photoUri) = resolveContact(context, address)
                showSmsNotification(context, address, contactName, photoUri, fullBody)
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
        message: String
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

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notif_sms)
            .setContentTitle(displayName)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (avatarBitmap != null) {
            notificationBuilder.setLargeIcon(avatarBitmap)
        }

        nm.notify(sender.hashCode(), notificationBuilder.build())
    }
}
