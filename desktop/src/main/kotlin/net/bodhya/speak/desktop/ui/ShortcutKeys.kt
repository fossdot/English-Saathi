package net.bodhya.speak.desktop.ui

import androidx.compose.ui.input.key.Key

/** The pilot's keyboard shortcuts. */
enum class Shortcut {
    /** Space: press and speak, or stop listening. */
    SPEAK,

    /** L: listen. */
    LISTEN,

    /** S: listen slowly. */
    SLOWLY,

    /** Enter: the screen's main button. */
    PRIMARY,
}

/**
 * Turns key presses into [Shortcut]s the way the pilot does: only without
 * ⌘, Ctrl or Option, never while typing in a text field, and not again
 * while a key is held down (a held key repeats). One per window.
 */
class ShortcutKeys {
    private val held = mutableSetOf<Key>()

    /** A key went [down] or up. Returns the shortcut to run, if any. */
    fun onKey(key: Key, down: Boolean, withModifiers: Boolean, typing: Boolean): Shortcut? {
        if (!down) {
            held -= key
            return null
        }
        if (!held.add(key)) return null // a repeat of a held key
        if (withModifiers || typing) return null
        return when (key) {
            Key.Spacebar -> Shortcut.SPEAK
            Key.L -> Shortcut.LISTEN
            Key.S -> Shortcut.SLOWLY
            Key.Enter, Key.NumPadEnter -> Shortcut.PRIMARY
            else -> null
        }
    }
}
