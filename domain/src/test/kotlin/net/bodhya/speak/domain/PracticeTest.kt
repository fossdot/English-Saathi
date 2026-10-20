package net.bodhya.speak.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class PracticeTest {

    private val config = GameConfig()
    private val day0 = LocalDate.of(2026, 10, 19)
    private val day1 = day0.plusDays(1)
    private val lesson = BusStand.lesson
    private val sentences = lesson.sentences

    private fun scored(progress: LearnerProgress, index: Int, heard: String, day: LocalDate = day0) =
        Practice.attemptSentence(progress, lesson, sentences[index], heard, day, config) as SentenceAttempt.Scored

    @Test
    fun `a scored attempt adds XP, marks the sentence done, starts the streak and fills the box`() {
        val r = scored(LearnerProgress(), 0, "where is bas stop")
        assertEquals(Score(3, 5, 2), r.score)
        assertEquals(20, r.xp)
        assertEquals(20, r.progress.xpTotal)
        assertEquals(20, r.progress.xpOn(day0))
        assertEquals(listOf("bus_01"), r.progress.doneOn(day0))
        assertEquals(mapOf("bus_01" to 2), r.progress.bestStars)
        assertEquals(StreakState(1, day0), r.progress.streak)
        assertEquals(listOf("the", "bus"), r.progress.leitner.keys.toList())
    }

    @Test
    fun `owner's example - Patna is not scored, so "patana" costs nothing`() {
        val r = scored(LearnerProgress(), 2, "1 ticket to patana please")
        assertEquals(WordResult("patna", WordStatus.WRONG, scored = false), r.alignment.words[3])
        assertEquals(Score(4, 4, 3), r.score)
        assertEquals(25, r.xp)
        assertEquals(emptyMap<String, LeitnerEntry>(), r.progress.leitner)
    }

    @Test
    fun `owner's example - Whisper's 2 for "to" is right`() {
        val r = scored(LearnerProgress(), 2, "1 ticket 2 Patna please")
        assertEquals(Score(4, 4, 3), r.score)
    }

    @Test
    fun `unscored words never enter the Leitner box`() {
        val r = scored(LearnerProgress(), 4, "does this bus go to")
        assertEquals(WordStatus.MISSING, r.alignment.words[5].status)
        assertEquals(Score(5, 5, 3), r.score)
        assertEquals(emptyMap<String, LeitnerEntry>(), r.progress.leitner)
    }

    @Test
    fun `a sentence can turn scoring off for one of its words`() {
        val noBus = sentences[0].copy(unscored = setOf("bus"))
        val r = Practice.attemptSentence(LearnerProgress(), lesson, noBus, "where is the bas stop", day0, config) as SentenceAttempt.Scored
        assertEquals(Score(4, 4, 3), r.score)
        assertEquals(emptyMap<String, LeitnerEntry>(), r.progress.leitner)
    }

    @Test
    fun `nothing heard changes nothing`() {
        val start = scored(LearnerProgress(), 0, "where is bas stop").progress
        for (heard in listOf("", "   ", "...", "[BLANK_AUDIO]", "(music)")) {
            assertSame(heard, SentenceAttempt.NothingHeard, Practice.attemptSentence(start, lesson, sentences[1], heard, day0, config))
        }
    }

    @Test
    fun `the daily goal counts different sentences, while every attempt earns XP`() {
        var p = LearnerProgress()
        repeat(3) { p = scored(p, 0, "where is the bus stop").progress }
        assertEquals(listOf("bus_01"), p.doneOn(day0))
        assertEquals(75, p.xpOn(day0))
    }

    @Test
    fun `best stars keep the best result`() {
        var p = scored(LearnerProgress(), 0, "where is the bus stop").progress
        p = scored(p, 0, "where").progress
        assertEquals(3, p.bestStars["bus_01"])
    }

    @Test
    fun `the lesson-done screen shows once per day, even after a restart`() {
        var p = LearnerProgress()
        for (i in 0..3) p = scored(p, i, sentences[i].text).progress
        assertEquals(AfterSentence.Next(4), Practice.afterSentence(p, sentences, 3, day0, config).next)

        p = scored(p, 4, sentences[4].text).progress
        val done = Practice.afterSentence(p, sentences, 4, day0, config)
        assertEquals(AfterSentence.GoalComplete, done.next)
        assertEquals(day0, done.progress.goalCelebratedOn)

        // Restarting the app keeps the progress record, so the screen doesn't come back today.
        val more = scored(done.progress, 0, sentences[0].text).progress
        assertEquals(AfterSentence.Next(1), Practice.afterSentence(more, sentences, 0, day0, config).next)

        // The next day the goal starts again.
        var q = more
        for (i in 0..4) q = scored(q, i, sentences[i].text, day1).progress
        assertEquals(AfterSentence.GoalComplete, Practice.afterSentence(q, sentences, 4, day1, config).next)
    }

    @Test
    fun `the daily goal comes from the config`() {
        var p = LearnerProgress()
        for (i in 0..1) p = scored(p, i, sentences[i].text).progress
        assertEquals(AfterSentence.GoalComplete, Practice.afterSentence(p, sentences, 1, day0, GameConfig(dailyGoal = 2)).next)
    }

    @Test
    fun `due words decide the next sentence and the start`() {
        // Day 0: "go" is missed in bus_05, so it is due on day 1. (Bettiah is unscored, so it never enters the box.)
        val p = scored(LearnerProgress(), 4, "does this bus to bettiah").progress
        assertEquals(4, Practice.startPractice(p, sentences, 0, day1, config))
        assertEquals(AfterSentence.Next(4), Practice.afterSentence(p, sentences, 0, day1, config).next)
    }

    @Test
    fun `words in the box that are not due yet don't change the choice`() {
        // "ticket" is missed in bus_04, so it is due tomorrow, not today.
        val p = scored(LearnerProgress(), 3, "how much is the").progress
        assertEquals(0, Practice.startPractice(p, sentences, 3, day0, config))
        assertEquals(AfterSentence.Next(4), Practice.afterSentence(p, sentences, 3, day0, config).next)
    }

    @Test
    fun `practise again tomorrow lists the words just missed`() {
        val p = scored(LearnerProgress(), 0, "where is bas stop").progress
        assertEquals(listOf("the", "bus"), Practice.wordsForTomorrow(p, day0, config))
    }

    @Test
    fun `opening the app pauses the streak, or shows it broken`() {
        val p = scored(LearnerProgress(), 0, "where is the bus stop").progress
        assertEquals(day1, Practice.openApp(p, day0.plusDays(2), config).streak.pausedSince)
        val broken = Practice.openApp(p, day0.plusDays(4), config)
        assertNull(broken.streak.pausedSince)
        assertEquals(StreakView(0, StreakStatus.BROKEN), Streaks.view(broken.streak, day0.plusDays(4), config))
        assertNull(Practice.openApp(p, day1, config).streak.pausedSince)
    }

    @Test
    fun `a phone date that went back records XP on that date but leaves the streak alone`() {
        val p = scored(scored(LearnerProgress(), 0, "where is the bus stop").progress, 1, "when will the bus come", day1).progress
        val back = day0.minusDays(3)
        val r = scored(p, 2, "one ticket to patna please", back)
        assertEquals(StreakState(2, day1), r.progress.streak)
        assertEquals(25, r.progress.xpOn(back))
        assertEquals(listOf("bus_03"), r.progress.doneOn(back))
    }
}
