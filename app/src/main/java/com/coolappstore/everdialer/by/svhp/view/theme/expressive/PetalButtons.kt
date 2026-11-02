package com.coolappstore.everdialer.by.svhp.view.theme.expressive

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class PetalButtonStyle { FILLED, TONAL, OUTLINED, TEXT }

/**
 * Material 3 Expressive button: a full pill at rest that morphs to a tighter rounded rectangle
 * and squashes slightly while pressed, then springs back (the expressive "shape morph on press").
 */
@Composable
fun PetalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: PetalButtonStyle = PetalButtonStyle.FILLED,
    height: Dp = 48.dp,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 14.dp else height / 2,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "petalButtonCorner",
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "petalButtonScale",
    )
    val shape = RoundedCornerShape(corner)
    val buttonModifier = modifier
        .heightIn(min = height)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }

    when (style) {
        PetalButtonStyle.FILLED -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = colors ?: ButtonDefaults.buttonColors(),
            interactionSource = source,
            content = content,
        )
        PetalButtonStyle.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            colors = colors ?: ButtonDefaults.filledTonalButtonColors(),
            interactionSource = source,
            content = content,
        )
        PetalButtonStyle.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            interactionSource = source,
            content = content,
        )
        PetalButtonStyle.TEXT -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled,
            shape = shape,
            interactionSource = source,
            content = content,
        )
    }
}
