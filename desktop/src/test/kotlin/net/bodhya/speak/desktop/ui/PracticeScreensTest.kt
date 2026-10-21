package net.bodhya.speak.desktop.ui

import net.bodhya.speak.desktop.TestContent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.bodhya.speak.desktop.app.AppController
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.Screen
import net.bodhya.speak.desktop.app.SentenceResult
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.practice.Listening
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.domain.Aligner
import net.bodhya.speak.domain.FeedbackSelector
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.domain.Scorer
import net.bodhya.speak.speech.EngineState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** The practice and feedback screens, drawn off screen. */
class PracticeScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val hints = TestContent.hints
    private val lesson = TestContent.lesson
    private val anita = Learner("s1", "Anita", CardColour.BLUE, CardSymbol.FISH, "2026-10-21T13:45:30+05:30")
    private val state = AppState(Screen.Practice(2), learner = anita, today = LocalDate.of(2026, 10, 21))
    private val ready = RecognitionState(SpeechModel.TINY_EN, EngineState.Ready(300))

    private fun result(index: Int, heard: String): SentenceResult {
        val s = lesson.sentences[index]
        val a = Aligner.align(s.targetWords, Normalizer.normalizeHeard(heard), lesson.accept).withUnscored(lesson.unscoredIn(s))
        val score = Scorer.score(a, GameConfig())
        val toFix = FeedbackSelector.wordToFix(a)
        return SentenceResult(
            index, a, score, Scorer.sentenceAttemptXp(score.stars, GameConfig()), heard, toFix,
            toFix?.let { AppController.writtenWord(s, it.index) }, toFix?.let { FeedbackSelector.tip(it.word, lesson, hints) },
        )
    }

    @Test
    fun `practice shows the sentence, Listen, Slowly and the mic`() {
        var pressed = 0
        val heard = mutableListOf<Boolean>()
        compose.setContent { PracticeScreen(state, Listening.State(), ready, lesson, 2, PracticeActions({ pressed++ }, { heard += it }, {}, {}, {})) }
        compose.onNodeWithText("One ticket to Patna, please.").assertExists()
        compose.onNodeWithText("Bus stand, sentence 3 (वाक्य 3)").assertExists()
        compose.onNodeWithContentDescription("Sentence 3 of 5").assertExists()
        compose.onNodeWithText("Press and speak").assertExists()
        compose.onNodeWithTag("mic").performClick()
        compose.onNodeWithText("Listen").performClick()
        compose.onNodeWithText("Slowly").performClick()
        assertEquals(1, pressed)
        assertEquals(listOf(false, true), heard)
    }

    @Test
    fun `the mic can't be pressed while the model loads, and shows when it listens`() {
        var pressed = 0
        compose.setContent {
            PracticeScreen(state, Listening.State(), RecognitionState(SpeechModel.SMALL_EN, EngineState.Loading), lesson, 2, PracticeActions({ pressed++ }, {}, {}, {}, {}))
        }
        compose.onNodeWithText("Loading the speech model…").assertExists()
        compose.onNodeWithTag("mic").performClick()
        assertEquals(0, pressed)
    }

    @Test
    fun `listening shows the stop button`() {
        compose.setContent { PracticeScreen(state, Listening.State(listening = true, level = 0.5f), ready, lesson, 2, PracticeActions({}, {}, {}, {}, {})) }
        compose.onNodeWithContentDescription("Stop listening").assertExists()
        compose.onNodeWithText("Listening…").assertExists()
    }

    @Test
    fun `feedback shows stars, praise, the count, chips, what was heard, the tip and Fix`() {
        var fixed = 0
        var skipped = 0
        compose.setContent { FeedbackScreen(anita, result(0, "were is the bus stop"), lesson, FeedbackActions({}, { fixed++ }, { skipped++ }, {}, {})) }
        compose.onNodeWithContentDescription("2 of 3 stars").assertExists()
        compose.onNodeWithText("Very good!").assertExists()
        compose.onNodeWithText("4 of 5 words correct").assertExists()
        compose.onNodeWithText("We heard: “were is the bus stop” (हमने सुना)").assertExists()
        compose.onNodeWithTag("tip").assertExists()
        compose.onNodeWithText("Where (वेयर): ", substring = true).assertExists()
        compose.onNodeWithText("+20 XP").assertExists()
        compose.onNodeWithText("Fix “Where”").performClick()
        compose.onNodeWithText("Skip for now").performClick()
        assertEquals(1, fixed)
        assertEquals(1, skipped)
    }

    @Test
    fun `all words right offers the next sentence and no tip`() {
        var next = 0
        compose.setContent { FeedbackScreen(anita, result(2, "1 ticket 2 but now please"), lesson, FeedbackActions({}, {}, { next++ }, {}, {})) }
        compose.onNodeWithText("Excellent!").assertExists()
        compose.onNodeWithText("Patna").assertExists() // unscored: a neutral chip, as written
        compose.onNodeWithTag("tip").assertDoesNotExist()
        compose.onNodeWithText("Skip for now").assertDoesNotExist()
        compose.onNodeWithText("Next sentence").performClick()
        assertEquals(1, next)
    }
}
