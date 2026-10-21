package net.bodhya.speak.desktop.app

import net.bodhya.speak.desktop.TestContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bodhya.speak.desktop.check.MessageKind
import net.bodhya.speak.desktop.check.SpeechCheckController
import net.bodhya.speak.desktop.check.SpeechOutput
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.data.AttemptRecord
import net.bodhya.speak.desktop.data.AttemptResult
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
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.LearnerProgress
import net.bodhya.speak.domain.Score
import net.bodhya.speak.domain.Sentence
import net.bodhya.speak.speech.ArrayPcmSource
import net.bodhya.speak.speech.EngineState
import net.bodhya.speak.speech.PcmSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Practice and feedback (milestone D6) without a window, a microphone or a
 * model: everything runs on the calling thread, so each action has finished
 * when it returns, unless the fake recognizer is made to wait.
 */
class PracticeFlowTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val hints = TestContent.hints
    private val lesson = TestContent.lesson
    private val clock = Clock.fixed(Instant.parse("2026-10-21T08:15:30Z"), ZoneId.of("Asia/Kolkata"))
    private val today = LocalDate.of(2026, 10, 21)

    private val spoken = mutableListOf<String>()
    private val output = object : SpeechOutput {
        override fun english(text: String, speed: Float) { spoken += "en $speed $text" }
        override fun hindi(text: String) { spoken += "hi $text" }
        override fun stop() = Unit
    }

    /** A recognizer that says [heard], or waits for [gate] when it is set. */
    private inner class FakeRecognition : Recognition {
        var heard = "where is the bus stop"
        var gate: CompletableDeferred<Unit>? = null
        var calls = 0
        override val state: StateFlow<RecognitionState> = MutableStateFlow(RecognitionState(SpeechModel.SMALL_EN, EngineState.Ready(1)))
        override fun switchTo(model: SpeechModel) = Unit
        override suspend fun transcribe(samples: FloatArray): Transcript {
            calls++
            gate?.await()
            return Transcript(heard, SpeechModel.SMALL_EN)
        }
    }

    private val recognition = FakeRecognition()
    private var microphone: () -> PcmSource = { ArrayPcmSource(spoken()) }
    private var micReady = true
    private val log = mutableListOf<String>()
    private val dataDir by lazy { tmp.newFolder("data") }
    private val store by lazy { LearnerStore(dataDir, clock) }
    private val listening = Listening(
        recognition, { microphone() }, { null }, { MicPermission.DENIED },
        CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined,
    )
    private val app by lazy { AppController(store, TestContent.library(dataDir), output, listening, { micReady }, clock = clock, log = { log += it }) }
    private val id by lazy { store.add("Anita", Card(CardColour.BLUE, CardSymbol.FISH)).id }

    /** Room noise, then a second of a loud tone (speech, as far as the endpointer can tell). */
    private fun spoken(): ShortArray {
        val r = Random(1)
        val noise = ShortArray(8_000) { (r.nextInt(-20, 21)).toShort() }
        val tone = ShortArray(16_000) { (12_000 * sin(2 * PI * 220 * it / 16_000)).toInt().toShort() }
        return noise + tone
    }

    private fun practise(sentence: Int) {
        app.pick(id)
        app.openSentence(sentence)
    }

    private fun rows(): List<AttemptRecord> = store.attempts(id)

    @Test
    fun `a sentence said right is scored, saved, logged and shown`() {
        practise(0)
        app.toggleRecording()
        val s = app.state.value
        assertEquals(Screen.Feedback(0), s.screen)
        val r = s.result!!
        assertEquals(Score(5, 5, 3), r.score)
        assertEquals(25, r.xp)
        assertNull("every word right: nothing to fix", r.toFix)

        val saved = store.progress(id)
        assertEquals(25, saved.xpTotal)
        assertEquals(listOf("bus_01"), saved.doneOn(today))
        assertEquals(3, saved.bestStars["bus_01"])
        assertEquals(1, saved.streak.days)
        assertEquals(saved, s.progress)

        val row = rows().single()
        assertEquals(AttemptRecord("2026-10-21T13:45:30+05:30", "bus_stand", "bus_01", "sentence", "Where is the bus stop?", "where is the bus stop", 5, 5, 3, "small.en", AttemptResult.SCORED, row.waitMs), row)
        assertTrue("the latest recording is kept", store.recordingFile(id, "bus_01").isFile)
        assertTrue(log.last(), log.last().startsWith("attempt | learner=$id | sentence | sentence=bus_01 | scored"))
    }

    @Test
    fun `a mistake gives the word to fix as written, with its tip`() {
        practise(0)
        recognition.heard = "were is the bus stop"
        app.toggleRecording()
        val r = app.state.value.result!!
        assertEquals(Score(4, 5, 2), r.score)
        assertEquals("where", r.toFix!!.word)
        assertEquals("Where", r.toFixWritten)
        assertEquals("v_w", r.tip!!.template.tag)
        app.hindiHelp()
        assertEquals("hi 5 में से 4 शब्द सही। " + r.tip!!.template.textHi, spoken.last())
        app.feedbackPrimary()
        assertEquals(Screen.Fix(0), app.state.value.screen)
    }

    @Test
    fun `nothing but non-speech tags is didn't hear you, logged as not heard`() {
        practise(0)
        recognition.heard = "[BLANK_AUDIO]"
        app.toggleRecording()
        assertEquals(Screen.Practice(0), app.state.value.screen)
        assertEquals(SpeechCheckController.DIDNT_HEAR, app.state.value.message!!.message)
        assertEquals(LearnerProgress(), store.progress(id))
        val row = rows().single()
        assertEquals(AttemptResult.NOT_HEARD, row.result)
        assertEquals("[BLANK_AUDIO]", row.heard)
        assertNull(row.correct)
        assertEquals("small.en", row.model)
    }

    @Test
    fun `nothing loud enough is didn't hear you without running the recognizer`() {
        practise(1)
        val r = Random(2)
        microphone = { ArrayPcmSource(ShortArray(16_000) { (r.nextInt(-20, 21)).toShort() }) }
        app.toggleRecording()
        assertEquals(SpeechCheckController.DIDNT_HEAR, app.state.value.message!!.message)
        assertEquals(0, recognition.calls)
        val row = rows().single()
        assertEquals(AttemptRecord.SENTENCE, row.mode)
        assertEquals(AttemptResult.NOT_HEARD, row.result)
        assertEquals("", row.heard)
        assertNull(row.model)
        assertTrue("no recording kept when the recognizer didn't run", !store.recordingFile(id, "bus_02").exists())
    }

    @Test
    fun `a blocked microphone says how to allow Terminal`() {
        practise(0)
        microphone = { ArrayPcmSource(ShortArray(0)) } // exact zeros for the full 8 s
        app.toggleRecording()
        val m = app.state.value.message!!
        assertEquals(MessageKind.PROBLEM, m.kind)
        assertTrue(m.message.en.contains("turn on Terminal"))
        assertEquals(AttemptResult.NOT_HEARD, rows().single().result)
    }

    @Test
    fun `a microphone that can't be opened is reported, not logged`() {
        practise(0)
        microphone = { throw IOException("the microphone \"AirPods\" is not connected") }
        app.toggleRecording()
        assertTrue(app.state.value.message!!.message.en.contains("AirPods"))
        assertEquals(emptyList<AttemptRecord>(), rows())
    }

    @Test
    fun `a result that arrives after the learner moved on is dropped`() {
        practise(0)
        recognition.gate = CompletableDeferred()
        app.toggleRecording()
        app.home()
        recognition.gate!!.complete(Unit)
        assertEquals(Screen.Home, app.state.value.screen)
        assertEquals(emptyList<AttemptRecord>(), rows())
        assertEquals(LearnerProgress(), store.progress(id))
        assertTrue(log.last(), log.last().startsWith("dropped a result for bus_01"))
    }

    @Test
    fun `the mic waits for the speech model`() {
        practise(0)
        micReady = false
        app.toggleRecording()
        assertEquals(0, recognition.calls)
        assertEquals(Screen.Practice(0), app.state.value.screen)
    }

    @Test
    fun `listen plays the sentence at the pilot speeds, and feedback plays it at normal speed`() {
        practise(1)
        app.listen(slow = false)
        app.listen(slow = true)
        recognition.heard = "when will the bus come"
        app.toggleRecording()
        app.listen(slow = false)
        assertEquals(listOf("en 0.9 When will the bus come?", "en 0.6 When will the bus come?", "en 0.9 When will the bus come?"), spoken)
    }

    @Test
    fun `next goes on to the next sentence not done, and to the goal screen once a day`() {
        val all = lesson.sentences.map { it.id }
        store.saveProgress(id, LearnerProgress(doneByDay = mapOf(today to all.take(4))))
        practise(4)
        recognition.heard = "does this bus go to bettiah"
        app.toggleRecording()
        app.next()
        assertEquals(Screen.GoalDone, app.state.value.screen)
        assertEquals("remembered across restarts", today, store.progress(id).goalCelebratedOn)
        app.startPractice()
        app.toggleRecording()
        app.next()
        assertTrue("only once a day", app.state.value.screen is Screen.Practice)
    }

    /** Says "were is the bus stop" for sentence 1 and opens the fixing loop for "Where". */
    private fun fixWhere() {
        practise(0)
        recognition.heard = "were is the bus stop"
        app.toggleRecording()
        app.fix()
    }

    private fun say(text: String) {
        recognition.heard = text
        app.toggleRecording()
    }

    private val step get() = app.state.value.fix!!.state.step

    @Test
    fun `the fixing loop goes through its five steps, logs each try and goes on after step 5`() {
        fixWhere()
        assertEquals(Screen.Fix(0), app.state.value.screen)
        val view = app.state.value.fix!!
        assertEquals("Where", view.written)
        assertEquals(listOf("where"), view.syllables)
        assertEquals("वेयर", view.soundHi)
        assertEquals(1, step)
        app.toggleRecording() // steps 1 and 2 need Next, not the mic
        assertEquals(1, step)
        app.fixNext()
        app.fixNext()
        assertEquals(3, step)
        app.fixNext() // step 3 needs the mic
        assertEquals(3, step)

        say("where")
        assertEquals(4, step)
        assertEquals(MessageKind.OK, app.state.value.message!!.kind)
        say("where is")
        assertEquals(5, step)
        say("where is the bus stop")
        assertEquals("step 5 passed: on to the next sentence", Screen.Practice(1), app.state.value.screen)

        val progress = store.progress(id)
        assertEquals("20 for the attempt, 5 for each of steps 3-5", 35, progress.xpTotal)
        val rows = rows()
        assertEquals(listOf("sentence", "fix step 3", "fix step 4", "fix step 5"), rows.map { it.mode })
        assertEquals(listOf("Where is the bus stop?", "Where", "Where is", "Where is the bus stop?"), rows.map { it.target })
        assertEquals(listOf(AttemptResult.SCORED, AttemptResult.PASSED, AttemptResult.PASSED, AttemptResult.PASSED), rows.map { it.result })
        assertEquals(listOf(1, 2, 5), rows.drop(1).map { it.total })
        assertTrue("fix steps have no stars", rows.drop(1).all { it.stars == null })
        assertTrue("only sentence attempts keep a recording", store.recordingFile(id, "bus_01").isFile)
    }

    @Test
    fun `two failed tries at a step go back one step, and didn't hear you doesn't count`() {
        fixWhere()
        app.fixNext()
        app.fixNext()
        say("[BLANK_AUDIO]")
        assertEquals(3, step)
        assertEquals(0, app.state.value.fix!!.state.fails)
        say("were")
        assertEquals(3, step)
        assertTrue(app.state.value.message!!.message.en.startsWith("Try once more. We heard: “were”"))
        say("were")
        assertEquals(2, step)
        assertEquals("Let’s go back one step.", app.state.value.message!!.message.en)
        assertEquals(
            listOf(AttemptResult.SCORED, AttemptResult.NOT_HEARD, AttemptResult.FAILED, AttemptResult.FAILED),
            rows().map { it.result },
        )
        assertEquals("fix step 3", rows()[1].mode)
    }

    @Test
    fun `L and S play the word slowly on steps 1-3, the phrase or sentence on steps 4-5`() {
        fixWhere()
        spoken.clear()
        app.fixListen(slow = false)
        app.fixListen(slow = true)
        app.fixNext()
        app.fixNext()
        say("where")
        app.fixListen(slow = false)
        app.fixListen(slow = true)
        assertEquals(listOf("en 0.6 where", "en 0.6 where", "en 0.9 Where is", "en 0.6 Where is"), spoken)
    }

    @Test
    fun `back goes to the feedback again, and skip goes on`() {
        fixWhere()
        app.backToFeedback()
        assertEquals(Screen.Feedback(0), app.state.value.screen)
        assertEquals("Where", app.state.value.result!!.toFixWritten)
        app.fix()
        app.next()
        assertEquals(Screen.Practice(1), app.state.value.screen)
        assertNull(app.state.value.fix)
    }

    @Test
    fun `the goal screen lists tomorrow's words as written, in the order they entered the box`() {
        val all = lesson.sentences.map { it.id }
        store.saveProgress(id, LearnerProgress(doneByDay = mapOf(today to all.drop(1))))
        practise(0)
        say("were is a bus stop")
        app.next()
        assertEquals(Screen.GoalDone, app.state.value.screen)
        assertEquals(listOf("Where", "the"), app.wordsForTomorrow())
    }

    @Test
    fun `the fix word is shown as written`() {
        val s = Sentence("t", "Is the bus-stop near Patna, sir?", "", emptyList())
        assertEquals(listOf("Is", "bus", "stop", "Patna", "sir"), listOf(0, 2, 3, 5, 6).map { AppController.writtenWord(s, it) })
        assertEquals("21", AppController.writtenWord(Sentence("n", "Bus 21 goes", "", emptyList()), 2))
    }
}
