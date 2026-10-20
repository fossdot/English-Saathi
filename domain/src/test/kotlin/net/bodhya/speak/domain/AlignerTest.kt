package net.bodhya.speak.domain

import net.bodhya.speak.domain.WordStatus.CORRECT
import net.bodhya.speak.domain.WordStatus.MISSING
import net.bodhya.speak.domain.WordStatus.WRONG
import org.junit.Assert.assertEquals
import org.junit.Test

/** Expected labels below were checked against the web pilot's `align()`. */
class AlignerTest {

    private fun align(target: String, heard: String, accept: Map<String, Set<String>> = emptyMap()) =
        Aligner.align(Normalizer.normalize(target), Normalizer.normalizeHeard(heard), accept)

    private fun Alignment.statuses() = words.map { it.status }

    @Test
    fun `wrong and missing words - owner's example`() {
        val a = align("Where is the bus stop?", "where is bas stop")
        assertEquals(listOf("where", "is", "the", "bus", "stop"), a.words.map { it.word })
        assertEquals(listOf(CORRECT, CORRECT, MISSING, WRONG, CORRECT), a.statuses())
        assertEquals(emptyList<String>(), a.extra)
    }

    @Test
    fun `digits and accepted spellings count as correct - owner's example`() {
        val a = align("One ticket to Patna, please.", "1 ticket to patana please", mapOf("patna" to setOf("patana")))
        assertEquals(List(5) { CORRECT }, a.statuses())
    }

    @Test
    fun `a word stopped short is missing`() {
        assertEquals(listOf(CORRECT, CORRECT, CORRECT, CORRECT, MISSING), align("Where is the bus stop?", "where is the bus").statuses())
    }

    @Test
    fun `a wrong last word pairs with the last target word`() {
        // Equal-cost alignments: the backtrace prefers pairing from the end.
        assertEquals(listOf(CORRECT, CORRECT, CORRECT, MISSING, WRONG), align("Where is the bus stop?", "where is the bas").statuses())
    }

    @Test
    fun `extra words are recorded separately and cost nothing`() {
        val front = align("Where is the bus stop?", "excuse me where is the bus stop")
        assertEquals(List(5) { CORRECT }, front.statuses())
        assertEquals(listOf("excuse", "me"), front.extra)

        val end = align("Where is the bus stop?", "where is the bus stop please")
        assertEquals(listOf("please"), end.extra)

        val repeated = align("Where is the bus stop?", "where where is the bus stop")
        assertEquals(List(5) { CORRECT }, repeated.statuses())
        assertEquals(listOf("where"), repeated.extra)
    }

    @Test
    fun `swapped neighbours are both wrong`() {
        val a = align("One ticket to Patna, please.", "ticket one to patna please")
        assertEquals(listOf(WRONG, WRONG, CORRECT, CORRECT, CORRECT), a.statuses())
        assertEquals(emptyList<String>(), a.extra)
    }

    @Test
    fun `a word moved far is missing in place and extra where it was said`() {
        val a = align("Where is the bus stop?", "where the bus stop is")
        assertEquals(listOf(CORRECT, MISSING, CORRECT, CORRECT, CORRECT), a.statuses())
        assertEquals(listOf("is"), a.extra)
    }

    @Test
    fun `several missing words in the middle`() {
        assertEquals(listOf(CORRECT, CORRECT, MISSING, MISSING, CORRECT), align("How much is the ticket?", "how much ticket").statuses())
    }

    @Test
    fun `nothing heard makes every word missing`() {
        assertEquals(List(5) { MISSING }, align("Where is the bus stop?", "").statuses())
    }

    @Test
    fun `empty target makes every heard word extra`() {
        val a = Aligner.align(emptyList(), listOf("x", "y"))
        assertEquals(emptyList<WordResult>(), a.words)
        assertEquals(listOf("x", "y"), a.extra)
    }

    @Test
    fun `on equal cost, a missing target word is preferred over an extra heard word`() {
        // Checked against the pilot: a reordered sentence pairs from the end first.
        val a = align("Where is the bus stop?", "the bus stop where is the")
        assertEquals(listOf(CORRECT, CORRECT, CORRECT, MISSING, MISSING), a.statuses())
        assertEquals(listOf("the", "bus", "stop"), a.extra)
    }

    @Test
    fun `plain swap of two words`() {
        assertEquals(listOf(WRONG, WRONG), Aligner.align(listOf("a", "b"), listOf("b", "a")).statuses())
    }

    @Test
    fun `accepted spellings belong to the target word only`() {
        val accept = mapOf("patna" to setOf("patana"))
        assertEquals(listOf(CORRECT), Aligner.align(listOf("patna"), listOf("patana"), accept).statuses())
        assertEquals(listOf(WRONG), Aligner.align(listOf("patana"), listOf("patna"), accept).statuses())
    }

    @Test
    fun `every target word gets exactly one label and extras keep heard order`() {
        val a = Aligner.align(listOf("a", "b", "c"), listOf("x", "a", "y", "c", "z"))
        assertEquals(3, a.words.size)
        assertEquals(listOf(CORRECT, WRONG, CORRECT), a.statuses())
        assertEquals(listOf("x", "z"), a.extra)
    }

    @Test
    fun `homophones match in both directions - owner's example`() {
        // Whisper writes "to" as 2, which is spelled out as "two".
        assertEquals(listOf(CORRECT, CORRECT, CORRECT, CORRECT, CORRECT), align("one ticket to patna please", "1 ticket 2 patna please").statuses())
        assertEquals(listOf(CORRECT, CORRECT), align("two tickets", "to tickets").statuses())
        assertEquals(listOf(CORRECT, CORRECT), align("for me", "4 me").statuses())
        assertEquals(listOf(CORRECT), align("won", "one").statuses())
        assertEquals(listOf(CORRECT), align("too", "two").statuses())
    }

    @Test
    fun `homophones are only the listed groups, and words keep their written form`() {
        assertEquals(listOf(WRONG), align("to", "ta").statuses())
        assertEquals(listOf(WRONG), align("for", "fore").statuses())
        assertEquals(listOf(WRONG), align("one", "two").statuses())
        assertEquals(listOf("to"), align("to", "2").words.map { it.word })
        assertEquals(listOf("two"), Normalizer.normalize("2"))
    }

    @Test
    fun `unscored words stay aligned but don't count`() {
        val a = align("one ticket to patna please", "one ticket to but now please").withUnscored(setOf("patna"))
        assertEquals(listOf(CORRECT, CORRECT, CORRECT, WRONG, CORRECT), a.statuses())
        assertEquals(listOf(true, true, true, false, true), a.words.map { it.scored })
        assertEquals(4, a.correctCount)
        assertEquals(4, a.total)
    }
}
