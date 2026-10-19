package net.bodhya.speak.speech

import net.bodhya.speak.speech.Signals.concat
import net.bodhya.speak.speech.Signals.noise
import net.bodhya.speak.speech.Signals.speech
import net.bodhya.speak.speech.Signals.toPcm
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import kotlin.concurrent.thread

class RecorderTest {

    @Test
    fun `records until 1,2 s after the speech, then closes the source`() {
        val clip = toPcm(concat(noise(500, -70.0), speech(1000, -20.0)))
        val source = ArrayPcmSource(clip)
        val recording = Recorder(source).record()
        assertEquals(EndReason.SILENCE_AFTER_SPEECH, recording.endReason)
        assertTrue(recording.speechHeard)
        assertTrue("${recording.durationMs} ms", recording.durationMs in 2_550L..2_700L)
        assertTrue(source.closed)
        // The audio is what the source delivered, then the silence after it.
        for (i in clip.indices) assertEquals(clip[i] / 32768f, recording.samples[i], 0f)
    }

    @Test
    fun `nothing loud means speech was not heard`() {
        val recording = Recorder(ArrayPcmSource(toPcm(noise(9000, -70.0)))).record()
        assertEquals(EndReason.MAX_LENGTH, recording.endReason)
        assertFalse(recording.speechHeard)
        assertEquals(8 * 16_000, recording.samples.size)
    }

    @Test
    fun `stop ends the recording after the current frame`() {
        lateinit var recorder: Recorder
        var frames = 0
        recorder = Recorder(ArrayPcmSource(toPcm(noise(9000, -70.0))))
        val recording = recorder.record {
            frames++
            if (frames == 25) recorder.stop()
        }
        assertEquals(EndReason.STOPPED, recording.endReason)
        assertEquals(500L, recording.durationMs)
    }

    @Test
    fun `stopping after speaking keeps what was said`() {
        val source = ArrayPcmSource(toPcm(concat(noise(300, -70.0), speech(1000, -20.0), noise(3000, -70.0, seed = 2))))
        lateinit var recorder: Recorder
        var frames = 0
        recorder = Recorder(source)
        val recording = recorder.record { if (++frames == 70) recorder.stop() }
        assertEquals(EndReason.STOPPED, recording.endReason)
        assertTrue(recording.speechHeard)
        assertEquals(1_400L, recording.durationMs)
        assertTrue(source.closed)
    }

    @Test
    fun `stop before recording gives an empty recording and still closes the source`() {
        val source = ArrayPcmSource(toPcm(noise(1000, -70.0)))
        val recorder = Recorder(source)
        recorder.stop()
        val recording = recorder.record()
        assertEquals(EndReason.STOPPED, recording.endReason)
        assertEquals(0, recording.samples.size)
        assertFalse(recording.speechHeard)
        assertFalse("too short to call the microphone blocked", recording.noSound)
        assertTrue(source.closed)
    }

    @Test
    fun `a quick stop during the microphone's start-up zeros is not a blocked microphone`() {
        lateinit var recorder: Recorder
        var frames = 0
        recorder = Recorder(ArrayPcmSource(ShortArray(0)))
        val recording = recorder.record { if (++frames == 15) recorder.stop() }
        assertEquals(EndReason.STOPPED, recording.endReason)
        assertFalse(recording.noSound)
    }

    @Test
    fun `a blocked microphone is told apart from a quiet room`() {
        val blocked = Recorder(ArrayPcmSource(ShortArray(0))).record()
        assertEquals(EndReason.MAX_LENGTH, blocked.endReason)
        assertEquals(8 * 16_000, blocked.samples.size)
        assertTrue(blocked.noSound)
        val quiet = Recorder(ArrayPcmSource(toPcm(noise(9000, -70.0)))).record()
        assertFalse(quiet.noSound)
        assertFalse(quiet.speechHeard)
    }

    /** A source that never delivers audio, as a stalled microphone. */
    private class StalledSource : PcmSource {
        var closed = false
        override val sampleRate = 16_000
        override fun start() {}
        override fun read(buffer: ShortArray, offset: Int, size: Int) = 0
        override fun close() {
            closed = true
        }
    }

    @Test
    fun `a microphone that stops delivering audio is an error, not an endless wait`() {
        val source = StalledSource()
        val start = System.nanoTime()
        try {
            Recorder(source).record()
            fail("expected an IOException")
        } catch (e: IOException) {
            val ms = (System.nanoTime() - start) / 1_000_000
            assertTrue("gave up after $ms ms", ms in 500..3_000)
            assertTrue(source.closed)
        }
    }

    @Test
    fun `stop works while waiting for audio`() {
        val recorder = Recorder(StalledSource())
        thread {
            Thread.sleep(50)
            recorder.stop()
        }
        val recording = recorder.record()
        assertEquals(EndReason.STOPPED, recording.endReason)
        assertEquals(0, recording.samples.size)
        assertFalse(recording.noSound)
    }

    @Test
    fun `reports the level after every frame`() {
        val levels = mutableListOf<Float>()
        val recording = Recorder(ArrayPcmSource(toPcm(concat(noise(200, -70.0), speech(300, -20.0))))).record { levels += it }
        assertEquals(recording.samples.size / 320, levels.size)
        assertEquals(0f, levels.first(), 0f)
        assertTrue(levels.max() > 0.5f)
    }

    @Test
    fun `short reads are joined into whole frames`() {
        val clip = toPcm(concat(noise(500, -70.0), speech(1000, -20.0)))
        val whole = Recorder(ArrayPcmSource(clip)).record()
        val inner = ArrayPcmSource(clip)
        val dribble = object : PcmSource {
            override val sampleRate = 16_000
            override fun start() = inner.start()
            override fun read(buffer: ShortArray, offset: Int, size: Int) = inner.read(buffer, offset, minOf(size, 7))
            override fun close() = inner.close()
        }
        val split = Recorder(dribble).record()
        assertArrayEquals(whole.samples, split.samples, 0f)
    }

    @Test
    fun `a failed read is an error, and the source is still closed`() {
        val source = object : PcmSource {
            var closed = false
            override val sampleRate = 16_000
            override fun start() {}
            override fun read(buffer: ShortArray, offset: Int, size: Int) = -3
            override fun close() {
                closed = true
            }
        }
        try {
            Recorder(source).record()
            fail("expected an IOException")
        } catch (e: IOException) {
            assertTrue(source.closed)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `the source must deliver 16 kHz`() {
        Recorder(ArrayPcmSource(ShortArray(0), sampleRate = 44_100))
    }

    @Test
    fun `samples are scaled to -1 to 1`() {
        val clip = shortArrayOf(Short.MIN_VALUE, 16_384, 0, Short.MAX_VALUE)
        val recording = Recorder(ArrayPcmSource(clip), EndpointConfig(maxMs = 20)).record()
        assertEquals(-1f, recording.samples[0], 0f)
        assertEquals(0.5f, recording.samples[1], 0f)
        assertEquals(0f, recording.samples[2], 0f)
        assertEquals(32767 / 32768f, recording.samples[3], 0f)
    }

    @Test
    fun `the meter maps -60 to 0 dBFS onto 0 to 1`() {
        assertEquals(0f, Recorder.meterLevel(-100.0), 0f)
        assertEquals(0f, Recorder.meterLevel(-60.0), 0f)
        assertEquals(0.5f, Recorder.meterLevel(-30.0), 1e-6f)
        assertEquals(1f, Recorder.meterLevel(3.0), 0f)
    }
}
