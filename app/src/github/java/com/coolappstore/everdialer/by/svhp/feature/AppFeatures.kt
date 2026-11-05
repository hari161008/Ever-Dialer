package com.coolappstore.everdialer.by.svhp.feature

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.onboarding.OnboardingSession
import com.coolappstore.evercallrecorder.by.svhp.services.call.CallRecordingComponentGuard
import com.coolappstore.evercallrecorder.by.svhp.services.call.CallSessionManager
import com.coolappstore.evercallrecorder.by.svhp.services.recording.RecordingForegroundService
import com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels.HomeViewModel
import com.coolappstore.evercallrecorder.by.svhp.utils.AppLogger
import com.coolappstore.everdialer.by.svhp.controller.util.UpdateNotificationManager
import com.coolappstore.everdialer.by.svhp.modal.data.RecordingItem

object AppFeatures {
    val isCallRecorderAvailable: Boolean = true
    val isNetworkSwitchAvailable: Boolean = true
    val isVolumeDndAvailable: Boolean = true
    val isUpdaterAvailable: Boolean = true

    fun init(application: Application) {
        try {
            AppLogger.init(application.applicationContext)
            OnboardingSession.skipped = false
            CallRecordingComponentGuard.sync(application.applicationContext)
        } catch (_: Throwable) {}
    }

    fun onCallAnswered(context: Context) {
        try {
            CallSessionManager.getInstance(context).notifyCallAnsweredInDialer()
        } catch (_: Throwable) {}
    }

    fun updateMultiCall(context: Context, number2: String, isMerged: Boolean) {
        try {
            CallSessionManager.getInstance(context).updateMultiCall(number2, isMerged)
        } catch (_: Throwable) {}
    }

    fun toggleManualRecording(context: Context, isCurrentlyRecording: Boolean = false) {
        try {
            val action = if (isCurrentlyRecording) {
                RecordingForegroundService.ACTION_STOP_RECORDING
            } else {
                RecordingForegroundService.ACTION_MANUAL_START
            }
            context.startService(Intent(context, RecordingForegroundService::class.java).setAction(action))
        } catch (_: Throwable) {}
    }

    fun openRecorderMainActivity(context: Context) {
        try {
            context.startActivity(Intent(context, com.coolappstore.evercallrecorder.by.svhp.MainActivity::class.java))
        } catch (_: Throwable) {}
    }

    fun syncCallRecordingComponentGuard(context: Context) {
        try {
            CallRecordingComponentGuard.sync(context)
        } catch (_: Throwable) {}
    }

    // Recorder notifications preferences
    fun isRecordingNotificationsEnabled(context: Context): Boolean = AppPreferences(context).isRecordingNotificationsEnabled()
    fun setRecordingNotificationsEnabled(context: Context, enabled: Boolean) = AppPreferences(context).setRecordingNotificationsEnabled(enabled)
    fun isPostRecordingNotificationEnabled(context: Context): Boolean = AppPreferences(context).isPostRecordingFileActionsNotificationEnabled()
    fun setPostRecordingNotificationEnabled(context: Context, enabled: Boolean) = AppPreferences(context).setPostRecordingFileActionsNotificationEnabled(enabled)
    fun isShowToastsEnabled(context: Context): Boolean = AppPreferences(context).isShowToastsEnabled()
    fun setShowToastsEnabled(context: Context, enabled: Boolean) = AppPreferences(context).setShowToastsEnabled(enabled)

    // Network switcher
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

    fun isNetworkSwitchNotificationsEnabled(context: Context): Boolean = com.supernova.networkswitch.util.AutomationSwitchStore.areNotificationsEnabled(context)
    fun setNetworkSwitchNotificationsEnabled(context: Context, enabled: Boolean) {
        try {
            com.supernova.networkswitch.util.AutomationSwitchStore.setNotificationsEnabled(context, enabled)
            com.supernova.networkswitch.service.AutomationService.updateNotification(context)
        } catch (_: Throwable) {}
    }

    // Updater
    fun checkUpdatesAndNotify(context: Context) {
        try {
            UpdateNotificationManager.checkUpdatesAndNotify(context)
        } catch (_: Throwable) {}
    }

    fun scheduleUpdateCheck(context: Context) {
        try {
            UpdateNotificationManager.scheduleUpdateCheck(context)
        } catch (_: Throwable) {}
    }

    fun cancelUpdateCheck(context: Context) {
        try {
            UpdateNotificationManager.cancelUpdateCheck(context)
        } catch (_: Throwable) {}
    }

    fun getPrivateStorageDir(context: Context): java.io.File =
        com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper.getPrivateStorageDir(context)

    fun getPrivateStorageAuthority(context: Context): String =
        com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper.getPrivateStorageAuthority(context)

    fun getRecordingFolderUri(context: Context): android.net.Uri? =
        AppPreferences(context).getRecordingFolderUri()

    fun ensurePrivateStorageMode(context: Context) {
        try {
            val prefs = AppPreferences(context)
            if (prefs.getStorageMode() == null) {
                prefs.setStorageMode(AppPreferences.StorageMode.PRIVATE)
            }
        } catch (_: Throwable) {}
    }

    @Composable
    fun rememberRecordings(): State<List<RecordingItem>> {
        val vm: HomeViewModel = viewModel()
        val rawState = vm.allRecordings.collectAsState()
        return androidx.compose.runtime.remember(rawState.value) {
            val list = rawState.value.map { item ->
                RecordingItem(
                    uri = item.uri,
                    displayName = item.displayName,
                    phoneNumber = item.phoneNumber,
                    contactName = item.contactName,
                    direction = item.direction,
                    date = item.date,
                    sizeBytes = item.sizeBytes,
                    durationMs = item.durationMs,
                    extension = item.extension,
                    isFavourite = item.isFavourite,
                    noteText = item.noteText,
                    contactId = item.contactId,
                    photoUri = item.photoUri,
                    secondPhoneNumber = item.secondPhoneNumber,
                    secondContactName = item.secondContactName,
                    secondPhotoUri = item.secondPhotoUri,
                    secondContactId = item.secondContactId,
                    isMultiCall = item.isMultiCall,
                    isMergedCall = item.isMergedCall
                )
            }
            androidx.compose.runtime.mutableStateOf(list)
        }
    }
}
