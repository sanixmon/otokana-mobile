package com.otokana.core

import kotlin.math.roundToInt

object Format {
    private fun split(ms: Long): Triple<Long, Long, Long> {
        val sec = maxOf(ms, 0) / 1000
        return Triple(sec / 3600, sec / 60 % 60, sec % 60)
    }

    /** MM:SS, or H:MM:SS from an hour on. */
    fun clock(ms: Long): String {
        val (h, m, s) = split(ms)
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    /** "2 min 48 s", leaving out zero parts. */
    fun duration(ms: Long): String {
        val (h, m, s) = split(ms)
        val parts = buildList {
            if (h > 0) add("$h h")
            if (m > 0) add("$m min")
            if (s > 0 || isEmpty()) add("$s s")
        }
        return parts.joinToString(" ")
    }

    /** Percentage of cards answered without a hint, rounded. */
    fun accuracy(cards: Int, hinted: Int): Int =
        if (cards <= 0) 0 else (100.0 * (cards - hinted) / cards).roundToInt()

    /** Names the deck, as "Hiragana basic". */
    fun deckName(s: DeckSettings): String {
        val scripts = s.scripts().map { it.id }
        val variants = listOf(s.monographs to "basic", s.diacritics to "dakuten", s.digraphs to "yōon")
            .filter { it.first }.map { it.second }
        val name = list(scripts) + " " + list(variants)
        return name.replaceFirstChar { it.uppercase() }
    }

    private fun list(w: List<String>): String = when (w.size) {
        0 -> ""
        1 -> w[0]
        2 -> "${w[0]} and ${w[1]}"
        else -> w.dropLast(1).joinToString(", ") + ", and " + w.last()
    }
}
