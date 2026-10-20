package net.bodhya.speak.desktop.voice

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/** Speech ready to play: 16-bit mono samples at [sampleRate]. */
class Speech(val samples: ShortArray, val sampleRate: Int) {
    val durationMs: Long get() = samples.size * 1000L / sampleRate
}

/**
 * Speaks text with a macOS voice at a given speed: the pilot's 0.9 for
 * sentences, 0.6 for "Slowly", 0.95 for Hindi help (docs/desktop-plan.md,
 * D2). `say` renders each text once (about a second); the other speeds are
 * stretched from that in milliseconds. Everything is kept in memory for the
 * session, so a replay starts at once; nothing is written to disk except
 * `say`'s temporary file. Call [prepare] when a screen appears, so the
 * learner doesn't wait for `say` on the first Listen.
 *
 * [prepare] and [play] block: call them off the main thread. [stop] may be
 * called from any thread, and must be before the microphone opens.
 */
class Speaker(private val renderer: SayRenderer = SayRenderer()) {

    private data class Key(val voice: Voice, val text: String, val speed: Float)

    /** Least recently used first; the oldest entries go once there are [CACHE_SIZE]. */
    private class Lru<K, V> : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>) = size > CACHE_SIZE
    }

    private val rendered = Lru<Pair<Voice, String>, ShortArray>()
    private val cache = Lru<Key, Speech>()

    @Volatile
    private var line: SourceDataLine? = null

    @Volatile
    private var stopped = false

    /** [text] spoken by [voice] at [speed], from the cache or freshly rendered and stretched. */
    fun prepare(voice: Voice, text: String, speed: Float): Speech {
        val key = Key(voice, text, speed)
        synchronized(cache) { cache[key] }?.let { return it }
        val plain = synchronized(rendered) { rendered[voice to text] }
            ?: renderer.render(voice, text).also { synchronized(rendered) { rendered[voice to text] = it } }
        val speech = Speech(TimeStretch.stretch(plain, SayRenderer.SAMPLE_RATE, speed), SayRenderer.SAMPLE_RATE)
        synchronized(cache) { cache[key] = speech }
        return speech
    }

    /** Plays [speech] to the end, or until [stop]. One speech at a time: [stop] the current one to start another at once. */
    @Synchronized
    fun play(speech: Speech) {
        stopped = false
        val format = AudioFormat(speech.sampleRate.toFloat(), 16, 1, true, false)
        val out = AudioSystem.getSourceDataLine(format)
        out.open(format)
        line = out
        try {
            out.start()
            val bytes = ByteBuffer.allocate(speech.samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            bytes.asShortBuffer().put(speech.samples)
            val data = bytes.array()
            var pos = 0
            while (pos < data.size && !stopped) {
                pos += out.write(data, pos, minOf(CHUNK_BYTES, data.size - pos))
            }
            if (!stopped) out.drain()
        } finally {
            out.stop()
            out.flush()
            out.close()
            line = null
        }
    }

    /** Stops playback at once. Safe to call from any thread, and when nothing is playing. */
    fun stop() {
        stopped = true
        line?.let {
            it.stop()
            it.flush()
        }
    }

    private companion object {
        const val CACHE_SIZE = 64
        const val CHUNK_BYTES = 4_410 // 0.1 s at 22,050 Hz: stop() takes effect within that
    }
}
