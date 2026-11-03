package com.otokana.core

import kotlin.math.pow

object Notes {
    private val names = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /** The name of the MIDI note, as UTAU writes it: E4 for 64, C#4 for 61, C-1 for 0. */
    fun name(midi: Int): String = names[Math.floorMod(midi, 12)] + (Math.floorDiv(midi, 12) - 1)

    /** The MIDI note of a name (# ♯ b ♭ accepted, any case), null unless 0..127. */
    fun parse(name: String?): Int? {
        var s = name?.trim().orEmpty()
        if (s.isEmpty()) return null
        val letter = "CDEFGAB".indexOf(s[0].uppercaseChar())
        if (letter < 0) return null
        var midi = intArrayOf(0, 2, 4, 5, 7, 9, 11)[letter]
        s = s.substring(1)
        for ((sign, step) in listOf("#" to 1, "♯" to 1, "b" to -1, "♭" to -1)) {
            if (s.startsWith(sign)) { midi += step; s = s.substring(sign.length); break }
        }
        val octave = s.toIntOrNull() ?: return null
        midi += (octave + 1) * 12
        return midi.takeIf { it in 0..127 }
    }

    /** Frequency in Hz, A4 (69) being 440. */
    fun freq(midi: Int) = 440 * 2.0.pow((midi - 69) / 12.0)
}
