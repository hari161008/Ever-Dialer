/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

// ── Static fallback schemes (original green) ──────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = DeepDarkGreen,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = GreenContainerLight,
    secondary = GreenGrey80,
    onSecondary = DarkGreyGreen,
    tertiary = AccentGreen80,
    onTertiary = AccentGreenDark,
    surface = DarkSurface,
    onSurface = OffWhiteText,
    outline = GreyGreenOutline
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = White,
    primaryContainer = GreenContainerLight,
    onPrimaryContainer = VeryDarkForest,
    secondary = GreenGrey40,
    onSecondary = White,
    surface = LightSurface,
    onSurface = NearBlackText,
    outline = GreyGreenOutline
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

    return if (dark) {
        val neutralSat = (sat * 0.28f).coerceIn(0.12f, 0.28f)
        val variantSat = (sat * 0.35f).coerceIn(0.16f, 0.35f)
        val containerSat = (sat * 0.35f).coerceIn(0.18f, 0.42f)

        darkColorScheme(
            primary = hslColor(hue, sat, 0.80f),
            onPrimary = hslColor(hue, sat, 0.20f),
            primaryContainer = hslColor(hue, (sat * 0.80f).coerceIn(0.35f, 0.85f), 0.30f),
            onPrimaryContainer = hslColor(hue, (sat * 0.80f).coerceIn(0.35f, 0.85f), 0.90f),
            inversePrimary = hslColor(hue, sat, 0.40f),

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
            primary = hslColor(hue, sat, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hslColor(hue, (sat * 0.75f).coerceIn(0.30f, 0.85f), 0.90f),
            onPrimaryContainer = hslColor(hue, sat, 0.10f),
            inversePrimary = hslColor(hue, sat, 0.80f),

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
    saturationScale: Float = 1.0f
): androidx.compose.material3.ColorScheme {
    val argb = seedColor.toArgb()
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(argb, hsl)
    val hue = hsl[0]

    val pcArgb = scheme.primaryContainer.toArgb()
    val pcHsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(pcArgb, pcHsl)

    val containerHue = if (pcHsl[1] > 0.15f) pcHsl[0] else hue
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

    return if (darkTheme) {
        val low = hslColor(containerHue, effectiveSat, (0.12f + boost * 0.06f).coerceIn(0.08f, 0.25f))
        val normal = hslColor(containerHue, effectiveSat, (0.15f + boost * 0.07f).coerceIn(0.10f, 0.30f))
        val high = hslColor(containerHue, effectiveSat, (0.19f + boost * 0.08f).coerceIn(0.12f, 0.35f))
        val highest = hslColor(containerHue, effectiveSat, (0.23f + boost * 0.09f).coerceIn(0.15f, 0.40f))
        val variant = hslColor(containerHue, effectiveSat, (0.26f + boost * 0.09f).coerceIn(0.18f, 0.42f))
        val brightWhite = Color(0xFFFFFFFF)
        val visibleGreyText = Color(0xFFD4D4D8)
        val visibleOutline = Color(0xFF9E9E9E)
        val vibrantPrimary = hslColor(containerHue, (0.90f * minOf(scale, 1.0f) + boost * 0.10f).coerceIn(0.50f, 1.0f), (0.58f + boost * 0.08f).coerceIn(0.50f, 0.70f))
        val vibrantOnPrimary = Color(0xFF1C1B1F)
        val vibrantPrimaryContainer = hslColor(containerHue, (0.85f * minOf(scale, 1.0f) + boost * 0.15f).coerceIn(0.50f, 1.0f), (0.32f + boost * 0.10f).coerceIn(0.25f, 0.48f))
        val vibrantOnPrimaryContainer = Color.White
        when (themeMode) {
            "black", "auto_bw" -> scheme.copy(
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
        val vibrantPrimaryLight = hslColor(containerHue, (0.90f * minOf(scale, 1.0f) + boost * 0.10f).coerceIn(0.50f, 1.0f), (0.42f - boost * 0.04f).coerceIn(0.35f, 0.50f))
        val vibrantOnPrimaryLight = Color.White
        val vibrantPrimaryContainerLight = hslColor(containerHue, (0.75f * minOf(scale, 1.0f) + boost * 0.25f).coerceIn(0.50f, 1.0f), (0.88f - boost * 0.12f).coerceIn(0.72f, 0.94f))
        val vibrantOnPrimaryContainerLight = Color(0xFF111827)
        when (themeMode) {
            "white", "auto_bw" -> scheme.copy(
                primary = vibrantPrimaryLight,
                onPrimary = vibrantOnPrimaryLight,
                primaryContainer = vibrantPrimaryContainerLight,
                onPrimaryContainer = vibrantOnPrimaryContainerLight,
                background = Color.White,
                surface = Color.White,
                onBackground = crispDark,
                onSurface = crispDark,
                onSurfaceVariant = visibleDarkGreyText,
                outline = visibleLightOutline,
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = low,
                surfaceContainer = normal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                surfaceVariant = variant
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

private fun isSaturatedForTheme(prefs: SharedPreferences?, isDark: Boolean, themeMode: String): Boolean {
    if (prefs == null || !prefs.getBoolean("saturated_colors", false)) return false
    val raw = prefs.getString("saturated_modes", "light,dark,white,black") ?: "light,dark,white,black"
    val modes = raw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    val effectiveMode = when (themeMode) {
        "white" -> "white"
        "black" -> "black"
        "light" -> "light"
        "dark" -> "dark"
        "auto_bw" -> if (isDark) "black" else "white"
        else -> if (isDark) "dark" else "light"
    }
    return modes.contains(effectiveMode)
}

private fun getSaturationLevel(prefs: SharedPreferences?, isDark: Boolean): Float {
    if (prefs == null) return 1.0f
    val defaultLevel = prefs.getFloat("saturation_level", 1.0f)
    return if (isDark) {
        prefs.getFloat("saturation_level_dark", defaultLevel)
    } else {
        prefs.getFloat("saturation_level_light", defaultLevel)
    }
}

// ── Public theme composable ───────────────────────────────────────────────────

@Composable
fun ShizucallrecorderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    accentArgb: Int? = null,
    isPureWhite: Boolean = false,
    isPureBlack: Boolean = false,
    fontFamily: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val rivoPrefs = remember {
        try { context.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE) }
        catch (_: Exception) { null }
    }

    val rivoThemeMode = rivoPrefs?.getString("theme_mode", "auto") ?: "auto"
    val effectiveThemeMode = when {
        isPureWhite -> "white"
        isPureBlack -> "black"
        else -> rivoThemeMode
    }

    val effectiveDarkTheme = when (effectiveThemeMode) {
        "light", "white" -> false
        "dark", "black" -> true
        "auto_bw" -> darkTheme
        else -> darkTheme
    }

    val rivoDynamicColor = rivoPrefs?.getBoolean("dynamic_colors", true) ?: true
    val effectiveDynamicColor = if (dynamicColor) rivoDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S else false

    val rivoCustomPrimary = rivoPrefs?.getInt("custom_primary_color", 0) ?: 0
    val effectiveCustomPrimary = if (accentArgb != null && accentArgb != 0) accentArgb else rivoCustomPrimary

    val defaultPrimary = Color(0xFF6750A4)

    var colorScheme = when {
        effectiveDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (effectiveDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> {
            val primary = if (effectiveCustomPrimary != 0) Color(effectiveCustomPrimary.toLong() and 0xFFFFFFFFL)
                          else defaultPrimary
            buildCustomColorScheme(primary, effectiveDarkTheme)
        }
    }

    val seedColor = if (effectiveDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorScheme.primary
    } else {
        if (effectiveCustomPrimary != 0) Color(effectiveCustomPrimary.toLong() and 0xFFFFFFFFL)
        else defaultPrimary
    }

    val isSaturated = isSaturatedForTheme(rivoPrefs, effectiveDarkTheme, effectiveThemeMode)
    val saturationScale = getSaturationLevel(rivoPrefs, effectiveDarkTheme)

    if (isSaturated) {
        colorScheme = applySaturatedContainers(colorScheme, seedColor, effectiveDarkTheme, effectiveThemeMode, saturationScale)
    } else {
        colorScheme = when (effectiveThemeMode) {
            "black" -> colorScheme.copy(
                background = Color.Black, surface = Color.Black,
                surfaceContainer = Color.Black, surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainerHigh = Color(0xFF151515), surfaceContainerHighest = Color(0xFF1A1A1A),
                surfaceContainerLowest = Color.Black, surfaceVariant = Color(0xFF1A1A1A)
            )
            "white" -> colorScheme.copy(
                background = Color.White, surface = Color.White,
                surfaceContainer = Color.White, surfaceContainerLow = Color(0xFFF4F4F4),
                surfaceContainerHigh = Color(0xFFEEEEEE), surfaceContainerHighest = Color(0xFFE8E8E8),
                surfaceContainerLowest = Color.White, surfaceVariant = Color(0xFFF0F0F0)
            )
            "auto_bw" -> if (effectiveDarkTheme) colorScheme.copy(
                background = Color.Black, surface = Color.Black,
                surfaceContainer = Color.Black, surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainerHigh = Color(0xFF151515), surfaceContainerHighest = Color(0xFF1A1A1A),
                surfaceContainerLowest = Color.Black, surfaceVariant = Color(0xFF1A1A1A)
            ) else colorScheme.copy(
                background = Color.White, surface = Color.White,
                surfaceContainer = Color.White, surfaceContainerLow = Color(0xFFF4F4F4),
                surfaceContainerHigh = Color(0xFFEEEEEE), surfaceContainerHighest = Color(0xFFE8E8E8),
                surfaceContainerLowest = Color.White, surfaceVariant = Color(0xFFF0F0F0)
            )
            else -> colorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !effectiveDarkTheme
            controller.isAppearanceLightNavigationBars = !effectiveDarkTheme
        }
    }

    val typography = remember(fontFamily) { buildTypography(fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
