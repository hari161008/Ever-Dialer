package com.coolappstore.everdialer.by.svhp.liquidglass

import androidx.compose.runtime.compositionLocalOf
import com.coolappstore.everdialer.by.svhp.liquidglass.backdrops.LayerBackdrop

/**
 * Provides the root [LayerBackdrop] that captures the full screen content,
 * so that liquid glass elements (pill nav, context menu, etc.) can sample
 * what is behind them.
 */
val LocalLiquidGlassBackdrop = compositionLocalOf<LayerBackdrop?> { null }
