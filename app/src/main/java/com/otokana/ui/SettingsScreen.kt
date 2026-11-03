package com.otokana.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otokana.core.Notes
import com.otokana.core.Session
import com.otokana.store.PlayOn
import com.otokana.store.Voicebank

/** The settings, saved as they change. */
@Composable
fun SettingsScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Header(onBack = { vm.overlay = Overlay.None })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp, 12.dp, 24.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.displaySmall)
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GroupLabel("When to play sounds")
                VSpace(6.dp)
                for ((on, label, detail) in listOf(
                    Triple(PlayOn.OnCorrect, "When answered right", "The kana you answer plays, as a sign it was right."),
                    Triple(PlayOn.OnShow, "When a card shows", "Hear the kana first, then type its romaji."),
                    Triple(PlayOn.OnBoth, "Both", "The kana you answer plays, then the next one."),
                )) {
                    val sel = vm.prefs.playOn == on
                    Choice(Modifier.selectable(sel, role = Role.RadioButton) { vm.setPlayOn(on) }, sel, 28.dp, "", 0.sp, label, detail)
                }
            }
            val v = vm.vb
            if (v != null) {
                VoiceColor(vm, v)
                BaseNote(vm, v)
            } else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GroupLabel("Base note")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("No voicebank yet.", color = Palette.TeksRedup)
                    LinkButton("Import") { vm.overlay = Overlay.Import }
                }
            }
            vm.prefsError?.let { Text("Settings not saved: $it", color = Palette.PenaMerah, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Palette.Hijau, unfocusedBorderColor = Palette.GarisMeja,
    cursorColor = Palette.Hijau, focusedLabelColor = Palette.Hijau,
)

/** The note [v]'s samples are sung at, saved as it is typed; a text that is not a note leaves it unknown. */
@Composable
fun BaseNote(vm: AppViewModel, v: Voicebank) {
    var text by remember(v.id) { mutableStateOf(v.base) }
    val bad = text.isNotBlank() && Notes.parse(text) == null
    val msg = vm.vbError?.let { "Not saved: $it" } ?: if (bad) "Not a note name, such as E4 or F#3." else null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel("Base note")
        OutlinedTextField(
            text, { text = it; vm.setBase(it) },
            Modifier.width(160.dp), singleLine = true, isError = msg != null,
            placeholder = { Text("e.g. E4") }, colors = fieldColors(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii),
        )
        if (msg != null) Text(msg, color = Palette.PenaMerah, style = MaterialTheme.typography.bodySmall)
        Text(
            "The note ${v.name}'s samples are sung at. Once set, each card plays up to ${Session.PITCH_RANGE} semitones above or below it, with its note name in the card's corner.",
            style = MaterialTheme.typography.bodySmall, color = Palette.TeksRedup,
        )
    }
}

/** Which of [v]'s voice colors its kana play, for a bank of more than one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceColor(vm: AppViewModel, v: Voicebank) {
    if (v.colors.isEmpty()) return
    fun name(c: String) = c.ifEmpty { "Default" }
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel("Voice color")
        ExposedDropdownMenuBox(open, { open = it }) {
            OutlinedTextField(
                name(v.color), {}, Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(0.8f),
                readOnly = true, singleLine = true, colors = fieldColors(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            )
            ExposedDropdownMenu(open, { open = false }) {
                for (c in v.colors) DropdownMenuItem({ Text(name(c)) }, { vm.setColor(c); open = false })
            }
        }
        Text("The voice its kana are sung in, as OpenUtau's voice colors.", style = MaterialTheme.typography.bodySmall, color = Palette.TeksRedup)
    }
}
