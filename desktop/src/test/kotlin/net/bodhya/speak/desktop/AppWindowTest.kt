package net.bodhya.speak.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import org.junit.Assert.assertEquals
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
    fun `the speech check opens from Settings and goes back to the picker`() {
        compose.setContent { Screens(ready, parts, prepared = true) }
        parts.app.speechCheck()
        compose.onNodeWithText("Speech check").assertExists()
        compose.onNodeWithContentDescription("Back to students").performClick()
        compose.onNodeWithText("Who is practising?").assertExists()
    }
}
