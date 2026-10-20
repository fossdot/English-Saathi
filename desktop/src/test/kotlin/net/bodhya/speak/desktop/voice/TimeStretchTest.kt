package net.bodhya.speak.desktop.voice

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class TimeStretchTest {

    private val rate = 22_050

    /** [seconds] of a [hz] tone, the way a voiced sound looks to Sonic. */
    private fun tone(hz: Double, seconds: Double) =
        ShortArray((rate * seconds).toInt()) { (8000 * sin(2 * PI * hz * it / rate)).toInt().toShort() }

    /** Zero crossings per second: twice the pitch of a pure tone. */
    private fun crossingsPerSecond(s: ShortArray): Double {
        var n = 0
        for (i in 1 until s.size) if ((s[i - 1] < 0) != (s[i] < 0)) n++
        return n * rate.toDouble() / s.size
    }

    @Test
    fun `slowly makes speech 1_67 times as long`() {
        val out = TimeStretch.stretch(tone(150.0, 1.0), rate, 0.6f)
        assertEquals(1.0 / 0.6, out.size.toDouble() / rate, 0.03)
    }

    @Test
    fun `the pitch stays the same`() {
        val input = tone(150.0, 1.0)
        val out = TimeStretch.stretch(input, rate, 0.6f)
        assertEquals(crossingsPerSecond(input), crossingsPerSecond(out), crossingsPerSecond(input) * 0.03)
    }

    @Test
    fun `speed 1 changes nothing`() {
        val input = tone(150.0, 0.3)
        assertArrayEquals(input, TimeStretch.stretch(input, rate, 1f))
    }

    @Test
    fun `say renders English and Hindi, and Slowly stretches them`() {
        assumeTrue("needs macOS and its say command", File("/usr/bin/say").canExecute())
        val renderer = SayRenderer()
        val voices = renderer.listVoices()
        val english = voices.choose(VoiceRole.ENGLISH)
        val hindi = voices.choose(VoiceRole.HINDI)
        assumeTrue("needs an Indian English and a Hindi voice", english != null && hindi != null)
        val speaker = Speaker(renderer)

        val word = speaker.prepare(english!!, "ticket", 1f)
        val slow = speaker.prepare(english, "ticket", 0.6f)
        assertEquals(1.0 / 0.6, slow.durationMs.toDouble() / word.durationMs, 0.05)

        // Hindi goes to say as UTF-8: a Hindi voice reads it, so it lasts seconds, not a fraction of one.
        val help = speaker.prepare(hindi!!, "पहले वाक्य सुनिए। फिर नारंगी माइक बटन दबाइए।", 1f)
        assertEquals(true, help.durationMs > 1500)
    }
}
