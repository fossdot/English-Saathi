package net.bodhya.speak.desktop.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.bodhya.speak.desktop.check.CheckResult
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.check.ShownMessage
import net.bodhya.speak.desktop.check.SpeechCheckController
import net.bodhya.speak.desktop.check.SpeechCheckState
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.domain.Aligner
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.domain.Scorer
import net.bodhya.speak.speech.EndReason
import net.bodhya.speak.speech.EngineState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** The speech check screen, drawn off screen. */
class SpeechCheckScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val lesson = LessonLoader.loadLesson(
        "bus_stand.json", File(projectDir, "content/bus_stand.json").readText(),
        LessonLoader.loadHints("hints.json", File(projectDir, "content/hints.json").readText()), GameConfig(),
    ).lesson
    private val info = CheckInfo("Rishi", "Lekha", emptyList(), "/tmp/speech-check.log", speechPrepared = true)
    private val ready = RecognitionState(SpeechModel.TINY_EN, EngineState.Ready(300))

    private fun result(sentence: Int, heard: String): CheckResult {
        val s = lesson.sentences[sentence]
        val a = Aligner.align(s.targetWords, Normalizer.normalizeHeard(heard), lesson.accept).withUnscored(lesson.unscoredIn(s))
        return CheckResult(s.id, "microphone", 2400, EndReason.SILENCE_AFTER_SPEECH, heard, SpeechModel.TINY_EN, a.words, Scorer.score(a, GameConfig()), 210)
    }

    @Test
    fun `a result shows the count, the words as written and what was heard`() {
        compose.setContent {
            SpeechCheckScreen(lesson, SpeechCheckState(sentence = 2, result = result(2, "1 ticket 2 but now please")), ready, info, CheckActions({}, {}, {}, {}))
        }
        compose.onNodeWithText("4 of 4 words correct · 3 stars").assertExists()
        compose.onNodeWithText("Patna").assertExists() // unscored: neutral, written as in the sentence
        compose.onNodeWithText("We heard: “1 ticket 2 but now please”").assertExists()
    }

    @Test
    fun `messages show in English and Hindi`() {
        val m = ShownMessage(MessageKind.INFO, SpeechCheckController.DIDNT_HEAR)
        compose.setContent { SpeechCheckScreen(lesson, SpeechCheckState(message = m), ready, info, CheckActions({}, {}, {}, {})) }
        compose.onNodeWithText(m.message.en).assertExists()
        compose.onNodeWithText(m.message.hi).assertExists()
    }

    @Test
    fun `the mic button records`() {
        var pressed = 0
        compose.setContent { SpeechCheckScreen(lesson, SpeechCheckState(), ready, info, CheckActions({}, {}, {}, { pressed++ })) }
        compose.onNodeWithTag("mic").performClick()
        assertEquals(1, pressed)
    }

    @Test
    fun `while the model loads the screen says so`() {
        compose.setContent {
            SpeechCheckScreen(lesson, SpeechCheckState(), RecognitionState(SpeechModel.SMALL_EN, EngineState.Loading), info, CheckActions({}, {}, {}, {}))
        }
        compose.onNodeWithText("Loading the speech model…").assertExists()
    }
}
