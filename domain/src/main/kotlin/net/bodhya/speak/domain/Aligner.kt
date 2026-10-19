package net.bodhya.speak.domain

/** How one word of the target sentence was said. */
enum class WordStatus { CORRECT, WRONG, MISSING }

/** One target word (normalized) and how it was said. */
data class WordResult(val word: String, val status: WordStatus)

/**
 * Result of comparing what was heard with the target.
 *
 * [words] has exactly one entry per target word, in target order. [extra] holds
 * heard words that were not matched to any target word, in heard order.
 */
data class Alignment(val words: List<WordResult>, val extra: List<String>) {
    val correctCount: Int get() = words.count { it.status == WordStatus.CORRECT }
    val total: Int get() = words.size
}

/**
 * Word-level Levenshtein alignment with backtrace, as in the pilot's `align()`.
 */
object Aligner {

    /**
     * Aligns normalized [heard] words against normalized [target] words.
     *
     * A heard word matches a target word when they are equal, or when the
     * heard word is in [accept] for that target word (alternate spellings, e.g.
     * "patana" for "patna"). Substitutions, missing and extra words all cost 1.
     *
     * When several alignments have the same cost, the backtrace (from the end)
     * prefers a match/substitution, then a missing target word, then an extra
     * heard word. For "where is bas stop" against "where is the bus stop" this
     * gives the → MISSING and bus → WRONG.
     */
    fun align(
        target: List<String>,
        heard: List<String>,
        accept: Map<String, Set<String>> = emptyMap(),
    ): Alignment {
        val n = target.size
        val m = heard.size
        fun same(t: String, h: String) = t == h || accept[t]?.contains(h) == true
        fun cost(i: Int, j: Int) = if (same(target[i - 1], heard[j - 1])) 0 else 1

        val d = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) d[i][0] = i
        for (j in 0..m) d[0][j] = j
        for (i in 1..n) {
            for (j in 1..m) {
                d[i][j] = minOf(d[i - 1][j - 1] + cost(i, j), d[i - 1][j] + 1, d[i][j - 1] + 1)
            }
        }

        val statuses = Array(n) { WordStatus.MISSING }
        val extra = ArrayDeque<String>()
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && d[i][j] == d[i - 1][j - 1] + cost(i, j)) {
                statuses[i - 1] = if (same(target[i - 1], heard[j - 1])) WordStatus.CORRECT else WordStatus.WRONG
                i--
                j--
            } else if (i > 0 && d[i][j] == d[i - 1][j] + 1) {
                statuses[i - 1] = WordStatus.MISSING
                i--
            } else {
                extra.addFirst(heard[j - 1])
                j--
            }
        }
        return Alignment(target.mapIndexed { k, w -> WordResult(w, statuses[k]) }, extra.toList())
    }
}
