package com.modiprincekr.thedialer.view.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.theme.expressive.PfpShapeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.io.File
import java.io.FileOutputStream

/**
 * Interactive Shape-Aware PFP Crop & Adjust Studio.
 * Lets the user zoom, pan, rotate, and frame their photo inside any of the 32 PFP Shapes
 * with an optional animated preview and export at 1024x1024 high resolution ARGB_8888.
 */
@Composable
fun PfpCropDialog(
    imageUri: Uri,
    contactId: String? = null,
    initialShapeKey: String? = null,
    onDismiss: () -> Unit,
    onCropSaved: (savedUri: Uri, selectedShapeKey: String, appliedEverywhere: Boolean) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val prefs = koinInject<PreferenceManager>()

    val defaultShapeKey = remember { initialShapeKey ?: prefs.getSelectedPfpShape() }
    var currentShapeKey by remember { mutableStateOf(defaultShapeKey) }
    var applyEverywhere by remember { mutableStateOf(false) }

    var userScale by remember { mutableFloatStateOf(1.0f) }
    var userPan by remember { mutableStateOf(Offset.Zero) }
    var userRotation by remember { mutableFloatStateOf(0f) }
    var isPreviewRotating by remember { mutableStateOf(false) }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Load source bitmap upright
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            val loaded = decodeSourceBitmap(context, imageUri)
            withContext(Dispatchers.Main) {
                sourceBitmap = loaded
            }
        }
    }

    val currentShape = remember(currentShapeKey) {
        when (currentShapeKey) {
            "circle" -> CircleShape
            "square" -> RoundedCornerShape(24.dp)
            else -> PfpShapeRegistry.getShape(currentShapeKey)
        }
    }
    val isExpressive = remember(currentShapeKey) {
        currentShapeKey != "circle" && currentShapeKey != "square" && PfpShapeRegistry.isExpressive(currentShapeKey)
    }

    val previewTransition = rememberInfiniteTransition(label = "cropPreviewRot")
    val previewRotationAngle by previewTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cropRotationAngle"
    )
    val effectivePreviewAngle = if (isPreviewRotating && isExpressive) previewRotationAngle else 0f

    val viewportSizeDp = 280.dp
    val viewportSizePx = with(density) { viewportSizeDp.toPx() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // ── Top Bar ──────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, enabled = !isSaving) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Crop & Adjust Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val bmp = sourceBitmap ?: return@Button
                            if (isSaving) return@Button
                            isSaving = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val safeId = contactId?.ifBlank { "pfp" } ?: "pfp"
                                    val pfpDir = File(context.filesDir, "contact_pfp")
                                    if (!pfpDir.exists()) pfpDir.mkdirs()
                                    val outputFile = File(pfpDir, "${safeId}_${System.currentTimeMillis()}.png")

                                    exportTransformedBitmap(
                                        source = bmp,
                                        outputFile = outputFile,
                                        viewportSizePx = viewportSizePx,
                                        userScale = userScale,
                                        userPan = userPan,
                                        userRotation = userRotation
                                    )

                                    if (applyEverywhere) {
                                        prefs.setSelectedPfpShape(currentShapeKey)
                                    }

                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        onCropSaved(Uri.fromFile(outputFile), currentShapeKey, applyEverywhere)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        android.widget.Toast.makeText(context, "Failed to save: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        enabled = !isSaving && sourceBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── Interactive Viewport ─────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(viewportSizeDp)
                            .then(
                                if (effectivePreviewAngle != 0f) Modifier.graphicsLayer { rotationZ = effectivePreviewAngle }
                                else Modifier
                            )
                            .clip(currentShape)
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, currentShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (effectivePreviewAngle != 0f) Modifier.graphicsLayer {
                                        rotationZ = -effectivePreviewAngle
                                        scaleX = 1.18f
                                        scaleY = 1.18f
                                    } else Modifier
                                )
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        userScale = (userScale * zoom).coerceIn(1.0f, 5.0f)
                                        userPan = Offset(userPan.x + pan.x, userPan.y + pan.y)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sourceBitmap != null) {
                                Image(
                                    bitmap = sourceBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            translationX = userPan.x
                                            translationY = userPan.y
                                            scaleX = userScale
                                            scaleY = userScale
                                            rotationZ = userRotation
                                        }
                                )
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                }

                // ── Helper Actions (Reset, Rotate 90, Preview Rotation) ──────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            userScale = 1.0f
                            userPan = Offset.Zero
                            userRotation = 0f
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Position")
                    }

                    OutlinedButton(
                        onClick = {
                            userRotation = (userRotation + 90f) % 360f
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Rotate 90°")
                    }

                    FilterChip(
                        selected = isPreviewRotating,
                        onClick = { isPreviewRotating = !isPreviewRotating },
                        label = { Text("Preview Rotation") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── 32 Shapes Scrollable Strip ───────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PFP Shape Mask",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "32 Shapes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        items(PfpShapeRegistry.allShapes, key = { it.key }) { item ->
                            val isSelected = item.key == currentShapeKey
                            val shape = remember(item.key) {
                                when (item.key) {
                                    "circle" -> CircleShape
                                    "square" -> RoundedCornerShape(12.dp)
                                    else -> item.shape
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        if (currentShapeKey != item.key) {
                                            currentShapeKey = item.key
                                            applyEverywhere = true
                                        }
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(shape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                            shape = shape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (sourceBitmap != null) {
                                        Image(
                                            bitmap = sourceBitmap!!.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                                scaleX = 1.15f
                                                scaleY = 1.15f
                                            }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.label.substringBefore(" ("),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // ── Apply Everywhere Checkbox ────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { applyEverywhere = !applyEverywhere }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = applyEverywhere,
                        onCheckedChange = { applyEverywhere = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Apply this shape everywhere in app",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Updates Call Logs, Contacts, Favourites, Messages, & Recorder",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Decodes source bitmap from Uri while respecting EXIF orientation.
 */
private fun decodeSourceBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = inputStream.readBytes()
        inputStream.close()
        if (bytes.isEmpty()) return null

        val exif = try {
            ExifInterface(java.io.ByteArrayInputStream(bytes))
        } catch (_: Throwable) {
            null
        }
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> {}
        }

        if (!matrix.isIdentity) {
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap && !rawBitmap.isRecycled) {
                rawBitmap.recycle()
            }
            rotated
        } else {
            rawBitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Renders the transformed/zoomed/panned source bitmap at 1024x1024 ARGB_8888 resolution into the target PNG file.
 */
private fun exportTransformedBitmap(
    source: Bitmap,
    outputFile: File,
    viewportSizePx: Float,
    userScale: Float,
    userPan: Offset,
    userRotation: Float
) {
    val outputSize = 1024
    val outputBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outputBitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        isDither = true
    }

    val srcW = source.width.toFloat()
    val srcH = source.height.toFloat()

    val matrix = Matrix()
    // 1. Move origin to center of source bitmap
    matrix.postTranslate(-srcW / 2f, -srcH / 2f)

    // 2. Base fit scale (ContentScale.Crop inside viewport)
    val baseScale = maxOf(viewportSizePx / srcW, viewportSizePx / srcH)
    matrix.postScale(baseScale, baseScale)

    // 3. User scale & rotation
    matrix.postScale(userScale, userScale)
    if (userRotation != 0f) {
        matrix.postRotate(userRotation)
    }

    // 4. User pan offset (in viewport px)
    matrix.postTranslate(userPan.x, userPan.y)

    // 5. Scale up to 1024x1024 output canvas
    val outputScale = outputSize.toFloat() / viewportSizePx
    matrix.postScale(outputScale, outputScale)

    // 6. Translate to center of 1024x1024 canvas
    matrix.postTranslate(outputSize / 2f, outputSize / 2f)

    canvas.drawBitmap(source, matrix, paint)

    FileOutputStream(outputFile).use { out ->
        outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    outputBitmap.recycle()
}
