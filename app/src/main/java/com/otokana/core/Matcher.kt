package com.otokana.core

/** How typed input compares with a card's answers. */
enum class MatchResult { Correct, Pending, Wrong }

/** Compares [input] with [aliases], Correct before Pending. */
fun match(input: String, aliases: List<String>): MatchResult = when {
    aliases.any { it == input } -> MatchResult.Correct
    aliases.any { it.startsWith(input) } -> MatchResult.Pending
    else -> MatchResult.Wrong
}
