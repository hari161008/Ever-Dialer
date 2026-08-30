package com.coolappstore.everdialer.by.svhp.view.theme

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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import org.koin.compose.koinInject
import java.io.File

private val DarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)
private val LightColorScheme = lightColorScheme(
    primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40
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

    val baseEffectiveSat = if (containerHue in 30f..85f) {
        containerSat.coerceIn(0.50f, 0.75f)
    } else {
        containerSat.coerceIn(0.60f, 0.90f)
    }
    val effectiveSat = (baseEffectiveSat * saturationScale).coerceIn(0.10f, 1.0f)

    return if (darkTheme) {
        val low = hslColor(containerHue, effectiveSat, 0.12f)
        val normal = hslColor(containerHue, effectiveSat, 0.15f)
        val high = hslColor(containerHue, effectiveSat, 0.19f)
        val highest = hslColor(containerHue, effectiveSat, 0.23f)
        val variant = hslColor(containerHue, effectiveSat, 0.26f)
        val brightWhite = Color(0xFFFFFFFF)
        val visibleGreyText = Color(0xFFD4D4D8)
        val visibleOutline = Color(0xFF9E9E9E)
        val vibrantPrimary = hslColor(containerHue, 0.90f, 0.58f)
        val isPrimaryBright = androidx.core.graphics.ColorUtils.calculateLuminance(vibrantPrimary.toArgb()) > 0.40
        val vibrantOnPrimary = if (isPrimaryBright) Color(0xFF1C1B1F) else Color.White
        val vibrantPrimaryContainer = hslColor(containerHue, 0.85f, 0.32f)
        val vibrantOnPrimaryContainer = if (androidx.core.graphics.ColorUtils.calculateLuminance(vibrantPrimaryContainer.toArgb()) > 0.40) Color(0xFF1C1B1F) else Color.White
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
                background = hslColor(containerHue, (effectiveSat * 0.35f).coerceIn(0.10f, 0.30f), 0.05f),
                surface = hslColor(containerHue, (effectiveSat * 0.35f).coerceIn(0.10f, 0.30f), 0.05f),
                onBackground = brightWhite,
                onSurface = brightWhite,
                onSurfaceVariant = visibleGreyText,
                outline = visibleOutline,
                surfaceContainerLowest = hslColor(containerHue, effectiveSat, 0.07f),
                surfaceContainerLow = low,
                surfaceContainer = normal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                surfaceVariant = variant
            )
        }
    } else {
        val lowest = hslColor(containerHue, (effectiveSat * 0.4f).coerceIn(0.15f, 0.40f), 0.96f)
        val low = hslColor(containerHue, effectiveSat, 0.90f)
        val normal = hslColor(containerHue, effectiveSat, 0.86f)
        val high = hslColor(containerHue, effectiveSat, 0.82f)
        val highest = hslColor(containerHue, effectiveSat, 0.78f)
        val variant = hslColor(containerHue, effectiveSat, 0.80f)
        val crispDark = Color(0xFF111827)
        val visibleDarkGreyText = Color(0xFF4B5563)
        val visibleLightOutline = Color(0xFF757575)
        val vibrantPrimaryLight = hslColor(containerHue, 0.90f, 0.42f)
        val vibrantOnPrimaryLight = Color.White
        val vibrantPrimaryContainerLight = hslColor(containerHue, 0.75f, 0.88f)
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
    val saturationScale = prefs.getFloat(PreferenceManager.KEY_SATURATION_LEVEL, 1.0f)

    val darkTheme = when (themeMode) {
        "light", "white"  -> false
        "dark",  "black"  -> true
        "auto_bw"         -> systemDark
        else              -> systemDark
    }

    val context = LocalContext.current

    val defaultPrimary = Color(0xFF6750A4)

    var colorScheme = when {
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

    val isSaturatedActive = prefs.isSaturatedForTheme(darkTheme)

    if (isSaturatedActive) {
        colorScheme = applySaturatedContainers(colorScheme, seedColor, darkTheme, themeMode, saturationScale)
    } else {
        colorScheme = when (themeMode) {
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
            "auto_bw" -> if (darkTheme) colorScheme.copy(
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
        if (customFontPath != null) {
            val file = File(customFontPath)
            if (file.exists()) {
                try { FontFamily(Typeface.createFromFile(file)) }
                catch (e: Exception) { FontFamily.Default }
            } else FontFamily.Default
        } else FontFamily.Default
    }

    val typography = remember(customFontFamily, fontSizeScale) {
        buildTypography(customFontFamily, fontSizeScale)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = typography,
        content     = content
    )
}
