package englishbolo.ui

import englishbolo.domain.Aligner
import englishbolo.domain.Alignment
import englishbolo.domain.GameConfig
import englishbolo.domain.Normalizer
import englishbolo.domain.Score
import englishbolo.domain.Scorer
import englishbolo.speech.Recording

/** One recorded answer compared with its sentence. */
data class AnswerCheck(
    /** What the recognizer returned; null when it was not run. */
    val recognized: String?,
    /** Null means "Didn't hear you": no speech, or only non-speech tags such as [BLANK_AUDIO]. */
    val alignment: Alignment?,
    val score: Score?,
)

/**
 * Recognizes [recording] and compares it with [target] using the domain
 * rules. A recording in which nothing counted as speech never reaches the
 * recognizer, so Whisper can't invent words from silence.
 */
suspend fun checkAnswer(
    target: String,
    recording: Recording,
    config: GameConfig,
    transcribe: suspend (FloatArray) -> String,
): AnswerCheck {
    if (!recording.speechHeard) return AnswerCheck(recognized = null, alignment = null, score = null)
    val text = transcribe(recording.samples)
    val heard = Normalizer.normalizeHeard(text)
    if (heard.isEmpty()) return AnswerCheck(recognized = text, alignment = null, score = null)
    val alignment = Aligner.align(Normalizer.normalize(target), heard)
    return AnswerCheck(text, alignment, Scorer.score(alignment, config))
}
