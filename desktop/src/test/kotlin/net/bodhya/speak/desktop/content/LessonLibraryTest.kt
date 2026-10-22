package net.bodhya.speak.desktop.content

import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.desktop.TestContent.bank
import net.bodhya.speak.desktop.TestContent.lessonJson
import net.bodhya.speak.desktop.TestContent.market
import net.bodhya.speak.desktop.TestContent.packJson
import net.bodhya.speak.domain.GameConfig
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
        assertEquals(listOf("starter.bus_stand"), library.lessons.map { it.lesson.id })
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
        assertEquals(listOf("starter.bus_stand", "bihar1.market", "bihar1.bank"), after.lessons.map { it.lesson.id })
        assertEquals(listOf("bihar1.json"), added.list()!!.toList())
        assertEquals("bihar1", after.lessonOf("bihar1.mkt_02")!!.pack.id)
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
        assertEquals(listOf("starter.bus_stand", "bihar1.market", "bihar1.shop"), library.install(r).lessons.map { it.lesson.id })
        assertEquals("the new version replaced the old file", listOf("bihar1.json"), added.list()!!.toList())
    }

    @Test
    fun `a pack may use the same ids as another pack, kept apart by the pack's id`() {
        val library = TestContent.library(dataDir)
        val depot = lessonJson("bus_stand", "Depot", "डिपो", listOf("bus_01" to "Where is the depot?"))
        val after = library.install(ready(library, packJson("bihar1", 1, 1, depot)))
        assertEquals(listOf("starter.bus_stand", "bihar1.bus_stand"), after.lessons.map { it.lesson.id })
        assertEquals("Where is the depot?", after.lessonOf("bihar1.bus_01")!!.lesson.sentences[0].text)
        assertEquals("Where is the bus stop?", after.lessonOf("starter.bus_01")!!.lesson.sentences[0].text)
    }

    @Test
    fun `bad files are refused with a clear reason`() {
        val library = TestContent.library(dataDir)
        assertTrue(rejected(library, "not json").startsWith("This file isn't a lesson pack English Saathi can use"))
        assertTrue(rejected(library, packJson("p", 1, 1, market, format = 2)).startsWith("This lesson pack needs a newer version of English Saathi"))
        val big = File(tmp.root, "big.json").also { f -> f.outputStream().use { o -> o.write(ByteArray(20_000_001)) } }
        assertTrue((library.check(big) as PackCheck.Rejected).message.en.startsWith("This file is too big"))
        assertTrue(rejected(library, "{}").contains("missing \"format\""))
        val typo = rejected(library, packJson("p", 1, 1, market.replace("\"words\": {}", "\"words\": { \"rice\": { \"syllables\": [\"rice\"], \"scored\": false } }")))
        assertTrue(typo, "word \"rice\": unknown field \"scored\" (did you mean \"score\"?)" in typo)
    }

    @Test
    fun `a pack must be UTF-8 text, with or without the mark some editors put first`() {
        val library = TestContent.library(dataDir)
        val text = packJson("bihar1", 1, 1, market)
        val utf16 = File(tmp.root, "utf16.json").also { it.writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + text.toByteArray(Charsets.UTF_16LE)) }
        assertTrue((library.check(utf16) as PackCheck.Rejected).message.en.startsWith("This file isn't UTF-8 text"))
        val latin1 = File(tmp.root, "latin1.json").also { it.writeBytes(text.replace("Market", "Märket").toByteArray(Charsets.ISO_8859_1)) }
        assertTrue(library.check(latin1) is PackCheck.Rejected)
        val bom = File(tmp.root, "bom.json").also { it.writeBytes(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + text.toByteArray()) }
        val r = library.check(bom) as PackCheck.Ready
        assertTrue("the installed copy has no mark", !library.install(r).packs.last().file.readText().startsWith("\uFEFF"))
    }

    @Test
    fun `installing never overwrites a file that isn't the version in use`() {
        added.mkdirs()
        // A pack this version can't read (a later format, say) under the name the new pack would get.
        File(added, "bihar1.json").writeText(packJson("bihar1", 5, 1, market, format = 2))
        val library = TestContent.library(dataDir)
        val after = library.install(ready(library, packJson("bihar1", 1, 1, market)))
        assertEquals(listOf("bihar1-2.json", "bihar1.json"), added.list()!!.sorted())
        assertTrue(File(added, "bihar1.json").readText().contains("\"format\": 2"))
        assertEquals(listOf("starter.bus_stand", "bihar1.market"), after.lessons.map { it.lesson.id })
    }

    @Test
    fun `an added file that can't be read is left out and reported, and the app's lessons stay`() {
        added.mkdirs()
        File(added, "broken.json").writeText("{ \"format\": 1, ")
        File(added, "deep.json").writeText("[".repeat(1_000_000))
        File(added, "big.json").outputStream().use { o -> o.write(ByteArray(20_000_001)) }
        val library = TestContent.library(dataDir)
        assertEquals(listOf("starter.bus_stand"), library.lessons.map { it.lesson.id })
        assertEquals(library.problems.toString(), 3, library.problems.size)
        assertTrue(library.problems.toString(), listOf("broken.json", "deep.json", "big.json").all { name -> library.problems.any { name in it } })
    }

    @Test
    fun `a daily goal no lessons can reach is flagged`() {
        val warnings = LessonLibrary.load(TestContent.builtInDir, dataDir, GameConfig(dailyGoal = 6)).warnings
        assertEquals(listOf("the daily goal (6) is more than all 5 sentences, so it can't be reached"), warnings)
        assertEquals(emptyList<String>(), TestContent.library(dataDir).warnings)
    }

    @Test
    fun `the app's pack comes first, then added packs by their order, then id`() {
        added.mkdirs()
        File(added, "b.json").writeText(packJson("bbb", 1, 2, bank))
        File(added, "a.json").writeText(packJson("aaa", 1, 0, market)) // order 0 and before "starter" by name: still after it
        assertEquals(listOf("starter.bus_stand", "aaa.market", "bbb.bank"), TestContent.library(dataDir).lessons.map { it.lesson.id })
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
        // Version 4 added then: the copy in use is the app's, so the old added file stays and the new one gets a free name.
        library.install(ready(library, starterV2.replace("\"version\": 2", "\"version\": 4")))
        assertEquals(listOf("starter-2.json", "starter.json"), added.list()!!.sorted())
        assertEquals(4, TestContent.library(dataDir, newApp).packs.single().pack.version)
    }
}
