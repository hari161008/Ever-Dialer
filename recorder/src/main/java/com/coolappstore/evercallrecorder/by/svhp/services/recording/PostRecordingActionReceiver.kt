/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.services.recording

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper
import com.coolappstore.evercallrecorder.by.svhp.utils.AppLogger

/**
 * Handles the "Delete" quick action on the post-recording notification (see
 * [RecordingNotificationHelper.showPostCallNotification]), so deleting a recording doesn't
 * require opening any app UI.
 */
class PostRecordingActionReceiver : BroadcastReceiver() {

    private companion object {
        const val TAG = "SCR:PostRecordingAction"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RecordingNotificationHelper.ACTION_DELETE_RECORDING) return

        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(RecordingNotificationHelper.EXTRA_RECORDING_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(RecordingNotificationHelper.EXTRA_RECORDING_URI)
        }
        val notificationId = intent.getIntExtra(RecordingNotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        if (uri != null) {
            try {
                SafHelper.deleteRecording(context.applicationContext, uri)
                AppLogger.i(TAG, "Deleted recording via post-call notification action: $uri")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to delete recording via post-call notification action", e)
            }
        }

        if (notificationId != -1) {
            context.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
        }
    }
}
