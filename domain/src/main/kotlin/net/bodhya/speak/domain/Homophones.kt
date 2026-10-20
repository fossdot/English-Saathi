package net.bodhya.speak.domain

/**
 * Words that sound the same and count as the same word when compared
 * (docs/decisions.md, "Homophones"). Whisper writes "to" as the digit 2,
 * which the normalizer spells out as "two". The rule is symmetric, and it
 * lives in the comparison, not the normalizer, so words keep their written
 * form everywhere else. The one table: add groups here after testing.
 */
object Homophones {
    val GROUPS: List<Set<String>> = listOf(
        setOf("to", "two", "too"),
        setOf("for", "four"),
        setOf("one", "won"),
    )

    /** True when [a] and [b] are different words of the same group. */
    fun same(a: String, b: String): Boolean = a != b && GROUPS.any { a in it && b in it }
}
