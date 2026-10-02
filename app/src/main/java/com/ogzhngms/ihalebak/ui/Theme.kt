package com.ogzhngms.ihalebak.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// The "Ajanda" look: warm grey paper, near-black ink, cyan for what can be pressed, magenta for what is urgent.
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,       // secondary text
    val faint: Color,       // days without tenders, an unstarred star
    val line: Color,        // dividers and outlines
    val well: Color,        // the track behind the segment control
    val handle: Color,
    val accent: Color,
    val onAccent: Color,
    val accentText: Color,  // cyan dark enough to read as text
    val accentSoft: Color,
    val onAccentSoft: Color,
    val urgent: Color,
    val urgentSoft: Color,
    val star: Color,
)

private val Light = Palette(
    bg = Color(0xFFF3F2F2), surface = Color(0xFFEAE9E9), text = Color(0xFF201E1D), muted = Color(0xFF605D5D),
    faint = Color(0xFF9B9797), line = Color(0x29201E1D), well = Color(0xFFEAE7E7), handle = Color(0xFFBAB6B6),
    accent = Color(0xFF0088B0), onAccent = Color(0xFFF3F2F2), accentText = Color(0xFF006786),
    accentSoft = Color(0xFFE9F8FF), onAccentSoft = Color(0xFF0A303E),
    urgent = Color(0xFFAA0B56), urgentSoft = Color(0xFFFFF1F4), star = Color(0xFFD6006C),
)

// The design is drawn in light only; the dark palette keeps its roles with the same hues turned down.
private val Dark = Palette(
    bg = Color(0xFF161514), surface = Color(0xFF24211F), text = Color(0xFFF3F2F2), muted = Color(0xFFBAB6B6),
    faint = Color(0xFF7D7979), line = Color(0x33F3F2F2), well = Color(0xFF2D2B2B), handle = Color(0xFF605D5D),
    accent = Color(0xFF38A6CF), onAccent = Color(0xFF0A1A20), accentText = Color(0xFF99E0FF),
    accentSoft = Color(0xFF0A303E), onAccentSoft = Color(0xFFCBEEFF),
    urgent = Color(0xFFFF90B1), urgentSoft = Color(0xFF4B1528), star = Color(0xFFFF458E),
)

private val LocalPalette = staticCompositionLocalOf { Light }

val palette: Palette
    @Composable get() = LocalPalette.current

// The design asks for Inter; until its font file is added to the project the system's sans-serif stands in.
private val Sans = FontFamily.Default

// A text style at one of the design's sizes. Headings are semibold with slightly tightened letters.
fun type(size: Int, weight: FontWeight = FontWeight.Normal, lineHeight: Float = 1.4f, tight: Boolean = false) = TextStyle(
    fontFamily = Sans,
    fontSize = size.sp,
    fontWeight = weight,
    lineHeight = (size * lineHeight).sp,
    letterSpacing = if (tight) (-0.02).em else 0.em,
)

fun heading(size: Int, lineHeight: Float = 1.15f) = type(size, FontWeight.SemiBold, lineHeight, tight = true)

@Composable
fun IhaleBakTheme(content: @Composable () -> Unit) {
    val p = if (isSystemInDarkTheme()) Dark else Light
    // Material's own pieces (the sheet, the dialog, the switch, ripples) take their colours from here.
    val scheme = (if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()).copy(
        primary = p.accent, onPrimary = p.onAccent, primaryContainer = p.accentSoft, onPrimaryContainer = p.onAccentSoft,
        background = p.bg, onBackground = p.text, surface = p.bg, onSurface = p.text,
        surfaceVariant = p.surface, onSurfaceVariant = p.muted, surfaceContainer = p.bg, surfaceContainerHigh = p.bg,
        surfaceContainerLow = p.bg, outline = p.faint, outlineVariant = p.line, error = p.urgent, errorContainer = p.urgentSoft,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
