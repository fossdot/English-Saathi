package englishbolo.domain

/** What follows a finished (or skipped) sentence. */
sealed interface AfterSentence {
    /** The daily goal was just reached: show the lesson-done screen. */
    data object GoalComplete : AfterSentence

    /** Carry on with the sentence at [index]. */
    data class Next(val index: Int) : AfterSentence
}

/**
 * Chooses which sentence to practise: sentences with words due for revision
 * first, then the pilot's order (docs/decisions.md). `current` is the index of
 * the sentence last shown (0 when none has been shown yet).
 */
object SentencePicker {

    /**
     * After Next, Skip or a fix: the first sentence not done today that
     * contains a [dueWords] word (lesson order); otherwise the first sentence
     * after [current] (wrapping) not done today; if all are done, the one after
     * [current].
     */
    fun next(sentences: List<Sentence>, current: Int, doneToday: Collection<String>, dueWords: Set<String>): Int {
        require(current in sentences.indices) { "current sentence $current is not in 0..${sentences.size - 1}" }
        withDueWord(sentences, doneToday, dueWords)?.let { return it }
        for (k in 1..sentences.size) {
            val i = (current + k) % sentences.size
            if (sentences[i].id !in doneToday) return i
        }
        return (current + 1) % sentences.size
    }

    /**
     * "Start practice" / "One more sentence": a sentence with a due word
     * first, otherwise the first sentence not done today; if all are done, the
     * one after [current].
     */
    fun start(sentences: List<Sentence>, current: Int, doneToday: Collection<String>, dueWords: Set<String>): Int {
        require(current in sentences.indices) { "current sentence $current is not in 0..${sentences.size - 1}" }
        withDueWord(sentences, doneToday, dueWords)?.let { return it }
        val firstNotDone = sentences.indexOfFirst { it.id !in doneToday }
        return if (firstNotDone >= 0) firstNotDone else (current + 1) % sentences.size
    }

    private fun withDueWord(sentences: List<Sentence>, doneToday: Collection<String>, dueWords: Set<String>): Int? {
        if (dueWords.isEmpty()) return null
        val i = sentences.indexOfFirst { s -> s.id !in doneToday && s.targetWords.any { it in dueWords } }
        return if (i >= 0) i else null
    }
}
