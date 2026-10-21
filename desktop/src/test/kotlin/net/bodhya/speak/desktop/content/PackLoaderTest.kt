package net.bodhya.speak.desktop.content

import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.desktop.TestContent.lessonJson
import net.bodhya.speak.desktop.TestContent.market
import net.bodhya.speak.desktop.TestContent.packJson
import net.bodhya.speak.domain.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PackLoaderTest {
    private val base = TestContent.starter.templates

    private fun load(text: String) = PackLoader.load("pack.json", text, base, GameConfig())

    private fun rejects(text: String, reason: String) {
        val e = assertThrows(ContentException::class.java) { load(text) }
        assertTrue(e.message, reason in e.message!!)
    }

    @Test
    fun `the pack that comes with the app is Bus stand, unchanged`() {
        val loaded = PackLoader.load("starter.json", java.io.File(TestContent.builtInDir, "starter.json").readText(), emptyList(), GameConfig())
        assertEquals(emptyList<String>(), loaded.warnings)
        with(loaded.pack) {
            assertEquals("starter" to 1, id to version)
            assertEquals(listOf("bus_stand"), lessons.map { it.id })
            assertEquals(listOf("bus_01", "bus_02", "bus_03", "bus_04", "bus_05"), lessons[0].sentences.map { it.id })
            assertEquals(setOf("patna"), lessons[0].unscoredIn(lessons[0].sentences[2]))
            assertEquals("v_w", hints.forTag("v_w").tag)
        }
    }

    @Test
    fun `a pack with two lessons loads in order, with the app's tips behind its own`() {
        val pack = load(packJson("bihar1", 3, 1, market, TestContent.bank, extra = """, "hints": { "v_w": { "text_hi": "नया", "reviewed": false } }""")).pack
        assertEquals(listOf("market", "bank"), pack.lessons.map { it.id })
        assertEquals(3, pack.sentenceCount)
        assertEquals("नया", pack.hints.forTag("v_w").textHi)
        assertEquals("th", pack.hints.forTag("th").tag)
    }

    @Test
    fun `fields must be there and of the right kind`() {
        rejects("[1, 2]", "must be a JSON object")
        rejects("{ \"format\": 1, \"id\": ", "not valid JSON")
        rejects(packJson("p", 1, 0, market).replace("\"version\": 1", "\"version\": \"1\""), "\"version\" must be a whole number")
        rejects(packJson("p", 0, 0, market), "\"version\" must be 1 or more")
        rejects(packJson("p", 1, -1, market), "\"order\" must be 0 or more")
        rejects(packJson("p", 1, 0, market).replace("\"version\": 1", "\"version\": 1.5"), "\"version\" must be a whole number")
        rejects(packJson("Bad Id", 1, 0, market), "pack id \"Bad Id\"")
        rejects(packJson("p", 1, 0), "no lessons")
        rejects(packJson("p", 1, 0, market).replace("\"title_en\": \"Pack p\"", "\"title_en\": \" \""), "\"title_en\" is empty")
    }

    @Test
    fun `lesson and sentence ids must be unique, ignoring case, and safe as file names`() {
        rejects(packJson("p", 1, 0, market, market), "lesson id \"market\" is used twice")
        val again = lessonJson("market2", "Market 2", "बाज़ार 2", listOf("MKT_01" to "Hello there."))
        rejects(packJson("p", 1, 0, market, again), "sentence id \"MKT_01\" is used twice")
        rejects(packJson("p", 1, 0, lessonJson("x", "X", "एक्स", listOf("../x" to "Hello there."))), "sentence id \"../x\"")
        rejects(packJson("p", 1, 0, lessonJson("a b", "X", "एक्स", listOf("s1" to "Hello there."))), "lesson id \"a b\"")
    }

    @Test
    fun `a lesson the loader rejects makes the pack unusable`() {
        rejects(packJson("p", 1, 0, lessonJson("x", "X", "एक्स", listOf("s1" to "?"))), "has no English words")
    }

    @Test
    fun `tips need a generic template somewhere`() {
        val e = assertThrows(ContentException::class.java) { PackLoader.load("pack.json", packJson("p", 1, 0, market), emptyList(), GameConfig()) }
        assertTrue(e.message, "no \"generic\" tip" in e.message!!)
    }

    @Test
    fun `a later format needs a newer app, and unknown fields are only warnings`() {
        val e = assertThrows(NewerFormatException::class.java) { load(packJson("p", 1, 0, market, format = 2)) }
        assertEquals(2, e.format)
        val warnings = load(packJson("p", 1, 0, market, extra = """, "pictures": []""")).warnings
        assertTrue(warnings.toString(), warnings.any { "unknown field \"pictures\"" in it })
    }
}
