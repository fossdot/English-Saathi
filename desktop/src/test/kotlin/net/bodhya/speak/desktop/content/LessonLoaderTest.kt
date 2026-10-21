package net.bodhya.speak.desktop.content

import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.Practice
import net.bodhya.speak.domain.Score
import net.bodhya.speak.domain.SentenceAttempt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class LessonLoaderTest {

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val config = GameConfig()
    private val hints = TestContent.hints

    private fun load(text: String, goal: Int = 1) = LessonLoader.loadLesson("test.json", text, hints, GameConfig(dailyGoal = goal))

    /** A small lesson: one sentence, "Where is the bus stop?", with [words] as its word table and [extra] sentence fields. */
    private fun lesson(words: String = WORDS, extra: String = "") = """
        { "id": "t", "title_en": "T", "title_hi": "टी", "words": { $words },
          "sentences": [ { "id": "s1", "text": "Where is the bus stop?", "hi": "", "chunks": ["Where is", "the bus stop"] $extra } ] }
    """

    private fun warnings(text: String, goal: Int = 1) = load(text, goal).warnings

    @Test
    fun `the bus stand lesson in the app's pack has everything decided`() {
        // It loads without warnings: PackLoaderTest.
        val lesson = TestContent.lesson
        assertEquals(listOf("bus_01", "bus_02", "bus_03", "bus_04", "bus_05"), lesson.sentences.map { it.id })
        assertEquals(setOf("patna"), lesson.unscoredIn(lesson.sentences[2]))
        assertEquals(setOf("bettiah"), lesson.unscoredIn(lesson.sentences[4]))
        assertEquals(emptyMap<String, Set<String>>(), lesson.accept)
        assertEquals(listOf("tic", "ket"), lesson.syllablesOf("ticket"))
        // The drafted entries the pilot didn't have.
        assertEquals("v_w", lesson.words.getValue("one").hintTag)
    }

    @Test
    fun `owner's examples work on the real lesson`() {
        val lesson = TestContent.lesson
        val r = Practice.attemptSentence(LearnerProgress(), lesson, lesson.sentences[2], "1 ticket 2 but now please", LocalDate.of(2026, 10, 20), config)
        assertEquals(Score(4, 4, 3), (r as SentenceAttempt.Scored).score)
    }

    @Test
    fun `the hints are the pilot's five templates, all unreviewed`() {
        assertEquals("फिर से सुनिए और धीरे-धीरे बोलिए।", hints.forTag("generic").textHi)
        for (tag in listOf("v_w", "th", "s_cluster", "short_vowel", "generic")) assertEquals(false, hints.forTag(tag).reviewed)
    }

    @Test
    fun `a word entry that no sentence uses is flagged`() {
        assertTrue(warnings(lesson("$WORDS, \"zebra\": { \"syllables\": [\"ze\", \"bra\"] }")).any { "\"zebra\" is in no sentence" in it })
    }

    @Test
    fun `a sentence word without an entry is flagged`() {
        val w = warnings(lesson(WORDS.replace("\"stop\": { \"syllables\": [\"stop\"] }", "\"stop2\": { \"syllables\": [\"stop\"] }")))
        assertTrue(w.toString(), w.any { "\"stop\" has no word entry" in it })
    }

    @Test
    fun `accept entries that are not one word are flagged`() {
        val w = warnings(lesson(WORDS.replace("\"bus\": { \"syllables\": [\"bus\"] }", "\"bus\": { \"syllables\": [\"bus\"], \"accept\": [\"bas\", \"b us\"] }")))
        assertEquals(listOf("test.json: word \"bus\": accept entry \"b us\" is not one word, so it is ignored"), w)
    }

    @Test
    fun `an accept list on an unscored word is flagged`() {
        val w = warnings(lesson(WORDS.replace("\"bus\": { \"syllables\": [\"bus\"] }", "\"bus\": { \"syllables\": [\"bus\"], \"score\": false, \"accept\": [\"bas\"] }")))
        assertEquals(listOf("test.json: word \"bus\" is not scored, so its accept list has no effect"), w)
    }

    @Test
    fun `an unscored override naming a word not in the sentence is flagged`() {
        assertEquals(listOf("test.json: sentence s1: unscored word \"patna\" is not in the sentence"), warnings(lesson(extra = ", \"unscored\": [\"patna\"]")))
        assertEquals(emptyList<String>(), warnings(lesson(extra = ", \"unscored\": [\"bus\"]")))
    }

    @Test
    fun `a daily goal larger than the sentences is flagged`() {
        assertEquals(listOf("test.json: the daily goal (5) is more than the 1 sentences, so it can't be reached"), warnings(lesson(), goal = 5))
    }

    @Test
    fun `a hint tag without a template is flagged`() {
        val w = warnings(lesson(WORDS.replace("\"bus\": { \"syllables\": [\"bus\"] }", "\"bus\": { \"syllables\": [\"bus\"], \"hint_tag\": \"z_j\" }")))
        assertEquals(listOf("test.json: word \"bus\": hint tag \"z_j\" has no template, so it gets the generic tip"), w)
    }

    @Test
    fun `unknown fields are flagged`() {
        assertEquals(listOf("test.json: sentence s1: unknown field \"acept\" is ignored"), warnings(lesson(extra = ", \"acept\": []")))
    }

    @Test
    fun `a sentence with nothing to score is an error`() {
        val e = assertThrows(ContentException::class.java) {
            load(lesson(extra = ", \"unscored\": [\"where\", \"is\", \"the\", \"bus\", \"stop\"]"))
        }
        assertEquals("test.json: lesson t: sentence s1 has no scored word, so it can't be scored", e.message)
    }

    @Test
    fun `what the domain rejects is an error naming the file`() {
        val e = assertThrows(ContentException::class.java) { load(lesson("\"Bus\": { \"syllables\": [\"bus\"] }")) }
        assertEquals("test.json: lesson t: word key \"Bus\" is not a normalized word", e.message)
        val twice = """{ "id": "t", "title_en": "T", "title_hi": "टी", "words": {}, "sentences": [
            { "id": "s1", "text": "Go.", "hi": "", "chunks": [] }, { "id": "s1", "text": "Come.", "hi": "", "chunks": [] } ] }"""
        assertEquals("test.json: lesson t repeats sentence ids [s1]", assertThrows(ContentException::class.java) { load(twice) }.message)
    }

    @Test
    fun `broken or incomplete files are errors`() {
        assertEquals("test.json: missing \"title_en\"", assertThrows(ContentException::class.java) { load("""{ "id": "t", "words": {}, "sentences": [] }""") }.message)
        assertTrue(assertThrows(ContentException::class.java) { load("{ \"id\": ") }.message!!.startsWith("test.json: not valid JSON"))
        assertEquals("test.json: \"score\" must be true or false",
            assertThrows(ContentException::class.java) { load(lesson(WORDS.replace("\"bus\": { \"syllables\": [\"bus\"] }", "\"bus\": { \"syllables\": [\"bus\"], \"score\": \"no\" }"))) }.message)
    }

    companion object {
        const val WORDS = """"where": { "syllables": ["where"] }, "is": { "syllables": ["is"] }, "the": { "syllables": ["the"] }, "bus": { "syllables": ["bus"] }, "stop": { "syllables": ["stop"] }"""
    }
}
