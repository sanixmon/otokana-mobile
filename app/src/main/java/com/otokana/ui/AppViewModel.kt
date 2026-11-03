package com.otokana.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.otokana.audio.AudioOut
import com.otokana.audio.Sound
import com.otokana.audio.Wav
import com.otokana.core.Card
import com.otokana.core.DeckSettings
import com.otokana.core.Format
import com.otokana.core.Notes
import com.otokana.core.Phase
import com.otokana.core.Session
import com.otokana.store.PlayOn
import com.otokana.store.Record
import com.otokana.store.Settings
import com.otokana.store.Store
import com.otokana.store.Voicebank
import com.otokana.voicebank.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant

/** What the session shows, copied out of it after every change. */
data class Play(
    val phase: Phase = Phase.Setup,
    val settings: DeckSettings = DeckSettings(),
    val deck: List<Card> = emptyList(),
    val input: String = "",
    val hint: Boolean = false,
    val wrong: Boolean = false,
    val started: Long = 0,
    val ended: Long = 0,
) {
    val active get() = deck.lastOrNull { !it.done }
    val remaining get() = deck.count { !it.done }
    val hinted get() = deck.count { it.hinted }
}

/** A sheet torn off by a correct answer, as it showed, turning over the binding. */
data class Torn(val card: Card, val hint: Boolean, val at: Long)

data class ImportState(val busy: Boolean = false, val done: Int = 0, val total: Int = 0, val error: String? = null)

enum class Overlay { None, Settings, Import }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app.filesDir)
    private val out = AudioOut()
    private val s = Session()

    var play by mutableStateOf(Play()); private set
    var prefs by mutableStateOf(Settings()); private set
    var prefsError by mutableStateOf<String?>(null); private set
    var vb by mutableStateOf<Voicebank?>(null); private set
    var banks by mutableStateOf<List<Voicebank>>(emptyList()); private set
    var overlay by mutableStateOf(Overlay.None)
    var imp by mutableStateOf(ImportState()); private set
    /** Why the voicebank's base note or color was not saved. */
    var vbError by mutableStateOf<String?>(null); private set
    val torn = mutableStateListOf<Torn>()
    var wrongAt by mutableLongStateOf(0L); private set

    private val sounds = HashMap<String, Sound?>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val p = runCatching { store.settings() }.getOrDefault(Settings())
            val active = runCatching { store.active() }.getOrNull()
            val list = runCatching { store.voicebanks() }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) {
                prefs = p
                banks = list
                vb = list.firstOrNull { it.id == active?.id } ?: active
            }
        }
    }

    fun now() = SystemClock.uptimeMillis()

    private fun sync(wrong: Boolean = play.wrong) {
        play = Play(s.phase, s.settings, s.deck, s.input, s.hint, wrong, s.started, s.ended)
    }

    // --- Setup ---

    fun setDeck(settings: DeckSettings) { s.settings = settings; sync() }

    fun canStart() = s.canStart()

    fun start() {
        s.start(now())
        torn.clear()
        sync(false)
        if (s.phase == Phase.Playing && prefs.playOn != PlayOn.OnCorrect) playSound(s.active())
    }

    // --- Playing ---

    fun type(text: String) {
        val hint = s.hint // before the answer hides it
        val t = now()
        val (done, wrong) = s.type(text, t)
        if (done != null) {
            torn.removeAll { t - it.at >= FLIP_MS }
            torn += Torn(done, hint, t)
            // The card answered as it is, the next one as it shows.
            val cards = buildList {
                if (prefs.playOn != PlayOn.OnShow) add(done)
                if (prefs.playOn != PlayOn.OnCorrect) s.active()?.let(::add)
            }
            playSound(*cards.toTypedArray())
            if (s.phase == Phase.Results) record()
        }
        if (wrong) wrongAt = t
        sync(s.result() == com.otokana.core.MatchResult.Wrong)
    }

    fun backspace() { s.backspace(); sync(s.result() == com.otokana.core.MatchResult.Wrong) }
    fun clearInput() { s.clear(); sync(false) }
    fun showHint() { s.showHint(); sync() }
    fun stop() { s.stop(); torn.clear(); sync(false) }
    fun playAgain() { s.playAgain(); sync(false) }
    fun dropTorn(t: Torn) { torn.remove(t) }

    private fun record() {
        val hinted = s.hinted()
        val r = Record(
            date = Instant.now().toString(), deck = Format.deckName(s.settings), cards = s.deck.size,
            hinted = hinted, ms = s.elapsed(s.ended), accuracy = Format.accuracy(s.deck.size, hinted),
        )
        viewModelScope.launch(Dispatchers.IO) { runCatching { store.addRecord(r) } }
    }

    // --- Sound ---

    /** The kana's sound, read once, or null. */
    fun sound(romaji: String): Sound? {
        val v = vb ?: return null
        val smp = v.sample(romaji) ?: return null
        val key = v.id + "/" + smp.file
        return sounds.getOrPut(key) {
            runCatching { File(v.dir, smp.file).readBytes().let(Wav::read) }.getOrNull()
        }
    }

    fun hasSound(card: Card?) = card != null && vb?.sample(card.entry.romaji) != null

    /** The note the card plays at, null when the base note is not known. */
    fun note(card: Card?): String? {
        if (card == null) return null
        val base = Notes.parse(vb?.base) ?: return null
        return Notes.name(base + card.pitch)
    }

    /** Plays the cards' sounds one after another, each at its card's note when known. */
    fun playSound(vararg cards: Card?) {
        val list = cards.filterNotNull()
        if (list.isEmpty() || vb == null) return
        viewModelScope.launch(Dispatchers.Default) {
            val snds = list.mapNotNull { c ->
                sound(c.entry.romaji)?.let { if (note(c) != null) it.shift(c.pitch) else it }
            }
            if (snds.isNotEmpty()) out.play(snds)
        }
    }

    // --- Settings ---

    fun setPlayOn(p: PlayOn) {
        prefs = prefs.copy(playOn = p)
        savePrefs()
    }

    private fun savePrefs() {
        val p = prefs
        viewModelScope.launch(Dispatchers.IO) {
            val err = runCatching { store.saveSettings(p) }.exceptionOrNull()?.message
            withContext(Dispatchers.Main) { prefsError = err }
        }
    }

    /** Saves the base note typed, "" when it is not a note. */
    fun setBase(text: String) {
        val v = vb ?: return
        val base = Notes.parse(text)?.let(Notes::name) ?: ""
        if (base != v.base) update(v.copy(base = base))
    }

    fun setColor(color: String) {
        val v = vb ?: return
        if (color != v.color) update(v.copy(color = color))
    }

    private fun update(v: Voicebank) {
        v.dir = vb?.dir
        vb = v
        banks = banks.map { if (it.id == v.id) v else it }
        vbError = runCatching { store.save(v) }.exceptionOrNull()?.message
    }

    // --- Voicebanks ---

    fun activate(v: Voicebank) {
        vb = v
        prefs = prefs.copy(voicebank = v.id)
        savePrefs()
    }

    fun delete(v: Voicebank) {
        viewModelScope.launch(Dispatchers.IO) {
            val err = runCatching { store.delete(v.id) }.exceptionOrNull()
            val list = runCatching { store.voicebanks() }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) {
                if (err != null) { imp = ImportState(error = err.message); return@withContext }
                sounds.keys.removeAll { it.startsWith(v.id + "/") }
                if (vb?.id == v.id) { vb = null; prefs = prefs.copy(voicebank = "") }
                banks = list
            }
        }
    }

    fun importFolder(uri: Uri) = load { Source.tree(getApplication(), uri) }

    fun importZip(uri: Uri) = load {
        val ctx = getApplication<Application>()
        val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "Voicebank.zip"
        val tmp = File(ctx.cacheDir, "import.zip")
        ctx.contentResolver.openInputStream(uri)?.use { i -> tmp.outputStream().use { i.copyTo(it) } }
            ?: throw java.io.FileNotFoundException("cannot open the zip")
        Source.zip(tmp, name)
    }

    /** Imports the voicebanks [open] gives in the background; the first becomes active. */
    private fun load(open: () -> Source) {
        if (imp.busy) return
        imp = ImportState(busy = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                open().use { src ->
                    store.import(src) { d, t -> viewModelScope.launch(Dispatchers.Main) { if (imp.busy) imp = imp.copy(done = d, total = t) } }
                }
            }
            File(getApplication<Application>().cacheDir, "import.zip").delete()
            val list = runCatching { store.voicebanks() }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) {
                result.onSuccess { imported ->
                    imp = ImportState()
                    sounds.clear()
                    banks = list
                    activate(list.firstOrNull { it.id == imported[0].id } ?: imported[0])
                }.onFailure { imp = ImportState(error = it.message ?: it.javaClass.simpleName) }
            }
        }
    }

    override fun onCleared() = out.release()

    companion object {
        const val FLIP_MS = 300L
        const val SHAKE_MS = 240L
    }
}
