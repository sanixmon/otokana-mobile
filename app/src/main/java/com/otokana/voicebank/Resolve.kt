package com.otokana.voicebank

import com.otokana.core.Entry
import com.otokana.core.KanaTable

/**
 * The oto entry a kana plays. [from] is the romaji of the kana whose alias
 * it is: the kana's own, or for ぢ, づ, ぢゃ, ぢゅ, ぢょ that of じ, ず, じゃ, じゅ, じょ.
 */
data class Resolved(val entry: OtoEntry, val from: String) {
    fun own(romaji: String) = from == romaji
}

object Resolver {
    private val standIns = mapOf("di" to "ji", "du" to "zu", "dya" to "ja", "dyu" to "ju", "dyo" to "jo")

    /**
     * The alias without a multi-pitch suffix, from its first capital, digit or
     * _ on: ka_DKC4 and - あCA3 give ka and - あ.
     */
    fun unsuffixed(alias: String): String {
        val i = alias.indexOfFirst { it in 'A'..'Z' || it in '0'..'9' || it == '_' }
        return if (i > 0) alias.substring(0, i) else alias
    }

    /**
     * The samples of the table's kana, by romaji: the first entry aliased as
     * the hiragana, else the romaji, else "- " + hiragana, else any of these
     * with a pitch suffix, else a stand-in. With [sb], only its aliases
     * (prefix + kana + suffix).
     */
    fun resolve(vb: ReadBank, sb: Subbank?): Map<String, Resolved> {
        val byAlias = HashMap<String, OtoEntry>()
        val bySuffixed = HashMap<String, OtoEntry>()
        for (e in vb.entries) {
            byAlias.putIfAbsent(e.alias, e)
            val base = unsuffixed(e.alias)
            if (base != e.alias) bySuffixed.putIfAbsent(base, e)
        }
        fun own(k: Entry): OtoEntry? {
            val maps = if (sb != null) listOf(byAlias) else listOf(byAlias, bySuffixed)
            for (m in maps) for (a in listOf(k.hiragana, k.romaji, "- " + k.hiragana)) {
                val alias = if (sb != null) sb.prefix + a + sb.suffix else a
                m[alias]?.let { return it }
            }
            return null
        }
        val out = LinkedHashMap<String, Resolved>()
        for (k in KanaTable) own(k)?.let { out[k.romaji] = Resolved(it, k.romaji) }
        for ((romaji, from) in standIns) if (romaji !in out) out[from]?.let { out[romaji] = it }
        return out
    }
}
