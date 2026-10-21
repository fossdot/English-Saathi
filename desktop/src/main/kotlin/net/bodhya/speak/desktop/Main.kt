package net.bodhya.speak.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import net.bodhya.speak.desktop.app.AppController
import net.bodhya.speak.desktop.app.Screen
import net.bodhya.speak.desktop.check.SpeechCheckController
import net.bodhya.speak.desktop.check.SpeechOutput
import net.bodhya.speak.desktop.data.AppLock
import net.bodhya.speak.desktop.practice.Listening
import net.bodhya.speak.desktop.mic.JavaSoundPcmSource
import net.bodhya.speak.desktop.mic.MicAccess
import net.bodhya.speak.desktop.ui.BigButton
import net.bodhya.speak.desktop.ui.Bilingual
import net.bodhya.speak.desktop.ui.CheckActions
import net.bodhya.speak.desktop.ui.CheckInfo
import net.bodhya.speak.desktop.ui.Colors
import net.bodhya.speak.desktop.ui.FeedbackActions
import net.bodhya.speak.desktop.ui.FeedbackScreen
import net.bodhya.speak.desktop.ui.HomeActions
import net.bodhya.speak.desktop.ui.HomeScreen
import net.bodhya.speak.desktop.ui.Icons
import net.bodhya.speak.desktop.ui.Line
import net.bodhya.speak.desktop.ui.NewStudentActions
import net.bodhya.speak.desktop.ui.NewStudentScreen
import net.bodhya.speak.desktop.ui.Page
import net.bodhya.speak.desktop.ui.PracticeActions
import net.bodhya.speak.desktop.ui.PracticeScreen
import net.bodhya.speak.desktop.ui.RoundIconButton
import net.bodhya.speak.desktop.ui.SettingsActions
import net.bodhya.speak.desktop.ui.SettingsPanel
import net.bodhya.speak.desktop.ui.Shortcut
import net.bodhya.speak.desktop.ui.ShortcutKeys
import net.bodhya.speak.desktop.ui.SpeechCheckScreen
import net.bodhya.speak.desktop.ui.StartNotes
import net.bodhya.speak.desktop.ui.StudentsActions
import net.bodhya.speak.desktop.ui.StudentsScreen
import net.bodhya.speak.desktop.voice.Speaker
import net.bodhya.speak.desktop.voice.Speeds
import net.bodhya.speak.desktop.voice.Voice
import net.bodhya.speak.speech.EngineState
import net.bodhya.speak.speech.WavReader
import net.bodhya.speak.speech.WavWriter
import java.awt.Desktop
import java.awt.desktop.PreferencesHandler
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

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
    // Taken before anything else: a second copy would overwrite the first one's progress.
    val lock = AppLock.acquire(DataFolder.default)
    // scripts/run_desktop.sh --check opens the facilitator's speech check (the MacBook trip).
    val startOnCheck = System.getenv("ENGLISH_SAATHI_SCREEN") == "check"
    application {
        if (lock == null) {
            AlreadyOpen(::exitApplication)
            return@application
        }
        var setup by remember { mutableStateOf<Result<Setup>?>(null) }
        LaunchedEffect(Unit) { setup = withContext(Dispatchers.IO) { runCatching { Setup.create(projectDir) } } }
        val windowState = rememberWindowState(size = DpSize(1280.dp, 880.dp))
        var showSettings by remember { mutableStateOf(false) }
        val keys = remember { ShortcutKeys() }
        var app by remember { mutableStateOf<AppController?>(null) }
        var check by remember { mutableStateOf<SpeechCheckController?>(null) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "English Saathi",
            state = windowState,
            onKeyEvent = { e ->
                val down = e.type == KeyEventType.KeyDown
                if (down && e.isMetaPressed && e.key == Key.Comma) {
                    showSettings = true
                    return@Window true
                }
                val a = app ?: return@Window false
                val screen = a.state.value.screen
                val shortcut = keys.onKey(e.key, down, e.isMetaPressed || e.isCtrlPressed || e.isAltPressed, typing = screen == Screen.NewStudent)
                    ?: return@Window false
                when (screen) {
                    Screen.Home -> if (shortcut == Shortcut.PRIMARY) a.startPractice() else return@Window false
                    // As in the pilot, Enter does nothing on the practice screen.
                    is Screen.Practice -> when (shortcut) {
                        Shortcut.SPEAK -> a.toggleRecording()
                        Shortcut.LISTEN -> a.listen(slow = false)
                        Shortcut.SLOWLY -> a.listen(slow = true)
                        Shortcut.PRIMARY -> return@Window false
                    }
                    is Screen.Feedback -> when (shortcut) {
                        Shortcut.PRIMARY -> a.feedbackPrimary()
                        Shortcut.LISTEN -> a.listen(slow = false)
                        else -> return@Window false
                    }
                    Screen.SpeechCheck -> {
                        val c = check ?: return@Window false
                        when (shortcut) {
                            Shortcut.SPEAK, Shortcut.PRIMARY -> c.toggleRecording()
                            Shortcut.LISTEN -> c.listen(slow = false)
                            Shortcut.SLOWLY -> c.listen(slow = true)
                        }
                    }
                    else -> return@Window false
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
                        val parts = remember(ready) { AppParts.create(ready, startOnCheck) }
                        LaunchedEffect(parts) {
                            app = parts.app
                            check = parts.check
                            ready.log.write("window: render API ${window.renderApi}")
                            // At every start, so the macOS prompt appears while the facilitator is there.
                            parts.check.checkMicrophone()
                        }
                        DisposableEffect(parts) {
                            val focus = object : WindowAdapter() {
                                override fun windowLostFocus(e: WindowEvent) = parts.leave()
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
                        Screens(ready, parts, prepared)
                        if (showSettings) {
                            DialogWindow(onCloseRequest = { showSettings = false }, title = "Settings", state = rememberDialogState(size = DpSize(760.dp, 720.dp))) {
                                Settings(ready, parts)
                            }
                        }
                    },
                    onFailure = { e -> Message("English Saathi couldn't start: ${e.message ?: e}") },
                )
            }
        }
    }
}

/** The controllers behind the main window, made once [Setup] is ready. */
internal class AppParts(val app: AppController, val check: SpeechCheckController, val listening: Listening) {
    /** Leaving the window (another app comes to the front) stops listening and speech. */
    fun leave() {
        app.leave()
        check.leave()
    }

    companion object {
        fun create(ready: Setup, startOnCheck: Boolean = false): AppParts {
            val output = MacSpeechOutput(ready.speaker, ready.english, ready.hindi, ready.log)
            val check = newController(ready, output)
            val listening = Listening(
                ready.recognition,
                openMicrophone = { input -> JavaSoundPcmSource(input) },
                // The input chosen in Settings, which the speech check keeps.
                input = { check.state.value.input },
                permission = MicAccess::permission,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
                io = Dispatchers.IO,
                log = ready.log::write,
            )
            val app = AppController(
                ready.store, ready.lesson, ready.hints, output, listening,
                micReady = { ready.recognition.state.value.engine is EngineState.Ready },
                log = ready.log::write,
            )
            if (startOnCheck) app.speechCheck()
            return AppParts(app, check, listening)
        }
    }
}

/** The main window's current screen. */
@Composable
internal fun Screens(ready: Setup, parts: AppParts, prepared: Boolean) {
    val a = parts.app
    val c = parts.check
    val state by a.state.collectAsState()
    val checkState by c.state.collectAsState()
    val recognition by ready.recognition.state.collectAsState()
    val problems by ready.dataProblems.collectAsState()
    val mic by parts.listening.state.collectAsState()
    when (val screen = state.screen) {
        Screen.Students -> StudentsScreen(
            state,
            facilitator = { StartNotes(recognition, ready.notices, checkState, problems) },
            StudentsActions(a::pick, a::newStudent, a::hindiHelp),
        )
        Screen.NewStudent -> NewStudentScreen(
            state.students.map { it.learner.card }.toSet(), state.message?.message,
            NewStudentActions(a::addStudent, a::showStudents, a::hindiHelp),
        )
        Screen.Home -> HomeScreen(state, ready.lesson, a.config, HomeActions(a::startPractice, a::openSentence, a::showStudents, a::hindiHelp))
        is Screen.Practice -> PracticeScreen(
            state, mic, recognition, ready.lesson, screen.sentence,
            PracticeActions(a::toggleRecording, a::listen, a::home, a::showStudents, a::hindiHelp),
        )
        is Screen.Feedback -> state.result?.let { result ->
            FeedbackScreen(state.learner!!, result, ready.lesson, FeedbackActions({ a.listen(slow = false) }, a::fix, a::next, a::showStudents, a::hindiHelp))
        }
        is Screen.Fix -> ToCome("Fix “${state.result?.toFixWritten}”", "शब्द ठीक करने की स्क्रीन अगले चरण (D7) में आएगी।", a::next)
        Screen.GoalDone -> ToCome("Goal complete, ${state.learner?.name}!", "लक्ष्य की स्क्रीन अगले चरण (D7) में आएगी।", a::home)
        Screen.SpeechCheck -> SpeechCheckScreen(
            ready.lesson, checkState, recognition,
            CheckInfo(ready.english?.name, ready.hindi?.name, ready.notices, ready.log.file.path, prepared),
            CheckActions(c::selectSentence, c::listen, c::hindiHelp, c::toggleRecording),
            onBack = a::showStudents,
        )
    }
}

/** The Settings window's content (⌘,). */
@Composable
internal fun Settings(ready: Setup, parts: AppParts) {
    val a = parts.app
    val c = parts.check
    val state by a.state.collectAsState()
    val checkState by c.state.collectAsState()
    val recognition by ready.recognition.state.collectAsState()
    SettingsPanel(
        recognition.model, checkState.input, ready.inputs, ready.clips.keys.toList(), state.students.map { it.learner },
        SettingsActions(
            chooseModel = c::chooseModel,
            chooseInput = c::chooseInput,
            checkMicrophone = c::checkMicrophone,
            openSpeechCheck = a::speechCheck,
            runClip = { clip -> a.speechCheck(); c.runClip(clip) },
            rename = a::renameStudent,
            delete = a::deleteStudent,
        ),
    )
}

/** A second copy of the app was started: say so and quit, leaving the first one's data alone. */
@Composable
private fun AlreadyOpen(exit: () -> Unit) {
    Window(onCloseRequest = exit, title = "English Saathi", state = rememberWindowState(size = DpSize(560.dp, 300.dp)), resizable = false) {
        Column(Modifier.fillMaxSize().background(Colors.bg).padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Drafted for the desktop app; needs the teacher review.
            Bilingual(
                "English Saathi is already open. Use the window that is open, or quit it first (⌘Q).",
                "English Saathi पहले से खुला है। खुली हुई विंडो इस्तेमाल कीजिए।",
                size = 20.sp,
            )
            BigButton("OK", "ठीक है", exit)
        }
    }
}

/** Until milestone D7 builds the fixing loop and the goal screen. */
@Composable
private fun ToCome(title: String, note: String, onward: () -> Unit) {
    Page {
        RoundIconButton(Icons.close, "Go on", onward, plain = true)
        Bilingual(title, note, size = 28.sp, bold = true)
    }
}

private fun newController(ready: Setup, output: SpeechOutput) = SpeechCheckController(
    lesson = ready.lesson,
    recognition = ready.recognition,
    output = output,
    openMicrophone = { input -> JavaSoundPcmSource(input) },
    permission = MicAccess::permission,
    readClip = { name -> ready.clips.getValue(name).inputStream().use { WavReader.readPcm16Mono(it) } },
    log = ready.log::write,
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    io = Dispatchers.IO,
    input = ready.input,
    // Only a choice in Settings is saved: a fallback at start keeps the saved microphone for when it is back.
    saveModel = { model -> ready.settings.update { it.copy(model = model) } },
    saveInput = { input -> ready.settings.update { it.copy(input = input) } },
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
