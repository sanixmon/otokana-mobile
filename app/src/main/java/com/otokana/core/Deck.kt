package com.otokana.core

/** The five choices of Setup. */
data class DeckSettings(
    val hiragana: Boolean = true,
    val katakana: Boolean = false,
    val monographs: Boolean = true,
    val diacritics: Boolean = false,
    val digraphs: Boolean = false,
) {
    fun scripts(): List<Script> = buildList {
        if (hiragana) add(Script.Hiragana)
        if (katakana) add(Script.Katakana)
    }

    /** Digraphs + diacritics come only with both diacritics and digraphs. */
    fun groups(): List<Group> = buildList {
        if (monographs) add(Group.Mono)
        if (diacritics) add(Group.Dia)
        if (digraphs) add(Group.Di)
        if (diacritics && digraphs) add(Group.DiDia)
    }
}

/**
 * One kana in one script. [pitch] is how many semitones above the voicebank's
 * base note it plays (below when negative).
 */
data class Card(
    val id: String,
    val entry: Entry,
    val script: Script,
    val order: Int,
    val pitch: Int = 0,
    val done: Boolean = false,
    val hinted: Boolean = false,
) {
    val kana get() = entry.kana(script)
}

object Deck {
    /** The cards of [s], unshuffled; empty without a script or a variant. */
    fun build(s: DeckSettings): List<Card> {
        val scripts = s.scripts()
        val out = mutableListOf<Card>()
        for (g in s.groups()) for (e in KanaTable) {
            if (e.group != g) continue
            for (sc in scripts) out += Card("${sc.id}:${e.romaji}", e, sc, out.size)
        }
        return out
    }

    /** Fisher–Yates, from the last element back; [intN] returns [0, n). */
    fun <T> shuffle(cards: MutableList<T>, intN: (Int) -> Int) {
        for (i in cards.indices.reversed()) {
            if (i == 0) break
            val j = intN(i + 1)
            val t = cards[i]; cards[i] = cards[j]; cards[j] = t
        }
    }
}
