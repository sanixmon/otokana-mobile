package com.otokana

import com.otokana.audio.Sound
import com.otokana.audio.Wav
import com.otokana.core.Deck
import com.otokana.core.DeckSettings
import com.otokana.core.Format
import com.otokana.core.KanaTable
import com.otokana.core.MatchResult
import com.otokana.core.Notes
import com.otokana.core.Phase
import com.otokana.core.Session
import com.otokana.core.match
import com.otokana.voicebank.Oto
import com.otokana.voicebank.ReadBank
import com.otokana.voicebank.Resolver
import com.otokana.voicebank.Subbanks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    @Test fun table() {
        assertEquals(107, KanaTable.size)
        assertEquals(KanaTable.size, KanaTable.map { it.romaji }.toSet().size)
        assertTrue(KanaTable.all { it.romaji in it.aliases })
    }

    @Test fun deckSizes() {
        assertEquals(46, Deck.build(DeckSettings()).size)
        assertEquals(0, Deck.build(DeckSettings(hiragana = false)).size)
        val all = DeckSettings(katakana = true, diacritics = true, digraphs = true)
        assertEquals(214, Deck.build(all).size)
        assertEquals(21, Deck.build(DeckSettings(monographs = false, digraphs = true)).size)
    }

    @Test fun matcher() {
        assertEquals(MatchResult.Correct, match("shi", listOf("shi", "si")))
        assertEquals(MatchResult.Pending, match("sh", listOf("shi", "si")))
        assertEquals(MatchResult.Wrong, match("sx", listOf("shi", "si")))
    }

    @Test fun session() {
        val s = Session(shuffle = { 0 })
        s.start(1000)
        assertEquals(Phase.Playing, s.phase)
        val first = s.active()!!
        val r = s.type(first.entry.romaji.uppercase(), 1000)
        assertEquals(first.id, r.done!!.id)
        assertEquals("", s.input)
        // Typing during the lock is dropped.
        s.type("a", 1100)
        assertEquals("", s.input)
        s.showHint()
        assertEquals(1, s.hinted())
        while (s.phase == Phase.Playing) {
            val c = s.active()!!
            s.type(c.entry.romaji, s.lockUntil)
        }
        assertEquals(Phase.Results, s.phase)
        assertEquals(0, s.remaining())
        assertTrue(s.deck.all { it.pitch in -3..3 })
    }

    @Test fun wrongKeepsInput() {
        val s = Session(shuffle = { 0 })
        s.start(0)
        val r = s.type("q", 0)
        assertTrue(r.wrong)
        assertEquals("q", s.input)
        s.backspace()
        assertEquals("", s.input)
    }

    @Test fun notes() {
        assertEquals("E4", Notes.name(64))
        assertEquals("C#4", Notes.name(61))
        assertEquals("C-1", Notes.name(0))
        assertEquals("B-2", Notes.name(-1))
        assertEquals(64, Notes.parse("e4"))
        assertEquals(66, Notes.parse("F#4"))
        assertEquals(63, Notes.parse("E♭4"))
        assertEquals(0, Notes.parse("C-1"))
        assertNull(Notes.parse("H4"))
        assertEquals(127, Notes.parse("G9"))
        assertNull(Notes.parse("G#9"))
    }

    @Test fun format() {
        assertEquals("02:48", Format.clock(168_000))
        assertEquals("1:00:05", Format.clock(3_605_000))
        assertEquals("2 min 48 s", Format.duration(168_000))
        assertEquals("0 s", Format.duration(0))
        assertEquals(67, Format.accuracy(3, 1))
        assertEquals("Hiragana basic", Format.deckName(DeckSettings()))
        assertEquals(
            "Hiragana and katakana basic, dakuten, and yōon",
            Format.deckName(DeckSettings(katakana = true, diacritics = true, digraphs = true)),
        )
    }

    @Test fun oto() {
        val (es, bad) = Oto.parse("a.wav=あ,10,20,-300,5,1\n# x\nbroken\nka.wav=,1,2,3\nki.wav=き,x\n", "dir")
        assertEquals(1 + 1 + 0, es.size)
        assertEquals(2, bad)
        assertEquals("dir/a.wav", es[0].file)
        assertEquals("ka", es[1].alias)
        assertEquals(300.0, es[0].duration(1000.0), 0.0)
        assertEquals(1000.0 - 1 - 3, es[1].duration(1000.0), 0.0)
    }

    @Test fun resolve() {
        val entries = Oto.parse("a.wav=あ\nka.wav=ka_C4\nji.wav=- じ\n", "").first
        val vb = ReadBank("t", "", 1, 0, entries, emptyList(), "")
        val r = Resolver.resolve(vb, null)
        assertEquals("あ", r["a"]!!.entry.alias)
        assertEquals("ka_C4", r["ka"]!!.entry.alias)
        assertEquals("ji", r["di"]!!.from) // ぢ plays じ
        assertTrue(r["ji"]!!.own("ji"))
    }

    @Test fun subbanks() {
        val y = """
            name: Test
            subbanks:
              - color: soft
                prefix: ""
                suffix: S
                tone_ranges:
                  - C1-B3
                  - C4
              - color: strong
                tone_ranges: [C5-G5]
        """.trimIndent()
        val sb = Subbanks.parseYaml(y)
        assertEquals(2, sb.size)
        assertEquals("soft", sb[0].color)
        assertEquals("S", sb[0].suffix)
        assertEquals(listOf(24..59, 60..60), sb[0].tones)
        assertEquals(listOf(72..79), sb[1].tones)
        val pm = Subbanks.parsePrefixMap("C4\t\t_L\nC#4\t\t_L\nD4\t\t\nE4\t\t_H\n", "")
        assertEquals(listOf(60..61), pm[0].tones)
        assertEquals("_H", pm[1].suffix)
    }

    @Test fun wavRoundTrip() {
        val s = Sound(22050, ShortArray(2205) { (it * 7).toShort() })
        val back = Wav.read(Wav.write(s))
        assertEquals(22050, back.rate)
        assertEquals(s.samples.toList(), back.samples.toList())
        assertEquals(4410, s.resample(44100).samples.size)
        assertEquals(100.0, s.ms, 0.001)
        assertEquals(1103, s.segment(25.0, 50.0).samples.size)
        assertTrue(s.shift(12).samples.size in 1100..1105)
    }
}
