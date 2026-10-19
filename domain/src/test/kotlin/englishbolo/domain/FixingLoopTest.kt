package englishbolo.domain

import englishbolo.domain.FixOutcome.DROPPED_BACK
import englishbolo.domain.FixOutcome.NOTHING_HEARD
import englishbolo.domain.FixOutcome.PASSED
import englishbolo.domain.FixOutcome.SENTENCE_FIXED
import englishbolo.domain.FixOutcome.TRY_AGAIN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FixingLoopTest {

    private val config = GameConfig()
    private val today = LocalDate.of(2026, 10, 19)
    private val busStop = BusStand.whereIsTheBusStop // where(0) is(1) the(2) bus(3) stop(4)

    /** Fixing "bus" in "Where is the bus stop?", at [step]. */
    private fun at(step: Int, fails: Int = 0) = FixingLoop.start(busStop, 3).copy(step = step, fails = fails)

    private fun say(state: FixState, heard: String, sentence: Sentence = busStop) =
        FixingLoop.submit(state, heard, sentence.accept, config)

    private fun chunkFor(sentence: Sentence, index: Int) = FixingLoop.start(sentence, index).let { it.chunk to it.chunkWordIndex }

    @Test
    fun `the loop starts at step 1 on the chosen word`() {
        assertEquals(
            FixState("bus_01", "bus", 3, "the bus stop", 1, "Where is the bus stop?", step = 1, fails = 0),
            FixingLoop.start(busStop, 3),
        )
    }

    @Test
    fun `step 4 uses the chunk around the word's position`() {
        assertEquals("Where is" to 0, chunkFor(busStop, 0))
        assertEquals("the bus stop" to 0, chunkFor(busStop, 2))
        assertEquals("go to Bettiah" to 2, chunkFor(BusStand.doesThisBusGoToBettiah, 5))
        assertEquals("please" to 0, chunkFor(BusStand.oneTicketToPatna, 4))
    }

    @Test
    fun `a repeated word gets the chunk around the occurrence being fixed`() {
        val s = Sentence("x", "The bus to the bus stand", "", listOf("The bus", "to the bus stand"))
        assertEquals("The bus" to 1, chunkFor(s, 1))
        assertEquals("to the bus stand" to 2, chunkFor(s, 4))
    }

    @Test
    fun `chunks are located one after another, not each from the start`() {
        val s = Sentence("x", "This bus or that bus?", "", listOf("This bus", "or that", "bus"))
        assertEquals("bus" to 0, chunkFor(s, 4))
    }

    @Test
    fun `chunks with no words take no room`() {
        val dash = "${Char(0x2014)}"
        for (empty in listOf("", dash, ",")) {
            val s = Sentence("x", "The bus to the bus stand", "", listOf("The bus", empty, "to the bus stand"))
            assertEquals("<$empty>", "to the bus stand" to 2, chunkFor(s, 4))
        }
    }

    @Test
    fun `chunks that can't be located in order fall back to the first chunk containing the word`() {
        val s = busStop.copy(chunks = listOf("the bus", "Where is"))
        assertEquals("Where is" to 0, chunkFor(s, 0))
        assertEquals("the bus" to 1, chunkFor(s, 3))
        val twoWithBus = busStop.copy(chunks = listOf("bus stop", "the bus")) // "the bus" can't follow "bus stop"
        assertEquals("bus stop" to 0, chunkFor(twoWithBus, 3))
    }

    @Test
    fun `in a fallback chunk the word's first place is used`() {
        val s = Sentence("x", "The bus to the bus stand", "", listOf("the bus to the bus", "The"))
        assertEquals("the bus to the bus" to 1, chunkFor(s, 4))
    }

    @Test
    fun `a word in no chunk practises the full sentence at step 4`() {
        val s = busStop.copy(chunks = listOf("Where is"))
        assertEquals("Where is the bus stop?" to 4, chunkFor(s, 4))
    }

    @Test
    fun `hyphenated chunks are matched word by word`() {
        val s = Sentence("x", "Where is the bus-stop?", "", listOf("Where is", "the bus-stop"))
        assertEquals("the bus-stop" to 2, chunkFor(s, 4))
    }

    @Test
    fun `steps 1 and 2 only need Next`() {
        val s1 = FixingLoop.start(busStop, 3)
        val s2 = FixingLoop.next(s1)
        assertEquals(2, s2.step)
        assertEquals(3, FixingLoop.next(s2).step)
        assertNull(FixingLoop.target(s1))
        assertNull(FixingLoop.target(s2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Next is not allowed on a recording step`() {
        FixingLoop.next(at(3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recording is not allowed on steps 1 and 2`() {
        say(at(2), "bus")
    }

    @Test
    fun `targets are the word, then the chunk, then the sentence`() {
        assertEquals("bus", FixingLoop.target(at(3)))
        assertEquals("the bus stop", FixingLoop.target(at(4)))
        assertEquals("Where is the bus stop?", FixingLoop.target(at(5)))
    }

    @Test
    fun `passing a step moves on and earns fix XP`() {
        val r = say(at(3), "bus")
        assertEquals(PASSED, r.outcome)
        assertEquals(at(4), r.state)
        assertEquals(config.fixStepXp, r.xp)
    }

    @Test
    fun `extra words do not stop a step from passing`() {
        assertEquals(PASSED, say(at(3), "where is the bus stop").outcome)
    }

    @Test
    fun `steps 4 and 5 pass when the word being fixed is right, whatever the other words`() {
        assertEquals(PASSED, say(at(4), "the bus top").outcome)
        assertEquals(SENTENCE_FIXED, say(at(5), "where is bus stop").outcome)
        assertEquals(TRY_AGAIN, say(at(4), "the bas stop").outcome)
        assertEquals(TRY_AGAIN, say(at(5), "where is the bas stop").outcome)
    }

    @Test
    fun `the word must be right at its own position`() {
        // "bus" said only in the wrong place: the aligner pairs it with "the".
        assertEquals(TRY_AGAIN, say(at(5), "where is bus the stop").outcome)
    }

    @Test
    fun `accepted spellings pass a step`() {
        val bettiah = FixingLoop.start(BusStand.doesThisBusGoToBettiah, 5).copy(step = 3)
        assertEquals(PASSED, say(bettiah, "betiya", BusStand.doesThisBusGoToBettiah).outcome)
    }

    @Test
    fun `failing once means try again at the same step`() {
        val r = say(at(4), "the bas stop")
        assertEquals(TRY_AGAIN, r.outcome)
        assertEquals(at(4, fails = 1), r.state)
        assertEquals(0, r.xp)
    }

    @Test
    fun `failing a step twice drops back one step`() {
        assertEquals(DROPPED_BACK to at(2), say(at(3, fails = 1), "bas").let { it.outcome to it.state })
        assertEquals(DROPPED_BACK to at(3), say(at(4, fails = 1), "the bas stop").let { it.outcome to it.state })
        assertEquals(DROPPED_BACK to at(4), say(at(5, fails = 1), "where is the bas stop").let { it.outcome to it.state })
    }

    @Test
    fun `a pass clears earlier fails`() {
        assertEquals(at(5), say(at(4, fails = 1), "the bus stop").state)
    }

    @Test
    fun `nothing heard is not a failed try`() {
        for (heard in listOf("", "...", "[BLANK_AUDIO]")) {
            val r = say(at(4, fails = 1), heard)
            assertEquals(NOTHING_HEARD, r.outcome)
            assertEquals(at(4, fails = 1), r.state)
            assertNull(r.alignment)
            assertEquals(0, r.xp)
        }
    }

    @Test
    fun `passing step 5 fixes the sentence`() {
        val r = say(at(5, fails = 1), "Where is the bus stop?")
        assertEquals(SENTENCE_FIXED, r.outcome)
        assertEquals(at(5, fails = 1), r.state)
        assertEquals(config.fixStepXp, r.xp)
    }

    @Test
    fun `fix XP and the drop-back count come from the config`() {
        val custom = GameConfig(fixStepXp = 0, fixFailsBeforeDropBack = 3)
        assertEquals(0, FixingLoop.submit(at(3), "bus", busStop.accept, custom).xp)
        val once = FixingLoop.submit(at(3), "bas", busStop.accept, custom)
        val twice = FixingLoop.submit(once.state, "bas", busStop.accept, custom)
        assertEquals(TRY_AGAIN to 2, twice.outcome to twice.state.fails)
        assertEquals(DROPPED_BACK, FixingLoop.submit(twice.state, "bas", busStop.accept, custom).outcome)
    }

    @Test
    fun `owner's example - the loop drills bus and can finish`() {
        val attempt = Practice.attemptSentence(LearnerProgress(), busStop, "where is bas stop", today, config) as SentenceAttempt.Scored
        val fix = FeedbackSelector.wordToFix(attempt.alignment)!!
        var state = FixingLoop.next(FixingLoop.next(FixingLoop.start(busStop, fix.index)))
        assertEquals("bus", state.word)
        for (heard in listOf("bus", "the bus stop", "where is bus stop")) {
            val r = say(state, heard)
            assertTrue("$heard -> ${r.outcome}", r.outcome == PASSED || r.outcome == SENTENCE_FIXED)
            state = r.state
        }
    }

    @Test
    fun `practice adds fix XP on a pass and nothing otherwise`() {
        val start = LearnerProgress(xpTotal = 20, xpByDay = mapOf(today to 20))
        val passed = Practice.attemptFixStep(start, at(3), busStop, "bus", today, config)
        assertEquals(25, passed.progress.xpTotal)
        assertEquals(25, passed.progress.xpOn(today))
        assertEquals(start, Practice.attemptFixStep(start, at(3), busStop, "bas", today, config).progress)
        assertEquals(start, Practice.attemptFixStep(start, at(3), busStop, "...", today, config).progress)
    }

    @Test
    fun `practice uses the sentence's own accept list in the fixing loop`() {
        val s = BusStand.doesThisBusGoToBettiah
        val r = Practice.attemptFixStep(LearnerProgress(), FixingLoop.start(s, 5).copy(step = 3), s, "betiya", today, config)
        assertEquals(PASSED, r.result.outcome)
    }

    @Test
    fun `finishing the loop earns XP but does not mark the sentence done or touch the streak`() {
        val r = Practice.attemptFixStep(LearnerProgress(), at(5), busStop, "where is the bus stop", today, config)
        assertEquals(LearnerProgress(xpTotal = 5, xpByDay = mapOf(today to 5)), r.progress)
    }

    @Test
    fun `only a passed step 5 updates the Leitner box, for every word of the sentence`() {
        // "bus" was just put in box 1; "stop" is due today in box 2.
        val box = mapOf("bus" to LeitnerEntry(1, today.plusDays(1)), "stop" to LeitnerEntry(2, today))
        val start = LearnerProgress(leitner = box)

        assertEquals(box, Practice.attemptFixStep(start, at(4), busStop, "the bus stop", today, config).progress.leitner)
        assertEquals(box, Practice.attemptFixStep(start, at(5), busStop, "where is the bas stop", today, config).progress.leitner)

        val fixed = Practice.attemptFixStep(start, at(5), busStop, "where is bus stop", today, config).progress.leitner
        assertEquals(LeitnerEntry(1, today.plusDays(1)), fixed["bus"]) // not due yet: unchanged
        assertEquals(LeitnerEntry(3, today.plusDays(7)), fixed["stop"]) // due and right: moves up
        assertEquals(LeitnerEntry(1, today.plusDays(1)), fixed["the"]) // missed at step 5: box 1
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a fix step must be checked against its own sentence`() {
        Practice.attemptFixStep(LearnerProgress(), at(3), BusStand.howMuchIsTheTicket, "bus", today, config)
    }

    @Test
    fun `fix states are checked when built`() {
        val bad = listOf(
            { at(3).copy(step = 6) },
            { at(3).copy(step = 0) },
            { at(3).copy(word = " ") },
            { at(3).copy(wordIndex = -1) },
            { at(3).copy(fails = -1) },
            { FixingLoop.start(busStop, 5) },
        )
        for ((i, make) in bad.withIndex()) {
            val failed = runCatching { make() }.exceptionOrNull()
            assertTrue("case $i: $failed", failed is IllegalArgumentException)
        }
    }
}
