package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.background
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

private val avatarColors = listOf(
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
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) {
    val prefs = koinInject<PreferenceManager>()
    // Collect settingsChanged once so prefs reads below are stable across recompositions.
    // Using 'by' delegation avoids an extra object allocation on every frame.
    @Suppress("UNUSED_VARIABLE")
    val settingsState by prefs.settingsChanged.collectAsState()

    val showPicture     = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true) }
    val showFirstLetter = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true) }
    val colorfulAvatars = remember(settingsState) { prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true) }

    val hasName  = name.trim().isNotEmpty()
    val colorKey = if (hasName) name else "unknown_caller"

    val (backgroundColor, contentColor) = when {
        iconContainerColor != null -> iconContainerColor.copy(alpha = 0.18f) to iconContainerColor
        colorfulAvatars -> avatarColors[abs(colorKey.hashCode()) % avatarColors.size] to Color.White
        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    BoxWithConstraints(
        modifier = modifier
            .background(backgroundColor, shape)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        val letterFontSize = (maxWidth.value * 0.40f).coerceIn(14f, 72f).sp
        val iconSize       = (maxWidth.value * 0.55f).coerceIn(16f, 48f).dp

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
            showFirstLetter && hasName -> {
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
