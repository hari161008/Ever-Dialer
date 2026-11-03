package com.coolappstore.everdialer.by.svhp.view.components

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Renders a side-by-side split avatar (left half for person 1, right half for person 2)
 * for multi-call and merged conference call entries in call logs and ongoing call screen.
 */
@Composable
fun DualSplitAvatar(
    name1: String?,
    number1: String,
    photoUri1: Any? = null,
    name2: String?,
    number2: String,
    photoUri2: Any? = null,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    size: Dp = 48.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Half - Person 1
            SplitHalf(
                name = name1,
                number = number1,
                photoUri = photoUri1,
                size = size,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds()
            )

            // Middle Seam / Divider
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
            )

            // Right Half - Person 2
            SplitHalf(
                name = name2,
                number = number2,
                photoUri = photoUri2,
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
private fun SplitHalf(
    name: String?,
    number: String,
    photoUri: Any?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val hasPhoto = photoUri != null && photoUri.toString().isNotBlank()
    val isNamedContact = !name.isNullOrBlank() && name != number && !name.equals("Unknown", ignoreCase = true)
    val key = (name?.ifBlank { null } ?: number).ifBlank { "unknown" }
    val bgColor = remember(key) {
        avatarColors[kotlin.math.abs(key.hashCode()) % avatarColors.size]
    }

    Box(
        modifier = modifier.background(if (hasPhoto) Color.Transparent else bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (hasPhoto) {
            AsyncImage(
                model = photoUri,
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
