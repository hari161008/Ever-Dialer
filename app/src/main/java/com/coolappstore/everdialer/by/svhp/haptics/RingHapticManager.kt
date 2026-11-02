package com.coolappstore.everdialer.by.svhp.haptics

import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

object RingHapticManager {

    private const val TAG = "RingHapticManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, t -> Log.w(TAG, "Coroutine failed", t) })

    private var ringingJob: Job? = null
    private var visualizer: Visualizer? = null
    private val isRingingActive = AtomicBoolean(false)
    private val lastHapticMs = AtomicLong(0L)

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun isSystemRingVibrationEnabled(context: Context): Boolean {
        return try {
            val cr = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val ringIntensity = android.provider.Settings.System.getInt(cr, "ring_vibration_intensity", -1)
                if (ringIntensity == 0) return false
            }
            val vibrateWhenRinging = android.provider.Settings.System.getInt(cr, "vibrate_when_ringing", -1)
            if (vibrateWhenRinging == 0) return false

            val samsungVibrate = android.provider.Settings.System.getInt(cr, "vibrate_on_call", -1)
            if (samsungVibrate == 0) return false

            val miuiVibrate = android.provider.Settings.System.getInt(cr, "vibrate_in_normal", -1)
            if (miuiVibrate == 0) {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                if (am?.ringerMode == android.media.AudioManager.RINGER_MODE_NORMAL) return false
            }
            true
        } catch (_: Exception) {
            true
        }
    }

    fun shouldUseTouchRouting(context: Context): Boolean {
        val prefs = PreferenceManager(context)
        if (prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_FORCE_TOUCH_ROUTING, false)) {
            return true
        }
        return !isSystemRingVibrationEnabled(context)
    }

    private fun getHapticAttributes(context: Context, isPreview: Boolean = false): VibrationAttributes? {
        val useTouch = isPreview || shouldUseTouchRouting(context)
        val usage = if (useTouch) {
            VibrationAttributes.USAGE_TOUCH
        } else {
            VibrationAttributes.USAGE_RINGTONE
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            VibrationAttributes.Builder().setUsage(usage).build()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            VibrationAttributes.createForUsage(usage)
        } else {
            null
        }
    }

    private fun doVibrate(vibrator: Vibrator, effect: VibrationEffect, attrs: VibrationAttributes?) {
        try {
            if (attrs != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                vibrator.vibrate(effect, attrs)
            } else {
                vibrator.vibrate(effect)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to vibrate with attributes, attempting direct vibrate", e)
            try {
                vibrator.vibrate(effect)
            } catch (_: Exception) {}
        }
    }

    fun startIncomingCallHaptics(context: Context) {
        val appContext = context.applicationContext
        val prefs = PreferenceManager(appContext)

        val enabled = prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_ENABLED, false)
        if (!enabled) return

        if (!isRingingActive.compareAndSet(false, true)) {
            // Already ringing
            return
        }

        val pattern = HapticPattern.fromStorageKey(prefs.getString(PreferenceManager.KEY_RING_HAPTIC_PATTERN, HapticPattern.HEAVY_CLICK.name))
        val intensity = prefs.getFloat(PreferenceManager.KEY_RING_HAPTIC_INTENSITY, 1.0f).coerceIn(0f, 1f)
        val customSequence = prefs.getRingHapticCustomSequence()
        val isAuto = prefs.getBoolean(PreferenceManager.KEY_RING_HAPTIC_AUTO, false)
        val sensitivity = prefs.getFloat(PreferenceManager.KEY_RING_HAPTIC_AUTO_SENSITIVITY, 0.65f)
        val strength = prefs.getFloat(PreferenceManager.KEY_RING_HAPTIC_AUTO_STRENGTH, 0.8f)

        stopIncomingCallHapticsInternal()
        isRingingActive.set(true)

        val vibrator = getVibrator(appContext)
        val attrs = getHapticAttributes(appContext, isPreview = false)

        if (isAuto) {
            startAutoVisualizer(appContext, vibrator, sensitivity, strength, attrs)
        } else {
            ringingJob = scope.launch {
                while (isActive && isRingingActive.get()) {
                    fireHaptic(vibrator, pattern, intensity, customSequence, attrs)
                    val duration = if (!customSequence.isEmpty) customSequence.durationMs + 800L else 1500L
                    delay(duration.coerceAtLeast(1200L))
                }
            }
        }
    }

    fun stopIncomingCallHaptics() {
        if (!isRingingActive.getAndSet(false)) return
        stopIncomingCallHapticsInternal()
    }

    private fun stopIncomingCallHapticsInternal() {
        ringingJob?.cancel()
        ringingJob = null
        stopAutoVisualizer()
    }

    fun testCallHaptic(context: Context) {
        val appContext = context.applicationContext
        val prefs = PreferenceManager(appContext)
        val pattern = HapticPattern.fromStorageKey(prefs.getString(PreferenceManager.KEY_RING_HAPTIC_PATTERN, HapticPattern.HEAVY_CLICK.name))
        val intensity = prefs.getFloat(PreferenceManager.KEY_RING_HAPTIC_INTENSITY, 1.0f).coerceIn(0f, 1f)
        val customSequence = prefs.getRingHapticCustomSequence()

        val vibrator = getVibrator(appContext)
        val attrs = getHapticAttributes(appContext, isPreview = true)

        scope.launch {
            fireHaptic(vibrator, pattern, intensity, customSequence, attrs)
        }
    }

    fun playSinglePattern(context: Context, pattern: HapticPattern, intensity: Float) {
        val appContext = context.applicationContext
        val vibrator = getVibrator(appContext)
        val attrs = getHapticAttributes(appContext, isPreview = true)
        scope.launch {
            val effect = buildEffect(vibrator, pattern, intensity)
            withContext(Dispatchers.Main) {
                doVibrate(vibrator, effect, attrs)
            }
        }
    }

    fun playSequence(context: Context, sequence: CustomHapticSequence) {
        val appContext = context.applicationContext
        val vibrator = getVibrator(appContext)
        val attrs = getHapticAttributes(appContext, isPreview = true)
        scope.launch {
            playCustomSequence(vibrator, sequence, attrs)
        }
    }

    private suspend fun fireHaptic(
        vibrator: Vibrator,
        pattern: HapticPattern,
        intensity: Float,
        customSequence: CustomHapticSequence,
        attrs: VibrationAttributes?
    ) {
        if (!customSequence.isEmpty) {
            playCustomSequence(vibrator, customSequence, attrs)
            return
        }
        val effect = buildEffect(vibrator, pattern, intensity)
        withContext(Dispatchers.Main) {
            doVibrate(vibrator, effect, attrs)
        }
    }

    private fun buildEffect(vibrator: Vibrator, pattern: HapticPattern, intensity: Float): VibrationEffect {
        val hasComp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
        val hasAmp = vibrator.hasAmplitudeControl()
        val amp = (intensity * 255f).toInt().coerceIn(1, 255)

        if (hasComp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return VibrationEffect.startComposition().apply {
                when (pattern) {
                    HapticPattern.CLICK -> addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    HapticPattern.TICK -> addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                    HapticPattern.HEAVY_CLICK -> {
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, (intensity * 0.6f).coerceIn(0f, 1f), 40)
                    }
                    HapticPattern.DOUBLE_CLICK -> {
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity, 80)
                    }
                    HapticPattern.SOFT_BUMP -> {
                        val p = if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_LOW_TICK))
                            VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                        else
                            VibrationEffect.Composition.PRIMITIVE_TICK
                        addPrimitive(p, (intensity * 0.5f).coerceIn(0f, 1f))
                    }
                    HapticPattern.DOUBLE_TICK -> {
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity)
                        addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, intensity, 60)
                    }
                }
            }.compose()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (pattern) {
                HapticPattern.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticPattern.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticPattern.HEAVY_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                HapticPattern.DOUBLE_CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                HapticPattern.SOFT_BUMP -> if (hasAmp) VibrationEffect.createOneShot(30L, (amp * 0.5f).toInt().coerceIn(1, 255))
                else VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticPattern.DOUBLE_TICK -> if (hasAmp) VibrationEffect.createWaveform(longArrayOf(0L, 30L, 60L, 30L), intArrayOf(0, amp, 0, amp), -1)
                else VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            }
        }

        return VibrationEffect.createOneShot(50L, amp)
    }

    private suspend fun playCustomSequence(vibrator: Vibrator, seq: CustomHapticSequence, attrs: VibrationAttributes?) {
        val sorted = seq.beats.sortedBy { it.offsetMs }
        val startMs = System.currentTimeMillis()
        for (beat in sorted) {
            if (!isRingingActive.get() && ringingJob != null) break
            val wait = beat.offsetMs - (System.currentTimeMillis() - startMs)
            if (wait > 0) delay(wait)
            withContext(Dispatchers.Main) {
                val effect = VibrationEffect.createOneShot(80L, beat.amplitude.coerceIn(1, 255))
                doVibrate(vibrator, effect, attrs)
            }
        }
    }

    private fun startAutoVisualizer(
        context: Context,
        vibrator: Vibrator,
        sensitivity: Float,
        strength: Float,
        attrs: VibrationAttributes?
    ) {
        stopAutoVisualizer()
        try {
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtLeast(1024)
            val vis = Visualizer(0)
            vis.captureSize = captureSize
            val dynamicAvg = floatArrayOf(0.04f)
            vis.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer, w: ByteArray, sr: Int) {}
                override fun onFftDataCapture(v: Visualizer, fft: ByteArray, samplingRate: Int) {
                    if (!isRingingActive.get()) return
                    val rateHz = (samplingRate / 1000).coerceAtLeast(8000)
                    val freqPerBin = rateHz.toFloat() / captureSize
                    val bassStart = (40f / freqPerBin).toInt().coerceIn(1, fft.size / 2 - 4)
                    val bassEnd = (300f / freqPerBin).toInt().coerceIn(bassStart + 2, fft.size / 2 - 2)
                    var sumSq = 0.0
                    var count = 0
                    for (k in bassStart..bassEnd) {
                        val re = fft[k * 2].toFloat()
                        val im = fft[k * 2 + 1].toFloat()
                        sumSq += re * re + im * im
                        count++
                    }
                    if (count == 0) return
                    val energy = sqrt(sumSq / count).toFloat() / 128f
                    dynamicAvg[0] = dynamicAvg[0] * 0.9f + energy * 0.1f
                    val threshold = dynamicAvg[0] * (1.8f - sensitivity * 1.0f)
                    val now = System.currentTimeMillis()
                    val last = lastHapticMs.get()
                    if (energy > threshold && energy > 0.008f && (now - last) >= 80L) {
                        if (lastHapticMs.compareAndSet(last, now)) {
                            val bs = ((energy - threshold) / threshold).coerceIn(0f, 1f)
                            val amp = (strength * (0.35f + 0.65f * bs) * 255f).toInt().coerceIn(30, 255)
                            val dur = (35L + (bs * 55f).toLong()).coerceIn(30L, 90L)
                            runCatching {
                                val effect = VibrationEffect.createOneShot(dur, amp)
                                doVibrate(vibrator, effect, attrs)
                            }
                        }
                    }
                }
            }, Visualizer.getMaxCaptureRate() / 2, false, true)
            vis.enabled = true
            visualizer = vis
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start Auto Visualizer", e)
        }
    }

    private fun stopAutoVisualizer() {
        visualizer?.apply {
            runCatching { enabled = false }
            runCatching { release() }
        }
        visualizer = null
    }
}
