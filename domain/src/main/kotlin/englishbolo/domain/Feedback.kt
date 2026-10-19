package englishbolo.domain

/** Headline on the feedback screen, by stars. */
enum class Praise { EXCELLENT, VERY_GOOD, GOOD_TRY }

/** The word to fix: its position among the sentence's words, and the word. */
data class WordToFix(val index: Int, val word: String)

/**
 * The one tip shown for the word to fix: the word, its Devanagari sound hint
 * (if any) and the tip template for its hint tag.
 */
data class Tip(val word: String, val soundHi: String?, val template: HintTemplate)

/** Picks what the feedback screen shows. */
object FeedbackSelector {

    /**
     * The word to fix and tip: the first word said WRONG, or if none, the first
     * MISSING word. Null when every word is correct.
     */
    fun wordToFix(alignment: Alignment): WordToFix? {
        val words = alignment.words
        val index = words.indexOfFirst { it.status == WordStatus.WRONG }.takeIf { it >= 0 }
            ?: words.indexOfFirst { it.status == WordStatus.MISSING }.takeIf { it >= 0 }
            ?: return null
        return WordToFix(index, words[index].word)
    }

    fun praise(stars: Int): Praise = when (stars) {
        3 -> Praise.EXCELLENT
        2 -> Praise.VERY_GOOD
        else -> Praise.GOOD_TRY
    }

    /**
     * Tip for [word]. A word without teaching data, or whose tag has no
     * template, gets the generic tip. (The pilot showed "undefined" for a tag
     * without a template.)
     */
    fun tip(word: String, sentence: Sentence, hints: Hints): Tip {
        val info = sentence.words[word]
        return Tip(word, info?.hintHi?.takeIf { it.isNotEmpty() }, hints.forTag(info?.hintTag?.takeIf { it.isNotEmpty() }))
    }
}
