package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.domain.WordResult
import net.bodhya.speak.domain.WordStatus

/** One chip per word as written ("Patna"), coloured by how it was said; unscored words are neutral. */
@Composable
fun WordChips(text: String, words: List<WordResult>) {
    var next = 0
    for (token in Normalizer.tokens(text)) {
        if (token.words.isEmpty()) continue
        val mine = words.subList(next, minOf(words.size, next + token.words.size))
        next += token.words.size
        val scored = mine.any { it.scored }
        val status = when {
            mine.any { it.status == WordStatus.WRONG } -> WordStatus.WRONG
            mine.any { it.status == WordStatus.MISSING } -> WordStatus.MISSING
            else -> WordStatus.CORRECT
        }
        WordChip(token.text.trim('.', ',', '?', '!'), if (scored) status else null)
    }
}

/**
 * The pilot's chip: check for right, retry for wrong, a dashed outline for
 * missing; [status] null is neutral. [marked] false leaves out the mark, as
 * the pilot's "Practise again tomorrow" does.
 */
@Composable
fun WordChip(label: String, status: WordStatus?, size: TextUnit = 26.sp, marked: Boolean = true) {
    val shape = RoundedCornerShape(12.dp)
    val (bg, fg, icon) = when (status) {
        WordStatus.CORRECT -> Triple(Colors.okSoft, Colors.okInk, Icons.check)
        WordStatus.WRONG -> Triple(Colors.badSoft, Colors.badInk, Icons.retry)
        WordStatus.MISSING -> Triple(Color.Transparent, Colors.badInk, Icons.dash)
        null -> Triple(Colors.bg, Colors.muted, null)
    }
    Box(
        Modifier.background(bg, shape)
            .then(if (status == WordStatus.WRONG) Modifier.border(2.dp, Colors.bad, shape) else Modifier)
            .then(if (status == WordStatus.MISSING) Modifier.drawBehind {
                drawRoundRect(Colors.bad, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))), cornerRadius = CornerRadius(12.dp.toPx()))
            } else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (marked) icon?.let { Icon(it, (size.value * 0.75f).dp, fg) }
            BasicText(label, style = textStyle(size, fg, FontWeight.SemiBold))
        }
    }
}
