package com.coolappstore.everdialer.by.svhp.view.components

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
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
    isMuted: Boolean = true
) {
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(videoFile.absolutePath) {
        onDispose {
            try {
                mediaPlayerRef?.stop()
                mediaPlayerRef?.release()
            } catch (_: Exception) {}
            mediaPlayerRef = null
        }
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                fun applyCenterCrop(player: MediaPlayer, viewW: Int, viewH: Int) {
                    if (viewW <= 0 || viewH <= 0) return
                    val vW = player.videoWidth
                    val vH = player.videoHeight
                    if (vW <= 0 || vH <= 0) return

                    val viewRatio = viewW.toFloat() / viewH
                    val videoRatio = vW.toFloat() / vH
                    var scaleX = 1f
                    var scaleY = 1f
                    if (videoRatio > viewRatio) {
                        // Video is wider than view -> scale X to crop sides and maintain aspect ratio
                        scaleX = videoRatio / viewRatio
                    } else {
                        // Video is taller than view -> scale Y to crop top/bottom and maintain aspect ratio
                        scaleY = viewRatio / videoRatio
                    }
                    val matrix = android.graphics.Matrix().apply {
                        setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
                    }
                    setTransform(matrix)
                }

                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        try {
                            mediaPlayerRef?.release()
                            val mp = MediaPlayer().apply {
                                setSurface(Surface(surfaceTexture))
                                setDataSource(videoFile.absolutePath)
                                isLooping = true
                                if (isMuted) setVolume(0f, 0f)
                                setOnVideoSizeChangedListener { player, _, _ ->
                                    applyCenterCrop(player, width, height)
                                }
                                setOnPreparedListener { player ->
                                    applyCenterCrop(player, width, height)
                                    player.start()
                                }
                                prepareAsync()
                            }
                            mediaPlayerRef = mp
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        mediaPlayerRef?.let { mp ->
                            applyCenterCrop(mp, width, height)
                        }
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        try {
                            mediaPlayerRef?.stop()
                            mediaPlayerRef?.release()
                        } catch (_: Exception) {}
                        mediaPlayerRef = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier
    )
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
                        iconTint = Color(0xFFE53935),
                        iconContainerColor = Color(0xFFE53935).copy(alpha = 0.12f),
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
                        iconTint = Color(0xFF2196F3),
                        iconContainerColor = Color(0xFF2196F3).copy(alpha = 0.12f),
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
                        iconTint = Color(0xFF4CAF50),
                        iconContainerColor = Color(0xFF4CAF50).copy(alpha = 0.12f),
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
                        iconTint = Color(0xFF9C27B0),
                        iconContainerColor = Color(0xFF9C27B0).copy(alpha = 0.12f),
                        title = "Custom Video",
                        subtitle = "Pick a looping video background",
                        isSelected = currentType == "video",
                        onClick = {
                            videoPickerLauncher.launch("video/*")
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
private fun BackgroundOptionItem(
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
 * - Floating pill-styled buttons: Save, Dim, Blur
 * - Separate Back button on the left side in the bottom of the screen
 */
@Composable
fun CustomBackgroundEditorDialog(
    target: CustomBackgroundTarget,
    mediaFile: File,
    isVideo: Boolean,
    bgType: String,
    initialZoom: Float = 1.0f,
    initialPanX: Float = 0f,
    initialPanY: Float = 0f,
    initialDim: Float = 0f,
    initialBlur: Float = 0f,
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

    // Active adjustable slider: null | "dim" | "blur"
    var activeSlider by remember { mutableStateOf<String?>(null) }

    var isSaving by remember { mutableStateOf(false) }

    fun saveBackground() {
        if (isSaving) return
        isSaving = true
        scope.launch(Dispatchers.IO) {
            try {
                val bgDir = File(context.filesDir, "backgrounds").apply { mkdirs() }
                val ext = if (isVideo) "mp4" else "png"
                val destFile = File(bgDir, "custom_bg_${target.prefix}.$ext")
                mediaFile.copyTo(destFile, overwrite = true)

                val p = target.prefix
                prefs.setString("${p}_bg_type", bgType)
                prefs.setString("${p}_bg_path", destFile.absolutePath)
                prefs.setFloat("${p}_bg_zoom", zoom)
                prefs.setFloat("${p}_bg_pan_x", panX)
                prefs.setFloat("${p}_bg_pan_y", panY)
                prefs.setFloat("${p}_bg_dim", dimValue)
                prefs.setFloat("${p}_bg_blur", blurValue)

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
        onDismissRequest = onDismiss,
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

                        if (zoom > 1.05f || panX != 0f || panY != 0f || dimValue > 0f || blurValue > 0f) {
                            FilledTonalButton(
                                onClick = {
                                    zoom = 1.0f
                                    panX = 0f
                                    panY = 0f
                                    dimValue = 0f
                                    blurValue = 0f
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
                // Interactive Adjustable Slider Panel (Dim or Blur)
                AnimatedVisibility(
                    visible = activeSlider != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        shadowElevation = 10.dp,
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
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
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
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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

                    // M3 Expressive Floating Action Bar (Dim, Blur, Save)
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
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
                                border = if (isDimActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
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
                                border = if (isBlurActive) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary) else null,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
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
