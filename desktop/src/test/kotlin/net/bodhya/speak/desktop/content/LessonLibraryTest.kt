package net.bodhya.speak.desktop.content

import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.desktop.TestContent.bank
import net.bodhya.speak.desktop.TestContent.lessonJson
import net.bodhya.speak.desktop.TestContent.market
import net.bodhya.speak.desktop.TestContent.packJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LessonLibraryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val dataDir by lazy { tmp.newFolder("English Saathi") }
    private val added by lazy { LessonLibrary.addedDir(dataDir) }

    private fun file(name: String, text: String) = File(tmp.root, name).also { it.writeText(text) }

    private fun ready(library: LessonLibrary, text: String): PackCheck.Ready {
        val check = library.check(file("p.json", text))
        assertTrue(check.toString(), check is PackCheck.Ready)
        return check as PackCheck.Ready
    }

    private fun rejected(library: LessonLibrary, text: String): String {
        val check = library.check(file("p.json", text))
        assertTrue(check.toString(), check is PackCheck.Rejected)
        return (check as PackCheck.Rejected).message.en
    }

    @Test
    fun `with nothing added, the app's own pack`() {
        val library = TestContent.library(dataDir)
        assertEquals(listOf("bus_stand"), library.lessons.map { it.lesson.id })
        assertEquals(5, library.sentenceCount)
        assertEquals(emptyList<String>(), library.problems)
        assertTrue(library.packs.single().builtIn)
    }

    @Test
    fun `a pack is checked, previewed, added to the data folder and its lessons follow in order`() {
        val library = TestContent.library(dataDir)
        val r = ready(library, packJson("bihar1", 1, 1, market, bank))
        assertEquals(listOf("Market" to true, "Bank" to true), r.preview.lessons.map { it.titleEn to it.new })
        assertEquals(listOf("बाज़ार", "बैंक"), r.preview.lessons.map { it.titleHi })
        assertNull(r.preview.replaces)
        assertTrue("checking changes nothing", !added.exists())
        val after = library.install(r)
        assertEquals(listOf("bus_stand", "market", "bank"), after.lessons.map { it.lesson.id })
        assertEquals(listOf("bihar1.json"), added.list()!!.toList())
        assertEquals("bihar1", after.lessonOf("mkt_02")!!.pack.id)
        assertEquals("the same pack again, read at the next start", after.lessons.map { it.lesson.id }, TestContent.library(dataDir).lessons.map { it.lesson.id })
    }

    @Test
    fun `the same or an older version is refused and nothing changes`() {
        val v2 = packJson("bihar1", 2, 1, market, bank)
        TestContent.library(dataDir).let { it.install(ready(it, v2)) }
        val before = File(added, "bihar1.json").readText()
        val library = TestContent.library(dataDir)
        assertTrue(rejected(library, v2).startsWith("This pack is already installed"))
        val older = rejected(library, packJson("bihar1", 1, 1, market))
        assertTrue(older, older.startsWith("This pack is older (version 1) than the one in use (version 2)") && older.endsWith("Nothing was changed."))
        assertEquals(before, File(added, "bihar1.json").readText())
    }

    @Test
    fun `a newer version replaces the older one and the preview says what it removes`() {
        TestContent.library(dataDir).let { it.install(ready(it, packJson("bihar1", 1, 1, market, bank))) }
        val library = TestContent.library(dataDir)
        val shop = lessonJson("shop", "Shop", "दुकान", listOf("shp_01" to "Do you have soap?"))
        val r = ready(library, packJson("bihar1", 2, 1, market, shop))
        assertEquals(1, r.preview.replaces)
        assertEquals(listOf("Market" to false, "Shop" to true), r.preview.lessons.map { it.titleEn to it.new })
        assertEquals(listOf("Bank"), r.preview.removed.map { it.titleEn })
        assertEquals(listOf("bus_stand", "market", "shop"), library.install(r).lessons.map { it.lesson.id })
    }

    @Test
    fun `a pack that reuses another pack's lesson or sentence ids is refused`() {
        val library = TestContent.library(dataDir)
        val clash = lessonJson("depot", "Depot", "डिपो", listOf("BUS_01" to "Where is the depot?"))
        val message = rejected(library, packJson("bihar1", 1, 1, clash))
        assertTrue(message, message.startsWith("The sentence “Where is the depot?” (BUS_01) is already in the pack “First lessons”"))
    }

    @Test
    fun `bad files are refused with a clear reason`() {
        val library = TestContent.library(dataDir)
        assertTrue(rejected(library, "not json").startsWith("This file isn't a lesson pack English Saathi can use"))
        assertTrue(rejected(library, packJson("p", 1, 1, market, format = 2)).startsWith("This lesson pack needs a newer version of English Saathi"))
        val big = File(tmp.root, "big.json").also { f -> f.outputStream().use { o -> o.write(ByteArray(20_000_001)) } }
        assertTrue((library.check(big) as PackCheck.Rejected).message.en.startsWith("This file is too big"))
        assertTrue(rejected(library, "{}").contains("missing \"format\""))
    }

    @Test
    fun `an added file that can't be read, or clashes, is left out and reported, and the app's lessons stay`() {
        added.mkdirs()
        File(added, "broken.json").writeText("{ \"format\": 1, ")
        File(added, "clash.json").writeText(packJson("clash", 1, 0, lessonJson("bus_stand", "Copy", "कॉपी", listOf("x1" to "Hello there."))))
        val library = TestContent.library(dataDir)
        assertEquals(listOf("bus_stand"), library.lessons.map { it.lesson.id })
        assertEquals(library.problems.toString(), 2, library.problems.size)
        assertTrue(library.problems.toString(), library.problems.any { "broken.json" in it } && library.problems.any { "“Pack clash” is left out" in it })
    }

    @Test
    fun `the app's pack comes first, then added packs by their order, then id`() {
        added.mkdirs()
        File(added, "b.json").writeText(packJson("bbb", 1, 2, bank))
        File(added, "a.json").writeText(packJson("aaa", 1, 0, market)) // order 0 and before "starter" by name: still after it
        assertEquals(listOf("bus_stand", "market", "bank"), TestContent.library(dataDir).lessons.map { it.lesson.id })
    }

    @Test
    fun `a newer copy of the app's own pack can be added, and a newer app's copy wins again`() {
        val starterV2 = File(TestContent.builtInDir, "starter.json").readText()
            .replace("\"version\": 1", "\"version\": 2")
            .replace("\"title_en\": \"First lessons\"", "\"title_en\": \"First lessons 2\"")
        TestContent.library(dataDir).let { it.install(ready(it, starterV2)) }
        assertEquals("First lessons 2", TestContent.library(dataDir).packs.single().pack.titleEn)
        // An app update with version 3 of its own pack: the app's copy is newer, so it is used; the added file stays.
        val newApp = tmp.newFolder("app-v2")
        File(newApp, "starter.json").writeText(starterV2.replace("\"version\": 2", "\"version\": 3").replace("First lessons 2", "First lessons 3"))
        val library = TestContent.library(dataDir, newApp)
        assertEquals("First lessons 3" to true, library.packs.single().pack.titleEn to library.packs.single().builtIn)
        assertTrue(File(added, "starter.json").isFile)
    }
}
