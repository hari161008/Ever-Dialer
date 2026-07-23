package com.coolappstore.evercallrecorder.by.svhp.ui.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.snapshotFlow

/**
 * Ever Dialer's own Scroll Haptics setting (Settings → Vibration → Scroll Haptics) lives in a
 * shared "rivo_prefs" SharedPreferences file. The recorder module can't depend on the app
 * module's PreferenceManager class (dependency direction is app -> recorder), so it reads the
 * same preferences file/keys directly. This keeps the Recordings list and Call Recording
 * Settings screen's scroll haptics in sync with the single toggle in Ever Dialer's Settings,
 * instead of the unrelated "Vibration" toggle in the recorder's own settings.
 */
private const val RIVO_PREFS_NAME = "rivo_prefs"
private const val KEY_SCROLL_HAPTICS = "scroll_haptics_enabled"
private const val KEY_SCROLL_HAPTIC_STRENGTH = "scroll_haptic_strength"

private fun isScrollHapticsEnabled(context: Context): Boolean =
    context.getSharedPreferences(RIVO_PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_SCROLL_HAPTICS, false)

private fun scrollHapticStrength(context: Context): Int =
    context.getSharedPreferences(RIVO_PREFS_NAME, Context.MODE_PRIVATE)
        .getInt(KEY_SCROLL_HAPTIC_STRENGTH, 60)

private fun performScrollHapticTick(context: Context, amplitude: Int) {
    try {
        val clampedAmplitude = amplitude.coerceIn(1, 255)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(10, clampedAmplitude))
        } else {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(10, clampedAmplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        }
    } catch (_: Exception) {}
}

/**
 * Light haptic "tick" while scrolling a [LazyListState]-backed list, gated by Ever Dialer's
 * Scroll Haptics setting (Settings → Vibration → Scroll Haptics) — the same toggle that
 * controls scroll haptics everywhere else in the app, now also covering the Recordings list
 * and the Call Recording Settings screen.
 */
@Composable
fun ScrollHapticsEffect(listState: LazyListState) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val pxPerCm = with(density) { (160f / 2.54f).dp.toPx() }
    val pxThreshold = (1.5f * pxPerCm).coerceAtLeast(8f)

    LaunchedEffect(listState) {
        var lastAbsolutePx = 0f
        var hapticBucket = 0f
        var initialized = false

        snapshotFlow {
            val info = listState.layoutInfo
            val firstItem = info.visibleItemsInfo.firstOrNull()
            val itemSize = firstItem?.size?.toFloat()?.takeIf { it > 0f }
                ?: info.viewportSize.height.toFloat().takeIf { it > 0f }
                ?: 1f
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            index * itemSize + offset
        }.collect { absolutePx ->
            if (!initialized) {
                lastAbsolutePx = absolutePx
                initialized = true
                return@collect
            }
            val delta = kotlin.math.abs(absolutePx - lastAbsolutePx)
            lastAbsolutePx = absolutePx
            if (!isScrollHapticsEnabled(context)) {
                hapticBucket = 0f
                return@collect
            }
            hapticBucket += delta
            if (hapticBucket >= pxThreshold) {
                val count = (hapticBucket / pxThreshold).toInt()
                hapticBucket -= count * pxThreshold
                performScrollHapticTick(context, scrollHapticStrength(context))
            }
        }
    }
}
