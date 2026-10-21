package net.bodhya.speak.desktop.content

import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.domain.GameConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * How big a 100-lesson pack is, and how quickly it loads: Bus stand's real
 * lesson (5 sentences, 14 word entries with Hindi hints), as written in
 * starter.json, copied 100 times with new ids.
 */
class PackSizeTest {
    private fun gzipped(text: String) = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(text.toByteArray()) } }.size()

    @Test
    fun `a 100-lesson text pack is about 300 KB and loads in well under a second`() {
        val starter = File(TestContent.builtInDir, "starter.json").readText()
        val lesson = starter.substring(starter.indexOf("\"lessons\": [") + "\"lessons\": [".length, starter.lastIndexOf(']')).trim()
        val lessons = (1..100).map { n -> lesson.replace("\"bus_stand\"", "\"lesson_$n\"").replace("\"Bus stand\"", "\"Lesson $n\"").replace("\"bus_0", "\"l${n}_s") }
        val text = starter.substring(0, starter.indexOf("\"lessons\": [")).replace("\"starter\"", "\"hundred\"") +
            "\"lessons\": [\n    " + lessons.joinToString(",\n    ") + "\n  ]\n}\n"
        val bytes = text.toByteArray().size
        // Real lessons differ, so compressing them together saves little more than compressing each alone.
        val compressed = lessons.sumOf { gzipped(it) }

        val start = System.nanoTime()
        val loaded = PackLoader.load("hundred.json", text, emptyList(), GameConfig())
        val ms = (System.nanoTime() - start) / 1_000_000
        println("100-lesson pack: ${bytes / 1024} KB as written, about ${compressed / 1024} KB compressed, loaded in $ms ms")

        assertEquals(100, loaded.pack.lessons.size)
        assertEquals(500, loaded.pack.sentenceCount)
        assertTrue("$bytes bytes", bytes in 200_000..500_000)
        assertTrue("$ms ms", ms < 2_000)
    }
}
