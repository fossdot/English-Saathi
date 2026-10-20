package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The pilot's light colours (docs/pilot, `:root`). */
object Colors {
    val bg = Color(0xFFFFFCF6)
    val surface = Color(0xFFF4EFE4)
    val ink = Color(0xFF1D1B18)
    val muted = Color(0xFF5A554D)
    val line = Color(0xFFE4DED2)
    val primary = Color(0xFF2C3E9E)
    val onPrimary = Color(0xFFFFFFFF)
    val primarySoft = Color(0xFFE6E9F7)
    val primaryInk = Color(0xFF1F2D78)
    val reward = Color(0xFFC25E00)
    val rewardSoft = Color(0xFFFFF0DC)
    val okSoft = Color(0xFFE2F1E7)
    val okInk = Color(0xFF174F2F)
    val badSoft = Color(0xFFFCE4D6)
    val badInk = Color(0xFF8A2F08)
    val bad = Color(0xFFA33A0B)
}

/** English above, Hindi below, at least 16 sp (docs/decisions.md, "Labels"). */
@Composable
fun Bilingual(en: String, hi: String, modifier: Modifier = Modifier, size: TextUnit = 18.sp, color: Color = Colors.ink, bold: Boolean = false) {
    Column(modifier) {
        BasicText(en, style = TextStyle(fontSize = size, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal))
        BasicText(hi, style = TextStyle(fontSize = maxOf(16f, size.value * 0.85f).sp, color = color.copy(alpha = 0.8f)))
    }
}

/**
 * A button that can't take keyboard focus, so Space and Enter always reach
 * the window's shortcuts instead of clicking whatever button was last used.
 */
@Composable
fun PilotButton(en: String, hi: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    val fg = if (primary) Colors.onPrimary else Colors.primaryInk
    Box(
        modifier
            .heightIn(min = 60.dp)
            .focusProperties { canFocus = false }
            .then(if (primary) Modifier.background(if (enabled) Colors.primary else Colors.muted, shape) else Modifier.border(2.dp, Colors.primary, shape))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Bilingual(en, hi, size = 18.sp, color = if (enabled) fg else Colors.muted, bold = true)
    }
}

/** A short line of plain text. */
@Composable
fun Line(text: String, size: TextUnit = 16.sp, color: Color = Colors.ink, bold: Boolean = false) =
    BasicText(text, style = TextStyle(fontSize = size, color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal))

val Gap = Arrangement.spacedBy(12.dp)
