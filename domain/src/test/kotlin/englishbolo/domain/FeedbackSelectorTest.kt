package englishbolo.domain

import englishbolo.domain.WordStatus.CORRECT
import englishbolo.domain.WordStatus.MISSING
import englishbolo.domain.WordStatus.WRONG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FeedbackSelectorTest {

    private val config = GameConfig()
    private val today = LocalDate.of(2026, 10, 19)

    private fun alignment(sentence: Sentence, heard: String) =
        (Practice.attemptSentence(LearnerProgress(), sentence, heard, today, config) as SentenceAttempt.Scored).alignment

    private fun statuses(vararg s: WordStatus) = Alignment(s.mapIndexed { i, st -> WordResult("w$i", st) }, emptyList())

    @Test
    fun `a word said wrong is fixed before a missing one - owner's example`() {
        // the → MISSING comes first, but bus → WRONG is the word to fix.
        assertEquals(WordToFix(3, "bus"), FeedbackSelector.wordToFix(alignment(BusStand.whereIsTheBusStop, "where is bas stop")))
    }

    @Test
    fun `the first wrong word in sentence order`() {
        assertEquals(WordToFix(1, "w1"), FeedbackSelector.wordToFix(statuses(MISSING, WRONG, CORRECT, WRONG)))
    }

    @Test
    fun `a missing word is fixed when nothing was said wrong`() {
        assertEquals(WordToFix(4, "please"), FeedbackSelector.wordToFix(alignment(BusStand.oneTicketToPatna, "one ticket to patna")))
        assertEquals(WordToFix(2, "w2"), FeedbackSelector.wordToFix(statuses(CORRECT, CORRECT, MISSING, MISSING)))
    }

    @Test
    fun `nothing to fix when every word is right`() {
        assertNull(FeedbackSelector.wordToFix(alignment(BusStand.whereIsTheBusStop, "where is the bus stop please")))
        assertNull(FeedbackSelector.wordToFix(statuses()))
    }

    @Test
    fun `praise follows the stars`() {
        assertEquals(Praise.EXCELLENT, FeedbackSelector.praise(3))
        assertEquals(Praise.VERY_GOOD, FeedbackSelector.praise(2))
        assertEquals(Praise.GOOD_TRY, FeedbackSelector.praise(1))
    }

    @Test
    fun `the tip uses the word's sound hint and its tag's template`() {
        val tip = FeedbackSelector.tip("bus", BusStand.whereIsTheBusStop, BusStand.hints)
        assertEquals(Tip("bus", "बस", BusStand.hints.forTag("short_vowel")), tip)
    }

    @Test
    fun `a word without teaching data gets the generic tip`() {
        val tip = FeedbackSelector.tip("is", BusStand.whereIsTheBusStop, BusStand.hints)
        assertEquals(Tip("is", null, BusStand.hints.forTag("generic")), tip)
    }

    @Test
    fun `a tag without a template falls back to the generic tip`() {
        val sentence = BusStand.whereIsTheBusStop.copy(words = mapOf("zero" to WordInfo(listOf("ze", "ro"), "ज़ीरो", "z_j")))
        assertEquals("generic", FeedbackSelector.tip("zero", sentence, BusStand.hints).template.tag)
    }

    @Test
    fun `an empty sound hint or tag counts as none`() {
        val sentence = BusStand.whereIsTheBusStop.copy(words = mapOf("bus" to WordInfo(listOf("bus"), "", "")))
        assertEquals(Tip("bus", null, BusStand.hints.forTag("generic")), FeedbackSelector.tip("bus", sentence, BusStand.hints))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hints must include the generic template`() {
        Hints(listOf(HintTemplate("th", "th tip", reviewed = false)))
    }
}
