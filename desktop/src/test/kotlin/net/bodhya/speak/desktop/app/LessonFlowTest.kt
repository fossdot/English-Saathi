package net.bodhya.speak.desktop.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.desktop.TestContent.bank
import net.bodhya.speak.desktop.TestContent.market
import net.bodhya.speak.desktop.TestContent.packJson
import net.bodhya.speak.desktop.check.SpeechOutput
import net.bodhya.speak.desktop.content.LessonLibrary
import net.bodhya.speak.desktop.content.PackCheck
import net.bodhya.speak.desktop.data.Card
import net.bodhya.speak.desktop.data.CardColour
import net.bodhya.speak.desktop.data.CardSymbol
import net.bodhya.speak.desktop.data.LearnerStore
import net.bodhya.speak.desktop.mic.MicPermission
import net.bodhya.speak.desktop.practice.Listening
import net.bodhya.speak.desktop.speech.Recognition
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.desktop.speech.Transcript
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.speech.ArrayPcmSource
import net.bodhya.speak.speech.EngineState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Several lessons (docs/decisions.md, "Lessons unlock in order" and "Daily
 * goal with several lessons"): unlocking, the current lesson, Start practice
 * moving on, "Open all lessons", and adding a pack while the app runs.
 */
class LessonFlowTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-10-21T08:15:30Z"), ZoneId.of("Asia/Kolkata"))
    private val today = LocalDate.of(2026, 10, 21)
    private val output = object : SpeechOutput {
        override fun english(text: String, speed: Float) = Unit
        override fun hindi(text: String) = Unit
        override fun stop() = Unit
    }

    /** Says whatever sentence is on screen, perfectly. */
    private inner class Echo : Recognition {
        var heard = ""
        override val state: StateFlow<RecognitionState> = MutableStateFlow(RecognitionState(SpeechModel.SMALL_EN, EngineState.Ready(1)))
        override fun switchTo(model: SpeechModel) = Unit
        override suspend fun transcribe(samples: FloatArray) = Transcript(heard, SpeechModel.SMALL_EN)
    }

    private val echo = Echo()
    private val dataDir by lazy { tmp.newFolder("English Saathi") }
    private val store by lazy { LearnerStore(dataDir, clock) }
    private val changes = mutableListOf<String>()

    private fun spoken(): ShortArray {
        val r = Random(1)
        return ShortArray(8_000) { (r.nextInt(-20, 21)).toShort() } + ShortArray(16_000) { (12_000 * sin(2 * PI * 220 * it / 16_000)).toInt().toShort() }
    }

    private val listening = Listening(echo, { ArrayPcmSource(spoken()) }, { null }, { MicPermission.ALLOWED }, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined)

    private fun addPack(text: String) {
        File(LessonLibrary.addedDir(dataDir), "p.json").also { it.parentFile.mkdirs() }.writeText(text)
    }

    private fun app() = AppController(store, TestContent.library(dataDir), output, listening, clock = clock, onLessonChange = { changes += it.id })

    private val id by lazy { store.add("Anita", Card(CardColour.BLUE, CardSymbol.FISH)).id }

    /** Says sentence [index] of the current lesson right and goes back home. */
    private fun AppController.say(index: Int) {
        openSentence(index)
        echo.heard = lesson.sentences[index].text
        toggleRecording()
        home()
    }

    private val AppController.rows get() = state.value.lessons.map { it.lesson.id to it.open }

    @Test
    fun `lesson 2 opens once every sentence of lesson 1 has been said, with any stars`() {
        addPack(packJson("bihar1", 1, 1, market, bank))
        val app = app()
        app.pick(id)
        assertEquals(listOf("bus_stand" to true, "market" to false, "bank" to false), app.rows)
        for (i in 0..3) app.say(i)
        assertEquals("four of five said: still locked", false, app.rows[1].second)
        app.openSentence(4)
        echo.heard = "does" // one word right of five: 1 star, but said
        app.toggleRecording()
        assertEquals(listOf("market"), app.state.value.result!!.opened.map(Lesson::id))
        assertEquals(listOf("bus_stand" to true, "market" to true, "bank" to false), app.rows)
        assertEquals(listOf("bus_stand", "market"), store.lessons(id).opened)
    }

    @Test
    fun `didn't hear you doesn't count as said`() {
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        for (i in 0..3) app.say(i)
        app.openSentence(4)
        echo.heard = "[BLANK_AUDIO]"
        app.toggleRecording()
        assertEquals(false, app.rows[1].second)
    }

    @Test
    fun `an opened lesson stays open when a pack update adds a sentence before it`() {
        val app = app()
        app.pick(id)
        for (i in 0..4) app.say(i)
        addPack(packJson("bihar1", 1, 1, market))
        val withMarket = app()
        withMarket.pick(id)
        assertEquals(true, withMarket.rows[1].second)
        // The app's pack gains a sixth sentence: Bus stand is no longer all said, but Market stays open.
        val newApp = tmp.newFolder("app-v2")
        File(newApp, "starter.json").writeText(
            File(TestContent.builtInDir, "starter.json").readText()
                .replace("\"version\": 1", "\"version\": 2")
                .replace("""{ "id": "bus_05",""", """{ "id": "bus_06", "text": "Is this seat free?", "hi": "क्या यह सीट ख़ाली है?", "chunks": [] },
        { "id": "bus_05","""),
        )
        val updated = AppController(store, TestContent.library(dataDir, newApp), output, listening, clock = clock)
        updated.pick(id)
        assertEquals(6, updated.lesson.sentences.size)
        assertEquals(listOf("bus_stand" to true, "market" to true), updated.rows)
    }

    @Test
    fun `start practice moves to the next open lesson once the current one is all done today`() {
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        for (i in 0..4) app.say(i)
        assertEquals("the goal of 5 counts any lesson", 5, app.state.value.progress.doneOn(today).size)
        // Every Bus stand sentence is done today, so Start practice goes on to Market.
        app.startPractice()
        assertEquals("market", app.lesson.id)
        assertEquals(Screen.Practice(0), app.state.value.screen)
        assertEquals("market", store.lessons(id).current)
        assertTrue(changes.toString(), changes.last() == "market")
    }

    @Test
    fun `next goes on to the next open lesson after the last sentence not done today`() {
        addPack(packJson("bihar1", 1, 1, market))
        store.saveProgress(id, LearnerProgress(goalCelebratedOn = today))
        val app = app()
        app.pick(id)
        for (i in 0..3) app.say(i)
        app.openSentence(4)
        echo.heard = app.lesson.sentences[4].text
        app.toggleRecording()
        app.next()
        assertEquals("market", app.lesson.id)
        assertEquals(Screen.Practice(0), app.state.value.screen)
    }

    @Test
    fun `an open lesson on Home becomes the current one, a locked one can't`() {
        addPack(packJson("bihar1", 1, 1, market, bank))
        val app = app()
        app.pick(id)
        app.chooseLesson("market")
        assertEquals("bus_stand", app.lesson.id)
        app.setOpenAll(id, true)
        assertEquals(listOf(true, true, true), app.rows.map { it.second })
        app.chooseLesson("bank")
        assertEquals("bank", app.lesson.id)
        assertEquals(true, app.state.value.students.single().openAll)
        app.setOpenAll(id, false)
        assertEquals("back to the unlock order: Bank closes and Bus stand is current again", "bus_stand", app.lesson.id)
        assertEquals(listOf(true, false, false), app.rows.map { it.second })
    }

    @Test
    fun `a pack added while a learner practises is used at once, and they go home`() {
        val app = app()
        app.pick(id)
        app.openSentence(2)
        val file = File(tmp.root, "bihar1.json").also { it.writeText(packJson("bihar1", 1, 1, market)) }
        val ready = app.checkPack(file) as PackCheck.Ready
        assertTrue(app.installPack(ready).en.startsWith("Added “Pack bihar1” (version 1): 1 lesson."))
        assertEquals(Screen.Home, app.state.value.screen)
        assertEquals(listOf("bus_stand" to true, "market" to false), app.rows)
        assertEquals(7, app.sentenceCount())
        val again = app.checkPack(file)
        assertTrue(again is PackCheck.Rejected)
    }

    @Test
    fun `a learner from before lesson packs keeps what they earned`() {
        // Progress saved by D5-D8: Bus stand all said, no lessons.json.
        store.saveProgress(id, LearnerProgress(bestStars = (1..5).associate { "bus_0$it" to 2 }))
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        assertEquals(listOf("bus_stand" to true, "market" to true), app.rows)
        assertEquals("bus_stand", app.lesson.id)
    }
}
