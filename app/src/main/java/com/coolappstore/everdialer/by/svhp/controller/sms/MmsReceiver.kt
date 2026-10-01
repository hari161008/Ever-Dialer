package com.coolappstore.everdialer.by.svhp.controller.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver required by Android to qualify as the default SMS app.
 * Handles MMS WAP push messages.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // MMS WAP push handling stub required by Android Telephony specification
    }
}
