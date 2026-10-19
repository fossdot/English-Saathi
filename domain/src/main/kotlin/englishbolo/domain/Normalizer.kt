package englishbolo.domain

/** A space-separated piece of a sentence as written ("Patna,") and the words it stands for. */
data class TextToken(val text: String, val words: List<String>)

/**
 * Turns lesson text and recognizer output into comparable words, with the same
 * rules for both sides (docs/decisions.md):
 *
 * - lowercase; hyphens separate words ("bus-stop" = "bus stop");
 * - keep only a–z and 0–9 in each token, so apostrophes and other punctuation
 *   go ("don't" = "dont");
 * - ignore tokens with nothing left (a lone "?" or "—");
 * - spell out tokens made only of digits ("21" → "twenty one").
 *
 * Recognizer output also loses its non-speech tags first ([normalizeHeard]).
 */
object Normalizer {

    /** JavaScript's `\s`, as in the pilot, so Unicode spaces separate words too. */
    private val WHITESPACE = Regex(
        "[\\t\\n\\u000B\\f\\r \\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+",
    )

    /**
     * Non-speech tags the recognizer writes, such as `[BLANK_AUDIO]` or
     * `(music)`. Only innermost bracket pairs match, so an unclosed bracket
     * can't swallow the speech after it.
     */
    private val NON_SPEECH_TAG = Regex("\\[[^\\[\\]]*\\]|\\([^()]*\\)")

    /** The normalized words of lesson text (a sentence, chunk or word). */
    fun normalize(text: String): List<String> = text.split(WHITESPACE).flatMap(::tokenWords)

    /**
     * The normalized words of recognizer output. Non-speech tags are replaced
     * by a space first (repeatedly, so nested tags go too), so they never join
     * the words around them. No words at all means "Didn't hear you".
     */
    fun normalizeHeard(text: String): List<String> = normalize(withoutTags(text))

    private tailrec fun withoutTags(text: String): String {
        val stripped = text.replace(NON_SPEECH_TAG, " ")
        return if (stripped == text) text else withoutTags(stripped)
    }

    /** The sentence split as written, each piece with its words (for word chips). */
    fun tokens(text: String): List<TextToken> =
        text.split(WHITESPACE).filter { it.isNotEmpty() }.map { TextToken(it, tokenWords(it)) }

    /** The words one space-separated token stands for: none for "—", two for "bus-stop". */
    fun tokenWords(token: String): List<String> =
        token.lowercase().split('-').flatMap { part ->
            val kept = part.filter { it in 'a'..'z' || it in '0'..'9' }
            when {
                kept.isEmpty() -> emptyList()
                kept.all { it in '0'..'9' } -> NumberWords.spell(kept) ?: listOf(kept)
                else -> listOf(kept)
            }
        }
}

/** Spells out whole numbers the way they are compared: "120" → one hundred twenty (no "and"). */
internal object NumberWords {

    private val SMALL = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
    )
    private val TENS = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    /** Numbers up to 999,999,999 are spelled out; larger ones stay digits. */
    private const val MAX_DIGITS = 9

    /** Words for a string of ASCII digits, or null if its value has more than 9 digits. */
    fun spell(digits: String): List<String>? {
        val significant = digits.trimStart('0')
        if (significant.length > MAX_DIGITS) return null
        return words(significant.ifEmpty { "0" }.toInt())
    }

    private fun words(n: Int): List<String> = when {
        n < 20 -> listOf(SMALL[n])
        n < 100 -> listOf(TENS[n / 10]) + rest(n % 10)
        n < 1_000 -> listOf(SMALL[n / 100], "hundred") + rest(n % 100)
        n < 1_000_000 -> words(n / 1_000) + "thousand" + rest(n % 1_000)
        else -> words(n / 1_000_000) + "million" + rest(n % 1_000_000)
    }

    private fun rest(n: Int): List<String> = if (n == 0) emptyList() else words(n)
}
