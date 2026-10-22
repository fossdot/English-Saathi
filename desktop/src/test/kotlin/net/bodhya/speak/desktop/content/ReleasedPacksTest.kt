package net.bodhya.speak.desktop.content

import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.domain.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Every lesson pack ever released (inside an app version or sent out to add
 * in Settings) is kept in `src/test/resources/released-packs/` as
 * `<id>-<version>.json` (docs/lesson-packs.md). A pack that loaded once must
 * load in every later version of the app, or an app update would drop
 * lessons that students have opened; and a released version never changes.
 */
class ReleasedPacksTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val released = File(TestContent.projectDir, "desktop/src/test/resources/released-packs")
        .listFiles { f -> f.name.endsWith(".json") }.orEmpty().sortedBy { it.name }

    @Test
    fun `every released pack still loads, as an added pack too`() {
        assertTrue("the first pack is kept", released.any { it.name == "starter-1.json" })
        val dataDir = tmp.newFolder("English Saathi")
        for (file in released) {
            val added = File(LessonLibrary.addedDir(dataDir), file.name).also { it.parentFile.mkdirs() }
            file.copyTo(added)
            val library = LessonLibrary.load(TestContent.builtInDir, dataDir, GameConfig())
            assertEquals("${file.name}: ${library.problems}", emptyList<String>(), library.problems)
            added.delete()
        }
    }

    @Test
    fun `a pack inside the app is a released version unchanged, or a new version`() {
        for (file in TestContent.builtInDir.listFiles { f -> f.name.endsWith(".json") }.orEmpty()) {
            val pack = PackLoader.load(file.name, file.readText(), emptyList()).pack
            val copy = released.firstOrNull { it.name == "${pack.id}-${pack.version}.json" }
            if (copy != null) {
                assertEquals("${file.name} changed: give it a new version", copy.readText(), file.readText())
            } else {
                val older = released.filter { it.name.startsWith("${pack.id}-") }.maxOfOrNull { it.name.removePrefix("${pack.id}-").removeSuffix(".json").toInt() } ?: 0
                assertTrue("${file.name}: version ${pack.version} isn't newer than the released version $older", pack.version > older)
            }
        }
    }
}
