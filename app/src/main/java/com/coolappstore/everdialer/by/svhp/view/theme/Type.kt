package com.coolappstore.everdialer.by.svhp.view.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp

val Typography = buildTypography(FontFamily.Default, 1.0f)

fun buildTypography(
    fontFamily: FontFamily,
    scale: Float = 1.0f,
    heightScale: Float = 1.0f,
    widthScale: Float = 1.0f,
    weightOffset: Int = 0,
    skewX: Float = 0.0f
): Typography {
    val effectiveScaleX = if (heightScale > 0.01f) widthScale / heightScale else widthScale
    val transform = if (effectiveScaleX != 1.0f || skewX != 0.0f) {
        TextGeometricTransform(scaleX = effectiveScaleX, skewX = skewX)
    } else {
        null
    }

    fun style(baseWeight: FontWeight, baseSize: Int, baseLine: Int, baseSpacing: Double): TextStyle {
        val effectiveWeight = FontWeight((baseWeight.weight + weightOffset).coerceIn(100, 900))
        val effectiveFontSize = (baseSize * scale * heightScale).sp
        val effectiveLineHeight = (baseLine * scale * heightScale).sp
        return TextStyle(
            fontFamily = fontFamily,
            fontWeight = effectiveWeight,
            fontSize = effectiveFontSize,
            lineHeight = effectiveLineHeight,
            letterSpacing = baseSpacing.sp,
            textGeometricTransform = transform
        )
    }

    return Typography(
        displayLarge   = style(FontWeight.Bold, 57, 64, -0.25),
        displayMedium  = style(FontWeight.Bold, 45, 52, 0.0),
        displaySmall   = style(FontWeight.Bold, 36, 44, 0.0),
        headlineLarge  = style(FontWeight.Bold, 32, 40, 0.0),
        headlineMedium = style(FontWeight.Bold, 28, 36, 0.0),
        headlineSmall  = style(FontWeight.SemiBold, 24, 32, 0.0),
        titleLarge     = style(FontWeight.SemiBold, 22, 28, 0.0),
        titleMedium    = style(FontWeight.SemiBold, 16, 24, 0.15),
        titleSmall     = style(FontWeight.Medium, 14, 20, 0.1),
        bodyLarge      = style(FontWeight.Normal, 16, 24, 0.5),
        bodyMedium     = style(FontWeight.Normal, 14, 20, 0.25),
        bodySmall      = style(FontWeight.Normal, 12, 16, 0.4),
        labelLarge     = style(FontWeight.SemiBold, 14, 20, 0.1),
        labelMedium    = style(FontWeight.SemiBold, 12, 16, 0.5),
        labelSmall     = style(FontWeight.SemiBold, 11, 16, 0.5)
    )
}
