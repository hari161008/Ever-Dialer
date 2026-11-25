package com.coolappstore.everdialer.by.svhp.controller.util

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Window
import android.view.WindowManager

/**
 * Centralized manager for WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER.
 * Multiple screens or composables (e.g. MainActivity, ContactDetails, Dialpad, Recents)
 * can register tokens requesting that the live device wallpaper be displayed through the window.
 * The flag is added on the first request and ONLY cleared when all registered tokens are removed.
 */
object WallpaperWindowManager {
    private const val TAG = "WallpaperWindowManager"
    private val activeTokens = mutableSetOf<String>()

    @Synchronized
    fun setWallpaperActive(window: Window?, token: String, active: Boolean) {
        if (window == null) return
        if (active) {
            val wasEmpty = activeTokens.isEmpty()
            activeTokens.add(token)
            Log.d(TAG, "Added token '$token', activeTokens=$activeTokens")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        } else {
            activeTokens.remove(token)
            Log.d(TAG, "Removed token '$token', activeTokens=$activeTokens")
            if (activeTokens.isEmpty()) {
                Log.d(TAG, "activeTokens is empty, clearing FLAG_SHOW_WALLPAPER")
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
            }
        }
    }

    @Synchronized
    fun setWallpaperActive(activity: Activity?, token: String, active: Boolean) {
        setWallpaperActive(activity?.window, token, active)
    }

    @Synchronized
    fun isWallpaperRequested(): Boolean = activeTokens.isNotEmpty()
}
