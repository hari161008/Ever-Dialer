package com.modiprincekr.thedialer.controller.util

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.modiprincekr.thedialer.R
import java.io.File
import java.io.FileOutputStream

/**
 * Manages custom looping media backgrounds (GIFs and Videos up to 30 seconds duration) for contacts.
 * Handles duration parsing from GIF binary structure and video metadata, local caching to internal storage,
 * and retrieval via contact ID or associated phone numbers.
 */
object ContactMediaManager {

    const val MAX_DURATION_MS = 30_000L // 30 seconds limit

    /**
     * Builds an [ImageLoader] configured with [ImageDecoderDecoder] (API 28+) or
     * [GifDecoder] (legacy) for continuous animated GIF playback.
     */
    fun createImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context.applicationContext)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    /**
     * Parses the total playback duration of a GIF from its raw bytes in milliseconds.
     * Evaluates standard Graphic Control Extension delay times (in 1/100s of a second).
     */
    fun parseGifDurationMs(bytes: ByteArray): Long {
        if (bytes.size < 6) return 0L
        val magic = String(bytes, 0, 6, Charsets.US_ASCII)
        if (!magic.startsWith("GIF")) return 0L

        var totalDelayCentiseconds = 0L
        var i = 6
        if (i + 7 > bytes.size) return 0L

        val packed = bytes[i + 4].toInt() and 0xFF
        val hasGct = (packed and 0x80) != 0
        val gctSize = 2 shl (packed and 0x07)
        i += 7
        if (hasGct) {
            i += 3 * gctSize
        }

        while (i < bytes.size) {
            val blockType = bytes[i].toInt() and 0xFF
            if (blockType == 0x3B) { // GIF Trailer
                break
            }
            if (blockType == 0x21) { // Extension Introducer
                if (i + 1 >= bytes.size) break
                val label = bytes[i + 1].toInt() and 0xFF
                if (label == 0xF9) { // Graphic Control Extension
                    if (i + 6 < bytes.size && (bytes[i + 2].toInt() and 0xFF) == 4) {
                        val delayLow = bytes[i + 4].toInt() and 0xFF
                        val delayHigh = bytes[i + 5].toInt() and 0xFF
                        val delay = delayLow or (delayHigh shl 8)
                        totalDelayCentiseconds += if (delay <= 1) 10 else delay
                    }
                }
                i += 2
                while (i < bytes.size) {
                    val subBlockSize = bytes[i].toInt() and 0xFF
                    i++
                    if (subBlockSize == 0) break
                    i += subBlockSize
                }
            } else if (blockType == 0x2C) { // Image Descriptor
                if (i + 9 >= bytes.size) break
                val imgPacked = bytes[i + 9].toInt() and 0xFF
                val hasLct = (imgPacked and 0x80) != 0
                val lctSize = 2 shl (imgPacked and 0x07)
                i += 10
                if (hasLct) {
                    i += 3 * lctSize
                }
                if (i >= bytes.size) break
                i++ // Skip LZW minimum code size
                while (i < bytes.size) {
                    val subBlockSize = bytes[i].toInt() and 0xFF
                    i++
                    if (subBlockSize == 0) break
                    i += subBlockSize
                }
            } else {
                i++
            }
        }
        return totalDelayCentiseconds * 10L
    }

    /**
     * Parses the playback duration of a video from a URI using [MediaMetadataRetriever].
     */
    fun parseVideoDurationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Parses the playback duration of a video from a local file.
     */
    fun parseVideoDurationFromFile(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Checks if a given file path is a video based on extension.
     */
    fun isVideo(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        val lower = path.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
                lower.endsWith(".mov") || lower.endsWith(".3gp") || lower.endsWith(".m4v")
    }

    private fun isGifStream(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = ByteArray(6)
                val read = stream.read(header)
                read >= 6 && String(header, 0, 3, Charsets.US_ASCII).equals("GIF", ignoreCase = true)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun isVideoMetadata(context: Context, uri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            hasVideo != null && hasVideo.equals("yes", ignoreCase = true)
        } catch (_: Exception) {
            false
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Reads, validates, duration-checks (for animated media), and saves a custom background
     * (Static Images, GIFs, or Videos up to 30s) for a contact into persistent internal app storage.
     * Enforces the 30-second duration cap on GIFs and videos; static images bypass duration check.
     * Returns [Result.success] with the absolute file path or [Result.failure] on violation or I/O error.
     */
    fun saveContactMedia(
        context: Context,
        contactKey: String,
        phoneNumbers: List<String>?,
        sourceUri: Uri
    ): Result<String> {
        return try {
            val cr = context.contentResolver
            val mimeType = cr.getType(sourceUri)?.lowercase() ?: ""
            val uriString = sourceUri.toString().lowercase()

            val isGif = mimeType == "image/gif" || uriString.endsWith(".gif") || isGifStream(context, sourceUri)
            val isImage = mimeType.startsWith("image/") || uriString.endsWith(".jpg") ||
                    uriString.endsWith(".jpeg") || uriString.endsWith(".png") ||
                    uriString.endsWith(".webp") || uriString.endsWith(".bmp")

            val dir = File(context.filesDir, "contact_backgrounds")
            if (!dir.exists()) dir.mkdirs()

            val sanitizedKey = contactKey.replace(Regex("[^a-zA-Z0-9_]"), "_")

            // Clean up any existing media files for this contact across all storage folders
            dir.listFiles { _, name -> name.startsWith("${sanitizedKey}_") || name.startsWith("media_${sanitizedKey}") }?.forEach { it.delete() }
            val legacyDir = File(context.filesDir, "contact_media")
            if (legacyDir.exists()) {
                legacyDir.listFiles { _, name -> name.startsWith("media_${sanitizedKey}") }?.forEach { it.delete() }
            }
            File(context.filesDir, "contact_gifs/gif_${sanitizedKey}.gif").delete()

            val targetFile: File
            val timestamp = System.currentTimeMillis()

            if (isGif) {
                val bytes = cr.openInputStream(sourceUri)?.use { it.readBytes() }
                    ?: return Result.failure(IllegalArgumentException("Could not read GIF from selected URI"))

                if (bytes.size < 6 || !String(bytes, 0, 3, Charsets.US_ASCII).equals("GIF", ignoreCase = true)) {
                    return Result.failure(IllegalArgumentException("Selected file is not a valid GIF image"))
                }

                val durationMs = parseGifDurationMs(bytes)
                if (durationMs > MAX_DURATION_MS) {
                    val sec = (durationMs + 999) / 1000
                    return Result.failure(IllegalArgumentException("GIF duration (${sec}s) exceeds the 30-second limit"))
                }

                targetFile = File(dir, "${sanitizedKey}_${timestamp}.gif")
                targetFile.writeBytes(bytes)
            } else if (isImage) {
                // Static image: bypass duration validation, persist with proper extension
                val ext = when {
                    mimeType.contains("png") || uriString.endsWith(".png") -> ".png"
                    mimeType.contains("webp") || uriString.endsWith(".webp") -> ".webp"
                    mimeType.contains("bmp") || uriString.endsWith(".bmp") -> ".bmp"
                    else -> ".jpg"
                }
                targetFile = File(dir, "${sanitizedKey}_${timestamp}$ext")
                cr.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return Result.failure(IllegalArgumentException("Could not read image from selected URI"))
            } else {
                val isVideo = mimeType.startsWith("video/") ||
                        uriString.endsWith(".mp4") || uriString.endsWith(".mkv") ||
                        uriString.endsWith(".webm") || uriString.endsWith(".mov") ||
                        uriString.endsWith(".3gp") || isVideoMetadata(context, sourceUri)

                if (!isVideo) {
                    return Result.failure(IllegalArgumentException("Selected file must be an image, GIF, or Video (up to 30s)"))
                }

                val durationMs = parseVideoDurationMs(context, sourceUri)
                if (durationMs > MAX_DURATION_MS) {
                    val sec = (durationMs + 999) / 1000
                    return Result.failure(IllegalArgumentException("Video duration (${sec}s) exceeds the 30-second limit"))
                }

                val ext = when {
                    mimeType.contains("webm") || uriString.endsWith(".webm") -> ".webm"
                    mimeType.contains("mkv") || uriString.endsWith(".mkv") -> ".mkv"
                    else -> ".mp4"
                }

                targetFile = File(dir, "${sanitizedKey}_${timestamp}$ext")
                cr.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return Result.failure(IllegalArgumentException("Could not read video from selected URI"))

                if (durationMs <= 0L) {
                    val localDuration = parseVideoDurationFromFile(targetFile)
                    if (localDuration > MAX_DURATION_MS) {
                        targetFile.delete()
                        val sec = (localDuration + 999) / 1000
                        return Result.failure(IllegalArgumentException("Video duration (${sec}s) exceeds the 30-second limit"))
                    }
                }
            }

            val savedPath = targetFile.absolutePath
            val prefs = PreferenceManager(context)
            prefs.setString("contact_${contactKey}_media_bg", savedPath)
            prefs.setString("contact_${contactKey}_gif_bg", savedPath)

            phoneNumbers?.forEach { phone ->
                val clean = normalizeNumber(phone)
                if (clean.isNotEmpty()) {
                    prefs.setString("contact_num_${clean}_media_bg", savedPath)
                    prefs.setString("contact_num_${clean}_gif_bg", savedPath)
                }
            }
            prefs.notifyChanged()

            Result.success(savedPath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resolves the saved media background path for a contact by contact ID or phone number.
     * Looks up internal storage `contact_backgrounds` and `PreferenceManager`.
     */
    fun getContactMedia(context: Context, contactId: String?, phoneNumber: String?): String? {
        val prefs = PreferenceManager(context)
        val bgDir = File(context.filesDir, "contact_backgrounds")

        if (!contactId.isNullOrBlank()) {
            val path = prefs.getString("contact_${contactId}_media_bg", null)
                ?: prefs.getString("contact_${contactId}_gif_bg", null)
            if (!path.isNullOrBlank() && File(path).exists()) return path

            // File system fallback by contact key
            if (bgDir.exists()) {
                val sanitized = contactId.replace(Regex("[^a-zA-Z0-9_]"), "_")
                val found = bgDir.listFiles { _, name -> name.startsWith("${sanitized}_") }?.firstOrNull()
                if (found != null && found.exists()) {
                    prefs.setString("contact_${contactId}_media_bg", found.absolutePath)
                    return found.absolutePath
                }
            }
        }
        if (!phoneNumber.isNullOrBlank()) {
            val clean = normalizeNumber(phoneNumber)
            if (clean.isNotEmpty()) {
                val path = prefs.getString("contact_num_${clean}_media_bg", null)
                    ?: prefs.getString("contact_num_${clean}_gif_bg", null)
                if (!path.isNullOrBlank() && File(path).exists()) return path

                if (bgDir.exists()) {
                    val found = bgDir.listFiles { _, name -> name.startsWith("${clean}_") }?.firstOrNull()
                    if (found != null && found.exists()) {
                        prefs.setString("contact_num_${clean}_media_bg", found.absolutePath)
                        return found.absolutePath
                    }
                }
            }
            // Fallback: look up contact ID via PhoneLookup
            try {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val resolvedId = cursor.getString(0)
                        if (!resolvedId.isNullOrBlank()) {
                            val path = prefs.getString("contact_${resolvedId}_media_bg", null)
                                ?: prefs.getString("contact_${resolvedId}_gif_bg", null)
                            if (!path.isNullOrBlank() && File(path).exists()) return path

                            if (bgDir.exists()) {
                                val sanitized = resolvedId.replace(Regex("[^a-zA-Z0-9_]"), "_")
                                val found = bgDir.listFiles { _, name -> name.startsWith("${sanitized}_") }?.firstOrNull()
                                if (found != null && found.exists()) {
                                    prefs.setString("contact_${resolvedId}_media_bg", found.absolutePath)
                                    return found.absolutePath
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Removes the contact media background, deleting the stored private file and preference entries.
     */
    fun removeContactMedia(context: Context, contactKey: String, phoneNumbers: List<String>?) {
        try {
            val prefs = PreferenceManager(context)
            val currentMediaPath = prefs.getString("contact_${contactKey}_media_bg", null)
            val currentGifPath = prefs.getString("contact_${contactKey}_gif_bg", null)

            listOfNotNull(currentMediaPath, currentGifPath).distinct().forEach { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }

            val sanitizedKey = contactKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
            File(context.filesDir, "contact_backgrounds").listFiles { _, name ->
                name.startsWith("${sanitizedKey}_") || name.startsWith("media_${sanitizedKey}")
            }?.forEach { it.delete() }

            prefs.remove("contact_${contactKey}_media_bg")
            prefs.remove("contact_${contactKey}_gif_bg")

            phoneNumbers?.forEach { phone ->
                val clean = normalizeNumber(phone)
                if (clean.isNotEmpty()) {
                    prefs.remove("contact_num_${clean}_media_bg")
                    prefs.remove("contact_num_${clean}_gif_bg")
                    File(context.filesDir, "contact_backgrounds").listFiles { _, name ->
                        name.startsWith("${clean}_")
                    }?.forEach { it.delete() }
                }
            }
            prefs.notifyChanged()
        } catch (_: Exception) {}
    }

    private fun normalizeNumber(number: String): String {
        return number.filter { it.isDigit() }.takeLast(10)
    }

    // --- Backwards Compatibility Aliases ---
    fun saveContactGif(context: Context, contactKey: String, phoneNumbers: List<String>?, sourceUri: Uri): Result<String> =
        saveContactMedia(context, contactKey, phoneNumbers, sourceUri)

    fun getContactGif(context: Context, contactId: String?, phoneNumber: String?): String? =
        getContactMedia(context, contactId, phoneNumber)

    fun removeContactGif(context: Context, contactKey: String, phoneNumbers: List<String>?) =
        removeContactMedia(context, contactKey, phoneNumbers)
}

typealias ContactGifManager = ContactMediaManager

/**
 * Returns a remembered [ImageLoader] tailored for animated GIF playback.
 */
@Composable
fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ContactMediaManager.createImageLoader(context)
    }
}

/**
 * Lightweight, performant ExoPlayer composable configured to play infinitely looping,
 * completely muted video backgrounds scaled to [ContentScale.Crop].
 */
@Composable
fun LoopingVideoPlayer(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(context, videoPath) {
        try {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                val uri = if (videoPath.startsWith("content://") || videoPath.startsWith("file://")) {
                    Uri.parse(videoPath)
                } else {
                    Uri.fromFile(File(videoPath))
                }
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
                playWhenReady = true
            }
        } catch (_: Exception) {
            null
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.stop()
            exoPlayer?.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                exoPlayer?.play()
            } else if (event == Lifecycle.Event.ON_STOP) {
                exoPlayer?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { ctx ->
            try {
                (LayoutInflater.from(ctx).inflate(R.layout.contact_video_player_view, null) as PlayerView).apply {
                    player = exoPlayer
                }
            } catch (_: Exception) {
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            }
        },
        update = { playerView ->
            if (playerView.player != exoPlayer) {
                playerView.player = exoPlayer
            }
        },
        modifier = modifier
    )
}

/**
 * Unified contact media background composable:
 * If a video is selected, uses [LoopingVideoPlayer] (ExoPlayer, muted, crop scale, looping).
 * If a GIF is selected, uses Coil's [AsyncImage].
 */
@Composable
fun ContactMediaBackground(
    mediaPath: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (mediaPath.isNullOrBlank()) return

    if (ContactMediaManager.isVideo(mediaPath)) {
        LoopingVideoPlayer(
            videoPath = mediaPath,
            modifier = modifier
        )
    } else {
        val imageLoader = rememberGifImageLoader()
        val imageModel = remember(mediaPath) {
            val clean = mediaPath.removePrefix("file://")
            val f = File(clean)
            if (f.exists()) f else mediaPath
        }
        AsyncImage(
            model = imageModel,
            imageLoader = imageLoader,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
