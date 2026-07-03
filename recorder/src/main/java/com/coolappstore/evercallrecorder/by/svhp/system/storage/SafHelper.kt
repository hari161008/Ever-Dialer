/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.system.storage

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * SafHelper provides utility functions for working with the Android Storage Access Framework (SAF).
 *
 * Users explicitly grant access to a folder via the system document-tree picker.
 */
object SafHelper {

    /**
     * Holds the result of a successful [createAudioFile] call.
     *
     * @param uri         The content URI of the newly created file (e.g. content://…).
     * @param descriptor  An open [ParcelFileDescriptor] in read-write mode.
     *                    Must be closed after use (after [ScrcpyAudioMuxer] finalises the container).
     * @param displayName A human-readable path for logging (e.g. "Recordings/call_incoming_….webm").
     */
    data class SafResult(
        val uri: Uri,
        val descriptor: ParcelFileDescriptor,
        val displayName: String
    )

    /**
     * Creates a new audio file using whichever storage mode the user has configured in
     * [preferences] - either the SAF-chosen folder, or the app's own private internal storage.
     *
     * @param context     App context used to resolve the destination and open the FD.
     * @param preferences The app-wide [AppPreferences] used to determine the active [AppPreferences.StorageMode].
     * @param fileName    The desired file name including extension (e.g. "call_incoming_….webm").
     * @param mimeType    The MIME type of the file (e.g. "audio/webm" for Opus, "audio/mp4" for AAC). Only used in SAF mode.
     * @return A [SafResult] with the URI, open FD, and display name; or null on failure or if no storage mode is configured.
     */
    fun createAudioFile(context: Context, preferences: AppPreferences, fileName: String, mimeType: String): SafResult? {
        return when (preferences.getStorageMode()) {
            AppPreferences.StorageMode.PRIVATE    -> createPrivateAudioFile(context, fileName)
            AppPreferences.StorageMode.SAF_FOLDER -> {
                val folderUri = preferences.getRecordingFolderUri() ?: return null
                createAudioFile(context, folderUri, fileName, mimeType)
            }
            null -> null
        }
    }

    /**
     * Creates a new audio file inside the user-chosen SAF folder.
     *
     * @param context    App context used to resolve the [DocumentFile] and open the FD.
     * @param folderUri  The tree URI of the destination folder (from the document-tree picker).
     * @param fileName   The desired file name including extension (e.g. "call_incoming_….webm").
     * @param mimeType   The MIME type of the file (e.g. "audio/webm" for Opus, "audio/mp4" for AAC).
     * @return A [SafResult] with the URI, open FD, and display name; or null on failure.
     */
    fun createAudioFile(context: Context, folderUri: Uri, fileName: String, mimeType: String): SafResult? {
        val directory = DocumentFile.fromTreeUri(context, folderUri) ?: return null
        if (!directory.canWrite()) return null

        val newFile = directory.createFile(mimeType, fileName) ?: return null
        // Open the file in read-write mode so MediaMuxer can seek back to write headers.
        val fileDescriptor = context.contentResolver.openFileDescriptor(newFile.uri, "rw") ?: return null
        val displayName = "${directory.name}/$fileName"
        return SafResult(newFile.uri, fileDescriptor, displayName)
    }

    /**
     * Returns true if [folderUri] points to an existing, writable SAF folder.
     * Used to validate the user's chosen recording folder before starting a session.
     *
     * @param context   App context used to resolve the [DocumentFile].
     * @param folderUri The tree URI to validate, or null.
     * @return true if the folder exists and is writable; false if null or inaccessible.
     */
    @OptIn(ExperimentalContracts::class)
    fun isFolderValid(context: Context, folderUri: Uri?): Boolean {
        // Tells the compiler: if we returns true, folderUri is not null. Prevent false compiler error and warnings.
        contract {
            returns(true) implies (folderUri != null)
        }
        if (folderUri == null) return false
        val directory = DocumentFile.fromTreeUri(context, folderUri)
        return directory != null && directory.exists() && directory.canWrite()
    }

    /**
     * Returns a human-readable display name for a SAF folder URI.
     * Used in the Settings screen to show which folder recordings are saved to.
     *
     * @param context   App context used to resolve the [DocumentFile].
     * @param folderUri The tree URI, or null.
     * @return The folder name (e.g. "Recordings"), or null.
     */
    fun getFolderDisplayNameOrNull(context: Context, folderUri: Uri?): String? {
        if (folderUri == null) return null
        val directory = DocumentFile.fromTreeUri(context, folderUri)
        return directory?.name
    }

    // ── Private (in-app) storage ────────────────────────────────────────────

    /** Name of the sub-folder, inside the app's private internal storage, used for recordings saved with [AppPreferences.StorageMode.PRIVATE]. */
    private const val PRIVATE_STORAGE_DIR_NAME = "call_recordings"

    /** The authority of the [FileProvider] used to expose private-storage recordings as content:// URIs (for playback and sharing). */
    fun getPrivateStorageAuthority(context: Context): String = "${context.packageName}.provider"

    /**
     * Returns (and creates if needed) the private, app-internal directory used to store recordings
     * when the user picks the "Save privately" storage option. This directory lives inside the app's
     * internal storage ([Context.getFilesDir]), which other apps cannot access without root, and which
     * is automatically removed if the app is uninstalled.
     */
    fun getPrivateStorageDir(context: Context): File {
        val dir = File(context.filesDir, PRIVATE_STORAGE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Returns true if [uri] points to a file exposed through the app's private-storage [FileProvider]. */
    fun isPrivateStorageUri(context: Context, uri: Uri): Boolean =
        uri.scheme == "content" && uri.authority == getPrivateStorageAuthority(context)

    /**
     * Creates a new audio file inside the app's private internal storage and exposes it through a
     * content:// URI (via [FileProvider]) so it stays fully compatible with playback and sharing,
     * just like a SAF-backed recording.
     *
     * @param context  App context used to resolve the destination directory and open the FD.
     * @param fileName The desired file name including extension (e.g. "call_incoming_….webm").
     * @return A [SafResult] with the URI, open FD, and display name; or null on failure.
     */
    fun createPrivateAudioFile(context: Context, fileName: String): SafResult? {
        return try {
            val file = File(getPrivateStorageDir(context), fileName)
            if (!file.createNewFile()) return null
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
            val uri = FileProvider.getUriForFile(context, getPrivateStorageAuthority(context), file)
            SafResult(uri, fileDescriptor, "Private storage/$fileName")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves the underlying [File] for a private-storage content:// [uri] previously created via
     * [createPrivateAudioFile]. Returns null if [uri] does not point to the private storage directory.
     */
    fun privateFileFromUri(context: Context, uri: Uri): File? {
        if (!isPrivateStorageUri(context, uri)) return null
        val name = uri.lastPathSegment ?: return null
        return File(getPrivateStorageDir(context), name)
    }

    // ── Storage-mode-agnostic helpers ───────────────────────────────────────

    /**
     * Returns true if the recording storage is fully configured for [preferences], regardless of
     * which [AppPreferences.StorageMode] is selected (a valid SAF folder, or the private mode which
     * is always considered ready since it needs no extra permission).
     */
    fun isStorageConfigured(context: Context, preferences: AppPreferences): Boolean {
        return when (preferences.getStorageMode()) {
            AppPreferences.StorageMode.PRIVATE    -> true
            AppPreferences.StorageMode.SAF_FOLDER -> isFolderValid(context, preferences.getRecordingFolderUri())
            null                                   -> false
        }
    }

    /**
     * Deletes a recording at [uri], regardless of whether it lives in the user-chosen SAF folder or
     * in the app's private internal storage.
     */
    fun deleteRecording(context: Context, uri: Uri): Boolean {
        return try {
            if (isPrivateStorageUri(context, uri)) {
                context.contentResolver.delete(uri, null, null) > 0
            } else {
                DocumentFile.fromSingleUri(context, uri)?.delete() == true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Renames a recording at [uri] to [newName], regardless of whether it lives in the user-chosen
     * SAF folder or in the app's private internal storage.
     */
    fun renameRecording(context: Context, uri: Uri, newName: String): Boolean {
        return try {
            if (isPrivateStorageUri(context, uri)) {
                val file = privateFileFromUri(context, uri) ?: return false
                file.renameTo(File(file.parentFile, newName))
            } else {
                DocumentFile.fromSingleUri(context, uri)?.renameTo(newName) == true
            }
        } catch (e: Exception) {
            false
        }
    }
}
