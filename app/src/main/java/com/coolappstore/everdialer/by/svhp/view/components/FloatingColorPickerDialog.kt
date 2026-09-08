package com.coolappstore.everdialer.by.svhp.view.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FloatingColorPickerDialog(
    title: String = "Color Picker",
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val hsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var sat by remember { mutableFloatStateOf(hsv[1].coerceIn(0f, 1f)) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceIn(0f, 1f)) }

    val currentColor = remember(hue, sat, value) {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))
        Color(rgb)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Live Color Preview Pill
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(80.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(currentColor)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 2D Saturation-Value Panel with Pointer Reticle
                Text(
                    text = "Shade & Brightness",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                sat = (offset.x / size.width).coerceIn(0f, 1f)
                                value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    sat = (offset.x / size.width).coerceIn(0f, 1f)
                                    value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                    value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. Horizontal gradient: White to pure Hue color
                        val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.White, pureHueColor)
                            )
                        )
                        // 2. Vertical gradient: Transparent to Black
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black)
                            )
                        )

                        // 3. Draw Pointer Reticle
                        val pointerX = (sat * size.width).coerceIn(0f, size.width)
                        val pointerY = ((1f - value) * size.height).coerceIn(0f, size.height)
                        val pointerCenter = Offset(pointerX, pointerY)

                        // Outer ring (black) for contrast
                        drawCircle(
                            color = Color.Black.copy(alpha = 0.6f),
                            radius = 13.dp.toPx(),
                            center = pointerCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        // Inner ring (white)
                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = pointerCenter,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                        // Center dot (current color)
                        drawCircle(
                            color = currentColor,
                            radius = 6.dp.toPx(),
                            center = pointerCenter
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hue Slider with pointer
                Text(
                    text = "Hue Spectrum",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val rainbow = listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red
                        )
                        drawRect(brush = Brush.horizontalGradient(rainbow))

                        // Pointer Thumb on Hue Bar
                        val thumbX = ((hue / 360f) * size.width).coerceIn(12.dp.toPx(), size.width - 12.dp.toPx())
                        val thumbCenter = Offset(thumbX, size.height / 2)

                        drawCircle(
                            color = Color.Black.copy(alpha = 0.5f),
                            radius = 14.dp.toPx(),
                            center = thumbCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = thumbCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        val currentHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        drawCircle(
                            color = currentHueColor,
                            radius = 8.dp.toPx(),
                            center = thumbCenter
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hex Code Display
                val hexStr = remember(currentColor) {
                    String.format("#%06X", 0xFFFFFF and currentColor.toArgb())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hex Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = hexStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onColorSelected(currentColor) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Select Color")
                    }
                }
            }
        }
    }
}
