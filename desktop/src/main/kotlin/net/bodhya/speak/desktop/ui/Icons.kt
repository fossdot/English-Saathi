package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The pilot's icons (docs/pilot, `const I`), drawn from the same SVG paths
 * on its 24-unit grid. `rect`s are written as paths.
 */
object Icons {
    val speaker = lines("speaker", "M4 9h4l5-4v14l-5-4H4z", "M16.5 8.5a5 5 0 0 1 0 7", "M19 6a8.5 8.5 0 0 1 0 12")
    val mic = lines("mic", "M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z", "M5 11a7 7 0 0 0 14 0", "M12 18v3")
    val stop = filled("stop", "M8 6h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z")
    val star = filled("star", "M12 3l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3 6.4 20.2l1.1-6.2L3 9.6l6.2-.9z")
    val flame = lines("flame", "M12 3c1 4 5 5.5 5 10a5 5 0 0 1-10 0c0-2.5 1.5-4 2.5-5 .3 2 1.5 3 2.5 3 0-3-1-5 0-8z")
    val check = lines("check", "M5 12.5l4.5 4.5L19 7.5", width = 3f)
    val retry = lines("retry", "M4 12a8 8 0 1 0 2.5-5.8", "M4 4v4h4", width = 2.6f)
    val dash = lines("dash", "M6 12h12", width = 2.6f)
    val close = lines("close", "M6 6l12 12M18 6L6 18", width = 2.4f)
    val back = lines("back", "M19 12H5", "M11 6l-6 6 6 6", width = 2.4f)
    val play = filled("play", "M8 5l11 7-11 7z")
    val lock = lines("lock", "M7 11h10a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z", "M8 11V7a4 4 0 0 1 8 0v4")
    val chart = lines("chart", "M4 20V10M10 20V4M16 20v-7M22 20H2")
}

/** A 24-unit icon drawn with round-ended lines, like the pilot's `stroke` icons. */
internal fun lines(name: String, vararg paths: String, width: Float = 2f): ImageVector = vector(name, paths, fill = false, width)

/** A 24-unit icon drawn as solid shapes, like the pilot's `fill` icons. */
internal fun filled(name: String, vararg paths: String): ImageVector = vector(name, paths, fill = true, 0f)

private fun vector(name: String, paths: Array<out String>, fill: Boolean, width: Float): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        for (d in paths) {
            addPath(
                addPathNodes(d),
                fill = if (fill) SolidColor(Color.Black) else null,
                stroke = if (fill) null else SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** An icon in one colour. */
@Composable
fun Icon(icon: ImageVector, size: Dp, color: Color, modifier: Modifier = Modifier, description: String? = null) {
    Image(rememberVectorPainter(icon), description, modifier.size(size), colorFilter = ColorFilter.tint(color))
}

/** Three stars with [n] of them lit, as the pilot shows results ("3 of 3 stars" for screen readers and tests). */
@Composable
fun Stars(n: Int, size: Dp, modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "$n of 3 stars" }) {
        for (i in 1..3) Icon(Icons.star, size, if (i <= n) Colors.reward else Colors.line)
    }
}
