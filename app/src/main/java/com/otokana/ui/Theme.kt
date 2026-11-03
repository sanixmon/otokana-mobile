package com.otokana.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.otokana.R

/**
 * The desktop app's palette: paper on a desk. It is fixed, following neither
 * the system's light or dark appearance nor dynamic color.
 */
object Palette {
    val Meja = Color(0xFF22201C) // background (the desk)
    val Kertas = Color(0xFFEFEBE0) // sheets, selected boxes, primary button
    val Tinta = Color(0xFF2A2723) // kana and text on paper
    val TintaRedup = Color(0xFF6B665C) // note label and icons on paper
    val Teks = Color(0xFFE9E4D8) // main text on the desk
    val TeksRedup = Color(0xFFA39C8E) // secondary text
    val GarisKotak = Color(0xFF6E8F72) // frame of a practice box
    val Hijau = Color(0xFF9DB89F) // answer line while typing, caret, links
    val TintaHijau = Color(0xFF4F7A56) // hint romaji on paper
    val PenaMerah = Color(0xFFE8705F) // wrong input
    val Pengikat = Color(0xFF3F5A45) // binding strip of the pad
    val GarisKotakRedup = Color(0xFF4E4A43)
    val GarisBantuRedup = Color(0xFF3E3B35)
    val GarisMeja = Color(0xFF57524A)
    val GarisJawaban = Color(0xFF5B564D)
    val TeksKeycap = Color(0xFFCFC9BC)
    val GarisPemisah = Color(0xFF3A3731)
    val TepiKertas = listOf(Color(0xFFDAD4C6), Color(0xFFCDC6B6), Color(0xFFC0B8A6), Color(0xFFB3AA97), Color(0xFFA69D89))
}

val KanaFont = FontFamily(Font(R.font.klee_one_semibold, FontWeight.SemiBold))
val UiFont = FontFamily(
    Font(R.font.zen_regular, FontWeight.Normal),
    Font(R.font.zen_medium, FontWeight.Medium),
    Font(R.font.zen_bold, FontWeight.Bold),
)

private val colors = darkColorScheme(
    primary = Palette.Kertas,
    onPrimary = Palette.Tinta,
    primaryContainer = Palette.GarisKotakRedup,
    onPrimaryContainer = Palette.Teks,
    secondary = Palette.Hijau,
    onSecondary = Palette.Tinta,
    tertiary = Palette.GarisKotak,
    background = Palette.Meja,
    onBackground = Palette.Teks,
    surface = Palette.Meja,
    onSurface = Palette.Teks,
    surfaceVariant = Palette.GarisBantuRedup,
    onSurfaceVariant = Palette.TeksRedup,
    surfaceContainer = Color(0xFF2B2924),
    surfaceContainerHigh = Color(0xFF312E29),
    surfaceContainerHighest = Palette.GarisBantuRedup,
    outline = Palette.GarisMeja,
    outlineVariant = Palette.GarisPemisah,
    error = Palette.PenaMerah,
    onError = Palette.Tinta,
)

private val typography = Typography().let { t ->
    fun TextStyle.ui() = copy(fontFamily = UiFont)
    Typography(
        displayLarge = t.displayLarge.copy(fontFamily = KanaFont, fontWeight = FontWeight.SemiBold),
        displayMedium = t.displayMedium.copy(fontFamily = KanaFont, fontWeight = FontWeight.SemiBold),
        displaySmall = t.displaySmall.copy(fontFamily = KanaFont, fontWeight = FontWeight.SemiBold),
        headlineLarge = t.headlineLarge.copy(fontFamily = KanaFont, fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontFamily = KanaFont, fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontFamily = KanaFont, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = KanaFont, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.ui().copy(fontWeight = FontWeight.Medium),
        titleSmall = t.titleSmall.ui().copy(fontWeight = FontWeight.Medium),
        bodyLarge = t.bodyLarge.ui().copy(fontSize = 16.sp, lineHeight = 26.sp),
        bodyMedium = t.bodyMedium.ui().copy(fontSize = 15.sp, lineHeight = 23.sp),
        bodySmall = t.bodySmall.ui().copy(fontSize = 13.sp, lineHeight = 19.sp),
        labelLarge = t.labelLarge.ui().copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        labelMedium = t.labelMedium.ui().copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        labelSmall = t.labelSmall.ui(),
    )
}

@Composable
fun OtoTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
