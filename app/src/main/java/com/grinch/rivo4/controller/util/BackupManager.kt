package com.grinch.rivo4.controller.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private const val PREFS_NAME = "rivo_prefs"
    private const val BACKUP_DIR = "EverDialer"

    fun getBackupDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "Backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createBackup(context: Context): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(getBackupDir(context), "EverDialer_Backup_$timestamp.everdialer")

            ZipOutputStream(FileOutputStream(backupFile)).use { zip ->
                // 1. Backup SharedPreferences
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val prefsJson = prefsToJson(prefs)
                zip.putNextEntry(ZipEntry("prefs.json"))
                zip.write(prefsJson.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 2. Backup notes
                val notesDir = NoteManager.getNotesDir(context)
                notesDir.listFiles()?.filter { it.extension == "txt" }?.forEach { noteFile ->
                    zip.putNextEntry(ZipEntry("notes/${noteFile.name}"))
                    FileInputStream(noteFile).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            backupFile
        } catch (_: Exception) { null }
    }

    fun restoreBackup(context: Context, backupFile: File): Boolean {
        return try {
            ZipInputStream(FileInputStream(backupFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "prefs.json" -> {
                            val json = zip.readBytes().toString(Charsets.UTF_8)
                            restorePrefs(context, json)
                        }
                        entry.name.startsWith("notes/") -> {
                            val fileName = entry.name.removePrefix("notes/")
                            if (fileName.isNotEmpty()) {
                                val noteFile = File(NoteManager.getNotesDir(context), fileName)
                                noteFile.parentFile?.mkdirs()
                                FileOutputStream(noteFile).use { zip.copyTo(it) }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            true
        } catch (_: Exception) { false }
    }

    private fun prefsToJson(prefs: SharedPreferences): String {
        val json = JSONObject()
        prefs.all.forEach { (key, value) ->
            when (value) {
                is Boolean -> json.put(key, value)
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is Float -> json.put(key, value)
                is String -> json.put(key, value)
                is Set<*> -> json.put(key, value.joinToString(","))
            }
        }
        return json.toString()
    }

    private fun restorePrefs(context: Context, json: String) {
        try {
            val jsonObj = JSONObject(json)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            jsonObj.keys().forEach { key ->
                when (val value = jsonObj.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                }
            }
            editor.apply()
        } catch (_: Exception) {}
    }

    fun listBackups(context: Context): List<File> =
        getBackupDir(context).listFiles()
            ?.filter { it.extension == "everdialer" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
