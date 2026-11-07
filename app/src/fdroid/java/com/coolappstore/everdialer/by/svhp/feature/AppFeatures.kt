package com.coolappstore.everdialer.by.svhp.feature

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.coolappstore.everdialer.by.svhp.modal.data.RecordingItem

object AppFeatures {
    val isCallRecorderAvailable: Boolean = false
    val isNetworkSwitchAvailable: Boolean = true
    val isVolumeDndAvailable: Boolean = true
    val isUpdaterAvailable: Boolean = false
    val showAndroid14WelcomeDialog: Boolean = true
    val installedBuildName: String = "Fdroid"
    val isFdroidOrPlay: Boolean = true
    val isDonationAvailable: Boolean = true
    val isPlayStore: Boolean = false

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

    fun openNetworkSwitch(context: Context) {
        try {
            context.startActivity(Intent(context, com.supernova.networkswitch.presentation.ui.activity.MainActivity::class.java))
        } catch (_: Throwable) {}
    }

    fun reloadNetworkSwitchStores(context: Context) {
        try {
            com.supernova.networkswitch.util.MasterSwitchStore.reload(context)
            com.supernova.networkswitch.util.AutomationSwitchStore.reload(context)
        } catch (_: Throwable) {}
    }

    fun updateNetworkSwitchNotification(context: Context) {
        try {
            com.supernova.networkswitch.service.AutomationService.updateNotification(context)
        } catch (_: Throwable) {}
    }

    fun isNetworkSwitchNotificationsEnabled(context: Context): Boolean =
        com.supernova.networkswitch.util.AutomationSwitchStore.areNotificationsEnabled(context)

    fun setNetworkSwitchNotificationsEnabled(context: Context, enabled: Boolean) {
        try {
            com.supernova.networkswitch.util.AutomationSwitchStore.setNotificationsEnabled(context, enabled)
            com.supernova.networkswitch.service.AutomationService.updateNotification(context)
        } catch (_: Throwable) {}
    }

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
