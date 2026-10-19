package englishbolo.speech

import englishbolo.speech.Signals.concat
import englishbolo.speech.Signals.noise
import englishbolo.speech.Signals.speech
import englishbolo.speech.Signals.tone
import englishbolo.speech.Signals.zeros
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointerTest {

    private val config = EndpointConfig()

    /** Feeds [audio] frame by frame; returns when (ms) and why it said stop, and whether speech started. */
    private fun run(audio: FloatArray, endpointer: Endpointer = Endpointer(config)): Triple<Int?, EndReason?, Boolean> {
        val n = config.frameSamples
        for (f in 0 until audio.size / n) {
            val reason = endpointer.accept(audio.copyOfRange(f * n, (f + 1) * n))
            if (reason != null) return Triple((f + 1) * config.frameMs, reason, endpointer.speechStarted)
        }
        return Triple(null, null, endpointer.speechStarted)
    }

    @Test
    fun `stops 1,2 s after the learner stops speaking`() {
        val audio = concat(noise(500, -70.0), speech(1000, -20.0), noise(4000, -70.0, seed = 2))
        val (at, reason, speech) = run(audio)
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        assertTrue(speech)
        // Speech ends at 1500 ms; its last syllable fades a little earlier.
        assertTrue("stopped at $at", at!! in 2_550..2_700)
    }

    @Test
    fun `pauses shorter than 1,2 s between words don't stop the recording`() {
        val audio = concat(
            noise(400, -65.0),
            speech(500, -25.0), noise(1000, -65.0, seed = 2),
            speech(500, -25.0), noise(900, -65.0, seed = 3),
            speech(500, -25.0), noise(3000, -65.0, seed = 4),
        )
        val (at, reason, _) = run(audio)
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        val lastSpeechEnd = 400 + 500 + 1000 + 500 + 900 + 500
        assertTrue("stopped at $at", at!! > lastSpeechEnd + 1_000 && at <= lastSpeechEnd + 1_200)
    }

    @Test
    fun `speech from the very first frame is still found`() {
        val (at, reason, speech) = run(concat(speech(1500, -20.0), noise(3000, -65.0)))
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        assertTrue(speech)
        assertTrue("stopped at $at", at!! in 2_550..2_700)
    }

    @Test
    fun `silence runs to the 8 s limit without speech`() {
        val (at, reason, speech) = run(noise(9000, -65.0))
        assertEquals(8_000, at)
        assertEquals(EndReason.MAX_LENGTH, reason)
        assertFalse(speech)
    }

    @Test
    fun `talking for more than 8 s stops at the limit`() {
        val (at, reason, speech) = run(concat(noise(300, -65.0), speech(9000, -20.0)))
        assertEquals(8_000, at)
        assertEquals(EndReason.MAX_LENGTH, reason)
        assertTrue(speech)
    }

    @Test
    fun `a long steady sound is not mistaken for silence`() {
        // A held vowel: the background estimate must not climb up to it.
        val (at, reason, _) = run(concat(noise(500, -65.0), tone(3000, -20.0), noise(3000, -65.0, seed = 2)))
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        assertEquals(500 + 3000 + 1200, at)
    }

    @Test
    fun `a click is not speech`() {
        val (_, reason, speech) = run(concat(noise(500, -65.0), tone(40, -10.0), noise(8000, -65.0, seed = 2)))
        assertEquals(EndReason.MAX_LENGTH, reason)
        assertFalse(speech)
    }

    @Test
    fun `steady loud noise is not speech`() {
        val (_, reason, speech) = run(noise(9000, -30.0))
        assertEquals(EndReason.MAX_LENGTH, reason)
        assertFalse(speech)
    }

    @Test
    fun `speech is found over steady background noise`() {
        val audio = concat(noise(500, -45.0), speech(1000, -20.0) plus noise(1000, -45.0, seed = 2), noise(3000, -45.0, seed = 3))
        val (at, reason, speech) = run(audio)
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        assertTrue(speech)
        assertTrue("stopped at $at", at!! in 2_500..2_700)
    }

    @Test
    fun `faint hiss above the background is not speech`() {
        // 15 dB above a very quiet room, but below the -55 dBFS floor for speech.
        val (_, reason, speech) = run(concat(noise(500, -80.0), speech(1000, -65.0), noise(8000, -80.0, seed = 2)))
        assertEquals(EndReason.MAX_LENGTH, reason)
        assertFalse(speech)
    }

    @Test
    fun `quiet speech in a quiet room is found`() {
        val (_, reason, speech) = run(concat(noise(500, -75.0), speech(1000, -45.0), noise(3000, -75.0, seed = 2)))
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        assertTrue(speech)
    }

    @Test
    fun `digital silence at the start doesn't hide the room's background level`() {
        // Some phones deliver zeros while the microphone starts. The room here is
        // louder than the -55 dBFS floor, so only the measured background keeps
        // its noise from counting as speech.
        val audio = concat(zeros(300), noise(500, -45.0), speech(1000, -20.0) plus noise(1000, -45.0, seed = 2), noise(3000, -45.0, seed = 3))
        val (at, reason, speech) = run(audio)
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        assertTrue(speech)
        assertTrue("stopped at $at", at!! in 2_750..3_000)
    }

    @Test
    fun `background that rises and falls a few dB is not speech`() {
        // Like a fan or distant traffic: 6 dB swings, every 100 ms.
        val audio = concat(*Array(90) { noise(100, if (it % 2 == 0) -50.0 else -44.0, seed = it) })
        val (_, reason, speech) = run(audio)
        assertEquals(EndReason.MAX_LENGTH, reason)
        assertFalse(speech)
    }

    @Test
    fun `reports the level of the last frame`() {
        val e = Endpointer(config)
        e.accept(tone(20, -20.0))
        assertEquals(-20.0, e.lastLevelDbfs, 0.2)
        e.accept(zeros(20))
        assertEquals(Endpointer.SILENT_DBFS, e.lastLevelDbfs, 0.0)
    }

    @Test
    fun `separate taps don't add up to speech`() {
        val tap = tone(20, -10.0)
        val audio = concat(
            noise(500, -65.0), tap, noise(200, -65.0, seed = 2), tap, noise(200, -65.0, seed = 3), tap,
            noise(8000, -65.0, seed = 4),
        )
        val (_, reason, speech) = run(audio)
        assertEquals(EndReason.MAX_LENGTH, reason)
        assertFalse(speech)
    }

    @Test
    fun `nothing but digital silence runs to the limit and reports no sound`() {
        val e = Endpointer(config)
        val (at, reason, speech) = run(zeros(9000), e)
        assertEquals(8_000, at)
        assertEquals(EndReason.MAX_LENGTH, reason)
        assertFalse(speech)
        assertFalse(e.anySound)
    }

    @Test
    fun `any real sound counts as sound`() {
        val e = Endpointer(config)
        run(concat(zeros(1000), noise(20, -80.0), zeros(8000)), e)
        assertTrue(e.anySound)
    }

    @Test
    fun `when silence and the limit fall due together, silence after speech wins`() {
        // Speech ends exactly 1.2 s before the 8 s limit.
        val (at, reason, _) = run(concat(noise(300, -65.0), tone(6500, -20.0), noise(3000, -65.0, seed = 2)))
        assertEquals(8_000, at)
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
    }

    @Test
    fun `quiet speech after a loud start is still found`() {
        // A bump while the phone is picked up, then a quiet room and soft speech.
        val audio = concat(noise(200, -35.0), noise(300, -70.0, seed = 2), speech(1000, -45.0), noise(3000, -70.0, seed = 3))
        val (at, reason, speech) = run(audio)
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        assertTrue(speech)
        assertTrue("stopped at $at", at!! in 2_550..2_700)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `the limit must be a whole number of frames`() {
        EndpointConfig(frameMs = 30)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `frames must have the configured size`() {
        Endpointer(config).accept(FloatArray(100))
    }

    @Test
    fun `the limits come from the config`() {
        val short = EndpointConfig(silenceAfterSpeechMs = 400, maxMs = 2_000)
        val (at, reason, _) = run(concat(noise(200, -65.0), speech(500, -20.0), noise(2000, -65.0, seed = 2)), Endpointer(short))
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, reason)
        assertTrue("stopped at $at", at!! in 1_000..1_100)
        val (max, maxReason, _) = run(noise(3000, -65.0), Endpointer(short))
        assertEquals(2_000, max)
        assertEquals(EndReason.MAX_LENGTH, maxReason)
    }

    private infix fun FloatArray.plus(other: FloatArray) = FloatArray(size) { this[it] + other[it] }
}
