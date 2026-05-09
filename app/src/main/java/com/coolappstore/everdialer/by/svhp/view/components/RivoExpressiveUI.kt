package com.coolappstore.everdialer.by.svhp.view.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import android.os.VibratorManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.material3.ripple
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import com.coolappstore.everdialer.by.svhp.liquidglass.drawBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.drawPlainBackdrop
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.blur
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.lens
import com.coolappstore.everdialer.by.svhp.liquidglass.effects.colorControls
import com.coolappstore.everdialer.by.svhp.liquidglass.highlight.Highlight
import com.coolappstore.everdialer.by.svhp.liquidglass.LocalLiquidGlassBackdrop

// ─── App Haptics Helper ────────────────────────────────────────────────────────

/**
 * strength: "light" | "strong" | "custom"
 * customIntensity: 0f..1f, only used when strength == "custom"
 */
fun performAppHaptic(
    context: android.content.Context,
    strength: String,
    customIntensity: Float = 0.5f
) {
    try {
        val durationMs: Long
        val amplitude: Int
        when (strength) {
            "strong" -> { durationMs = 40; amplitude = VibrationEffect.DEFAULT_AMPLITUDE }
            "custom" -> {
                durationMs = (10 + customIntensity * 70).toLong().coerceIn(10, 80)
                amplitude  = (40  + (customIntensity * 215)).toInt().coerceIn(40, 255)
            }
            else -> { durationMs = 20; amplitude = 80 } // light
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        } else {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        }
    } catch (_: Exception) {}
}

fun performScrollHaptic(context: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            val vibrator = vm?.defaultVibrator
            val effect = VibrationEffect.createOneShot(12, 60)
            vibrator?.vibrate(effect)
        } else {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(12, 60)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(12L)
            }
        }
    } catch (_: Exception) {}
}

/**
 * A composable effect that triggers scroll haptics whenever the first visible item
 * of a LazyListState changes, if scroll haptics are enabled.
 */
@Composable
fun ScrollHapticsEffect(listState: LazyListState) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsVersion by prefs.settingsChanged.collectAsState()
    val scrollHapticsEnabled = remember(settingsVersion) { prefs.getBoolean(PreferenceManager.KEY_SCROLL_HAPTICS, false) }
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    var prevFirstVisibleItem by remember { mutableIntStateOf(listState.firstVisibleItemIndex) }
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (scrollHapticsEnabled) {
            val current = listState.firstVisibleItemIndex
            if (current != prevFirstVisibleItem) {
                prevFirstVisibleItem = current
                try {
                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) {
                    performScrollHaptic(context)
                }
            }
        }
    }
}

// ─── Animated Section ──────────────────────────────────────────────────────────
/**
 * Wraps content in a staggered fade+slide-up entrance animation.
 * delayMs controls when the animation fires relative to screen entry.
 */
@Composable
fun RivoAnimatedSection(
    delayMs: Long = 0L,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0L) delay(delayMs)
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(280),
        label = "sectionProgress"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 18.dp.toPx()
        }
    ) {
        content()
    }
}

// ─── Card ──────────────────────────────────────────────────────────────────────

@Composable
fun RivoExpressiveCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(28.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (title != null || icon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    if (icon != null) {
                        Icon(
                            icon, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            content()
        }
    }
}

// ─── Section Header ────────────────────────────────────────────────────────────

@Composable
fun RivoSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    )
}

// ─── Expressive Button ─────────────────────────────────────────────────────────

@Composable
fun RivoExpressiveButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = 64.dp,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) (size / 4) else (size / 2.2f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ButtonShape"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ButtonScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(size).scale(scale),
            shape = RoundedCornerShape(cornerRadius),
            color = containerColor,
            contentColor = contentColor,
            interactionSource = interactionSource,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(iconSize))
            }
        }
        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

@Composable
fun RivoStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Colored icon background
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon, null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Icon Container Helper ────────────────────────────────────────────────────
/**
 * Renders a colored square icon box with translucent tinted background.
 * iconContainerColor = null → falls back to secondaryContainer theming.
 */
@Composable
internal fun RivoIconBox(
    icon: ImageVector,
    iconContainerColor: Color?,
    modifier: Modifier = Modifier
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val iconScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.5f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "iconScale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(250),
        label = "iconAlpha"
    )

    val bgColor = iconContainerColor?.copy(alpha = 0.15f)
        ?: MaterialTheme.colorScheme.secondaryContainer
    val fgColor = iconContainerColor
        ?: MaterialTheme.colorScheme.onSecondaryContainer

    Surface(
        modifier = modifier.size(44.dp).scale(iconScale).alpha(iconAlpha),
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon, null,
                tint = fgColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── List Item ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RivoListItem(
    headline: String,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    iconContainerColor: Color? = null,
    trailingIcon: ImageVector? = null,
    avatarName: String? = null,
    photoUri: String? = null,
    onClick: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isMenuOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isMenuOpen) 0.97f else if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ListItemScale"
    )

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth().scale(scale),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = {
                        if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                            performAppHaptic(
                                context,
                                prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                                prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                            )
                        }
                        onClick()
                    },
                    onLongClick = onLongClick
                )
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarName != null || photoUri != null) {
                RivoAvatar(
                    name = avatarName ?: "",
                    photoUri = photoUri,
                    modifier = Modifier
                        .size(48.dp)
                        .then(
                            if (onAvatarClick != null)
                                Modifier.combinedClickable(onClick = onAvatarClick)
                            else Modifier
                        )
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else if (leadingIcon != null) {
                RivoIconBox(
                    icon = leadingIcon,
                    iconContainerColor = iconContainerColor
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingIcon != null) {
                Icon(
                    trailingIcon, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ─── Switch List Item ─────────────────────────────────────────────────────────

@Composable
fun RivoSwitchListItem(
    headline: String,
    supporting: String? = null,
    leadingIcon: ImageVector? = null,
    iconContainerColor: Color? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "SwitchItemScale"
    )

    Surface(
        onClick = {
            if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                performAppHaptic(
                    context,
                    prefs.getString(PreferenceManager.KEY_APP_HAPTICS_STRENGTH, "light") ?: "light",
                    prefs.getFloat(PreferenceManager.KEY_HAPTICS_CUSTOM_INTENSITY, 0.5f)
                )
            }
            onCheckedChange(!checked)
        },
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth().scale(scale),
        shadowElevation = 0.dp,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                RivoIconBox(
                    icon = leadingIcon,
                    iconContainerColor = iconContainerColor
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (supporting != null) {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}

// ─── Scroll Animated Item ─────────────────────────────────────────────────────

/**
 * Wraps a list item with a scroll-in fade+slide animation, controlled by the
 * scroll animation preference. Uses a unique composition key so that each time
 * the item is composed (or re-composed after filter changes), the entrance
 * animation replays correctly.
 */
@Composable
fun RivoScrollAnimatedItem(
    delayMs: Long = 0L,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val scrollAnimEnabled = remember { prefs.getBoolean(PreferenceManager.KEY_SCROLL_ANIMATION, true) }

    if (scrollAnimEnabled) {
        // Use a key that changes each time this composable enters composition,
        // ensuring LaunchedEffect(Unit) inside RivoAnimatedSection always fires
        // fresh — both on first load and when the item scrolls back into view.
        val animKey = remember { Any() }
        key(animKey) {
            RivoAnimatedSection(delayMs = delayMs, modifier = modifier, content = content)
        }
    } else {
        Box(modifier = modifier) { content() }
    }
}

// ─── Rivo Dropdown Menu ───────────────────────────────────────────────────────

/**
 * Styled context menu that matches Ever Dialer's card-based design.
 * Uses a Popup so the menu is statically positioned without jumping on finger release.
 * The shadow is rendered by Compose's draw.shadow (not the window elevation)
 * so it clips correctly to the rounded shape on all API levels.
 */
@Composable
fun RivoDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()
    val liquidGlass = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) }
    val lgDropdownMenu = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_LG_DROPDOWN_MENU, false) }
    val blurEffects = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_BLUR_EFFECTS, false) }
    val blurDropdownMenu = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_BLUR_DROPDOWN_MENU, false) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(expanded) {
        if (expanded) showContent = true
    }

    if (showContent) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val dimAlpha by animateFloatAsState(
                targetValue = if (expanded) 0.45f else 0f,
                animationSpec = tween(320),
                label = "dimAlpha",
                finishedListener = { if (!expanded) showContent = false }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = scaleIn(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
                        initialScale = 0.75f,
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    ) + fadeIn(tween(280)),
                    exit = scaleOut(
                        animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        targetScale = 0.85f,
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    ) + fadeOut(tween(200))
                ) {
                    val menuShape = RoundedCornerShape(35.dp)
                    val globalBackdrop = LocalLiquidGlassBackdrop.current
                    val useLgDropdown = liquidGlass && lgDropdownMenu && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && globalBackdrop != null
                    val useBlurDropdown = blurEffects && blurDropdownMenu && !useLgDropdown

                    Box(
                        modifier = modifier
                            .width(260.dp)
                            .then(
                                if (useLgDropdown) Modifier
                                else Modifier.shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    spotColor = Color.Black.copy(alpha = 0.28f),
                                    ambientColor = Color.Black.copy(alpha = 0.12f)
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                    ) {
                        val dropdownShape = if (useLgDropdown) menuShape else RoundedCornerShape(24.dp)
                        if (useLgDropdown) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .drawBackdrop(
                                        backdrop = globalBackdrop!!,
                                        shape = { menuShape },
                                        effects = {
                                            val d = density
                                            colorControls(brightness = -0.13f, saturation = 1.4f)
                                            blur(6f * d)
                                            lens(
                                                refractionHeight = 40f * d,
                                                refractionAmount = 248f * d
                                            )
                                        },
                                        highlight = { Highlight.Plain }
                                    ),
                                shape = menuShape,
                                color = Color.Black.copy(alpha = 0.25f),
                                tonalElevation = 0.dp
                            ) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
                            }
                        } else if (useBlurDropdown && globalBackdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Surface(
                                modifier = Modifier.fillMaxWidth().drawPlainBackdrop(
                                    backdrop = globalBackdrop,
                                    shape    = { dropdownShape },
                                    effects  = { blur(30f * density) }
                                ),
                                shape = dropdownShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                                tonalElevation = 0.dp
                            ) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = dropdownShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                tonalElevation = 0.dp
                            ) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) { content() }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single styled item for [RivoDropdownMenu].
 * Icons are rendered inside a tinted rounded box matching the app's icon containers.
 * Supports destructive (error-coloured) styling.
 */
@Composable
fun RivoDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    isDestructive: Boolean = false
) {
    val prefs2 = koinInject<PreferenceManager>()
    val settingsVer2 by prefs2.settingsChanged.collectAsState()
    val liquidGlass2 = remember(settingsVer2) { prefs2.getBoolean(PreferenceManager.KEY_LIQUID_GLASS, false) }

    // In liquid glass mode: white text for readability on glass surface; bright solid icon bg
    val textColor  = when {
        isDestructive -> MaterialTheme.colorScheme.error
        liquidGlass2  -> Color.White
        else          -> MaterialTheme.colorScheme.onSurface
    }
    val tintColor  = if (isDestructive) MaterialTheme.colorScheme.error else iconTint
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "rMenuItemScale"
    )

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                // Solid bright icon background; slightly lighter/brighter in liquid glass mode
                val iconBgColor = if (liquidGlass2)
                    tintColor.copy(red = (tintColor.red * 1.15f).coerceAtMost(1f),
                                   green = (tintColor.green * 1.15f).coerceAtMost(1f),
                                   blue = (tintColor.blue * 1.15f).coerceAtMost(1f),
                                   alpha = 1f)
                else tintColor.copy(alpha = 0.20f)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconBgColor,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector        = icon,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text       = text,
                style      = MaterialTheme.typography.bodyLarge,
                color      = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * A FAB that optionally shows a background-only blur effect (frosted glass).
 * When [useBlur] is true and API >= 31, a blurred background layer is drawn
 * behind the content so the icon remains sharp and fully readable.
 */
@Composable
fun RivoBlurFab(
    onClick: () -> Unit,
    shape: androidx.compose.foundation.shape.RoundedCornerShape,
    useBlur: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (useBlur) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = shape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            modifier = modifier
        ) { content() }
    } else {
        FloatingActionButton(
            onClick = onClick,
            containerColor = containerColor,
            contentColor = contentColor,
            shape = shape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp),
            modifier = modifier
        ) { content() }
    }
}
