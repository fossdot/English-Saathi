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

    private fun load(text: String) = LessonLoader.loadLesson("test.json", text, hints)

    /** A small lesson: one sentence, "Where is the bus stop?", with [words] as its word table and [extra] sentence fields. */
    private fun lesson(words: String = WORDS, extra: String = "") = """
        { "id": "t", "title_en": "T", "title_hi": "टी", "words": { $words },
          "sentences": [ { "id": "s1", "text": "Where is the bus stop?", "hi": "", "chunks": ["Where is", "the bus stop"] $extra } ] }
    """

    private fun warnings(text: String) = load(text).warnings

    @Test
    fun `the bus stand lesson in the app's pack has everything decided`() {
        // It loads without warnings: PackLoaderTest.
        val lesson = TestContent.lesson
        assertEquals(listOf("starter.bus_01", "starter.bus_02", "starter.bus_03", "starter.bus_04", "starter.bus_05"), lesson.sentences.map { it.id })
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
    fun `a hint tag without a template is flagged`() {
        val w = warnings(lesson(WORDS.replace("\"bus\": { \"syllables\": [\"bus\"] }", "\"bus\": { \"syllables\": [\"bus\"], \"hint_tag\": \"z_j\" }")))
        assertEquals(listOf("test.json: word \"bus\": hint tag \"z_j\" has no template, so it gets the generic tip"), w)
    }

    @Test
    fun `a field it doesn't know in a word, sentence, lesson or tip is an error, with the field probably meant`() {
        val word = assertThrows(ContentException::class.java) {
            load(lesson(WORDS.replace("\"bus\": { \"syllables\": [\"bus\"] }", "\"bus\": { \"syllables\": [\"bus\"], \"scored\": false }")))
        }
        assertEquals("test.json: word \"bus\": unknown field \"scored\" (did you mean \"score\"?)", word.message)
        val sentence = assertThrows(ContentException::class.java) { load(lesson(extra = ", \"acept\": []")) }
        assertEquals("test.json: sentence s1: unknown field \"acept\"", sentence.message)
        val lesson = assertThrows(ContentException::class.java) { load(lesson().replace("\"title_en\": \"T\"", "\"title_en\": \"T\", \"titel_hi\": \"टी\"")) }
        assertEquals("test.json: lesson: unknown field \"titel_hi\" (did you mean \"title_hi\"?)", lesson.message)
        val hint = assertThrows(ContentException::class.java) { LessonLoader.loadHintTemplates("h.json", """{ "th": { "text_hi": "…", "reviewd": true } }""") }
        assertEquals("h.json: hint \"th\": unknown field \"reviewd\" (did you mean \"reviewed\"?)", hint.message)
    }

    @Test
    fun `a suggestion only when a field is close`() {
        val words = setOf("syllables", "hint_hi", "hint_tag", "accept", "score", "reviewed")
        assertEquals("score", LessonLoader.closest("Score", words))
        assertEquals("accept", LessonLoader.closest("acept", words))
        assertEquals("hint_hi", LessonLoader.closest("hint", words))
        assertEquals("syllables", LessonLoader.closest("sylables", words))
        assertEquals(null, LessonLoader.closest("x", words))
        assertEquals(null, LessonLoader.closest("colour", words))
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
