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
 *
 * The window [track]s every press and release before a text field can take
 * it, and asks for the [shortcut] only for presses that reach the window:
 * Return pressed in the name field and held on is a repeat, so it can't
 * start practice on the next screen.
 */
class ShortcutKeys {
    private val held = mutableSetOf<Key>()

    /** Whether the last press [track]ed was a repeat of a held key. */
    private var repeat = false

    /** Every press and release (the window's onPreviewKeyEvent). */
    fun track(key: Key, down: Boolean) {
        if (down) {
            repeat = !held.add(key)
        } else {
            held -= key
            repeat = false
        }
    }

    /** A press that reached the window (its onKeyEvent), already [track]ed: the shortcut to run, if any. */
    fun shortcut(key: Key, withModifiers: Boolean, typing: Boolean): Shortcut? {
        if (repeat || withModifiers || typing) return null
        return when (key) {
            Key.Spacebar -> Shortcut.SPEAK
            Key.L -> Shortcut.LISTEN
            Key.S -> Shortcut.SLOWLY
            Key.Enter, Key.NumPadEnter -> Shortcut.PRIMARY
            else -> null
        }
    }

    /** [track] and then [shortcut], for a press or release nothing else took. */
    fun onKey(key: Key, down: Boolean, withModifiers: Boolean, typing: Boolean): Shortcut? {
        track(key, down)
        return if (down) shortcut(key, withModifiers, typing) else null
    }
}
