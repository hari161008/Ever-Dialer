package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import com.coolappstore.everdialer.by.svhp.modal.data.CallLogEntry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Disk- and memory-backed cache for the resolved call logs list, mirroring [ContactsCache].
 *
 * Keeps the last-known call logs immediately available on cold and warm starts (0ms to render),
 * preventing spinner delays and avoiding expensive full queries before the UI first displays.
 * Stored in [Context.getFilesDir] so the Android OS does not clear it under storage pressure.
 */
object CallLogsCache {

    private const val CACHE_FILE_NAME = "call_logs_cache.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Volatile
    private var inMemoryCache: List<CallLogEntry>? = null

    fun getInMemoryCache(): List<CallLogEntry>? = inMemoryCache

    fun setInMemoryCache(logs: List<CallLogEntry>) {
        inMemoryCache = logs
    }

    private fun cacheFile(context: Context): File =
        File(context.filesDir, CACHE_FILE_NAME)

    /**
     * Reads the cached call logs from memory or disk. Safe to call from background thread.
     * Migrates from legacy cacheDir if present.
     */
    fun read(context: Context): List<CallLogEntry> {
        inMemoryCache?.let { if (it.isNotEmpty()) return it }
        return try {
            val file = cacheFile(context)
            if (!file.exists()) {
                // Check legacy cache in cacheDir if present
                val legacyFile = File(context.cacheDir, CACHE_FILE_NAME)
                if (legacyFile.exists()) {
                    val text = legacyFile.readText()
                    if (text.isNotBlank()) {
                        val parsed = json.decodeFromString<List<CallLogEntry>>(text)
                        inMemoryCache = parsed
                        write(context, parsed)
                        try { legacyFile.delete() } catch (_: Exception) {}
                        return parsed
                    }
                }
                return emptyList()
            }
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            val logs = json.decodeFromString<List<CallLogEntry>>(text)
            inMemoryCache = logs
            logs
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Writes [logs] to disk and memory atomically.
     */
    fun write(context: Context, logs: List<CallLogEntry>) {
        inMemoryCache = logs
        try {
            val text = json.encodeToString(logs)
            val tmp = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            tmp.writeText(text)
            tmp.renameTo(cacheFile(context))
        } catch (_: Exception) {
            // Best-effort
        }
    }

    fun clear(context: Context) {
        inMemoryCache = null
        try {
            cacheFile(context).delete()
        } catch (_: Exception) {}
    }
}
