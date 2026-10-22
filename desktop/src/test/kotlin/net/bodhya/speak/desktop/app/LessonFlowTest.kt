package net.bodhya.speak.desktop.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bodhya.speak.desktop.TestContent
import net.bodhya.speak.desktop.TestContent.at
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
import net.bodhya.speak.domain.LeitnerEntry
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
 * Several lessons (docs/decisions.md, "Lessons unlock in order", "Daily goal
 * with several lessons" and "Lesson review: owner answers"): unlocking,
 * moving on once a lesson is all said, never-said sentences first, revision
 * from other lessons, "Open all lessons", and adding a pack while the app runs.
 */
class LessonFlowTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private var clock = Clock.fixed(Instant.parse("2026-10-21T08:15:30Z"), ZoneId.of("Asia/Kolkata"))
    private val today = LocalDate.of(2026, 10, 21)
    private val output = object : SpeechOutput {
        override fun english(text: String, speed: Float) = Unit
        override fun hindi(text: String) = Unit
        override fun stop() = Unit
    }

    /** Says whatever it is told to. */
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

    /** The same learner's next day, with a fresh app. */
    private fun nextDay(): AppController {
        clock = Clock.offset(clock, java.time.Duration.ofDays(1))
        return app()
    }

    private val id by lazy { store.add("Anita", Card(CardColour.BLUE, CardSymbol.FISH)).id }

    private val busStand get() = TestContent.lesson

    /** Says what is on the practice screen, as [words] (the sentence itself, right, by default). */
    private fun AppController.sayShown(words: String? = null) {
        val screen = state.value.screen as Screen.Practice
        echo.heard = words ?: screen.ref.sentence.text
        toggleRecording()
    }

    /** Says sentence [index] of the current lesson right and goes back home. */
    private fun AppController.say(index: Int, words: String? = null) {
        openSentence(index)
        sayShown(words)
        home()
    }

    private val AppController.rows get() = state.value.lessons.map { it.lesson.id to it.open }

    @Test
    fun `lesson 2 opens once every sentence of lesson 1 has been said, with any stars, and becomes current`() {
        addPack(packJson("bihar1", 1, 1, market, bank))
        val app = app()
        app.pick(id)
        assertEquals(listOf("starter.bus_stand" to true, "bihar1.market" to false, "bihar1.bank" to false), app.rows)
        for (i in 0..3) app.say(i)
        assertEquals("four of five said: still locked", false, app.rows[1].second)
        app.openSentence(4)
        app.sayShown("does") // one word right of five: 1 star, but said
        assertEquals(listOf("bihar1.market"), app.state.value.result!!.opened.map(Lesson::id))
        assertEquals("the feedback is still for the sentence just said", Screen.Feedback(at(4)), app.state.value.screen)
        assertEquals(listOf("starter.bus_stand" to true, "bihar1.market" to true, "bihar1.bank" to false), app.rows)
        assertEquals(listOf("starter.bus_stand", "bihar1.market"), store.lessons(id).opened)
        assertEquals("bihar1.market", app.lesson.id)
        assertEquals("bihar1.market", store.lessons(id).current)
        assertEquals("bihar1.market", changes.last())
    }

    @Test
    fun `didn't hear you doesn't count as said`() {
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        for (i in 0..3) app.say(i)
        app.openSentence(4)
        app.sayShown("[BLANK_AUDIO]")
        assertEquals(false, app.rows[1].second)
        assertEquals("starter.bus_stand", app.lesson.id)
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
        assertEquals(listOf("starter.bus_stand" to true, "bihar1.market" to true), updated.rows)
        assertEquals("the first open lesson with a sentence never said", "starter.bus_stand", updated.lesson.id)
        updated.startPractice()
        assertEquals("the new sentence first", "starter.bus_06", (updated.state.value.screen as Screen.Practice).ref.sentence.id)
    }

    @Test
    fun `stopping at the goal screen after the last sentence of a lesson, the next day starts the next lesson`() {
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        for (i in 0..3) app.say(i)
        app.openSentence(4)
        app.sayShown()
        app.next()
        assertEquals("the goal of 5", Screen.GoalDone, app.state.value.screen)
        assertEquals("bihar1.market", app.lesson.id)
        // The student stops here. The next day:
        val tomorrow = nextDay()
        tomorrow.pick(id)
        assertEquals("bihar1.market", tomorrow.lesson.id)
        tomorrow.startPractice()
        assertEquals(Screen.Practice(SentenceRef(tomorrow.lesson, 0)), tomorrow.state.value.screen)
    }

    @Test
    fun `next goes on to the next lesson after the last sentence of this one`() {
        addPack(packJson("bihar1", 1, 1, market))
        store.saveProgress(id, LearnerProgress(goalCelebratedOn = today))
        val app = app()
        app.pick(id)
        for (i in 0..3) app.say(i)
        app.openSentence(4)
        app.sayShown()
        app.next()
        assertEquals("bihar1.market", app.lesson.id)
        assertEquals(Screen.Practice(SentenceRef(app.lesson, 0)), app.state.value.screen)
    }

    @Test
    fun `sentences never said come before ones said on another day`() {
        val app = app()
        app.pick(id)
        app.say(0)
        app.say(1)
        val tomorrow = nextDay()
        tomorrow.pick(id)
        tomorrow.startPractice()
        assertEquals("the pilot would start again at sentence 1", Screen.Practice(at(2)), tomorrow.state.value.screen)
        tomorrow.sayShown()
        tomorrow.next()
        assertEquals(Screen.Practice(at(3)), tomorrow.state.value.screen)
    }

    @Test
    fun `a word due for revision brings back a sentence from an earlier lesson, then the current lesson goes on`() {
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        for (i in listOf(0, 1, 2, 4)) app.say(i)
        app.say(3, "how much is the jacket") // "ticket" wrong: due tomorrow
        assertEquals("bihar1.market", app.lesson.id)
        val tomorrow = nextDay()
        tomorrow.pick(id)
        tomorrow.startPractice()
        val revision = (tomorrow.state.value.screen as Screen.Practice).ref
        assertEquals("the first Bus stand sentence with \"ticket\"", busStand.sentences[2].id, revision.sentence.id)
        assertEquals("bihar1.market", tomorrow.lesson.id)
        tomorrow.sayShown()
        assertEquals("logged under its own lesson", "starter.bus_stand", store.attempts(id).last().lesson)
        tomorrow.next()
        assertEquals("back to the current lesson", Screen.Practice(SentenceRef(tomorrow.lesson, 0)), tomorrow.state.value.screen)
    }

    @Test
    fun `revision never brings a sentence from a later lesson`() {
        addPack(packJson("bihar1", 1, 1, market))
        // "bananas" is only in Market, which "Open all lessons" opens before the student reached it.
        store.saveProgress(id, LearnerProgress(leitner = mapOf("bananas" to LeitnerEntry(1, today), "ticket" to LeitnerEntry(1, today.plusDays(1)))))
        store.saveLessons(id, store.lessons(id).copy(openAll = true))
        val app = app()
        app.pick(id)
        app.startPractice()
        assertEquals("Bus stand's first sentence never said, not Market's", Screen.Practice(at(0)), app.state.value.screen)
        assertEquals(listOf("ticket"), app.wordsForTomorrow())
    }

    @Test
    fun `a student who finished the last lesson moves on when a new pack brings the next one`() {
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        for (i in 0..4) app.say(i)
        app.say(0)
        app.say(1)
        assertEquals("every lesson said: Market stays current", "bihar1.market", app.lesson.id)
        val file = File(tmp.root, "bihar2.json").also { it.writeText(packJson("bihar2", 1, 2, bank)) }
        app.installPack(app.checkPack(file) as PackCheck.Ready)
        assertEquals("bihar2.bank", app.lesson.id)
        assertEquals("bihar2.bank", store.lessons(id).current)
        app.startPractice()
        assertEquals(Screen.Practice(SentenceRef(app.lesson, 0)), app.state.value.screen)
    }

    @Test
    fun `a pack found at the next start also moves a student on`() {
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        for (i in 0..4) app.say(i)
        app.say(0)
        app.say(1)
        File(LessonLibrary.addedDir(dataDir), "q.json").writeText(packJson("bihar2", 1, 2, bank))
        val tomorrow = nextDay()
        tomorrow.pick(id)
        assertEquals("bihar2.bank", tomorrow.lesson.id)
    }

    @Test
    fun `a lesson chosen on Home stays current, even when all of it has been said`() {
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        for (i in 0..4) app.say(i)
        assertEquals("bihar1.market", app.lesson.id)
        app.chooseLesson("starter.bus_stand")
        assertEquals(true, store.lessons(id).chosen)
        val tomorrow = nextDay()
        tomorrow.pick(id)
        assertEquals("starter.bus_stand", tomorrow.lesson.id)
    }

    @Test
    fun `the goal screen lists only words that can come back`() {
        val tomorrow = today.plusDays(1)
        // "bananas" is only in Market, which isn't installed (a pack removed, say); "ticket" is in Bus stand.
        store.saveProgress(id, LearnerProgress(leitner = mapOf("bananas" to LeitnerEntry(1, tomorrow), "ticket" to LeitnerEntry(1, tomorrow))))
        val app = app()
        app.pick(id)
        assertEquals(listOf("ticket"), app.wordsForTomorrow())
    }

    @Test
    fun `an open lesson on Home becomes the current one, a locked one can't`() {
        addPack(packJson("bihar1", 1, 1, market, bank))
        val app = app()
        app.pick(id)
        app.chooseLesson("bihar1.market")
        assertEquals("starter.bus_stand", app.lesson.id)
        app.setOpenAll(id, true)
        assertEquals(listOf(true, true, true), app.rows.map { it.second })
        app.chooseLesson("bihar1.bank")
        assertEquals("bihar1.bank", app.lesson.id)
        assertEquals(true, app.state.value.students.single().openAll)
        app.setOpenAll(id, false)
        assertEquals("back to the unlock order: Bank closes and Bus stand is current again", "starter.bus_stand", app.lesson.id)
        assertEquals(listOf(true, false, false), app.rows.map { it.second })
    }

    @Test
    fun `changing lessons after a sentence late in a long lesson starts the new lesson at its beginning`() {
        addPack(packJson("bihar1", 1, 1, bank))
        val app = app()
        app.pick(id)
        app.setOpenAll(id, true)
        app.say(4)
        app.chooseLesson("bihar1.bank")
        app.say(0)
        app.next() // Bank has one sentence: the pilot's order must not use Bus stand's sentence 5 there
        assertEquals(Screen.Practice(SentenceRef(app.lesson, 0)), app.state.value.screen)
    }

    @Test
    fun `turning Open all lessons off while practising a lesson that closes goes home`() {
        addPack(packJson("bihar1", 1, 1, market, bank))
        val app = app()
        app.pick(id)
        app.setOpenAll(id, true)
        app.chooseLesson("bihar1.bank")
        app.openSentence(0)
        app.setOpenAll(id, true)
        assertTrue("still open: the student carries on", app.state.value.screen is Screen.Practice)
        app.setOpenAll(id, false)
        assertEquals(Screen.Home, app.state.value.screen)
        assertEquals("starter.bus_stand", app.lesson.id)
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
        assertEquals(listOf("starter.bus_stand" to true, "bihar1.market" to false), app.rows)
        assertEquals(7, app.sentenceCount())
        val again = app.checkPack(file)
        assertTrue(again is PackCheck.Rejected)
    }

    @Test
    fun `a learner from before lesson packs keeps what they earned and moves on`() {
        // Progress saved by D5-D8: Bus stand all said under the old ids, no lessons.json.
        store.saveProgress(id, LearnerProgress(bestStars = (1..5).associate { "bus_0$it" to 2 }))
        addPack(packJson("bihar1", 1, 1, market))
        val app = app()
        app.pick(id)
        assertEquals(listOf("starter.bus_stand" to true, "bihar1.market" to true), app.rows)
        assertEquals(5, app.state.value.lessons[0].said)
        assertEquals("bihar1.market", app.lesson.id)
    }
}
