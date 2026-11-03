package com.otokana.voicebank

/**
 * A line of an oto.ini: a sample, in milliseconds of its file. [cutoff] is
 * where it ends: that far before the end of the file when positive, its
 * length from [offset] when negative.
 */
data class OtoEntry(
    val alias: String,
    val file: String, // slash path of the wav in the source
    val offset: Double = 0.0,
    val consonant: Double = 0.0,
    val cutoff: Double = 0.0,
    val preutterance: Double = 0.0,
    val overlap: Double = 0.0,
) {
    /** How long the sample plays in a file [fileMs] long, within the file. */
    fun duration(fileMs: Double): Double {
        val left = maxOf(fileMs - maxOf(offset, 0.0), 0.0)
        val d = if (cutoff < 0) -cutoff else left - cutoff
        return minOf(maxOf(d, 0.0), left)
    }
}

object Oto {
    /**
     * Reads the lines of an oto.ini, file.wav=alias,offset,consonant,cutoff,
     * preutterance,overlap, whose files are in [dir]. An empty alias is the
     * file's name without extension; missing numbers are 0. Returns the
     * entries and how many lines could not be read.
     */
    fun parse(text: String, dir: String): Pair<List<OtoEntry>, Int> {
        val out = mutableListOf<OtoEntry>()
        var bad = 0
        for (raw in lines(text)) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val eq = line.indexOf('=')
            val wav = (if (eq < 0) line else line.substring(0, eq)).replace('\\', '/').trim()
            if (eq < 0 || wav.isEmpty()) { bad++; continue }
            val fields = line.substring(eq + 1).split(',')
            var alias = fields[0].trim()
            if (alias.isEmpty()) alias = P.base(wav).removeSuffix(P.ext(wav))
            val nums = DoubleArray(5)
            var ok = true
            for (i in 0 until 5) {
                if (i + 1 >= fields.size) break
                val s = fields[i + 1].trim()
                if (s.isEmpty()) continue
                val v = s.toDoubleOrNull()
                if (v == null) { ok = false; break }
                nums[i] = v
            }
            if (!ok) { bad++; continue }
            out += OtoEntry(alias, P.join(dir, wav), nums[0], nums[1], nums[2], nums[3], nums[4])
        }
        return out to bad
    }

    data class Character(val name: String, val author: String, val web: String, val image: String)

    /**
     * The name, author, web site and image of a character.txt (key=value or
     * key:value), or of a character.yaml (key: value only, [yaml] true).
     */
    fun parseCharacter(text: String, yaml: Boolean): Character {
        var name = ""; var author = ""; var web = ""; var image = ""
        val seps = if (yaml) charArrayOf(':') else charArrayOf('=', ':')
        for (line in lines(text)) {
            val i = line.indexOfAny(seps)
            if (i < 0 || line.startsWith(" ") || line.startsWith("\t")) continue
            val v = line.substring(i + 1).trim().trim('"', '\'')
            when (line.substring(0, i).trim().lowercase()) {
                "name" -> name = v
                "author" -> author = v
                "image" -> image = v
                "web" -> web = v
            }
        }
        return Character(name, author, web, image)
    }
}
