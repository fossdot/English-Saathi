package net.bodhya.speak.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import net.bodhya.speak.desktop.app.AppState
import net.bodhya.speak.desktop.check.LIBRISPEECH_TEXT
import net.bodhya.speak.desktop.content.LessonLibrary
import net.bodhya.speak.desktop.mic.MicAccess
import net.bodhya.speak.desktop.speech.JvmWhisperRecognizer
import net.bodhya.speak.desktop.speech.NativeLibraries
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.desktop.ui.StudentsActions
import net.bodhya.speak.desktop.ui.StudentsScreen
import net.bodhya.speak.desktop.voice.SayRenderer
import net.bodhya.speak.desktop.voice.VoiceRole
import net.bodhya.speak.domain.Aligner
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.speech.ArrayPcmSource
import net.bodhya.speak.speech.AsrSettings
import net.bodhya.speak.speech.Recorder
import net.bodhya.speak.speech.WavReader
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * `English Saathi --self-test`: checks, where the app runs and without a
 * window, the microphone or the data folder, what a facilitator can't see
 * (docs/packaging.md): that the app's signature lets it record, that macOS
 * will ask for the microphone by name, that the licence notices are there,
 * that the lessons load, that both speech models hear the LibriSpeech clip
 * word for word on this processor, and that the window draws with its font.
 * Missing voices are reported, not failed: downloading them is a setup step.
 * Returns 0 when every check passed.
 */
object SelfTest {
    fun run(files: AppFiles, out: PrintStream): Int {
        var failures = 0
        fun check(what: String, block: () -> String) {
            val start = System.nanoTime()
            try {
                val detail = block()
                out.println("ok    $what: $detail (${(System.nanoTime() - start) / 1_000_000} ms)")
            } catch (e: Throwable) {
                failures++
                out.println("FAIL  $what: ${e.message ?: e}")
            }
        }
        out.println(
            "English Saathi self-test (${if (files.packaged) "the app" else "from Gradle"}): os.arch ${System.getProperty("os.arch")}, " +
                "macOS ${System.getProperty("os.version")}, Java ${System.getProperty("java.runtime.version")}",
        )

        check("the signature lets it record") {
            val j = MicAccess.checkJava()
            require(j.canRecord) { "${j.javaPath} has the hardened runtime without the audio-input entitlement, so it would only record silence" }
            "${j.javaPath}, hardened runtime ${j.hardened}"
        }
        if (files.packaged) {
            check("macOS asks for the microphone by name") {
                val plist = File(files.packs, "../../../../Info.plist").canonicalFile
                val p = ProcessBuilder("/usr/libexec/PlistBuddy", "-c", "Print :NSMicrophoneUsageDescription", plist.path).redirectErrorStream(true).start()
                val text = p.inputStream.bufferedReader().readText().trim()
                require(p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0 && text.isNotEmpty()) { "no NSMicrophoneUsageDescription in $plist" }
                "\"${text.lineSequence().first()}\""
            }
            check("licence notices") {
                val resources = File(files.packs, "../..").canonicalFile
                val needed = listOf("THIRD-PARTY-NOTICES.txt", "onnxruntime-LICENSE.txt", "onnxruntime-ThirdPartyNotices.txt").map { File(resources, it) } +
                    File(resources, "../../runtime/Contents/Home/legal/java.base/LICENSE").canonicalFile
                val missing = needed.filter { !it.isFile }
                require(missing.isEmpty()) { "missing ${missing.joinToString { it.path }}" }
                "${needed.size} files, the Java runtime's licence included"
            }
        }
        check("lessons") {
            val library = LessonLibrary.load(files.packs, Files.createTempDirectory("english-saathi-self-test").toFile(), GameConfig())
            "${library.packs.size} pack(s), ${library.lessons.size} lesson(s), ${library.sentenceCount} sentences"
        }
        check("speech library") { NativeLibraries.use(files.native).path }
        val clip = files.clips.firstOrNull { it.name == "librispeech-0.wav" }
        for (model in SpeechModel.entries) {
            check("speech model ${model.id}") {
                JvmWhisperRecognizer.load(model, files.model(model), AsrSettings(numThreads = 4)).use { asr ->
                    if (clip == null) return@use "loaded (no test clip here)"
                    val recording = Recorder(ArrayPcmSource(clip.inputStream().use { WavReader.readPcm16Mono(it) })).record()
                    val heard = asr.transcribe(recording.samples)
                    val alignment = Aligner.align(Normalizer.normalize(LIBRISPEECH_TEXT), Normalizer.normalizeHeard(heard))
                    require(alignment.correctCount == alignment.total) { "heard \"${heard.trim()}\"" }
                    "the LibriSpeech clip word for word"
                }
            }
        }
        check("window drawing and font") {
            val scene = ImageComposeScene(800, 500, Density(1f)) { StudentsScreen(AppState(today = LocalDate.now()), {}, StudentsActions({}, {}, {}, {})) }
            try {
                val image = scene.render()
                "${image.width}×${image.height} frame"
            } finally {
                scene.close()
            }
        }
        check("voices") {
            val voices = SayRenderer().listVoices()
            "English ${voices.choose(VoiceRole.ENGLISH)?.name ?: "missing (setup step)"}, Hindi ${voices.choose(VoiceRole.HINDI)?.name ?: "missing (setup step)"}"
        }
        out.println(if (failures == 0) "all checks passed" else "$failures check(s) FAILED")
        return if (failures == 0) 0 else 1
    }
}
