package net.bodhya.speak.speech

import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** 16-bit mono audio coming in, from the microphone or (in tests) from memory. */
interface PcmSource : AutoCloseable {
    val sampleRate: Int

    /** Starts capturing. Throws [IOException] if the audio can't be opened. */
    fun start()

    /**
     * Blocks until audio is available and copies up to [size] samples into
     * [buffer] at [offset]. Returns how many were copied (0 if none came),
     * or a negative error code.
     */
    fun read(buffer: ShortArray, offset: Int, size: Int): Int
}

/** A finished recording: 16 kHz mono samples in [-1, 1]. */
class Recording(
    val samples: FloatArray,
    val sampleRate: Int,
    /** False if nothing loud enough to be speech was heard: "Didn't hear you". */
    val speechHeard: Boolean,
    val endReason: EndReason,
    /**
     * True if at least a second was recorded and every sample was exactly
     * zero. A real microphone always picks up some noise, so this means it is
     * blocked: switched off in the phone's privacy settings, or taken by a
     * call. (Shorter recordings say nothing: some phones start with zeros.)
     */
    val noSound: Boolean = false,
    /** What the endpointer saw, for the log. */
    val stats: EndpointStats? = null,
) {
    val durationMs: Long get() = samples.size * 1000L / sampleRate
}

/**
 * Records one answer from [source]: until the learner has been quiet for a
 * while after speaking, the time limit, or [stop]. One recorder per answer.
 */
class Recorder(
    private val source: PcmSource,
    private val config: EndpointConfig = EndpointConfig(),
) {
    private val stopRequested = AtomicBoolean(false)

    init {
        require(source.sampleRate == config.sampleRate) { "source is ${source.sampleRate} Hz, expected ${config.sampleRate}" }
    }

    /** Ends the recording after the current frame. Safe to call from any thread. */
    fun stop() = stopRequested.set(true)

    /**
     * Records until done and closes the source. Blocking: call it off the main
     * thread. [onLevel] gets the input level (0 to 1) after every frame, on
     * the recording thread.
     */
    fun record(onLevel: (Float) -> Unit = {}): Recording {
        val endpointer = Endpointer(config)
        val frameSize = config.frameSamples
        val pcm = ShortArray(frameSize)
        val frame = FloatArray(frameSize)
        val out = FloatArray(config.maxFrames * frameSize)
        var count = 0
        fun recording(end: EndReason) = Recording(
            out.copyOf(count), config.sampleRate, endpointer.speechStarted, end,
            noSound = !endpointer.anySound && count >= config.sampleRate,
            stats = endpointer.stats,
        )
        source.use { src ->
            src.start()
            while (true) {
                if (stopRequested.get() || !readFrame(src, pcm)) return recording(EndReason.STOPPED)
                for (i in 0 until frameSize) frame[i] = pcm[i] / 32768f
                frame.copyInto(out, count)
                count += frameSize
                val end = endpointer.accept(frame)
                onLevel(meterLevel(endpointer.lastLevelDbfs))
                if (end != null) return recording(end)
            }
        }
    }

    /**
     * Fills [pcm] with the next frame. Returns false if [stop] was called while
     * waiting. Throws if the source fails, or delivers nothing for a second.
     */
    private fun readFrame(src: PcmSource, pcm: ShortArray): Boolean {
        var filled = 0
        var emptyReads = 0
        while (filled < pcm.size) {
            val n = src.read(pcm, filled, pcm.size - filled)
            if (n < 0) throw IOException("microphone read failed (error $n)")
            if (n == 0) {
                if (stopRequested.get()) return false
                if (++emptyReads > MAX_EMPTY_READS) throw IOException("the microphone stopped delivering audio")
                Thread.sleep(EMPTY_READ_PAUSE_MS)
            }
            filled += n
        }
        return true
    }

    companion object {
        private const val EMPTY_READ_PAUSE_MS = 10L
        private const val MAX_EMPTY_READS = 100 // about a second

        /** Maps -60..0 dBFS to 0..1 for the level meter. */
        fun meterLevel(dbfs: Double): Float = ((dbfs + 60) / 60).coerceIn(0.0, 1.0).toFloat()
    }
}

/**
 * Plays [samples] as if they came from the microphone, then digital silence.
 * Used to run test recordings through the [Recorder] exactly like live audio.
 */
class ArrayPcmSource(private val samples: ShortArray, override val sampleRate: Int = 16_000) : PcmSource {
    private var position = 0
    var started = false
        private set
    var closed = false
        private set

    override fun start() {
        started = true
    }

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
        check(started && !closed) { "not recording" }
        val n = minOf(size, samples.size - position).coerceAtLeast(0)
        samples.copyInto(buffer, offset, position, position + n)
        buffer.fill(0, offset + n, offset + size)
        position += n
        return size
    }

    override fun close() {
        closed = true
    }
}
