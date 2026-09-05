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
import com.coolappstore.evercallrecorder.by.svhp.utils.RecordingFileNameFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    // Public read-only view of every recording on disk (unfiltered by searchQuery/filterTab),
    // used by the dialer's own global search screen to search recording notes.
    val allRecordings: StateFlow<List<RecordingItem>> = _allRecordings

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
            val itemsByUri = _allRecordings.value.associateBy { it.uri }
            // Bug fix: this used to fire-and-forget SafHelper.deleteRecording() and then remove
            // every selected item from the list regardless of whether the delete actually
            // succeeded. If the underlying delete silently failed, the file stayed on disk but
            // the app acted like it was gone — so it would reappear the next time the list was
            // reloaded (looking like recordings can't be permanently deleted). We now only purge
            // notes/favourites and drop an item from the list when its file was actually removed.
            val actuallyDeleted = mutableSetOf<Uri>()
            toDelete.forEach { uri ->
                val deleted = runCatching { SafHelper.deleteRecording(context, uri) }.getOrDefault(false)
                if (deleted) {
                    actuallyDeleted.add(uri)
                    notesPrefs.edit().remove(uri.toString()).apply()
                    favPrefs.edit().remove(uri.toString()).apply()
                    itemsByUri[uri]?.let { deleteIntegratedContactNoteIfNeeded(context, it.phoneNumber) }
                }
            }
            withContext(Dispatchers.Main) {
                selectedUris.value = selectedUris.value - actuallyDeleted
                _allRecordings.value = _allRecordings.value.filter { it.uri !in actuallyDeleted }
                applyFilters()
                if (preferences.isShowToastsEnabled() && actuallyDeleted.size < toDelete.size) {
                    val failed = toDelete.size - actuallyDeleted.size
                    android.widget.Toast.makeText(
                        context,
                        "Failed to delete $failed recording${if (failed != 1) "s" else ""}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
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
            // Bug fix: same root cause as deleteSelected() above — only treat the recording as
            // gone (dropping it from the list, clearing its note/favourite) if the file was
            // actually deleted. Previously this always removed it from the UI, so a failed
            // delete left the file behind and it would silently reappear on the next load.
            val deleted = runCatching { SafHelper.deleteRecording(context, item.uri) }.getOrDefault(false)
            if (deleted) {
                notesPrefs.edit().remove(item.uri.toString()).apply()
                favPrefs.edit().remove(item.uri.toString()).apply()
                deleteIntegratedContactNoteIfNeeded(context, item.phoneNumber)
            }
            withContext(Dispatchers.Main) {
                if (deleted) {
                    _allRecordings.value = _allRecordings.value.filter { it.uri != item.uri }
                    applyFilters()
                } else if (preferences.isShowToastsEnabled()) {
                    android.widget.Toast.makeText(context, "Failed to delete recording", android.widget.Toast.LENGTH_SHORT).show()
                }
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

    /**
     * Ever Dialer's Settings → App Settings → "Integrate Notes Section" treats this app's Notes
     * section and call-recording notes as one merged place. Settings → App Settings →
     * "Delete Notes With Recording" (only shown/settable while that's on) extends this: when a
     * recording is deleted, the matching contact note (kept in the main app's own Notes
     * section/files, not this module's [notesPrefs]) is deleted too — since with integration on
     * they're presented as the same note. Off by default so deleting a recording never removes
     * a contact note unless the user opted in. Reads straight from the shared "rivo_prefs"
     * SharedPreferences file (same file/keys Ever Dialer's own PreferenceManager uses) and the
     * app's Notes directory convention, since this module can't depend on the app module.
     */
    private fun deleteIntegratedContactNoteIfNeeded(context: Context, phoneNumber: String) {
        try {
            val prefs = context.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
            val integrateNotes = prefs.getBoolean("integrate_notes_section", true)
            val deleteWithRecording = prefs.getBoolean("delete_notes_with_recording", false)
            if (!integrateNotes || !deleteWithRecording) return
            val safeNumber = phoneNumber.filter { it.isDigit() || it == '+' }
            if (safeNumber.isEmpty()) return
            val notesDir = File(context.getExternalFilesDir(null), "Notes")
            notesDir.listFiles()
                ?.filter { it.extension == "txt" && it.nameWithoutExtension.contains("[$safeNumber]") }
                ?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

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
                val folderUri = preferences.getRecordingFolderUri()
                val safEntries = if (folderUri != null) {
                    val dir = DocumentFile.fromTreeUri(context, folderUri)
                    if (dir != null && dir.exists() && dir.canRead()) {
                        dir.listFiles()
                            .filter { it.isFile && it.name != null }
                            .map { file -> RecordingFileEntry(uri = file.uri, name = file.name!!, length = file.length()) }
                    } else emptyList()
                } else emptyList()

                val authority = SafHelper.getPrivateStorageAuthority(context)
                val privateEntries = SafHelper.getPrivateStorageDir(context).listFiles()
                    ?.filter { it.isFile }
                    ?.map { file ->
                        RecordingFileEntry(
                            uri    = FileProvider.getUriForFile(context, authority, file),
                            name   = file.name,
                            length = file.length()
                        )
                    }
                    ?: emptyList()

                val seen = mutableSetOf<String>()
                (safEntries + privateEntries).filter { seen.add(it.name) }
            }
            null -> {
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
        }

        entries.mapNotNull { entry ->
            val name     = entry.name
            val ext      = name.substringAfterLast('.', "")
            var baseName = name.substringBeforeLast('.')

            // Strip the hidden phone-number suffix (see RecordingFileNameFormatter.formatFileName)
            // before running the template parser below, so it doesn't confuse the field matching,
            // and remember the number it carried — this is the number we fall back to whenever the
            // visible template itself doesn't expose one.
            val hiddenPhoneNumber = extractHiddenPhoneSuffix(baseName)
            if (hiddenPhoneNumber != null) {
                baseName = baseName.substringBeforeLast(RecordingFileNameFormatter.HIDDEN_NUMBER_MARKER)
            }

            val parsed   = parseFilenameWithTemplate(baseName, template)
            val date        = parseDate(parsed.dateStr)
            // Bug fix: previously this only ever fell back to "Unknown" when the template simply
            // didn't produce a phone number (e.g. the default "{contact_name}_{date}_{direction}"
            // template never includes one at all), which is exactly what made the player and the
            // recordings list permanently show "Unknown" for the number — and, since contact name/
            // photo lookups both key off that number, the contact's saved photo (and sometimes name)
            // disappeared right along with it. The hidden suffix recovered above now supplies the
            // real number in that case.
            val phoneNumber = parsed.phoneNumber.trim().ifBlank { hiddenPhoneNumber ?: "" }.ifBlank { "Unknown" }
            // Prefer contact name embedded in filename (if template uses {contact_name}),
            // then fall back to a live contacts-db lookup by phone number.
            //
            // Bug fix: previously this only ever consulted contactFromFile/resolveContactName
            // when phoneNumber != "Unknown", which meant a contact name embedded directly in
            // the filename (via {contact_name}) was thrown away and shown as "Unknown" on any
            // template that doesn't also include {phone_number} — e.g. the default template.
            // contactFromFile is now checked first, independent of whether a phone number was
            // parsed, and the live lookup is only attempted when we actually have a number.
            val contactFromFile = if (template.contains("{contact_name}"))
                parsed.contactName.ifBlank { null } else null
            val contactName = contactFromFile
                ?: if (phoneNumber != "Unknown") resolveContactName(context, phoneNumber) else null
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

    /**
     * Recovers the real phone number hidden by [RecordingFileNameFormatter.formatFileName] when the
     * user's visible file name template doesn't include {phone_number}. Returns null when the
     * marker isn't present (e.g. legacy recordings made before this fix, or templates that already
     * include {phone_number} and so never got the hidden suffix in the first place).
     */
    private fun extractHiddenPhoneSuffix(baseName: String): String? {
        val markerIndex = baseName.lastIndexOf(RecordingFileNameFormatter.HIDDEN_NUMBER_MARKER)
        if (markerIndex < 0) return null
        val suffix = baseName.substring(markerIndex + RecordingFileNameFormatter.HIDDEN_NUMBER_MARKER.length)
        return suffix.trim().ifBlank { null }
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

    private fun numbersMatch(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true
        try {
            if (android.telephony.PhoneNumberUtils.compare(a, b)) return true
        } catch (_: Exception) {}

        fun normDigits(s: String): String {
            val d = s.filter { it.isDigit() }
            return when {
                d.length > 10 && d.startsWith("91") -> d.substring(2)
                d.length > 10 && d.startsWith("1") -> d.substring(1)
                d.length > 10 && d.startsWith("00") -> d.substring(2)
                d.length > 10 && d.startsWith("0") -> d.substring(1)
                d.startsWith("0") -> d.substring(1)
                else -> d
            }
        }

        val rawA = a.filter { it.isDigit() }
        val rawB = b.filter { it.isDigit() }
        if (rawA.isEmpty() || rawB.isEmpty()) return false
        if (rawA == rawB || rawA.endsWith(rawB) || rawB.endsWith(rawA)) return true

        val da = normDigits(a)
        val db = normDigits(b)
        if (da.isEmpty() || db.isEmpty()) return false
        if (da == db || da.endsWith(db) || db.endsWith(da)) return true

        val minLen = minOf(da.length, db.length)
        if (minLen >= 7) {
            val checkLen = minOf(minLen, 10)
            for (len in checkLen downTo 7) {
                if (da.takeLast(len) == db.takeLast(len)) return true
            }
        }
        return false
    }

    private fun resolveContactName(context: Context, phoneNumber: String): String? {
        return try {
            val normalized = com.coolappstore.evercallrecorder.by.svhp.utils.PhoneNumberManager.normalisePhoneNumber(phoneNumber)
            if (normalized.isBlank()) return null
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalized)
            )
            val directMatch = context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.NUMBER),
                null, null, null
            )?.use { cursor ->
                var matchedName: String? = null
                while (cursor.moveToNext()) {
                    val matchedNumber = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)) }.getOrNull() ?: ""
                    if (numbersMatch(phoneNumber, matchedNumber)) {
                        matchedName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                        break
                    }
                }
                matchedName
            }
            directMatch ?: fallbackScanContactName(context, phoneNumber)
        } catch (_: Exception) { null }
    }

    /** Fallback for [resolveContactName]: PhoneLookup's built-in fuzzy matching can return
     *  zero rows at all when a contact is saved WITH a country code but the recording's number
     *  is WITHOUT one (or vice versa), especially when it disagrees with the device's detected
     *  region — row-walking above can't help then since there's nothing to walk. Recover by
     *  scanning every saved phone number directly with the same plausibility check. */
    private fun fallbackScanContactName(context: Context, queryNumber: String): String? {
        if (queryNumber.isBlank()) return null
        return try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                var matchedName: String? = null
                while (cursor.moveToNext()) {
                    val savedNumber = cursor.getString(numberIdx) ?: continue
                    if (numbersMatch(queryNumber, savedNumber)) {
                        matchedName = cursor.getString(nameIdx)
                        break
                    }
                }
                matchedName
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
        // Bug fix: only drop items whose file actually got deleted — see deleteRecording()/
        // deleteSelected() above for why blindly trusting the call here let "deleted" recordings
        // keep reappearing.
        val itemsByUri = working.associateBy { it.uri }
        val actuallyDeleted = mutableSetOf<Uri>()
        urisToDelete.forEach { uri ->
            val deleted = runCatching { SafHelper.deleteRecording(context, uri) }.getOrDefault(false)
            if (deleted) {
                actuallyDeleted.add(uri)
                notesPrefs.edit().remove(uri.toString()).apply()
                favPrefs.edit().remove(uri.toString()).apply()
                itemsByUri[uri]?.let { deleteIntegratedContactNoteIfNeeded(context, it.phoneNumber) }
            }
        }

        if (actuallyDeleted.isEmpty()) return

        withContext(Dispatchers.Main) {
            _allRecordings.value = _allRecordings.value.filter { it.uri !in actuallyDeleted }
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
                val queryDigits = normalized.filter { it.isDigit() }
                var photoUriStr: String? = context.contentResolver.query(
                    lookupUri,
                    arrayOf(ContactsContract.PhoneLookup.PHOTO_URI, ContactsContract.PhoneLookup.NUMBER),
                    null, null, null
                )?.use { cursor ->
                    // Same multi-row fix as resolveContactName(): a contact with several saved
                    // numbers can produce multiple rows, so walk all of them for a genuine match
                    // instead of giving up after an unrelated first row.
                    var matched: String? = null
                    while (cursor.moveToNext()) {
                        val matchedNumber = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER)) }.getOrNull()
                        val matchedDigits = matchedNumber?.filter { it.isDigit() }.orEmpty()
                        val isPlausibleMatch = matchedDigits.isNotEmpty() && queryDigits.isNotEmpty() &&
                            (matchedDigits.endsWith(queryDigits.takeLast(7)) || queryDigits.endsWith(matchedDigits.takeLast(7)))
                        if (!isPlausibleMatch) continue
                        matched = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                        if (matched != null) break
                    }
                    matched
                }

                // Same fallback rationale as resolveContactName(): PhoneLookup can return zero
                // rows at all for a country-code-mismatched number, so if nothing matched
                // above, fall back to a manual scan of every saved phone number.
                if (photoUriStr == null) {
                    photoUriStr = fallbackScanContactPhotoUri(context, queryDigits)
                }
                if (photoUriStr == null) return@withContext null

                val stream = context.contentResolver.openInputStream(Uri.parse(photoUriStr))
                    ?: return@withContext null
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            } catch (_: Exception) { null }
        }

    private fun fallbackScanContactPhotoUri(context: Context, queryDigits: String): String? {
        if (queryDigits.isEmpty()) return null
        return try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.PHOTO_URI, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )?.use { cursor ->
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                var matchedPhoto: String? = null
                while (cursor.moveToNext()) {
                    val savedDigits = cursor.getString(numberIdx)?.filter { it.isDigit() }.orEmpty()
                    val isPlausibleMatch = savedDigits.isNotEmpty() &&
                        (savedDigits.endsWith(queryDigits.takeLast(7)) || queryDigits.endsWith(savedDigits.takeLast(7)))
                    if (!isPlausibleMatch) continue
                    matchedPhoto = cursor.getString(photoIdx)
                    if (matchedPhoto != null) break
                }
                matchedPhoto
            }
        } catch (_: Exception) { null }
    }
}
