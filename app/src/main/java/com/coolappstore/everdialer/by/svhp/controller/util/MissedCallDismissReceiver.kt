package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MissedCallDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        MissedCallBadgeManager.markMissedCallsAsRead(context)
    }
}
