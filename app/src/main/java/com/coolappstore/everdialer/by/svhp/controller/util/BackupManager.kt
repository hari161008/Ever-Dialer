package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import android.content.SharedPreferences
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.`interface`.IContactsRepository
import com.coolappstore.everdialer.by.svhp.modal.repository.ContactsRepository
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.context.GlobalContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    data class BackupContents(
        val hasSettings: Boolean = false,
        val hasCallingCards: Boolean = false,
        val hasNotes: Boolean = false,
        val hasRecordings: Boolean = false,
        val hasContacts: Boolean = false,
        val hasCallLogs: Boolean = false
    ) {
        val hasAny: Boolean
            get() = hasSettings || hasCallingCards || hasNotes || hasRecordings || hasContacts || hasCallLogs
    }

    const val PREFS_RIVO = "rivo_prefs"
    const val PREFS_RECORDER = "evercallrecorder_prefs"
    const val PREFS_RECORDER_FAV = "home_favourites"
    const val PREFS_RECORDER_NOTES = "recording_notes"
    const val PREFS_RECORDER_SORT = "sort_config"
    const val PREFS_RECORDER_DURATION = "recording_duration"
    const val PREFS_NS_MASTER = "network_switch_master_switch"
    const val PREFS_NS_AUTO = "network_switch_automation_flags"
    const val PREFS_MISSED_CALL_DURATION = "missed_call_durations"
    const val PREFS_WIDGET = "recent_calls_widget_prefs"

    val KNOWN_PREFS = listOf(
        PREFS_RIVO,
        PREFS_RECORDER,
        PREFS_RECORDER_FAV,
        PREFS_RECORDER_NOTES,
        PREFS_RECORDER_SORT,
        PREFS_RECORDER_DURATION,
        PREFS_NS_MASTER,
        PREFS_NS_AUTO,
        PREFS_MISSED_CALL_DURATION,
        PREFS_WIDGET
    )

    fun getBackupDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "Backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getBackgroundsDir(context: Context): File {
        val dir = File(context.filesDir, "backgrounds")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun isCallingCardKey(key: String): Boolean {
        return key.startsWith("contact_") ||
                key.startsWith("incoming_bg_") ||
                key.startsWith("ongoing_bg_") ||
                key.startsWith("incoming_font_") ||
                key.startsWith("ongoing_font_") ||
                key.startsWith("incoming_elements_") ||
                key.startsWith("ongoing_elements_") ||
                key.startsWith("incoming_custom_pfp_") ||
                key.startsWith("ongoing_custom_pfp_") ||
                key.startsWith("incoming_auto_refresh_wallpaper") ||
                key.startsWith("ongoing_auto_refresh_wallpaper") ||
                key == PreferenceManager.KEY_INCOMING_SHOW_CONTACT_PFP ||
                key == PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP ||
                key == PreferenceManager.KEY_INCOMING_SHOW_PHONE_NUMBER ||
                key == PreferenceManager.KEY_ONGOING_SHOW_PHONE_NUMBER
    }

    fun writeBackup(
        context: Context,
        outputStream: OutputStream,
        backupSettings: Boolean = true,
        backupCallingCards: Boolean = true,
        backupNotes: Boolean = true,
        backupRecordings: Boolean = true,
        backupContacts: Boolean = false,
        backupCallLogs: Boolean = false
    ): Boolean {
        return try {
            ZipOutputStream(outputStream).use { zip ->
                val backedUpFiles = mutableSetOf<String>()

                if (backupSettings) {
                    val prefsToBackup = KNOWN_PREFS.toMutableSet()
                    try {
                        val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                        if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                            sharedPrefsDir.listFiles()?.filter { it.extension == "xml" }?.forEach { file ->
                                prefsToBackup.add(file.nameWithoutExtension)
                            }
                        }
                    } catch (_: Exception) {}

                    // 1. Backup SharedPreferences files
                    prefsToBackup.forEach { prefName ->
                        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                        if (prefs.all.isNotEmpty() || prefName == PREFS_RIVO) {
                            val prefsJson = prefsToJson(prefs)
                            zip.putNextEntry(ZipEntry("prefs/$prefName.json"))
                            zip.write(prefsJson.toByteArray(Charsets.UTF_8))
                            zip.closeEntry()

                            // Backward compatibility with legacy restores expecting prefs.json at zip root
                            if (prefName == PREFS_RIVO) {
                                zip.putNextEntry(ZipEntry("prefs.json"))
                                zip.write(prefsJson.toByteArray(Charsets.UTF_8))
                                zip.closeEntry()
                            }
                        }
                    }

                    // 2. Backup Network Switch DataStore preferences
                    try {
                        val datastoreDir = File(context.filesDir, "datastore")
                        if (datastoreDir.exists() && datastoreDir.isDirectory) {
                            datastoreDir.listFiles()?.forEach { dsFile ->
                                if (dsFile.isFile) {
                                    zip.putNextEntry(ZipEntry("datastore/${dsFile.name}"))
                                    FileInputStream(dsFile).use { it.copyTo(zip) }
                                    zip.closeEntry()
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    // 3. Backup custom font if set
                    try {
                        val fontFile = File(context.filesDir, "custom_font.ttf")
                        if (fontFile.exists() && fontFile.isFile && fontFile.length() > 0) {
                            zip.putNextEntry(ZipEntry("custom_font.ttf"))
                            FileInputStream(fontFile).use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    } catch (_: Exception) {}
                } else if (backupCallingCards) {
                    // Backup calling cards contact preferences only if settings backup is not selected
                    val rivoPrefs = context.getSharedPreferences(PREFS_RIVO, Context.MODE_PRIVATE)
                    val callingCardPrefs = filterCallingCardPrefs(rivoPrefs)
                    if (callingCardPrefs.isNotEmpty()) {
                        val prefsJson = mapToJson(callingCardPrefs)
                        zip.putNextEntry(ZipEntry("prefs/$PREFS_RIVO.json"))
                        zip.write(prefsJson.toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                }

                // 4. Backup Calling Cards Media (Backgrounds: images, videos, wallpapers)
                if (backupSettings || backupCallingCards) {
                    val bgDir = getBackgroundsDir(context)
                    bgDir.listFiles()?.forEach { bgFile ->
                        if (bgFile.isFile && !backedUpFiles.contains(bgFile.name)) {
                            zip.putNextEntry(ZipEntry("backgrounds/${bgFile.name}"))
                            FileInputStream(bgFile).use { it.copyTo(zip) }
                            zip.closeEntry()
                            backedUpFiles.add(bgFile.name)
                        }
                    }

                    // Check any referenced files in SharedPreferences that might reside elsewhere
                    val rivoPrefs = context.getSharedPreferences(PREFS_RIVO, Context.MODE_PRIVATE)
                    rivoPrefs.all.forEach { (key, value) ->
                        val isMediaKey = key.endsWith("_bg_path") ||
                                key.endsWith("_custom_pfp_path") ||
                                key.endsWith("_pfp_path") ||
                                key == PreferenceManager.KEY_INCOMING_CUSTOM_PFP_PATH ||
                                key == PreferenceManager.KEY_ONGOING_CUSTOM_PFP_PATH
                        if (isMediaKey && value is String && value.isNotBlank()) {
                            val f = File(value)
                            if (f.exists() && f.isFile && !backedUpFiles.contains(f.name)) {
                                zip.putNextEntry(ZipEntry("backgrounds/${f.name}"))
                                FileInputStream(f).use { it.copyTo(zip) }
                                zip.closeEntry()
                                backedUpFiles.add(f.name)
                            }
                        }
                    }
                }

                // 5. Backup notes (contact notes & general notes)
                if (backupNotes || backupCallingCards) {
                    val notesDir = NoteManager.getNotesDir(context)
                    notesDir.listFiles()?.filter { it.extension == "txt" }?.forEach { noteFile ->
                        zip.putNextEntry(ZipEntry("notes/${noteFile.name}"))
                        FileInputStream(noteFile).use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }

                // 6. Backup call recordings & their metadata (favourites & notes)
                if (backupRecordings) {
                    val backedUpRecordingNames = mutableSetOf<String>()
                    val recordingsMetaArray = JSONArray()

                    val favPrefs = context.getSharedPreferences(PREFS_RECORDER_FAV, Context.MODE_PRIVATE)
                    val notesPrefs = context.getSharedPreferences(PREFS_RECORDER_NOTES, Context.MODE_PRIVATE)

                    // Helper to copy recording and record metadata
                    fun archiveRecording(name: String, openInput: () -> java.io.InputStream, uriString: String?) {
                        if (!backedUpRecordingNames.add(name)) return
                        zip.putNextEntry(ZipEntry("recordings/$name"))
                        openInput().use { it.copyTo(zip) }
                        zip.closeEntry()

                        val isFav = if (uriString != null) favPrefs.getBoolean(uriString, false) else false
                        val note = if (uriString != null) notesPrefs.getString(uriString, "") ?: "" else ""
                        if (isFav || note.isNotBlank()) {
                            val item = JSONObject()
                            item.put("fileName", name)
                            item.put("isFavourite", isFav)
                            item.put("noteText", note)
                            recordingsMetaArray.put(item)
                        }
                    }

                    // A) App private storage recordings
                    val privateDir = com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper.getPrivateStorageDir(context)
                    val authority = com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper.getPrivateStorageAuthority(context)
                    privateDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                        val uriStr = try {
                            androidx.core.content.FileProvider.getUriForFile(context, authority, file).toString()
                        } catch (_: Exception) { null }
                        archiveRecording(file.name, { FileInputStream(file) }, uriStr)
                    }

                    // B) SAF folder recordings if configured
                    val appPrefs = com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences(context)
                    val folderUri = appPrefs.getRecordingFolderUri()
                    if (folderUri != null) {
                        try {
                            val dir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, folderUri)
                            if (dir != null && dir.exists() && dir.canRead()) {
                                dir.listFiles().filter { it.isFile && it.name != null }.forEach { docFile ->
                                    val fileName = docFile.name!!
                                    val uriStr = docFile.uri.toString()
                                    archiveRecording(fileName, { context.contentResolver.openInputStream(docFile.uri) ?: throw java.io.FileNotFoundException() }, uriStr)
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    // Write recordings_meta.json
                    if (recordingsMetaArray.length() > 0) {
                        zip.putNextEntry(ZipEntry("recordings/recordings_meta.json"))
                        zip.write(recordingsMetaArray.toString().toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                }

                // 7. Backup contacts (if enabled)
                if (backupContacts) {
                    try {
                        val contactsRepo = GlobalContext.get().getOrNull<IContactsRepository>()
                            ?: ContactsRepository(context.contentResolver, context)
                        val contactsList = contactsRepo.getContacts()
                        if (contactsList.isNotEmpty()) {
                            val jsonArray = JSONArray()
                            contactsList.forEach { c ->
                                val obj = JSONObject()
                                obj.put("name", c.name)
                                obj.put("isFavorite", c.isFavorite)
                                val phoneArr = JSONArray()
                                c.phoneNumbers.forEach { phoneArr.put(it) }
                                obj.put("phoneNumbers", phoneArr)
                                val emailArr = JSONArray()
                                c.emails.forEach { emailArr.put(it) }
                                obj.put("emails", emailArr)
                                val addrArr = JSONArray()
                                c.addresses.forEach { addrArr.put(it) }
                                obj.put("addresses", addrArr)
                                jsonArray.put(obj)
                            }
                            zip.putNextEntry(ZipEntry("contacts/contacts.json"))
                            zip.write(jsonArray.toString().toByteArray(Charsets.UTF_8))
                            zip.closeEntry()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BackupManager", "Failed to backup contacts", e)
                    }
                }

                if (backupCallLogs) {
                    try {
                        val cursor = context.contentResolver.query(
                            android.provider.CallLog.Calls.CONTENT_URI,
                            arrayOf(
                                android.provider.CallLog.Calls.NUMBER,
                                android.provider.CallLog.Calls.CACHED_NAME,
                                android.provider.CallLog.Calls.TYPE,
                                android.provider.CallLog.Calls.DATE,
                                android.provider.CallLog.Calls.DURATION,
                                android.provider.CallLog.Calls.PHONE_ACCOUNT_ID,
                                android.provider.CallLog.Calls.IS_READ,
                                android.provider.CallLog.Calls.NEW
                            ),
                            null,
                            null,
                            "${android.provider.CallLog.Calls.DATE} DESC"
                        )
                        cursor?.use { c ->
                            val numIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                            val nameIdx = c.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                            val typeIdx = c.getColumnIndex(android.provider.CallLog.Calls.TYPE)
                            val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
                            val durIdx = c.getColumnIndex(android.provider.CallLog.Calls.DURATION)
                            val accIdx = c.getColumnIndex(android.provider.CallLog.Calls.PHONE_ACCOUNT_ID)
                            val isReadIdx = c.getColumnIndex(android.provider.CallLog.Calls.IS_READ)
                            val newIdx = c.getColumnIndex(android.provider.CallLog.Calls.NEW)

                            val jsonArray = JSONArray()
                            while (c.moveToNext()) {
                                val obj = JSONObject()
                                if (numIdx >= 0) obj.put("number", c.getString(numIdx) ?: "")
                                if (nameIdx >= 0 && !c.isNull(nameIdx)) obj.put("cachedName", c.getString(nameIdx))
                                if (typeIdx >= 0) obj.put("type", c.getInt(typeIdx))
                                if (dateIdx >= 0) obj.put("date", c.getLong(dateIdx))
                                if (durIdx >= 0) obj.put("duration", c.getLong(durIdx))
                                if (accIdx >= 0 && !c.isNull(accIdx)) obj.put("phoneAccountId", c.getString(accIdx))
                                if (isReadIdx >= 0) obj.put("isRead", c.getInt(isReadIdx))
                                if (newIdx >= 0) obj.put("isNew", c.getInt(newIdx))
                                jsonArray.put(obj)
                            }
                            if (jsonArray.length() > 0) {
                                zip.putNextEntry(ZipEntry("call_logs/call_logs.json"))
                                zip.write(jsonArray.toString().toByteArray(Charsets.UTF_8))
                                zip.closeEntry()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BackupManager", "Failed to backup call logs", e)
                    }
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun createBackup(
        context: Context,
        backupSettings: Boolean = true,
        backupCallingCards: Boolean = true,
        backupNotes: Boolean = true,
        backupRecordings: Boolean = true,
        backupContacts: Boolean = false,
        backupCallLogs: Boolean = false
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(getBackupDir(context), "EverDialer_Backup_$timestamp.everdialer")
            val ok = FileOutputStream(backupFile).use { outputStream ->
                writeBackup(context, outputStream, backupSettings, backupCallingCards, backupNotes, backupRecordings, backupContacts, backupCallLogs)
            }
            if (ok) backupFile else null
        } catch (_: Exception) {
            null
        }
    }

    fun inspectBackup(backupFile: File): BackupContents {
        var hasSettings = false
        var hasCallingCards = false
        var hasNotes = false
        var hasRecordings = false
        var hasContacts = false
        var hasCallLogs = false

        try {
            ZipInputStream(FileInputStream(backupFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name.startsWith("prefs/") || name == "prefs.json" -> {
                            hasSettings = true
                            hasCallingCards = true
                        }
                        name.startsWith("datastore/") || name == "custom_font.ttf" -> {
                            hasSettings = true
                        }
                        name.startsWith("backgrounds/") -> {
                            hasCallingCards = true
                        }
                        name.startsWith("notes/") -> {
                            hasNotes = true
                        }
                        name.startsWith("recordings/") && !name.endsWith("/") -> {
                            hasRecordings = true
                        }
                        name.startsWith("contacts/") && !name.endsWith("/") -> {
                            hasContacts = true
                        }
                        name.startsWith("call_logs/") && !name.endsWith("/") -> {
                            hasCallLogs = true
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (_: Exception) {}

        return BackupContents(
            hasSettings = hasSettings,
            hasCallingCards = hasCallingCards,
            hasNotes = hasNotes,
            hasRecordings = hasRecordings,
            hasContacts = hasContacts,
            hasCallLogs = hasCallLogs
        )
    }

    fun restoreBackup(
        context: Context,
        backupFile: File,
        restoreSettings: Boolean = true,
        restoreCallingCards: Boolean = true,
        restoreNotes: Boolean = true,
        restoreRecordings: Boolean = true,
        restoreContacts: Boolean = true,
        restoreCallLogs: Boolean = true
    ): Boolean {
        return try {
            var restoredAny = false
            var restoredRivoFromPrefsDir = false
            var recordingsRestoredCount = 0
            var restoredMetaJson: String? = null

            val privateRecDir = com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper.getPrivateStorageDir(context)

            ZipInputStream(FileInputStream(backupFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name.startsWith("prefs/") && name.endsWith(".json") -> {
                            val prefName = name.removePrefix("prefs/").removeSuffix(".json")
                            val json = zip.readBytes().toString(Charsets.UTF_8)
                            if (restoreSettings) {
                                restorePrefs(context, prefName, json, onlyCallingCardKeys = false)
                                if (prefName == PREFS_RIVO) {
                                    restoredRivoFromPrefsDir = true
                                }
                                restoredAny = true
                            } else if (restoreCallingCards && prefName == PREFS_RIVO) {
                                restorePrefs(context, prefName, json, onlyCallingCardKeys = true)
                                restoredRivoFromPrefsDir = true
                                restoredAny = true
                            }
                        }
                        name == "prefs.json" -> {
                            val json = zip.readBytes().toString(Charsets.UTF_8)
                            if (!restoredRivoFromPrefsDir) {
                                if (restoreSettings) {
                                    restorePrefs(context, PREFS_RIVO, json, onlyCallingCardKeys = false)
                                    restoredAny = true
                                } else if (restoreCallingCards) {
                                    restorePrefs(context, PREFS_RIVO, json, onlyCallingCardKeys = true)
                                    restoredAny = true
                                }
                            }
                        }
                        name.startsWith("datastore/") -> {
                            if (restoreSettings) {
                                val relativePath = name.removePrefix("datastore/")
                                if (relativePath.isNotEmpty()) {
                                    val dsFile = File(File(context.filesDir, "datastore"), relativePath)
                                    dsFile.parentFile?.mkdirs()
                                    FileOutputStream(dsFile).use { zip.copyTo(it) }
                                    restoredAny = true
                                }
                            }
                        }
                        name == "custom_font.ttf" -> {
                            if (restoreSettings) {
                                val fontFile = File(context.filesDir, "custom_font.ttf")
                                FileOutputStream(fontFile).use { zip.copyTo(it) }
                                val rivoPrefs = context.getSharedPreferences(PREFS_RIVO, Context.MODE_PRIVATE)
                                rivoPrefs.edit().putString(PreferenceManager.KEY_CUSTOM_FONT_PATH, fontFile.absolutePath).apply()
                                restoredAny = true
                            }
                        }
                        name.startsWith("backgrounds/") -> {
                            if (restoreCallingCards || restoreSettings) {
                                val fileName = name.removePrefix("backgrounds/")
                                if (fileName.isNotEmpty()) {
                                    val bgDir = getBackgroundsDir(context)
                                    val bgFile = File(bgDir, fileName)
                                    bgFile.parentFile?.mkdirs()
                                    FileOutputStream(bgFile).use { zip.copyTo(it) }
                                    restoredAny = true
                                }
                            }
                        }
                        name.startsWith("notes/") -> {
                            if (restoreNotes || restoreCallingCards) {
                                val fileName = name.removePrefix("notes/")
                                if (fileName.isNotEmpty()) {
                                    val noteFile = File(NoteManager.getNotesDir(context), fileName)
                                    noteFile.parentFile?.mkdirs()
                                    FileOutputStream(noteFile).use { zip.copyTo(it) }
                                    restoredAny = true
                                }
                            }
                        }
                        name == "recordings/recordings_meta.json" -> {
                            if (restoreRecordings) {
                                restoredMetaJson = zip.readBytes().toString(Charsets.UTF_8)
                                restoredAny = true
                            }
                        }
                        name.startsWith("recordings/") -> {
                            if (restoreRecordings) {
                                val fileName = name.removePrefix("recordings/")
                                if (fileName.isNotEmpty() && !fileName.endsWith(".json")) {
                                    val recFile = File(privateRecDir, fileName)
                                    recFile.parentFile?.mkdirs()
                                    FileOutputStream(recFile).use { zip.copyTo(it) }
                                    recordingsRestoredCount++
                                    restoredAny = true
                                }
                            }
                        }
                        name == "contacts/contacts.json" -> {
                            if (restoreContacts) {
                                try {
                                    val jsonStr = zip.readBytes().toString(Charsets.UTF_8)
                                    val jsonArray = JSONArray(jsonStr)
                                    val contactsRepo = GlobalContext.get().getOrNull<IContactsRepository>()
                                        ?: ContactsRepository(context.contentResolver, context)
                                    val existingContacts = contactsRepo.getContacts()
                                    for (i in 0 until jsonArray.length()) {
                                        val obj = jsonArray.getJSONObject(i)
                                        val cName = obj.optString("name", "")
                                        val isFavorite = obj.optBoolean("isFavorite", false)
                                        val phoneNumbers = mutableListOf<String>()
                                        val phoneArr = obj.optJSONArray("phoneNumbers")
                                        if (phoneArr != null) {
                                            for (j in 0 until phoneArr.length()) {
                                                phoneNumbers.add(phoneArr.getString(j))
                                            }
                                        }
                                        val emails = mutableListOf<String>()
                                        val emailArr = obj.optJSONArray("emails")
                                        if (emailArr != null) {
                                            for (j in 0 until emailArr.length()) {
                                                emails.add(emailArr.getString(j))
                                            }
                                        }
                                        val addresses = mutableListOf<String>()
                                        val addrArr = obj.optJSONArray("addresses")
                                        if (addrArr != null) {
                                            for (j in 0 until addrArr.length()) {
                                                addresses.add(addrArr.getString(j))
                                            }
                                        }

                                        val alreadyExists = existingContacts.any { ec ->
                                            ec.name.equals(cName, ignoreCase = true) &&
                                            (phoneNumbers.isEmpty() || ec.phoneNumbers.any { ep -> phoneNumbers.any { numbersLikelyMatch(ep, it) } })
                                        }
                                        if (!alreadyExists && (cName.isNotBlank() || phoneNumbers.isNotEmpty())) {
                                            val contactToSave = Contact(
                                                id = "",
                                                name = cName.ifBlank { phoneNumbers.firstOrNull() ?: "Unknown" },
                                                phoneNumbers = phoneNumbers,
                                                emails = emails,
                                                addresses = addresses,
                                                isFavorite = isFavorite
                                            )
                                            contactsRepo.saveContact(contactToSave)
                                        }
                                    }
                                    restoredAny = true
                                } catch (e: Exception) {
                                    android.util.Log.e("BackupManager", "Failed to restore contacts", e)
                                }
                            }
                        }
                        name == "call_logs/call_logs.json" -> {
                            if (restoreCallLogs) {
                                try {
                                    val jsonStr = zip.readBytes().toString(Charsets.UTF_8)
                                    val jsonArray = JSONArray(jsonStr)

                                    val existingKeys = mutableSetOf<String>()
                                    try {
                                        val cursor = context.contentResolver.query(
                                            android.provider.CallLog.Calls.CONTENT_URI,
                                            arrayOf(
                                                android.provider.CallLog.Calls.NUMBER,
                                                android.provider.CallLog.Calls.DATE,
                                                android.provider.CallLog.Calls.TYPE
                                            ),
                                            null,
                                            null,
                                            null
                                        )
                                        cursor?.use { c ->
                                            val numIdx = c.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                                            val dateIdx = c.getColumnIndex(android.provider.CallLog.Calls.DATE)
                                            val typeIdx = c.getColumnIndex(android.provider.CallLog.Calls.TYPE)
                                            while (c.moveToNext()) {
                                                val num = if (numIdx >= 0) c.getString(numIdx) ?: "" else ""
                                                val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                                                val type = if (typeIdx >= 0) c.getInt(typeIdx) else 0
                                                existingKeys.add("$num|$date|$type")
                                            }
                                        }
                                    } catch (_: Exception) {}

                                    for (i in 0 until jsonArray.length()) {
                                        val obj = jsonArray.getJSONObject(i)
                                        val number = obj.optString("number", "")
                                        val date = obj.optLong("date", 0L)
                                        val type = obj.optInt("type", 1)
                                        val key = "$number|$date|$type"
                                        if (!existingKeys.contains(key)) {
                                            val values = android.content.ContentValues().apply {
                                                put(android.provider.CallLog.Calls.NUMBER, number)
                                                if (obj.has("cachedName")) {
                                                    put(android.provider.CallLog.Calls.CACHED_NAME, obj.getString("cachedName"))
                                                }
                                                put(android.provider.CallLog.Calls.TYPE, type)
                                                put(android.provider.CallLog.Calls.DATE, date)
                                                put(android.provider.CallLog.Calls.DURATION, obj.optLong("duration", 0L))
                                                if (obj.has("phoneAccountId")) {
                                                    put(android.provider.CallLog.Calls.PHONE_ACCOUNT_ID, obj.getString("phoneAccountId"))
                                                }
                                                if (obj.has("isRead")) {
                                                    put(android.provider.CallLog.Calls.IS_READ, obj.getInt("isRead"))
                                                }
                                                if (obj.has("isNew")) {
                                                    put(android.provider.CallLog.Calls.NEW, obj.getInt("isNew"))
                                                }
                                            }
                                            try {
                                                context.contentResolver.insert(android.provider.CallLog.Calls.CONTENT_URI, values)
                                                existingKeys.add(key)
                                            } catch (e: Exception) {
                                                android.util.Log.e("BackupManager", "Failed to insert call log entry", e)
                                            }
                                        }
                                    }
                                    restoredAny = true
                                } catch (e: Exception) {
                                    android.util.Log.e("BackupManager", "Failed to restore call logs", e)
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // If recordings were restored and recorder storage mode was unset, set it to PRIVATE so they show immediately
            if (recordingsRestoredCount > 0) {
                val appPrefs = com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences(context)
                if (appPrefs.getStorageMode() == null) {
                    appPrefs.setStorageMode(com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences.StorageMode.PRIVATE)
                }
            }

            // Restore recordings metadata (favourites and notes) re-keyed to current URIs
            if (!restoredMetaJson.isNullOrBlank()) {
                try {
                    val metaArray = JSONArray(restoredMetaJson)
                    val favPrefs = context.getSharedPreferences(PREFS_RECORDER_FAV, Context.MODE_PRIVATE)
                    val notesPrefs = context.getSharedPreferences(PREFS_RECORDER_NOTES, Context.MODE_PRIVATE)
                    val favEditor = favPrefs.edit()
                    val notesEditor = notesPrefs.edit()

                    val authority = com.coolappstore.evercallrecorder.by.svhp.system.storage.SafHelper.getPrivateStorageAuthority(context)

                    for (i in 0 until metaArray.length()) {
                        val item = metaArray.getJSONObject(i)
                        val fileName = item.optString("fileName", "")
                        val isFav = item.optBoolean("isFavourite", false)
                        val note = item.optString("noteText", "")

                        if (fileName.isNotEmpty()) {
                            val localFile = File(privateRecDir, fileName)
                            if (localFile.exists()) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, localFile).toString()
                                if (isFav) favEditor.putBoolean(uri, true)
                                if (note.isNotEmpty()) notesEditor.putString(uri, note)
                            }
                        }
                    }
                    favEditor.apply()
                    notesEditor.apply()
                } catch (_: Exception) {}
            }

            // Fix and validate all background paths after restore so they point to current device directories
            if (restoreCallingCards || restoreSettings) {
                fixBackgroundPathsAfterRestore(context)
            }

            // Sync services and stores after restore
            try {
                com.supernova.networkswitch.util.MasterSwitchStore.reload(context)
            } catch (_: Throwable) {}

            try {
                com.supernova.networkswitch.util.AutomationSwitchStore.reload(context)
            } catch (_: Throwable) {}

            try {
                com.coolappstore.evercallrecorder.by.svhp.services.call.CallRecordingComponentGuard.sync(context)
            } catch (_: Throwable) {}

            try {
                org.koin.java.KoinJavaComponent.getKoin().getOrNull<PreferenceManager>()?.notifyChanged()
            } catch (_: Throwable) {}

            restoredAny
        } catch (_: Exception) {
            false
        }
    }

    private fun filterCallingCardPrefs(prefs: SharedPreferences): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        prefs.all.forEach { (key, value) ->
            if (value != null && isCallingCardKey(key)) {
                result[key] = value
            }
        }
        return result
    }

    private fun mapToJson(map: Map<String, Any>): String {
        val json = JSONObject()
        val meta = JSONObject()
        map.forEach { (key, value) ->
            when (value) {
                is Boolean -> {
                    json.put(key, value)
                    meta.put(key, "boolean")
                }
                is Int -> {
                    json.put(key, value)
                    meta.put(key, "int")
                }
                is Long -> {
                    json.put(key, value)
                    meta.put(key, "long")
                }
                is Float -> {
                    json.put(key, value.toDouble())
                    meta.put(key, "float")
                }
                is String -> {
                    json.put(key, value)
                    meta.put(key, "string")
                }
                is Set<*> -> {
                    val arr = JSONArray()
                    value.forEach { item -> if (item != null) arr.put(item.toString()) }
                    json.put(key, arr)
                    meta.put(key, "string_set")
                }
            }
        }
        val wrapper = JSONObject()
        wrapper.put("data", json)
        wrapper.put("meta", meta)
        return wrapper.toString()
    }

    private fun prefsToJson(prefs: SharedPreferences): String {
        val json = JSONObject()
        val meta = JSONObject() // store type hints for unambiguous restore
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> {
                    json.put(key, value)
                    meta.put(key, "boolean")
                }
                is Int -> {
                    json.put(key, value)
                    meta.put(key, "int")
                }
                is Long -> {
                    json.put(key, value)
                    meta.put(key, "long")
                }
                is Float -> {
                    json.put(key, value.toDouble())
                    meta.put(key, "float")
                }
                is String -> {
                    json.put(key, value)
                    meta.put(key, "string")
                }
                is Set<*> -> {
                    val arr = JSONArray()
                    value.forEach { item -> if (item != null) arr.put(item.toString()) }
                    json.put(key, arr)
                    meta.put(key, "string_set")
                }
            }
        }
        val wrapper = JSONObject()
        wrapper.put("data", json)
        wrapper.put("meta", meta)
        return wrapper.toString()
    }

    private fun restorePrefs(context: Context, prefName: String, json: String, onlyCallingCardKeys: Boolean = false) {
        try {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val raw = JSONObject(json)
            val jsonObj = if (raw.has("data")) raw.getJSONObject("data") else raw
            val meta = if (raw.has("meta")) raw.getJSONObject("meta") else JSONObject()

            val bgDir = getBackgroundsDir(context)

            fun remapString(key: String, strValue: String): String {
                val isMediaKey = key.endsWith("_bg_path") ||
                        key.endsWith("_custom_pfp_path") ||
                        key.endsWith("_pfp_path") ||
                        key == PreferenceManager.KEY_INCOMING_CUSTOM_PFP_PATH ||
                        key == PreferenceManager.KEY_ONGOING_CUSTOM_PFP_PATH
                return if (isMediaKey && strValue.isNotBlank()) {
                    val fileName = File(strValue).name
                    val localFile = File(bgDir, fileName)
                    if (localFile.exists()) localFile.absolutePath else strValue
                } else if (key == PreferenceManager.KEY_CUSTOM_FONT_PATH && strValue.isNotBlank()) {
                    val fontFile = File(context.filesDir, "custom_font.ttf")
                    if (fontFile.exists()) fontFile.absolutePath else strValue
                } else {
                    strValue
                }
            }

            jsonObj.keys().forEach { key ->
                if (onlyCallingCardKeys && !isCallingCardKey(key)) {
                    return@forEach
                }
                val typeHint = meta.optString(key, "")
                when (typeHint) {
                    "boolean" -> {
                        editor.putBoolean(key, jsonObj.getBoolean(key))
                    }
                    "int" -> {
                        editor.putInt(key, jsonObj.getInt(key))
                    }
                    "long" -> {
                        editor.putLong(key, jsonObj.getLong(key))
                    }
                    "float" -> {
                        editor.putFloat(key, jsonObj.getDouble(key).toFloat())
                    }
                    "string" -> {
                        val str = jsonObj.optString(key, "")
                        editor.putString(key, remapString(key, str))
                    }
                    "string_set" -> {
                        val arr = jsonObj.optJSONArray(key)
                        if (arr != null) {
                            val set = mutableSetOf<String>()
                            for (i in 0 until arr.length()) {
                                set.add(arr.getString(i))
                            }
                            editor.putStringSet(key, set)
                        }
                    }
                    else -> {
                        // Fallback for legacy backups without full type hints
                        when (val value = jsonObj.get(key)) {
                            is Boolean -> editor.putBoolean(key, value)
                            is Int -> editor.putInt(key, value)
                            is Long -> {
                                if (value in Int.MIN_VALUE..Int.MAX_VALUE) editor.putInt(key, value.toInt())
                                else editor.putLong(key, value)
                            }
                            is Double -> editor.putFloat(key, value.toFloat())
                            is String -> {
                                editor.putString(key, remapString(key, value))
                            }
                            is JSONArray -> {
                                val set = mutableSetOf<String>()
                                for (i in 0 until value.length()) {
                                    set.add(value.getString(i))
                                }
                                editor.putStringSet(key, set)
                            }
                        }
                    }
                }
            }
            editor.apply()
        } catch (_: Exception) {}
    }

    /**
     * Fixes any background and custom PFP file paths in SharedPreferences after a restore.
     * Ensures all background and PFP paths point to existing files in the current device's backgrounds directory.
     */
    private fun fixBackgroundPathsAfterRestore(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_RIVO, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val bgDir = getBackgroundsDir(context)
            var modified = false

            prefs.all.forEach { (key, value) ->
                val isMediaKey = key.endsWith("_bg_path") ||
                        key.endsWith("_custom_pfp_path") ||
                        key.endsWith("_pfp_path") ||
                        key == PreferenceManager.KEY_INCOMING_CUSTOM_PFP_PATH ||
                        key == PreferenceManager.KEY_ONGOING_CUSTOM_PFP_PATH
                if (isMediaKey && value is String && value.isNotBlank()) {
                    val file = File(value)
                    if (!file.exists()) {
                        val fileName = file.name
                        val localFile = File(bgDir, fileName)
                        if (localFile.exists()) {
                            editor.putString(key, localFile.absolutePath)
                            modified = true
                        }
                    }
                }
            }

            // Also check if any background or custom PFP type is configured but its path is empty or pointing to a missing file
            val allKeys = prefs.all.keys.toList()
            allKeys.forEach { key ->
                if (key.endsWith("_bg_type")) {
                    val prefix = key.removeSuffix("_bg_type")
                    val type = prefs.getString(key, "none") ?: "none"
                    if (type == "wallpaper" || type == "picture" || type == "video") {
                        val pathKey = "${prefix}_bg_path"
                        val currentPath = prefs.getString(pathKey, "") ?: ""
                        val file = if (currentPath.isNotBlank()) File(currentPath) else null
                        if (file == null || !file.exists()) {
                            val candidatePng = File(bgDir, "custom_bg_${prefix}.png")
                            val candidateJpg = File(bgDir, "custom_bg_${prefix}.jpg")
                            val candidateMp4 = File(bgDir, "custom_bg_${prefix}.mp4")
                            when {
                                candidatePng.exists() -> {
                                    editor.putString(pathKey, candidatePng.absolutePath)
                                    modified = true
                                }
                                candidateJpg.exists() -> {
                                    editor.putString(pathKey, candidateJpg.absolutePath)
                                    modified = true
                                }
                                candidateMp4.exists() -> {
                                    editor.putString(pathKey, candidateMp4.absolutePath)
                                    modified = true
                                }
                            }
                        }
                    }
                } else if (key.endsWith("_custom_pfp_type")) {
                    val prefix = key.removeSuffix("_custom_pfp_type")
                    val type = prefs.getString(key, "none") ?: "none"
                    if (type == "wallpaper" || type == "picture" || type == "video") {
                        val pathKey = "${prefix}_custom_pfp_path"
                        val currentPath = prefs.getString(pathKey, "") ?: ""
                        val file = if (currentPath.isNotBlank()) File(currentPath) else null
                        if (file == null || !file.exists()) {
                            val candidatePng = File(bgDir, "custom_pfp_${prefix}.png")
                            val candidateJpg = File(bgDir, "custom_pfp_${prefix}.jpg")
                            val candidateMp4 = File(bgDir, "custom_pfp_${prefix}.mp4")
                            when {
                                candidatePng.exists() -> {
                                    editor.putString(pathKey, candidatePng.absolutePath)
                                    modified = true
                                }
                                candidateJpg.exists() -> {
                                    editor.putString(pathKey, candidateJpg.absolutePath)
                                    modified = true
                                }
                                candidateMp4.exists() -> {
                                    editor.putString(pathKey, candidateMp4.absolutePath)
                                    modified = true
                                }
                            }
                        }
                    }
                }
            }

            if (modified) {
                editor.apply()
            }
        } catch (_: Exception) {}
    }

    fun listBackups(context: Context): List<File> =
        getBackupDir(context).listFiles()
            ?.filter { it.extension == "everdialer" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
