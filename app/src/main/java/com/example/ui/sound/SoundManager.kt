package com.example.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sound Effect Types for game events
 */
enum class SoundEffect {
    PIECE_PLACE_X,       // High clean tone for player X placement
    PIECE_PLACE_O,       // Deep resonant tone for player O placement
    PIECE_PLACE_TICK,    // Crisp rising chime for player Tick placement
    PIECE_PLACE_TRIANGLE,// Rich warm chime for player Triangle placement
    PIECE_VANISH,        // Subtle swoosh / disappearing decay tone when piece limit triggers FIFO drop
    POINT_SCORED,        // Harmonious chord when 1 point is scored in endless mode
    VICTORY_FANFARE,     // Grand celebration arpeggio on match completion
    BUTTON_CLICK,        // Crisp interface feedback click
    REACTION_POP         // Pop bubble sound for live reactions
}

/**
 * High-performance, zero-latency procedural audio synthesizer and sound effect manager.
 * Generates crisp 44.1kHz PCM waveforms dynamically without external asset dependencies or network buffering.
 */
class SoundManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val sampleRate = 44100

    private val prefs = context.getSharedPreferences("ettt_sound_settings", Context.MODE_PRIVATE)

    private val _isSoundEnabled = MutableStateFlow(prefs.getBoolean("sound_enabled", true))
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    // Precomputed PCM buffers and reusable static AudioTracks for instantaneous playback
    private val soundBuffers = mutableMapOf<SoundEffect, ShortArray>()
    private val audioTracks = mutableMapOf<SoundEffect, AudioTrack>()

    init {
        precomputeSoundBuffers()
        initAudioTracks()
    }

    companion object {
        @Volatile
        private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        if (enabled) {
            playSound(SoundEffect.BUTTON_CLICK)
        }
    }

    fun toggleSound() {
        setSoundEnabled(!_isSoundEnabled.value)
    }

    private fun initAudioTracks() {
        soundBuffers.forEach { (effect, pcmData) ->
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(pcmData.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcmData, 0, pcmData.size)
                audioTracks[effect] = track
            } catch (e: Exception) {
                Log.w("SoundManager", "Error initializing audio track for $effect: ${e.message}")
            }
        }
    }

    fun playSound(effect: SoundEffect) {
        if (!_isSoundEnabled.value) return

        scope.launch {
            try {
                val track = audioTracks[effect]
                if (track != null) {
                    synchronized(track) {
                        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            track.stop()
                        }
                        track.setPlaybackHeadPosition(0)
                        track.play()
                    }
                }
            } catch (e: Exception) {
                Log.w("SoundManager", "Failed to play sound: ${e.message}")
            }
        }
    }

    private fun precomputeSoundBuffers() {
        soundBuffers[SoundEffect.PIECE_PLACE_X] = generateTone(
            freqStart = 587.33, // D5
            freqEnd = 880.0,    // A5
            durationMs = 90,
            volume = 0.75f,
            envelopeType = Envelope.POP
        )

        soundBuffers[SoundEffect.PIECE_PLACE_O] = generateTone(
            freqStart = 329.63, // E4
            freqEnd = 440.0,    // A4
            durationMs = 110,
            volume = 0.8f,
            envelopeType = Envelope.POP
        )

        soundBuffers[SoundEffect.PIECE_PLACE_TICK] = generateTone(
            freqStart = 493.88, // B4
            freqEnd = 739.99,   // F#5
            durationMs = 95,
            volume = 0.75f,
            envelopeType = Envelope.POP
        )

        soundBuffers[SoundEffect.PIECE_PLACE_TRIANGLE] = generateTone(
            freqStart = 415.30, // G#4
            freqEnd = 622.25,   // D#5
            durationMs = 105,
            volume = 0.8f,
            envelopeType = Envelope.POP
        )

        soundBuffers[SoundEffect.PIECE_VANISH] = generateVanishSound(
            durationMs = 180,
            volume = 0.7f
        )

        soundBuffers[SoundEffect.POINT_SCORED] = generateChord(
            frequencies = listOf(523.25, 659.25, 783.99, 1046.50), // C Major (C5, E5, G5, C6)
            durationMs = 380,
            volume = 0.8f
        )

        soundBuffers[SoundEffect.VICTORY_FANFARE] = generateFanfare(
            frequencies = listOf(523.25, 659.25, 783.99, 1046.50, 1318.51), // C-E-G-C-E arpeggio
            noteDurationMs = 90,
            volume = 0.85f
        )

        soundBuffers[SoundEffect.BUTTON_CLICK] = generateTone(
            freqStart = 800.0,
            freqEnd = 1200.0,
            durationMs = 35,
            volume = 0.5f,
            envelopeType = Envelope.CLICK
        )

        soundBuffers[SoundEffect.REACTION_POP] = generateTone(
            freqStart = 400.0,
            freqEnd = 1100.0,
            durationMs = 80,
            volume = 0.7f,
            envelopeType = Envelope.POP
        )
    }

    private enum class Envelope {
        POP, CLICK, FADE
    }

    private fun generateTone(
        freqStart: Double,
        freqEnd: Double,
        durationMs: Int,
        volume: Float,
        envelopeType: Envelope
    ): ShortArray {
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val currentFreq = freqStart + (freqEnd - freqStart) * progress
            phase += 2.0 * PI * currentFreq / sampleRate

            val envelope = when (envelopeType) {
                Envelope.CLICK -> exp(-15.0 * progress)
                Envelope.POP -> {
                    if (progress < 0.1) progress / 0.1 else exp(-6.0 * (progress - 0.1))
                }
                Envelope.FADE -> sin(progress * PI)
            }

            val sample = (sin(phase) * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateVanishSound(durationMs: Int, volume: Float): ShortArray {
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        var phase = 0.0

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            // Downward sweeping pitch with subtle noise/vibrato for vanishing effect
            val freq = 880.0 * (1.0 - progress * 0.7)
            phase += 2.0 * PI * freq / sampleRate
            val envelope = (1.0 - progress) * (1.0 - progress)
            val sample = (sin(phase) * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateChord(frequencies: List<Double>, durationMs: Int, volume: Float): ShortArray {
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)
        val phases = DoubleArray(frequencies.size)

        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val envelope = if (progress < 0.05) progress / 0.05 else exp(-3.0 * (progress - 0.05))

            var sum = 0.0
            for (f in frequencies.indices) {
                phases[f] += 2.0 * PI * frequencies[f] / sampleRate
                sum += sin(phases[f])
            }
            sum /= frequencies.size

            val sample = (sum * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateFanfare(frequencies: List<Double>, noteDurationMs: Int, volume: Float): ShortArray {
        val noteSamples = (sampleRate * (noteDurationMs / 1000.0)).toInt()
        val totalSamples = noteSamples * frequencies.size + (sampleRate * 0.3).toInt()
        val buffer = ShortArray(totalSamples)

        for (noteIdx in frequencies.indices) {
            val freq = frequencies[noteIdx]
            var phase = 0.0
            val startSample = noteIdx * noteSamples
            val noteLength = if (noteIdx == frequencies.lastIndex) noteSamples + (sampleRate * 0.3).toInt() else noteSamples

            for (i in 0 until noteLength) {
                if (startSample + i >= totalSamples) break
                val progress = i.toDouble() / noteLength
                phase += 2.0 * PI * freq / sampleRate
                val envelope = if (progress < 0.1) progress / 0.1 else exp(-2.5 * (progress - 0.1))
                val sample = (sin(phase) * envelope * volume * Short.MAX_VALUE).toInt()
                buffer[startSample + i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return buffer
    }
}
