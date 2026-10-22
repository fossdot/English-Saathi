package net.bodhya.speak.desktop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import net.bodhya.speak.desktop.data.AttemptRecord
import net.bodhya.speak.desktop.data.AttemptResult
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import net.bodhya.speak.desktop.speech.SpeechModel
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
    private val ready by lazy {
        val dataDir = File(tmp.root, "English Saathi")
        // tiny.en: these tests start the app six times, and small.en (the default) would load 375 MB each time.
        SettingsFile(File(dataDir, "settings.json")).update { it.copy(model = SpeechModel.TINY_EN) }
        Setup.create(projectDir, dataDir)
    }
    private val parts by lazy { AppParts.create(ready) }

    @Test
    fun `a new student is added, goes home and is saved`() {
        compose.setContent { Screens(ready, parts) }
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
        // A row saved before ids carried their pack's id: it is read, and exported, as starter.bus_01.
        ready.store.logAttempt(
            anita.id,
            AttemptRecord("2026-10-21T13:45:30+05:30", "bus_stand", "bus_01", "sentence", "Where is the bus stop?", "where is the bus stop", 5, 5, 3, "tiny.en", AttemptResult.SCORED, 420),
        )
        parts.app.showStudents()
        val target = File(tmp.root, "chosen/results.csv").also { it.parentFile.mkdirs() }
        compose.setContent { Screens(ready, parts, chooseCsvFile = { name -> assertEquals(parts.app.csvFileName(), name); target }) }
        compose.onNodeWithText("Results").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("5 of 5").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Save as CSV…").performClick()
        compose.waitUntil(5_000) { target.isFile }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Results saved: results.csv").fetchSemanticsNodes().isNotEmpty() }
        val lines = target.readText().removePrefix("\uFEFF").lines()
        assertEquals("2026-10-21 13:45:30,Anita,starter.bus_01,sentence,Where is the bus stop?,where is the bus stop,5,5,3,tiny.en,scored,420", lines[1])
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
        compose.setContent { Screens(ready, parts, chooseCsvFile = { target }) }
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
    fun `Add lessons shows what a pack adds and what its checks flagged, then adds it`() {
        val market = TestContent.market.replace("\"words\": {}", "\"words\": { \"zebra\": { \"syllables\": [\"ze\", \"bra\"] } }")
        val pack = File(tmp.root, "bihar1.json").also { it.writeText(TestContent.packJson("bihar1", 1, 1, market, title = "Places in town")) }
        compose.setContent { Settings(ready, parts, choosePack = { pack }) }
        compose.onNodeWithText("Add lessons…").performClick()
        compose.onNodeWithTag("pack-preview").assertExists()
        compose.onNodeWithText("“Places in town”, version 1").assertExists()
        compose.onNodeWithText("Market: 2 sentences (new)").assertExists()
        compose.onNodeWithText("For whoever made this pack (the lessons still work):").assertExists()
        compose.onNodeWithText("word \"zebra\" is in no sentence", substring = true).assertExists()
        compose.onNodeWithText("Add these lessons").performScrollTo().performClick()
        compose.onNodeWithText("Added “Places in town” (version 1): 1 lesson.").assertExists()
        assertEquals(listOf("starter.bus_stand", "bihar1.market"), parts.app.library.lessons.map { it.lesson.id })
    }

    @Test
    fun `Add lessons refuses a pack with a misspelt field and changes nothing`() {
        val market = TestContent.market.replace("\"words\": {}", "\"words\": { \"rice\": { \"syllables\": [\"rice\"], \"scored\": false } }")
        val pack = File(tmp.root, "bihar1.json").also { it.writeText(TestContent.packJson("bihar1", 1, 1, market)) }
        compose.setContent { Settings(ready, parts, choosePack = { pack }) }
        compose.onNodeWithText("Add lessons…").performClick()
        compose.onNodeWithText("unknown field \"scored\" (did you mean \"score\"?)", substring = true).assertExists()
        compose.onNodeWithText("Nothing was changed.", substring = true).assertExists()
        assertTrue(!File(ready.dataDir, "lessons").exists())
    }

    @Test
    fun `the speech check opens from Settings and goes back to the picker`() {
        compose.setContent { Screens(ready, parts) }
        parts.app.speechCheck()
        compose.onNodeWithText("Speech check").assertExists()
        compose.onNodeWithContentDescription("Back to students").performClick()
        compose.onNodeWithText("Who is practising?").assertExists()
    }
}
