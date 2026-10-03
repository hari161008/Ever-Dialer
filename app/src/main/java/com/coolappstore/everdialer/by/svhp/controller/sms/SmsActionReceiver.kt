package com.coolappstore.everdialer.by.svhp.controller.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.coolappstore.everdialer.by.svhp.modal.`interface`.ISmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class SmsActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_READ = "com.coolappstore.everdialer.SMS_MARK_READ"
        const val ACTION_DELETE = "com.coolappstore.everdialer.SMS_DELETE"
        const val ACTION_REPLY = "com.coolappstore.everdialer.SMS_REPLY"

        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_ADDRESS = "address"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
        val messageId = intent.getLongExtra(EXTRA_MESSAGE_ID, -1L)
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: ""

        val nm = NotificationManagerCompat.from(context)
        if (notifId != 0) {
            nm.cancel(notifId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_MARK_READ -> {
                        if (threadId > 0) {
                            try {
                                val smsRepo = GlobalContext.get().get<ISmsRepository>()
                                smsRepo.markThreadAsRead(threadId)
                            } catch (_: Throwable) {
                                val values = ContentValues().apply {
                                    put(Telephony.Sms.READ, 1)
                                }
                                context.contentResolver.update(
                                    Telephony.Sms.Inbox.CONTENT_URI,
                                    values,
                                    "${Telephony.Sms.THREAD_ID} = ?",
                                    arrayOf(threadId.toString())
                                )
                            }
                        }
                        notifyChanges(context, threadId)
                    }
                    ACTION_DELETE -> {
                        if (messageId > 0) {
                            context.contentResolver.delete(
                                Telephony.Sms.CONTENT_URI,
                                "${Telephony.Sms._ID} = ?",
                                arrayOf(messageId.toString())
                            )
                        } else if (threadId > 0) {
                            context.contentResolver.delete(
                                Telephony.Sms.CONTENT_URI,
                                "${Telephony.Sms.THREAD_ID} = ?",
                                arrayOf(threadId.toString())
                            )
                        }
                        notifyChanges(context, threadId)
                    }
                    ACTION_REPLY -> {
                        val remoteInput = RemoteInput.getResultsFromIntent(intent)
                        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()
                        if (!replyText.isNullOrBlank() && address.isNotBlank()) {
                            try {
                                val smsRepo = GlobalContext.get().get<ISmsRepository>()
                                smsRepo.sendSms(address, replyText, null)
                                if (threadId > 0) {
                                    smsRepo.markThreadAsRead(threadId)
                                }
                            } catch (_: Throwable) {}
                            notifyChanges(context, threadId)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun notifyChanges(context: Context, threadId: Long) {
        try {
            context.contentResolver.notifyChange(Telephony.Sms.CONTENT_URI, null)
            context.contentResolver.notifyChange(Telephony.Sms.Inbox.CONTENT_URI, null)
            context.contentResolver.notifyChange(Uri.parse("content://mms-sms/conversations"), null)
            if (threadId > 0) {
                context.contentResolver.notifyChange(Uri.parse("content://mms-sms/conversations/$threadId"), null)
            }
        } catch (_: Throwable) {}
        SmsEventBus.notifyNewSms(threadId)
    }
}
