package net.bodhya.speak.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol

/**
 * A learner's card: a white symbol on a coloured disc (docs/decisions.md,
 * P1). Colours and symbols are provisional until the desktop designs
 * arrive; the colours differ in lightness as well as hue, and the symbol
 * tells cards apart where colours look alike.
 */
object Cards {
    fun colour(c: CardColour): Color = when (c) {
        CardColour.ORANGE -> Color(0xFFC25E00)
        CardColour.RED -> Color(0xFFB3261E)
        CardColour.PINK -> Color(0xFFC2185B)
        CardColour.PURPLE -> Color(0xFF6A3FA0)
        CardColour.BLUE -> Color(0xFF2C3E9E)
        CardColour.TEAL -> Color(0xFF00737A)
        CardColour.GREEN -> Color(0xFF2E7D32)
        CardColour.BROWN -> Color(0xFF6D4C41)
    }

    /** Hindi names, for the facilitator ("the blue fish"); not shown to learners. */
    fun nameHi(s: CardSymbol): String = when (s) {
        CardSymbol.SUN -> "सूरज"
        CardSymbol.MOON -> "चाँद"
        CardSymbol.STAR -> "तारा"
        CardSymbol.FLOWER -> "फूल"
        CardSymbol.LEAF -> "पत्ता"
        CardSymbol.FISH -> "मछली"
        CardSymbol.KITE -> "पतंग"
        CardSymbol.HOUSE -> "घर"
    }

    /** Drawn on the pilot's 24-unit grid, in its line style. */
    fun symbol(s: CardSymbol): ImageVector = symbols.getValue(s)

    private val symbols = mapOf(
        CardSymbol.SUN to lines(
            "sun", "M16 12a4 4 0 1 1-8 0a4 4 0 1 1 8 0z",
            "M19 12h2.5M16.95 16.95l1.77 1.77M12 19v2.5M7.05 16.95l-1.77 1.77M5 12H2.5M7.05 7.05L5.28 5.28M12 5V2.5M16.95 7.05l1.77-1.77",
        ),
        CardSymbol.MOON to lines("moon", "M11.1 3.55A8.5 8.5 0 1 0 20.29 13.89A7 7 0 0 1 11.1 3.55z"),
        CardSymbol.STAR to lines("star", "M12 3.2L14.35 9.56L21.13 9.83L15.8 14.04L17.64 20.57L12 16.8L6.36 20.57L8.2 14.04L2.87 9.83L9.65 9.56z"),
        CardSymbol.FLOWER to lines(
            "flower", "M14 12a2 2 0 1 1-4 0a2 2 0 1 1 4 0z",
            "M14.8 6a2.8 2.8 0 1 1-5.6 0a2.8 2.8 0 1 1 5.6 0z", "M20.51 10.15a2.8 2.8 0 1 1-5.6 0a2.8 2.8 0 1 1 5.6 0z",
            "M18.33 16.85a2.8 2.8 0 1 1-5.6 0a2.8 2.8 0 1 1 5.6 0z", "M11.27 16.85a2.8 2.8 0 1 1-5.6 0a2.8 2.8 0 1 1 5.6 0z",
            "M9.09 10.15a2.8 2.8 0 1 1-5.6 0a2.8 2.8 0 1 1 5.6 0z",
        ),
        CardSymbol.LEAF to lines("leaf", "M5 19C5 10 10 5 19 5C19 14 14 19 5 19z", "M5 19L13 11"),
        CardSymbol.FISH to lines("fish", "M3 12C6 7 12 6 16 9L21 6V18L16 15C12 18 6 17 3 12z", "M8 11h.01"),
        CardSymbol.KITE to lines("kite", "M12 2L19 10L12 18L5 10z", "M5 10h14M12 2v16", "M12 18q-1.5 1 0 2q1.5 1 0 2"),
        CardSymbol.HOUSE to lines("house", "M3 11L12 3L21 11", "M5 9.5V21H19V9.5", "M10 21v-6h4v6"),
    )
}

/** A learner's card as a disc, [size] across. */
@Composable
fun CardBadge(card: Card, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).background(Cards.colour(card.colour), CircleShape)
            .semantics { contentDescription = "${card.colour.name.lowercase()} ${card.symbol.name.lowercase()}" },
        contentAlignment = Alignment.Center,
    ) { Icon(Cards.symbol(card.symbol), size * 0.6f, Colors.onPrimary) }
}
