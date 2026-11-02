package com.coolappstore.everdialer.by.svhp.haptics

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurCircular
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DensityMedium
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.ui.graphics.vector.ImageVector
import com.coolappstore.everdialer.by.svhp.R

enum class HapticPattern(
    @get:StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
) {
    CLICK(
        labelRes = R.string.pattern_click,
        descriptionRes = R.string.pattern_click_desc,
        icon = Icons.Rounded.TouchApp,
    ),
    TICK(
        labelRes = R.string.pattern_tick,
        descriptionRes = R.string.pattern_tick_desc,
        icon = Icons.Rounded.GraphicEq,
    ),
    HEAVY_CLICK(
        labelRes = R.string.pattern_heavy_click,
        descriptionRes = R.string.pattern_heavy_click_desc,
        icon = Icons.Rounded.Bolt,
    ),
    DOUBLE_CLICK(
        labelRes = R.string.pattern_double_click,
        descriptionRes = R.string.pattern_double_click_desc,
        icon = Icons.Rounded.Repeat,
    ),
    SOFT_BUMP(
        labelRes = R.string.pattern_soft_bump,
        descriptionRes = R.string.pattern_soft_bump_desc,
        icon = Icons.Rounded.BlurCircular,
    ),
    DOUBLE_TICK(
        labelRes = R.string.pattern_double_tick,
        descriptionRes = R.string.pattern_double_tick_desc,
        icon = Icons.Rounded.DensityMedium,
    );

    companion object {
        val Default: HapticPattern = HEAVY_CLICK

        fun fromStorageKey(key: String?): HapticPattern {
            val normalized = key?.trim().orEmpty()
            if (normalized.isEmpty()) return Default

            val canonical = when (normalized.lowercase()) {
                "default" -> Default.name
                "click" -> CLICK.name
                "tick" -> TICK.name
                "heavy_click", "heavy-click", "heavyclick" -> HEAVY_CLICK.name
                "double_click", "double-click", "doubleclick" -> DOUBLE_CLICK.name
                "soft_bump", "soft-bump", "softbump" -> SOFT_BUMP.name
                "double_tick", "double-tick", "doubletick" -> DOUBLE_TICK.name
                else -> normalized.uppercase()
            }

            return entries.firstOrNull { it.name == canonical } ?: Default
        }
    }
}
