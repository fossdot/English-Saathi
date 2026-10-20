package net.bodhya.speak.desktop.voice

import net.bodhya.speak.domain.Lesson

/** The pilot's speech speeds (CONFIG.normalRate, CONFIG.slowRate, and its Hindi help). */
object Speeds {
    const val NORMAL = 0.9f
    const val SLOW = 0.6f
    const val HINDI_HELP = 0.95f
}

/** One thing to say, at one speed. */
data class SpeechRequest(val text: String, val speed: Float)

/**
 * Everything a lesson's screens can play in English, as in the pilot: each
 * sentence at both speeds (Listen, Slowly, "Hear the correct sentence"),
 * each chunk at both (fixing step 4), and each word slowly (fixing steps
 * 1–3 play the word at 0.6 for both L and S). Sentences come first, since
 * they are needed first.
 */
object LessonSpeech {
    fun requests(lesson: Lesson): List<SpeechRequest> {
        val sentences = lesson.sentences.flatMap { s -> listOf(SpeechRequest(s.text, Speeds.NORMAL), SpeechRequest(s.text, Speeds.SLOW)) }
        val words = lesson.sentences.flatMap { it.targetWords }.distinct().map { SpeechRequest(it, Speeds.SLOW) }
        val chunks = lesson.sentences.flatMap { it.chunks }.filter { it.isNotBlank() }.distinct()
            .flatMap { listOf(SpeechRequest(it, Speeds.NORMAL), SpeechRequest(it, Speeds.SLOW)) }
        return (sentences + words + chunks).distinct()
    }
}
