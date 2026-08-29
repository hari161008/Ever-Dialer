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

            // Default incoming & ongoing paths
            prefs.getString(PreferenceManager.KEY_INCOMING_BG_PATH, "")?.let {
                if (it.isNotBlank()) referencedPaths.add(File(it).canonicalPath)
            }
            prefs.getString(PreferenceManager.KEY_ONGOING_BG_PATH, "")?.let {
                if (it.isNotBlank()) referencedPaths.add(File(it).canonicalPath)
            }

            // Per-contact and dynamic keys
            allKeys.forEach { key ->
                if (key.endsWith("_bg_path")) {
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
}
