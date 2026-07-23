package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Disk-backed cache for the resolved contacts list.
 *
 * On devices with a large address book (2500+ contacts is common for people with a synced
 * Gmail/Workspace account), building the list from [android.provider.ContactsContract] from
 * scratch — the two full-table queries in ContactsRepository plus the per-row merge — can take
 * a noticeable amount of time. That cost was being paid on every cold start / process restart,
 * which also directly delayed the unified Search screen since it filters the same
 * [ContactsViewModel.allContacts] list.
 *
 * This cache lets [ContactsViewModel] show the last-known contacts list immediately (from a
 * small local JSON file, which parses in milliseconds even for a few thousand contacts) while a
 * fresh read from the ContentResolver happens in the background and silently replaces it — so
 * the very first frame (and the very first search) already has data instead of blocking on a
 * live provider query.
 */
object ContactsCache {

    private const val CACHE_FILE_NAME = "contacts_cache.json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun cacheFile(context: Context): File =
        File(context.filesDir, CACHE_FILE_NAME)

    /** Reads the cached contacts list from disk. Safe to call from a background thread only.
     *  Returns an empty list if there's no cache yet or it failed to parse (e.g. corrupted,
     *  or written by an older/incompatible app version). */
    fun read(context: Context): List<Contact> {
        return try {
            val file = cacheFile(context)
            if (!file.exists()) return emptyList()
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            json.decodeFromString<List<Contact>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Writes [contacts] to disk, replacing whatever was cached before. Safe to call from a
     *  background thread only. Failures are swallowed — the cache is a best-effort accelerator,
     *  never a requirement for correctness, since the source of truth is always the
     *  ContentResolver. */
    fun write(context: Context, contacts: List<Contact>) {
        try {
            val text = json.encodeToString(contacts)
            val tmp = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            tmp.writeText(text)
            // Rename over the real file so a crash/kill mid-write never leaves a half-written,
            // unparseable cache behind.
            tmp.renameTo(cacheFile(context))
        } catch (_: Exception) {
            // Best-effort — just skip caching this round.
        }
    }

    /** Clears the cache (e.g. if it's ever suspected to be stale/corrupt). */
    fun clear(context: Context) {
        try {
            cacheFile(context).delete()
        } catch (_: Exception) {}
    }
}
