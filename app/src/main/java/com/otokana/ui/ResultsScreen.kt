package com.otokana.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otokana.core.Format

/** How many cards were answered without their hint, of all. */
@Composable
fun ResultsScreen(vm: AppViewModel) {
    val p = vm.play
    val cards = p.deck.size
    val hinted = p.hinted
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .onKeyEvent { if (it.type == KeyEventType.KeyUp && it.key == Key.Enter) { vm.playAgain(); true } else false }
    ) {
        Header(actions = { SettingsButton(vm) })
        Column(
            Modifier.weight(1f).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("Finished", style = MaterialTheme.typography.displayMedium)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${cards - hinted}/$cards", fontFamily = KanaFont, fontWeight = FontWeight.SemiBold, fontSize = 88.sp, lineHeight = 92.sp)
                Text("correct", fontSize = 20.sp, modifier = Modifier.padding(bottom = 14.dp))
            }
            Text(
                "${Format.deckName(p.settings)} · ${Format.duration(p.ended - p.started)} · ${Format.accuracy(cards, hinted)}%",
                style = MaterialTheme.typography.bodyLarge, color = Palette.TeksRedup,
            )
        }
        Button(
            vm::playAgain, Modifier.fillMaxWidth().padding(24.dp, 12.dp, 24.dp, 20.dp).height(56.dp),
            shape = RoundedCornerShape(6.dp),
        ) { Text("Play again", style = MaterialTheme.typography.labelLarge, fontSize = 17.sp) }
    }
}
