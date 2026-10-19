package englishbolo.domain

import englishbolo.domain.WordStatus.CORRECT
import englishbolo.domain.WordStatus.MISSING
import englishbolo.domain.WordStatus.WRONG
import org.junit.Assert.assertEquals
import org.junit.Test

class ScorerTest {

    private val config = GameConfig()

    private fun alignment(vararg statuses: WordStatus, extra: List<String> = emptyList()) =
        Alignment(statuses.mapIndexed { i, s -> WordResult("w$i", s) }, extra)

    private fun stars(vararg statuses: WordStatus) = Scorer.score(alignment(*statuses), config).stars

    @Test
    fun `all correct is 3 stars, even with extra words`() {
        assertEquals(Score(5, 5, 3), Scorer.score(alignment(CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, extra = listOf("please")), config))
    }

    @Test
    fun `exactly 60 percent correct is 2 stars`() {
        assertEquals(2, stars(CORRECT, CORRECT, CORRECT, WRONG, MISSING))
    }

    @Test
    fun `below 60 percent is 1 star`() {
        assertEquals(1, stars(CORRECT, CORRECT, WRONG, WRONG, MISSING))
        assertEquals(1, stars(CORRECT, CORRECT, CORRECT, WRONG, WRONG, MISSING)) // 3 of 6
        assertEquals(1, stars(MISSING, MISSING, MISSING, MISSING, MISSING))
    }

    @Test
    fun `4 of 6 is 2 stars`() {
        assertEquals(2, stars(CORRECT, CORRECT, CORRECT, CORRECT, MISSING, MISSING))
    }

    @Test
    fun `an empty target counts as all correct`() {
        assertEquals(Score(0, 0, 3), Scorer.score(alignment(), config))
    }

    @Test
    fun `the two-star share comes from the config`() {
        val strict = GameConfig(twoStarRatio = 0.8)
        assertEquals(1, Scorer.score(alignment(CORRECT, CORRECT, CORRECT, WRONG, WRONG), strict).stars)
        assertEquals(2, Scorer.score(alignment(CORRECT, CORRECT, CORRECT, CORRECT, WRONG), strict).stars)
    }

    @Test
    fun `sentence XP is effort plus a bonus per star`() {
        assertEquals(15, Scorer.sentenceAttemptXp(1, config))
        assertEquals(20, Scorer.sentenceAttemptXp(2, config))
        assertEquals(25, Scorer.sentenceAttemptXp(3, config))
        assertEquals(7, Scorer.sentenceAttemptXp(2, GameConfig(attemptXp = 1, starXp = 3)))
    }
}
