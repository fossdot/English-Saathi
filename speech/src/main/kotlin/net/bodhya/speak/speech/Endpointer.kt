package net.bodhya.speak.speech

import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * When to stop recording. The limits come from the spec: stop about 1.2 s
 * after the learner stops speaking, and never record more than 8 s.
 *
 * The levels that decide what counts as speech are starting values, tested
 * only on synthetic and emulator audio. They need checking on real phones.
 */
data class EndpointConfig(
    val sampleRate: Int = 16_000,
    val frameMs: Int = 20,
    val silenceAfterSpeechMs: Int = 1_200,
    val maxMs: Int = 8_000,
    /** Loud audio must last this long before speech counts as started, so clicks and taps don't. */
    val speechStartMs: Int = 60,
    /** A frame is loud when it is this many dB above the background level... */
    val aboveBackgroundDb: Double = 12.0,
    /** ...and also above this level, so faint hiss never counts as speech. */
    val minSpeechDbfs: Double = -55.0,
    /** How fast the background estimate may rise: it follows growing noise, but not speech. */
    val backgroundRiseDbPerSecond: Double = 2.5,
    /**
     * The first moments are not used for the background level: a Mac's
     * microphone can start with a moment far quieter than the room, which
     * would make the room itself sound like speech for many seconds.
     */
    val startupMs: Int = 100,
) {
    init {
        require(sampleRate > 0 && frameMs > 0 && sampleRate * frameMs % 1000 == 0) { "frames must be whole samples" }
        require(maxMs >= frameMs && maxMs % frameMs == 0) { "the limit must be a whole number of frames" }
        require(silenceAfterSpeechMs > 0 && speechStartMs >= frameMs && startupMs >= 0)
    }

    val maxFrames: Int get() = maxMs / frameMs

    val frameSamples: Int get() = sampleRate * frameMs / 1000
}

/**
 * What the endpointer saw in one recording, for the log: levels in dBFS
 * ([Endpointer.SILENT_DBFS] when there was nothing but digital silence).
 */
data class EndpointStats(
    /** Quietest and loudest frames that weren't exact zeros. */
    val quietestDbfs: Double,
    val loudestDbfs: Double,
    /** The quietest frame of the start-up moments, which the background ignores. */
    val startupQuietestDbfs: Double,
    /** The background estimate at the end. */
    val backgroundDbfs: Double,
    /** When loud audio had lasted long enough to count as speech; null if it never did. */
    val speechStartMs: Int?,
    /** Loud sounds too short to be speech (clicks, taps, breaths). */
    val shortSounds: Int,
) {
    override fun toString() = "levels ${f(quietestDbfs)}..${f(loudestDbfs)} dBFS, start-up quietest ${f(startupQuietestDbfs)}, " +
        "background ${f(backgroundDbfs)}, speech from ${speechStartMs?.let { "$it ms" } ?: "never"}, short sounds $shortSounds"

    private fun f(x: Double) = if (x.isNaN()) "-" else "%.0f".format(x)
}

enum class EndReason {
    /** The learner spoke, then was quiet for [EndpointConfig.silenceAfterSpeechMs]. */
    SILENCE_AFTER_SPEECH,

    /** [EndpointConfig.maxMs] reached. */
    MAX_LENGTH,

    /** The learner tapped stop. */
    STOPPED,
}

/**
 * Decides frame by frame whether the learner has started speaking and
 * whether they have finished. Each call to [accept] takes one frame of
 * [EndpointConfig.frameSamples] samples.
 *
 * A frame is loud when its level is [EndpointConfig.aboveBackgroundDb] above
 * the background. The background estimate drops at once to any quieter
 * frame and otherwise rises slowly; the first [EndpointConfig.startupMs] are
 * left out of it.
 *
 * Speech is loud audio lasting [EndpointConfig.speechStartMs]. Only speech
 * restarts the [EndpointConfig.silenceAfterSpeechMs] of quiet: a click, a tap
 * or a breath does not.
 */
class Endpointer(private val config: EndpointConfig = EndpointConfig()) {
    private var backgroundDbfs = Double.NaN
    private var loudFrames = 0
    private var quietFrames = 0
    private var frames = 0

    /** True once loud audio has lasted [EndpointConfig.speechStartMs]. */
    var speechStarted = false
        private set

    /** False while every frame so far was exact digital silence, as from a blocked microphone. */
    var anySound = false
        private set

    /** Level of the last frame, in dBFS (0 is full scale), for the level meter. */
    var lastLevelDbfs = SILENT_DBFS
        private set

    private var quietest = Double.NaN
    private var loudest = Double.NaN
    private var startupQuietest = Double.NaN
    private var speechStartFrame: Int? = null
    private var shortSounds = 0

    /** What this endpointer has seen so far. */
    val stats: EndpointStats
        get() = EndpointStats(quietest, loudest, startupQuietest, backgroundDbfs, speechStartFrame?.let { it * config.frameMs }, shortSounds)

    /** Takes the next frame; returns why recording should stop now, or null to go on. */
    fun accept(frame: FloatArray): EndReason? {
        require(frame.size == config.frameSamples) { "expected ${config.frameSamples} samples, got ${frame.size}" }
        val level = levelDbfs(frame)
        lastLevelDbfs = level
        frames++

        // Exact digital silence (some devices start with zeros) says nothing about the room.
        if (level > SILENT_DBFS) {
            anySound = true
            if (quietest.isNaN() || level < quietest) quietest = level
            if (loudest.isNaN() || level > loudest) loudest = level
            if (frames * config.frameMs <= config.startupMs && (startupQuietest.isNaN() || level < startupQuietest)) startupQuietest = level
            if (frames * config.frameMs > config.startupMs) {
                val rise = config.backgroundRiseDbPerSecond * config.frameMs / 1000
                backgroundDbfs = if (backgroundDbfs.isNaN() || level < backgroundDbfs) level else backgroundDbfs + rise
            }
        }
        val loud = !backgroundDbfs.isNaN() &&
            level > max(backgroundDbfs + config.aboveBackgroundDb, config.minSpeechDbfs)

        if (!loud && loudFrames > 0 && loudFrames * config.frameMs < config.speechStartMs) shortSounds++
        loudFrames = if (loud) loudFrames + 1 else 0
        if (loudFrames * config.frameMs >= config.speechStartMs) {
            if (!speechStarted) speechStartFrame = frames - loudFrames
            speechStarted = true
            quietFrames = 0
        } else {
            quietFrames++
        }

        return when {
            speechStarted && quietFrames * config.frameMs >= config.silenceAfterSpeechMs -> EndReason.SILENCE_AFTER_SPEECH
            frames >= config.maxFrames -> EndReason.MAX_LENGTH
            else -> null
        }
    }

    companion object {
        /** The level reported for exact digital silence. */
        const val SILENT_DBFS = -100.0

        fun levelDbfs(frame: FloatArray): Double {
            var sum = 0.0
            for (x in frame) sum += x.toDouble() * x
            val rms = sqrt(sum / frame.size)
            return if (rms == 0.0) SILENT_DBFS else max(SILENT_DBFS, 20 * log10(rms))
        }
    }
}
