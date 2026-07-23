package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import java.io.File

object NoteManager {

    fun getNotesDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "Notes")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun sanitizeFileName(name: String): String =
        name.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim()

    fun getFileName(contactName: String, phoneNumber: String): String {
        val safeName = sanitizeFileName(contactName.ifBlank { "Unknown" })
        val safeNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        return "$safeName - [$safeNumber].txt"
    }

    fun getNoteFile(context: Context, contactName: String, phoneNumber: String): File {
        val dir = getNotesDir(context)
        val safeNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        // Always resolve to the single existing file for this phone number first, regardless
        // of what name is passed in. This prevents duplicate note files when the contact name
        // resolves differently at different times (e.g. "Unknown"/raw number during an ongoing
        // call vs. the actual saved contact name once resolved).
        if (safeNumber.isNotEmpty()) {
            val existing = dir.listFiles()
                ?.filter { it.extension == "txt" && it.nameWithoutExtension.endsWith("[$safeNumber]") }
                ?.maxByOrNull { it.lastModified() }
            if (existing != null) {
                val desiredName = getFileName(contactName, phoneNumber)
                if (existing.name != desiredName && contactName.isNotBlank() && contactName != "Unknown") {
                    val renamed = File(dir, desiredName)
                    if (!renamed.exists() && existing.renameTo(renamed)) return renamed
                }
                return existing
            }
        }
        return File(dir, getFileName(contactName, phoneNumber))
    }

    fun readNote(context: Context, contactName: String, phoneNumber: String): String =
        try { getNoteFile(context, contactName, phoneNumber).readText() } catch (_: Exception) { "" }

    fun readNoteByPhone(context: Context, phoneNumber: String): String {
        val safeNumber = phoneNumber.filter { it.isDigit() || it == '+' }
        if (safeNumber.isEmpty()) return ""
        return try {
            getNotesDir(context).listFiles()
                ?.filter { it.extension == "txt" && it.nameWithoutExtension.contains("[$safeNumber]") }
                ?.maxByOrNull { it.lastModified() }
                ?.readText() ?: ""
        } catch (_: Exception) { "" }
    }

    fun writeNote(context: Context, contactName: String, phoneNumber: String, content: String) {
        if (content.isBlank()) {
            deleteNote(context, contactName, phoneNumber)
            return
        }
        try {
            val file = getNoteFile(context, contactName, phoneNumber)
            file.parentFile?.mkdirs()
            file.writeText(content)
        } catch (_: Exception) {}
    }

    fun deleteNote(context: Context, contactName: String, phoneNumber: String) {
        try { getNoteFile(context, contactName, phoneNumber).delete() } catch (_: Exception) {}
    }

    fun deleteNoteFile(file: File) {
        try { file.delete() } catch (_: Exception) {}
    }

    fun getAllNotes(context: Context): List<NoteEntry> = try {
        val files = getNotesDir(context).listFiles()?.filter { it.extension == "txt" } ?: emptyList()

        // De-duplicate: multiple note files can exist for the same phone number if a note was
        // written while the contact name hadn't resolved yet and again later with the real
        // name. Keep only the most recently modified file per phone number.
        val byNumber = files.groupBy { file ->
            val base = file.nameWithoutExtension
            val idx = base.lastIndexOf(" - [")
            if (idx >= 0) base.substring(idx + 4).trimEnd(']') else ""
        }
        val deduped = byNumber.flatMap { (number, group) ->
            if (number.isBlank() || group.size <= 1) group
            else {
                val newest = group.maxByOrNull { it.lastModified() }!!
                group.filter { it !== newest }.forEach { deleteNoteFile(it) }
                listOf(newest)
            }
        }

        deduped.sortedByDescending { it.lastModified() }
            .map { file ->
                val base = file.nameWithoutExtension
                val idx = base.lastIndexOf(" - [")
                val contactName = if (idx >= 0) base.substring(0, idx) else base
                val phoneNumber = if (idx >= 0) base.substring(idx + 4).trimEnd(']') else ""
                NoteEntry(file, contactName, phoneNumber, file.readText(), file.lastModified())
            }
    } catch (_: Exception) { emptyList() }
}

data class NoteEntry(
    val file: File,
    val contactName: String,
    val phoneNumber: String,
    val content: String,
    val lastModified: Long
)
