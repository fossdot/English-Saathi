package net.bodhya.speak.desktop.data

import kotlinx.serialization.Serializable

/**
 * One student on the shared Mac (docs/decisions.md, P1): a card with a
 * colour and a drawn symbol the learner recognises, and a name the
 * facilitator types, used in Results and the CSV. No two learners have the
 * same card or the same name.
 */
@Serializable
data class Learner(
    val id: String,
    val name: String,
    val colour: CardColour,
    val symbol: CardSymbol,
    /** When the learner was added, the Mac's local time (2026-10-21T13:45:30+05:30). Orders the picker. */
    val created: String,
) {
    val card: Card get() = Card(colour, symbol)
}

data class Card(val colour: CardColour, val symbol: CardSymbol)

/** The card colours, drawn in `ui/Cards.kt`. Provisional until the desktop designs arrive. */
@Serializable
enum class CardColour { ORANGE, RED, PINK, PURPLE, BLUE, TEAL, GREEN, BROWN }

/** About 8 drawn symbols (docs/decisions.md, "Owner answers"), drawn in `ui/Cards.kt`. Provisional until the desktop designs arrive. */
@Serializable
enum class CardSymbol { SUN, MOON, STAR, FLOWER, LEAF, FISH, KITE, HOUSE }
