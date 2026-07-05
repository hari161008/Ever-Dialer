package com.coolappstore.evercallrecorder.by.svhp.ui.viewmodels

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingItem(
    val uri: Uri,
    val displayName: String,
    val phoneNumber: String,
    val contactName: String?,
    val direction: String,
    val date: Date?,
    val sizeBytes: Long,
    val durationMs: Long = 0L,
    val extension: String,
    val isFavourite: Boolean = false,
    val noteText: String = ""
)

// SortField.DATE kept for safe deserialization of old prefs, treated as TIME
enum class SortField { DATE, NAME, TIME }
enum class SortOrder { ASC, DESC }

data class SortConfig(
    val field: SortField = SortField.TIME,
    val order: SortOrder = SortOrder.DESC
)

enum class FilterTab { ALL, FAVOURITES }

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val preferences = AppPreferences(application)
    private val favPrefs      = application.getSharedPreferences("home_favourites",   Context.MODE_PRIVATE)
    private val notesPrefs    = application.getSharedPreferences("recording_notes",    Context.MODE_PRIVATE)
    private val sortPrefs     = application.getSharedPreferences("sort_config",        Context.MODE_PRIVATE)
    private val durationCache = application.getSharedPreferences("recording_duration", Context.MODE_PRIVATE)

    private val _allRecordings = MutableStateFlow<List<RecordingItem>>(emptyList())
    private val _isLoading     = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val sortConfig = MutableStateFlow(
        run {
            val raw = sortPrefs.getString("sort_field", SortField.TIME.name) ?: SortField.TIME.name
            // Gracefully handle old DATE value — treat as TIME
            val field = runCatching { SortField.valueOf(raw) }.getOrDefault(SortField.TIME)
                .let { if (it == SortField.DATE) SortField.TIME else it }
            SortConfig(
                field = field,
                order = SortOrder.valueOf(sortPrefs.getString("sort_order", SortOrder.DESC.name) ?: SortOrder.DESC.name)
            )
        }
    )

    val filterTab   = MutableStateFlow(FilterTab.ALL)
    val searchQuery = MutableStateFlow("")
    val recordings  = MutableStateFlow<List<RecordingItem>>(emptyList())

    private val dateFormats = listOf(
        SimpleDateFormat("yyyyMMdd_HHmmss.SSSZ", Locale.CANADA),
        SimpleDateFormat("yyyyMMdd_HHmmss",       Locale.CANADA)
    )

    init {
        loadRecordings()
        viewModelScope.launch {
            sortConfig.collect { config ->
                sortPrefs.edit()
                    .putString("sort_field", config.field.name)
                    .putString("sort_order", config.order.name)
                    .apply()
                applyFilters()
            }
        }
        viewModelScope.launch { filterTab.collect   { applyFilters() } }
        viewModelScope.launch { searchQuery.collect { applyFilters() } }
    }

    fun refresh() { if (!_isLoading.value) loadRecordings() }

    val selectedUris = MutableStateFlow<Set<Uri>>(emptySet())

    fun toggleSelection(uri: Uri) {
        val current = selectedUris.value.toMutableSet()
        if (uri in current) current.remove(uri) else current.add(uri)
        selectedUris.value = current
    }

    fun ensureSelected(uri: Uri) {
        if (uri !in selectedUris.value)
            selectedUris.value = selectedUris.value + uri
    }

    /** Selects all given URIs in a single atomic StateFlow update instead of N individual ones. */
    fun selectAll(uris: Collection<Uri>) {
        selectedUris.value = selectedUris.value + uris
    }

    fun clearSelection() { selectedUris.value = emptySet() }

    fun deleteSelected(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val toDelete = selectedUris.value.toSet()
            toDelete.forEach { uri ->
                runCatching { SafHelper.deleteRecording(context, uri) }
                notesPrefs.edit().remove(uri.toString()).apply()
                favPrefs.edit().remove(uri.toString()).apply()
            }
            withContext(Dispatchers.Main) {
                selectedUris.value = emptySet()
                _allRecordings.value = _allRecordings.value.filter { it.uri !in toDelete }
                applyFilters()
            }
        }
    }

    fun toggleFavourite(item: RecordingItem) {
        val key  = item.uri.toString()
        val isFav = favPrefs.getBoolean(key, false)
        favPrefs.edit().putBoolean(key, !isFav).apply()
        _allRecordings.value = _allRecordings.value.map {
            if (it.uri == item.uri) it.copy(isFavourite = !isFav) else it
        }
        applyFilters()
    }

    fun deleteRecording(context: Context, item: RecordingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                SafHelper.deleteRecording(context, item.uri)
            }
            notesPrefs.edit().remove(item.uri.toString()).apply()
            favPrefs.edit().remove(item.uri.toString()).apply()
            withContext(Dispatchers.Main) {
                _allRecordings.value = _allRecordings.value.filter { it.uri != item.uri }
                applyFilters()
            }
        }
    }

    fun getNote(uri: Uri)                = notesPrefs.getString(uri.toString(), "") ?: ""
    fun saveNote(uri: Uri, note: String) = notesPrefs.edit().putString(uri.toString(), note).apply()

    /**
     * Copies the currently selected recordings into [destinationFolderUri], a SAF folder picked
     * by the user specifically for this export. Works regardless of the configured storage mode
     * (SAF folder or private app storage) since it reads through the standard [ContentResolver],
     * which both [RecordingItem.uri] sources support. This is the only way to get a copy of a
     * privately-stored recording into a location any file manager (or other app) can reach.
     */
    fun saveSelectedToFolder(context: Context, destinationFolderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val toSave      = selectedUris.value.toSet()
            val itemsByUri  = _allRecordings.value.associateBy { it.uri }
            val destDir     = DocumentFile.fromTreeUri(context, destinationFolderUri)
            var successCount = 0
            var failureCount = 0

            if (destDir != null && destDir.exists() && destDir.canWrite()) {
                toSave.forEach { uri ->
                    val item = itemsByUri[uri]
                    val name = item?.displayName?.takeIf { it.isNotBlank() } ?: (uri.lastPathSegment ?: "recording_${System.currentTimeMillis()}")
                    val mime = context.contentResolver.getType(uri) ?: "audio/*"
                    try {
                        val targetFile = destDir.createFile(mime, name)
                        if (targetFile != null) {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                context.contentResolver.openOutputStream(targetFile.uri)?.use { output ->
                                    input.copyTo(output)
                                }
                            }
                            successCount++
                        } else {
                            failureCount++
                        }
                    } catch (e: Exception) {
                        failureCount++
                    }
                }
            } else {
                failureCount = toSave.size
            }

            withContext(Dispatchers.Main) {
                if (preferences.isShowToastsEnabled()) {
                    val message = when {
                        successCount == 0 -> "Failed to save recordings"
                        failureCount == 0 -> "Saved $successCount recording${if (successCount != 1) "s" else ""}"
                        else              -> "Saved $successCount, failed to save $failureCount"
                    }
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Copies the currently selected recordings into [directory], a plain [java.io.File] path
     * chosen through the in-app file manager. Uses direct stream I/O (no SAF) so it works
     * immediately without requiring an additional system folder-picker round-trip.
     */
    fun saveSelectedToDirectory(context: Context, directory: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
            val toSave     = selectedUris.value.toSet()
            val itemsByUri = _allRecordings.value.associateBy { it.uri }
            var successCount = 0
            var failureCount = 0

            val privateAuthority = com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper
                .getPrivateStorageAuthority(context)
            val privateDir = com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper
                .getPrivateStorageDir(context)

            toSave.forEach { uri ->
                val item    = itemsByUri[uri]
                val rawName = item?.displayName?.takeIf { it.isNotBlank() }
                    ?: (uri.lastPathSegment?.substringAfterLast('/') ?: "recording_${System.currentTimeMillis()}")

                // Derive MIME type from extension so it's always correct regardless of
                // how the ContentResolver resolves a FileProvider URI.
                val ext  = rawName.substringAfterLast('.', "").lowercase()
                val mime = when (ext) {
                    "m4a"  -> "audio/mp4"
                    "aac"  -> "audio/aac"
                    "mp3"  -> "audio/mpeg"
                    "opus" -> "audio/opus"
                    "ogg"  -> "audio/ogg"
                    "flac" -> "audio/flac"
                    "wav"  -> "audio/wav"
                    else   -> "audio/webm"
                }

                try {
                    var copied = false

                    // For private-storage recordings the URI is a FileProvider content:// URI.
                    // Opening it via ContentResolver on a background thread can silently return
                    // null or throw on newer Android versions, so we resolve the real File and
                    // read it directly — we always have permission to our own filesDir.
                    val inputStream: java.io.InputStream? =
                        if (uri.scheme == "content" && uri.authority == privateAuthority) {
                            val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: rawName
                            val srcFile  = java.io.File(privateDir, fileName)
                            if (srcFile.exists()) srcFile.inputStream() else null
                        } else {
                            context.contentResolver.openInputStream(uri)
                        }

                    inputStream?.use { input ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val externalRoot = Environment.getExternalStorageDirectory().absolutePath
                            val rel = directory.absolutePath
                                .removePrefix(externalRoot)
                                .trimStart('/')
                            val relativePath = if (rel.isEmpty()) "Download" else rel
                            val cv = ContentValues().apply {
                                put(MediaStore.Downloads.DISPLAY_NAME, rawName)
                                put(MediaStore.Downloads.MIME_TYPE, mime)
                                put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
                            }
                            val outUri = context.contentResolver.insert(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv
                            )
                            outUri?.let { dest ->
                                context.contentResolver.openOutputStream(dest)?.use { output ->
                                    input.copyTo(output)
                                    copied = true
                                }
                            }
                        } else {
                            if (directory.exists() || directory.mkdirs()) {
                                val targetFile = java.io.File(directory, rawName)
                                java.io.FileOutputStream(targetFile).use { output ->
                                    input.copyTo(output)
                                    copied = true
                                }
                            }
                        }
                    }
                    if (copied) successCount++ else failureCount++
                } catch (_: Exception) {
                    failureCount++
                }
            }

            withContext(Dispatchers.Main) {
                if (preferences.isShowToastsEnabled()) {
                    val message = when {
                        successCount == 0 -> "Failed to save recordings"
                        failureCount == 0 -> "Saved $successCount recording${if (successCount != 1) "s" else ""}"
                        else              -> "Saved $successCount, failed to save $failureCount"
                    }
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isFavourite(uri: Uri) = favPrefs.getBoolean(uri.toString(), false)

    private fun loadRecordings() {
        viewModelScope.launch {
            _isLoading.value = true
            val fetched = fetchRecordings()
            _allRecordings.value = fetched
            applyFilters()
            _isLoading.value = false
            // Run cleanup rules after the initial load so UI shows immediately
            launch(Dispatchers.IO) { runAutoDeleteIfNeeded(getApplication(), fetched) }
        }
    }

    private fun applyFilters() {
        val query = searchQuery.value.trim().lowercase()
        val tab   = filterTab.value
        val sort  = sortConfig.value
        var list  = _allRecordings.value

        if (query.isNotEmpty()) {
            list = list.filter {
                it.phoneNumber.lowercase().contains(query) ||
                it.displayName.lowercase().contains(query) ||
                (it.contactName?.lowercase()?.contains(query) == true) ||
                it.noteText.lowercase().contains(query)
            }
        }
        if (tab == FilterTab.FAVOURITES) list = list.filter { it.isFavourite }

        list = when (sort.field) {
            SortField.DATE, SortField.TIME -> list.sortedBy { it.date?.time ?: 0L }
            SortField.NAME -> list.sortedBy { (it.contactName ?: it.phoneNumber).lowercase() }
        }
        if (sort.order == SortOrder.DESC) list = list.reversed()
        recordings.value = list
    }

    /** Minimal description of a file on disk, used to unify SAF-folder and private-storage listings before mapping to [RecordingItem]. */
    private data class RecordingFileEntry(val uri: Uri, val name: String, val length: Long)

    private suspend fun fetchRecordings(): List<RecordingItem> = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val template = preferences.getFileNameTemplate()

        val entries: List<RecordingFileEntry> = when (preferences.getStorageMode()) {
            AppPreferences.StorageMode.PRIVATE -> {
                val authority = SafHelper.getPrivateStorageAuthority(context)
                SafHelper.getPrivateStorageDir(context).listFiles()
                    ?.filter { it.isFile }
                    ?.map { file ->
                        RecordingFileEntry(
                            uri    = FileProvider.getUriForFile(context, authority, file),
                            name   = file.name,
                            length = file.length()
                        )
                    }
                    ?: emptyList()
            }
            AppPreferences.StorageMode.SAF_FOLDER -> {
                val folderUri = preferences.getRecordingFolderUri() ?: return@withContext emptyList()
                val dir = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
                if (!dir.exists() || !dir.canRead()) return@withContext emptyList()
                dir.listFiles()
                    .filter { it.isFile && it.name != null }
                    .map { file -> RecordingFileEntry(uri = file.uri, name = file.name!!, length = file.length()) }
            }
            null -> return@withContext emptyList()
        }

        entries.mapNotNull { entry ->
            val name     = entry.name
            val ext      = name.substringAfterLast('.', "")
            val baseName = name.substringBeforeLast('.')
            val parsed   = parseFilenameWithTemplate(baseName, template)
            val date        = parseDate(parsed.dateStr)
            val phoneNumber = parsed.phoneNumber.trim().ifBlank { "Unknown" }
            // Prefer contact name embedded in filename (if template uses {contact_name}),
            // then fall back to a live contacts-db lookup.
            val contactFromFile = if (template.contains("{contact_name}"))
                parsed.contactName.ifBlank { null } else null
            val contactName = if (phoneNumber != "Unknown")
                contactFromFile ?: resolveContactName(context, phoneNumber)
            else null
            val noteText = notesPrefs.getString(entry.uri.toString(), "") ?: ""
            val fileSize = entry.length
            val durationMs = resolveAudioDuration(context, entry.uri, fileSize)
            RecordingItem(
                uri         = entry.uri,
                displayName = name,
                phoneNumber = phoneNumber,
                contactName = contactName,
                direction   = parsed.direction,
                date        = date,
                sizeBytes   = fileSize,
                durationMs  = durationMs,
                extension   = ext,
                isFavourite = isFavourite(entry.uri),
                noteText    = noteText
            )
        }
    }

    // ── Template-aware filename parser ────────────────────────────────────────

    private data class ParsedFilename(
        val direction   : String,
        val phoneNumber : String,
        val dateStr     : String,
        val contactName : String
    )

    /**
     * Parses a recording filename base-name using the user's configured template.
     *
     * Converts each placeholder into a regex capture group so the parser works correctly
     * regardless of field order or extra fields in the template.
     *
     * Falls back to a heuristic approach when the regex doesn't match (e.g. legacy files).
     */
    private fun parseFilenameWithTemplate(baseName: String, template: String): ParsedFilename {
        val fieldOrder  = mutableListOf<String>()
        val patternSb   = StringBuilder("^")
        var i = 0
        while (i < template.length) {
            val rem = template.substring(i)
            when {
                rem.startsWith("{date}") -> {
                    patternSb.append("""(\d{8}_\d{6}(?:\.\d{3}[+-]\d{4})?)""")
                    fieldOrder.add("date"); i += "{date}".length
                }
                rem.startsWith("{direction}") -> {
                    patternSb.append("(in|out)")
                    fieldOrder.add("direction"); i += "{direction}".length
                }
                rem.startsWith("{phone_number}") -> {
                    patternSb.append("""([+\d()\s.\-]*)""")
                    fieldOrder.add("phone"); i += "{phone_number}".length
                }
                rem.startsWith("{contact_name}") -> {
                    patternSb.append("(.+?)")
                    fieldOrder.add("contact"); i += "{contact_name}".length
                }
                rem.startsWith("{cross_country}") -> {
                    patternSb.append("(?:true|false)")
                    i += "{cross_country}".length
                }
                else -> {
                    val ch = template[i]
                    patternSb.append(if (ch in """\\.+*?[](){}|^$""") "\\$ch" else ch.toString())
                    i++
                }
            }
        }
        patternSb.append("$")

        val match = Regex(patternSb.toString()).find(baseName)
        if (match != null) {
            val vals = fieldOrder.zip(match.groupValues.drop(1)).toMap()
            return ParsedFilename(
                direction   = vals["direction"]  ?: "",
                phoneNumber = vals["phone"]?.trim()   ?: "",
                dateStr     = vals["date"]         ?: "",
                contactName = vals["contact"]?.trim() ?: ""
            )
        }

        // Fallback heuristic for files that don't match the current template
        return parseFilenameHeuristic(baseName)
    }

    /** Best-effort parser for filenames whose template is unknown or has changed. */
    private fun parseFilenameHeuristic(baseName: String): ParsedFilename {
        val direction = when {
            Regex("(^|_)in($|_)").containsMatchIn(baseName)  -> "in"
            Regex("(^|_)out($|_)").containsMatchIn(baseName) -> "out"
            else -> ""
        }
        val dateMatch = Regex("""\d{8}_\d{6}(?:\.\d{3}[+-]\d{4})?""").find(baseName)
        val dateStr   = dateMatch?.value ?: ""
        // Phone: segment immediately after direction (if direction found)
        val parts  = baseName.split("_")
        val dirIdx = parts.indexOfFirst { it == "in" || it == "out" }
        val phone  = if (dirIdx in 0 until parts.lastIndex)
            parts.subList(dirIdx + 1, parts.size).joinToString("_") else ""
        return ParsedFilename(direction, phone, dateStr, "")
    }

    private fun parseDate(raw: String): Date? {
        for (fmt in dateFormats) { runCatching { return fmt.parse(raw) } }
        return null
    }

    private fun resolveContactName(context: Context, phoneNumber: String): String? {
        return try {
            // The raw phone number parsed from a recording's filename can still contain
            // formatting characters (spaces, dashes, parentheses, etc). Passing that raw
            // string straight into PhoneLookup's filter URI makes the underlying loose
            // number-matching unreliable, and it can end up matching a *different*
            // contact that merely shares some of the same digits — which is exactly why
            // the wrong contact name/photo could show up for a recording. Normalising the
            // number first (digits only, keeping a leading '+') and safely encoding it
            // before appending it to the URI ensures each recording is looked up against
            // its own, correct number.
            val normalized = com.coolappstore.evercallrecorder.by.svhp.utils.PhoneNumberManager.normalisePhoneNumber(phoneNumber)
            if (normalized.isBlank()) return null
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalized)
            )
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER),
                null, null, null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                // Defensive check: only trust the match if the returned contact number
                // actually shares the normalized digits with the number we looked up,
                // guarding against loose-match false positives returning an unrelated contact.
                val matchedNumber = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)) }.getOrNull()
                val matchedDigits = matchedNumber?.filter { it.isDigit() }.orEmpty()
                val queryDigits   = normalized.filter { it.isDigit() }
                val isPlausibleMatch = matchedDigits.isEmpty() || queryDigits.isEmpty() ||
                    matchedDigits.endsWith(queryDigits.takeLast(7)) || queryDigits.endsWith(matchedDigits.takeLast(7))
                if (!isPlausibleMatch) return@use null
                cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        } catch (_: Exception) { null }
    }

    private fun resolveAudioDuration(context: Context, uri: Uri, fileSizeBytes: Long): Long {
        // Cache key = uri + file size so cache is invalidated when the file is replaced
        val cacheKey = "${uri}_$fileSizeBytes"
        val cached = durationCache.getLong(cacheKey, -1L)
        if (cached >= 0L) return cached

        val duration = try {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                }
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }
        } catch (_: Exception) { 0L }

        durationCache.edit().putLong(cacheKey, duration).apply()
        return duration
    }

    /** Runs time-based and space-based auto-delete rules.
     *  Must be called from an IO coroutine. Mutates [_allRecordings] on Main. */
    private suspend fun runAutoDeleteIfNeeded(context: Context, recordings: List<RecordingItem>) {
        val timeEnabled  = preferences.isAutoDeleteByTimeEnabled()
        val spaceEnabled = preferences.isAutoDeleteBySpaceEnabled()
        if (!timeEnabled && !spaceEnabled) return
        if (preferences.getStorageMode() == null) return

        val urisToDelete = mutableSetOf<Uri>()
        var working = recordings.toMutableList()

        // ── Time-based ───────────────────────────────────────────────────────
        if (timeEnabled) {
            val value       = preferences.getAutoDeleteByTimeValue().toLong().coerceAtLeast(1L)
            val unit        = preferences.getAutoDeleteByTimeUnit()
            val thresholdMs = if (unit == "hours") value * 3_600_000L else value * 86_400_000L
            val cutoff      = System.currentTimeMillis() - thresholdMs
            working.filter { it.date != null && it.date.time < cutoff }
                .forEach { urisToDelete.add(it.uri) }
        }

        // ── Space-based ──────────────────────────────────────────────────────
        if (spaceEnabled) {
            val value      = preferences.getAutoDeleteBySpaceValue().toLong().coerceAtLeast(1L)
            val unit       = preferences.getAutoDeleteBySpaceUnit()
            val limitBytes = if (unit == "gb") value * 1_073_741_824L else value * 1_048_576L
            // Exclude items already marked for time-based deletion so we don't over-delete
            val remaining  = working.filter { it.uri !in urisToDelete }
            var total      = remaining.sumOf { it.sizeBytes }
            if (total > limitBytes) {
                // Sort oldest first, delete until under limit
                for (item in remaining.sortedBy { it.date }) {
                    if (total <= limitBytes) break
                    urisToDelete.add(item.uri)
                    total -= item.sizeBytes
                }
            }
        }

        if (urisToDelete.isEmpty()) return

        // Delete works uniformly across SAF-folder and private-storage recordings.
        urisToDelete.forEach { uri ->
            runCatching { SafHelper.deleteRecording(context, uri) }
            notesPrefs.edit().remove(uri.toString()).apply()
            favPrefs.edit().remove(uri.toString()).apply()
        }

        withContext(Dispatchers.Main) {
            _allRecordings.value = _allRecordings.value.filter { it.uri !in urisToDelete }
            applyFilters()
        }
    }

    /** Loads contact photo as ImageBitmap, or null if unavailable. */
    suspend fun loadContactPhoto(context: Context, phoneNumber: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            try {
                // See resolveContactName() for why the number must be normalized and
                // encoded before being appended to the PhoneLookup URI — otherwise a
                // recording can end up showing a different contact's photo.
                val normalized = com.coolappstore.evercallrecorder.by.svhp.utils.PhoneNumberManager.normalisePhoneNumber(phoneNumber)
                if (normalized.isBlank()) return@withContext null
                val lookupUri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(normalized)
                )
                context.contentResolver.query(
                    lookupUri,
                    arrayOf(ContactsContract.PhoneLookup.PHOTO_URI, ContactsContract.PhoneLookup.NUMBER),
                    null, null, null
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) return@withContext null
                    val matchedNumber = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)) }.getOrNull()
                    val matchedDigits = matchedNumber?.filter { it.isDigit() }.orEmpty()
                    val queryDigits   = normalized.filter { it.isDigit() }
                    val isPlausibleMatch = matchedDigits.isEmpty() || queryDigits.isEmpty() ||
                        matchedDigits.endsWith(queryDigits.takeLast(7)) || queryDigits.endsWith(matchedDigits.takeLast(7))
                    if (!isPlausibleMatch) return@withContext null
                    val photoUriStr = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)) ?: return@withContext null
                    val stream = context.contentResolver.openInputStream(Uri.parse(photoUriStr))
                        ?: return@withContext null
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) { null }
        }
}
