package com.otokana.core

import kotlin.random.Random

enum class Phase { Setup, Playing, Results }

/**
 * A game, from Setup through Results. Times are milliseconds of one clock
 * the caller gives (SystemClock.uptimeMillis in the app).
 *
 * Plain Kotlin: the view model copies what it shows into Compose state.
 */
class Session(var shuffle: (Int) -> Int = { Random.nextInt(it) }) {
    var phase = Phase.Setup; private set
    var settings = DeckSettings()
    var deck: List<Card> = emptyList(); private set
    var input = ""; private set
    /** The active card shows its romaji. */
    var hint = false; private set
    var started = 0L; private set
    var lockUntil = 0L; private set
    var ended = 0L; private set

    fun canStart() = Deck.build(settings).isNotEmpty()

    /** Builds and shuffles the deck and plays it from [now]; nothing when empty. */
    fun start(now: Long) {
        val cards = Deck.build(settings).toMutableList()
        if (cards.isEmpty()) return
        Deck.shuffle(cards, shuffle)
        deck = cards.map { it.copy(pitch = shuffle(2 * PITCH_RANGE + 1) - PITCH_RANGE) }
        phase = Phase.Playing
        input = ""; hint = false
        started = now; lockUntil = 0; ended = 0
    }

    private fun activeIndex() = deck.indexOfLast { !it.done }

    /** The last card not done, or null. */
    fun active(): Card? = deck.getOrNull(activeIndex())

    fun remaining() = deck.count { !it.done }
    fun hinted() = deck.count { it.hinted }

    fun elapsed(now: Long): Long = when (phase) {
        Phase.Playing -> now - started
        Phase.Results -> ended - started
        Phase.Setup -> 0
    }

    fun result(): MatchResult = active()?.let { match(input, it.entry.aliases) } ?: MatchResult.Pending

    /** What [type] did. */
    data class Typed(val done: Card?, val wrong: Boolean)

    /**
     * Adds the letters a–z of [text], lower-cased, judging after each: a
     * correct answer finishes the card, clears and locks the input; a wrong
     * one keeps it. Text typed during the lock is dropped.
     */
    fun type(text: String, now: Long): Typed {
        var done: Card? = null
        var wrong = false
        for (ch in text) {
            if (phase != Phase.Playing || now < lockUntil) break
            val r = ch.lowercaseChar()
            if (r !in 'a'..'z') continue
            input += r
            when (result()) {
                MatchResult.Correct -> {
                    val i = activeIndex()
                    val c = deck[i].copy(done = true)
                    deck = deck.toMutableList().also { it[i] = c }
                    done = c
                    input = ""; hint = false
                    lockUntil = now + LOCK_MS
                    if (active() == null) { phase = Phase.Results; ended = now }
                }
                MatchResult.Wrong -> wrong = true
                MatchResult.Pending -> {}
            }
        }
        return Typed(done, wrong)
    }

    fun backspace() { input = input.dropLast(1) }
    fun clear() { input = "" }

    /** Shows the active card's romaji until answered, and marks it hinted. */
    fun showHint() {
        val i = activeIndex()
        if (phase != Phase.Playing || i < 0) return
        hint = true
        deck = deck.toMutableList().also { it[i] = it[i].copy(hinted = true) }
    }

    /** Ends a game without results, back to Setup. */
    fun stop() { phase = Phase.Setup; deck = emptyList(); input = ""; hint = false }

    fun playAgain() = stop()

    companion object {
        const val LOCK_MS = 150L
        /** Semitones at most a card plays from the base note. */
        const val PITCH_RANGE = 3
    }
}
