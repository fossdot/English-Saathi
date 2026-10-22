package net.bodhya.speak.desktop.check

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.bodhya.speak.desktop.AppFiles
import net.bodhya.speak.desktop.speech.JvmWhisperRecognizer
import net.bodhya.speak.desktop.speech.NativeLibraries
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.domain.Aligner
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.domain.Scorer
import net.bodhya.speak.domain.WordStatus
import net.bodhya.speak.speech.ArrayPcmSource
import net.bodhya.speak.speech.AsrSettings
import net.bodhya.speak.speech.EngineState
import net.bodhya.speak.speech.Recorder
import net.bodhya.speak.speech.SpeechEngine
import net.bodhya.speak.speech.WavReader
import java.io.File
import java.util.concurrent.Executors
import kotlin.system.exitProcess

/** A test clip and the sentence said in it. */
private data class Clip(val file: File, val expected: String)

internal const val LIBRISPEECH_TEXT =
    "AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS"

/** The pilot's bus-stand sentences, as recorded by scripts/make_test_wavs.sh. */
private val BUS_SENTENCES = listOf(
    "Where is the bus stop?",
    "When will the bus come?",
    "One ticket to Patna, please.",
    "How much is the ticket?",
    "Does this bus go to Bettiah?",
)

/** Recognitions per clip: the first warms up, the rest give the median. */
private const val RUNS = 4

/**
 * Milestone D1's check (docs/desktop-plan.md): loads one Whisper model
 * through sherpa-onnx's Java library and runs the test clips through the
 * same Recorder, SpeechEngine and domain scoring the app uses. Prints, and
 * appends to the log file, the machine, the model, load time, time per
 * clip and memory. One model per process, so the memory figures belong to
 * that model.
 *
 * Arguments: project folder, model id (tiny.en or small.en), threads, log
 * folder. The log is `<os.arch>.log` in that folder.
 */
fun main(args: Array<String>) {
    if (args.size != 4) {
        System.err.println("usage: SpeechCheck <project folder> <tiny.en|small.en> <threads> <log folder>")
        exitProcess(2)
    }
    val projectDir = File(args[0])
    val model = SpeechModel.byId(args[1])
    val settings = AsrSettings(numThreads = args[2].toInt())
    val log = File(args[3], "${System.getProperty("os.arch")}.log").apply { parentFile.mkdirs() }
    fun out(line: String) {
        println(line)
        log.appendText(line + "\n")
    }

    out("== ${model.id}, ${settings.numThreads} threads, tail padding ${settings.tailPaddings}")
    out("machine: ${command("sysctl", "-n", "machdep.cpu.brand_string")}, ${Runtime.getRuntime().availableProcessors()} cores; macOS ${System.getProperty("os.version")}")
    out("java: ${System.getProperty("java.vendor")} ${System.getProperty("java.runtime.version")}, os.arch ${System.getProperty("os.arch")} (${command("sysctl", "-n", "sysctl.proc_translated").let { if (it == "1") "under Rosetta" else "native" }})")
    val nativeDir = NativeLibraries.use(File(projectDir, "desktop/native"))
    out("native libraries: $nativeDir")
    out("memory before loading: ${residentMb()} MB")

    val clips = buildList {
        add(Clip(File(projectDir, "app/src/androidTest/assets/asr-test/librispeech-0.wav"), LIBRISPEECH_TEXT))
        BUS_SENTENCES.forEachIndexed { i, s -> add(Clip(File(projectDir, "app/src/debug/assets/asr-test/bus_0${i + 1}.wav"), s)) }
    }
    val missing = clips.filterNot { it.file.isFile }
    if (missing.isNotEmpty()) out("missing clips (scripts/fetch_deps.sh, scripts/make_test_wavs.sh): ${missing.joinToString { it.file.name }}")

    val executor = Executors.newSingleThreadExecutor { Thread(it, "speech") }
    val scope = CoroutineScope(SupervisorJob())
    val engine = SpeechEngine(scope, executor.asCoroutineDispatcher(), System::currentTimeMillis) {
        JvmWhisperRecognizer.load(model, AppFiles.repo(projectDir).model(model), settings)
    }
    var failed = false
    runBlocking {
        when (val state = engine.state.first { it !is EngineState.Loading }) {
            is EngineState.Failed -> {
                out("FAILED to load: ${state.error}")
                failed = true
                return@runBlocking
            }
            is EngineState.Ready -> out("loaded in ${state.loadMs} ms; memory ${residentMb()} MB")
            EngineState.Loading -> error("unreachable")
        }
        var peakMb = residentMb()
        out("clip | audio s | first s | median s | heard | words right")
        for (clip in clips - missing.toSet()) {
            val recording = Recorder(ArrayPcmSource(WavReader.readPcm16Mono(clip.file.inputStream()))).record()
            if (!recording.speechHeard) {
                out("${clip.file.name} | ${"%.2f".format(recording.durationMs / 1000.0)} | - | - | (no speech detected) | -")
                failed = true
                continue
            }
            val times = mutableListOf<Double>()
            var heard = ""
            repeat(RUNS) {
                val start = System.nanoTime()
                heard = engine.transcribe(recording.samples)
                times += (System.nanoTime() - start) / 1e9
                peakMb = maxOf(peakMb, residentMb())
            }
            val alignment = Aligner.align(Normalizer.normalize(clip.expected), Normalizer.normalizeHeard(heard))
            val score = Scorer.score(alignment, GameConfig())
            val wrong = alignment.words.filter { it.status != WordStatus.CORRECT }.joinToString(" ") { "${it.word}:${it.status}" }
            // Both models get this clip word for word; anything else means the pipeline is broken.
            if (clip.expected == LIBRISPEECH_TEXT && score.correct != score.total) failed = true
            out(
                "${clip.file.name} | ${"%.2f".format(recording.durationMs / 1000.0)} | ${"%.2f".format(times.first())} | " +
                    "${"%.2f".format(times.drop(1).sorted()[(RUNS - 1) / 2])} | ${heard.trim()} | ${score.correct}/${score.total}" +
                    if (wrong.isEmpty()) "" else " ($wrong)",
            )
        }
        out("memory, highest seen: $peakMb MB")
    }
    scope.cancel()
    executor.shutdownNow()
    exitProcess(if (failed) 1 else 0)
}

/** This process's resident memory, from `ps` (macOS has no /proc). */
private fun residentMb(): Long =
    command("ps", "-o", "rss=", "-p", ProcessHandle.current().pid().toString()).trim().toLongOrNull()?.div(1024) ?: -1

private fun command(vararg cmd: String): String = runCatching {
    val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
    p.inputStream.bufferedReader().readText().trim().also { p.waitFor() }
}.getOrDefault("?")
