package com.otokana

import com.otokana.audio.Sound
import com.otokana.audio.Wav
import com.otokana.store.PlayOn
import com.otokana.store.Settings
import com.otokana.store.Store
import com.otokana.voicebank.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class StoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun bank(dir: File) {
        dir.mkdirs()
        val wav = Wav.write(Sound(22050, ShortArray(22050) { (it % 100 * 50).toShort() }))
        File(dir, "a.wav").writeBytes(wav)
        File(dir, "ka.wav").writeBytes(wav)
        File(dir, "ji.wav").writeBytes(wav)
        // Shift-JIS oto.ini, as many banks ship it.
        File(dir, "oto.ini").writeBytes("a.wav=あ,100,50,-200,0,0\r\nka.wav=か,0,0,0\r\nji.wav=じ,10,0,500\r\n".toByteArray(charset("Shift_JIS")))
        File(dir, "character.txt").writeText("name=Test Singer\nauthor=Someone\n")
    }

    @Test fun settingsRoundTrip() {
        val st = Store(tmp.newFolder("data"))
        assertEquals(Settings(), st.settings())
        st.saveSettings(Settings("abc", PlayOn.OnBoth))
        assertEquals(Settings("abc", PlayOn.OnBoth), st.settings())
        st.saveSettings(Settings("abc", PlayOn.OnCorrect))
        assertEquals(PlayOn.OnCorrect, st.settings().playOn)
    }

    @Test fun importFolder() {
        val src = tmp.newFolder("bank")
        bank(src)
        val st = Store(tmp.newFolder("data"))
        var last = 0 to 0
        val vbs = st.import(Source.dir(src)) { d, t -> last = d to t }
        assertEquals(1, vbs.size)
        val v = vbs[0]
        assertEquals("Test Singer", v.name)
        assertEquals("Someone", v.credit)
        assertEquals(3 to 3, last)
        val a = v.sample("a")!!
        assertEquals(200.0, a.duration, 0.0)
        assertEquals(44100 * 200 / 1000, Wav.read(File(v.dir, a.file).readBytes()).samples.size)
        assertTrue(v.sample("di")!!.from == "ji") // stand-in
        assertEquals(3 to 4, v.own())

        st.saveSettings(Settings(v.id))
        val active = st.active()
        assertNotNull(active)
        st.save(active!!.copy(base = "E4").also { it.dir = active.dir })
        // Imported again, it keeps its base note.
        val again = st.import(Source.dir(src)) { _, _ -> }[0]
        assertEquals("E4", again.base)
        assertEquals(1, st.voicebanks().size)
        st.delete(v.id)
        assertEquals(0, st.voicebanks().size)
        assertEquals("", st.settings().voicebank)
    }

    @Test fun importZipOfTwoBanks() {
        val root = tmp.newFolder("zipsrc")
        bank(File(root, "one"))
        bank(File(root, "two"))
        File(root, "two/character.txt").writeText("name=Second\n")
        val zip = tmp.newFile("banks.zip")
        ZipOutputStream(zip.outputStream()).use { z ->
            root.walkTopDown().filter { it.isFile }.forEach {
                z.putNextEntry(ZipEntry(it.relativeTo(root).invariantSeparatorsPath)); z.write(it.readBytes()); z.closeEntry()
            }
        }
        val st = Store(tmp.newFolder("data"))
        val vbs = Source.zip(zip, "banks.zip").use { st.import(it) { _, _ -> } }
        assertEquals(listOf("Test Singer", "Second").sorted(), vbs.map { it.name }.sorted())
    }
}
