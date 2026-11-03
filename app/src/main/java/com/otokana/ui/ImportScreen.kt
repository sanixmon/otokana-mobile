package com.otokana.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otokana.core.KanaTable
import com.otokana.store.Voicebank

@Composable
fun ImportScreen(vm: AppViewModel) {
    val close = { vm.overlay = Overlay.None }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Header(onBack = close, actions = { SettingsButton(vm) })
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp, 12.dp, 24.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Import a voicebank", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Choose a UTAU voicebank folder or zip file. Its files are read on this device and are not uploaded anywhere.",
                    style = MaterialTheme.typography.bodyLarge, color = Palette.TeksRedup,
                )
            }
            DropZone(vm)
            Text(
                "Voicebanks are not bundled with the app, so use one you have the right to use. Without a voicebank, practice still works, just without sound.",
                style = MaterialTheme.typography.bodyMedium, color = Palette.TeksRedup,
            )
            LinkButton("Skip, practice without sound", Modifier.padding(start = 0.dp), close)
            if (vm.banks.isNotEmpty()) {
                HorizontalDivider(color = Palette.GarisPemisah)
                BankList(vm)
            }
            vm.vb?.let {
                HorizontalDivider(color = Palette.GarisPemisah)
                ActiveVoicebank(vm, it)
            }
        }
    }
}

/** The buttons choosing a folder or zip, and how importing it goes. */
@Composable
private fun DropZone(vm: AppViewModel) {
    val folder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(vm::importFolder) }
    val zip = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importZip) }
    val im = vm.imp
    val (title, detail) = when {
        im.busy -> "Importing voicebank…" to if (im.total > 0) "Copying sample ${im.done} of ${im.total}" else "Reading its files"
        im.error != null -> "The voicebank could not be imported" to sentence(im.error)
        else -> "Choose a folder or .zip file" to "It must contain oto.ini and wav files"
    }
    Column(
        Modifier.fillMaxWidth().dashedBorder(1.5.dp, Palette.GarisMeja, 6.dp).padding(20.dp, 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!im.busy) Icon(Icons.Rounded.FileUpload, null, Modifier.size(28.dp), tint = Palette.TeksRedup)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontSize = 17.sp, textAlign = TextAlign.Center)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = if (im.error != null) Palette.PenaMerah else Palette.TeksRedup, textAlign = TextAlign.Center)
        }
        if (im.busy) {
            if (im.total > 0) LinearProgressIndicator({ im.done / im.total.toFloat() }, Modifier.fillMaxWidth(), color = Palette.Hijau, trackColor = Palette.GarisKotakRedup)
            else LinearProgressIndicator(Modifier.fillMaxWidth(), color = Palette.Hijau, trackColor = Palette.GarisKotakRedup)
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button({ folder.launch(null) }, shape = RoundedCornerShape(6.dp)) { Text("Choose folder") }
            OutlinedButton({ zip.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }, shape = RoundedCornerShape(6.dp)) {
                Text("Zip file", color = Palette.Teks)
            }
        }
    }
}

/** The imported voicebanks, the active one marked; a tap activates, the bin deletes once confirmed. */
@Composable
private fun BankList(vm: AppViewModel) {
    var deleting by remember { mutableStateOf<Voicebank?>(null) }
    deleting?.let { v ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete “${v.name}”?") },
            text = { Text("Its samples are removed from OtoKana. The folder or zip file it was imported from stays.") },
            confirmButton = { TextButton({ vm.delete(v); deleting = null }) { Text("Delete", color = Palette.PenaMerah) } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel", color = Palette.Teks) } },
            containerColor = Palette.GarisBantuRedup,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupLabel("Your voicebanks")
        for (v in vm.banks) {
            val on = vm.vb?.id == v.id
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(6.dp))
                        .background(if (on) Palette.GarisKotakRedup else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { vm.activate(v) }.padding(6.dp, 6.dp, 10.dp, 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    BankPicture(v, 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(v.name, fontFamily = KanaFont, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (v.credit.isNotEmpty()) Text(v.credit, style = MaterialTheme.typography.bodySmall, color = Palette.TeksRedup, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton({ deleting = v }) { Icon(Icons.Rounded.DeleteOutline, "Delete ${v.name}", tint = Palette.TeksRedup) }
            }
        }
    }
}

/**
 * The active voicebank, and the sheet of what its kana play: their own
 * sample on paper, another's in a dashed frame, none in a dim box.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveVoicebank(vm: AppViewModel, v: Voicebank) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            BankPicture(v, 72.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GroupLabel("Active voicebank")
                Text(v.name, fontFamily = KanaFont, fontSize = 24.sp, lineHeight = 30.sp, maxLines = 2)
                if (v.credit.isNotEmpty()) Text("Credit: ${v.credit}", style = MaterialTheme.typography.bodySmall, color = Palette.TeksRedup)
            }
        }
        VoiceColor(vm, v)
        BaseNote(vm, v)
        var standIns = 0
        var missing = 0
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (k in KanaTable) {
                val size = if (k.hiragana.length > 1) 11.sp else 15.sp
                val s = v.sample(k.romaji)
                when {
                    s == null -> { missing++; PracticeBox(28.dp, false, k.hiragana, size) }
                    !s.own(k.romaji) -> { standIns++; PracticeBox(28.dp, true, k.hiragana, size, dashed = true) }
                    else -> PracticeBox(28.dp, true, k.hiragana, size)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val note = MaterialTheme.typography.bodySmall
            Text("${v.own().first} of ${KanaTable.size} kana have their own sample.", style = note)
            if (standIns > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PracticeBox(20.dp, true, "ぢ", 11.sp, dashed = true)
                Text("Dashed: a stand-in sound, as ぢ uses じ's.", style = note, color = Palette.TeksRedup)
            }
            if (missing > 0) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PracticeBox(20.dp, false, "", 11.sp)
                Text("Dim: $missing kana without sound.", style = note, color = Palette.TeksRedup)
            }
            if (v.skipped.isNotEmpty()) Text("${v.skipped.size} samples skipped, such as ${v.skipped[0]}.", style = note, color = Palette.TeksRedup)
        }
    }
}

/** Capitalizes [s] and ends it with a period. */
fun sentence(s: String): String {
    if (s.isEmpty()) return s
    val c = s.replaceFirstChar { it.uppercase() }
    return if (c.endsWith(".")) c else "$c."
}
