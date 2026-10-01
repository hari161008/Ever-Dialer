package com.coolappstore.everdialer.by.svhp.controller.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Service required by Android to qualify as the default SMS app.
 * Handles the ACTION_RESPOND_VIA_MESSAGE intent for quick responses (e.g. from lock screen or Bluetooth).
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
