package net.bodhya.speak.desktop.ui

import net.bodhya.speak.desktop.TestContent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.FixView
import net.bodhya.speak.desktop.app.Screen
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.practice.Listening
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.domain.FixingLoop
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.StreakStatus
import net.bodhya.speak.speech.EngineState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** The fixing and goal screens, drawn off screen. */
class FixScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val hints = TestContent.hints
    private val lesson = TestContent.lesson
    private val anita = Learner("s1", "Anita", CardColour.BLUE, CardSymbol.FISH, "2026-10-21T13:45:30+05:30")
    private val today = LocalDate.of(2026, 10, 21)
    private val ready = RecognitionState(SpeechModel.TINY_EN, EngineState.Ready(300))

    /** Fixing "ticket" in "How much is the ticket?" at [step]. */
    private fun view(step: Int) = FixView(
        FixingLoop.start(lesson.sentences[3], 4).copy(step = step), "ticket", listOf("tic", "ket"), "टि-केट", hints.forTag("generic").textHi,
    )

    private fun show(step: Int, actions: FixActions = FixActions({}, {}, {}, {}, {}, {}, {})) = compose.setContent {
        FixScreen(AppState(Screen.Fix(3), learner = anita, today = today), view(step), Listening.State(), ready, actions)
    }

    @Test
    fun `step 1 shows the big word and Next`() {
        var next = 0
        show(1, FixActions({ next++ }, {}, {}, {}, {}, {}, {}))
        compose.onNodeWithText("Step 1: Listen slowly").assertExists()
        compose.onNodeWithContentDescription("Step 1 of 5").assertExists()
        compose.onNodeWithTag("big-word").assertExists()
        compose.onNodeWithText("टि-केट").assertExists()
        compose.onNodeWithText("Next").performClick()
        assertEquals(1, next)
    }

    @Test
    fun `step 2 adds the syllables and the tip`() {
        show(2)
        compose.onNodeWithText("tic").assertExists()
        compose.onNodeWithText("ket").assertExists()
        compose.onNodeWithText(hints.forTag("generic").textHi).assertExists()
    }

    @Test
    fun `step 3 is recorded, and Skip is always there`() {
        var skipped = 0
        var pressed = 0
        show(3, FixActions({}, { pressed++ }, {}, {}, { skipped++ }, {}, {}))
        compose.onNodeWithText("Step 3: Say the word").assertExists()
        compose.onNodeWithText("Next").assertDoesNotExist()
        compose.onNodeWithTag("mic").performClick()
        compose.onNodeWithText("Skip for now").performClick()
        assertEquals(1, pressed)
        assertEquals(1, skipped)
    }

    @Test
    fun `step 4 shows the phrase with the word`() {
        show(4)
        compose.onNodeWithText("Step 4: Say the phrase").assertExists()
        compose.onNodeWithTag("target").assertExists()
        compose.onNodeWithText("is the ticket").assertExists()
    }

    @Test
    fun `the goal screen shows XP today, the streak and tomorrow's words`() {
        var more = 0
        val progress = LearnerProgress(xpTotal = 120, xpByDay = mapOf(today to 85))
        compose.setContent {
            GoalScreen(AppState(Screen.GoalDone, learner = anita, progress = progress, today = today), 3, StreakStatus.ACTIVE, listOf("Where", "the"), GoalActions({ more++ }, {}, {}, {}))
        }
        compose.onNodeWithText("Goal complete, Anita!").assertExists()
        compose.onNodeWithText("+85").assertExists()
        compose.onNodeWithText("days in a row").assertExists()
        compose.onNodeWithText("Practise again tomorrow").assertExists()
        compose.onNodeWithText("Where").assertExists()
        compose.onNodeWithText("One more sentence").performClick()
        assertEquals(1, more)
    }
}
