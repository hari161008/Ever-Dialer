package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Utility to manage background media files, prevent cache accumulation,
 * clean up temporary picked/extracted files, and prune orphaned background assets.
 */
object BackgroundMediaManager {

    private val TEMP_PREFIXES = listOf(
        "picked_image_",
        "picked_video_",
        "picked_file_",
        "extracted_wallpaper_"
    )

    /**
     * Deletes temporary picked/extracted media files from cacheDir.
     * @param olderThanMs If > 0, only deletes files older than this duration in ms. If 0, deletes all matching temp files.
     */
    fun cleanTemporaryPickedMedia(context: Context, olderThanMs: Long = 0L) {
        try {
            val cacheDir = context.cacheDir ?: return
            val files = cacheDir.listFiles() ?: return
            val now = System.currentTimeMillis()

            for (file in files) {
                if (TEMP_PREFIXES.any { file.name.startsWith(it) }) {
                    if (olderThanMs <= 0L || (now - file.lastModified()) >= olderThanMs) {
                        try {
                            file.delete()
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Safely deletes [file] if it resides inside [context.cacheDir].
     */
    fun cleanupFileIfInCache(context: Context, file: File?) {
        if (file == null || !file.exists()) return
        try {
            val cacheDir = context.cacheDir ?: return
            val filePath = file.canonicalPath
            val cachePath = cacheDir.canonicalPath
            if (filePath.startsWith(cachePath)) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    /**
     * Scans filesDir/backgrounds and deletes files that are not referenced by any *_bg_path key in [prefs].
     */
    fun pruneOrphanedBackgrounds(context: Context, prefs: PreferenceManager) {
        try {
            val bgDir = File(context.filesDir, "backgrounds")
            if (!bgDir.exists() || !bgDir.isDirectory) return

            val allKeys = prefs.getAllKeys()
            val referencedPaths = mutableSetOf<String>()

            // Default incoming, ongoing & dialpad paths
            prefs.getString(PreferenceManager.KEY_INCOMING_BG_PATH, "")?.let {
                if (it.isNotBlank()) referencedPaths.add(File(it).canonicalPath)
            }
            prefs.getString(PreferenceManager.KEY_ONGOING_BG_PATH, "")?.let {
                if (it.isNotBlank()) referencedPaths.add(File(it).canonicalPath)
            }
            prefs.getString(PreferenceManager.KEY_DIALPAD_BG_PATH, "")?.let {
                if (it.isNotBlank()) referencedPaths.add(File(it).canonicalPath)
            }
            prefs.getString(PreferenceManager.KEY_INCOMING_CUSTOM_PFP_PATH, "")?.let {
                if (it.isNotBlank()) referencedPaths.add(File(it).canonicalPath)
            }
            prefs.getString(PreferenceManager.KEY_ONGOING_CUSTOM_PFP_PATH, "")?.let {
                if (it.isNotBlank()) referencedPaths.add(File(it).canonicalPath)
            }

            // Per-contact and dynamic keys
            allKeys.forEach { key ->
                if (key.endsWith("_bg_path") || key.endsWith("_custom_pfp_path") || key.endsWith("_pfp_path")) {
                    prefs.getString(key, "")?.let { path ->
                        if (path.isNotBlank()) {
                            try {
                                referencedPaths.add(File(path).canonicalPath)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            val files = bgDir.listFiles() ?: return
            for (file in files) {
                try {
                    val canonical = file.canonicalPath
                    if (!referencedPaths.contains(canonical)) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    /**
     * Launches a background IO coroutine to perform both temporary cache cleanup and orphaned background pruning.
     */
    fun autoCleanInBackground(context: Context, prefs: PreferenceManager) {
        CoroutineScope(Dispatchers.IO).launch {
            // Delete temp cache files older than 5 minutes
            cleanTemporaryPickedMedia(context, olderThanMs = 5 * 60 * 1000L)
            // Prune unreferenced backgrounds
            pruneOrphanedBackgrounds(context, prefs)
        }
    }

    private val luminanceCache = java.util.concurrent.ConcurrentHashMap<String, Float>()

    /**
     * Samples media luminance (taking dim into account) to determine if background is bright (> 0.48f) or dark.
     * Results are cached in-memory by file path, timestamp, video flag, and dim value.
     */
    fun calculateMediaLuminance(file: File?, isVideo: Boolean, dim: Float): Float {
        if (file == null || !file.exists()) return 0.2f
        val cacheKey = "${file.absolutePath}_${file.lastModified()}_${isVideo}_$dim"
        luminanceCache[cacheKey]?.let { return it }

        val lum = try {
            val bitmap = if (isVideo) {
                val retriever = android.media.MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val frame = retriever.getFrameAtTime(0)
                retriever.release()
                frame
            } else {
                val options = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = 16
                }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
            }
            if (bitmap != null) {
                var totalLum = 0.0
                var count = 0
                val stepX = maxOf(1, bitmap.width / 12)
                val stepY = maxOf(1, bitmap.height / 12)
                for (x in 0 until bitmap.width step stepX) {
                    for (y in 0 until bitmap.height step stepY) {
                        val pixel = bitmap.getPixel(x, y)
                        totalLum += androidx.core.graphics.ColorUtils.calculateLuminance(pixel)
                        count++
                    }
                }
                bitmap.recycle()
                if (count > 0) {
                    val rawLum = (totalLum / count).toFloat()
                    rawLum * (1f - dim.coerceIn(0f, 1f))
                } else 0.2f
            } else 0.2f
        } catch (_: Exception) {
            0.2f
        }
        luminanceCache[cacheKey] = lum
        return lum
    }

    /**
     * Returns precalculated dialpad background luminance without blocking the main UI thread.
     * Reads from in-memory cache or PreferenceManager. If not yet computed, kicks off background calculation.
     */
    fun getOrCalculateDialpadLuminance(prefs: PreferenceManager, file: File?, isVideo: Boolean, dim: Float): Float {
        if (file == null || !file.exists()) return 0.2f
        val cacheKey = "${file.absolutePath}_${file.lastModified()}_${isVideo}_$dim"
        luminanceCache[cacheKey]?.let { return it }

        val savedLum = prefs.getFloat(PreferenceManager.KEY_DIALPAD_BG_LUMINANCE, -1f)
        if (savedLum >= 0f) {
            luminanceCache[cacheKey] = savedLum
            return savedLum
        }

        // Return sensible default immediately, calculate on IO thread in background
        CoroutineScope(Dispatchers.IO).launch {
            val computed = calculateMediaLuminance(file, isVideo, dim)
            prefs.setFloat(PreferenceManager.KEY_DIALPAD_BG_LUMINANCE, computed)
        }
        return 0.2f
    }

    /**
     * Determines current device wallpaper luminance taking dim into account using WallpaperColors.
     * Does not require external storage permissions and works with system / live wallpapers.
     */
    fun getWallpaperLuminance(context: Context, dim: Float): Float {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                val wm = android.app.WallpaperManager.getInstance(context)
                val colors = wm.getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)
                if (colors != null) {
                    val primaryColor = colors.primaryColor.toArgb()
                    val rawLum = androidx.core.graphics.ColorUtils.calculateLuminance(primaryColor).toFloat()
                    return rawLum * (1f - dim.coerceIn(0f, 1f))
                }
            }
            0.2f
        } catch (_: Throwable) {
            0.2f
        }
    }
}
