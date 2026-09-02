package com.supernova.networkswitch.presentation.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private fun buildCustomColorScheme(primary: Color, dark: Boolean): ColorScheme {
    val argb = primary.toArgb()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    val hue = hsl[0]
    val sat = hsl[1].coerceIn(0.25f, 0.95f)

    fun hslColor(h: Float, s: Float, l: Float): Color {
        val clampedH = (h % 360f + 360f) % 360f
        val clampedS = s.coerceIn(0f, 1f)
        val clampedL = l.coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(floatArrayOf(clampedH, clampedS, clampedL)))
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
            primaryContainer = hslColor(hue, (sat * 0.80f).coerceIn(0.35f, 0.85f), 0.90f),
            onPrimaryContainer = hslColor(hue, (sat * 0.80f).coerceIn(0.35f, 0.85f), 0.10f),
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
            onBackground = hslColor(neutralHue, neutralSat, 0.10f),
            surface = hslColor(neutralHue, neutralSat, 0.98f),
            onSurface = hslColor(neutralHue, neutralSat, 0.10f),
            surfaceVariant = hslColor(neutralHue, variantSat, 0.90f),
            onSurfaceVariant = hslColor(neutralHue, variantSat, 0.30f),

            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslColor(neutralHue, containerSat, 0.96f),
            surfaceContainer = hslColor(neutralHue, containerSat, 0.94f),
            surfaceContainerHigh = hslColor(neutralHue, containerSat, 0.92f),
            surfaceContainerHighest = hslColor(neutralHue, containerSat, 0.90f),

            outline = hslColor(neutralHue, variantSat, 0.50f),
            outlineVariant = hslColor(neutralHue, variantSat, 0.80f),
            inverseSurface = hslColor(neutralHue, neutralSat, 0.20f),
            inverseOnSurface = hslColor(neutralHue, (neutralSat * 0.4f).coerceIn(0.04f, 0.12f), 0.95f)
        )
    }
}

private fun applySaturatedContainers(
    scheme: ColorScheme,
    seedColor: Color,
    dark: Boolean,
    themeMode: String,
    scale: Float = 1.0f
): ColorScheme {
    val argb = seedColor.toArgb()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    val hue = hsl[0]
    val baseSat = hsl[1].coerceIn(0.20f, 0.95f)
    val effectiveSat = (baseSat * scale).coerceIn(0.15f, 1.0f)
    val containerHue = hue

    fun hslColor(h: Float, s: Float, l: Float): Color {
        val clampedH = (h % 360f + 360f) % 360f
        val clampedS = s.coerceIn(0f, 1f)
        val clampedL = l.coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(floatArrayOf(clampedH, clampedS, clampedL)))
    }

    val vibrantPrimaryDark = hslColor(hue, effectiveSat, 0.80f)
    val vibrantOnPrimaryDark = hslColor(hue, effectiveSat, 0.12f)
    val vibrantPrimaryContainerDark = hslColor(hue, effectiveSat, 0.30f)
    val vibrantOnPrimaryContainerDark = hslColor(hue, effectiveSat, 0.90f)

    val vibrantPrimaryLight = hslColor(hue, effectiveSat, 0.40f)
    val vibrantOnPrimaryLight = Color.White
    val vibrantPrimaryContainerLight = hslColor(hue, effectiveSat, 0.88f)
    val vibrantOnPrimaryContainerLight = hslColor(hue, effectiveSat, 0.10f)

    return if (dark) {
        val sat = effectiveSat
        val lowest  = hslColor(containerHue, (sat * 0.55f).coerceIn(0.14f, 0.65f), 0.05f)
        val low     = hslColor(containerHue, (sat * 0.55f).coerceIn(0.16f, 0.65f), 0.10f)
        val normal  = hslColor(containerHue, (sat * 0.58f).coerceIn(0.18f, 0.70f), 0.14f)
        val high    = hslColor(containerHue, (sat * 0.62f).coerceIn(0.20f, 0.75f), 0.18f)
        val highest = hslColor(containerHue, (sat * 0.65f).coerceIn(0.22f, 0.80f), 0.22f)
        val variant = hslColor(containerHue, (sat * 0.40f).coerceIn(0.12f, 0.50f), 0.18f)

        val crispWhite = Color(0xFFF2F2F2)
        val visibleGreyText = Color(0xFFCCCCCC)
        val visibleDarkOutline = Color(0xFF6E6E73)

        when (themeMode) {
            "black", "auto_bw" -> scheme.copy(
                primary = vibrantPrimaryDark,
                onPrimary = vibrantOnPrimaryDark,
                primaryContainer = vibrantPrimaryContainerDark,
                onPrimaryContainer = vibrantOnPrimaryContainerDark,
                background = Color.Black,
                surface = Color.Black,
                onBackground = crispWhite,
                onSurface = crispWhite,
                onSurfaceVariant = visibleGreyText,
                outline = visibleDarkOutline,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = low,
                surfaceContainer = normal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                surfaceVariant = variant
            )
            else -> scheme.copy(
                primary = vibrantPrimaryDark,
                onPrimary = vibrantOnPrimaryDark,
                primaryContainer = vibrantPrimaryContainerDark,
                onPrimaryContainer = vibrantOnPrimaryContainerDark,
                background = hslColor(containerHue, (sat * 0.45f).coerceIn(0.10f, 0.50f), 0.07f),
                surface = hslColor(containerHue, (sat * 0.45f).coerceIn(0.10f, 0.50f), 0.07f),
                onBackground = crispWhite,
                onSurface = crispWhite,
                onSurfaceVariant = visibleGreyText,
                outline = visibleDarkOutline,
                surfaceContainerLowest = lowest,
                surfaceContainerLow = low,
                surfaceContainer = normal,
                surfaceContainerHigh = high,
                surfaceContainerHighest = highest,
                surfaceVariant = variant
            )
        }
    } else {
        val sat = effectiveSat
        val lowest  = Color.White
        val low     = hslColor(containerHue, (sat * 0.65f).coerceIn(0.20f, 0.85f), 0.94f)
        val normal  = hslColor(containerHue, (sat * 0.70f).coerceIn(0.22f, 0.90f), 0.90f)
        val high    = hslColor(containerHue, (sat * 0.75f).coerceIn(0.25f, 0.92f), 0.86f)
        val highest = hslColor(containerHue, (sat * 0.80f).coerceIn(0.28f, 0.95f), 0.82f)
        val variant = hslColor(containerHue, (sat * 0.45f).coerceIn(0.14f, 0.60f), 0.88f)

        val crispDark = Color(0xFF1C1B1F)
        val visibleDarkGreyText = Color(0xFF49454F)
        val visibleLightOutline = Color(0xFF79747E)

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
fun NetworkSwitchTheme(
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("rivo_prefs", Context.MODE_PRIVATE)
    }

    val themeMode = prefs.getString("theme_mode", "auto") ?: "auto"
    val dynamicColor = prefs.getBoolean("dynamic_colors", true)
    val customPrimaryInt = prefs.getInt("custom_primary_color", 0)

    val darkTheme = when (themeMode) {
        "light", "white" -> false
        "dark", "black" -> true
        "auto_bw" -> systemDark
        else -> systemDark
    }

    val isSaturatedActive = when {
        !prefs.getBoolean("saturated_colors", false) -> false
        else -> {
            val raw = prefs.getString("saturated_modes", "light,dark,white,black") ?: "light,dark,white,black"
            val modes = raw.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
            val effectiveMode = when (themeMode) {
                "white" -> "white"
                "black" -> "black"
                "light" -> "light"
                "dark" -> "dark"
                "auto_bw" -> if (darkTheme) "black" else "white"
                else -> if (darkTheme) "dark" else "light"
            }
            modes.contains(effectiveMode)
        }
    }

    val defaultSaturation = prefs.getFloat("saturation_level", 1.0f)
    val saturationScale = if (darkTheme) {
        prefs.getFloat("saturation_level_dark", defaultSaturation)
    } else {
        prefs.getFloat("saturation_level_light", defaultSaturation)
    }

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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
