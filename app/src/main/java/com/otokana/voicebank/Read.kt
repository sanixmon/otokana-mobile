package com.otokana.voicebank

/** What the oto.ini files and the character file of a source say. */
class ReadBank(
    val name: String,
    /** The author and their web site. */
    val credit: String,
    val otos: Int,
    val bad: Int,
    /** Every line whose file is there, the shallowest oto.ini first. */
    val entries: List<OtoEntry>,
    val subbanks: List<Subbank>,
    /** The file the character file names as its picture, "" without one. */
    val image: String,
) {
    /** The voice colors of the subbanks, in order; none for a bank of one. */
    fun colors(): List<String> = subbanks.map { it.color }.distinct().takeIf { it.size >= 2 } ?: emptyList()
}

class NoOtoException : Exception("no oto.ini inside")

object VoicebankReader {
    /** Reads the oto.ini files of [src], and its character.txt, and character.yaml over it. */
    fun read(src: Source): ReadBank {
        val byDepth = compareBy<String>({ it.count { c -> c == '/' } }, { it })
        fun named(base: String) = src.files.filter { P.base(it).equals(base, ignoreCase = true) }.sortedWith(byDepth)
        val otos = named("oto.ini")
        if (otos.isEmpty()) throw NoOtoException()
        var name = src.name
        var root = ""
        var author = ""
        var web = ""
        var image = ""
        var subbanks = emptyList<Subbank>()
        for ((file, yaml) in listOf("character.txt" to false, "character.yaml" to true)) {
            val f = named(file).firstOrNull() ?: continue
            val text = decode(src.readFile(f))
            if (root.isEmpty()) root = P.dir(f)
            val c = Oto.parseCharacter(text, yaml)
            name = c.name.ifEmpty { name }
            author = c.author.ifEmpty { author }
            web = c.web.ifEmpty { web }
            if (c.image.isNotEmpty()) src.find(P.join(P.dir(f), c.image.replace('\\', '/')))?.let { image = it }
            if (yaml) subbanks = Subbanks.parseYaml(text)
        }
        if (subbanks.isEmpty()) subbanks = Subbanks.readPrefixMaps(src, root.ifEmpty { P.dir(otos[0]) })
        val entries = mutableListOf<OtoEntry>()
        var bad = 0
        for (o in otos) {
            val (es, b) = Oto.parse(decode(src.readFile(o)), P.dir(o))
            // Some banks ship a stale oto.ini whose files are gone; its lines would hide the real ones.
            entries += es.filter { src.find(it.file) != null }
            bad += b
        }
        return ReadBank(name, listOfNotNull(author.ifEmpty { null }, web.ifEmpty { null }).joinToString(", "),
            otos.size, bad, entries, subbanks, image)
    }
}
