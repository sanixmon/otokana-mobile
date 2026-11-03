package com.otokana.voicebank

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

private val shiftJis: Charset = runCatching { Charset.forName("windows-31j") }.getOrElse { Charset.forName("Shift_JIS") }

/** Whether [b] is valid UTF-8. */
fun isUtf8(b: ByteArray): Boolean = runCatching {
    Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(b))
}.isSuccess

/**
 * [b] as text: UTF-8 when valid (without its BOM), else decoded from
 * Shift-JIS, as many Japanese voicebanks have it.
 */
fun decode(b0: ByteArray): String {
    val b = if (b0.size >= 3 && b0[0] == 0xEF.toByte() && b0[1] == 0xBB.toByte() && b0[2] == 0xBF.toByte())
        b0.copyOfRange(3, b0.size) else b0
    return if (isUtf8(b)) String(b, Charsets.UTF_8) else String(b, shiftJis)
}

/** Lines with their endings kept, as Go's strings.Lines. */
internal fun lines(text: String): Sequence<String> = sequence {
    var start = 0
    while (start < text.length) {
        val nl = text.indexOf('\n', start)
        if (nl < 0) { yield(text.substring(start)); break }
        yield(text.substring(start, nl + 1)); start = nl + 1
    }
}

/** Slash path helpers, as Go's package path. */
internal object P {
    fun clean(p: String): String {
        val out = ArrayList<String>()
        for (part in p.split('/')) when (part) {
            "", "." -> {}
            ".." -> if (out.isNotEmpty() && out.last() != "..") out.removeAt(out.size - 1) else if (!p.startsWith("/")) out += ".."
            else -> out += part
        }
        val s = out.joinToString("/")
        return if (p.startsWith("/")) "/$s" else s.ifEmpty { "." }
    }
    fun join(vararg parts: String): String {
        val nonEmpty = parts.filter { it.isNotEmpty() }
        return if (nonEmpty.isEmpty()) "" else clean(nonEmpty.joinToString("/"))
    }
    fun dir(p: String): String { val i = p.lastIndexOf('/'); return if (i < 0) "." else clean(p.substring(0, i).ifEmpty { "/" }) }
    fun base(p: String): String { val s = p.trimEnd('/'); return s.substring(s.lastIndexOf('/') + 1).ifEmpty { if (p.isEmpty()) "." else "/" } }
    fun ext(p: String): String { val b = base(p); val i = b.lastIndexOf('.'); return if (i < 0) "" else b.substring(i) }
}
