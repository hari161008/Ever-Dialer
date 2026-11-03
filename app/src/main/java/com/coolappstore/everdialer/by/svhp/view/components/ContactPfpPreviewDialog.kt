package com.coolappstore.everdialer.by.svhp.view.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.max

/**
 * Ultra-smooth, slow zoom-in/out full-screen preview for contact profile photos (PFP).
 * Features:
 * - Slowly and smoothly zooms in from a dot size (0.01f) on the PFP origin to full screen.
 * - Slowly and smoothly zooms out down to a dot size (0.01f) into the PFP origin when closed.
 * - Multi-touch finger pinch-to-zoom (up to 5x) with exact centroid tracking (zooms from the specific touch spot).
 * - Full responsive panning when zoomed in.
 * - Double-tap to quickly toggle zoom directly at the tapped spot.
 * - Fully opaque dynamic background matching light, dark, pure white, or AMOLED pure black theme.
 * - Predictive Back gesture support: swiping back smoothly shrinks and follows the back gesture.
 */
@Composable
fun ContactPfpPreviewDialog(
    visible: Boolean,
    displayName: String,
    photoUri: String?,
    transformOrigin: TransformOrigin = TransformOrigin(0.5f, 0.28f),
    shape: Shape = CircleShape,
    onDismissRequest: () -> Unit
) {
    if (!visible) return

    val prefs = koinInject<PreferenceManager>()
    val predictiveBackEnabled = remember { prefs.getBoolean(PreferenceManager.KEY_PREDICTIVE_BACK_GESTURE, true) }

    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    // Trigger smooth slow expansion from dot size once mounted
    LaunchedEffect(Unit) {
        isExpanded = true
    }

    // Finger zoom and pan states
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Controlled dismissal: smoothly and slowly shrinks back down to dot size
    fun dismissWithAnimation() {
        if (isClosing) return
        isClosing = true
        coroutineScope.launch {
            zoom = 1f
            panX = 0f
            panY = 0f
            delay(750)
            onDismissRequest()
            isClosing = false
            isExpanded = false
        }
    }

    // Predictive Back Gesture Animation states
    val predictiveBackScale = remember { Animatable(1f) }
    val predictiveBackOffsetX = remember { Animatable(0f) }

    if (predictiveBackEnabled) {
        PredictiveBackHandler(enabled = !isClosing) { progressFlow ->
            try {
                if (zoom > 1.05f) {
                    zoom = 1f
                    panX = 0f
                    panY = 0f
                    return@PredictiveBackHandler
                }
                progressFlow.collect { backEvent ->
                    val p = backEvent.progress
                    predictiveBackScale.snapTo((1f - p * 0.32f).coerceAtLeast(0.5f))
                    val edgeFactor = if (backEvent.swipeEdge == BackEventCompat.EDGE_LEFT) 1f else -1f
                    predictiveBackOffsetX.snapTo(p * 80f * edgeFactor)
                }
                dismissWithAnimation()
            } catch (e: CancellationException) {
                coroutineScope.launch {
                    predictiveBackScale.animateTo(1f, spring(stiffness = Spring.StiffnessMediumLow))
                    predictiveBackOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                }
            }
        }
    } else {
        BackHandler(enabled = !isClosing) {
            if (zoom > 1.05f) {
                zoom = 1f
                panX = 0f
                panY = 0f
            } else {
                dismissWithAnimation()
            }
        }
    }

    // Ever Dialer Settings smooth easing curve
    val settingsEasing = remember { CubicBezierEasing(0.2f, 0f, 0f, 1f) }

    // Animated scrim: 100% fully opaque when open, fades out on dismiss
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isExpanded && !isClosing) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = settingsEasing),
        label = "pfpPreviewScrim"
    )

    // Animated scale: smoothly zooms in from dot size (0.01f) to 1.0f, and back to 0.01f
    val contentScale by animateFloatAsState(
        targetValue = if (isExpanded && !isClosing) 1f else 0.01f,
        animationSpec = tween(durationMillis = 750, easing = settingsEasing),
        label = "pfpPreviewScale"
    )

    // Animated alpha
    val contentAlpha by animateFloatAsState(
        targetValue = if (isExpanded && !isClosing) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = settingsEasing),
        label = "pfpPreviewAlpha"
    )

    // Dynamic background and content colors based on current theme (light, dark, white, black/AMOLED)
    val backgroundColor = MaterialTheme.colorScheme.background
    val contentColor = MaterialTheme.colorScheme.onBackground
    val iconButtonBg = MaterialTheme.colorScheme.surfaceContainerHigh

    val effectiveScrim = (scrimAlpha * predictiveBackScale.value).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor.copy(alpha = effectiveScrim))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (zoom <= 1.05f) {
                    dismissWithAnimation()
                } else {
                    zoom = 1f
                    panX = 0f
                    panY = 0f
                }
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val viewWidth = constraints.maxWidth.toFloat()
            val viewHeight = constraints.maxHeight.toFloat()
            val center = Offset(viewWidth / 2f, viewHeight / 2f)

            // Clamp pan when zoom changes
            LaunchedEffect(zoom) {
                if (zoom <= 1.01f) {
                    panX = 0f
                    panY = 0f
                } else {
                    val curMaxPanX = max(0f, (viewWidth * (zoom - 1f)) / 2f)
                    val curMaxPanY = max(0f, (viewHeight * (zoom - 1f)) / 2f)
                    panX = panX.coerceIn(-curMaxPanX, curMaxPanX)
                    panY = panY.coerceIn(-curMaxPanY, curMaxPanY)
                }
            }

            // Outer layer: Handles the settings-style opening/closing transition from dot size + predictive back
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.transformOrigin = transformOrigin
                        scaleX = contentScale * predictiveBackScale.value
                        scaleY = contentScale * predictiveBackScale.value
                        translationX = predictiveBackOffsetX.value
                        alpha = contentAlpha * predictiveBackScale.value.coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                // Inner layer: Handles interactive multi-touch finger zoom and pan from specific touch centroid
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.transformOrigin = TransformOrigin.Center
                            scaleX = zoom
                            scaleY = zoom
                            translationX = panX
                            translationY = panY
                        }
                        .pointerInput(viewWidth, viewHeight) {
                            detectTransformGestures { centroid, pan, gestureZoom, _ ->
                                val oldZoom = zoom
                                val newZoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                zoom = newZoom

                                if (newZoom > 1.001f) {
                                    val centroidOffset = centroid - center
                                    val zoomFactor = newZoom / oldZoom
                                    val newPanX = (panX - centroidOffset.x) * zoomFactor + centroidOffset.x + pan.x
                                    val newPanY = (panY - centroidOffset.y) * zoomFactor + centroidOffset.y + pan.y

                                    val curMaxPanX = max(0f, (viewWidth * (newZoom - 1f)) / 2f)
                                    val curMaxPanY = max(0f, (viewHeight * (newZoom - 1f)) / 2f)
                                    panX = newPanX.coerceIn(-curMaxPanX, curMaxPanX)
                                    panY = newPanY.coerceIn(-curMaxPanY, curMaxPanY)
                                } else {
                                    panX = 0f
                                    panY = 0f
                                }
                            }
                        }
                        .pointerInput(viewWidth, viewHeight) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    if (zoom > 1.05f) {
                                        zoom = 1f
                                        panX = 0f
                                        panY = 0f
                                    } else {
                                        val newZoom = 2.5f
                                        val centroidOffset = tapOffset - center
                                        val targetPanX = -centroidOffset.x * (newZoom - 1f)
                                        val targetPanY = -centroidOffset.y * (newZoom - 1f)
                                        val curMaxPanX = max(0f, (viewWidth * (newZoom - 1f)) / 2f)
                                        val curMaxPanY = max(0f, (viewHeight * (newZoom - 1f)) / 2f)
                                        zoom = newZoom
                                        panX = targetPanX.coerceIn(-curMaxPanX, curMaxPanX)
                                        panY = targetPanY.coerceIn(-curMaxPanY, curMaxPanY)
                                    }
                                },
                                onTap = {
                                    if (zoom <= 1.05f) {
                                        dismissWithAnimation()
                                    } else {
                                        zoom = 1f
                                        panX = 0f
                                        panY = 0f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = displayName,
                            modifier = Modifier
                                .fillMaxWidth(0.94f)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        RivoAvatar(
                            name = displayName,
                            photoUri = null,
                            forcePersonIcon = true,
                            modifier = Modifier
                                .size(280.dp)
                                .clip(shape),
                            shape = shape
                        )
                    }
                }
            }

            // Top Header: Back Button + Contact Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .graphicsLayer { alpha = contentAlpha * predictiveBackScale.value.coerceIn(0f, 1f) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconButtonBg,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = { dismissWithAnimation() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = contentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Profile Photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
