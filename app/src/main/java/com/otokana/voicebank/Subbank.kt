package com.otokana.voicebank

import com.otokana.core.Notes

/**
 * A set of a multi-pitch bank's aliases (prefix + kana + suffix), sung in
 * its [tones] with its voice [color], as OpenUtau reads them from
 * character.yaml, or prefix.map without one.
 */
data class Subbank(
    val color: String = "",
    val prefix: String = "",
    val suffix: String = "",
    /** MIDI ranges, both ends in; none for every tone. */
    val tones: List<IntRange> = emptyList(),
) {
    fun sings(midi: Int) = tones.isEmpty() || tones.any { midi in it }
}

internal object Subbanks {
    /** A tone range of character.yaml, as C1-B3, or one note, as G4. */
    fun toneRange(s0: String): IntRange? {
        val s = s0.trim()
        Notes.parse(s)?.let { return it..it }
        // Notes have - only for octave -1, so try each - as the separator.
        for (i in 1 until s.length) {
            if (s[i] != '-') continue
            val lo = Notes.parse(s.substring(0, i)) ?: continue
            val hi = Notes.parse(s.substring(i + 1)) ?: continue
            return lo..hi
        }
        return null
    }

    private fun unquote(s: String) = s.trim().trim('"', '\'')

    /** The subbanks list of a character.yaml: color, prefix, suffix, tone_ranges. */
    fun parseYaml(text: String): List<Subbank> {
        val out = mutableListOf<Subbank>()
        var inList = false
        var inTones = false
        for (line in lines(text)) {
            var t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) continue
            val top = !line.startsWith(" ") && !line.startsWith("\t") && !t.startsWith("-")
            if (top) { inList = t.startsWith("subbanks:"); continue }
            if (!inList) continue
            val item = t.startsWith("-")
            if (item) t = t.substring(1).trim()
            val colon = t.indexOf(':')
            if (item && colon < 0) {
                if (inTones && out.isNotEmpty()) toneRange(unquote(t))?.let {
                    out[out.size - 1] = out.last().copy(tones = out.last().tones + listOf(it))
                }
                continue
            }
            if (item) out += Subbank()
            if (out.isEmpty()) continue
            val k = if (colon < 0) t else t.substring(0, colon)
            val v = if (colon < 0) "" else unquote(t.substring(colon + 1))
            inTones = false
            val s = out.last()
            out[out.size - 1] = when (k.trim()) {
                "color" -> s.copy(color = v)
                "prefix" -> s.copy(prefix = v)
                "suffix" -> s.copy(suffix = v)
                "tone_ranges" -> {
                    inTones = true
                    // A flow list, [C1-B3, C4-D#4], on the same line.
                    s.copy(tones = s.tones + v.trim('[', ']').split(',').mapNotNull { toneRange(unquote(it)) })
                }
                else -> s
            }
        }
        return out
    }

    /**
     * A prefix.map: lines of note, prefix and suffix split by tabs, into
     * subbanks of [color], one per prefix and suffix, its notes in ranges.
     */
    fun parsePrefixMap(text: String, color: String): List<Subbank> {
        val tones = LinkedHashMap<Pair<String, String>, MutableList<Int>>()
        for (line in lines(text)) {
            val f = line.trimEnd('\r', '\n').split('\t')
            if (f.size < 3) continue
            val n = Notes.parse(f[0].trim()) ?: continue
            val p = f[1].trim() to color + f[2].trim()
            if (p.first.isEmpty() && p.second.isEmpty()) continue // the plain aliases
            tones.getOrPut(p) { mutableListOf() } += n
        }
        return tones.map { (p, ns) ->
            val ranges = mutableListOf<IntRange>()
            for (n in ns.sorted()) {
                val last = ranges.lastOrNull()
                if (last != null && last.last == n - 1) ranges[ranges.size - 1] = last.first..n
                else ranges += n..n
            }
            Subbank(color, p.first, p.second, ranges)
        }
    }

    /** [root]'s prefix.map, of no color, and those in its prefix folder, of the color their names say. */
    fun readPrefixMaps(src: Source, root: String): List<Subbank> {
        val out = mutableListOf<Subbank>()
        src.find(P.join(root, "prefix.map"))?.let { out += parsePrefixMap(decode(src.readFile(it)), "") }
        val colored = src.files.filter {
            P.dir(it).equals(P.join(root, "prefix"), ignoreCase = true) && P.ext(it).equals(".map", ignoreCase = true)
        }.sorted()
        for (f in colored) out += parsePrefixMap(decode(src.readFile(f)), P.base(f).removeSuffix(P.ext(f)))
        return out
    }
}
