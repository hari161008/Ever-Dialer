package com.coolappstore.everdialer.by.svhp.feature

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.coolappstore.everdialer.by.svhp.modal.data.RecordingItem

object AppFeatures {
    val isCallRecorderAvailable: Boolean = false
    val isNetworkSwitchAvailable: Boolean = false
    val isVolumeDndAvailable: Boolean = false
    val isUpdaterAvailable: Boolean = false

    fun init(application: Application) {}

    fun onCallAnswered(context: Context) {}

    fun updateMultiCall(context: Context, number2: String, isMerged: Boolean) {}

    fun toggleManualRecording(context: Context, isCurrentlyRecording: Boolean = false) {}

    fun openRecorderMainActivity(context: Context) {}

    fun syncCallRecordingComponentGuard(context: Context) {}

    fun isRecordingNotificationsEnabled(context: Context): Boolean = false
    fun setRecordingNotificationsEnabled(context: Context, enabled: Boolean) {}
    fun isPostRecordingNotificationEnabled(context: Context): Boolean = false
    fun setPostRecordingNotificationEnabled(context: Context, enabled: Boolean) {}
    fun isShowToastsEnabled(context: Context): Boolean = false
    fun setShowToastsEnabled(context: Context, enabled: Boolean) {}

    fun openNetworkSwitch(context: Context) {}

    fun reloadNetworkSwitchStores(context: Context) {}

    fun updateNetworkSwitchNotification(context: Context) {}

    fun isNetworkSwitchNotificationsEnabled(context: Context): Boolean = false
    fun setNetworkSwitchNotificationsEnabled(context: Context, enabled: Boolean) {}

    fun checkUpdatesAndNotify(context: Context) {}

    fun scheduleUpdateCheck(context: Context) {}

    fun cancelUpdateCheck(context: Context) {}

    fun getPrivateStorageDir(context: Context): java.io.File {
        val dir = java.io.File(context.filesDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPrivateStorageAuthority(context: Context): String = "${context.packageName}.provider"

    fun getRecordingFolderUri(context: Context): android.net.Uri? = null

    fun ensurePrivateStorageMode(context: Context) {}

    @Composable
    fun rememberRecordings(): State<List<RecordingItem>> {
        return remember { mutableStateOf(emptyList()) }
    }
}
