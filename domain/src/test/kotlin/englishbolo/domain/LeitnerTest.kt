package englishbolo.domain

import englishbolo.domain.WordStatus.CORRECT
import englishbolo.domain.WordStatus.MISSING
import englishbolo.domain.WordStatus.WRONG
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class LeitnerTest {

    private val config = GameConfig()
    private val day0 = LocalDate.of(2026, 10, 19)

    private fun update(box: Map<String, LeitnerEntry>, today: LocalDate, vararg results: Pair<String, WordStatus>) =
        Leitner.update(box, results.map { (w, s) -> WordResult(w, s) }, today, config)

    @Test
    fun `a wrong or missing word goes to box 1, due tomorrow`() {
        val box = update(emptyMap(), day0, "bus" to WRONG, "the" to MISSING, "stop" to CORRECT)
        assertEquals(mapOf("bus" to LeitnerEntry(1, day0.plusDays(1)), "the" to LeitnerEntry(1, day0.plusDays(1))), box)
    }

    @Test
    fun `a correct word that is not in the box is not added`() {
        assertEquals(emptyMap<String, LeitnerEntry>(), update(emptyMap(), day0, "bus" to CORRECT))
    }

    @Test
    fun `correct before the due day changes nothing`() {
        val box = mapOf("bus" to LeitnerEntry(1, day0.plusDays(1)))
        assertEquals(box, update(box, day0, "bus" to CORRECT))
    }

    @Test
    fun `correct when due moves up through boxes 1 to 3, then the word is learned`() {
        var box = update(emptyMap(), day0, "bus" to WRONG)
        assertEquals(LeitnerEntry(1, day0.plusDays(1)), box["bus"])

        val day1 = day0.plusDays(1)
        box = update(box, day1, "bus" to CORRECT)
        assertEquals(LeitnerEntry(2, day1.plusDays(3)), box["bus"])

        val day4 = day1.plusDays(3)
        box = update(box, day4, "bus" to CORRECT)
        assertEquals(LeitnerEntry(3, day4.plusDays(7)), box["bus"])

        box = update(box, day4.plusDays(7), "bus" to CORRECT)
        assertEquals(emptyMap<String, LeitnerEntry>(), box)
    }

    @Test
    fun `a late review is scheduled from the day it happens`() {
        val box = mapOf("bus" to LeitnerEntry(2, day0))
        val late = day0.plusDays(3)
        assertEquals(LeitnerEntry(3, late.plusDays(7)), update(box, late, "bus" to CORRECT)["bus"])
    }

    @Test
    fun `wrong in any box goes back to box 1`() {
        val box = mapOf("bus" to LeitnerEntry(3, day0.plusDays(5)))
        assertEquals(LeitnerEntry(1, day0.plusDays(1)), update(box, day0, "bus" to WRONG)["bus"])
    }

    @Test
    fun `a word repeated in one sentence is updated in order`() {
        // Wrong then right: the second occurrence is not due yet (tomorrow).
        assertEquals(LeitnerEntry(1, day0.plusDays(1)), update(emptyMap(), day0, "the" to WRONG, "the" to CORRECT)["the"])
        // Right (and due) then wrong: wrong wins.
        val due = mapOf("the" to LeitnerEntry(2, day0))
        assertEquals(LeitnerEntry(1, day0.plusDays(1)), update(due, day0, "the" to CORRECT, "the" to WRONG)["the"])
    }

    @Test
    fun `words keep the order they entered the box`() {
        var box = update(emptyMap(), day0, "where" to WRONG, "bus" to WRONG, "stop" to WRONG)
        box = update(box, day0, "where" to WRONG) // already there: stays first
        assertEquals(listOf("where", "bus", "stop"), box.keys.toList())

        val day1 = day0.plusDays(1)
        box = update(box, day1, "where" to CORRECT) // moves up a box, keeps its place
        assertEquals(listOf("where", "bus", "stop"), box.keys.toList())
    }

    @Test
    fun `due lists words due today or earlier`() {
        val box = mapOf(
            "a" to LeitnerEntry(1, day0.minusDays(2)),
            "b" to LeitnerEntry(2, day0),
            "c" to LeitnerEntry(1, day0.plusDays(1)),
        )
        assertEquals(listOf("a", "b"), Leitner.due(box, day0, config))
    }

    @Test
    fun `practise again tomorrow lists words due tomorrow or earlier`() {
        val box = mapOf(
            "overdue" to LeitnerEntry(1, day0.minusDays(1)),
            "tomorrow" to LeitnerEntry(1, day0.plusDays(1)),
            "in3days" to LeitnerEntry(2, day0.plusDays(3)),
            "in7days" to LeitnerEntry(3, day0.plusDays(7)),
        )
        assertEquals(listOf("overdue", "tomorrow"), Leitner.forTomorrow(box, day0, config))
    }

    @Test
    fun `due dates set while the phone's date was ahead are pulled back`() {
        // Written when the phone said it was a year later.
        val box = mapOf("bus" to LeitnerEntry(1, day0.plusDays(366)), "stop" to LeitnerEntry(2, day0.plusDays(368)))
        assertEquals(
            mapOf("bus" to LeitnerEntry(1, day0.plusDays(1)), "stop" to LeitnerEntry(2, day0.plusDays(3))),
            Leitner.pullBack(box, day0, config),
        )
        assertEquals(listOf("bus"), Leitner.forTomorrow(box, day0, config))
        // An update stores the pulled-back dates; the word then comes due as usual.
        val day1 = day0.plusDays(1)
        val afterDay1 = update(box, day1, "the" to WRONG)
        assertEquals(LeitnerEntry(1, day1.plusDays(1)), afterDay1["bus"])
        assertEquals(LeitnerEntry(2, day1.plusDays(3)), afterDay1["stop"])
        val day2 = day1.plusDays(1)
        assertEquals(LeitnerEntry(2, day2.plusDays(3)), update(afterDay1, day2, "bus" to CORRECT)["bus"])
    }

    @Test
    fun `due dates within their interval are not touched`() {
        val box = mapOf("bus" to LeitnerEntry(3, day0.plusDays(7)), "the" to LeitnerEntry(1, day0.minusDays(30)))
        assertEquals(box, Leitner.pullBack(box, day0, config))
    }

    @Test
    fun `a box beyond the configured intervals is learned on its next correct answer`() {
        val box = mapOf("bus" to LeitnerEntry(5, day0))
        assertEquals(emptyMap<String, LeitnerEntry>(), update(box, day0, "bus" to CORRECT))
        assertEquals(LeitnerEntry(1, day0.plusDays(1)), update(box, day0, "bus" to WRONG)["bus"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `boxes start at 1`() {
        LeitnerEntry(0, day0)
    }

    @Test
    fun `intervals come from the config`() {
        val custom = GameConfig(leitnerIntervalsDays = listOf(2, 5))
        var box = Leitner.update(emptyMap(), listOf(WordResult("bus", WRONG)), day0, custom)
        assertEquals(LeitnerEntry(1, day0.plusDays(2)), box["bus"])
        val later = day0.plusDays(2)
        box = Leitner.update(box, listOf(WordResult("bus", CORRECT)), later, custom)
        assertEquals(LeitnerEntry(2, later.plusDays(5)), box["bus"])
        box = Leitner.update(box, listOf(WordResult("bus", CORRECT)), later.plusDays(5), custom)
        assertEquals(emptyMap<String, LeitnerEntry>(), box)
    }
}
