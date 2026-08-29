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
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        try {
                            mediaPlayerRef?.release()
                            val mp = MediaPlayer().apply {
                                setSurface(Surface(surfaceTexture))
                                setDataSource(videoFile.absolutePath)
                                isLooping = true
                                if (isMuted) setVolume(0f, 0f)
                                prepareAsync()
                                setOnPreparedListener { player ->
                                    player.start()
                                }
                            }
                            mediaPlayerRef = mp
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

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

    // Dim values cycle: 0% -> 25% -> 50% -> 75% -> 0%
    val dimSteps = listOf(0.0f, 0.25f, 0.50f, 0.75f)
    var dimIndex by remember {
        mutableIntStateOf(dimSteps.indexOfFirst { kotlin.math.abs(it - initialDim) < 0.1f }.coerceAtLeast(0))
    }
    val currentDim = dimSteps[dimIndex]

    // Blur values cycle: 0dp -> 12dp -> 24dp -> 36dp -> 48dp -> 0dp
    val blurSteps = listOf(0.0f, 12.0f, 24.0f, 36.0f, 48.0f)
    var blurIndex by remember {
        mutableIntStateOf(blurSteps.indexOfFirst { kotlin.math.abs(it - initialBlur) < 5f }.coerceAtLeast(0))
    }
    val currentBlur = blurSteps[blurIndex]

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
                prefs.setFloat("${p}_bg_dim", currentDim)
                prefs.setFloat("${p}_bg_blur", currentBlur)

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
            // Media Layer with Transform Gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, gestureZoom, _ ->
                            zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                            val maxPan = (zoom - 1f) * 600f
                            panX = (panX + pan.x).coerceIn(-maxPan, maxPan)
                            panY = (panY + pan.y).coerceIn(-maxPan, maxPan)
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
                            .then(if (currentBlur > 0f) Modifier.blur(currentBlur.dp) else Modifier)
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
                            .then(if (currentBlur > 0f) Modifier.blur(currentBlur.dp) else Modifier)
                    )
                }

                // Dim overlay
                if (currentDim > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = currentDim))
                    )
                }
            }

            // Top overlay bar with title and quick zoom reset
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
                        )
                    )
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Background Editor",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Pinch or drag to zoom & position",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }

                    if (zoom > 1.05f || panX != 0f || panY != 0f) {
                        Surface(
                            onClick = {
                                zoom = 1.0f
                                panX = 0f
                                panY = 0f
                            },
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Reset", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Floating Zoom Buttons (Right side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { zoom = (zoom + 0.25f).coerceAtMost(4f) },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Surface(
                    onClick = {
                        zoom = (zoom - 0.25f).coerceAtLeast(1f)
                        if (zoom == 1f) { panX = 0f; panY = 0f }
                    },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Bottom Control Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(start = 20.dp, end = 20.dp, bottom = 40.dp, top = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Separate Back Button on the LEFT side of the bottom screen
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Floating Pill-Styled Buttons (Dim, Blur, Save)
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color(0xFF1E1E1E).copy(alpha = 0.92f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        shadowElevation = 10.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Dim Button
                            Surface(
                                onClick = {
                                    dimIndex = (dimIndex + 1) % dimSteps.size
                                },
                                shape = RoundedCornerShape(24.dp),
                                color = if (currentDim > 0f) Color(0xFFFFB74D).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Brightness4,
                                        contentDescription = "Dim",
                                        tint = if (currentDim > 0f) Color(0xFFFFB74D) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        if (currentDim > 0f) "${(currentDim * 100).toInt()}%" else "Dim",
                                        color = if (currentDim > 0f) Color(0xFFFFB74D) else Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Blur Button
                            Surface(
                                onClick = {
                                    blurIndex = (blurIndex + 1) % blurSteps.size
                                },
                                shape = RoundedCornerShape(24.dp),
                                color = if (currentBlur > 0f) Color(0xFF81D4FA).copy(alpha = 0.25f)
                                        else Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.BlurOn,
                                        contentDescription = "Blur",
                                        tint = if (currentBlur > 0f) Color(0xFF81D4FA) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        if (currentBlur > 0f) "${currentBlur.toInt()}dp" else "Blur",
                                        color = if (currentBlur > 0f) Color(0xFF81D4FA) else Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Save Button
                            Surface(
                                onClick = { saveBackground() },
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        "Save",
                                        color = MaterialTheme.colorScheme.onPrimary,
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
}
