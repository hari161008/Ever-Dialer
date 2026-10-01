package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.result.ActivityResultLauncher

object DefaultSmsManager {

    /**
     * Checks whether Ever Dialer is currently the default SMS app.
     * Guaranteed to never throw an exception or crash.
     */
    fun isDefaultSms(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    return roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                }
            }
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Creates an intent to request that Ever Dialer be set as the default SMS app.
     * Returns null if no valid intent could be constructed.
     */
    fun createDefaultSmsIntent(context: Context): Intent? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    return roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                }
            }
            // Fallback for API 26-28 or when RoleManager is unavailable
            @Suppress("DEPRECATION")
            Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
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
     * Safely prompts the user to make Ever Dialer the default SMS app using an ActivityResultLauncher.
     * Returns true if an intent was successfully launched.
     */
    fun requestDefaultSms(launcher: ActivityResultLauncher<Intent>, context: Context): Boolean {
        val intent = createDefaultSmsIntent(context) ?: return false
        return try {
            launcher.launch(intent)
            true
        } catch (_: Throwable) {
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
