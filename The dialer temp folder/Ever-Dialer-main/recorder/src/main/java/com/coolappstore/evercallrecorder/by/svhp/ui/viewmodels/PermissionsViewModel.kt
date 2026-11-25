/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels

import android.Manifest
import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel

import com.coolappstore.evercallrecorder.by.svhp.integrations.shizuku.ShizukuConnectionManager
import com.coolappstore.evercallrecorder.by.svhp.onboarding.OnboardingStatus
import com.coolappstore.evercallrecorder.by.svhp.system.openAppSettings
import com.coolappstore.evercallrecorder.by.svhp.system.openShizukuManager
import com.coolappstore.evercallrecorder.by.svhp.ui.screens.PermissionsScreen

/**
 * The "Brain" of the permissions setup flow.
 *
 * This ViewModel decides which action to take based on the current [OnboardingStatus.Status].
 */
class PermissionsViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Application context — safe to store in a ViewModel because it lives as long as the
     * app process, unlike an Activity context which is destroyed and recreated on every rotation.
     */
    private val appContext = application.applicationContext

    /**
     * Works through each missing setup step in the correct order and invokes the matching
     * callback. Once all steps are complete, calls [onPermissionGranted] so the UI can refresh.
     *
     * For each runtime permission:
     *  - First press → the system permission dialog is shown via [requestRuntimePermission].
     *  - If the OS cannot show the popup (permanent denial), [PermissionsScreen] handles the
     *    fallback by calling [openAppSettings] in the launcher result callback.
     *
     * @param status                   Current state of every permission and setup step.
     * @param requestRuntimePermission Launches the system permission dialog for a given permission.
     * @param showStorageChoice        Shows the popup letting the user pick between a SAF folder or private app storage.
     * @param onPermissionGranted      Called after any step completes so the UI can refresh.
     */
    fun onGrantAccess(
        status: OnboardingStatus.Status,
        requestRuntimePermission: (String) -> Unit,
        showStorageChoice: () -> Unit,
        onPermissionGranted: () -> Unit
    ) {
        when {
            !status.shizukuRunning           -> appContext.openShizukuManager()
            !status.shizukuPermissionGranted -> ShizukuConnectionManager.requestPermission()
            !status.notificationsGranted     -> requestRuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
            !status.contactsGranted          -> requestRuntimePermission(Manifest.permission.READ_CONTACTS)
            !status.phoneStateGranted        -> requestRuntimePermission(Manifest.permission.READ_PHONE_STATE)
            !status.callLogGranted           -> requestRuntimePermission(Manifest.permission.READ_CALL_LOG)
            !status.batteryExempted          -> {
                appContext.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${appContext.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            !status.storageSelected          -> showStorageChoice()
            else                             -> { /* All steps completed, all permission granted.*/ }
        }
        // Always trigger a refresh of the UI to detect and show new permission changes.
        onPermissionGranted()
    }
}