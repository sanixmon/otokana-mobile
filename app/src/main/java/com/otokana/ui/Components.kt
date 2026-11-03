package com.otokana.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otokana.store.Voicebank

/** Kana, or hint romaji, in the handwriting font. */
@Composable
fun KanaText(text: String, size: TextUnit, color: Color = Color.Unspecified, modifier: Modifier = Modifier) =
    Text(
        text, modifier, color = color, fontFamily = KanaFont, fontWeight = FontWeight.SemiBold,
        fontSize = size, lineHeight = size, textAlign = TextAlign.Center, maxLines = 1, softWrap = false,
    )

/** A dashed or solid rounded frame. */
fun Modifier.dashedBorder(width: Dp, color: Color, radius: Dp, dash: Dp = 6.dp) = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color, topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx() * 0.66f))),
    )
}

/**
 * A box of a Japanese writing workbook: a frame, of paper when [on],
 * see-through and dim when off; dashed for a stand-in sound.
 */
@Composable
fun PracticeBox(
    size: Dp, on: Boolean, kana: String, fontSize: TextUnit,
    modifier: Modifier = Modifier, dashed: Boolean = false, textColor: Color? = null,
) {
    val frame = if (on) Palette.GarisKotak else Palette.GarisKotakRedup
    var m = modifier.size(size)
    if (on) m = m.background(Palette.Kertas)
    m = if (dashed) m.dashedBorder(1.5.dp, frame, 0.dp, 3.dp) else m.border(1.5.dp, frame)
    Box(m, contentAlignment = Alignment.Center) {
        KanaText(kana, fontSize, textColor ?: if (on) Palette.Tinta else Palette.TeksRedup)
    }
}

/** How many sheet edges show under the top one: 5 for a full deck, none on the last card. */
fun sheetLayers(remaining: Int, total: Int): Int {
    if (remaining <= 1 || total <= 1) return 0
    val n = (5 * (remaining - 1) + total - 2) / (total - 1)
    return minOf(n, remaining - 1, 5)
}

/** The edges of [layers] sheets under a pad of the size of what follows. */
@Composable
fun PadEdges(layers: Int, modifier: Modifier = Modifier) {
    Box(modifier) {
        for (i in layers - 1 downTo 0) {
            val k = i + 1
            Box(
                Modifier.fillMaxSize().offset(y = (3 * k).dp).padding(horizontal = k.dp)
                    .clip(RoundedCornerShape(3.dp)).background(Palette.TepiKertas[i])
            )
        }
    }
}

/** The paper of a pad: shadow, paper, and the binding strip on top. */
fun Modifier.padPaper() = shadow(18.dp, RoundedCornerShape(3.dp), ambientColor = Color.Black, spotColor = Color.Black)
    .clip(RoundedCornerShape(3.dp)).background(Palette.Kertas)

@Composable
fun Binding() = Box(Modifier.fillMaxWidth().height(12.dp).background(Palette.Pengikat))

/** The logo: 音 in a practice box beside the name. */
@Composable
fun Logo() = Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    PracticeBox(30.dp, true, "音", 19.sp)
    Text("OtoKana", fontFamily = KanaFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(onBack: (() -> Unit)? = null, actions: @Composable () -> Unit = {}) {
    val colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, actionIconContentColor = Palette.TeksRedup, navigationIconContentColor = Palette.TeksRedup)
    TopAppBar(
        title = { Logo() },
        navigationIcon = {
            if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
        },
        actions = { actions() },
        colors = colors,
    )
}

/** The small grey label above a group. */
@Composable
fun GroupLabel(text: String) =
    Text(text, style = MaterialTheme.typography.labelMedium, color = Palette.TeksRedup)

/** A voicebank's picture, or its initials without one. */
@Composable
fun BankPicture(vb: Voicebank, size: Dp) {
    val bmp = remember(vb.id, vb.image, vb.dir) {
        vb.imageFile()?.let { runCatching { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }.getOrNull() }
    }
    if (bmp != null) {
        Image(bmp, null, Modifier.size(size).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
    } else {
        val initials = vb.name.split(' ', '　').filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()
        Box(Modifier.size(size).clip(CircleShape).background(Palette.GarisKotakRedup), contentAlignment = Alignment.Center) {
            Text(initials, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.Medium, color = Palette.Teks)
        }
    }
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
