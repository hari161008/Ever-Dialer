/*
 * ShizuCallRecorder: FOSS Call recording powered through ADB/Shizuku!
 *  Copyright (C) 2026-present kitsumed (Med)
 *  This software is licensed under the GNU General Public License v3 or later, with additional terms as permitted under Section 7.
 *  The full license text is available in the LICENSE file at the root of this project.
 *  This software is distributed WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package com.coolappstore.evercallrecorder.by.svhp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

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

// ── Parametric scheme from user-chosen accent color ───────────────────────────

private fun colorFromHsv(h: Float, s: Float, v: Float) =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))

private fun buildLightScheme(hue: Float) = lightColorScheme(
    primary             = colorFromHsv(hue, 0.70f, 0.42f),
    onPrimary           = White,
    primaryContainer    = colorFromHsv(hue, 0.22f, 0.94f),
    onPrimaryContainer  = colorFromHsv(hue, 0.80f, 0.24f),
    secondary           = colorFromHsv(hue, 0.40f, 0.42f),
    onSecondary         = White,
    secondaryContainer  = colorFromHsv(hue, 0.18f, 0.88f),
    onSecondaryContainer= colorFromHsv(hue, 0.55f, 0.18f),
    tertiary            = colorFromHsv((hue + 40f) % 360f, 0.55f, 0.40f),
    onTertiary          = White,
    tertiaryContainer   = colorFromHsv((hue + 40f) % 360f, 0.20f, 0.90f),
    onTertiaryContainer = colorFromHsv((hue + 40f) % 360f, 0.55f, 0.22f),
    surface             = LightSurface,
    onSurface           = NearBlackText,
    outline             = colorFromHsv(hue, 0.15f, 0.58f)
)

private fun buildDarkScheme(hue: Float) = darkColorScheme(
    primary             = colorFromHsv(hue, 0.24f, 0.83f),
    onPrimary           = colorFromHsv(hue, 0.80f, 0.22f),
    primaryContainer    = colorFromHsv(hue, 0.71f, 0.32f),
    onPrimaryContainer  = colorFromHsv(hue, 0.22f, 0.94f),
    secondary           = colorFromHsv(hue, 0.20f, 0.78f),
    onSecondary         = colorFromHsv(hue, 0.40f, 0.18f),
    secondaryContainer  = colorFromHsv(hue, 0.25f, 0.30f),
    onSecondaryContainer= colorFromHsv(hue, 0.15f, 0.82f),
    tertiary            = colorFromHsv((hue + 40f) % 360f, 0.24f, 0.80f),
    onTertiary          = colorFromHsv((hue + 40f) % 360f, 0.75f, 0.20f),
    tertiaryContainer   = colorFromHsv((hue + 40f) % 360f, 0.60f, 0.28f),
    onTertiaryContainer = colorFromHsv((hue + 40f) % 360f, 0.18f, 0.86f),
    surface             = DarkSurface,
    onSurface           = OffWhiteText,
    outline             = GreyGreenOutline
)

// ── Pure White / Pure Black palettes ─────────────────────────────────────────

private val PureWhiteColorScheme = lightColorScheme(
    primary             = Color(0xFF1A1A1A),
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFFEEEEEE),
    onPrimaryContainer  = Color(0xFF1A1A1A),
    secondary           = Color(0xFF444444),
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFE8E8E8),
    onSecondaryContainer= Color(0xFF111111),
    tertiary            = Color(0xFF666666),
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFEFEFEF),
    onTertiaryContainer = Color(0xFF222222),
    surface             = Color.White,
    surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainerHigh= Color(0xFFEDEDED),
    onSurface           = Color(0xFF1A1A1A),
    outline             = Color(0xFFCCCCCC),
    background          = Color.White,
    onBackground        = Color(0xFF1A1A1A)
)

private val PureBlackColorScheme = darkColorScheme(
    primary             = Color(0xFFE0E0E0),
    onPrimary           = Color.Black,
    primaryContainer    = Color(0xFF1E1E1E),
    onPrimaryContainer  = Color(0xFFE0E0E0),
    secondary           = Color(0xFFB0B0B0),
    onSecondary         = Color.Black,
    secondaryContainer  = Color(0xFF181818),
    onSecondaryContainer= Color(0xFFCCCCCC),
    tertiary            = Color(0xFF909090),
    onTertiary          = Color.Black,
    tertiaryContainer   = Color(0xFF141414),
    onTertiaryContainer = Color(0xFFBBBBBB),
    surface             = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainerHigh= Color(0xFF1A1A1A),
    onSurface           = Color(0xFFE5E5E5),
    outline             = Color(0xFF333333),
    background          = Color.Black,
    onBackground        = Color(0xFFE5E5E5)
)

// ── Parametric scheme from user-chosen accent color, forced onto a pure white/black canvas ────
// Used when the user picked "White"/"Black"/"Auto W/B" AND turned dynamic color off AND chose a
// custom accent color: the background/surface stays pure white (or black), but every accent role
// (primary, secondary, tertiary, containers, outline) follows the custom hue, exactly like the
// regular accent-color scheme does for Light/Dark mode.

private fun buildLightSchemeOnWhite(hue: Float) = buildLightScheme(hue).copy(
    surface                 = Color.White,
    surfaceContainerLowest  = Color.White,
    surfaceContainerLow     = Color(0xFFF8F8F8),
    surfaceContainer        = Color(0xFFF2F2F2),
    surfaceContainerHigh    = Color(0xFFEDEDED),
    surfaceContainerHighest = Color(0xFFE8E8E8),
    background              = Color.White,
    onBackground            = NearBlackText
)

private fun buildDarkSchemeOnBlack(hue: Float) = buildDarkScheme(hue).copy(
    surface                 = Color.Black,
    surfaceContainerLowest  = Color.Black,
    surfaceContainerLow     = Color(0xFF0A0A0A),
    surfaceContainer        = Color(0xFF121212),
    surfaceContainerHigh    = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    background              = Color.Black,
    onBackground            = OffWhiteText
)

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
    val colorScheme = when {
        isPureWhite && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context).copy(
                background          = Color.White,
                surface             = Color.White,
                surfaceContainerLow = Color(0xFFF8F8F8),
                surfaceContainerHigh= Color(0xFFEDEDED),
                surfaceContainer    = Color(0xFFF2F2F2),
                surfaceContainerLowest = Color.White,
                surfaceContainerHighest = Color(0xFFE8E8E8)
            )
        }
        // Dynamic color is off but the user picked a custom accent: keep the white canvas but
        // tint every accent role with their chosen hue instead of falling back to monochrome.
        isPureWhite && accentArgb != null -> {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(accentArgb, hsv)
            buildLightSchemeOnWhite(hsv[0])
        }
        isPureWhite -> PureWhiteColorScheme
        isPureBlack && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context).copy(
                background          = Color.Black,
                surface             = Color.Black,
                surfaceContainerLow = Color(0xFF0A0A0A),
                surfaceContainerHigh= Color(0xFF1A1A1A),
                surfaceContainer    = Color(0xFF121212),
                surfaceContainerLowest = Color.Black,
                surfaceContainerHighest = Color(0xFF222222)
            )
        }
        // Same fix as above, mirrored for the pure black canvas.
        isPureBlack && accentArgb != null -> {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(accentArgb, hsv)
            buildDarkSchemeOnBlack(hsv[0])
        }
        isPureBlack -> PureBlackColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        accentArgb != null -> {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(accentArgb, hsv)
            if (darkTheme) buildDarkScheme(hsv[0]) else buildLightScheme(hsv[0])
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val typography = remember(fontFamily) { buildTypography(fontFamily) }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
