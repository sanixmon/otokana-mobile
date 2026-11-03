package com.otokana.store

import com.otokana.audio.RATE
import com.otokana.audio.Sound
import com.otokana.audio.Wav
import com.otokana.core.KanaTable
import com.otokana.core.Notes
import com.otokana.voicebank.OtoEntry
import com.otokana.voicebank.Resolved
import com.otokana.voicebank.Resolver
import com.otokana.voicebank.Source
import com.otokana.voicebank.Subbank as ReadSubbank
import com.otokana.voicebank.VoicebankReader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.io.File

/** When cards play their sounds. */
@Serializable
enum class PlayOn {
    @SerialName("") OnCorrect,
    @SerialName("show") OnShow,
    @SerialName("both") OnBoth,
}

/** settings.json: the active voicebank, and what the Settings screen sets. */
@Serializable
data class Settings(
    val voicebank: String = "",
    val playOn: PlayOn = PlayOn.OnCorrect,
)

/** A session played to its end, in history.json. */
@Serializable
data class Record(
    val date: String, // ISO 8601, when it ended
    val deck: String,
    val cards: Int,
    val hinted: Int,
    val ms: Long,
    val accuracy: Int,
)

/** Where a kana's sound comes from, and its file in the voicebank's folder. */
@Serializable
data class Sample(
    val alias: String,
    val source: String,
    val offset: Double,
    val duration: Double,
    val from: String = "",
    val file: String = "",
) {
    fun own(romaji: String) = from == romaji
}

/** The samples of a subbank, sung in its tones (MIDI ranges). */
@Serializable
data class Subbank(
    val color: String = "",
    val tones: List<List<Int>> = emptyList(),
    val samples: Map<String, Sample> = emptyMap(),
) {
    fun sings(midi: Int) = tones.isEmpty() || tones.any { it.size == 2 && midi in it[0]..it[1] }
}

/** An imported voicebank: its samples, cut from its files, and voicebank.json about them. */
@Serializable
data class Voicebank(
    val id: String,
    val name: String,
    val credit: String = "",
    val image: String = "",
    val samples: Map<String, Sample> = emptyMap(),
    val subbanks: List<Subbank> = emptyList(),
    val colors: List<String> = emptyList(),
    val color: String = "",
    /** The note the samples are sung at, as E4; "" when not known. */
    val base: String = "",
    val skipped: List<String> = emptyList(),
) {
    @Transient var dir: File? = null

    /**
     * The sample the kana plays, as OpenUtau picks a note's: that of the first
     * subbank of the chosen color sung in the tone of the base note (C4
     * without one), else of the first of no color in it, else the plain one.
     */
    fun sample(romaji: String): Sample? {
        val tone = Notes.parse(base) ?: 60
        for (c in listOf(color, "")) {
            val sb = subbanks.firstOrNull { it.color == c && it.sings(tone) }
            sb?.samples?.get(romaji)?.let { return it }
        }
        return samples[romaji]
    }

    /** How many kana have samples of their own, and how many have any. */
    fun own(): Pair<Int, Int> {
        var own = 0; var sounding = 0
        for (k in KanaTable) sample(k.romaji)?.let { sounding++; if (it.own(k.romaji)) own++ }
        return own to sounding
    }

    fun imageFile(): File? = if (image.isEmpty()) null else dir?.let { File(it, image) }?.takeIf { it.exists() }
}

class NoSamplesException(msg: String = "no oto.ini alias matches a kana") : Exception(msg)

/** Keeps the app's data in [dir]: settings.json, history.json, voicebanks/<id>/. */
class Store(private val dir: File) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = false }

    private inline fun <reified T> read(f: File, default: T): T =
        if (!f.exists()) default else json.decodeFromString(f.readText())

    private inline fun <reified T> write(f: File, v: T) {
        f.parentFile?.mkdirs()
        val tmp = File(f.path + ".tmp")
        tmp.writeText(json.encodeToString(v))
        if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
    }

    fun settings(): Settings = read(File(dir, "settings.json"), Settings())
    fun saveSettings(s: Settings) = write(File(dir, "settings.json"), s)

    fun addRecord(r: Record) {
        val f = File(dir, "history.json")
        write(f, read(f, emptyList<Record>()) + r)
    }

    fun history(): List<Record> = runCatching { read(File(dir, "history.json"), emptyList<Record>()) }.getOrDefault(emptyList())

    private fun bankDir(id: String) = File(dir, "voicebanks/$id")

    private fun load(d: File): Voicebank? =
        runCatching { json.decodeFromString<Voicebank>(File(d, "voicebank.json").readText()) }.getOrNull()
            ?.takeIf { it.id.isNotEmpty() }?.also { it.dir = d }

    /** The active voicebank, null when none. */
    fun active(): Voicebank? = settings().voicebank.takeIf { it.isNotEmpty() }?.let { load(bankDir(it)) }

    /** The imported voicebanks, by name. */
    fun voicebanks(): List<Voicebank> =
        (File(dir, "voicebanks").listFiles() ?: emptyArray())
            .filter { it.isDirectory && !it.name.endsWith(".tmp") }
            .mapNotNull(::load).sortedBy { it.name }

    /** Writes [v]'s voicebank.json again, as after its base note changes. */
    fun save(v: Voicebank) = write(File(v.dir ?: bankDir(v.id), "voicebank.json"), v)

    /** Removes the voicebank and its samples; no voicebank is active if it was. */
    fun delete(id: String) {
        require(id.isNotEmpty() && id.none { it in "/\\." }) { "not a voicebank ID: $id" }
        bankDir(id).deleteRecursively()
        val s = settings()
        if (s.voicebank == id) saveSettings(s.copy(voicebank = ""))
    }

    /**
     * Imports the voicebanks [src] holds, cut to their oto.ini segments and
     * resampled, each in place of one of the same name. Those that fail are
     * left out unless all do.
     */
    fun import(src: Source, progress: (Int, Int) -> Unit): List<Voicebank> {
        val out = mutableListOf<Voicebank>()
        var first: Exception? = null
        for (b in src.banks()) {
            try { out += importBank(b, progress) } catch (e: Exception) { if (first == null) first = e }
        }
        if (out.isEmpty()) throw first ?: NoSamplesException()
        return out
    }

    private fun fnv32a(s: String): String {
        var h = 0x811c9dc5.toInt()
        for (b in s.toByteArray()) { h = h xor (b.toInt() and 0xff); h *= 0x01000193 }
        return "%08x".format(h)
    }

    private class SetOf(val dir: String, val resolved: Map<String, Resolved>, val sb: ReadSubbank?) {
        val own = KanaTable.filter { k -> resolved[k.romaji]?.own(k.romaji) == true }
    }

    private fun importBank(src: Source, progress: (Int, Int) -> Unit): Voicebank {
        val read = VoicebankReader.read(src)
        val id = fnv32a(read.name)
        val final = bankDir(id)
        val colors = read.colors()
        // Imported again, it keeps the base note it was told and its color.
        val old = load(final)
        val tmp = File(final.path + ".tmp")
        tmp.deleteRecursively()
        tmp.mkdirs()
        try {
            var image = ""
            if (read.image.isNotEmpty()) runCatching { src.readFile(read.image) }.getOrNull()?.let {
                image = "image" + read.image.substringAfterLast('.', "").lowercase().let { e -> if (e.isEmpty()) "" else ".$e" }
                File(tmp, image).writeBytes(it)
            }
            val sets = listOf(SetOf("", Resolver.resolve(read, null), null)) +
                read.subbanks.mapIndexed { i, sb -> SetOf("$i", Resolver.resolve(read, sb), sb) }
            val total = sets.sumOf { it.own.size }
            var done = 0
            var found = false
            val skipped = mutableListOf<String>()
            val cuts = HashMap<OtoEntry, Sample>() // the same entry in several sets is cut once
            var plain = emptyMap<String, Sample>()
            val subbanks = mutableListOf<Subbank>()
            for ((i, st) in sets.withIndex()) {
                val samples = LinkedHashMap<String, Sample>()
                if (st.dir.isNotEmpty()) File(tmp, st.dir).mkdirs()
                // The files several samples share, of this set alone, to bound memory.
                val wavs = HashMap<String, Sound>()
                for (k in st.own) {
                    val r = st.resolved.getValue(k.romaji)
                    var smp = cuts[r.entry]
                    if (smp == null) {
                        val file = if (st.dir.isEmpty()) "${k.romaji}.wav" else "${st.dir}/${k.romaji}.wav"
                        try {
                            smp = cut(src, wavs, r, File(tmp, file)).copy(file = file)
                            cuts[r.entry] = smp
                        } catch (e: Exception) {
                            skipped += "${k.hiragana} (${r.entry.alias}): ${e.message}"
                        }
                    }
                    if (smp != null) { samples[k.romaji] = smp.copy(from = k.romaji); found = true }
                    progress(++done, total)
                }
                for ((romaji, r) in st.resolved) {
                    val from = samples[r.from]
                    if (from != null && !r.own(romaji)) samples[romaji] = from // the file of the kana standing in
                }
                if (i == 0) plain = samples
                else if (samples.isNotEmpty()) subbanks += Subbank(st.sb!!.color, st.sb.tones.map { listOf(it.first, it.last) }, samples)
            }
            if (!found) throw if (skipped.isNotEmpty()) NoSamplesException("no usable sample: ${skipped[0]}") else NoSamplesException()
            val v = Voicebank(
                id = id, name = read.name, credit = read.credit, image = image,
                samples = plain, subbanks = subbanks, colors = colors,
                color = old?.color?.takeIf { it in colors } ?: "",
                base = old?.base ?: "", skipped = skipped,
            )
            write(File(tmp, "voicebank.json"), v)
            final.deleteRecursively()
            if (!tmp.renameTo(final)) throw java.io.IOException("cannot move the voicebank into place")
            v.dir = final
            return v
        } finally {
            tmp.deleteRecursively() // gone once renamed, or after a failure
        }
    }

    private fun cut(src: Source, wavs: HashMap<String, Sound>, r: Resolved, dst: File): Sample {
        val e = r.entry
        val snd = wavs.getOrPut(e.file) { Wav.read(src.readFile(e.file)) }
        val d = e.duration(snd.ms)
        if (d <= 0) throw IllegalStateException("the sample is 0 ms long")
        dst.writeBytes(Wav.write(snd.segment(e.offset, d).resample(RATE)))
        return Sample(alias = e.alias, source = e.file, offset = e.offset, duration = d, from = r.from)
    }
}
