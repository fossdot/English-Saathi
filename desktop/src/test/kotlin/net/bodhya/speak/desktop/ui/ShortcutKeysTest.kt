package net.bodhya.speak.desktop.ui

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutKeysTest {
    private val keys = ShortcutKeys()

    private fun press(key: Key, modifiers: Boolean = false, typing: Boolean = false) = keys.onKey(key, true, modifiers, typing)
    private fun release(key: Key) = keys.onKey(key, false, false, false)

    @Test
    fun `the pilot's keys`() {
        assertEquals(Shortcut.SPEAK, press(Key.Spacebar)).also { release(Key.Spacebar) }
        assertEquals(Shortcut.LISTEN, press(Key.L)).also { release(Key.L) }
        assertEquals(Shortcut.SLOWLY, press(Key.S)).also { release(Key.S) }
        assertEquals(Shortcut.PRIMARY, press(Key.Enter)).also { release(Key.Enter) }
        assertEquals(Shortcut.PRIMARY, press(Key.NumPadEnter))
        assertNull(press(Key.A))
    }

    @Test
    fun `a held key fires once`() {
        assertEquals(Shortcut.SPEAK, press(Key.Spacebar))
        assertNull(press(Key.Spacebar))
        assertNull(press(Key.Spacebar))
        release(Key.Spacebar)
        assertEquals(Shortcut.SPEAK, press(Key.Spacebar))
    }

    @Test
    fun `not with modifiers, and not while typing`() {
        assertNull(press(Key.L, modifiers = true))
        release(Key.L)
        assertNull(press(Key.S, typing = true))
    }
}
