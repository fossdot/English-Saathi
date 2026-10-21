package net.bodhya.speak.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.bodhya.speak.desktop.TestContent.market
import net.bodhya.speak.desktop.TestContent.packJson
import net.bodhya.speak.desktop.app.AppController
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
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.speech.ArrayPcmSource
import net.bodhya.speak.speech.EngineState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * An app update replaces English Saathi.app only: everything a session
 * made lives in the data folder and survives (owner, 2026-10-21). "The app"
 * here is its files folder (the lesson packs inside the app bundle); the
 * update deletes it and puts a newer one in its place.
 */
class AppUpdateTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-10-21T08:15:30Z"), ZoneId.of("Asia/Kolkata"))
    private val dataDir by lazy { tmp.newFolder("English Saathi") }
    private val output = object : SpeechOutput {
        override fun english(text: String, speed: Float) = Unit
        override fun hindi(text: String) = Unit
        override fun stop() = Unit
    }
    private val recognition = object : Recognition {
        var heard = ""
        override val state: StateFlow<RecognitionState> = MutableStateFlow(RecognitionState(SpeechModel.SMALL_EN, EngineState.Ready(1)))
        override fun switchTo(model: SpeechModel) = Unit
        override suspend fun transcribe(samples: FloatArray) = Transcript(heard, SpeechModel.SMALL_EN)
    }
    private val listening = Listening(recognition, {
        val r = Random(1)
        ArrayPcmSource(ShortArray(8_000) { (r.nextInt(-20, 21)).toShort() } + ShortArray(16_000) { (12_000 * sin(2 * PI * 220 * it / 16_000)).toInt().toShort() })
    }, { null }, { MicPermission.ALLOWED }, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined)

    /** An app's files: its lesson pack folder, with the app's own pack at [version]. */
    private fun appFiles(name: String, version: Int, extraSentence: Boolean): File {
        val dir = File(tmp.newFolder(name), "content/packs").also { it.mkdirs() }
        var text = File(TestContent.builtInDir, "starter.json").readText().replace("\"version\": 1", "\"version\": $version")
        if (extraSentence) {
            text = text.replace("""{ "id": "bus_05",""", """{ "id": "bus_06", "text": "Is this seat free?", "hi": "क्या यह सीट ख़ाली है?", "chunks": [] },
        { "id": "bus_05",""")
        }
        File(dir, "starter.json").writeText(text)
        return dir
    }

    private fun fingerprint(dir: File): Map<String, String> = dir.walkTopDown().filter { it.isFile }.associate { f ->
        f.relativeTo(dir).path to MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }
    }

    private fun session(packs: File) = AppController(LearnerStore(dataDir, clock), LessonLibrary.load(packs, dataDir, GameConfig()), output, listening, clock = clock)

    @Test
    fun `progress, attempts, recordings, added lessons and settings survive an app update`() {
        // App version 1: a learner practises, a pack is added, settings are chosen.
        val v1 = appFiles("app-v1", 1, extraSentence = false)
        v1.walkTopDown().forEach { it.setWritable(false) } // nothing may be written inside the app
        val v1Before = fingerprint(v1)
        val app = session(v1)
        val anita = LearnerStore(dataDir, clock).add("Anita", Card(CardColour.BLUE, CardSymbol.FISH)).id
        app.pick(anita)
        for (i in 0..4) {
            app.openSentence(i)
            recognition.heard = app.lesson.sentences[i].text
            app.toggleRecording()
        }
        val pack = File(tmp.root, "bihar1.json").also { it.writeText(packJson("bihar1", 1, 1, market)) }
        app.installPack(app.checkPack(pack) as PackCheck.Ready)
        SettingsFile(File(dataDir, "settings.json")).update { it.copy(model = SpeechModel.TINY_EN, microphone = MicChoice.Named("USB PnP Sound Device")) }
        assertEquals("the app's files are untouched by a whole session", v1Before, fingerprint(v1))

        val store = LearnerStore(dataDir, clock)
        val progress = store.progress(anita)
        val attempts = store.attempts(anita)
        val lessons = store.lessons(anita)
        val data = fingerprint(dataDir)
        assertEquals(5, attempts.size)
        assertEquals(listOf("bus_stand", "market"), lessons.opened)

        // The update: the old app is deleted and the new one, with version 2 of its own pack, takes its place.
        v1.walkTopDown().forEach { it.setWritable(true) }
        v1.parentFile.parentFile.deleteRecursively()
        val v2 = appFiles("app-v2", 2, extraSentence = true)
        val updated = session(v2)

        assertEquals("the data folder is exactly as the old app left it", data, fingerprint(dataDir))
        val after = LearnerStore(dataDir, clock)
        assertEquals(listOf("Anita"), after.learners().map { it.name })
        assertEquals(progress, after.progress(anita))
        assertEquals(attempts, after.attempts(anita))
        assertTrue(after.recordingFile(anita, "bus_01").isFile)
        assertEquals(SavedSettings(SpeechModel.TINY_EN, MicChoice.Named("USB PnP Sound Device")), SettingsFile(File(dataDir, "settings.json")).read())
        updated.pick(anita)
        assertEquals("the new app's own pack, and the added one", listOf("bus_stand" to 6, "market" to 2), updated.library.lessons.map { it.lesson.id to it.lesson.sentences.size })
        assertEquals("Market stays open although Bus stand gained a sentence", listOf(true, true), updated.state.value.lessons.map { it.open })
        assertEquals(2, updated.library.packs.single { it.pack.id == "starter" }.pack.version)
    }
}
