package net.bodhya.speak.domain

/**
 * A situation (e.g. Bus stand) with its practice sentences.
 *
 * [words] is the lesson's one word table, keyed by normalized word
 * ("patna" for "Patna,"), like the pilot's `WORDS`: sentences use it by
 * their words' text (docs/decisions.md, "Word data scope"). A word without
 * an entry gets the generic tip ([FeedbackSelector.tip]) and is its own
 * single syllable ([syllablesOf]).
 */
data class Lesson(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val sentences: List<Sentence>,
    val words: Map<String, WordInfo> = emptyMap(),
) {
    init {
        require(sentences.isNotEmpty()) { "lesson $id has no sentences" }
        val duplicates = sentences.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "lesson $id repeats sentence ids $duplicates" }
        for (key in words.keys) {
            require(Normalizer.normalize(key) == listOf(key)) { "lesson $id: word key \"$key\" is not a normalized word" }
        }
    }

    /**
     * Alternate spellings per target word, normalized, for [Aligner.align].
     * Entries that are not a single word are ignored (multi-word alternates are
     * not supported).
     */
    val accept: Map<String, Set<String>>
        get() = words
            .mapValues { (_, info) -> info.accept.mapNotNull { Normalizer.normalize(it).singleOrNull() }.toSet() }
            .filterValues { it.isNotEmpty() }

    /** Syllables to show for [word] at fixing step 2: its own entry, or the word itself. */
    fun syllablesOf(word: String): List<String> = words[word]?.syllables?.takeIf { it.isNotEmpty() } ?: listOf(word)

    /**
     * The words of [sentence] that are not scored: those whose entry says
     * `"score": false` (place names) and those the sentence lists as
     * [Sentence.unscored].
     */
    fun unscoredIn(sentence: Sentence): Set<String> =
        sentence.targetWords.filter { words[it]?.score == false || it in sentence.unscored }.toSet()
}

/**
 * One practice sentence. Its teaching data comes from the lesson's word
 * table; [unscored] lists words the recognizer can't be expected to hear in
 * this sentence, although they are scored elsewhere. The override can only
 * turn scoring off.
 */
data class Sentence(
    val id: String,
    val text: String,
    val hi: String,
    val chunks: List<String>,
    val picture: String? = null,
    val unscored: Set<String> = emptySet(),
) {
    init {
        require(Normalizer.normalize(text).isNotEmpty()) { "sentence $id has no English words: \"$text\"" }
        for (word in unscored) {
            require(Normalizer.normalize(word) == listOf(word)) { "sentence $id: unscored word \"$word\" is not a normalized word" }
        }
    }

    /** The words the learner must say, normalized. */
    val targetWords: List<String> get() = Normalizer.normalize(text)
}

/**
 * Teaching data for one word of a lesson. [hintHi] is the Devanagari sound
 * hint ("बस"), and [hintTag] selects the tip template.
 *
 * [accept] lists other spellings the recognizer may produce for a correctly
 * said word. [score] false means the word is never scored (place names such
 * as Patna and Bettiah, which the recognizer rarely hears right): it shows
 * as a neutral chip, isn't counted, is never the word to fix and never
 * enters the Leitner box.
 */
data class WordInfo(
    val syllables: List<String>,
    val hintHi: String? = null,
    val hintTag: String? = null,
    val accept: List<String> = emptyList(),
    val score: Boolean = true,
)

/** A tip template from `hints.json`, keyed by hint tag. */
data class HintTemplate(val tag: String, val textHi: String, val reviewed: Boolean)

/** All tip templates. Unknown or missing tags fall back to [GENERIC]. */
class Hints(templates: Collection<HintTemplate>) {
    private val byTag = templates.associateBy { it.tag }

    init {
        require(GENERIC in byTag) { "hints must include the \"$GENERIC\" tag" }
    }

    fun forTag(tag: String?): HintTemplate = byTag[tag ?: GENERIC] ?: byTag.getValue(GENERIC)

    companion object {
        const val GENERIC = "generic"
    }
}
