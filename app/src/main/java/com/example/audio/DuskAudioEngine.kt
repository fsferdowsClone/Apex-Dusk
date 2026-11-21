package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class DuskAudioEngine {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val BUFFER_SAMPLES = 1024
        private const val TWO_PI = (2.0 * PI)
    }

    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val muted = AtomicBoolean(false)

    @Volatile private var targetSpeedKmh: Float = 115f
    @Volatile private var targetNitroActive: Boolean = false
    @Volatile private var targetEngineGain: Float = 0.25f

    private val nearMissTriggerCount = AtomicInteger(0)
    @Volatile private var pendingNearMissCombo: Int = 1

    private val crashTriggerCount = AtomicInteger(0)

    private var audioThread: Thread? = null
    private var audioTrack: AudioTrack? = null

    fun start() {
        if (running.getAndSet(true)) return

        val minBufBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferBytes = maxOf(minBufBytes, BUFFER_SAMPLES * 2 * 2)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Throwable) {
            running.set(false)
            return
        }

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            running.set(false)
            return
        }

        audioTrack = track
        try {
            track.play()
        } catch (_: Throwable) {
            track.release()
            audioTrack = null
            running.set(false)
            return
        }

        audioThread = Thread(::renderLoop, "ApexDuskAudio").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun updateTelemetry(
        speedKmh: Float,
        isNitroActive: Boolean,
        isMenuPreview: Boolean,
        isPlaying: Boolean,
    ) {
        targetSpeedKmh = speedKmh
        targetNitroActive = isNitroActive && isPlaying
        targetEngineGain = when {
            isPlaying -> 1.0f
            isMenuPreview -> 0.28f
            else -> 0.08f
        }
    }

    fun triggerNearMiss(combo: Int) {
        pendingNearMissCombo = combo
        nearMissTriggerCount.incrementAndGet()
    }

    fun triggerCrash() {
        crashTriggerCount.incrementAndGet()
    }

    fun setMuted(isMuted: Boolean) {
        muted.set(isMuted)
    }

    fun pause() {
        paused.set(true)
        try {
            audioTrack?.pause()
        } catch (_: Throwable) {
        }
    }

    fun resume() {
        paused.set(false)
        try {
            if (running.get()) {
                audioTrack?.play()
            }
        } catch (_: Throwable) {
        }
    }

    fun release() {
        running.set(false)
        paused.set(false)
        try {
            audioThread?.join(300)
        } catch (_: InterruptedException) {
        }
        audioThread = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Throwable) {
        }
        audioTrack = null
    }

    private fun renderLoop() {
        val pcm = ShortArray(BUFFER_SAMPLES)

        var enginePhase1 = 0.0
        var enginePhase2 = 0.0
        var enginePhaseSub = 0.0
        var lfoPhase = 0.0
        var turbinePhase = 0.0

        var smoothedSpeed = 115f
        var smoothedEngineGain = 0.25f
        var smoothedNitro = 0f

        var noiseState = 0x12345678
        var filterLp = 0.0
        var filterBp = 0.0

        var lastNearMissSeen = nearMissTriggerCount.get()
        var chimeTimer = -1.0
        var chimeBaseFreq = 523.25
        var chimePhase1 = 0.0
        var chimePhase2 = 0.0
        var chimePhase3 = 0.0

        var lastCrashSeen = crashTriggerCount.get()
        var crashTimer = -1.0
        var crashPhase = 0.0

        val invSampleRate = 1.0 / SAMPLE_RATE

        while (running.get()) {
            if (paused.get()) {
                try {
                    Thread.sleep(25)
                } catch (_: InterruptedException) {
                }
                continue
            }

            val currentNearMiss = nearMissTriggerCount.get()
            if (currentNearMiss != lastNearMissSeen) {
                lastNearMissSeen = currentNearMiss
                chimeTimer = 0.0
                val comboSemitones = (pendingNearMissCombo - 1).coerceIn(0, 8)
                chimeBaseFreq = 523.25 * Math.pow(2.0, comboSemitones / 12.0)
                chimePhase1 = 0.0
                chimePhase2 = 0.0
                chimePhase3 = 0.0
            }

            val currentCrash = crashTriggerCount.get()
            if (currentCrash != lastCrashSeen) {
                lastCrashSeen = currentCrash
                crashTimer = 0.0
                crashPhase = 0.0
            }

            val isMutedNow = muted.get()
            val tSpeed = targetSpeedKmh
            val tGain = targetEngineGain
            val tNitro = if (targetNitroActive) 1f else 0f

            for (i in 0 until BUFFER_SAMPLES) {
                smoothedSpeed += (tSpeed - smoothedSpeed) * 0.0012f
                smoothedEngineGain += (tGain - smoothedEngineGain) * 0.002f
                smoothedNitro += (tNitro - smoothedNitro) * 0.0025f

                // 1. Sportbike engine hum (harmonic saw-triangle hybrid + sub-octave + exhaust flutter)
                val speedRatio = ((smoothedSpeed - 60f) / 280f).coerceIn(0f, 1.15f)
                val baseFreq = 64.0 + speedRatio * 145.0 + (smoothedNitro * 24.0)

                enginePhase1 = (enginePhase1 + baseFreq * invSampleRate) % 1.0
                enginePhase2 = (enginePhase2 + (baseFreq * 1.995) * invSampleRate) % 1.0
                enginePhaseSub = (enginePhaseSub + (baseFreq * 0.5) * invSampleRate) % 1.0
                lfoPhase = (lfoPhase + (18.0 + speedRatio * 22.0) * invSampleRate) % 1.0

                val saw1 = 2.0 * enginePhase1 - 1.0
                val tri2 = 1.0 - 4.0 * kotlin.math.abs(enginePhase2 - 0.5)
                val sub = sin(TWO_PI * enginePhaseSub)
                val exhaustPulse = 0.85 + 0.15 * sin(TWO_PI * lfoPhase)
                val engineSample =
                    (0.32 * saw1 + 0.28 * tri2 + 0.40 * sub) * exhaustPulse * 0.22 * smoothedEngineGain

                // 2. Nitro boost filtered noise whoosh + turbine whistle
                noiseState = noiseState xor (noiseState shl 13)
                noiseState = noiseState xor (noiseState ushr 17)
                noiseState = noiseState xor (noiseState shl 5)
                val white = (noiseState and 0xFFFF) / 32768.0 - 1.0

                // Simple state-variable bandpass filter for jet whoosh
                val cutoff = 0.12 + smoothedNitro * 0.28
                filterLp += cutoff * (white - filterLp)
                filterBp += 0.25 * (filterLp - filterBp)

                turbinePhase = (turbinePhase + (380.0 + smoothedNitro * 440.0) * invSampleRate) % 1.0
                val turbine = sin(TWO_PI * turbinePhase) * 0.10
                val nitroSample = if (smoothedNitro > 0.01f) {
                    (filterBp * 0.42 + turbine) * smoothedNitro
                } else {
                    0.0
                }

                // 3. Crisp synth chime for Near-Miss combos
                var chimeSample = 0.0
                if (chimeTimer >= 0.0) {
                    val t = chimeTimer
                    if (t < 0.48) {
                        chimePhase1 = (chimePhase1 + chimeBaseFreq * invSampleRate) % 1.0
                        chimePhase2 = (chimePhase2 + (chimeBaseFreq * 1.25) * invSampleRate) % 1.0
                        chimePhase3 = (chimePhase3 + (chimeBaseFreq * 1.50) * invSampleRate) % 1.0

                        val env1 = exp(-t * 10.0)
                        val env2 = if (t > 0.045) exp(-(t - 0.045) * 9.0) else 0.0
                        val env3 = if (t > 0.090) exp(-(t - 0.090) * 8.0) else 0.0

                        chimeSample = (
                            sin(TWO_PI * chimePhase1) * env1 * 0.35 +
                                sin(TWO_PI * chimePhase2) * env2 * 0.32 +
                                sin(TWO_PI * chimePhase3) * env3 * 0.35
                            ) * 0.45
                        chimeTimer += invSampleRate
                    } else {
                        chimeTimer = -1.0
                    }
                }

                // 4. Low-frequency impact crash sound
                var crashSample = 0.0
                if (crashTimer >= 0.0) {
                    val t = crashTimer
                    if (t < 0.65) {
                        val impactFreq = 145.0 * exp(-t * 11.0) + 32.0
                        crashPhase = (crashPhase + impactFreq * invSampleRate) % 1.0
                        val subBoom = sin(TWO_PI * crashPhase) * exp(-t * 6.5) * 0.85
                        val crunch = white * exp(-t * 9.5) * 0.55
                        crashSample = (subBoom + crunch) * 0.68
                        crashTimer += invSampleRate
                    } else {
                        crashTimer = -1.0
                    }
                }

                val mixed = if (isMutedNow) {
                    0.0
                } else {
                    (engineSample + nitroSample + chimeSample + crashSample).coerceIn(-0.98, 0.98)
                }

                pcm[i] = (mixed * 32767.0).toInt().toShort()
            }

            val track = audioTrack ?: break
            try {
                val written = track.write(pcm, 0, pcm.size)
                if (written < 0) break
            } catch (_: Throwable) {
                break
            }
        }
    }
}
