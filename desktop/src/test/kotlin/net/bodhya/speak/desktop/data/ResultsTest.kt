package net.bodhya.speak.desktop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ResultsTest {
    private val anita = Learner("s1", "Anita", CardColour.BLUE, CardSymbol.FISH, "2026-10-21T13:00:00+05:30")
    private val ravi = Learner("s2", "Ravi, \"RK\"", CardColour.RED, CardSymbol.SUN, "2026-10-21T13:01:00+05:30")

    private fun sentence(time: String, sentence: String, correct: Int, total: Int, stars: Int) =
        AttemptRecord(time, "bus_stand", sentence, AttemptRecord.SENTENCE, "Where is the bus stop?", "where is the bus stop", correct, total, stars, "tiny.en", AttemptResult.SCORED, 640)

    private fun notHeard(time: String) =
        AttemptRecord(time, "bus_stand", "bus_02", AttemptRecord.SENTENCE, "When will the bus come?", "", result = AttemptResult.NOT_HEARD)

    private val rows = listOf(
        LoggedAttempt(anita, sentence("2026-10-21T13:45:30+05:30", "bus_01", 3, 5, 1)),
        LoggedAttempt(anita, AttemptRecord("2026-10-21T13:46:00+05:30", "bus_stand", "bus_01", "fix step 3", "Where", "where", 1, 1, null, "tiny.en", AttemptResult.PASSED, 300)),
        LoggedAttempt(ravi, notHeard("2026-10-21T13:47:00+05:30")),
        LoggedAttempt(anita, sentence("2026-10-21T13:48:00+05:30", "bus_01", 5, 5, 3)),
        LoggedAttempt(anita, notHeard("2026-10-21T13:49:00+05:30")),
        LoggedAttempt(anita, sentence("2026-10-21T13:50:00+05:30", "bus_03", 4, 4, 3)),
    )

    @Test
    fun `the summary counts heard sentence attempts, with not heard apart`() {
        assertEquals(
            listOf(
                StudentSummary("Anita", attempts = 3, notHeard = 1, sentencesTried = 2, threeStars = 2, correct = 12, total = 14),
                StudentSummary("Ravi, \"RK\"", attempts = 0, notHeard = 1, sentencesTried = 0, threeStars = 0, correct = 0, total = 0),
            ),
            Results.summary(rows),
        )
    }

    @Test
    fun `the every-attempt table is newest first and capped`() {
        assertEquals(listOf("13:50", "13:49"), Results.recent(rows, limit = 2).map { it.record.time.substring(11, 16) })
        assertEquals("21 Oct, 01:45 PM", Results.shortTime(rows[0].record.time))
    }

    @Test
    fun `the CSV has the pilot's columns, then model, result and wait_ms, in local time`() {
        val csv = Results.csv(rows)
        assertTrue("a byte-order mark for Excel", csv.startsWith("﻿"))
        val lines = csv.removePrefix("﻿").split("\n")
        assertEquals("time,student,sentence,mode,target,heard,correct,total,stars,model,result,wait_ms", lines[0])
        assertEquals("2026-10-21 13:45:30,Anita,bus_01,sentence,Where is the bus stop?,where is the bus stop,3,5,1,tiny.en,scored,640", lines[1])
        assertEquals("2026-10-21 13:46:00,Anita,bus_01,fix step 3,Where,where,1,1,,tiny.en,passed,300", lines[2])
        assertEquals("not heard: empty counts", "2026-10-21 13:47:00,\"Ravi, \"\"RK\"\"\",bus_02,sentence,When will the bus come?,,,,,,not heard,", lines[3])
        assertEquals(rows.size + 2, lines.size) // and a final newline
        assertEquals("", lines.last())
    }

    @Test
    fun `text with commas, quotes or line breaks is quoted`() {
        val odd = LoggedAttempt(anita, sentence("2026-10-21T13:45:30+05:30", "bus_01", 1, 5, 1).copy(heard = "well,\nI said \"hi\""))
        assertTrue(Results.csv(listOf(odd)).contains(",\"well,\nI said \"\"hi\"\"\",1,5,"))
    }

    @Test
    fun `the Save dialog suggests a dated name`() {
        assertEquals("english-saathi-results-2026-10-21.csv", Results.fileName(LocalDate.of(2026, 10, 21)))
    }
}
