package net.bodhya.speak.desktop

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.bodhya.speak.desktop.mic.MicAccess
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.speech.EngineState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The app's real start-up on this Mac (speech library, lesson, voices, model,
 * log), without opening a window or the microphone, into a temporary data
 * folder.
 */
class SetupTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `starts up on small_en and logs what the facilitator needs`() {
        val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
        val setup = Setup.create(projectDir, File(tmp.root, "English Saathi"))
        val loaded = runBlocking { setup.recognition.state.first { it.engine !is EngineState.Loading } }
        assertEquals("small.en is the default (owner, 2026-10-21)", SpeechModel.SMALL_EN, loaded.model)
        assertTrue(loaded.engine.toString(), loaded.engine is EngineState.Ready)
        assertEquals(5, setup.lesson.sentences.size)

        val log = setup.log.file.readText()
        println(log)
        // The one-line AVFoundation query gives a real answer from inside the JVM.
        assertTrue(log, "permission (for the app that started Gradle): UNKNOWN" !in log)
        for (expected in listOf("machine:", "excluded from Time Machine", "java:", "microphone permission", "inputs:", "voices: English", "model small.en, 4 threads", "microphone built-in")) {
            assertTrue("log lacks \"$expected\":\n$log", expected in log)
        }
    }

    @Test
    fun `a saved microphone that isn't connected falls back to the built-in one and says so`() {
        val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
        val dataDir = File(tmp.root, "English Saathi")
        SettingsFile(File(dataDir, "settings.json")).update { it.copy(model = SpeechModel.TINY_EN, microphone = MicChoice.Named("Not A Real Microphone")) }
        val setup = Setup.create(projectDir, dataDir)
        val builtIn = MicAccess.builtInInput()?.takeIf { it in setup.inputs }
        assertEquals(builtIn, setup.input)
        assertTrue(setup.notices.toString(), setup.notices.any { "\"Not A Real Microphone\" isn't connected" in it.en })
        assertEquals("the saved choice is kept for when it is back", MicChoice.Named("Not A Real Microphone"), SettingsFile(File(dataDir, "settings.json")).read().microphone)
        val log = setup.log.file.readText()
        assertTrue(log, "microphone Not A Real Microphone" in log && "(not available: using ${builtIn ?: "Mac's default"})" in log)
    }
}
