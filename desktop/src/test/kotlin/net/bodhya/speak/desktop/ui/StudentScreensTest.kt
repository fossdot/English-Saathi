package net.bodhya.speak.desktop.ui

import net.bodhya.speak.desktop.TestContent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.Screen
import net.bodhya.speak.desktop.app.StudentCard
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.StreakState
import net.bodhya.speak.domain.StreakStatus
import net.bodhya.speak.domain.StreakView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate

/** The picker, the new-student card and home, drawn off screen. */
class StudentScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val lesson = TestContent.lesson
    private val today = LocalDate.of(2026, 10, 21)
    private val anita = Learner("s1", "Anita", CardColour.BLUE, CardSymbol.FISH, "2026-10-21T13:45:30+05:30")
    private val ravi = Learner("s2", "Ravi", CardColour.RED, CardSymbol.SUN, "2026-10-21T13:46:30+05:30")

    @Test
    fun `the picker shows each card, name and streak`() {
        var picked: String? = null
        val state = AppState(
            Screen.Students,
            listOf(StudentCard(anita, StreakView(3, StreakStatus.PAUSED)), StudentCard(ravi, StreakView(0, StreakStatus.NONE))),
            today = today,
        )
        compose.setContent { StudentsScreen(state, {}, StudentsActions({ picked = it }, {}, {}, {})) }
        compose.onNodeWithText("Anita").assertExists()
        compose.onNodeWithText("3 days · paused").assertExists()
        compose.onNodeWithText("New · नया").assertExists()
        compose.onNodeWithContentDescription("blue fish").assertExists()
        compose.onNodeWithTag("student-s2").performClick()
        assertEquals("s2", picked)
    }

    @Test
    fun `with no students the picker says how to add one`() {
        compose.setContent { StudentsScreen(AppState(today = today), {}, StudentsActions({}, {}, {}, {})) }
        compose.onNodeWithText("No students yet. Press New student to add one.").assertExists()
    }

    @Test
    fun `a new student chooses a free card and the facilitator types the name`() {
        var added: Pair<String, Card?>? = null
        compose.setContent {
            NewStudentScreen(setOf(Card(CardColour.BLUE, CardSymbol.FISH)), null, NewStudentActions({ n, c -> added = n to c; null }, {}, {}))
        }
        compose.onNodeWithContentDescription("colour blue").performClick()
        compose.onNodeWithContentDescription("picture fish (taken)").assertExists()
        compose.onNodeWithContentDescription("picture kite").performClick()
        compose.onNodeWithTag("name").performTextInput("Sunita")
        compose.onNodeWithText("Add student").performClick()
        assertEquals("Sunita" to Card(CardColour.BLUE, CardSymbol.KITE), added)
    }

    @Test
    fun `home shows the goal, best stars, a paused streak, XP and the locked places`() {
        val progress = LearnerProgress(
            xpTotal = 75,
            doneByDay = mapOf(today to listOf("bus_01", "bus_03")),
            bestStars = mapOf("bus_01" to 3, "bus_03" to 1),
            streak = StreakState(4, today.minusDays(2), today.minusDays(1)),
        )
        var started = 0
        var opened = -1
        compose.setContent {
            HomeScreen(AppState(Screen.Home, learner = anita, progress = progress, today = today), lesson, GameConfig(), HomeActions({ started++ }, { opened = it }, {}, {}, {}))
        }
        compose.onNodeWithTag("goal").assertExists()
        compose.onNodeWithText("2 / 5").assertExists()
        compose.onNodeWithText("4 days").assertExists()
        compose.onNodeWithText("paused · रुका हुआ").assertExists()
        compose.onNodeWithText("75 XP").assertExists()
        compose.onNodeWithContentDescription("3 of 3 stars").assertExists()
        compose.onNodeWithContentDescription("1 of 3 stars").assertExists()
        compose.onNodeWithText("Market").assertExists()
        compose.onNodeWithText("Start practice").performClick()
        compose.onNodeWithTag("sentence-3").performClick()
        assertEquals(1, started)
        assertEquals(3, opened)
    }

    @Test
    fun `a broken streak shows 0 days and a reached goal offers more practice`() {
        val progress = LearnerProgress(
            doneByDay = mapOf(today to listOf("bus_01", "bus_02", "bus_03", "bus_04", "bus_05")),
            streak = StreakState(9, today.minusDays(5)),
        )
        compose.setContent { HomeScreen(AppState(Screen.Home, learner = anita, progress = progress, today = today), lesson, GameConfig(), HomeActions({}, {}, {}, {}, {})) }
        compose.onNodeWithText("0 days").assertExists()
        compose.onNodeWithText("Practise more").assertExists()
    }
}
