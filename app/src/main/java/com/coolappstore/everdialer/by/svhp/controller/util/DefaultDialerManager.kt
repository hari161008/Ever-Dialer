package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.result.ActivityResultLauncher

object DefaultDialerManager {

    /**
     * Checks whether Ever Dialer is currently the default dialer.
     * Guaranteed to never throw an exception or crash.
     */
    fun isDefaultDialer(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    return roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
                }
            }
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.defaultDialerPackage == context.packageName
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Creates an intent to request that Ever Dialer be set as the default dialer.
     * Returns null if no valid intent could be constructed.
     */
    fun createDefaultDialerIntent(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    return roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                }
            }
            // Fallback for API 26-28 or when RoleManager is unavailable
            @Suppress("DEPRECATION")
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            }
        } catch (_: Throwable) {
            try {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * Safely prompts the user to make Ever Dialer the default dialer using an ActivityResultLauncher.
     * Returns true if an intent was successfully launched.
     */
    fun requestDefaultDialer(launcher: ActivityResultLauncher<Intent>, context: Context): Boolean {
        val intent = createDefaultDialerIntent(context) ?: return false
        return try {
            launcher.launch(intent)
            true
        } catch (_: Throwable) {
            // If the specific role/telecom intent cannot be handled, fallback to default apps settings
            try {
                val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (_: Throwable) {
                false
            }
        }
    }
}
