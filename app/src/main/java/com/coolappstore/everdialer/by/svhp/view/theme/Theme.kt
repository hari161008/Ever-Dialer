package com.coolappstore.everdialer.by.svhp.view.theme

import android.graphics.Typeface
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import org.koin.compose.koinInject
import java.io.File

private val DarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)
private val LightColorScheme = lightColorScheme(
    primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40
)

/**
 * Derive a Material3-coherent color scheme from a single seed/primary color.
 * We blend the user-chosen color into secondary and tertiary to avoid
 * the residual purple from the default scheme.
 */
private fun buildCustomColorScheme(primary: Color, dark: Boolean): androidx.compose.material3.ColorScheme {
    // Harmonise companion roles from the primary
    val argb = primary.toArgb()
    val r = android.graphics.Color.red(argb)
    val g = android.graphics.Color.green(argb)
    val b = android.graphics.Color.blue(argb)

    // Build desaturated / tinted variants
    fun blend(a: Int, b2: Int, ratio: Float) = (a + (b2 - a) * ratio).toInt().coerceIn(0, 255)

    // Secondary: slightly desaturated version of primary
    val secR = blend(r, 128, 0.4f)
    val secG = blend(g, 128, 0.4f)
    val secB = blend(b, 128, 0.4f)
    val secondary = Color(secR, secG, secB)

    // Tertiary: complementary hue shift (rotate ~180° in simplified RGB space)
    val terR = blend(b, r, 0.3f)
    val terG = blend(r, g, 0.3f)
    val terB = blend(g, b, 0.3f)
    val tertiary = Color(terR, terG, terB)

    return if (dark) {
        // Dark container colours: lighter/more vibrant
        val primaryContainer = Color(
            blend(r, 255, 0.55f),
            blend(g, 255, 0.55f),
            blend(b, 255, 0.55f)
        )
        val onPrimaryContainer = Color(
            blend(r, 0, 0.7f),
            blend(g, 0, 0.7f),
            blend(b, 0, 0.7f)
        )
        darkColorScheme(
            primary = primaryContainer, // In dark mode primary is the "light" version
            onPrimary = onPrimaryContainer,
            primaryContainer = Color(blend(r, 40, 0.7f), blend(g, 40, 0.7f), blend(b, 40, 0.7f)),
            onPrimaryContainer = primaryContainer,
            secondary = Color(blend(secR, 200, 0.5f), blend(secG, 200, 0.5f), blend(secB, 200, 0.5f)),
            tertiary = Color(blend(terR, 200, 0.5f), blend(terG, 200, 0.5f), blend(terB, 200, 0.5f))
        ).copy(
            background           = Color(0xFF1C1B1F),
            surface              = Color(0xFF1C1B1F),
            surfaceVariant       = Color(0xFF49454F),
            surfaceContainer     = Color(0xFF211F26),
            surfaceContainerLow  = Color(0xFF1D1B20),
            surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerHighest = Color(0xFF36343B),
            surfaceContainerLowest  = Color(0xFF0F0D13)
        )
    } else {
        val primaryContainer = Color(
            blend(r, 255, 0.75f),
            blend(g, 255, 0.75f),
            blend(b, 255, 0.75f)
        )
        val onPrimaryContainer = Color(
            blend(r, 0, 0.7f),
            blend(g, 0, 0.7f),
            blend(b, 0, 0.7f)
        )
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            tertiary = tertiary
        ).copy(
            background           = Color(0xFFFFFBFE),
            surface              = Color(0xFFFFFBFE),
            surfaceVariant       = Color(0xFFE7E0EC),
            surfaceContainer     = Color(0xFFF3EDF7),
            surfaceContainerLow  = Color(0xFFF7F2FA),
            surfaceContainerHigh = Color(0xFFECE6F0),
            surfaceContainerHighest = Color(0xFFE6E0E9),
            surfaceContainerLowest  = Color(0xFFFFFFFF)
        )
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
    val customPrimaryInt = prefs.getInt("custom_primary_color", 0)
    val customFontPath = prefs.getString(PreferenceManager.KEY_CUSTOM_FONT_PATH, null)
    val fontSizeScale  = prefs.getFloat(PreferenceManager.KEY_CUSTOM_FONT_SIZE, 1.0f)

    // Resolve dark flag from theme mode
    val darkTheme = when (themeMode) {
        "light", "white"  -> false
        "dark",  "black"  -> true
        "auto_bw"         -> systemDark
        else              -> systemDark   // "auto"
    }

    val context = LocalContext.current

    // Default primary when none set
    val defaultPrimary = Color(0xFF6750A4)

    // Build colour scheme
    var colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> {
            val primary = if (customPrimaryInt != 0) Color(customPrimaryInt.toLong() and 0xFFFFFFFFL)
                          else defaultPrimary
            buildCustomColorScheme(primary, darkTheme)
        }
    }

    // Apply pure-white / pure-black overrides
    colorScheme = when (themeMode) {
        "black" -> colorScheme.copy(
            background = Color.Black, surface = Color.Black,
            surfaceContainer = Color.Black, surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainerHigh = Color(0xFF151515), surfaceContainerHighest = Color(0xFF1A1A1A),
            surfaceContainerLowest = Color.Black, surfaceVariant = Color(0xFF1A1A1A)
        )
        "white" -> colorScheme.copy(
            background = Color.White, surface = Color.White,
            surfaceContainer = Color(0xFFF8F8F8), surfaceContainerLow = Color(0xFFF4F4F4),
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
            surfaceContainer = Color(0xFFF8F8F8), surfaceContainerLow = Color(0xFFF4F4F4),
            surfaceContainerHigh = Color(0xFFEEEEEE), surfaceContainerHighest = Color(0xFFE8E8E8),
            surfaceContainerLowest = Color.White, surfaceVariant = Color(0xFFF0F0F0)
        )
        else -> colorScheme
    }

    // Custom font
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
