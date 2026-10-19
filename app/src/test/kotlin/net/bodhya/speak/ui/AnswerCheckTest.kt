package net.bodhya.speak.ui

import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Score
import net.bodhya.speak.domain.WordStatus.CORRECT
import net.bodhya.speak.domain.WordStatus.MISSING
import net.bodhya.speak.domain.WordStatus.WRONG
import net.bodhya.speak.speech.EndReason
import net.bodhya.speak.speech.Recording
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class AnswerCheckTest {
    private val config = GameConfig()
    private val target = "Where is the bus stop?"

    private fun recording(speechHeard: Boolean) =
        Recording(FloatArray(16_000), 16_000, speechHeard, EndReason.SILENCE_AFTER_SPEECH)

    @Test
    fun `no speech means the recognizer is never asked`() = runBlocking {
        val check = checkAnswer(target, recording(speechHeard = false), config) { fail("should not transcribe"); "" }
        assertEquals(AnswerCheck(recognized = null, alignment = null, score = null), check)
    }

    @Test
    fun `only non-speech tags means Didn't hear you, not a 1-star answer`() = runBlocking {
        val check = checkAnswer(target, recording(speechHeard = true), config) { "[BLANK_AUDIO]" }
        assertEquals("[BLANK_AUDIO]", check.recognized)
        assertNull(check.alignment)
        assertNull(check.score)
    }

    @Test
    fun `a right answer gets every word and 3 stars`() = runBlocking {
        val check = checkAnswer(target, recording(speechHeard = true), config) { "Where is the bus stop?" }
        assertEquals(Score(correct = 5, total = 5, stars = 3), check.score)
    }

    @Test
    fun `the pilot's example finds the wrong and the missing word`() = runBlocking {
        val check = checkAnswer(target, recording(speechHeard = true), config) { "where is bas stop" }
        assertEquals(listOf(CORRECT, CORRECT, MISSING, WRONG, CORRECT), check.alignment!!.words.map { it.status })
        assertEquals(Score(correct = 3, total = 5, stars = 2), check.score)
    }

    @Test
    fun `the recognizer gets the recording's samples`() = runBlocking {
        val r = recording(speechHeard = true)
        var given: FloatArray? = null
        checkAnswer(target, r, config) { given = it; "where" }
        assert(given === r.samples)
    }
}
