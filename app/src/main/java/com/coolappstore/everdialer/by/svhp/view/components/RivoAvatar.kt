package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import org.koin.compose.koinInject
import kotlin.math.abs

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Immutable

@Immutable
data class AvatarDisplayConfig(
    val showPicture: Boolean,
    val showFirstLetter: Boolean,
    val colorfulAvatars: Boolean,
    val solidIcons: Boolean,
    val solidIconsDynamic: Boolean,
    val isDark: Boolean,
    val solidStyle: String,
    val isSaturatedActive: Boolean,
    val solidBgColor: Color,
    val solidContentColor: Color
)

val LocalAvatarDisplayConfig = compositionLocalOf<AvatarDisplayConfig?> { null }

@Composable
fun rememberAvatarDisplayConfig(prefs: PreferenceManager, settingsVersion: Int): AvatarDisplayConfig {
    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    return remember(settingsVersion, isDark, surfaceColor, primaryColor, primaryContainer) {
        val showPicture = prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)
        val showFirstLetter = prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true)
        val colorfulAvatars = prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)
        val solidIcons = prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS, false)
        val solidIconsDynamic = prefs.getBoolean(PreferenceManager.KEY_SOLID_ICONS_DYNAMIC, false)
        val solidStyle = prefs.getSolidIconsStyle(isDark)
        val isSaturatedActive = prefs.isSaturatedForTheme(isDark)
        val (solidBgColor, solidContentColor) = if (solidStyle == PreferenceManager.SOLID_ICONS_STYLE_BRIGHT) {
            val isPrimaryBright = androidx.core.graphics.ColorUtils.calculateLuminance(primaryColor.toArgb()) > 0.45
            if (isSaturatedActive && isDark) {
                primaryColor to Color.Black
            } else {
                primaryColor to (if (isPrimaryBright) Color(0xFF1C1B1F) else Color.White)
            }
        } else {
            val container = if (!isDark && isSaturatedActive) androidx.compose.ui.graphics.lerp(primaryContainer, primaryColor, 0.30f) else primaryContainer
            val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(container.toArgb()) > 0.45
            container to (if (isBright) Color(0xFF1C1B1F) else if (isDark) Color.White else onPrimaryContainer)
        }

        AvatarDisplayConfig(
            showPicture = showPicture,
            showFirstLetter = showFirstLetter,
            colorfulAvatars = colorfulAvatars,
            solidIcons = solidIcons,
            solidIconsDynamic = solidIconsDynamic,
            isDark = isDark,
            solidStyle = solidStyle,
            isSaturatedActive = isSaturatedActive,
            solidBgColor = solidBgColor,
            solidContentColor = solidContentColor
        )
    }
}

val avatarColors = listOf(
    Color(0xFFC62828), Color(0xFFAD1457), Color(0xFF6A1B9A), Color(0xFF4527A0),
    Color(0xFF283593), Color(0xFF1565C0), Color(0xFF0277BD), Color(0xFF00838F),
    Color(0xFF00695C), Color(0xFF2E7D32), Color(0xFF558B2F), Color(0xFF9E9D24),
    Color(0xFFF9A825), Color(0xFFFF8F00), Color(0xFFE65100), Color(0xFFBF360C)
)

@Composable
fun RivoAvatar(
    name: String,
    photoUri: String? = null,
    icon: ImageVector? = null,
    /** Optional explicit tint colour for vector icon tiles. */
    iconContainerColor: Color? = null,
    forcePersonIcon: Boolean = false,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    size: androidx.compose.ui.unit.Dp? = null
) {
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
        val prefs = koinInject<PreferenceManager>()
        val settingsState by prefs.settingsChanged.collectAsState()

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

    val hasName  = name.trim().isNotEmpty()
    val colorKey = if (hasName) name else "unknown_caller"

    val (backgroundColor, contentColor) = when {
        iconContainerColor != null -> {
            if (solidIcons) {
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
            if (solidIcons) {
                if (solidIconsDynamic) solidBgColor to solidContentColor
                else {
                    val fallbackCol = avatarColors[abs(colorKey.hashCode()) % avatarColors.size]
                    val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(fallbackCol.toArgb()) > 0.45
                    fallbackCol to (if (isBright) Color(0xFF1C1B1F) else Color.White)
                }
            } else MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }
        solidIcons -> {
            if (solidIconsDynamic) solidBgColor to solidContentColor
            else if (colorfulAvatars) avatarColors[abs(colorKey.hashCode()) % avatarColors.size] to Color.White
            else solidBgColor to solidContentColor
        }
        colorfulAvatars -> avatarColors[abs(colorKey.hashCode()) % avatarColors.size] to Color.White
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    if (size != null) {
        val letterFontSize = (size.value * 0.40f).coerceIn(14f, 72f).sp
        val iconSize       = (size.value * 0.55f).coerceIn(16f, 130f).dp

        Box(
            modifier = modifier
                .size(size)
                .background(backgroundColor, shape)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
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
                    Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
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
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
                }
            }
        }
    } else {
        BoxWithConstraints(
            modifier = modifier
                .background(backgroundColor, shape)
                .clip(shape),
            contentAlignment = Alignment.Center
        ) {
            val letterFontSize = (maxWidth.value * 0.40f).coerceIn(14f, 72f).sp
            val iconSize       = (maxWidth.value * 0.55f).coerceIn(16f, 130f).dp

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
                    Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
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
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
                }
            }
        }
    }
}
