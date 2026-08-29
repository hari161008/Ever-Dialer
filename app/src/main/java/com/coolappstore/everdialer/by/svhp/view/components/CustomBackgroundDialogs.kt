package com.coolappstore.everdialer.by.svhp.view.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.util.BackgroundMediaManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.WallpaperExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class CustomBackgroundTarget(val prefix: String, val title: String) {
    INCOMING("incoming", "Incoming Call"),
    ONGOING("ongoing", "Ongoing Call")
}

@Composable
fun LoopingVideoPlayer(
    videoFile: File,
    modifier: Modifier = Modifier,
    isMuted: Boolean = true,
    videoSpeed: Float = 1.0f
) {
    AndroidView(
        factory = { ctx ->
            SeamlessLoopingVideoView(ctx).apply {
                this.isMuted = isMuted
                this.videoSpeed = videoSpeed
                setVideoFile(videoFile)
            }
        },
        update = { view ->
            view.isMuted = isMuted
            view.videoSpeed = videoSpeed
            view.setVideoFile(videoFile)
        },
        modifier = modifier
    )
}

class SeamlessLoopingVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val textureViewA = TextureView(context)
    private val textureViewB = TextureView(context)

    private var playerA: MediaPlayer? = null
    private var playerB: MediaPlayer? = null

    private var surfaceA: Surface? = null
    private var surfaceB: Surface? = null

    private var videoFile: File? = null
    private var isPreparedA = false
    private var isPreparedB = false

    private var activePlayerIndex = 0 // 0 = A, 1 = B
    private var isCrossfading = false
    private var crossfadeAnimator: ValueAnimator? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val loopCheckRunnable = object : Runnable {
        override fun run() {
            checkLoopCrossfade()
            mainHandler.postDelayed(this, 25)
        }
    }

    var isMuted: Boolean = true
        set(value) {
            field = value
            val vol = if (value) 0f else 1f
            try { playerA?.setVolume(vol, vol) } catch (_: Exception) {}
            try { playerB?.setVolume(vol, vol) } catch (_: Exception) {}
        }

    var videoSpeed: Float = 1.0f
        set(value) {
            val clamped = value.coerceIn(0.25f, 3.0f)
            if (field == clamped) return
            field = clamped
            if (isPreparedA && playerA != null) applyPlaybackSpeed(playerA, clamped)
            if (isPreparedB && playerB != null) applyPlaybackSpeed(playerB, clamped)
        }

    init {
        addView(textureViewA, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(textureViewB, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        textureViewA.alpha = 1f
        textureViewB.alpha = 0f
        setupTextureListeners()
    }

    private fun setupTextureListeners() {
        textureViewA.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                surfaceA?.release()
                surfaceA = Surface(st)
                initPlayerA(w, h)
            }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                playerA?.let { applyCenterCrop(textureViewA, it, w, h) }
            }
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                surfaceA?.release()
                surfaceA = null
                releasePlayerA()
                return true
            }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }

        textureViewB.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                surfaceB?.release()
                surfaceB = Surface(st)
                initPlayerB(w, h)
            }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                playerB?.let { applyCenterCrop(textureViewB, it, w, h) }
            }
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                surfaceB?.release()
                surfaceB = null
                releasePlayerB()
                return true
            }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }

    fun setVideoFile(file: File) {
        if (!file.exists()) return
        val isSameFile = videoFile?.absolutePath == file.absolutePath
        videoFile = file
        if (isSameFile && (playerA != null || playerB != null)) {
            val currentActive = if (activePlayerIndex == 0) playerA else playerB
            try {
                val isReady = if (activePlayerIndex == 0) isPreparedA else isPreparedB
                if (currentActive != null && !currentActive.isPlaying && isReady) {
                    currentActive.start()
                }
            } catch (_: Exception) {}
            startMonitoring()
            return
        }

        ensureSurfaces()
        if (textureViewA.isAvailable) initPlayerA(textureViewA.width, textureViewA.height)
        if (textureViewB.isAvailable) initPlayerB(textureViewB.width, textureViewB.height)
    }

    private fun ensureSurfaces() {
        if ((surfaceA == null || !surfaceA!!.isValid) && textureViewA.isAvailable && textureViewA.surfaceTexture != null) {
            try {
                surfaceA?.release()
                surfaceA = Surface(textureViewA.surfaceTexture)
            } catch (_: Exception) {}
        }
        if ((surfaceB == null || !surfaceB!!.isValid) && textureViewB.isAvailable && textureViewB.surfaceTexture != null) {
            try {
                surfaceB?.release()
                surfaceB = Surface(textureViewB.surfaceTexture)
            } catch (_: Exception) {}
        }
    }

    private fun applyPlaybackSpeed(mp: MediaPlayer?, speed: Float) {
        if (mp == null) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val params = mp.playbackParams
                params.speed = speed
                params.pitch = 1.0f
                mp.playbackParams = params
            } catch (_: Exception) {
                try {
                    val params = android.media.PlaybackParams()
                    params.speed = speed
                    params.pitch = 1.0f
                    mp.playbackParams = params
                } catch (_: Exception) {}
            }
        }
    }

    private fun applyCenterCrop(texture: TextureView, player: MediaPlayer, viewW: Int, viewH: Int) {
        if (viewW <= 0 || viewH <= 0) return
        val vW = try { player.videoWidth } catch (_: Exception) { 0 }
        val vH = try { player.videoHeight } catch (_: Exception) { 0 }
        if (vW <= 0 || vH <= 0) return

        val viewRatio = viewW.toFloat() / viewH
        val videoRatio = vW.toFloat() / vH
        var scaleX = 1f
        var scaleY = 1f
        if (videoRatio > viewRatio) {
            scaleX = videoRatio / viewRatio
        } else {
            scaleY = viewRatio / videoRatio
        }
        val matrix = Matrix().apply {
            setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
        }
        texture.setTransform(matrix)
    }

    private fun initPlayerA(w: Int, h: Int) {
        val file = videoFile ?: return
        ensureSurfaces()
        val surf = surfaceA ?: return
        if (!file.exists() || !surf.isValid) return

        try {
            releasePlayerA()
            isPreparedA = false
            val mp = MediaPlayer().apply {
                setSurface(surf)
                setDataSource(file.absolutePath)
                isLooping = false
                val vol = if (isMuted) 0f else 1f
                setVolume(vol, vol)

                setOnVideoSizeChangedListener { p, _, _ ->
                    applyCenterCrop(textureViewA, p, w, h)
                }

                setOnPreparedListener { p ->
                    isPreparedA = true
                    applyCenterCrop(textureViewA, p, w, h)
                    applyPlaybackSpeed(p, videoSpeed)
                    if (activePlayerIndex == 0) {
                        textureViewA.alpha = 1f
                        try { p.start() } catch (_: Exception) {}
                        startMonitoring()
                    }
                }

                setOnCompletionListener { p ->
                    // Fallback loop if crossfade didn't trigger
                    if (!isCrossfading && activePlayerIndex == 0) {
                        try {
                            p.seekTo(0)
                            p.start()
                        } catch (_: Exception) {}
                    }
                }

                setOnErrorListener { p, _, _ ->
                    try { p.reset() } catch (_: Exception) {}
                    true
                }

                prepareAsync()
            }
            playerA = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initPlayerB(w: Int, h: Int) {
        val file = videoFile ?: return
        ensureSurfaces()
        val surf = surfaceB ?: return
        if (!file.exists() || !surf.isValid) return

        try {
            releasePlayerB()
            isPreparedB = false
            val mp = MediaPlayer().apply {
                setSurface(surf)
                setDataSource(file.absolutePath)
                isLooping = false
                val vol = if (isMuted) 0f else 1f
                setVolume(vol, vol)

                setOnVideoSizeChangedListener { p, _, _ ->
                    applyCenterCrop(textureViewB, p, w, h)
                }

                setOnPreparedListener { p ->
                    isPreparedB = true
                    applyCenterCrop(textureViewB, p, w, h)
                    applyPlaybackSpeed(p, videoSpeed)
                    if (activePlayerIndex == 1) {
                        textureViewB.alpha = 1f
                        try { p.start() } catch (_: Exception) {}
                        startMonitoring()
                    }
                }

                setOnCompletionListener { p ->
                    // Fallback loop if crossfade didn't trigger
                    if (!isCrossfading && activePlayerIndex == 1) {
                        try {
                            p.seekTo(0)
                            p.start()
                        } catch (_: Exception) {}
                    }
                }

                setOnErrorListener { p, _, _ ->
                    try { p.reset() } catch (_: Exception) {}
                    true
                }

                prepareAsync()
            }
            playerB = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startMonitoring() {
        mainHandler.removeCallbacks(loopCheckRunnable)
        mainHandler.post(loopCheckRunnable)
    }

    private fun triggerCrossfade() {
        if (isCrossfading) return
        val file = videoFile ?: return
        if (!file.exists()) return

        val activeIndex = activePlayerIndex
        val outgoingPlayer = if (activeIndex == 0) playerA else playerB
        val incomingPlayer = if (activeIndex == 0) playerB else playerA
        val outgoingTexture = if (activeIndex == 0) textureViewA else textureViewB
        val incomingTexture = if (activeIndex == 0) textureViewB else textureViewA
        val isIncomingPrepared = if (activeIndex == 0) isPreparedB else isPreparedA

        if (outgoingPlayer == null || incomingPlayer == null || !isIncomingPrepared) {
            try {
                outgoingPlayer?.seekTo(0)
                outgoingPlayer?.start()
            } catch (_: Exception) {}
            return
        }

        try {
            val dur = outgoingPlayer.duration
            val crossfadeDuration = if (dur > 0) {
                minOf(1200L, (dur * 0.35f).toLong().coerceAtLeast(250L).coerceAtMost((dur * 0.5f).toLong().coerceAtLeast(250L)))
            } else 800L

            isCrossfading = true

            // Cancel any ongoing animator
            crossfadeAnimator?.cancel()
            crossfadeAnimator = null

            incomingPlayer.seekTo(0)
            incomingPlayer.start()

            incomingTexture.visibility = View.VISIBLE
            incomingTexture.alpha = 0f

            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = crossfadeDuration
                interpolator = LinearInterpolator()
                addUpdateListener { anim ->
                    val fraction = anim.animatedValue as Float
                    incomingTexture.alpha = fraction
                    outgoingTexture.alpha = 1f - fraction
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        try {
                            outgoingPlayer.pause()
                            outgoingPlayer.seekTo(0)
                        } catch (_: Exception) {}
                        incomingTexture.alpha = 1f
                        outgoingTexture.alpha = 0f
                        activePlayerIndex = 1 - activeIndex
                        isCrossfading = false
                        crossfadeAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        isCrossfading = false
                        crossfadeAnimator = null
                    }
                })
            }
            crossfadeAnimator = animator
            animator.start()
        } catch (e: Exception) {
            isCrossfading = false
            crossfadeAnimator = null
            try {
                outgoingPlayer.seekTo(0)
                outgoingPlayer.start()
            } catch (_: Exception) {}
        }
    }

    private fun checkLoopCrossfade() {
        val currentActivePlayer = if (activePlayerIndex == 0) playerA else playerB
        val isCurrentPrepared = if (activePlayerIndex == 0) isPreparedA else isPreparedB

        if (currentActivePlayer == null || !isCurrentPrepared) return

        try {
            if (!currentActivePlayer.isPlaying && !isCrossfading) {
                try { currentActivePlayer.start() } catch (_: Exception) {}
            }

            val pos = currentActivePlayer.currentPosition
            val dur = currentActivePlayer.duration
            if (dur <= 0) return

            val crossfadeDuration = minOf(1200L, (dur * 0.35f).toLong().coerceAtLeast(250L).coerceAtMost((dur * 0.5f).toLong().coerceAtLeast(250L)))
            val triggerPosition = dur - crossfadeDuration

            if (pos >= triggerPosition && !isCrossfading) {
                triggerCrossfade()
            }
        } catch (_: Exception) {}
    }

    private fun releasePlayerA() {
        isPreparedA = false
        try {
            playerA?.stop()
            playerA?.release()
        } catch (_: Exception) {}
        playerA = null
    }

    private fun releasePlayerB() {
        isPreparedB = false
        try {
            playerB?.stop()
            playerB?.release()
        } catch (_: Exception) {}
        playerB = null
    }

    private fun releasePlayer() {
        mainHandler.removeCallbacks(loopCheckRunnable)
        crossfadeAnimator?.cancel()
        crossfadeAnimator = null
        releasePlayerA()
        releasePlayerB()
        isCrossfading = false
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureSurfaces()
        if (playerA == null && videoFile != null && textureViewA.isAvailable) {
            initPlayerA(textureViewA.width, textureViewA.height)
        }
        if (playerB == null && videoFile != null && textureViewB.isAvailable) {
            initPlayerB(textureViewB.width, textureViewB.height)
        }
        val currentActive = if (activePlayerIndex == 0) playerA else playerB
        val isReady = if (activePlayerIndex == 0) isPreparedA else isPreparedB
        try {
            if (currentActive != null && !currentActive.isPlaying && isReady) {
                currentActive.start()
            }
        } catch (_: Exception) {}
        startMonitoring()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        releasePlayer()
    }
}

/**
 * Modern floating popup offering 4 background options: None, Wallpaper, Custom Picture, Custom Video.
 */
@Composable
fun CustomBackgroundOptionsPopup(
    target: CustomBackgroundTarget,
    currentType: String,
    onDismiss: () -> Unit,
    onSelectNone: () -> Unit,
    onOpenEditor: (file: File, isVideo: Boolean, bgType: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoadingWallpaper by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tempFile = File(context.cacheDir, "picked_image_${System.currentTimeMillis()}.png")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onOpenEditor(tempFile, false, "picture")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val tempFile = File(context.cacheDir, "picked_video_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onOpenEditor(tempFile, true, "video")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load video", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val systemFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val cr = context.contentResolver
                    val mime = cr.getType(uri) ?: ""
                    val uriStr = uri.toString().lowercase()
                    val isVideo = mime.startsWith("video") || uriStr.endsWith(".mp4") || uriStr.endsWith(".mkv") || uriStr.endsWith(".webm") || uriStr.endsWith(".mov") || uriStr.endsWith(".3gp")
                    val ext = if (isVideo) ".mp4" else ".png"
                    val tempFile = File(context.cacheDir, "picked_file_${System.currentTimeMillis()}$ext")
                    cr.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (tempFile.exists() && tempFile.length() > 0) {
                            onOpenEditor(tempFile, isVideo, if (isVideo) "video" else "picture")
                        } else {
                            Toast.makeText(context, "Could not open selected file", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to load file: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Wallpaper,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Choose Custom Background",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${target.title} Screen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Option 1: None (Default)
                    BackgroundOptionItem(
                        icon = Icons.Outlined.NotInterested,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        title = "None",
                        subtitle = "Default caller background",
                        isSelected = currentType == "none" || currentType.isEmpty(),
                        onClick = {
                            onSelectNone()
                            onDismiss()
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    // Option 2: Wallpaper
                    BackgroundOptionItem(
                        icon = Icons.Outlined.PhoneAndroid,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        title = "Wallpaper",
                        subtitle = if (isLoadingWallpaper) "Extracting wallpaper..." else "Current device system wallpaper",
                        isSelected = currentType == "wallpaper",
                        onClick = {
                            if (!isLoadingWallpaper) {
                                isLoadingWallpaper = true
                                scope.launch(Dispatchers.IO) {
                                    val wallpaperFile = WallpaperExportHelper.extractWallpaperToFile(context)
                                    withContext(Dispatchers.Main) {
                                        isLoadingWallpaper = false
                                        if (wallpaperFile != null) {
                                            onOpenEditor(wallpaperFile, false, "wallpaper")
                                        } else {
                                            Toast.makeText(context, "Could not extract system wallpaper", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    // Option 3: Custom Picture
                    BackgroundOptionItem(
                        icon = Icons.Outlined.Image,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        title = "Custom Picture",
                        subtitle = "Pick a photo or image from gallery",
                        isSelected = currentType == "picture",
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    // Option 4: Custom Video
                    BackgroundOptionItem(
                        icon = Icons.Outlined.Videocam,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        title = "Custom Video",
                        subtitle = "Pick a looping video background",
                        isSelected = currentType == "video",
                        onClick = {
                            videoPickerLauncher.launch("video/*")
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    // Option 5: File Manager (any picture or video)
                    BackgroundOptionItem(
                        icon = Icons.Outlined.FolderOpen,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        title = "Choose from File Manager",
                        subtitle = "Select any picture or video file",
                        isSelected = false,
                        onClick = {
                            systemFilePickerLauncher.launch(arrayOf("image/*", "video/*", "*/*"))
                        }
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BackgroundOptionItem(
    icon: ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                 else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconContainerColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Full-screen Interactive Background Editor Menu:
 * - Zoom in / out (interactive pinch and pan gesture + zoom buttons)
 * - Floating pill-styled buttons: Save, Dim, Blur, Speed
 * - Separate Back button on the left side in the bottom of the screen
 */
@Composable
fun CustomBackgroundEditorDialog(
    target: CustomBackgroundTarget,
    mediaFile: File,
    isVideo: Boolean,
    bgType: String,
    prefixOverride: String? = null,
    initialZoom: Float = 1.0f,
    initialPanX: Float = 0f,
    initialPanY: Float = 0f,
    initialDim: Float = 0f,
    initialBlur: Float = 0f,
    initialVideoSpeed: Float = 1.0f,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val prefs = org.koin.compose.koinInject<PreferenceManager>()
    val scope = rememberCoroutineScope()

    var zoom by remember { mutableFloatStateOf(initialZoom.coerceIn(1f, 4f)) }
    var panX by remember { mutableFloatStateOf(initialPanX) }
    var panY by remember { mutableFloatStateOf(initialPanY) }

    var dimValue by remember { mutableFloatStateOf(initialDim.coerceIn(0f, 0.90f)) }
    var blurValue by remember { mutableFloatStateOf(initialBlur.coerceIn(0f, 50f)) }
    var videoSpeed by remember { mutableFloatStateOf(initialVideoSpeed.coerceIn(0.25f, 3.0f)) }

    // Active adjustable slider: null | "dim" | "blur" | "speed"
    var activeSlider by remember { mutableStateOf<String?>(null) }

    var isSaving by remember { mutableStateOf(false) }

    fun handleDismiss() {
        scope.launch(Dispatchers.IO) {
            BackgroundMediaManager.cleanupFileIfInCache(context, mediaFile)
        }
        onDismiss()
    }

    fun saveBackground() {
        if (isSaving) return
        isSaving = true
        scope.launch(Dispatchers.IO) {
            try {
                val bgDir = File(context.filesDir, "backgrounds").apply { mkdirs() }
                val ext = if (isVideo) "mp4" else "png"
                val p = prefixOverride ?: target.prefix
                val destFile = File(bgDir, "custom_bg_${p}.$ext")
                mediaFile.copyTo(destFile, overwrite = true)

                // Delete temporary picked file from cacheDir
                BackgroundMediaManager.cleanupFileIfInCache(context, mediaFile)

                prefs.setString("${p}_bg_type", bgType)
                prefs.setString("${p}_bg_path", destFile.absolutePath)
                prefs.setFloat("${p}_bg_zoom", zoom)
                prefs.setFloat("${p}_bg_pan_x", panX)
                prefs.setFloat("${p}_bg_pan_y", panY)
                prefs.setFloat("${p}_bg_dim", dimValue)
                prefs.setFloat("${p}_bg_blur", blurValue)
                if (isVideo) {
                    prefs.setFloat("${p}_bg_video_speed", videoSpeed)
                }

                // Prune any unreferenced backgrounds
                BackgroundMediaManager.pruneOrphanedBackgrounds(context, prefs)

                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "Background saved successfully", Toast.LENGTH_SHORT).show()
                    onSaveSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSaving = false
                    Toast.makeText(context, "Failed to save background: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Media Layer with Transform Gestures - allow horizontal & vertical panning even at 1x zoom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                            val maxPanX = 1200f * zoom
                            val maxPanY = 1600f * zoom
                            panX = (panX + pan.x).coerceIn(-maxPanX, maxPanX)
                            panY = (panY + pan.y).coerceIn(-maxPanY, maxPanY)
                        }
                    }
            ) {
                if (isVideo) {
                    LoopingVideoPlayer(
                        videoFile = mediaFile,
                        videoSpeed = videoSpeed,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = panX
                                translationY = panY
                            }
                            .then(if (blurValue > 0f) Modifier.blur(blurValue.dp) else Modifier)
                    )
                } else {
                    AsyncImage(
                        model = mediaFile,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = panX
                                translationY = panY
                            }
                            .then(if (blurValue > 0f) Modifier.blur(blurValue.dp) else Modifier)
                    )
                }

                // Dim overlay
                if (dimValue > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = dimValue))
                    )
                }
            }

            // Top overlay bar with M3 Expressive floating glass pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Background Editor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Pinch or drag to position & scale",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (zoom > 1.05f || panX != 0f || panY != 0f || dimValue > 0f || blurValue > 0f || videoSpeed != 1.0f) {
                            FilledTonalButton(
                                onClick = {
                                    zoom = 1.0f
                                    panX = 0f
                                    panY = 0f
                                    dimValue = 0f
                                    blurValue = 0f
                                    videoSpeed = 1.0f
                                    activeSlider = null
                                },
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Floating Zoom Buttons (Right side) - M3 Expressive Tonal Pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = { zoom = (zoom + 0.25f).coerceAtMost(4f) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                    }
                    FilledTonalIconButton(
                        onClick = { zoom = (zoom - 0.25f).coerceAtLeast(1f) },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Bottom Control Area (M3 Expressive Sliders Panel + Floating Action Bar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(start = 16.dp, end = 16.dp, bottom = 36.dp, top = 16.dp)
            ) {
                // Interactive Adjustable Slider Panel (Dim, Blur, or Speed)
                AnimatedVisibility(
                    visible = activeSlider != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.98f),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activeSlider == "dim") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.Brightness4, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            "Dim Intensity: ${(dimValue * 100).toInt()}%",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (dimValue > 0f) {
                                            FilledTonalIconButton(
                                                onClick = { dimValue = 0f },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Reset Dim", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        FilledTonalIconButton(
                                            onClick = { activeSlider = null },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Slider", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Slider(
                                    value = dimValue,
                                    onValueChange = { dimValue = it },
                                    valueRange = 0f..0.90f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (activeSlider == "blur") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.BlurOn, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            "Blur Intensity: ${blurValue.toInt()} dp",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (blurValue > 0f) {
                                            FilledTonalIconButton(
                                                onClick = { blurValue = 0f },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Reset Blur", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        FilledTonalIconButton(
                                            onClick = { activeSlider = null },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Slider", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Slider(
                                    value = blurValue,
                                    onValueChange = { blurValue = it },
                                    valueRange = 0f..50f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.tertiary,
                                        activeTrackColor = MaterialTheme.colorScheme.tertiary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else if (activeSlider == "speed") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text(
                                            "Playback Speed: ${String.format(java.util.Locale.US, "%.2f", videoSpeed)}x",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (videoSpeed != 1.0f) {
                                            FilledTonalIconButton(
                                                onClick = { videoSpeed = 1.0f },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Reset Speed", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        FilledTonalIconButton(
                                            onClick = { activeSlider = null },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Slider", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Slider(
                                    value = videoSpeed,
                                    onValueChange = { videoSpeed = it },
                                    valueRange = 0.25f..2.5f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.secondary,
                                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { preset ->
                                        SuggestionChip(
                                            onClick = { videoSpeed = preset },
                                            label = { Text("${preset}x", style = MaterialTheme.typography.labelSmall) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = if (kotlin.math.abs(videoSpeed - preset) < 0.05f) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // M3 Expressive Back Button
                    Surface(
                        onClick = { handleDismiss() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // M3 Expressive Floating Action Bar (Dim, Blur, Speed, Save)
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Dim Button - toggles Dim Slider
                            val isDimActive = activeSlider == "dim"
                            Surface(
                                onClick = {
                                    activeSlider = if (isDimActive) null else "dim"
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDimActive) MaterialTheme.colorScheme.primaryContainer
                                        else if (dimValue > 0f) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isDimActive) 1.5.dp else 1.dp,
                                    if (isDimActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Brightness4,
                                        contentDescription = "Dim",
                                        tint = if (dimValue > 0f || isDimActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (dimValue > 0f) "${(dimValue * 100).toInt()}%" else "Dim",
                                        color = if (dimValue > 0f || isDimActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Blur Button - toggles Blur Slider
                            val isBlurActive = activeSlider == "blur"
                            Surface(
                                onClick = {
                                    activeSlider = if (isBlurActive) null else "blur"
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isBlurActive) MaterialTheme.colorScheme.tertiaryContainer
                                        else if (blurValue > 0f) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isBlurActive) 1.5.dp else 1.dp,
                                    if (isBlurActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.BlurOn,
                                        contentDescription = "Blur",
                                        tint = if (blurValue > 0f || isBlurActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (blurValue > 0f) "${blurValue.toInt()}dp" else "Blur",
                                        color = if (blurValue > 0f || isBlurActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Speed Button (video only)
                            if (isVideo) {
                                val isSpeedActive = activeSlider == "speed"
                                Surface(
                                    onClick = {
                                        activeSlider = if (isSpeedActive) null else "speed"
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSpeedActive) MaterialTheme.colorScheme.secondaryContainer
                                            else if (videoSpeed != 1.0f) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                            else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSpeedActive) 1.5.dp else 1.dp,
                                        if (isSpeedActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Speed,
                                            contentDescription = "Speed",
                                            tint = if (videoSpeed != 1.0f || isSpeedActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            if (videoSpeed != 1.0f) "${String.format(java.util.Locale.US, "%.1f", videoSpeed)}x" else "Speed",
                                            color = if (videoSpeed != 1.0f || isSpeedActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Save Button - M3 Primary Filled
                            Button(
                                onClick = { saveBackground() },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Save",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Save",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
