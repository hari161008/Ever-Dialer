package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.BlockedNumberContract

/**
 * Single source of truth for Ever Dialer's number-blocking feature.
 *
 * Fully integrated with Android's system-wide [BlockedNumberContract] provider:
 * - System-wide blocked numbers (from Android settings, Google Phone, Messages, etc.) are read and displayed.
 * - When Ever Dialer blocks a number, it writes to both app preferences and the system-wide BlockedNumberContract.
 * - When checking if a number is blocked, [BlockedNumberContract.isBlocked] is queried directly for system-wide accuracy.
 * - CallService and all UI screens automatically reject and reflect blocked numbers system-wide.
 */
object BlockedNumbersManager {

    private fun normalize(number: String) = number.replace(" ", "").replace("-", "").trim()

    /** True when the OS lets this app read/write the system blocked-number list (default dialer role on API 24+). */
    fun canUseSystemBlockList(context: Context): Boolean = try {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            BlockedNumberContract.canCurrentUserBlockNumbers(context)
    } catch (_: Exception) {
        false
    }

    /**
     * Reads all blocked numbers from the Android system BlockedNumberContract provider.
     */
    fun getSystemBlockedList(context: Context): List<String> {
        if (!canUseSystemBlockList(context)) return emptyList()
        val result = mutableListOf<String>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val cursor = context.contentResolver.query(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    arrayOf(
                        BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER,
                        BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER
                    ),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    val origIdx = it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                    val e164Idx = it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER)
                    while (it.moveToNext()) {
                        val orig = if (origIdx != -1) it.getString(origIdx) else null
                        val e164 = if (e164Idx != -1) it.getString(e164Idx) else null
                        val num = orig?.takeIf { s -> s.isNotBlank() } ?: e164?.takeIf { s -> s.isNotBlank() }
                        if (num != null && !result.any { existing -> normalize(existing) == normalize(num) }) {
                            result.add(num)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    /** Current blocked list (merges system-wide and app-level blocked numbers). */
    fun getBlockedList(context: Context?, prefs: PreferenceManager): List<String> {
        val appList = prefs.getString(PreferenceManager.KEY_BLOCKED_CONTACTS, "")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val systemList = if (context != null) getSystemBlockedList(context) else emptyList()
        val merged = (appList + systemList).distinctBy { normalize(it) }
        return merged
    }

    /** Overload without context for backward compatibility. */
    fun getBlockedList(prefs: PreferenceManager): List<String> = getBlockedList(null, prefs)

    /**
     * Checks whether a number is blocked, checking both the Android system provider
     * and the app-level blocked list.
     */
    fun isBlocked(context: Context?, prefs: PreferenceManager, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        val target = normalize(number)
        if (target.isEmpty()) return false

        // 1. Check system-wide BlockedNumberContract first if context is available
        if (context != null && canUseSystemBlockList(context)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    BlockedNumberContract.isBlocked(context, number)
                ) {
                    return true
                }
            } catch (_: Exception) {}
        }

        // 2. Check app-level list / cached list
        return getBlockedList(context, prefs).any { blocked ->
            val cb = normalize(blocked)
            cb.isNotEmpty() && (target.endsWith(cb) || cb.endsWith(target))
        }
    }

    /** Overload without context. */
    fun isBlocked(prefs: PreferenceManager, number: String?): Boolean = isBlocked(null, prefs, number)

    fun block(context: Context, prefs: PreferenceManager, number: String) {
        val trimmed = number.trim()
        if (trimmed.isBlank()) return
        val current = prefs.getString(PreferenceManager.KEY_BLOCKED_CONTACTS, "")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        if (current.none { normalize(it) == normalize(trimmed) }) {
            prefs.setString(PreferenceManager.KEY_BLOCKED_CONTACTS, (current + trimmed).joinToString(","))
        }
        syncToSystem(context, trimmed, block = true)
    }

    fun unblock(context: Context, prefs: PreferenceManager, number: String) {
        val current = prefs.getString(PreferenceManager.KEY_BLOCKED_CONTACTS, "")
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val updated = current.filterNot { normalize(it) == normalize(number) }
        if (updated.size != current.size) {
            prefs.setString(PreferenceManager.KEY_BLOCKED_CONTACTS, updated.joinToString(","))
        }
        syncToSystem(context, number, block = false)
    }

    fun toggle(context: Context, prefs: PreferenceManager, number: String) {
        if (isBlocked(context, prefs, number)) unblock(context, prefs, number) else block(context, prefs, number)
    }

    fun openSystemBlockedNumbers(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                val intent = telecomManager?.createManageBlockedNumbersIntent()
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    true
                } else false
            } else false
        } catch (_: Exception) {
            false
        }
    }

    private fun syncToSystem(context: Context, number: String, block: Boolean) {
        if (!canUseSystemBlockList(context)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (block) {
                    val values = ContentValues().apply {
                        put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
                    }
                    context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
                } else {
                    try {
                        BlockedNumberContract.unblock(context, number)
                    } catch (_: Exception) {}
                    context.contentResolver.delete(
                        BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                        "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ? OR ${BlockedNumberContract.BlockedNumbers.COLUMN_E164_NUMBER} = ?",
                        arrayOf(number, number)
                    )
                }
            }
        } catch (_: Exception) {
            // Best-effort.
        }
    }
}
