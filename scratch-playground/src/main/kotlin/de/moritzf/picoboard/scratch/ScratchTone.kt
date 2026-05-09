package de.moritzf.picoboard.scratch

import korlibs.audio.sound.AudioData
import korlibs.audio.sound.AudioSample
import korlibs.audio.sound.AudioSamples
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal object ScratchTone {
    private val notePattern = Regex("^([A-Ha-h])([#b]?)(-?\\d+)?$")
    private const val SAMPLE_RATE = 44_100
    private const val FADE_SECONDS = 0.015

    fun generate(note: String, durationSeconds: Double, volume: Double): AudioData {
        val sampleCount = (SAMPLE_RATE * durationSeconds).toInt()
        val samples = AudioSamples(1, sampleCount)
        val channel = samples[0]
        val frequency = frequency(note)
        val floatVolume = volume.toFloat()
        val fadeSampleCount = min((SAMPLE_RATE * FADE_SECONDS).toInt(), sampleCount / 2)

        for (sampleIndex in 0 until sampleCount) {
            val ratio = sampleIndex.toDouble() * frequency / SAMPLE_RATE
            val envelope = envelope(sampleIndex, sampleCount, fadeSampleCount)
            channel[sampleIndex] = AudioSample((sin(ratio * PI * 2.0) * floatVolume * envelope).toFloat())
        }

        return AudioData(SAMPLE_RATE, samples)
    }

    private fun envelope(sampleIndex: Int, sampleCount: Int, fadeSampleCount: Int): Double {
        if (fadeSampleCount == 0) {
            return 1.0
        }

        val fadeIn = sampleIndex.toDouble() / fadeSampleCount
        val fadeOut = (sampleCount - sampleIndex - 1).toDouble() / fadeSampleCount
        return minOf(1.0, fadeIn, fadeOut)
    }

    fun frequency(note: String): Double {
        val match = notePattern.matchEntire(note.trim())
            ?: throw IllegalArgumentException("note must look like C, C#, Cb, H, or C5")

        val baseNote = match.groupValues[1].uppercase()
        val accidental = match.groupValues[2]
        val octave = match.groupValues[3].ifBlank { "4" }.toInt()
        val semitone = baseSemitone(baseNote) + accidentalOffset(accidental)
        val midiNote = (octave + 1) * 12 + semitone

        return 440.0 * 2.0.pow((midiNote - 69) / 12.0)
    }

    private fun baseSemitone(note: String): Int = when (note) {
        "C" -> 0
        "D" -> 2
        "E" -> 4
        "F" -> 5
        "G" -> 7
        "A" -> 9
        "H" -> 11
        "B" -> 10
        else -> throw IllegalArgumentException("unsupported note: $note")
    }

    private fun accidentalOffset(accidental: String): Int = when (accidental) {
        "#" -> 1
        "b" -> -1
        "" -> 0
        else -> throw IllegalArgumentException("unsupported accidental: $accidental")
    }
}
