package net.bodhya.speak.desktop

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
    fun `starts up, loads tiny_en and logs what the facilitator needs`() {
        val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
        val setup = Setup.create(projectDir, File(tmp.root, "English Saathi"))
        val loaded = runBlocking { setup.recognition.state.first { it.engine !is EngineState.Loading } }
        assertEquals(SpeechModel.TINY_EN, loaded.model)
        assertTrue(loaded.engine.toString(), loaded.engine is EngineState.Ready)
        assertEquals(5, setup.lesson.sentences.size)

        val log = setup.log.file.readText()
        println(log)
        // The one-line AVFoundation query gives a real answer from inside the JVM.
        assertTrue(log, "permission (for the app that started Gradle): UNKNOWN" !in log)
        for (expected in listOf("machine:", "excluded from Time Machine", "java:", "microphone permission", "inputs:", "voices: English", "model tiny.en, 4 threads")) {
            assertTrue("log lacks \"$expected\":\n$log", expected in log)
        }
    }
}
