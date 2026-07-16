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
import com.coolappstore.evercallrecorder.by.svhp.data.AppPreferences

private fun performScrollHapticTick(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(10, 60))
        } else {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(10, 60))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(10L)
            }
        }
    } catch (_: Exception) {}
}

/**
 * Light haptic "tick" while scrolling a [LazyListState]-backed list, gated by the
 * existing Vibration setting (Settings → Vibration) — same toggle that already
 * controls recording-notification vibration, now also covering scroll feedback in
 * the Recordings list and the Recording Settings screen.
 */
@Composable
fun ScrollHapticsEffect(listState: LazyListState) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = remember { AppPreferences(context) }

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
            if (!prefs.isVibrationEnabled()) {
                hapticBucket = 0f
                return@collect
            }
            hapticBucket += delta
            if (hapticBucket >= pxThreshold) {
                val count = (hapticBucket / pxThreshold).toInt()
                hapticBucket -= count * pxThreshold
                performScrollHapticTick(context)
            }
        }
    }
}
