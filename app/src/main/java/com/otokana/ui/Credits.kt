package com.otokana.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Credit(val title: String, val detail: String, val url: String, val license: String? = null)

private val credits = listOf(
    Credit(
        "OtoKana by kumorinka",
        "The original desktop app. Its design, palette, kana table, practice rules, voicebank handling and icon are theirs; this app ports them to Android.",
        "https://github.com/kumorinka/otokana",
    ),
    Credit(
        "Klee One",
        "Copyright 2020 The Klee Project Authors. SIL Open Font License 1.1.",
        "https://github.com/fontworks-fonts/Klee", "licenses/OFL-KleeOne.txt",
    ),
    Credit(
        "Zen Kaku Gothic New",
        "Copyright 2022 The Zen Kaku Gothic Project Authors. SIL Open Font License 1.1.",
        "https://github.com/googlefonts/zen-kakugothic", "licenses/OFL-ZenKakuGothicNew.txt",
    ),
    Credit(
        "UTAU and OpenUtau",
        "The voicebank format, and OpenUtau's way of reading oto.ini, character.yaml, prefix.map and voice colors, which the import follows.",
        "https://github.com/stakira/OpenUtau",
    ),
    Credit(
        "Jetpack Compose, Material 3, AndroidX, kotlinx.serialization",
        "Apache License 2.0.",
        "https://developer.android.com/jetpack/compose",
    ),
)

/** Who made what the app is built on, with links and the font licenses. */
@Composable
fun CreditsSection() {
    val uri = LocalUriHandler.current
    val ctx = LocalContext.current
    var license by remember { mutableStateOf<Pair<String, String>?>(null) }
    license?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { license = null },
            title = { Text(title) },
            text = { Text(text, Modifier.verticalScroll(rememberScrollState()), fontSize = 12.sp, lineHeight = 17.sp) },
            confirmButton = { TextButton({ license = null }) { Text("Close", color = Palette.Teks) } },
            containerColor = Palette.GarisBantuRedup,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GroupLabel("Credits")
        for (c in credits) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(c.title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(c.detail, style = MaterialTheme.typography.bodySmall, color = Palette.TeksRedup)
            androidx.compose.foundation.layout.Row {
                LinkButton("Website") { runCatching { uri.openUri(c.url) } }
                c.license?.let { path ->
                    LinkButton("License") {
                        license = c.title to runCatching { ctx.assets.open(path).bufferedReader().readText() }.getOrDefault("")
                    }
                }
            }
        }
        Text(
            "No voicebank or audio comes with the app: use one you have the right to use.",
            style = MaterialTheme.typography.bodySmall, color = Palette.TeksRedup,
        )
    }
}

private typealias Modifier = androidx.compose.ui.Modifier
