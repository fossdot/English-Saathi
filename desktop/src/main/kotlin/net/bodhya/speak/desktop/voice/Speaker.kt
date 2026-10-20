package net.bodhya.speak.desktop.voice

import net.bodhya.speak.domain.Lesson
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/** Speech ready to play: 16-bit mono samples at [sampleRate]. */
class Speech(val samples: ShortArray, val sampleRate: Int) {
    val durationMs: Long get() = samples.size * 1000L / sampleRate
    val bytes: Long get() = samples.size * 2L
}

/**
 * Speaks text with a macOS voice at a given speed: the pilot's 0.9 for
 * sentences, 0.6 for "Slowly", 0.95 for Hindi help (docs/desktop-plan.md,
 * D2). [render] (normally `say`, about a second per text) runs once per
 * text; the other speeds are stretched from that in milliseconds.
 *
 * When a lesson opens, [prepareLesson] makes everything its screens can play
 * ahead of time, so Listen is instant even where `say` is slow. Speech lives
 * in memory only: nothing is written to disk except `say`'s temporary file,
 * which [SayRenderer] deletes as soon as it is read.
 *
 * [prepare], [prepareLesson] and [play] block: call them off the main
 * thread. [stop] may be called from any thread, and must be before the
 * microphone opens.
 */
class Speaker(private val render: (Voice, String) -> ShortArray) {

    constructor(renderer: SayRenderer = SayRenderer()) : this(renderer::render)

    private data class Key(val voice: Voice, val text: String, val speed: Float)

    /** Least recently used first; the oldest entries go once there are [CACHE_SIZE]. */
    private class Lru<K, V> : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>) = size > CACHE_SIZE
    }

    private val rendered = Lru<Pair<Voice, String>, ShortArray>()
    private val cache = Lru<Key, Speech>()

    /** The open lesson's speech, and its texts at 1×, kept whole until another lesson opens. */
    @Volatile
    private var lessonSpeech: Map<Key, Speech> = emptyMap()

    @Volatile
    private var lessonRendered: Map<Pair<Voice, String>, ShortArray> = emptyMap()

    @Volatile
    private var line: SourceDataLine? = null

    @Volatile
    private var stopped = false

    /** [text] spoken by [voice] at [speed]: prepared with the lesson, cached, or made now. */
    fun prepare(voice: Voice, text: String, speed: Float): Speech {
        val key = Key(voice, text, speed)
        lessonSpeech[key]?.let { return it }
        synchronized(cache) { cache[key] }?.let { return it }
        val speech = stretch(renderedOnce(voice, text), speed)
        synchronized(cache) { cache[key] = speech }
        return speech
    }

    /**
     * Prepares everything [lesson]'s screens can play ([LessonSpeech]) with
     * [voice], replacing the previous lesson's speech. Runs `say`
     * [PARALLEL_RENDERS] at a time. Returns the bytes now held in memory.
     */
    fun prepareLesson(voice: Voice, lesson: Lesson): Long {
        val requests = LessonSpeech.requests(lesson)
        val texts = requests.map { it.text }.distinct()
        val pool = Executors.newFixedThreadPool(PARALLEL_RENDERS) { Thread(it, "say").apply { isDaemon = true } }
        val plain = try {
            texts.map { text -> pool.submit(Callable { text to render(voice, text) }) }.associate { it.get() }
        } finally {
            pool.shutdown()
        }
        val speech = requests.associate { Key(voice, it.text, it.speed) to stretch(plain.getValue(it.text), it.speed) }
        lessonSpeech = speech
        lessonRendered = plain.mapKeys { (text, _) -> voice to text }
        return speech.values.sumOf { it.bytes } + plain.values.sumOf { it.size * 2L }
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

    private fun renderedOnce(voice: Voice, text: String): ShortArray =
        lessonRendered[voice to text]
            ?: synchronized(rendered) { rendered[voice to text] }
            ?: render(voice, text).also { synchronized(rendered) { rendered[voice to text] = it } }

    private fun stretch(plain: ShortArray, speed: Float) =
        Speech(TimeStretch.stretch(plain, SayRenderer.SAMPLE_RATE, speed), SayRenderer.SAMPLE_RATE)

    private companion object {
        const val CACHE_SIZE = 64
        const val PARALLEL_RENDERS = 3
        const val CHUNK_BYTES = 4_410 // 0.1 s at 22,050 Hz: stop() takes effect within that
    }
}
