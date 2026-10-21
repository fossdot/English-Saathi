package net.bodhya.speak.desktop.check

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.mic.MicPermission
import net.bodhya.speak.desktop.speech.Recognition
import net.bodhya.speak.desktop.speech.RecognitionState
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.desktop.speech.Transcript
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Score
import net.bodhya.speak.speech.ArrayPcmSource
import net.bodhya.speak.speech.EngineState
import net.bodhya.speak.speech.PcmSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * The speech check without a window, a microphone or a model. Everything
 * runs on the calling thread (Dispatchers.Unconfined), so each action has
 * finished when it returns, unless the fake recognizer is made to wait.
 */
class SpeechCheckControllerTest {

    private val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))
    private val lesson = LessonLoader.loadLesson(
        "bus_stand.json", File(projectDir, "content/bus_stand.json").readText(),
        LessonLoader.loadHints("hints.json", File(projectDir, "content/hints.json").readText()), GameConfig(),
    ).lesson

    private val log = mutableListOf<String>()
    private val played = mutableListOf<String>()

    /** Speech output that records what it was asked to do. */
    private val output = object : SpeechOutput {
        override fun english(text: String, speed: Float) { played += "en $speed $text" }
        override fun hindi(text: String) { played += "hi $text" }
        override fun stop() { played += "stop" }
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
    private var permission = MicPermission.ALLOWED
    private val saved = mutableListOf<String>()

    private val controller = SpeechCheckController(
        lesson, recognition, output,
        openMicrophone = { microphone() },
        permission = { permission },
        readClip = { spoken() },
        log = { log += it },
        scope = CoroutineScope(Dispatchers.Unconfined),
        io = Dispatchers.Unconfined,
        saveRecording = { id, _ -> saved += id; "/data/$id.wav" },
    )

    /** Room noise, then a second of a loud tone (speech, as far as the endpointer can tell). */
    private fun spoken(): ShortArray {
        val r = Random(1)
        val noise = ShortArray(8_000) { (r.nextInt(-20, 21)).toShort() }
        val tone = ShortArray(16_000) { (12_000 * sin(2 * PI * 220 * it / 16_000)).toInt().toShort() }
        return noise + tone
    }

    @Test
    fun `a recording is recognized, scored against the sentence and logged with its model`() {
        controller.toggleRecording()
        val r = controller.state.value.result!!
        assertEquals("bus_01", r.sentenceId)
        assertEquals(Score(5, 5, 3), r.score)
        assertEquals(SpeechModel.SMALL_EN, r.model)
        assertTrue(log.last(), log.last().startsWith("result | model=small.en | sentence=bus_01 | source=microphone"))
        assertTrue("logs what the endpointer saw: ${log.last()}", "speech from" in log.last() && log.last().endsWith("| saved /data/bus_01.wav"))
        assertEquals(listOf("bus_01"), saved)
        assertEquals("speech stops before the microphone opens", listOf("stop"), played)
        assertEquals(false, controller.state.value.listening)
    }

    @Test
    fun `place names are unscored and Whisper's 2 for to is right`() {
        controller.selectSentence(2)
        recognition.heard = "1 ticket 2 but now please"
        controller.toggleRecording()
        assertEquals(Score(4, 4, 3), controller.state.value.result!!.score)
    }

    @Test
    fun `a blocked microphone says how to allow Terminal`() {
        microphone = { ArrayPcmSource(ShortArray(0)) } // nothing but exact zeros, for the full 8 s
        permission = MicPermission.DENIED
        controller.toggleRecording()
        val m = controller.state.value.message!!
        assertEquals(MessageKind.PROBLEM, m.kind)
        assertTrue(m.message.en, m.message.en.contains("Privacy & Security › Microphone, turn on Terminal"))
        assertEquals("the recognizer is not run on silence", 0, recognition.calls)
    }

    @Test
    fun `nothing loud enough is didn't hear you, without running the recognizer`() {
        val r = Random(2)
        microphone = { ArrayPcmSource(ShortArray(16_000) { (r.nextInt(-20, 21)).toShort() }) }
        controller.toggleRecording()
        assertEquals(SpeechCheckController.DIDNT_HEAR, controller.state.value.message!!.message)
        assertNull(controller.state.value.result!!.words)
        assertEquals(0, recognition.calls)
    }

    @Test
    fun `only non-speech tags is didn't hear you`() {
        recognition.heard = "[BLANK_AUDIO]"
        controller.toggleRecording()
        assertEquals(SpeechCheckController.DIDNT_HEAR, controller.state.value.message!!.message)
        assertEquals(1, recognition.calls)
    }

    @Test
    fun `a result that arrives after the learner moved on is dropped`() {
        recognition.gate = CompletableDeferred()
        controller.toggleRecording()
        controller.selectSentence(3)
        recognition.gate!!.complete(Unit)
        assertNull(controller.state.value.result)
        assertTrue(log.last().startsWith("dropped a result for bus_01"))
    }

    @Test
    fun `a microphone that can't be opened is reported, not a crash`() {
        microphone = { throw IOException("the microphone \"AirPods\" is not connected") }
        controller.toggleRecording()
        assertTrue(controller.state.value.message!!.message.en.contains("AirPods"))
        assertEquals(false, controller.state.value.listening)
    }

    @Test
    fun `the one-second check tells sound, silence and no microphone apart`() {
        controller.checkMicrophone()
        assertTrue(controller.state.value.mic is MicStatus.Working)

        microphone = { ArrayPcmSource(ShortArray(0)) }
        permission = MicPermission.NOT_ASKED
        controller.checkMicrophone()
        assertEquals(MicStatus.Silent(MicPermission.NOT_ASKED), controller.state.value.mic)
        assertTrue(controller.state.value.message!!.message.en.contains("click Allow"))

        microphone = { throw IOException("no microphone") }
        controller.checkMicrophone()
        assertEquals(MicStatus.Unavailable("no microphone"), controller.state.value.mic)
    }

    @Test
    fun `listen plays the sentence at the pilot's speeds and help plays Hindi`() {
        controller.selectSentence(1)
        controller.listen(slow = false)
        controller.listen(slow = true)
        controller.hindiHelp()
        assertEquals(listOf("en 0.9 When will the bus come?", "en 0.6 When will the bus come?", "hi ${SpeechCheckController.HELP_HI}"), played)
    }

    @Test
    fun `choices in Settings are saved, and a restored input is used`() {
        val models = mutableListOf<SpeechModel>()
        val inputs = mutableListOf<String?>()
        val opened = mutableListOf<String?>()
        val c = SpeechCheckController(
            lesson, recognition, output,
            openMicrophone = { opened += it; microphone() },
            permission = { permission }, readClip = { spoken() }, log = { log += it },
            scope = CoroutineScope(Dispatchers.Unconfined), io = Dispatchers.Unconfined,
            input = "USB PnP Sound Device", saveModel = { models += it }, saveInput = { inputs += it },
        )
        assertEquals("USB PnP Sound Device", c.state.value.input)
        c.checkMicrophone()
        assertEquals(listOf<String?>("USB PnP Sound Device"), opened)
        c.chooseModel(SpeechModel.TINY_EN)
        c.chooseInput(null)
        assertEquals(listOf(SpeechModel.TINY_EN), models)
        assertEquals(listOf<String?>(null), inputs)
    }

    @Test
    fun `a choice that can't be saved is logged, not a crash`() {
        val c = SpeechCheckController(
            lesson, recognition, output, openMicrophone = { microphone() }, permission = { permission }, readClip = { spoken() },
            log = { log += it }, scope = CoroutineScope(Dispatchers.Unconfined), io = Dispatchers.Unconfined,
            saveInput = { throw IOException("disk full") },
        )
        c.chooseInput("AirPods")
        assertEquals("AirPods", c.state.value.input)
        assertTrue(log.last(), log.last().startsWith("settings not saved") && "disk full" in log.last())
    }

    @Test
    fun `test clips go through the same recorder`() {
        controller.runClip("bus_01.wav")
        assertEquals("bus_01.wav", controller.state.value.result!!.source)
        assertEquals("test clips are not saved", emptyList<String>(), saved)
        assertEquals(Score(5, 5, 3), controller.state.value.result!!.score)
    }
}
