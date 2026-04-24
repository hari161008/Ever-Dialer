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

    fun getNoteFile(context: Context, contactName: String, phoneNumber: String): File =
        File(getNotesDir(context), getFileName(contactName, phoneNumber))

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
        getNotesDir(context).listFiles()
            ?.filter { it.extension == "txt" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { file ->
                val base = file.nameWithoutExtension
                val idx = base.lastIndexOf(" - [")
                val contactName = if (idx >= 0) base.substring(0, idx) else base
                val phoneNumber = if (idx >= 0) base.substring(idx + 4).trimEnd(']') else ""
                NoteEntry(file, contactName, phoneNumber, file.readText(), file.lastModified())
            } ?: emptyList()
    } catch (_: Exception) { emptyList() }
}

data class NoteEntry(
    val file: File,
    val contactName: String,
    val phoneNumber: String,
    val content: String,
    val lastModified: Long
)
