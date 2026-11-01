package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MissedCallDismissReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_CALLER_KEY = "extra_missed_caller_key"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val callerKey = intent?.getStringExtra(EXTRA_CALLER_KEY)
        if (!callerKey.isNullOrBlank()) {
            MissedCallBadgeManager.dismissCallerNotification(context, callerKey)
        } else {
            MissedCallBadgeManager.markMissedCallsAsRead(context)
        }
    }
}
