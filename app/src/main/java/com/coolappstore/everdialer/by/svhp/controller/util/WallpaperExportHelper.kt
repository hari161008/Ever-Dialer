package com.coolappstore.everdialer.by.svhp.controller.util

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
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
}
