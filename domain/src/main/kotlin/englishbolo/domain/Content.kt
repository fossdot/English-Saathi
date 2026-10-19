package englishbolo.domain

/** A situation (e.g. Bus stand) with its practice sentences. */
data class Lesson(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val sentences: List<Sentence>,
) {
    init {
        require(sentences.isNotEmpty()) { "lesson $id has no sentences" }
        val duplicates = sentences.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) { "lesson $id repeats sentence ids $duplicates" }
    }
}

/**
 * One practice sentence.
 *
 * [words] holds teaching data keyed by normalized word ("patna" for
 * "Patna,"). A word without an entry gets the generic tip
 * ([FeedbackSelector.tip]) and is its own single syllable ([syllablesOf]).
 */
data class Sentence(
    val id: String,
    val text: String,
    val hi: String,
    val chunks: List<String>,
    val picture: String? = null,
    val words: Map<String, WordInfo> = emptyMap(),
) {
    init {
        require(Normalizer.normalize(text).isNotEmpty()) { "sentence $id has no English words: \"$text\"" }
        for (key in words.keys) {
            require(Normalizer.normalize(key) == listOf(key)) { "sentence $id: word key \"$key\" is not a normalized word" }
        }
    }

    /** The words the learner must say, normalized. */
    val targetWords: List<String> get() = Normalizer.normalize(text)

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
}

/**
 * Teaching data for one word of a sentence. [hintHi] is the Devanagari sound
 * hint ("बस"), and [hintTag] selects the tip template.
 *
 * [accept] lists other spellings the recognizer may produce for a correctly
 * said word (mostly place names, e.g. "betiya" for "bettiah").
 */
data class WordInfo(
    val syllables: List<String>,
    val hintHi: String? = null,
    val hintTag: String? = null,
    val accept: List<String> = emptyList(),
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
