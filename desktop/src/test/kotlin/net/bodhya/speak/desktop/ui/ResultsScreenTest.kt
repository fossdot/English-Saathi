package net.bodhya.speak.desktop.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.app.ResultsData
import net.bodhya.speak.desktop.app.Screen
import net.bodhya.speak.desktop.data.AttemptRecord
import net.bodhya.speak.desktop.data.AttemptResult
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import net.bodhya.speak.desktop.data.Learner
import net.bodhya.speak.desktop.data.LoggedAttempt
import net.bodhya.speak.desktop.data.Results
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** The Results screen, drawn off screen. */
class ResultsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val anita = Learner("s1", "Anita", CardColour.BLUE, CardSymbol.FISH, "2026-10-21T13:00:00+05:30")
    private val state = AppState(Screen.Results, today = LocalDate.of(2026, 10, 21))
    private val rows = listOf(
        LoggedAttempt(anita, AttemptRecord("2026-10-21T13:45:30+05:30", "bus_stand", "bus_01", "sentence", "Where is the bus stop?", "were is the bus stop", 4, 5, 2, "tiny.en", AttemptResult.SCORED, 500)),
        LoggedAttempt(anita, AttemptRecord("2026-10-21T13:46:30+05:30", "bus_stand", "bus_02", "sentence", "When will the bus come?", "", result = AttemptResult.NOT_HEARD)),
    )

    @Test
    fun `the tables show each student and every attempt, and the CSV can be saved`() {
        var saved = 0
        compose.setContent { ResultsScreen(state, ResultsData(rows, Results.summary(rows), Results.recent(rows)), 5, ResultsActions({}, { saved++ }, {})) }
        compose.onNodeWithText(TEACHER_NOTE.en).assertExists()
        compose.onNodeWithText("Not heard").assertExists()
        compose.onNodeWithText("1 / 5").assertExists()
        compose.onNodeWithText("4 of 5").assertExists()
        compose.onNodeWithText("were is the bus stop").assertExists()
        compose.onNodeWithText("not heard").assertExists()
        compose.onNodeWithText("Save as CSV…").performClick()
        assertEquals(1, saved)
    }

    @Test
    fun `with no attempts it says so and offers no CSV`() {
        compose.setContent { ResultsScreen(state, ResultsData(emptyList(), emptyList(), emptyList()), 5, ResultsActions({}, {}, {})) }
        compose.onNodeWithText("No attempts yet. Results appear here after students practise.").assertExists()
        compose.onNodeWithText("Save as CSV…").assertDoesNotExist()
    }
}
