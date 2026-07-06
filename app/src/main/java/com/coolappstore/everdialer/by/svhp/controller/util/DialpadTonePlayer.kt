package com.coolappstore.everdialer.by.svhp.controller.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/** The available dialpad keypress sound styles, picked in Sound & Vibration settings. */
enum class DialpadToneStyle(val key: String, val label: String, val description: String) {
    STANDARD("standard", "Standard Beeps", "Classic DTMF dial tones — the default"),
    PIANO("piano", "Piano Notes", "Every key plays a musical note, like an instrument"),
    WATER_DROP("water_drop", "Water Drops", "Soft, relaxing droplet sounds"),
    MECHANICAL("mechanical", "Mechanical Keyboard", "Clicky, satisfying ASMR-style key switches"),
    SCIFI("scifi", "Sci-Fi / Retro Gaming", "Futuristic 8-bit blips and zaps");

    companion object {
        fun fromKey(key: String?): DialpadToneStyle = entries.firstOrNull { it.key == key } ?: STANDARD
    }
}

/**
 * Plays the sound that accompanies a dialpad keypress. "Standard" reuses the system's real
 * [ToneGenerator] DTMF tones unchanged — fast and non-blocking, since it's just a native call.
 *
 * Every other style is a tiny PCM clip synthesized in code (no audio assets needed). The
 * synthesis, WAV encoding, and [SoundPool] loading all happen once per key on a background
 * thread and are cached; every later press is just a [SoundPool.play] call, which is as cheap
 * as the standard tone. This avoids doing CPU-heavy synthesis or allocating a new AudioTrack
 * on the caller's thread (the UI thread) on every single keypress, which is what made the
 * dialpad feel slow/jittery whenever a non-default tone style was selected.
 */
object DialpadTonePlayer {

    private const val SAMPLE_RATE = 44100

    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // key = "${style.key}:$digit" -> loaded SoundPool sound id (once ready to play)
    private val soundIdCache = ConcurrentHashMap<String, Int>()
    // Guards against kicking off the same synth+load job twice while it's in flight
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    private var soundPool: SoundPool? = null

    private fun soundPoolFor(context: Context): SoundPool {
        soundPool?.let { return it }
        synchronized(this) {
            soundPool?.let { return it }
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val pool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(attributes)
                .build()
            soundPool = pool
            return pool
        }
    }

    /** Plays the keypress sound for [digit] (one of 0-9, *, #) in the given [style]. */
    fun play(context: Context, digit: String, style: DialpadToneStyle) {
        if (style == DialpadToneStyle.STANDARD) {
            toneTypeFor(digit)?.let { playTone(it) }
            return
        }

        val cacheKey = "${style.key}:$digit"
        val appContext = context.applicationContext
        val pool = soundPoolFor(appContext)

        val cachedId = soundIdCache[cacheKey]
        if (cachedId != null) {
            // Already loaded — this is just a native call, same cost as the standard tone.
            pool.play(cachedId, 1f, 1f, 1, 0, 1f)
            return
        }

        // Not loaded yet: kick off synthesis + loading on a background thread, never on the
        // caller's (UI) thread. If it's already in flight for this key, don't queue it twice.
        if (!inFlight.add(cacheKey)) return
        ioExecutor.execute {
            try {
                val pcm = synthesize(style, digit)
                if (pcm.isNotEmpty()) {
                    val wavFile = File(appContext.cacheDir, "dtmf_${style.key}_${digit.hashCode()}.wav")
                    writeWav(wavFile, pcm)
                    val soundId = pool.load(wavFile.absolutePath, 1)
                    pool.setOnLoadCompleteListener { sp, id, status ->
                        if (status == 0 && id == soundId) {
                            soundIdCache[cacheKey] = soundId
                            // Play the very press that triggered the load, once it's ready.
                            mainHandler.post { sp.play(soundId, 1f, 1f, 1, 0, 1f) }
                        }
                        inFlight.remove(cacheKey)
                    }
                } else {
                    inFlight.remove(cacheKey)
                }
            } catch (_: Exception) {
                inFlight.remove(cacheKey)
            }
        }
    }

    private fun toneTypeFor(digit: String): Int? = when (digit) {
        "0" -> ToneGenerator.TONE_DTMF_0
        "1" -> ToneGenerator.TONE_DTMF_1
        "2" -> ToneGenerator.TONE_DTMF_2
        "3" -> ToneGenerator.TONE_DTMF_3
        "4" -> ToneGenerator.TONE_DTMF_4
        "5" -> ToneGenerator.TONE_DTMF_5
        "6" -> ToneGenerator.TONE_DTMF_6
        "7" -> ToneGenerator.TONE_DTMF_7
        "8" -> ToneGenerator.TONE_DTMF_8
        "9" -> ToneGenerator.TONE_DTMF_9
        "*" -> ToneGenerator.TONE_DTMF_S
        "#" -> ToneGenerator.TONE_DTMF_P
        else -> null
    }

    private fun playTone(toneType: Int) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_DTMF, 80)
            toneGen.startTone(toneType, 150)
            mainHandler.postDelayed({ toneGen.release() }, 200)
        } catch (_: Exception) {}
    }

    // ── Minimal WAV encoding so SoundPool can load our synthesized PCM ──────

    private fun writeWav(file: File, pcm: ShortArray) {
        val dataSize = pcm.size * 2
        val byteRate = SAMPLE_RATE * 2
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeBytes("RIFF")
            raf.write(intLE(36 + dataSize))
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.write(intLE(16))
            raf.write(shortLE(1))              // PCM
            raf.write(shortLE(1))              // mono
            raf.write(intLE(SAMPLE_RATE))
            raf.write(intLE(byteRate))
            raf.write(shortLE(2))               // block align
            raf.write(shortLE(16))              // bits per sample
            raf.writeBytes("data")
            raf.write(intLE(dataSize))
            val bytes = ByteArray(dataSize)
            for (i in pcm.indices) {
                val v = pcm[i].toInt()
                bytes[i * 2] = (v and 0xFF).toByte()
                bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
            }
            raf.write(bytes)
        }
    }

    private fun intLE(v: Int) = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte()
    )

    private fun shortLE(v: Int) = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte()
    )

    // ── Synthesis dispatch ───────────────────────────────────────────────────

    private fun synthesize(style: DialpadToneStyle, digit: String): ShortArray = when (style) {
        DialpadToneStyle.PIANO -> synthesizePiano(digit)
        DialpadToneStyle.WATER_DROP -> synthesizeWaterDrop(digit)
        DialpadToneStyle.MECHANICAL -> synthesizeMechanical(digit)
        DialpadToneStyle.SCIFI -> synthesizeSciFi(digit)
        DialpadToneStyle.STANDARD -> ShortArray(0)
    }

    private fun digitIndex(digit: String): Int = when (digit) {
        "1", "2", "3", "4", "5", "6", "7", "8", "9" -> digit.toInt()
        "0" -> 0
        "*" -> 10
        "#" -> 11
        else -> 5
    }

    /** 1-9 map to a C-major run (C4..D5), with 0 / * / # as lower & upper bookend notes. */
    private fun noteFrequency(digit: String): Double = when (digit) {
        "1" -> 261.63 // C4
        "2" -> 293.66 // D4
        "3" -> 329.63 // E4
        "4" -> 349.23 // F4
        "5" -> 392.00 // G4
        "6" -> 440.00 // A4
        "7" -> 493.88 // B4
        "8" -> 523.25 // C5
        "9" -> 587.33 // D5
        "0" -> 130.81 // C3 — low anchor note
        "*" -> 659.25 // E5
        "#" -> 783.99 // G5
        else -> 440.0
    }

    // 1) Piano — additive harmonics (fundamental + overtones) with a plucked-string decay
    private fun synthesizePiano(digit: String): ShortArray {
        val freq = noteFrequency(digit)
        val durationSec = 0.42
        val samples = (SAMPLE_RATE * durationSec).toInt()
        val attackSamples = (SAMPLE_RATE * 0.004).toInt().coerceAtLeast(1)
        val out = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val decay = exp(-3.6 * t)
            val env = attack * decay
            val wave = 0.55 * sin(2 * PI * freq * t) +
                0.24 * sin(2 * PI * freq * 2 * t) +
                0.12 * sin(2 * PI * freq * 3 * t) +
                0.06 * sin(2 * PI * freq * 4 * t)
            out[i] = (env * wave * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    // 2) Water Drop — a downward pitch glide with a fast "pop" envelope and a touch of splash
    private fun synthesizeWaterDrop(digit: String): ShortArray {
        val baseFreq = 650.0 + digitIndex(digit) * 45.0
        val durationSec = 0.26
        val samples = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(samples)
        var phase = 0.0
        val random = Random(digit.hashCode())
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = baseFreq * exp(-14.0 * t) + 220.0
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = exp(-9.0 * t)
            val splash = if (t < 0.012) (random.nextDouble(-1.0, 1.0) * exp(-300.0 * t)) * 0.25 else 0.0
            val wave = sin(phase) * env + splash
            out[i] = (wave * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    // 3) Mechanical Keyboard — broadband click transient + a low "thock" body
    private fun synthesizeMechanical(digit: String): ShortArray {
        val durationSec = 0.07
        val samples = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(samples)
        val random = Random(digit.hashCode() * 31 + 17)
        val thockFreq = 130.0 + digitIndex(digit) * 6.0
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            val clickEnv = exp(-420.0 * t)
            val thockEnv = exp(-55.0 * t)
            val click = random.nextDouble(-1.0, 1.0) * clickEnv
            val thock = sin(2 * PI * thockFreq * t) * thockEnv
            val wave = click * 0.55 + thock * 0.55
            out[i] = (wave * Short.MAX_VALUE * 0.8).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    // 4) Sci-Fi / Retro Gaming — an upward square-wave "blip" sweep, like an 8-bit UI sound
    private fun synthesizeSciFi(digit: String): ShortArray {
        val startFreq = 280.0 + digitIndex(digit) * 60.0
        val endFreq = startFreq * 2.4
        val durationSec = 0.16
        val samples = (SAMPLE_RATE * durationSec).toInt()
        val out = ShortArray(samples)
        var phase = 0.0
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = (t / durationSec).coerceIn(0.0, 1.0)
            val freq = startFreq + (endFreq - startFreq) * progress
            phase += 2 * PI * freq / SAMPLE_RATE
            val env = exp(-9.0 * t)
            val square = sign(sin(phase))
            out[i] = (square * env * Short.MAX_VALUE * 0.5).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }
}
