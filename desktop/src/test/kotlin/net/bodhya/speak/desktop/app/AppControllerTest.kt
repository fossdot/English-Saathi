package net.bodhya.speak.desktop.app

import net.bodhya.speak.desktop.TestContent.at
import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.desktop.check.SpeechOutput
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import net.bodhya.speak.desktop.data.LearnerStore
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.StreakState
import net.bodhya.speak.domain.StreakStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The student picker, new-student card and home flow, without a window. */
class AppControllerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val hints = TestContent.hints
    private val lesson = TestContent.lesson

    /** 2026-10-21, 13:45 in India. */
    private val clock = Clock.fixed(Instant.parse("2026-10-21T08:15:30Z"), ZoneId.of("Asia/Kolkata"))
    private val today = LocalDate.of(2026, 10, 21)
    private val spoken = mutableListOf<String>()
    private val output = object : SpeechOutput {
        override fun english(text: String, speed: Float) { spoken += "en $text" }
        override fun hindi(text: String) { spoken += "hi $text" }
        override fun stop() = Unit
    }
    private val dataDir by lazy { tmp.newFolder("data") }
    private val store by lazy { LearnerStore(dataDir, clock) }
    private val app by lazy { AppController(store, TestContent.library(dataDir), output, clock = clock) }

    private val blueFish = Card(CardColour.BLUE, CardSymbol.FISH)

    @Test
    fun `the app starts at the picker`() {
        assertEquals(Screen.Students, app.state.value.screen)
        assertEquals(emptyList<StudentCard>(), app.state.value.students)
        assertNull(app.state.value.learner)
    }

    @Test
    fun `a new student needs a name and a free card, then goes home`() {
        app.newStudent()
        assertEquals(AppController.NAME_MISSING, app.addStudent("  ", blueFish))
        assertEquals("shown on the screen", AppController.NAME_MISSING, app.state.value.message!!.message)
        assertEquals(AppController.CARD_MISSING, app.addStudent("Anita", null))
        assertEquals(Screen.NewStudent, app.state.value.screen)

        assertNull(app.addStudent("Anita", blueFish))
        val s = app.state.value
        assertEquals(Screen.Home, s.screen)
        assertEquals("Anita", s.learner!!.name)
        assertEquals(LearnerProgress(), s.progress)
        assertNull(s.message)
        assertEquals(listOf("Anita"), s.students.map { it.learner.name })

        app.newStudent()
        assertEquals(AppController.CARD_TAKEN, app.addStudent("Ravi", blueFish))
        assertTrue(app.addStudent("anita", Card(CardColour.RED, CardSymbol.SUN))!!.en.startsWith("There is already a student called anita"))
    }

    @Test
    fun `picking a learner notes missed days for the streak and saves it`() {
        val id = store.add("Anita", blueFish).id
        store.saveProgress(id, LearnerProgress(streak = StreakState(4, today.minusDays(2))))
        app.showStudents()
        assertEquals(StreakStatus.PAUSED, app.state.value.students.single().streak.status)
        app.pick(id)
        assertEquals(today.minusDays(1), app.state.value.progress.streak.pausedSince)
        assertEquals("saved at once", today.minusDays(1), store.progress(id).streak.pausedSince)
    }

    @Test
    fun `start practice opens the first sentence never said`() {
        val id = store.add("Anita", blueFish).id
        store.saveProgress(id, LearnerProgress(doneByDay = mapOf(today to listOf("starter.bus_01", "starter.bus_02")), bestStars = mapOf("starter.bus_01" to 3, "starter.bus_02" to 3)))
        app.pick(id)
        app.startPractice()
        assertEquals(Screen.Practice(at(2)), app.state.value.screen)
        app.home()
        app.openSentence(4)
        assertEquals(Screen.Practice(at(4)), app.state.value.screen)
    }

    @Test
    fun `Hindi help speaks the screen's instruction`() {
        app.hindiHelp()
        app.newStudent()
        app.hindiHelp()
        assertEquals(
            listOf("hi " + AppController.helpText(AppState(Screen.Students, today = today)), "hi " + AppController.helpText(AppState(Screen.NewStudent, today = today))),
            spoken,
        )
    }

    @Test
    fun `renaming checks the name and updates the picker`() {
        val id = store.add("Anita", blueFish).id
        store.add("Ravi", Card(CardColour.RED, CardSymbol.SUN))
        app.showStudents()
        assertTrue(app.renameStudent(id, "ravi") != null)
        assertNull(app.renameStudent(id, "Anita K"))
        assertEquals(listOf("Anita K", "Ravi"), app.state.value.students.map { it.learner.name })
    }

    @Test
    fun `deleting the learner who is practising goes back to the picker`() {
        val anita = store.add("Anita", blueFish).id
        val ravi = store.add("Ravi", Card(CardColour.RED, CardSymbol.SUN)).id
        app.pick(anita)
        app.deleteStudent(ravi)
        assertEquals(Screen.Home, app.state.value.screen)
        assertEquals(listOf("Anita"), app.state.value.students.map { it.learner.name })
        app.deleteStudent(anita)
        assertEquals(Screen.Students, app.state.value.screen)
        assertEquals(emptyList<StudentCard>(), app.state.value.students)
    }
}
