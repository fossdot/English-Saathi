package englishbolo.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ContentTest {

    private fun bettiah(vararg accept: String) = Sentence(
        id = "x", text = "Does this bus go to Bettiah?", hi = "", chunks = emptyList(),
        words = mapOf("bettiah" to WordInfo(listOf("Bet", "ti", "ah"), accept = accept.toList())),
    )

    @Test
    fun `accept entries are normalized like everything else`() {
        assertEquals(mapOf("bettiah" to setOf("betiya", "betia")), bettiah("Betiya", " BETIA. ").accept)
    }

    @Test
    fun `accept entries that are not one word are ignored`() {
        assertEquals(emptyMap<String, Set<String>>(), bettiah("betty ah", "bet-tiah", "?").accept)
        assertEquals(mapOf("bettiah" to setOf("betiya")), bettiah("betty ah", "betiya").accept)
    }

    @Test
    fun `a word without an entry is its own single syllable`() {
        val s = BusStand.oneTicketToPatna
        assertEquals(listOf("tic", "ket"), s.syllablesOf("ticket"))
        assertEquals(listOf("to"), s.syllablesOf("to"))
        assertEquals(listOf("one"), s.copy(words = mapOf("one" to WordInfo(emptyList()))).syllablesOf("one"))
    }

    @Test
    fun `target words come from the sentence text`() {
        assertEquals(listOf("one", "ticket", "to", "patna", "please"), BusStand.oneTicketToPatna.targetWords)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `word keys must be normalized words`() {
        Sentence("x", "One ticket to Patna", "", emptyList(), words = mapOf("Patna" to WordInfo(listOf("Pat", "na"))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a sentence needs English words`() {
        Sentence("x", "बस स्टॉप कहाँ है?", "Where is the bus stop?", emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a lesson needs sentences`() {
        Lesson("empty", "Empty", "खाली", emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `sentence ids are unique within a lesson`() {
        val s = BusStand.whereIsTheBusStop
        Lesson("dup", "Dup", "डुप", listOf(s, BusStand.whenWillTheBusCome.copy(id = s.id)))
    }

    @Test
    fun `game config values are checked`() {
        val bad = listOf(
            { GameConfig(twoStarRatio = 1.5) },
            { GameConfig(dailyGoal = 0) },
            { GameConfig(fixFailsBeforeDropBack = 0) },
            { GameConfig(leitnerIntervalsDays = emptyList()) },
            { GameConfig(leitnerIntervalsDays = listOf(1, 0)) },
            { GameConfig(streakResetMissedDays = 0) },
        )
        for ((i, make) in bad.withIndex()) {
            val failed = runCatching { make() }.exceptionOrNull()
            assertEquals("case $i", IllegalArgumentException::class, failed?.let { it::class })
        }
    }
}
