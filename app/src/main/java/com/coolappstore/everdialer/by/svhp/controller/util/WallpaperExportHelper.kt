package com.coolappstore.everdialer.by.svhp.controller.util

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Helper to extract the device wallpaper using WallpaperManager (based on WallpaperExport).
 */
object WallpaperExportHelper {
    @SuppressLint("MissingPermission")
    fun extractWallpaperToFile(context: Context): File? {
        return try {
            val wallpaperManager = WallpaperManager.getInstance(context)
            var drawable: Drawable? = null
            try {
                drawable = wallpaperManager.drawable
            } catch (_: Exception) {}

            if (drawable == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    drawable = wallpaperManager.getDrawable(WallpaperManager.FLAG_SYSTEM)
                } catch (_: Exception) {}
            }

            if (drawable == null) {
                try {
                    drawable = wallpaperManager.getBuiltInDrawable(30000, 30000, false, 0.5f, 0.5f)
                } catch (_: Exception) {}
            }

            if (drawable == null) {
                return null
            }

            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1080
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1920
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            val tempFile = File(context.cacheDir, "extracted_wallpaper_${System.currentTimeMillis()}.png")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if auto-refresh wallpaper is enabled for incoming, ongoing or any contact custom wallpaper,
     * and refreshes the wallpaper files automatically.
     */
    fun refreshAutoWallpaperIfEnabled(context: Context, prefs: PreferenceManager) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val allKeys = prefs.getAllKeys()
                val activeAutoRefreshPrefixes = mutableListOf<String>()

                // Check default incoming
                if (prefs.getString(PreferenceManager.KEY_INCOMING_BG_TYPE, "none") == "wallpaper" &&
                    prefs.getBoolean(PreferenceManager.KEY_INCOMING_AUTO_REFRESH_WALLPAPER, false)
                ) {
                    activeAutoRefreshPrefixes.add("incoming")
                }

                // Check default ongoing
                if (prefs.getString(PreferenceManager.KEY_ONGOING_BG_TYPE, "none") == "wallpaper" &&
                    prefs.getBoolean(PreferenceManager.KEY_ONGOING_AUTO_REFRESH_WALLPAPER, false)
                ) {
                    activeAutoRefreshPrefixes.add("ongoing")
                }

                // Check contact-specific prefixes
                allKeys.forEach { key ->
                    if (key.endsWith("_auto_refresh_wallpaper") && prefs.getBoolean(key, false)) {
                        val prefix = key.removeSuffix("_auto_refresh_wallpaper")
                        if (prefix != "incoming" && prefix != "ongoing") {
                            val typeKey = "${prefix}_bg_type"
                            if (prefs.getString(typeKey, "none") == "wallpaper") {
                                activeAutoRefreshPrefixes.add(prefix)
                            }
                        }
                    }
                }

                if (activeAutoRefreshPrefixes.isNotEmpty()) {
                    val extracted = extractWallpaperToFile(context)
                    if (extracted != null && extracted.exists()) {
                        val bgDir = File(context.filesDir, "backgrounds").apply { mkdirs() }
                        activeAutoRefreshPrefixes.forEach { prefix ->
                            val currentPath = prefs.getString("${prefix}_bg_path", "") ?: ""
                            val dest = if (currentPath.isNotEmpty()) File(currentPath) else File(bgDir, "custom_bg_${prefix}.png")
                            extracted.copyTo(dest, overwrite = true)
                            if (currentPath.isEmpty()) {
                                prefs.setString("${prefix}_bg_path", dest.absolutePath)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
