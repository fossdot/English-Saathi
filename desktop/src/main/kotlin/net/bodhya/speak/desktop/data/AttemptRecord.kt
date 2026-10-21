package net.bodhya.speak.desktop.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of the attempt log: every recording of a sentence or a fixing
 * step, including "Didn't hear you" (docs/decisions.md, P6). The learner is
 * the folder the row is saved in, so Results and the CSV always show the
 * learner's current name.
 */
@Serializable
data class AttemptRecord(
    /** When the result came, the Mac's local time with its offset (2026-10-21T13:45:30+05:30). */
    val time: String,
    val lesson: String,
    val sentence: String,
    /** "sentence", or "fix step 3" to "fix step 5", as in the pilot. */
    val mode: String,
    /** What the learner was asked to say. */
    val target: String,
    /** What the recognizer returned; empty when it wasn't run because nothing was loud enough. */
    val heard: String,
    /** Words of [target] said right, and how many count; null when nothing was heard. */
    val correct: Int? = null,
    val total: Int? = null,
    /** Sentence attempts only. */
    val stars: Int? = null,
    /** The speech model that heard it ("tiny.en", "small.en"); null when it wasn't run. */
    val model: String? = null,
    val result: AttemptResult,
    /** From the end of the recording to the result; null when the recognizer wasn't run. */
    val waitMs: Long? = null,
) {
    companion object {
        const val SENTENCE = "sentence"

        fun fixStep(step: Int) = "fix step $step"
    }
}

/** The CSV's `result` column. */
@Serializable
enum class AttemptResult(val text: String) {
    /** A sentence attempt that was scored. */
    @SerialName("scored") SCORED("scored"),

    /** A fixing step said right. */
    @SerialName("passed") PASSED("passed"),

    /** A fixing step said wrong. */
    @SerialName("failed") FAILED("failed"),

    /** "Didn't hear you": no score, no XP. */
    @SerialName("not heard") NOT_HEARD("not heard"),
}
