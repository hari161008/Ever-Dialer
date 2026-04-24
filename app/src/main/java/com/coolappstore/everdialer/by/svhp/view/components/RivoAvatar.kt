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
    val settingsState by prefs.settingsChanged.collectAsState()

    val showPicture     = prefs.getBoolean(PreferenceManager.KEY_SHOW_PICTURE, true)
    val showFirstLetter = prefs.getBoolean(PreferenceManager.KEY_SHOW_FIRST_LETTER, true)
    val colorfulAvatars = prefs.getBoolean(PreferenceManager.KEY_COLORFUL_AVATARS, true)

    val avatarColors = listOf(
        Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
        Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF26C6DA),
        Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157),
        Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFFFA726), Color(0xFFFF7043)
    )

    val hasName = name.trim().isNotEmpty()

    val colorKey = if (hasName) name else "unknown_caller"

    val (backgroundColor, contentColor) = when {
        iconContainerColor != null -> {
            iconContainerColor.copy(alpha = 0.18f) to iconContainerColor
        }
        colorfulAvatars -> {
            avatarColors[abs(colorKey.hashCode()) % avatarColors.size] to Color.White
        }
        else -> {
            MaterialTheme.colorScheme.secondaryContainer to
                    MaterialTheme.colorScheme.onSecondaryContainer
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(backgroundColor, shape)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        // Scale letter size proportionally with the avatar box (40% of width)
        val letterFontSize = (maxWidth.value * 0.40f).coerceIn(14f, 72f).sp
        val iconSize = (maxWidth.value * 0.55f).coerceIn(16f, 48f).dp

        if (showPicture && !photoUri.isNullOrEmpty()) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        } else if (showFirstLetter && hasName) {
            Text(
                text = name.trim().take(1).uppercase(),
                fontSize = letterFontSize,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                lineHeight = letterFontSize
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
