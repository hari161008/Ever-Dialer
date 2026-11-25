package com.modiprincekr.thedialer.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.theme.expressive.PfpShapeRegistry
import org.koin.compose.koinInject
import kotlin.math.abs

/**
 * Single shared rotation angle ticker provided at the root of the app (`MainActivity`).
 * Ensures 120fps smooth scrolling with zero lag across all lists without per-item animation transitions.
 */
val LocalPfpRotationAngle = compositionLocalOf { 0f }

/**
 * Reusable Expressive Avatar Composable.
 * Clips the outer container to the user's active PFP Shape (from [PreferenceManager.getSelectedPfpShape]),
 * rotates the outer shape container by [LocalPfpRotationAngle], and applies exact counter-rotation
 * + 1.18f scale to the inner content so contact photos, fallback icons, and initial letters stay
 * upright, stationary, and unclipped.
 */
@Composable
fun ExpressiveAvatar(
    name: String,
    photoUri: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    icon: ImageVector? = null,
    iconContainerColor: Color? = null,
    forcePersonIcon: Boolean = false,
    autoColorAvatars: Boolean? = null,
    obeySolidIcons: Boolean = true,
    shapeKey: String? = null,
    animateRotation: Boolean? = null,
    onClick: (() -> Unit)? = null,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val settingsState by prefs.settingsChanged.collectAsState()

    val activeShapeKey = shapeKey ?: remember(settingsState) { prefs.getSelectedPfpShape() }
    val allowAnimation = animateRotation ?: remember(settingsState) { prefs.isAnimatePfpShapeRotation() }
    val isExpressive = PfpShapeRegistry.isExpressive(activeShapeKey) && activeShapeKey != "circle" && activeShapeKey != "square"

    val effectiveShape = remember(activeShapeKey, size) {
        when (activeShapeKey) {
            "circle" -> CircleShape
            "square" -> RoundedCornerShape((size.value * 0.35f).coerceIn(8f, 28f).dp)
            else -> PfpShapeRegistry.getShape(activeShapeKey)
        }
    }

    val sharedAngle = LocalPfpRotationAngle.current
    val effectiveAngle = if (allowAnimation && isExpressive) sharedAngle else 0f
    val shouldRotate = effectiveAngle != 0f

    val localConfig = LocalAvatarDisplayConfig.current

    val showPicture: Boolean
    val showFirstLetter: Boolean
    val colorfulAvatars: Boolean
    val solidIcons: Boolean
    val solidIconsDynamic: Boolean
    val isDark: Boolean
    val solidStyle: String
    val isSaturatedActive: Boolean
    val solidBgColor: Color
    val solidContentColor: Color

    if (localConfig != null) {
        showPicture = localConfig.showPicture
        showFirstLetter = localConfig.showFirstLetter
        colorfulAvatars = localConfig.colorfulAvatars
        solidIcons = localConfig.solidIcons
        solidIconsDynamic = localConfig.solidIconsDynamic
        isDark = localConfig.isDark
        solidStyle = localConfig.solidStyle
        isSaturatedActive = localConfig.isSaturatedActive
        solidBgColor = localConfig.solidBgColor
        solidContentColor = localConfig.solidContentColor
    } else {
        showPicture     = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true) }
        showFirstLetter = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true) }
        colorfulAvatars = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true) }
        solidIcons      = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS, false) }
        solidIconsDynamic = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS_DYNAMIC, false) }
        isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
        solidStyle = remember(settingsState, isDark) { prefs.getSolidIconsStyle(isDark) }
        isSaturatedActive = remember(settingsState, isDark) { prefs.isSaturatedForTheme(isDark) }

        val (sBg, sFg) = if (solidStyle == PreferenceManager.SOLID_ICONS_STYLE_BRIGHT) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val isPrimaryBright = androidx.core.graphics.ColorUtils.calculateLuminance(primaryColor.toArgb()) > 0.45
            if (isSaturatedActive && isDark) {
                primaryColor to Color.Black
            } else {
                primaryColor to (if (isPrimaryBright) Color(0xFF1C1B1F) else Color.White)
            }
        } else {
            val container = if (!isDark && isSaturatedActive) androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, 0.30f) else MaterialTheme.colorScheme.primaryContainer
            val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(container.toArgb()) > 0.45
            container to (if (isBright) Color(0xFF1C1B1F) else if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer)
        }
        solidBgColor = sBg
        solidContentColor = sFg
    }

    val effectiveColorfulAvatars = autoColorAvatars ?: colorfulAvatars
    val effectiveSolidIcons = if (obeySolidIcons) solidIcons else false

    val hasName  = name.trim().isNotEmpty()
    val colorKey = if (hasName) name else "unknown_caller"

    val (backgroundColor, contentColor) = when {
        iconContainerColor != null -> {
            if (effectiveSolidIcons) {
                if (solidIconsDynamic) solidBgColor to solidContentColor
                else {
                    val adjusted = adjustIconColorForTheme(iconContainerColor, isDark)
                    val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(adjusted.toArgb()) > 0.45
                    adjusted to (if (isBright) Color(0xFF1C1B1F) else Color.White)
                }
            } else {
                val adjusted = adjustIconColorForTheme(iconContainerColor, isDark)
                adjusted.copy(alpha = if (isDark) 0.22f else 0.14f) to adjusted
            }
        }
        icon != null -> {
            if (effectiveSolidIcons) {
                if (solidIconsDynamic) solidBgColor to solidContentColor
                else {
                    val fallbackCol = avatarColors[abs(colorKey.hashCode()) % avatarColors.size]
                    val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(fallbackCol.toArgb()) > 0.45
                    fallbackCol to (if (isBright) Color(0xFF1C1B1F) else Color.White)
                }
            } else MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }
        effectiveSolidIcons -> {
            if (solidIconsDynamic) solidBgColor to solidContentColor
            else if (effectiveColorfulAvatars) avatarColors[abs(colorKey.hashCode()) % avatarColors.size] to Color.White
            else solidBgColor to solidContentColor
        }
        effectiveColorfulAvatars -> avatarColors[abs(colorKey.hashCode()) % avatarColors.size] to Color.White
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    val letterFontSize = (size.value * 0.40f).coerceIn(14f, 72f).sp
    val iconSize       = (size.value * 0.55f).coerceIn(16f, 130f).dp

    @Composable
    fun AvatarInnerContent() {
        if (content != null) {
            Box(contentAlignment = Alignment.Center) { content() }
        } else {
            when {
                showPicture && !photoUri.isNullOrEmpty() -> {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                icon != null -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
                !forcePersonIcon && showFirstLetter && hasName -> {
                    Text(
                        text = name.trim().take(1).uppercase(),
                        fontSize = letterFontSize,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        lineHeight = letterFontSize
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }

    if (shouldRotate) {
        Box(
            modifier = modifier
                .size(size)
                .graphicsLayer {
                    rotationZ = effectiveAngle
                    this.shape = effectiveShape
                    this.clip = true
                }
                .background(backgroundColor)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationZ = -effectiveAngle
                        scaleX = 1.18f
                        scaleY = 1.18f
                    },
                contentAlignment = Alignment.Center
            ) {
                AvatarInnerContent()
            }
        }
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(effectiveShape)
                .background(backgroundColor)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            AvatarInnerContent()
        }
    }
}
