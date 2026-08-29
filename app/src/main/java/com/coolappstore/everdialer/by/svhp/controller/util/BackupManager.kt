package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
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

    const val PREFS_RIVO = "rivo_prefs"
    const val PREFS_RECORDER = "evercallrecorder_prefs"
    const val PREFS_RECORDER_FAV = "home_favourites"
    const val PREFS_RECORDER_NOTES = "recording_notes"
    const val PREFS_RECORDER_SORT = "sort_config"
    const val PREFS_RECORDER_DURATION = "recording_duration"
    const val PREFS_NS_MASTER = "network_switch_master_switch"
    const val PREFS_NS_AUTO = "network_switch_automation_flags"

    val KNOWN_PREFS = listOf(
        PREFS_RIVO,
        PREFS_RECORDER,
        PREFS_RECORDER_FAV,
        PREFS_RECORDER_NOTES,
        PREFS_RECORDER_SORT,
        PREFS_RECORDER_DURATION,
        PREFS_NS_MASTER,
        PREFS_NS_AUTO
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

    fun writeBackup(
        context: Context,
        outputStream: OutputStream,
        backupSettings: Boolean = true,
        backupCallingCards: Boolean = true
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

                // 3. Backup Calling Cards Media (Backgrounds: images, videos, wallpapers)
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
                        if (key.endsWith("_bg_path") && value is String && value.isNotBlank()) {
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

                if (backupCallingCards) {
                    // 4. Backup notes & calling cards text files
                    val notesDir = NoteManager.getNotesDir(context)
                    notesDir.listFiles()?.filter { it.extension == "txt" }?.forEach { noteFile ->
                        zip.putNextEntry(ZipEntry("notes/${noteFile.name}"))
                        FileInputStream(noteFile).use { it.copyTo(zip) }
                        zip.closeEntry()
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
        backupCallingCards: Boolean = true
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(getBackupDir(context), "EverDialer_Backup_$timestamp.everdialer")
            val ok = FileOutputStream(backupFile).use { outputStream ->
                writeBackup(context, outputStream, backupSettings, backupCallingCards)
            }
            if (ok) backupFile else null
        } catch (_: Exception) {
            null
        }
    }

    fun restoreBackup(context: Context, backupFile: File): Boolean {
        return try {
            var restoredAny = false
            var restoredRivoFromPrefsDir = false

            ZipInputStream(FileInputStream(backupFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name.startsWith("prefs/") && name.endsWith(".json") -> {
                            val prefName = name.removePrefix("prefs/").removeSuffix(".json")
                            val json = zip.readBytes().toString(Charsets.UTF_8)
                            restorePrefs(context, prefName, json)
                            if (prefName == PREFS_RIVO) {
                                restoredRivoFromPrefsDir = true
                            }
                            restoredAny = true
                        }
                        name == "prefs.json" -> {
                            val json = zip.readBytes().toString(Charsets.UTF_8)
                            if (!restoredRivoFromPrefsDir) {
                                restorePrefs(context, PREFS_RIVO, json)
                            }
                            restoredAny = true
                        }
                        name.startsWith("datastore/") -> {
                            val relativePath = name.removePrefix("datastore/")
                            if (relativePath.isNotEmpty()) {
                                val dsFile = File(File(context.filesDir, "datastore"), relativePath)
                                dsFile.parentFile?.mkdirs()
                                FileOutputStream(dsFile).use { zip.copyTo(it) }
                                restoredAny = true
                            }
                        }
                        name.startsWith("backgrounds/") -> {
                            val fileName = name.removePrefix("backgrounds/")
                            if (fileName.isNotEmpty()) {
                                val bgDir = getBackgroundsDir(context)
                                val bgFile = File(bgDir, fileName)
                                bgFile.parentFile?.mkdirs()
                                FileOutputStream(bgFile).use { zip.copyTo(it) }
                                restoredAny = true
                            }
                        }
                        name.startsWith("notes/") -> {
                            val fileName = name.removePrefix("notes/")
                            if (fileName.isNotEmpty()) {
                                val noteFile = File(NoteManager.getNotesDir(context), fileName)
                                noteFile.parentFile?.mkdirs()
                                FileOutputStream(noteFile).use { zip.copyTo(it) }
                                restoredAny = true
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            // Fix and validate all background paths after restore so they point to current device directories
            fixBackgroundPathsAfterRestore(context)

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

            restoredAny
        } catch (_: Exception) {
            false
        }
    }

    private fun filterCallingCardPrefs(prefs: SharedPreferences): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        prefs.all.forEach { (key, value) ->
            if (value != null) {
                if (key.startsWith("contact_") ||
                    key.startsWith("incoming_bg_") ||
                    key.startsWith("ongoing_bg_") ||
                    key.startsWith("incoming_font_") ||
                    key.startsWith("ongoing_font_") ||
                    key.startsWith("incoming_elements_") ||
                    key.startsWith("ongoing_elements_") ||
                    key == PreferenceManager.KEY_INCOMING_SHOW_CONTACT_PFP ||
                    key == PreferenceManager.KEY_ONGOING_SHOW_CONTACT_PFP
                ) {
                    result[key] = value
                }
            }
        }
        return result
    }

    private fun mapToJson(map: Map<String, Any>): String {
        val json = JSONObject()
        val meta = JSONObject()
        map.forEach { (key, value) ->
            when (value) {
                is Boolean -> json.put(key, value)
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is Float -> {
                    json.put(key, value.toDouble())
                    meta.put(key, "float")
                }
                is String -> json.put(key, value)
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
        val meta = JSONObject() // store type hints for ambiguous types
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> json.put(key, value)
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is Float -> {
                    json.put(key, value.toDouble())
                    meta.put(key, "float")
                }
                is String -> json.put(key, value)
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

    private fun restorePrefs(context: Context, prefName: String, json: String) {
        try {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val raw = JSONObject(json)
            val jsonObj = if (raw.has("data")) raw.getJSONObject("data") else raw
            val meta = if (raw.has("meta")) raw.getJSONObject("meta") else JSONObject()

            val bgDir = getBackgroundsDir(context)

            jsonObj.keys().forEach { key ->
                val typeHint = meta.optString(key, "")
                when {
                    typeHint == "string_set" -> {
                        val arr = jsonObj.optJSONArray(key)
                        if (arr != null) {
                            val set = mutableSetOf<String>()
                            for (i in 0 until arr.length()) {
                                set.add(arr.getString(i))
                            }
                            editor.putStringSet(key, set)
                        }
                    }
                    typeHint == "float" -> {
                        editor.putFloat(key, jsonObj.getDouble(key).toFloat())
                    }
                    else -> {
                        when (val value = jsonObj.get(key)) {
                            is Boolean -> editor.putBoolean(key, value)
                            is Int -> editor.putInt(key, value)
                            is Long -> {
                                if (value in Int.MIN_VALUE..Int.MAX_VALUE) editor.putInt(key, value.toInt())
                                else editor.putLong(key, value)
                            }
                            is Double -> editor.putFloat(key, value.toFloat())
                            is String -> {
                                // Remap background path to current device backgrounds dir if applicable
                                val finalStr = if (key.endsWith("_bg_path") && value.isNotBlank()) {
                                    val fileName = File(value).name
                                    val localFile = File(bgDir, fileName)
                                    if (localFile.exists()) localFile.absolutePath else value
                                } else {
                                    value
                                }
                                editor.putString(key, finalStr)
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
     * Fixes any background file paths in SharedPreferences after a restore.
     * Ensures all background paths point to existing files in the current device's backgrounds directory.
     */
    private fun fixBackgroundPathsAfterRestore(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_RIVO, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val bgDir = getBackgroundsDir(context)
            var modified = false

            prefs.all.forEach { (key, value) ->
                if (key.endsWith("_bg_path") && value is String && value.isNotBlank()) {
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

            // Also check if any background type is configured but its path is empty or pointing to a missing file
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
                            val candidateMp4 = File(bgDir, "custom_bg_${prefix}.mp4")
                            when {
                                candidatePng.exists() -> {
                                    editor.putString(pathKey, candidatePng.absolutePath)
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
