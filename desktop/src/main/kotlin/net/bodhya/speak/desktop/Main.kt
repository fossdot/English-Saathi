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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bodhya.speak.desktop.app.AppController
import net.bodhya.speak.desktop.app.ResultsData
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
import net.bodhya.speak.desktop.ui.FixActions
import net.bodhya.speak.desktop.ui.FixScreen
import net.bodhya.speak.desktop.ui.GoalActions
import net.bodhya.speak.desktop.ui.GoalScreen
import net.bodhya.speak.desktop.ui.HomeActions
import net.bodhya.speak.desktop.ui.HomeScreen
import net.bodhya.speak.desktop.ui.Line
import net.bodhya.speak.desktop.ui.NewStudentActions
import net.bodhya.speak.desktop.ui.NewStudentScreen
import net.bodhya.speak.desktop.ui.PracticeActions
import net.bodhya.speak.desktop.ui.PracticeScreen
import net.bodhya.speak.desktop.ui.ResultsActions
import net.bodhya.speak.desktop.ui.ResultsScreen
import net.bodhya.speak.desktop.ui.SettingsInfo
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
import net.bodhya.speak.desktop.voice.VoiceHelp
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.Streaks
import net.bodhya.speak.speech.EngineState
import net.bodhya.speak.speech.WavReader
import net.bodhya.speak.speech.WavWriter
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.desktop.PreferencesHandler
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess

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
    val files = AppFiles.find(args)
    // English Saathi --self-test: headless checks of this copy of the app (docs/packaging.md).
    if ("--self-test" in args) exitProcess(SelfTest.run(files, System.out))
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
        LaunchedEffect(Unit) { setup = withContext(Dispatchers.IO) { runCatching { Setup.create(files) } } }
        val windowState = rememberWindowState(size = DpSize(1280.dp, 880.dp))
        var showSettings by remember { mutableStateOf(false) }
        val keys = remember { ShortcutKeys() }
        var app by remember { mutableStateOf<AppController?>(null) }
        var check by remember { mutableStateOf<SpeechCheckController?>(null) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "English Saathi",
            state = windowState,
            // Every press and release is seen here first, even one a text field then takes, so a held key never repeats a shortcut.
            onPreviewKeyEvent = { e ->
                keys.track(e.key, e.type == KeyEventType.KeyDown)
                false
            },
            onKeyEvent = { e ->
                val down = e.type == KeyEventType.KeyDown
                if (down && e.isMetaPressed && e.key == Key.Comma) {
                    showSettings = true
                    return@Window true
                }
                val a = app ?: return@Window false
                val screen = a.state.value.screen
                if (!down) return@Window false
                val shortcut = keys.shortcut(e.key, e.isMetaPressed || e.isCtrlPressed || e.isAltPressed, typing = screen == Screen.NewStudent)
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
                    // Steps 1–2: Enter is Next; steps 3–5: Space speaks. L and S play the word, phrase or sentence.
                    is Screen.Fix -> when (shortcut) {
                        Shortcut.SPEAK -> a.toggleRecording()
                        Shortcut.PRIMARY -> a.fixNext()
                        Shortcut.LISTEN -> a.fixListen(slow = false)
                        Shortcut.SLOWLY -> a.fixListen(slow = true)
                    }
                    Screen.GoalDone -> if (shortcut == Shortcut.PRIMARY) a.startPractice() else return@Window false
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
                        Screens(ready, parts, chooseCsvFile = { name -> chooseSaveFile(window, name) })
                        if (showSettings) {
                            DialogWindow(onCloseRequest = { showSettings = false }, title = "Settings", state = rememberDialogState(size = DpSize(760.dp, 720.dp))) {
                                Settings(ready, parts, choosePack = { choosePackFile(window) })
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
internal class AppParts(val app: AppController, val check: SpeechCheckController, val listening: Listening, val speech: LessonPreparer) {
    /** Leaving the window (another app comes to the front) stops listening and speech. */
    fun leave() {
        app.leave()
        check.leave()
    }

    companion object {
        fun create(ready: Setup, startOnCheck: Boolean = false): AppParts {
            val output = MacSpeechOutput(ready.speaker, ready.english, ready.hindi, ready.log)
            val speech = LessonPreparer(ready.speaker, ready.english, ready.log::write)
            val first = ready.library.lessons.first().lesson
            // The speech check uses the first lesson; a learner's own lesson is prepared when they are picked.
            speech.prepare(first)
            val check = newController(ready, output, first)
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
                ready.store, ready.library, output, listening,
                micReady = { ready.recognition.state.value.engine is EngineState.Ready },
                log = ready.log::write,
                onLessonChange = speech::prepare,
                permissionApp = ready.permissionApp,
            )
            if (startOnCheck) app.speechCheck()
            return AppParts(app, check, listening, speech)
        }
    }
}

/**
 * The main window's current screen. [chooseCsvFile] shows the macOS Save
 * dialog with a suggested name and returns the chosen file, or null if the
 * facilitator cancelled.
 */
@Composable
internal fun Screens(ready: Setup, parts: AppParts, chooseCsvFile: (String) -> File? = { null }) {
    val a = parts.app
    val c = parts.check
    val state by a.state.collectAsState()
    val checkState by c.state.collectAsState()
    val recognition by ready.recognition.state.collectAsState()
    val problems by ready.dataProblems.collectAsState()
    val mic by parts.listening.state.collectAsState()
    val prepared by parts.speech.ready.collectAsState()
    when (val screen = state.screen) {
        Screen.Students -> StudentsScreen(
            state,
            facilitator = { StartNotes(recognition, ready.notices, checkState, problems) },
            StudentsActions(a::pick, a::newStudent, a::results, a::hindiHelp),
        )
        Screen.NewStudent -> NewStudentScreen(
            state.students.map { it.learner.card }.toSet(), state.message?.message,
            NewStudentActions(a::addStudent, a::showStudents, a::hindiHelp),
        )
        Screen.Home -> HomeScreen(state, a.lesson, a.config, HomeActions(a::startPractice, a::openSentence, a::chooseLesson, a::showStudents, a::hindiHelp))
        is Screen.Practice -> PracticeScreen(
            state, mic, recognition, screen.ref,
            PracticeActions(a::toggleRecording, a::listen, a::home, a::showStudents, a::hindiHelp),
        )
        is Screen.Feedback -> state.result?.let { result ->
            FeedbackScreen(state.learner!!, result, FeedbackActions({ a.listen(slow = false) }, a::fix, a::next, a::showStudents, a::hindiHelp))
        }
        is Screen.Fix -> state.fix?.let { fix ->
            FixScreen(state, fix, mic, recognition, FixActions(a::fixNext, a::toggleRecording, a::fixListen, a::backToFeedback, a::next, a::showStudents, a::hindiHelp))
        }
        Screen.GoalDone -> {
            val streak = Streaks.view(state.progress.streak, state.today, a.config)
            GoalScreen(state, streak.days, streak.status, remember(state.progress) { a.wordsForTomorrow() }, GoalActions(a::startPractice, a::showStudents, a::home, a::hindiHelp))
        }
        Screen.Results -> {
            // Read again whenever the students change (a delete or rename in Settings while Results is open).
            var data by remember(state.students) { mutableStateOf<ResultsData?>(null) }
            LaunchedEffect(state.students) { data = withContext(Dispatchers.IO) { a.loadResults() } }
            val scope = rememberCoroutineScope()
            ResultsScreen(
                state, data, a.sentenceCount(),
                ResultsActions(
                    back = a::showStudents,
                    save = { chooseCsvFile(a.csvFileName())?.let { file -> scope.launch(Dispatchers.IO) { a.saveCsv(file) } } },
                    help = a::hindiHelp,
                ),
            )
        }
        Screen.SpeechCheck -> SpeechCheckScreen(
            c.lesson, checkState, recognition,
            CheckInfo(ready.english?.name, ready.hindi?.name, ready.notices, ready.log.file.path, prepared == c.lesson),
            CheckActions(c::selectSentence, c::listen, c::hindiHelp, c::toggleRecording),
            onBack = {
                // The speech check records on its own: stop it and drop its result, so only one microphone line is ever open.
                c.close()
                a.showStudents()
            },
        )
    }
}

/** The Settings window's content (⌘,). [choosePack] shows the macOS Open dialog for a lesson pack. */
@Composable
internal fun Settings(ready: Setup, parts: AppParts, choosePack: () -> File? = { null }) {
    val a = parts.app
    val c = parts.check
    val state by a.state.collectAsState()
    val checkState by c.state.collectAsState()
    val recognition by ready.recognition.state.collectAsState()
    SettingsPanel(
        recognition.model, checkState.input, ready.inputs, ready.builtIn, ready.clips.keys.toList(), state.students, a.packs(),
        SettingsInfo(ready.english?.name, ready.hindi?.name, VoiceHelp.settingsPath(System.getProperty("os.version")), ready.dataDir.path, ready.log.file.path),
        SettingsActions(
            chooseModel = c::chooseModel,
            chooseInput = c::chooseInput,
            checkMicrophone = c::checkMicrophone,
            openSpeechCheck = a::speechCheck,
            runClip = { clip -> a.speechCheck(); c.runClip(clip) },
            rename = a::renameStudent,
            delete = a::deleteStudent,
            openDataFolder = { runCatching { Desktop.getDesktop().open(ready.dataDir) }.onFailure { ready.log.write("could not open the data folder: $it") } },
            setOpenAll = a::setOpenAll,
            choosePack = choosePack,
            checkPack = a::checkPack,
            installPack = a::installPack,
        ),
    )
}

/** The macOS Open dialog for a lesson pack (a .json file), from the Settings window. */
private fun choosePackFile(parent: java.awt.Dialog): File? {
    val dialog = FileDialog(parent, "Choose a lesson pack", FileDialog.LOAD)
    dialog.directory = File(System.getProperty("user.home"), "Downloads").path
    dialog.setFilenameFilter { _, name -> name.endsWith(".json", ignoreCase = true) }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return File(dialog.directory, file)
}

/** The macOS Save dialog for the CSV, suggesting [name] in Documents; a ".csv" is added if the name lost it. */
private fun chooseSaveFile(parent: java.awt.Frame, name: String): File? {
    val dialog = FileDialog(parent, "Save the results", FileDialog.SAVE)
    dialog.directory = File(System.getProperty("user.home"), "Documents").path
    dialog.file = name
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return File(dialog.directory, if (file.endsWith(".csv", ignoreCase = true)) file else "$file.csv")
}

/**
 * Prepares one lesson's speech at a time, on its own thread, in memory
 * (docs/decisions.md, "Speech is prepared per lesson"). When another lesson
 * is asked for meanwhile, only the newest is prepared. [ready] is the lesson
 * whose speech is ready.
 */
internal class LessonPreparer(private val speaker: Speaker, private val voice: Voice?, private val log: (String) -> Unit) {
    private val thread = Executors.newSingleThreadExecutor { Thread(it, "lesson-speech").apply { isDaemon = true } }
    private val wanted = java.util.concurrent.atomic.AtomicReference<Lesson?>(null)
    private val _ready = MutableStateFlow<Lesson?>(null)
    val ready: StateFlow<Lesson?> = _ready

    fun prepare(lesson: Lesson) {
        val v = voice ?: return
        wanted.set(lesson)
        thread.execute {
            if (wanted.get() != lesson || _ready.value == lesson) return@execute
            runCatching {
                val start = System.nanoTime()
                val bytes = speaker.prepareLesson(v, lesson)
                log("speech for ${lesson.id} prepared in ${(System.nanoTime() - start) / 1_000_000} ms, ${bytes / 1024} KB in memory")
                _ready.value = lesson
            }.onFailure { log("speech for ${lesson.id} not prepared: $it") }
        }
    }
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

private fun newController(ready: Setup, output: SpeechOutput, lesson: Lesson) = SpeechCheckController(
    lesson = lesson,
    recognition = ready.recognition,
    output = output,
    openMicrophone = { input -> JavaSoundPcmSource(input) },
    permission = MicAccess::permission,
    readClip = { name -> ready.clips.getValue(name).inputStream().use { WavReader.readPcm16Mono(it) } },
    log = ready.log::write,
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    io = Dispatchers.IO,
    permissionApp = ready.permissionApp,
    input = ready.input,
    // Only a choice in Settings is saved: a fallback at start keeps the saved microphone for when it is back.
    saveModel = { model -> ready.settings.update { it.copy(model = model) } },
    saveInput = { input ->
        val choice = when (input) {
            null -> MicChoice.MacDefault
            ready.builtIn -> MicChoice.BuiltIn
            else -> MicChoice.Named(input)
        }
        ready.settings.update { it.copy(microphone = choice) }
    },
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
