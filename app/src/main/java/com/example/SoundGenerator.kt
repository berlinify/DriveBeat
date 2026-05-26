package com.example

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Native Synthwave audio synthesizer using android.media.AudioTrack.
 * Plays cute synthesized chip melodies in a background coroutine that sync
 * with the pixelated cat dancing animations!
 */
class SoundGenerator {
    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Beat callback triggered on every note tick
    var onBeatCallback: ((step: Int) -> Unit)? = null
    private var beatStep = 0

    // Volume level: 0.0f to 1.0f
    private var volume = 0.8f

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        try {
            audioTrack?.setVolume(volume)
        } catch (e: Exception) {
            Log.e("SoundGenerator", "Error setting volume", e)
        }
    }

    /**
     * Start playing synthesized melodies for a specific song mode
     */
    fun start(songName: String) {
        stop()

        beatStep = 0
        val sampleRate = 22050
        val numSamples = 2205 // 100ms ticks
        val generatedSnd = ShortArray(numSamples)

        // Select scale and speed based on song name
        val scale = when {
            songName.contains("Cyber", ignoreCase = true) -> doubleArrayOf(261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25) // C Major
            songName.contains("Grid", ignoreCase = true) -> doubleArrayOf(146.83, 164.81, 174.61, 196.00, 220.00, 246.94, 261.63, 293.66) // D minor (faster, deeper)
            songName.contains("Midnight", ignoreCase = true) -> doubleArrayOf(220.00, 246.94, 261.63, 293.66, 329.63, 349.23, 392.00, 440.00) // A minor cozy lofi
            else -> doubleArrayOf(196.00, 220.00, 246.94, 277.18, 329.63, 369.99, 392.00, 440.00) // Starry Drive (pentatonic, uplifting)
        }

        val tempoMs = when {
            songName.contains("Grid", ignoreCase = true) -> 220L // Fast (around 136 BPM)
            songName.contains("Midnight", ignoreCase = true) -> 450L // Cozy (LOFI slow)
            songName.contains("Cyber", ignoreCase = true) -> 300L // Retro upbeat
            else -> 320L // Starry Drive
        }

        // Initialize AudioTrack
        try {
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize.coerceAtLeast(numSamples * 2),
                AudioTrack.MODE_STREAM
            )
            audioTrack?.setVolume(volume)
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e("SoundGenerator", "Failed to start AudioTrack", e)
        }

        synthJob = scope.launch {
            while (true) {
                // Select note based on current step
                val noteIndex = when (beatStep % 8) {
                    0 -> 0
                    1 -> 2
                    2 -> 4
                    3 -> 5
                    4 -> 7
                    5 -> 5
                    6 -> 4
                    7 -> 2
                    else -> 0
                }
                
                // Add a bit of variations sometimes
                val frequency = scale[noteIndex % scale.size] * if ((beatStep / 8) % 2 == 1 && noteIndex == 5) 1.2 else 1.0

                // Generate synthesized square/sine wave combination for a retro sound
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Double oscillator: primary square wave + sub-octave sine wave
                    val primaryVal = if (sin(2.0 * Math.PI * frequency * t) >= 0.0) 0.15 else -0.15
                    val subVal = 0.3 * sin(2.0 * Math.PI * (frequency / 2.0) * t)
                    
                    // Standard ADSR style decay envelope
                    val envelope = 1.0 - (i.toDouble() / numSamples)
                    val sample = (primaryVal + subVal) * envelope * Short.MAX_VALUE
                    generatedSnd[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                // Write synthetic audio chunk
                try {
                    audioTrack?.write(generatedSnd, 0, numSamples)
                } catch (e: Exception) {
                    Log.e("SoundGenerator", "Error writing audio", e)
                }

                // Notify UI to animate cat/waves
                launch(Dispatchers.Main) {
                    onBeatCallback?.invoke(beatStep)
                }

                beatStep++
                delay(tempoMs)
            }
        }
    }

    /**
     * Stop synthesizing
     */
    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("SoundGenerator", "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }
}
