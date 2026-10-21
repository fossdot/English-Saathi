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
    fun `the first start uses small_en and the built-in microphone`() {
        assertEquals(SavedSettings(SpeechModel.SMALL_EN, MicChoice.BuiltIn), file.read())
    }

    @Test
    fun `each choice is kept and the other one stays`() {
        file.update { it.copy(model = SpeechModel.TINY_EN) }
        file.update { it.copy(microphone = MicChoice.Named("USB PnP Sound Device")) }
        assertEquals(SavedSettings(SpeechModel.TINY_EN, MicChoice.Named("USB PnP Sound Device")), SettingsFile(file.file).read())
        file.update { it.copy(microphone = MicChoice.MacDefault) }
        assertEquals(SavedSettings(SpeechModel.TINY_EN, MicChoice.MacDefault), file.read())
        file.update { it.copy(microphone = MicChoice.BuiltIn) }
        assertEquals(SavedSettings(SpeechModel.TINY_EN, MicChoice.BuiltIn), file.read())
        assertEquals(listOf("settings.json"), file.file.parentFile.list()!!.toList())
    }

    @Test
    fun `settings from before 2026-10-21 keep a named microphone, and null meant nothing chosen`() {
        file.file.parentFile.mkdirs()
        file.file.writeText("{\"model\":\"tiny.en\",\"input\":\"USB PnP Sound Device\"}\n")
        assertEquals(SavedSettings(SpeechModel.TINY_EN, MicChoice.Named("USB PnP Sound Device")), file.read())
        file.file.writeText("{\"model\":\"small.en\",\"input\":null}\n")
        assertEquals(SavedSettings(SpeechModel.SMALL_EN, MicChoice.BuiltIn), file.read())
    }

    @Test
    fun `a damaged file gives the defaults and says so`() {
        file.file.parentFile.mkdirs()
        val problems = mutableListOf<String>()
        file.file.writeText("{\"model\": \"small.en\", \"micro")
        assertEquals(SavedSettings(), file.read { problems += it })
        file.file.writeText("{\"model\": \"medium.en\", \"microphone\": \"named\", \"microphone_name\": \"AirPods\"}")
        assertEquals(SavedSettings(SpeechModel.SMALL_EN, MicChoice.Named("AirPods")), file.read { problems += it })
        file.file.writeText("{\"model\": \"tiny.en\", \"microphone\": \"loudest\"}")
        assertEquals(SavedSettings(SpeechModel.TINY_EN, MicChoice.BuiltIn), file.read { problems += it })
        assertEquals(problems.toString(), 3, problems.size)
        assertTrue(problems[1], "unknown model \"medium.en\"" in problems[1])
    }
}
