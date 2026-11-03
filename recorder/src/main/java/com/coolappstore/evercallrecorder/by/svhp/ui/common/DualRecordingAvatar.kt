package com.coolappstore.evercallrecorder.by.svhp.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val dualAvatarColors = listOf(
    Color(0xFFC62828), Color(0xFFAD1457), Color(0xFF6A1B9A), Color(0xFF4527A0),
    Color(0xFF283593), Color(0xFF1565C0), Color(0xFF0277BD), Color(0xFF00838F),
    Color(0xFF00695C), Color(0xFF2E7D32), Color(0xFF558B2F), Color(0xFF9E9D24),
    Color(0xFFF9A825), Color(0xFFFF8F00), Color(0xFFE65100), Color(0xFFBF360C)
)

/**
 * Renders a side-by-side split avatar (left half for person 1, right half for person 2)
 * for multi-call and merged conference call entries in recording logs.
 */
@Composable
fun DualRecordingAvatar(
    name1: String?,
    number1: String,
    photo1: ImageBitmap?,
    name2: String?,
    number2: String,
    photo2: ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Half: Person 1
            HalfAvatar(
                name = name1,
                number = number1,
                photo = photo1,
                size = size,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
            )

            // Vertical Seam / Divider
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            )

            // Right Half: Person 2
            HalfAvatar(
                name = name2,
                number = number2,
                photo = photo2,
                size = size,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
            )
        }
    }
}

@Composable
private fun HalfAvatar(
    name: String?,
    number: String,
    photo: ImageBitmap?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val isNamedContact = !name.isNullOrBlank() && name != number && !name.equals("Unknown", ignoreCase = true)
    val key = (name?.ifBlank { null } ?: number).ifBlank { "unknown" }
    val bgColor = remember(key) {
        dualAvatarColors[kotlin.math.abs(key.hashCode()) % dualAvatarColors.size]
    }

    Box(
        modifier = modifier.background(if (photo != null) Color.Transparent else bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = name ?: number,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isNamedContact) {
            Text(
                text = name!!.trim().take(1).uppercase(),
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}
