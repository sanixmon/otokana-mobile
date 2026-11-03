package com.otokana.ui

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otokana.core.Card
import com.otokana.core.Format
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/** What the hidden field holds, so that a backspace always has something to delete. */
private const val SENTINEL = "        "

@Composable
fun PlayScreen(vm: AppViewModel) {
    val p = vm.play
    val ctx = LocalContext.current
    val reduce = remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    val summon = { runCatching { focus.requestFocus() }; kb?.show() }
    LaunchedEffect(Unit) { summon() }

    var now by remember { mutableLongStateOf(vm.now()) }
    LaunchedEffect(p.started) {
        while (true) {
            now = vm.now()
            delay(1000 - (now - p.started).mod(1000L))
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Box(Modifier.alpha(0.55f)) {
            Header(actions = { TextButton(vm::stop) { Text("Stop", color = Palette.TeksRedup, fontSize = 15.sp) } })
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
            // The sheet as large as fits above the answer line, 320 by 380 at most.
            val answerH = 104.dp
            val w = minOf(maxWidth, 320.dp, (maxHeight - answerH - 24.dp) * (320f / 380f)).coerceAtLeast(120.dp)
            val h = w * (380f / 320f)
            val answerW = minOf(maxWidth, 440.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Sheet(vm, w, h, reduce)
                Answer(vm, now, reduce, answerW, Modifier.clickable(remember { MutableInteractionSource() }, null) { summon() })
            }
        }
        // Takes the typing, with suggestions and caps off so the keyboard sends plain letters.
        var field by remember { mutableStateOf(TextFieldValue(SENTINEL, TextRange(SENTINEL.length))) }
        BasicTextField(
            value = field,
            onValueChange = { new ->
                val old = field.text
                val common = old.commonPrefixWith(new.text).length
                repeat(old.length - common) { vm.backspace() }
                val typed = new.text.substring(common)
                if (typed.isNotEmpty()) vm.type(typed)
                field = TextFieldValue(SENTINEL, TextRange(SENTINEL.length))
            },
            modifier = Modifier.size(1.dp).alpha(0f).focusRequester(focus)
                .semantics { contentDescription = "Answer" }
                .onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        it.key == Key.Escape -> { vm.stop(); true }
                        it.key == Key.Backspace && (it.isCtrlPressed || it.isAltPressed || it.isMetaPressed) -> { vm.clearInput(); true }
                        else -> false
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password, imeAction = ImeAction.None,
            ),
        )
    }
}

/** The pad with the active card on top, which a tap turns to its hint. */
@Composable
private fun Sheet(vm: AppViewModel, w: Dp, h: Dp, reduce: Boolean) {
    val p = vm.play
    val card = p.active ?: return
    Box(Modifier.size(w, h)) {
        PadEdges(sheetLayers(p.remaining, p.deck.size), Modifier.matchParentSize())
        Column(
            Modifier.matchParentSize().padPaper()
                .clickable(onClickLabel = "Show hint") { vm.showHint() }
        ) {
            Binding()
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Face(card, p.hint, w) }
        }
        Corner(vm, card, w, button = true)
        // The sheets torn off turn over the next one, the oldest on top.
        for (t in vm.torn.reversed()) androidx.compose.runtime.key(t.at) { Flip(vm, t, w, reduce) }
    }
}

/** The note the card plays at, and its sound again, over the sheet's corners. */
@Composable
private fun Corner(vm: AppViewModel, card: Card, w: Dp, button: Boolean) {
    val k = w / 320.dp
    vm.note(card)?.let {
        Text(it, Modifier.offset(42.dp * k, 12.dp + 18.dp * k), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Palette.TintaRedup)
    }
    if (vm.hasSound(card)) Box(Modifier.fillMaxWidth().padding(top = 12.dp + 4.dp * k, end = 22.dp * k), contentAlignment = Alignment.TopEnd) {
        if (button) IconButton({ vm.playSound(card) }) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, "Play sound", tint = Palette.TintaRedup)
        } else Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Palette.TintaRedup)
        }
    }
}

/** The practice box of a sheet, with the kana, or its romaji in green for the hint. */
@Composable
private fun Face(card: Card, hint: Boolean, w: Dp) {
    val box = w * (236f / 320f)
    val d = LocalDensity.current
    val text = if (hint) card.entry.romaji else card.kana
    val ratio = when {
        hint -> 96f / 236f
        text.length > 1 -> 104f / 236f // yōon, two kana wide
        else -> 172f / 236f
    }
    PracticeBox(
        box, true, text, with(d) { (box * ratio).toSp() },
        textColor = if (hint) Palette.TintaHijau else Palette.Tinta,
    )
}

private val Tnum = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")

private val flipEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/**
 * A sheet torn off: it swings on the binding toward the viewer and up over
 * it, shaded as it stands, its back showing past upright, and fades out.
 * With less motion, it only fades.
 */
@Composable
private fun Flip(vm: AppViewModel, t: Torn, w: Dp, reduce: Boolean) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(1f, tween(AppViewModel.FLIP_MS.toInt(), easing = LinearEasing))
        vm.dropTorn(t)
    }
    val k = anim.value
    val turn = if (reduce) 0f else 165f * flipEase.transform(k)
    val fade = if (reduce) 1 - k else 1 - maxOf(k - 0.4f, 0f) / 0.6f
    val stand = sin(turn * PI / 180).toFloat() // 0 lying flat, 1 upright
    val d = LocalDensity.current
    Box(Modifier.fillMaxSize().padding(top = 12.dp)) {
        // The shadow it casts on the next sheet, below the binding.
        if (stand > 0) Box(
            Modifier.fillMaxWidth().height(80.dp * stand)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f * stand * fade), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    rotationX = turn
                    cameraDistance = 8 * d.density * 100
                    alpha = fade
                }
                .background(if (turn >= 90) Palette.TepiKertas[0] else Palette.Kertas)
                .drawWithContent { drawContent(); drawRect(Color.Black.copy(alpha = 0.2f * stand)) },
            contentAlignment = Alignment.Center,
        ) {
            if (turn < 90) {
                Face(t.card, t.hint, w)
                Box(Modifier.fillMaxSize().offset(y = (-12).dp)) { Corner(vm, t.card, w, button = false) }
            }
        }
    }
}

/** The line the typing shows on, between the time and the cards left. */
@Composable
private fun Answer(vm: AppViewModel, now: Long, reduce: Boolean, width: Dp, modifier: Modifier) {
    val p = vm.play
    val (line, text) = when {
        p.wrong -> Palette.PenaMerah to Palette.PenaMerah
        p.input.isNotEmpty() -> Palette.Hijau to Palette.Teks
        else -> Palette.GarisJawaban to Palette.Teks
    }
    val caret = if (p.wrong) Palette.PenaMerah else Palette.Hijau
    val shake = remember { Animatable(1f) }
    LaunchedEffect(vm.wrongAt) {
        if (vm.wrongAt > 0 && !reduce) { shake.snapTo(0f); shake.animateTo(1f, tween(AppViewModel.SHAKE_MS.toInt(), easing = LinearEasing)) }
    }
    val k = shake.value
    val dx = (3 * sin(k * 6 * PI) * (1 - k)).toFloat()
    Column(modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                Format.clock(now - p.started), Modifier.width(72.dp).padding(bottom = 10.dp),
                fontSize = 16.sp, color = Palette.TeksRedup, style = Tnum,
            )
            Row(
                Modifier.weight(1f).height(56.dp).offset(x = dx.dp)
                    .drawBehind {
                        val sw = 2.dp.toPx()
                        drawLine(line, Offset(0f, size.height - sw / 2), Offset(size.width, size.height - sw / 2), sw)
                    },
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (p.input.isNotEmpty()) Text(p.input, fontSize = 30.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.8.sp, color = text)
                Box(Modifier.size(2.dp, 30.dp).background(caret))
            }
            Row(Modifier.width(72.dp).padding(bottom = 10.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                Text("${p.remaining}", fontSize = 16.sp, style = Tnum)
                Text("/${p.deck.size}", fontSize = 13.sp, color = Palette.TeksRedup, style = Tnum)
            }
        }
        Text(
            "Tap the card to see its romaji", style = MaterialTheme.typography.bodySmall,
            color = Palette.TeksRedup.copy(alpha = 0.7f), textAlign = TextAlign.Center,
        )
    }
}
