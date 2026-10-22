package net.bodhya.speak.desktop.data

/**
 * Lesson and sentence ids in saved data. The app keeps them with their
 * pack's id in front (`starter.bus_01`, docs/decisions.md, "Lesson review:
 * owner answers"). Data saved before that has bare ids, which can only be
 * the first pack's (Bus stand was the only lesson in use), so they are read
 * as that pack's. The next save writes them in full.
 */
object SavedIds {
    /** The pack every bare id belongs to. */
    const val FIRST_PACK = "starter"

    fun qualify(id: String): String = if ('.' in id) id else "$FIRST_PACK.$id"
}
