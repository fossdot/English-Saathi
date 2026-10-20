package net.bodhya.speak.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import net.bodhya.speak.desktop.check.SpeechCheckController
import net.bodhya.speak.desktop.check.SpeechOutput
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.mic.JavaSoundPcmSource
import net.bodhya.speak.desktop.mic.MicAccess
import net.bodhya.speak.desktop.speech.ModelSwitcher
import net.bodhya.speak.desktop.speech.NativeLibraries
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.desktop.ui.CheckActions
import net.bodhya.speak.desktop.ui.CheckInfo
import net.bodhya.speak.desktop.ui.Colors
import net.bodhya.speak.desktop.ui.Line
import net.bodhya.speak.desktop.ui.SettingsPanel
import net.bodhya.speak.desktop.ui.Shortcut
import net.bodhya.speak.desktop.ui.ShortcutKeys
import net.bodhya.speak.desktop.ui.SpeechCheckScreen
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.desktop.voice.SayRenderer
import net.bodhya.speak.desktop.voice.Speaker
import net.bodhya.speak.desktop.voice.Speeds
import net.bodhya.speak.desktop.voice.Voice
import net.bodhya.speak.desktop.voice.VoiceHelp
import net.bodhya.speak.desktop.voice.VoiceRole
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.speech.AsrSettings
import net.bodhya.speak.speech.WavReader
import net.bodhya.speak.speech.WavWriter
import java.awt.Desktop
import java.awt.desktop.PreferencesHandler
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** The model the switch starts on until the MacBook trip measures small.en (docs/decisions.md, Q3). */
private val FIRST_MODEL = SpeechModel.TINY_EN

/** Threads for Whisper: D1 measured 4 as the useful number under Rosetta; the MacBook trip checks 2, 4 and 6. */
private const val THREADS = 4

/** Everything the window needs, made once at start, off the main thread. */
internal class Setup(
    val projectDir: File,
    val dataDir: File,
    val log: SessionLog,
    val lesson: Lesson,
    val recognition: ModelSwitcher,
    val speaker: Speaker,
    val english: Voice?,
    val hindi: Voice?,
    val notices: List<Message>,
    val inputs: List<String>,
    val clips: Map<String, File>,
) {
    companion object {
        fun create(projectDir: File, dataDir: File = DataFolder.default): Setup {
            val excluded = DataFolder.prepare(dataDir)
            val log = SessionLog(File(dataDir, "logs/speech-check.log"))
            log.write("== English Saathi speech check (milestone D3)")
            log.write("machine: ${command("sysctl", "-n", "machdep.cpu.brand_string")}; macOS ${System.getProperty("os.version")}; " +
                "os.arch ${System.getProperty("os.arch")} (${if (command("sysctl", "-n", "sysctl.proc_translated") == "1") "under Rosetta" else "native"})")
            log.write("data folder $dataDir: ${if (excluded) "excluded from Time Machine" else "NOT excluded from Time Machine"}")
            val notices = mutableListOf<Message>()

            val java = MicAccess.checkJava()
            log.write("java: ${java.vendor} ${System.getProperty("java.runtime.version")} at ${java.javaPath}; hardened runtime ${java.hardened}, " +
                "audio-input entitlement ${java.hasAudioEntitlement} -> ${if (java.canRecord) "can record" else "CANNOT record"}")
            if (!java.canRecord) {
                notices += Message(
                    "This Java (${java.vendor}) can't use the microphone: it only ever records silence. Install Amazon Corretto 17 and start English Saathi with scripts/run_desktop.sh.",
                    "यह Java माइक इस्तेमाल नहीं कर सकता। Amazon Corretto 17 इंस्टॉल करके scripts/run_desktop.sh से खोलिए।",
                )
            }
            log.write("microphone permission (for the app that started Gradle): ${MicAccess.permission()}")
            val inputs = MicAccess.inputs()
            log.write("inputs: ${inputs.joinToString()}")

            NativeLibraries.use(File(projectDir, "desktop/native"))
            val config = GameConfig()
            val hints = LessonLoader.loadHints("hints.json", File(projectDir, "content/hints.json").readText())
            val loaded = LessonLoader.loadLesson("bus_stand.json", File(projectDir, "content/bus_stand.json").readText(), hints, config)
            loaded.warnings.forEach { log.write("lesson warning: $it") }

            val renderer = SayRenderer()
            val voices = renderer.listVoices()
            val macOs = System.getProperty("os.version")
            val english = voices.choose(VoiceRole.ENGLISH) ?: voices.englishFallback().also {
                notices += VoiceHelp.englishMissing(macOs, it)
            }
            val hindi = voices.choose(VoiceRole.HINDI)
            if (hindi == null) notices += VoiceHelp.hindiMissing(macOs)
            log.write("voices: English ${english?.name ?: "none"}, Hindi ${hindi?.name ?: "none"} (en_IN: " +
                "${voices.all.filter { it.locale == "en_IN" }.joinToString { it.name }}; hi_IN: ${voices.all.filter { it.locale == "hi_IN" }.joinToString { it.name }})")

            val clips = (listOf(File(projectDir, "app/src/androidTest/assets/asr-test/librispeech-0.wav")) +
                (1..5).map { File(projectDir, "app/src/debug/assets/asr-test/bus_0$it.wav") }).filter { it.isFile }.associateBy { it.name }

            return Setup(
                projectDir, dataDir, log, loaded.lesson,
                ModelSwitcher(projectDir, AsrSettings(numThreads = THREADS), FIRST_MODEL).also { log.write("model ${FIRST_MODEL.id}, $THREADS threads") },
                Speaker(renderer), english, hindi, notices, inputs, clips,
            )
        }

        private fun command(vararg cmd: String) = runCatching {
            val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
        }.getOrDefault("?")
    }
}

/**
 * Plays English and Hindi speech one at a time on its own thread. [stop]
 * also cancels speech that was asked for but hasn't started, so nothing
 * plays once the microphone is open.
 */
private class MacSpeechOutput(private val speaker: Speaker, private val englishVoice: Voice?, private val hindiVoice: Voice?, private val log: SessionLog) : SpeechOutput {
    private val thread = Executors.newSingleThreadExecutor { Thread(it, "speech-out").apply { isDaemon = true } }
    private val generation = AtomicInteger()

    override fun english(text: String, speed: Float) = say(englishVoice, text, speed)

    override fun hindi(text: String) = say(hindiVoice, text, Speeds.HINDI_HELP)

    override fun stop() {
        generation.incrementAndGet()
        speaker.stop()
    }

    private fun say(voice: Voice?, text: String, speed: Float) {
        voice ?: return
        stop()
        val mine = generation.get()
        thread.execute {
            runCatching {
                val speech = speaker.prepare(voice, text, speed)
                if (generation.get() == mine) speaker.play(speech)
            }.onFailure { log.write("speech failed: $it") }
        }
    }
}

fun main(args: Array<String>) {
    val projectDir = File(args.firstOrNull() ?: ".").absoluteFile
    application {
        var setup by remember { mutableStateOf<Result<Setup>?>(null) }
        LaunchedEffect(Unit) { setup = withContext(Dispatchers.IO) { runCatching { Setup.create(projectDir) } } }
        val windowState = rememberWindowState(size = DpSize(1280.dp, 880.dp))
        var showSettings by remember { mutableStateOf(false) }
        val keys = remember { ShortcutKeys() }
        var controller by remember { mutableStateOf<SpeechCheckController?>(null) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "English Saathi · Speech check",
            state = windowState,
            onKeyEvent = { e ->
                val down = e.type == KeyEventType.KeyDown
                if (down && e.isMetaPressed && e.key == Key.Comma) {
                    showSettings = true
                    return@Window true
                }
                val c = controller ?: return@Window false
                when (keys.onKey(e.key, down, e.isMetaPressed || e.isCtrlPressed || e.isAltPressed, typing = false)) {
                    Shortcut.SPEAK, Shortcut.PRIMARY -> c.toggleRecording()
                    Shortcut.LISTEN -> c.listen(slow = false)
                    Shortcut.SLOWLY -> c.listen(slow = true)
                    null -> return@Window false
                }
                true
            },
        ) {
            window.minimumSize = java.awt.Dimension(1024, 720)
            LaunchedEffect(Unit) {
                // The standard "English Saathi › Settings…" menu item (⌘,).
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_PREFERENCES)) {
                    Desktop.getDesktop().setPreferencesHandler(PreferencesHandler { showSettings = true })
                }
            }
            when (val s = setup) {
                null -> Message("Starting… (loading the speech library, the lesson and the voices)")
                else -> s.fold(
                    onSuccess = { ready ->
                        val c = remember(ready) { newController(ready) }
                        LaunchedEffect(c) {
                            controller = c
                            ready.log.write("window: render API ${window.renderApi}")
                            c.checkMicrophone()
                        }
                        DisposableEffect(c) {
                            val focus = object : WindowAdapter() {
                                override fun windowLostFocus(e: WindowEvent) = c.leave()
                            }
                            window.addWindowFocusListener(focus)
                            onDispose { window.removeWindowFocusListener(focus) }
                        }
                        var prepared by remember { mutableStateOf(false) }
                        LaunchedEffect(ready) {
                            ready.english?.let { voice ->
                                val start = System.nanoTime()
                                val bytes = withContext(Dispatchers.IO) { ready.speaker.prepareLesson(voice, ready.lesson) }
                                ready.log.write("lesson speech prepared in ${(System.nanoTime() - start) / 1_000_000} ms, ${bytes / 1024} KB in memory")
                            }
                            prepared = true
                        }
                        val state by c.state.collectAsState()
                        val recognition by ready.recognition.state.collectAsState()
                        SpeechCheckScreen(
                            ready.lesson, state, recognition,
                            CheckInfo(ready.english?.name, ready.hindi?.name, ready.notices, ready.log.file.path, prepared),
                            CheckActions(c::selectSentence, c::listen, c::hindiHelp, c::toggleRecording),
                        )
                        if (showSettings) {
                            DialogWindow(onCloseRequest = { showSettings = false }, title = "Settings", state = rememberDialogState(size = DpSize(760.dp, 640.dp))) {
                                SettingsPanel(
                                    recognition.model, state.input, ready.inputs, ready.clips.keys.toList(),
                                    onModel = c::chooseModel, onInput = c::chooseInput, onCheckMicrophone = c::checkMicrophone, onClip = c::runClip,
                                )
                            }
                        }
                    },
                    onFailure = { e -> Message("English Saathi couldn't start: ${e.message ?: e}") },
                )
            }
        }
    }
}

private fun newController(ready: Setup) = SpeechCheckController(
    lesson = ready.lesson,
    recognition = ready.recognition,
    output = MacSpeechOutput(ready.speaker, ready.english, ready.hindi, ready.log),
    openMicrophone = { input -> JavaSoundPcmSource(input) },
    permission = MicAccess::permission,
    readClip = { name -> ready.clips.getValue(name).inputStream().use { WavReader.readPcm16Mono(it) } },
    log = ready.log::write,
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    io = Dispatchers.IO,
    saveRecording = { sentenceId, recording ->
        // The latest recording of each sentence only, in the data folder (kept out of Time Machine).
        val file = File(ready.dataDir, "recordings/speech-check/$sentenceId.wav")
        file.parentFile.mkdirs()
        file.writeBytes(WavWriter.pcm16Mono(recording.samples, recording.sampleRate))
        file.path
    },
)

@Composable
private fun Message(text: String) {
    Box(Modifier.fillMaxSize().background(Colors.bg).padding(40.dp)) { Line(text) }
}
