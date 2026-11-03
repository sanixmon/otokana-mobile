package com.otokana.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otokana.core.Deck
import com.otokana.core.Group
import com.otokana.core.KanaTable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(vm: AppViewModel) {
    val s = vm.play.settings
    val start = { if (vm.canStart()) vm.start() }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .onKeyEvent { if (it.type == KeyEventType.KeyUp && it.key == Key.Enter) { start(); true } else false }
    ) {
        Header(actions = { SettingsButton(vm) })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Kana practice", style = MaterialTheme.typography.displaySmall)
                Text("Type the romaji of each kana that shows up.", style = MaterialTheme.typography.bodyLarge, color = Palette.TeksRedup)
            }
            Preview(vm)
            OptionGroup("Scripts") {
                OptionToggle(s.hiragana, { vm.setDeck(s.copy(hiragana = it)) }, "か", "Hiragana")
                OptionToggle(s.katakana, { vm.setDeck(s.copy(katakana = it)) }, "カ", "Katakana")
            }
            OptionGroup("Variants") {
                OptionToggle(s.monographs, { vm.setDeck(s.copy(monographs = it)) }, "か", "Basic", "46 kana")
                OptionToggle(s.diacritics, { vm.setDeck(s.copy(diacritics = it)) }, "が", "Dakuten", "25 kana")
                OptionToggle(s.digraphs, { vm.setDeck(s.copy(digraphs = it)) }, "きゃ", "Yōon", "21 kana", 24.sp)
            }
            VoicebankLine(vm)
            VSpace(4.dp)
        }
        Button(
            onClick = start, enabled = vm.canStart(),
            modifier = Modifier.fillMaxWidth().padding(24.dp, 12.dp, 24.dp, 20.dp).height(56.dp),
            shape = RoundedCornerShape(6.dp),
        ) { Text("Start practice", style = MaterialTheme.typography.labelLarge, fontSize = 17.sp) }
    }
}

@Composable
fun SettingsButton(vm: AppViewModel) =
    IconButton({ vm.overlay = Overlay.Settings }) { Icon(Icons.Rounded.Tune, "Settings") }

/** The pad with a kana of the choice, and the deck's size. */
@Composable
private fun Preview(vm: AppViewModel) {
    val s = vm.play.settings
    val cards = Deck.build(s)
    val sample = if (cards.isEmpty()) "" else {
        val romaji = mapOf(Group.Mono to "ka", Group.Dia to "ga", Group.Di to "kya")[s.groups()[0]]
        cards.firstOrNull { it.entry.romaji == romaji }?.kana.orEmpty()
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(Modifier.size(120.dp, 138.dp)) {
            PadEdges(sheetLayers(cards.size, cards.size), Modifier.matchParentSize())
            Column(Modifier.matchParentSize().padPaper(), horizontalAlignment = Alignment.CenterHorizontally) {
                Binding()
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PracticeBox(88.dp, true, sample, if (sample.length > 1) 40.sp else 64.sp)
                }
            }
        }
        Text(
            if (cards.isEmpty()) "Choose at least one script and one variant" else "${cards.size} cards, shuffled",
            style = MaterialTheme.typography.bodyMedium, color = Palette.TeksRedup,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionGroup(label: String, content: @Composable () -> Unit) =
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GroupLabel(label)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }

/** A setting shown as a practice box beside its label, on paper when on. */
@Composable
private fun OptionToggle(on: Boolean, set: (Boolean) -> Unit, sample: String, label: String, detail: String = "", sampleSize: TextUnit = 32.sp) =
    Choice(Modifier.toggleable(on, role = Role.Checkbox, onValueChange = set), on, 56.dp, sample, sampleSize, label, detail)

/** A control that is on or off, styled as a practice box beside its label and detail. */
@Composable
fun Choice(toggle: Modifier, on: Boolean, size: Dp, sample: String, sampleSize: TextUnit, label: String, detail: String) {
    Row(
        Modifier.clip(RoundedCornerShape(4.dp)).then(toggle).padding(4.dp, 4.dp, 10.dp, 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PracticeBox(size, on, sample, sampleSize)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = if (on) Palette.Teks else Palette.TeksRedup)
            if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall, color = Palette.TeksRedup)
        }
    }
}

@Composable
private fun VoicebankLine(vm: AppViewModel) {
    val v = vm.vb
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, Modifier.size(18.dp), tint = Palette.TeksRedup)
        Column(Modifier.weight(1f)) {
            if (v != null) {
                Text(v.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${v.own().second} of ${KanaTable.size} kana with sound", style = MaterialTheme.typography.bodySmall, color = Palette.TeksRedup)
            } else {
                Text("No voicebank yet, practicing without sound.", style = MaterialTheme.typography.bodyMedium, color = Palette.TeksRedup)
            }
        }
        LinkButton(if (v != null) "Change" else "Import") { vm.overlay = Overlay.Import }
    }
}

/** A button of underlined green text. */
@Composable
fun LinkButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) =
    TextButton(onClick, modifier.widthIn(min = 48.dp)) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Palette.Hijau, textDecoration = TextDecoration.Underline)
    }
