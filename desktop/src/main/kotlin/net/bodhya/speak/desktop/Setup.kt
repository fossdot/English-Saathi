package net.bodhya.speak.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import net.bodhya.speak.desktop.content.LessonLoader
import net.bodhya.speak.desktop.data.LearnerStore
import net.bodhya.speak.desktop.mic.MicAccess
import net.bodhya.speak.desktop.speech.ModelSwitcher
import net.bodhya.speak.desktop.speech.NativeLibraries
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.desktop.voice.SayRenderer
import net.bodhya.speak.desktop.voice.Speaker
import net.bodhya.speak.desktop.voice.Voice
import net.bodhya.speak.desktop.voice.VoiceHelp
import net.bodhya.speak.desktop.voice.VoiceRole
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.speech.AsrSettings
import java.io.File

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
    /** The input to start on: the saved one, or the built-in microphone when it isn't connected. */
    val input: String?,
    val settings: SettingsFile,
    val clips: Map<String, File>,
    val store: LearnerStore,
    /** Saved files that couldn't be read, found at start or later (nothing is deleted; details are in the log). */
    val dataProblems: StateFlow<List<String>>,
) {
    companion object {
        fun create(projectDir: File, dataDir: File = DataFolder.default): Setup {
            val excluded = DataFolder.prepare(dataDir)
            // Still called speech-check.log: the MacBook trip's checklist asks for this file.
            val log = SessionLog(File(dataDir, "logs/speech-check.log"))
            log.write("== English Saathi starts")
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

            val settings = SettingsFile(File(dataDir, "settings.json"))
            val saved = settings.read { log.write("settings: $it") }
            val input = MicAccess.restoreInput(saved.input, inputs, MicAccess::builtInInput)
            log.write("settings: model ${saved.model.id}, microphone ${saved.input ?: "Mac's default"}" +
                if (input != saved.input) " (not connected: using ${input ?: "Mac's default"})" else "")
            if (input != saved.input) notices += inputMissing(saved.input!!, input)

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

            val dataProblems = MutableStateFlow(emptyList<String>())
            val store = LearnerStore(dataDir) { problem ->
                log.write("data: $problem")
                dataProblems.update { it + problem }
            }
            log.write("learners: ${store.learners().size}")

            return Setup(
                projectDir, dataDir, log, loaded.lesson,
                ModelSwitcher(projectDir, AsrSettings(numThreads = THREADS), saved.model).also { log.write("model ${saved.model.id}, $THREADS threads") },
                Speaker(renderer), english, hindi, notices, inputs, input, settings, clips, store, dataProblems,
            )
        }

        /** Drafted for the desktop app; needs the teacher review. */
        fun inputMissing(saved: String, using: String?): Message = if (using != null) {
            Message(
                "The chosen microphone \"$saved\" isn't connected, so the Mac's built-in microphone ($using) is used. To use \"$saved\", connect it and open English Saathi again.",
                "चुना हुआ माइक \"$saved\" जुड़ा नहीं है, इसलिए Mac में लगा माइक इस्तेमाल हो रहा है। \"$saved\" के लिए उसे जोड़कर English Saathi फिर से खोलिए।",
            )
        } else {
            Message(
                "The chosen microphone \"$saved\" isn't connected, and no built-in microphone was found, so the Mac's default microphone is used. Check the microphone in Settings (⌘,).",
                "चुना हुआ माइक \"$saved\" जुड़ा नहीं है, इसलिए Mac का अपना माइक इस्तेमाल हो रहा है। Settings (⌘,) में माइक जाँचिए।",
            )
        }

        private fun command(vararg cmd: String) = runCatching {
            val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
        }.getOrDefault("?")
    }
}
