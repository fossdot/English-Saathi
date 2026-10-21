package net.bodhya.speak.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import net.bodhya.speak.desktop.data.AttemptRecord
import net.bodhya.speak.desktop.data.AttemptResult
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The main window's real start-up and screens, drawn off screen into a
 * temporary data folder, without opening the microphone.
 */
class AppWindowTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val ready by lazy { Setup.create(projectDir, File(tmp.root, "English Saathi")) }
    private val parts by lazy { AppParts.create(ready) }

    @Test
    fun `a new student is added, goes home and is saved`() {
        compose.setContent { Screens(ready, parts, prepared = true) }
        compose.onNodeWithText("Who is practising?").assertExists()
        compose.onNodeWithText("Speech model: tiny.en", substring = true).assertExists()
        compose.onNodeWithText("No students yet. Press New student to add one.").assertExists()

        compose.onNodeWithText("New student").performClick()
        compose.onNodeWithContentDescription("colour teal").performClick()
        compose.onNodeWithContentDescription("picture house").performClick()
        compose.onNodeWithTag("name").performTextInput("Anita")
        compose.onNodeWithText("Add student").performClick()

        compose.onNodeWithText("Anita").assertExists()
        compose.onNodeWithText("0 / 5").assertExists()
        compose.onNodeWithText("0 days").assertExists()
        val saved = ready.store.learners().single()
        assertEquals("Anita", saved.name)
        assertEquals(Card(CardColour.TEAL, CardSymbol.HOUSE), saved.card)

        compose.onNodeWithText("Start practice").performClick()
        compose.onNodeWithText("Where is the bus stop?").assertExists()
        compose.onNodeWithContentDescription("Close and go home").performClick()
        compose.onNodeWithContentDescription("Change student").performClick()
        compose.onNodeWithText("Who is practising?").assertExists()
        compose.onNodeWithText("New · नया").assertExists()
    }

    @Test
    fun `Settings deletes a student only after the confirmation`() {
        ready.store.add("Ravi", Card(CardColour.RED, CardSymbol.SUN))
        parts.app.showStudents()
        compose.setContent { Settings(ready, parts) }
        compose.onNodeWithText("Ravi").assertExists()
        compose.onNodeWithText("Delete…").performClick()
        compose.onNodeWithText("Delete Ravi? Their progress, attempts and recordings", substring = true).assertExists()
        assertEquals("nothing deleted before the confirmation", 1, ready.store.learners().size)
        compose.onNodeWithText("Delete Ravi").performClick()
        compose.onNodeWithText("No students yet.").assertExists()
        assertEquals(0, ready.store.learners().size)
    }

    @Test
    fun `Results reads the attempt log and saves the CSV where the facilitator chose`() {
        val anita = ready.store.add("Anita", Card(CardColour.BLUE, CardSymbol.FISH))
        ready.store.logAttempt(
            anita.id,
            AttemptRecord("2026-10-21T13:45:30+05:30", "bus_stand", "bus_01", "sentence", "Where is the bus stop?", "where is the bus stop", 5, 5, 3, "tiny.en", AttemptResult.SCORED, 420),
        )
        parts.app.showStudents()
        val target = File(tmp.root, "chosen/results.csv").also { it.parentFile.mkdirs() }
        compose.setContent { Screens(ready, parts, prepared = true, chooseCsvFile = { name -> assertEquals(parts.app.csvFileName(), name); target }) }
        compose.onNodeWithText("Results").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("5 of 5").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Save as CSV…").performClick()
        compose.waitUntil(5_000) { target.isFile }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Results saved: results.csv").fetchSemanticsNodes().isNotEmpty() }
        val lines = target.readText().removePrefix("\uFEFF").lines()
        assertEquals("2026-10-21 13:45:30,Anita,bus_01,sentence,Where is the bus stop?,where is the bus stop,5,5,3,tiny.en,scored,420", lines[1])
    }

    @Test
    fun `a student deleted in Settings while Results is open leaves the tables and the CSV`() {
        val record = AttemptRecord("2026-10-21T13:45:30+05:30", "bus_stand", "bus_01", "sentence", "Where is the bus stop?", "where is the bus stop", 5, 5, 3, "tiny.en", AttemptResult.SCORED, 420)
        val anita = ready.store.add("Anita", Card(CardColour.BLUE, CardSymbol.FISH))
        val ravi = ready.store.add("Ravi", Card(CardColour.RED, CardSymbol.SUN))
        ready.store.logAttempt(anita.id, record)
        ready.store.logAttempt(ravi.id, record.copy(time = "2026-10-21T13:46:30+05:30"))
        parts.app.showStudents()
        val target = File(tmp.root, "results.csv")
        compose.setContent { Screens(ready, parts, prepared = true, chooseCsvFile = { target }) }
        compose.onNodeWithText("Results").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Ravi").fetchSemanticsNodes().isNotEmpty() }
        parts.app.deleteStudent(ravi.id) // from Settings, which can open over any screen
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Ravi").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Save as CSV…").performClick()
        compose.waitUntil(5_000) { target.isFile }
        val text = target.readText()
        assertTrue(text, "Anita" in text && "Ravi" !in text)
    }

    @Test
    fun `Settings shows the teacher note, the voices and the data folder`() {
        compose.setContent { Settings(ready, parts) }
        compose.onNodeWithText("To be checked by an English teacher").assertExists()
        compose.onNodeWithText("English: ", substring = true).assertExists()
        compose.onNodeWithText(ready.dataDir.path).assertExists()
    }

    @Test
    fun `the speech check opens from Settings and goes back to the picker`() {
        compose.setContent { Screens(ready, parts, prepared = true) }
        parts.app.speechCheck()
        compose.onNodeWithText("Speech check").assertExists()
        compose.onNodeWithContentDescription("Back to students").performClick()
        compose.onNodeWithText("Who is practising?").assertExists()
    }
}
