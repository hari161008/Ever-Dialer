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

@Composable
fun Rivo4Theme(
    systemDark: Boolean = isSystemInDarkTheme(),
    prefs: PreferenceManager = koinInject(),
    content: @Composable () -> Unit
) {
    val settingsState by prefs.settingsChanged.collectAsState()

    val themeMode      = prefs.getString(PreferenceManager.KEY_THEME_MODE, "auto") ?: "auto"
    val dynamicColor   = prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true)
    val customPrimary  = prefs.getInt("custom_primary_color", -1)
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

    // Build colour scheme
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    var colorScheme = baseScheme

    // Custom primary when dynamic colours off
    if (!dynamicColor && customPrimary != -1) {
        val primary = Color(customPrimary)
        colorScheme = if (darkTheme) darkColorScheme(primary = primary) else lightColorScheme(primary = primary)
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
        "dark" -> {
            // Legacy amoled support via separate pref
            val amoled = prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)
            if (amoled) colorScheme.copy(
                background = Color.Black, surface = Color.Black,
                surfaceContainer = Color.Black, surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainerHigh = Color(0xFF151515), surfaceContainerHighest = Color(0xFF1A1A1A),
                surfaceContainerLowest = Color.Black, surfaceVariant = Color(0xFF1A1A1A)
            ) else colorScheme
        }
        "auto" -> {
            val amoled = prefs.getBoolean(PreferenceManager.KEY_AMOLED_MODE, false)
            if (darkTheme && amoled) colorScheme.copy(
                background = Color.Black, surface = Color.Black,
                surfaceContainer = Color.Black, surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainerHigh = Color(0xFF151515), surfaceContainerHighest = Color(0xFF1A1A1A),
                surfaceContainerLowest = Color.Black, surfaceVariant = Color(0xFF1A1A1A)
            ) else colorScheme
        }
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
