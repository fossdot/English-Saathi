package net.bodhya.speak.desktop

import net.bodhya.speak.desktop.speech.SpeechModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SavedSettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val file by lazy { SettingsFile(File(tmp.root, "English Saathi/settings.json")) }

    @Test
    fun `the first start uses tiny_en and the Mac's default microphone`() {
        assertEquals(SavedSettings(SpeechModel.TINY_EN, null), file.read())
    }

    @Test
    fun `each choice is kept and the other one stays`() {
        file.update { it.copy(model = SpeechModel.SMALL_EN) }
        file.update { it.copy(input = "USB PnP Sound Device") }
        assertEquals(SavedSettings(SpeechModel.SMALL_EN, "USB PnP Sound Device"), SettingsFile(file.file).read())
        file.update { it.copy(input = null) }
        assertEquals(SavedSettings(SpeechModel.SMALL_EN, null), file.read())
        assertEquals(listOf("settings.json"), file.file.parentFile.list()!!.toList())
    }

    @Test
    fun `a damaged file gives the defaults and says so`() {
        file.file.parentFile.mkdirs()
        val problems = mutableListOf<String>()
        file.file.writeText("{\"model\": \"small.en\", \"inp")
        assertEquals(SavedSettings(), file.read { problems += it })
        file.file.writeText("{\"model\": \"medium.en\", \"input\": \"AirPods\"}")
        assertEquals(SavedSettings(SpeechModel.TINY_EN, "AirPods"), file.read { problems += it })
        assertEquals(2, problems.size)
        assertTrue(problems[1], "unknown model \"medium.en\"" in problems[1])
    }
}
