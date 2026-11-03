package com.otokana.voicebank

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.InputStream
import java.text.Normalizer
import java.util.zip.ZipFile

/**
 * The folder or zip file a voicebank is read from; its files are never
 * changed. [files] are slash paths, sorted.
 */
class Source private constructor(
    val name: String,
    private val open: (String) -> InputStream,
    private val onClose: () -> Unit,
) : AutoCloseable {
    private val _files = mutableListOf<String>()
    val files: List<String> get() = _files

    /** Finds files whose names differ in case or Unicode normalization. */
    private val keys = HashMap<String, String>()

    private fun add(name: String) {
        _files += name
        keys.putIfAbsent(key(name), name)
    }

    /** The name of the file [name] refers to, in its case and normalization or not. */
    fun find(name: String): String? = keys[key(name)]

    fun readFile(name: String): ByteArray {
        val f = find(name) ?: throw java.io.FileNotFoundException("$name: file does not exist")
        return open(f).use { it.readBytes() }
    }

    override fun close() = onClose()

    /**
     * Splits a source of several voicebanks, each a folder of its own
     * character.txt or character.yaml, into one source each, as OpenUtau
     * installs them; else returns this source alone.
     */
    fun banks(): List<Source> {
        val roots = mutableListOf<String>()
        for (f in files) {
            val b = P.base(f).lowercase()
            if (b == "character.txt" || b == "character.yaml") {
                val d = P.dir(f)
                if (d !in roots) roots += d
            }
        }
        fun under(f: String, root: String) = root == "." || f.startsWith("$root/")
        val top = roots.filter { r -> roots.none { o -> o != r && under(r, o) } }.sorted()
        if (top.size < 2) return listOf(this)
        return top.map { r ->
            Source(P.base(r), open) {}.also { b -> files.filter { under(it, r) }.forEach(b::add) }
        }
    }

    companion object {
        private fun key(name: String) = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFC)

        /**
         * A zip file, whose names not marked UTF-8 are read as UTF-8 when they
         * are, else as Shift-JIS.
         */
        fun zip(file: File, name: String): Source {
            val utf8 = runCatching { ZipFile(file, Charsets.UTF_8) }.getOrNull()?.let { z ->
                val clean = runCatching { z.entries().asSequence().none { '�' in it.name } }.getOrDefault(false)
                if (clean) z else { z.close(); null }
            }
            val zf = utf8 ?: ZipFile(file, shiftJisCharset)
            val entries = HashMap<String, java.util.zip.ZipEntry>()
            val s = Source(name.removeSuffix(".zip").removeSuffix(".ZIP"), { n ->
                zf.getInputStream(entries[n] ?: throw java.io.FileNotFoundException(n))
            }, { zf.close() })
            for (e in zf.entries()) {
                val n = P.clean(e.name.replace('\\', '/').trimStart('/'))
                if (e.isDirectory || n == "." || n.startsWith("__MACOSX/")) continue
                if (entries.putIfAbsent(n, e) == null) s.add(n)
            }
            s._files.sort()
            return s
        }

        /** A folder the user picked, read recursively through the document tree. */
        fun tree(context: Context, treeUri: Uri): Source {
            val cr = context.contentResolver
            val rootId = DocumentsContract.getTreeDocumentId(treeUri)
            val docs = HashMap<String, Uri>()
            fun walk(docId: String, prefix: String) {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                cr.query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    ), null, null, null,
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0)
                        val n = c.getString(1)
                        val path = if (prefix.isEmpty()) n else "$prefix/$n"
                        if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) walk(id, path)
                        else docs[path] = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    }
                }
            }
            walk(rootId, "")
            val rootName = cr.query(
                DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId),
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null,
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "Voicebank"
            val s = Source(rootName, { n ->
                cr.openInputStream(docs[n] ?: throw java.io.FileNotFoundException(n))
                    ?: throw java.io.FileNotFoundException(n)
            }, {})
            docs.keys.sorted().forEach(s::add)
            return s
        }

        /** A folder on disk, for tests. */
        fun dir(dir: File): Source {
            val s = Source(dir.name, { n -> File(dir, n).inputStream() }, {})
            dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }.sorted().forEach(s::add)
            return s
        }
    }
}

private val shiftJisCharset: java.nio.charset.Charset =
    runCatching { java.nio.charset.Charset.forName("windows-31j") }.getOrElse { java.nio.charset.Charset.forName("Shift_JIS") }
