package com.otokana.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.roundToInt

/** Rate sounds play at, which imports resample to. */
const val RATE = 44100

/** Silence between sounds played one after another. */
const val GAP_MS = 150

/** Fade in and out, against clicks. */
const val FADE_MS = 5

/** Mono 16-bit PCM. */
class Sound(val rate: Int, val samples: ShortArray) {
    val ms: Double get() = if (rate <= 0) 0.0 else samples.size * 1000.0 / rate

    /** The part [lengthMs] long from [fromMs], within the sound. */
    fun segment(fromMs: Double, lengthMs: Double): Sound {
        fun at(ms: Double) = (ms * rate / 1000).roundToInt().coerceIn(0, samples.size)
        val from = at(fromMs)
        val to = maxOf(at(fromMs + lengthMs), from)
        return Sound(rate, samples.copyOfRange(from, to))
    }

    /** The same sound at [to] samples a second, linearly interpolated. */
    fun resample(to: Int): Sound {
        if (to == rate || samples.isEmpty()) return Sound(to, samples.copyOf())
        val n = (samples.size.toLong() * to / rate).toInt()
        val out = ShortArray(n)
        val step = rate.toDouble() / to
        val last = samples.size - 1
        for (i in 0 until n) {
            val pos = i * step
            val j = pos.toInt()
            if (j >= last) { out[i] = samples[last]; continue }
            val f = pos - j
            out[i] = (samples[j] * (1 - f) + samples[j + 1] * f).roundToInt().toShort()
        }
        return Sound(to, out)
    }

    /** [semitones] higher (lower when negative), and shorter or longer as much. */
    fun shift(semitones: Int): Sound {
        if (semitones == 0) return this
        val r = resample((rate / 2.0.pow(semitones / 12.0)).roundToInt())
        return Sound(rate, r.samples)
    }

    fun faded(): ShortArray {
        val out = samples.copyOf()
        val n = minOf(rate * FADE_MS / 1000, out.size / 2)
        for (i in 0 until n) {
            val g = i.toDouble() / n
            out[i] = (out[i] * g).toInt().toShort()
            out[out.size - 1 - i] = (out[out.size - 1 - i] * g).toInt().toShort()
        }
        return out
    }

    companion object {
        /** The sounds at [RATE], faded, one after another, [GAP_MS] apart. */
        fun join(sounds: List<Sound?>): ShortArray {
            val parts = mutableListOf<ShortArray>()
            for (s0 in sounds) {
                if (s0 == null || s0.samples.isEmpty()) continue
                val s = if (s0.rate != RATE) s0.resample(RATE) else s0
                if (parts.isNotEmpty()) parts += ShortArray(RATE * GAP_MS / 1000)
                parts += s.faded()
            }
            val out = ShortArray(parts.sumOf { it.size })
            var p = 0
            for (a in parts) { a.copyInto(out, p); p += a.size }
            return out
        }
    }
}

class WavException(msg: String) : Exception(msg)

object Wav {
    private const val PCM = 1
    private const val FLOAT = 3
    private const val EXTENSIBLE = 0xFFFE

    /**
     * Reads a RIFF WAV of 16-bit integer PCM, mono or stereo (mixed to mono),
     * at any rate. A data chunk cut short is read as far as it goes.
     */
    fun read(b: ByteArray): Sound {
        if (b.size < 12 || String(b, 0, 4, Charsets.US_ASCII) != "RIFF" || String(b, 8, 4, Charsets.US_ASCII) != "WAVE")
            throw WavException("not a WAV file")
        val le = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        var channels = 0
        var haveFmt = false
        var rate = 0
        var p = 12
        while (p + 8 <= b.size) {
            val id = String(b, p, 4, Charsets.US_ASCII)
            val size = le.getInt(p + 4).toLong() and 0xFFFFFFFFL
            val start = p + 8
            val end = minOf(start + size, b.size.toLong()).toInt()
            when (id) {
                "fmt " -> {
                    if (end - start < 16) throw WavException("not a WAV file")
                    var format = le.getShort(start).toInt() and 0xFFFF
                    channels = le.getShort(start + 2).toInt() and 0xFFFF
                    rate = le.getInt(start + 4)
                    val bits = le.getShort(start + 14).toInt() and 0xFFFF
                    if (format == EXTENSIBLE && end - start >= 26) format = le.getShort(start + 24).toInt() and 0xFFFF
                    when {
                        format == FLOAT -> throw WavException("$bits-bit float WAV is not supported, only 16-bit PCM")
                        format != PCM -> throw WavException("WAV format 0x%x is not supported, only 16-bit PCM".format(format))
                        bits != 16 -> throw WavException("$bits-bit WAV is not supported, only 16-bit PCM")
                        channels != 1 && channels != 2 -> throw WavException("$channels-channel WAV is not supported, only mono or stereo")
                        rate <= 0 -> throw WavException("not a WAV file")
                    }
                    haveFmt = true
                }
                "data" -> {
                    if (!haveFmt) throw WavException("not a WAV file")
                    val frames = (end - start) / (2 * channels)
                    val out = ShortArray(frames)
                    for (i in 0 until frames) {
                        val at = start + 2 * channels * i
                        var l = le.getShort(at).toInt()
                        if (channels == 2) l = (l + le.getShort(at + 2)) / 2
                        out[i] = l.toShort()
                    }
                    return Sound(rate, out)
                }
            }
            val next = start + size + size % 2 // chunks are padded to even sizes
            if (next > Int.MAX_VALUE) break
            p = next.toInt()
        }
        throw WavException("not a WAV file: no audio data")
    }

    /** 16-bit mono PCM WAV, as [read] reads. */
    fun write(s: Sound): ByteArray {
        val data = s.samples.size * 2
        val buf = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()).putInt(36 + data).put("WAVEfmt ".toByteArray())
        buf.putInt(16).putShort(PCM.toShort()).putShort(1).putInt(s.rate).putInt(2 * s.rate)
        buf.putShort(2).putShort(16).put("data".toByteArray()).putInt(data)
        for (v in s.samples) buf.putShort(v)
        return buf.array()
    }
}
