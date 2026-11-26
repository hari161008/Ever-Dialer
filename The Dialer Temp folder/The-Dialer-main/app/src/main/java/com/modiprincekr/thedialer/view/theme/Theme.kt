package com.modiprincekr.thedialer.view.theme

import android.app.Activity
import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.modiprincekr.thedialer.controller.util.AppLanguageManager
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.components.LocalCardCornerRadius
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.roundToInt

val LocalMaterialThemeStyle = androidx.compose.runtime.compositionLocalOf { PreferenceManager.MATERIAL_THEME_YOU }

@Composable
fun isMaterialExpressive(): Boolean {
    return LocalMaterialThemeStyle.current == PreferenceManager.MATERIAL_THEME_EXPRESSIVE
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)
private val LightColorScheme = lightColorScheme(
    primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40
)

val NothingDarkMonochromeColorScheme = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF282828),
    surfaceVariant = Color(0xFF1E1E1E),
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFD4D4D4),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF222222),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF9E9E9E),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF2C2C2C),
    onTertiaryContainer = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFA0A0A0),
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF262626),
    surfaceTint = Color(0xFFFFFFFF),
    inverseSurface = Color(0xFFFFFFFF),
    inverseOnSurface = Color(0xFF000000),
    inversePrimary = Color(0xFF000000),
    scrim = Color(0xFF000000),
    error = Color(0xFFE53935),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFF3B1515),
    onErrorContainer = Color(0xFFFFDAD6)
)

val NothingLightMonochromeColorScheme = lightColorScheme(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFF3F3F3),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFDFDFDF),
    surfaceVariant = Color(0xFFE8E8E8),
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF424242),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEDED),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFF757575),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0E0E0),
    onTertiaryContainer = Color(0xFF000000),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF5E5E5E),
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE0E0E0),
    surfaceTint = Color(0xFF000000),
    inverseSurface = Color(0xFF000000),
    inverseOnSurface = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFFFFFFF),
    scrim = Color(0xFF000000),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private fun buildCustomColorScheme(primary: Color, dark: Boolean): androidx.compose.material3.ColorScheme {
    val argb = primary.toArgb()
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(argb, hsl)
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.25f, 0.95f)

    fun hslColor(h: Float, s: Float, l: Float): Color {
        val clampedH = (h % 360f + 360f) % 360f
        val clampedS = s.coerceIn(0f, 1f)
        val clampedL = l.coerceIn(0f, 1f)
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(clampedH, clampedS, clampedL)))
    }

    val neutralHue = hue
    val secHue = hue
    val tertHue = (hue + 50f) % 360f

    val secSat = (sat * 0.45f).coerceIn(0.18f, 0.50f)
    val tertSat = (sat * 0.55f).coerceIn(0.20f, 0.60f)

    val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(argb) > 0.45
    val onPrimaryColor = if (isBright) Color(0xFF1C1B1F) else Color.White

    return if (dark) {
        val neutralSat = (sat * 0.28f).coerceIn(0.12f, 0.28f)
        val variantSat = (sat * 0.35f).coerceIn(0.16f, 0.35f)
        val containerSat = (sat * 0.35f).coerceIn(0.18f, 0.42f)

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimaryColor,
            primaryContainer = hslColor(hue, (sat * 0.80f).coerceIn(0.35f, 0.85f), 0.30f),
            onPrimaryContainer = hslColor(hue, (sat * 0.80f).coerceIn(0.35f, 0.85f), 0.90f),
            inversePrimary = hslColor(hue, sat, 0.80f),

            secondary = hslColor(secHue, secSat, 0.80f),
            onSecondary = hslColor(secHue, secSat, 0.20f),
            secondaryContainer = hslColor(secHue, secSat, 0.28f),
            onSecondaryContainer = hslColor(secHue, secSat, 0.90f),

            tertiary = hslColor(tertHue, tertSat, 0.80f),
            onTertiary = hslColor(tertHue, tertSat, 0.20f),
            tertiaryContainer = hslColor(tertHue, tertSat, 0.28f),
            onTertiaryContainer = hslColor(tertHue, tertSat, 0.90f),

            background = hslColor(neutralHue, neutralSat, 0.06f),
            onBackground = hslColor(neutralHue, (neutralSat * 0.4f).coerceIn(0.04f, 0.12f), 0.90f),
            surface = hslColor(neutralHue, neutralSat, 0.06f),
            onSurface = hslColor(neutralHue, (neutralSat * 0.4f).coerceIn(0.04f, 0.12f), 0.90f),
            surfaceVariant = hslColor(neutralHue, variantSat, 0.22f),
            onSurfaceVariant = hslColor(neutralHue, variantSat, 0.80f),

            surfaceContainerLowest = hslColor(neutralHue, neutralSat, 0.04f),
            surfaceContainerLow = hslColor(neutralHue, containerSat, 0.10f),
            surfaceContainer = hslColor(neutralHue, containerSat, 0.14f),
            surfaceContainerHigh = hslColor(neutralHue, containerSat, 0.18f),
            surfaceContainerHighest = hslColor(neutralHue, containerSat, 0.22f),

            outline = hslColor(neutralHue, variantSat, 0.58f),
            outlineVariant = hslColor(neutralHue, variantSat, 0.28f),
            inverseSurface = hslColor(neutralHue, (neutralSat * 0.4f).coerceIn(0.04f, 0.12f), 0.90f),
            inverseOnSurface = hslColor(neutralHue, neutralSat, 0.15f)
        )
    } else {
        val neutralSat = (sat * 0.30f).coerceIn(0.12f, 0.30f)
        val variantSat = (sat * 0.38f).coerceIn(0.18f, 0.40f)
        val containerSat = (sat * 0.38f).coerceIn(0.18f, 0.45f)

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimaryColor,
            primaryContainer = hslColor(hue, (sat * 0.75f).coerceIn(0.30f, 0.85f), 0.90f),
            onPrimaryContainer = hslColor(hue, sat, 0.10f),
            inversePrimary = hslColor(hue, sat, 0.40f),

            secondary = hslColor(secHue, secSat, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hslColor(secHue, secSat, 0.90f),
            onSecondaryContainer = hslColor(secHue, secSat, 0.10f),

            tertiary = hslColor(tertHue, tertSat, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hslColor(tertHue, tertSat, 0.90f),
            onTertiaryContainer = hslColor(tertHue, tertSat, 0.10f),

            background = hslColor(neutralHue, neutralSat, 0.98f),
            onBackground = hslColor(neutralHue, (neutralSat * 0.4f).coerceIn(0.04f, 0.12f), 0.10f),
            surface = hslColor(neutralHue, neutralSat, 0.98f),
            onSurface = hslColor(neutralHue, (neutralSat * 0.4f).coerceIn(0.04f, 0.12f), 0.10f),
            surfaceVariant = hslColor(neutralHue, variantSat, 0.88f),
            onSurfaceVariant = hslColor(neutralHue, variantSat, 0.30f),

            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslColor(neutralHue, containerSat, 0.95f),
            surfaceContainer = hslColor(neutralHue, containerSat, 0.92f),
            surfaceContainerHigh = hslColor(neutralHue, containerSat, 0.88f),
            surfaceContainerHighest = hslColor(neutralHue, containerSat, 0.84f),

            outline = hslColor(neutralHue, variantSat, 0.50f),
            outlineVariant = hslColor(neutralHue, variantSat, 0.80f),
            inverseSurface = hslColor(neutralHue, (neutralSat * 0.4f).coerceIn(0.04f, 0.12f), 0.20f),
            inverseOnSurface = hslColor(neutralHue, (neutralSat * 0.4f).coerceIn(0.04f, 0.12f), 0.95f)
        )
    }
}

private fun applySaturatedContainers(
    scheme: androidx.compose.material3.ColorScheme,
    seedColor: Color,
    darkTheme: Boolean,
    themeMode: String,
    saturationScale: Float = 1.0f,
    isDynamic: Boolean = true,
    saturatedTarget: String = PreferenceManager.SATURATED_TARGET_CONTAINER
): androidx.compose.material3.ColorScheme {
    if (themeMode == "auto_bw") {
        return if (darkTheme) NothingDarkMonochromeColorScheme else NothingLightMonochromeColorScheme
    }

    val argb = seedColor.toArgb()
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(argb, hsl)
    val hue = hsl[0]

    val pcArgb = scheme.primaryContainer.toArgb()
    val pcHsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(pcArgb, pcHsl)

    val containerHue = if (isDynamic) {
        if (pcHsl[1] > 0.15f) pcHsl[0] else hue
    } else {
        hue
    }
    val containerSat = maxOf(hsl[1], pcHsl[1]).coerceIn(0.60f, 0.95f)

    fun hslColor(h: Float, s: Float, l: Float): Color {
        val clampedH = (h % 360f + 360f) % 360f
        val clampedS = s.coerceIn(0f, 1f)
        val clampedL = l.coerceIn(0f, 1f)
        return Color(androidx.core.graphics.ColorUtils.HSLToColor(floatArrayOf(clampedH, clampedS, clampedL)))
    }

    val scale = saturationScale.coerceIn(0.20f, 2.0f)
    val boost = if (scale > 1.0f) (scale - 1.0f) else 0f
    val baseEffectiveSat = if (containerHue in 30f..85f) {
        containerSat.coerceIn(0.50f, 0.75f)
    } else {
        containerSat.coerceIn(0.60f, 0.90f)
    }
    val effectiveSat = (baseEffectiveSat * minOf(scale, 1.0f)).coerceIn(0.10f, 1.0f)

    val isBright = androidx.core.graphics.ColorUtils.calculateLuminance(argb) > 0.45
    val onSeedColor = if (isBright) Color(0xFF1C1B1F) else Color.White

    if (saturatedTarget == PreferenceManager.SATURATED_TARGET_BACKGROUND) {
        return if (darkTheme) {
            val dynamicBgDark = hslColor(containerHue, (effectiveSat * (0.85f + boost * 0.15f)).coerceIn(0.20f, 1.0f), (0.13f + boost * 0.05f).coerceIn(0.08f, 0.22f))
            val dynamicSurfaceDark = hslColor(containerHue, (effectiveSat * (0.85f + boost * 0.15f)).coerceIn(0.20f, 1.0f), (0.15f + boost * 0.06f).coerceIn(0.10f, 0.25f))
            val brightWhite = Color(0xFFFFFFFF)
            val visibleGreyText = Color(0xFFD4D4D8)
            val visibleOutline = Color(0xFF9E9E9E)
            val vibrantPrimary = if (isDynamic) hslColor(containerHue, (0.90f * minOf(scale, 1.0f) + boost * 0.10f).coerceIn(0.50f, 1.0f), (0.58f + boost * 0.08f).coerceIn(0.50f, 0.70f)) else seedColor
            val vibrantOnPrimary = if (isDynamic) Color(0xFF1C1B1F) else onSeedColor
            val isAmoled = themeMode == "black" || themeMode == "auto_bw"
            scheme.copy(
                primary = vibrantPrimary,
                onPrimary = vibrantOnPrimary,
                primaryContainer = if (isAmoled) Color(0xFF141414) else Color(0xFF1E1E1E),
                onPrimaryContainer = Color.White,
                background = dynamicBgDark,
                surface = dynamicSurfaceDark,
                onBackground = brightWhite,
                onSurface = brightWhite,
                onSurfaceVariant = visibleGreyText,
                outline = visibleOutline,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = if (isAmoled) Color.Black else Color(0xFF0C0C0C),
                surfaceContainer = if (isAmoled) Color(0xFF080808) else Color(0xFF121212),
                surfaceContainerHigh = if (isAmoled) Color(0xFF121212) else Color(0xFF1A1A1A),
                surfaceContainerHighest = if (isAmoled) Color(0xFF1A1A1A) else Color(0xFF222222),
                surfaceVariant = if (isAmoled) Color(0xFF121212) else Color(0xFF1A1A1A)
            )
        } else {
            val dynamicBgLight = hslColor(containerHue, (effectiveSat * (0.65f + boost * 0.35f)).coerceIn(0.25f, 0.90f), (0.86f - boost * 0.08f).coerceIn(0.74f, 0.93f))
            val dynamicSurfaceLight = hslColor(containerHue, (effectiveSat * (0.65f + boost * 0.35f)).coerceIn(0.25f, 0.90f), (0.88f - boost * 0.08f).coerceIn(0.76f, 0.95f))
            val crispDark = Color(0xFF111827)
            val visibleDarkGreyText = Color(0xFF4B5563)
            val visibleLightOutline = Color(0xFF757575)
            val vibrantPrimaryLight = if (isDynamic) hslColor(containerHue, (0.90f * minOf(scale, 1.0f) + boost * 0.10f).coerceIn(0.50f, 1.0f), (0.42f - boost * 0.04f).coerceIn(0.35f, 0.50f)) else seedColor
            val vibrantOnPrimaryLight = if (isDynamic) Color.White else onSeedColor
            val isPureWhite = themeMode == "white"
            scheme.copy(
                primary = vibrantPrimaryLight,
                onPrimary = vibrantOnPrimaryLight,
                primaryContainer = if (isPureWhite) Color(0xFFF2F2F4) else Color.White,
                onPrimaryContainer = if (isPureWhite) Color(0xFF1A1A1C) else crispDark,
                background = if (isPureWhite) Color(0xFFFFFFFF) else dynamicBgLight,
                surface = if (isPureWhite) Color(0xFFFFFFFF) else dynamicSurfaceLight,
                onBackground = crispDark,
                onSurface = crispDark,
                onSurfaceVariant = visibleDarkGreyText,
                outline = visibleLightOutline,
                outlineVariant = if (isPureWhite) Color(0xFFE0E0E4) else scheme.outlineVariant,
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = if (isPureWhite) Color(0xFFF4F4F6) else Color.White,
                surfaceContainer = if (isPureWhite) Color(0xFFEEEEF1) else Color(0xFFFBFBFB),
                surfaceContainerHigh = if (isPureWhite) Color(0xFFE6E6EA) else Color(0xFFF2F2F2),
                surfaceContainerHighest = if (isPureWhite) Color(0xFFDFDFE3) else Color(0xFFEBEBEB),
                surfaceVariant = if (isPureWhite) Color(0xFFEEEEEE) else Color(0xFFEBEBEB)
            )
        }
    }

    return if (darkTheme) {
        val low = hslColor(containerHue, effectiveSat, (0.12f + boost * 0.06f).coerceIn(0.08f, 0.25f))
        val normal = hslColor(containerHue, effectiveSat, (0.15f + boost * 0.07f).coerceIn(0.10f, 0.30f))
        val high = hslColor(containerHue, effectiveSat, (0.19f + boost * 0.08f).coerceIn(0.12f, 0.35f))
        val highest = hslColor(containerHue, effectiveSat, (0.23f + boost * 0.09f).coerceIn(0.15f, 0.40f))
        val variant = hslColor(containerHue, effectiveSat, (0.26f + boost * 0.09f).coerceIn(0.18f, 0.42f))
        val brightWhite = Color(0xFFFFFFFF)
        val visibleGreyText = Color(0xFFD4D4D8)
        val visibleOutline = Color(0xFF9E9E9E)
        val vibrantPrimary = if (isDynamic) hslColor(containerHue, (0.90f * minOf(scale, 1.0f) + boost * 0.10f).coerceIn(0.50f, 1.0f), (0.58f + boost * 0.08f).coerceIn(0.50f, 0.70f)) else seedColor
        val vibrantOnPrimary = if (isDynamic) Color(0xFF1C1B1F) else onSeedColor
        val vibrantPrimaryContainer = hslColor(containerHue, (0.85f * minOf(scale, 1.0f) + boost * 0.15f).coerceIn(0.50f, 1.0f), (0.32f + boost * 0.10f).coerceIn(0.25f, 0.48f))
        val vibrantOnPrimaryContainer = Color.White
        when (themeMode) {
            "black" -> scheme.copy(
                primary = vibrantPrimary,
                onPrimary = vibrantOnPrimary,
                primaryContainer = vibrantPrimaryContainer,
                onPrimaryContainer = vibrantOnPrimaryContainer,
                background = Color.Black,
                surface = Color.Black,
                onBackground = brightWhite,
                onSurface = brightWhite,
                onSurfaceVariant = visibleGreyText,
                outline = visibleOutline,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = low,
                surfaceContainer = normal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                surfaceVariant = variant
            )
            else -> scheme.copy(
                primary = vibrantPrimary,
                onPrimary = vibrantOnPrimary,
                primaryContainer = vibrantPrimaryContainer,
                onPrimaryContainer = vibrantOnPrimaryContainer,
                background = hslColor(containerHue, (effectiveSat * (0.35f + boost * 0.35f)).coerceIn(0.10f, 0.70f), (0.05f + boost * 0.03f).coerceIn(0.03f, 0.12f)),
                surface = hslColor(containerHue, (effectiveSat * (0.35f + boost * 0.35f)).coerceIn(0.10f, 0.70f), (0.05f + boost * 0.03f).coerceIn(0.03f, 0.12f)),
                onBackground = brightWhite,
                onSurface = brightWhite,
                onSurfaceVariant = visibleGreyText,
                outline = visibleOutline,
                surfaceContainerLowest = hslColor(containerHue, effectiveSat, (0.07f + boost * 0.04f).coerceIn(0.05f, 0.16f)),
                surfaceContainerLow = low,
                surfaceContainer = normal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                surfaceVariant = variant
            )
        }
    } else {
        val lowest = hslColor(containerHue, (effectiveSat * (0.4f + boost * 0.40f)).coerceIn(0.15f, 0.80f), (0.96f - boost * 0.08f).coerceIn(0.85f, 0.98f))
        val low = hslColor(containerHue, effectiveSat, (0.90f - boost * 0.10f).coerceIn(0.75f, 0.95f))
        val normal = hslColor(containerHue, effectiveSat, (0.86f - boost * 0.12f).coerceIn(0.70f, 0.92f))
        val high = hslColor(containerHue, effectiveSat, (0.82f - boost * 0.14f).coerceIn(0.65f, 0.88f))
        val highest = hslColor(containerHue, effectiveSat, (0.78f - boost * 0.15f).coerceIn(0.60f, 0.85f))
        val variant = hslColor(containerHue, effectiveSat, (0.80f - boost * 0.14f).coerceIn(0.62f, 0.86f))
        val crispDark = Color(0xFF111827)
        val visibleDarkGreyText = Color(0xFF4B5563)
        val visibleLightOutline = Color(0xFF757575)
        val vibrantPrimaryLight = if (isDynamic) hslColor(containerHue, (0.90f * minOf(scale, 1.0f) + boost * 0.10f).coerceIn(0.50f, 1.0f), (0.42f - boost * 0.04f).coerceIn(0.35f, 0.50f)) else seedColor
        val vibrantOnPrimaryLight = if (isDynamic) Color.White else onSeedColor
        val vibrantPrimaryContainerLight = hslColor(containerHue, (0.75f * minOf(scale, 1.0f) + boost * 0.25f).coerceIn(0.50f, 1.0f), (0.88f - boost * 0.12f).coerceIn(0.72f, 0.94f))
        val vibrantOnPrimaryContainerLight = Color(0xFF111827)
        when (themeMode) {
            "white" -> scheme.copy(
                primary = vibrantPrimaryLight,
                onPrimary = vibrantOnPrimaryLight,
                primaryContainer = Color(0xFFF2F2F4),
                onPrimaryContainer = Color(0xFF1A1A1C),
                background = Color(0xFFFFFFFF),
                surface = Color(0xFFFFFFFF),
                onBackground = crispDark,
                onSurface = crispDark,
                onSurfaceVariant = visibleDarkGreyText,
                outline = visibleLightOutline,
                outlineVariant = Color(0xFFE0E0E4),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = Color(0xFFF4F4F6),
                surfaceContainer = Color(0xFFEEEEF1),
                surfaceContainerHigh = Color(0xFFE6E6EA),
                surfaceContainerHighest = Color(0xFFDFDFE3),
                surfaceVariant = Color(0xFFF0F0F0)
            )
            else -> scheme.copy(
                primary = vibrantPrimaryLight,
                onPrimary = vibrantOnPrimaryLight,
                primaryContainer = vibrantPrimaryContainerLight,
                onPrimaryContainer = vibrantOnPrimaryContainerLight,
                background = hslColor(containerHue, (effectiveSat * 0.25f).coerceIn(0.08f, 0.25f), 0.98f),
                surface = hslColor(containerHue, (effectiveSat * 0.25f).coerceIn(0.08f, 0.25f), 0.98f),
                onBackground = crispDark,
                onSurface = crispDark,
                onSurfaceVariant = visibleDarkGreyText,
                outline = visibleLightOutline,
                surfaceContainerLowest = lowest,
                surfaceContainerLow = low,
                surfaceContainer = normal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                surfaceVariant = variant
            )
        }
    }
}


@Composable
fun Rivo4Theme(
    systemDark: Boolean = isSystemInDarkTheme(),
    prefs: PreferenceManager = koinInject(),
    content: @Composable () -> Unit
) {
    val settingsState by prefs.settingsChanged.collectAsState()

    val themeMode      = prefs.getString(PreferenceManager.KEY_THEME_MODE, "auto") ?: "auto"
    val dynamicColor   = prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)
    val saturatedColors = prefs.getBoolean(PreferenceManager.KEY_SATURATED_COLORS, false)
    val customPrimaryInt = prefs.getInt("custom_primary_color", 0)
    val customFontPath = prefs.getString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null)
    val fontSizeScale  = prefs.getFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)
    val fontHeightScale = prefs.getFloat(PreferenceManager.KEY_FONT_HEIGHT_SCALE, 1.0f)
    val fontWidthScale  = prefs.getFloat(PreferenceManager.KEY_FONT_WIDTH_SCALE, 1.0f)
    val fontWeightOverride = prefs.getFloat(PreferenceManager.KEY_FONT_WEIGHT_OVERRIDE, 400f)
    val fontOrientation = prefs.getFloat(PreferenceManager.KEY_FONT_ORIENTATION, 0f)
    val cornerRadius    = prefs.getFloat(PreferenceManager.KEY_CORNER_RADIUS, 28f)

    val darkTheme = when (themeMode) {
        "light", "white"  -> false
        "dark",  "black"  -> true
        "auto_bw"         -> systemDark
        else              -> systemDark
    }

    val saturationScale = prefs.getSaturationLevel(darkTheme)

    val context = LocalContext.current

    val appLanguage = prefs.getAppLanguage()
    val isRtl = remember(appLanguage, settingsState) {
        AppLanguageManager.isRtl(context, appLanguage)
    }
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val defaultPrimary = Color(0xFF6750A4)
    val isAutoBw = themeMode == "auto_bw"

    var colorScheme = when {
        isAutoBw -> if (darkTheme) NothingDarkMonochromeColorScheme else NothingLightMonochromeColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> {
            val primary = if (customPrimaryInt != 0) Color(customPrimaryInt.toLong() and 0xFFFFFFFFL)
                          else defaultPrimary
            buildCustomColorScheme(primary, darkTheme)
        }
    }

    val seedColor = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorScheme.primary
    } else {
        if (customPrimaryInt != 0) Color(customPrimaryInt.toLong() and 0xFFFFFFFFL)
        else defaultPrimary
    }

    val isDynamicActive = !isAutoBw && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val isSaturatedActive = if (isAutoBw) false else prefs.isSaturatedForTheme(darkTheme)
    val saturatedTarget = prefs.getSaturatedTarget()

    if (isSaturatedActive) {
        colorScheme = applySaturatedContainers(
            colorScheme,
            seedColor,
            darkTheme,
            themeMode,
            saturationScale,
            isDynamic = isDynamicActive,
            saturatedTarget = saturatedTarget
        )
    } else {
        colorScheme = when (themeMode) {
            "black" -> colorScheme.copy(
                background = Color.Black, surface = Color.Black,
                surfaceContainer = Color.Black, surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainerHigh = Color(0xFF151515), surfaceContainerHighest = Color(0xFF1A1A1A),
                surfaceContainerLowest = Color.Black, surfaceVariant = Color(0xFF1A1A1A)
            )
            "white" -> colorScheme.copy(
                background = Color(0xFFFFFFFF), surface = Color(0xFFFFFFFF),
                surfaceContainerLowest = Color(0xFFFFFFFF),
                surfaceContainerLow = Color(0xFFF4F4F6),
                surfaceContainer = Color(0xFFEEEEF1),
                surfaceContainerHigh = Color(0xFFE6E6EA),
                surfaceContainerHighest = Color(0xFFDFDFE3),
                outlineVariant = Color(0xFFE0E0E4),
                surfaceVariant = Color(0xFFF0F0F0),
                primaryContainer = Color(0xFFF2F2F4), onPrimaryContainer = Color(0xFF1A1A1C)
            )
            "auto_bw" -> if (darkTheme) NothingDarkMonochromeColorScheme else NothingLightMonochromeColorScheme
            else -> colorScheme
        }
    }

    // ── Sync status bar / nav bar with theme ──────────────────────────────────
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            // Light icons on dark theme, dark icons on light theme
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val customFontFamily: FontFamily = remember(customFontPath, settingsState) {
        AppFontHelper.resolveFontFamily(customFontPath)
    }

    val weightOffset = remember(fontWeightOverride) { (fontWeightOverride - 400f).roundToInt() }
    val skewX = remember(fontOrientation) { -fontOrientation / 60f }

    val typography = remember(customFontFamily, fontSizeScale, fontHeightScale, fontWidthScale, weightOffset, skewX) {
        buildTypography(
            fontFamily = customFontFamily,
            scale = fontSizeScale,
            heightScale = fontHeightScale,
            widthScale = fontWidthScale,
            weightOffset = weightOffset,
            skewX = skewX
        )
    }

    val materialThemeStyle = prefs.getMaterialTheme()

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalCardCornerRadius provides cornerRadius.dp,
        LocalMaterialThemeStyle provides materialThemeStyle,
        com.modiprincekr.thedialer.controller.util.LocalAppLanguage provides appLanguage
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = typography
        ) {
            ProvideScaledDensity(prefs = prefs, content = content)
        }
    }
}

@Composable
fun ProvideScaledDensity(
    prefs: PreferenceManager = koinInject(),
    content: @Composable () -> Unit
) {
    val settingsState by prefs.settingsChanged.collectAsState()
    val displayScale = prefs.getFloat(PreferenceManager.KEY_DISPLAY_SCALE, 1.0f)
    val fontSizeScale = prefs.getFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)
    val baseDensity = androidx.compose.ui.platform.LocalDensity.current
    val scaledDensity = remember(baseDensity.density, baseDensity.fontScale, displayScale, fontSizeScale, settingsState) {
        androidx.compose.ui.unit.Density(
            density = baseDensity.density * displayScale,
            fontScale = baseDensity.fontScale * fontSizeScale
        )
    }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides scaledDensity,
        content = content
    )
}

fun getActiveThemeColor(context: android.content.Context, prefs: PreferenceManager): Int {
    val dynamicColor = prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)
    val customPrimaryInt = prefs.getInt("custom_primary_color", 0)
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            return dynamicLightColorScheme(context).primary.toArgb()
        } catch (_: Throwable) {}
    }
    if (customPrimaryInt != 0) {
        return customPrimaryInt
    }
    return 0xFF6750A4.toInt()
}
