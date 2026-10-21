package net.bodhya.speak.desktop.check

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.bodhya.speak.desktop.mic.MicPermission
import net.bodhya.speak.desktop.speech.Recognition
import net.bodhya.speak.desktop.speech.SpeechModel
import net.bodhya.speak.desktop.voice.Message
import net.bodhya.speak.desktop.voice.Speeds
import net.bodhya.speak.domain.Aligner
import net.bodhya.speak.domain.GameConfig
import net.bodhya.speak.domain.Lesson
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.domain.Score
import net.bodhya.speak.domain.Scorer
import net.bodhya.speak.domain.WordResult
import net.bodhya.speak.speech.ArrayPcmSource
import net.bodhya.speak.speech.EndReason
import net.bodhya.speak.speech.Endpointer
import net.bodhya.speak.speech.PcmSource
import net.bodhya.speak.speech.Recorder
import net.bodhya.speak.speech.Recording
import java.io.IOException

/** "1 star", "2 stars". */
fun starsText(stars: Int): String = if (stars == 1) "1 star" else "$stars stars"

/** One recognized answer, compared with the sentence on screen when recording started. */
data class CheckResult(
    val sentenceId: String,
    /** "microphone", or the name of the test clip. */
    val source: String,
    val audioMs: Long,
    val endReason: EndReason,
    /** What the recognizer returned; null when it wasn't run because no speech was heard. */
    val heard: String?,
    val model: SpeechModel?,
    /** Null means "Didn't hear you". */
    val words: List<WordResult>?,
    val score: Score?,
    /** From the end of the recording to the result. */
    val waitMs: Long?,
)

/** The 1-second microphone check, run at start and from Settings. */
sealed interface MicStatus {
    data object NotChecked : MicStatus
    data object Checking : MicStatus

    /** Sound came in; [levelDbfs] is the room's loudest frame. */
    data class Working(val levelDbfs: Double) : MicStatus

    /** A second of exact zeros: the permission, the lid, or another app. */
    data class Silent(val permission: MicPermission) : MicStatus
    data class Unavailable(val detail: String) : MicStatus
}

enum class MessageKind { INFO, OK, PROBLEM }

data class ShownMessage(val kind: MessageKind, val message: Message)

data class SpeechCheckState(
    val sentence: Int = 0,
    val listening: Boolean = false,
    val level: Float = 0f,
    val working: Boolean = false,
    val result: CheckResult? = null,
    val message: ShownMessage? = null,
    val mic: MicStatus = MicStatus.NotChecked,
    /** The chosen recording input; null is the Mac's default. */
    val input: String? = null,
)

/** What the check window plays: English sentences and words, and Hindi instructions. */
interface SpeechOutput {
    fun english(text: String, speed: Float)
    fun hindi(text: String)

    /** Stops at once; called before the microphone opens. */
    fun stop()
}

/**
 * Milestone D3's speech check window, without the window: record from the
 * Mac's microphone (or replay a test clip through the same recorder),
 * recognize, and compare with a lesson sentence using the domain rules.
 * Every result goes to [log] with its model and what the endpointer saw;
 * [saveRecording] keeps the latest microphone recording of each sentence
 * (docs/decisions.md, "Recordings"), so the silence rules can be tuned on
 * real audio.
 *
 * Actions run on [scope]; blocking work runs on [io]. A result that arrives
 * after the learner chose another sentence is dropped (docs/decisions.md,
 * "Tie a recording to its screen and step").
 */
class SpeechCheckController(
    val lesson: Lesson,
    private val recognition: Recognition,
    private val output: SpeechOutput,
    private val openMicrophone: (input: String?) -> PcmSource,
    private val permission: () -> MicPermission,
    private val readClip: (name: String) -> ShortArray,
    private val log: (String) -> Unit,
    private val scope: CoroutineScope,
    /** Keeps [Recording] as the latest of sentence [String]; returns where, or null. Runs on [io]. */
    private val saveRecording: (String, Recording) -> String? = { _, _ -> null },
    /** The input to start on, restored from the data folder; null is the Mac's default. */
    input: String? = null,
    /** Keep the facilitator's choice of model and of microphone for the next start. */
    private val saveModel: (SpeechModel) -> Unit = {},
    private val saveInput: (String?) -> Unit = {},
    private val io: CoroutineDispatcher,
    private val config: GameConfig = GameConfig(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val _state = MutableStateFlow(SpeechCheckState(input = input))
    val state: StateFlow<SpeechCheckState> = _state.asStateFlow()

    /** Changes whenever the screen moves on; a result for an older one is dropped. */
    private var screen = 0
    private var recorder: Recorder? = null

    private val busy get() = _state.value.listening || _state.value.working || _state.value.mic == MicStatus.Checking

    fun selectSentence(index: Int) {
        require(index in lesson.sentences.indices)
        if (_state.value.listening) recorder?.stop()
        screen++
        _state.update { it.copy(sentence = index, result = null, message = null) }
    }

    fun listen(slow: Boolean) {
        output.english(lesson.sentences[_state.value.sentence].text, if (slow) Speeds.SLOW else Speeds.NORMAL)
    }

    fun hindiHelp() = output.hindi(HELP_HI)

    fun chooseModel(model: SpeechModel) {
        recognition.switchTo(model)
        log("model switched to ${model.id}")
        save { saveModel(model) }
    }

    fun chooseInput(input: String?) {
        _state.update { it.copy(input = input, mic = MicStatus.NotChecked) }
        log("input: ${input ?: "Mac's default"}")
        save { saveInput(input) }
    }

    private fun save(write: () -> Unit) {
        runCatching(write).onFailure { log("settings not saved: $it") }
    }

    /** Space and the mic button: start recording, or stop it early. */
    fun toggleRecording() {
        if (_state.value.listening) {
            recorder?.stop()
            return
        }
        if (busy) return
        output.stop()
        val r = try {
            Recorder(openMicrophone(_state.value.input))
        } catch (e: IOException) {
            showProblem(unavailable(e.message))
            return
        }
        recorder = r
        _state.update { it.copy(listening = true, level = 0f, message = null, result = null) }
        check("microphone") { r.record { level -> _state.update { s -> s.copy(level = level) } } }
    }

    /** Replays a test clip through the same recorder, as if it came from the microphone. */
    fun runClip(name: String) {
        if (busy) return
        output.stop()
        _state.update { it.copy(working = true, message = null, result = null) }
        check(name) { Recorder(ArrayPcmSource(readClip(name))).record() }
    }

    /** Leaving the window stops listening (milestone b: "Leaving the app stops listening"). */
    fun leave() {
        if (_state.value.listening) recorder?.stop()
        output.stop()
    }

    /**
     * Records one second and looks for sound. At first start this makes the
     * macOS permission prompt appear while the facilitator is there.
     */
    fun checkMicrophone() {
        if (busy) return
        output.stop()
        _state.update { it.copy(mic = MicStatus.Checking) }
        scope.launch {
            val status = withContext(io) { probe(_state.value.input) }
            log("microphone check: $status")
            _state.update {
                it.copy(
                    mic = status,
                    message = when (status) {
                        is MicStatus.Silent -> ShownMessage(MessageKind.PROBLEM, silent(status.permission))
                        is MicStatus.Unavailable -> ShownMessage(MessageKind.PROBLEM, unavailable(status.detail))
                        else -> it.message
                    },
                )
            }
        }
    }

    private fun probe(input: String?): MicStatus = try {
        openMicrophone(input).use { src ->
            src.start()
            val buffer = ShortArray(PROBE_SAMPLES)
            var filled = 0
            while (filled < buffer.size) {
                val n = src.read(buffer, filled, buffer.size - filled)
                if (n < 0) throw IOException("microphone read failed (error $n)")
                filled += n
            }
            if (buffer.all { it.toInt() == 0 }) {
                MicStatus.Silent(permission())
            } else {
                val frames = buffer.toList().chunked(320).map { f -> Endpointer.levelDbfs(FloatArray(f.size) { f[it] / 32768f }) }
                MicStatus.Working(frames.max())
            }
        }
    } catch (e: IOException) {
        MicStatus.Unavailable(e.message ?: e.toString())
    }

    private fun check(source: String, capture: () -> Recording) {
        val sentence = lesson.sentences[_state.value.sentence]
        val myScreen = screen
        scope.launch {
            try {
                val recording = withContext(io) { capture() }
                _state.update { it.copy(listening = false, working = true, level = 0f) }
                val saved = if (source == "microphone") withContext(io) { runCatching { saveRecording(sentence.id, recording) }.getOrNull() } else null
                val result = recognize(sentence.id, source, recording)
                if (myScreen != screen) {
                    log("dropped a result for ${sentence.id}: the screen moved on")
                    return@launch
                }
                log(logLine(result) + " | ${recording.stats ?: "-"}" + (saved?.let { " | saved $it" } ?: ""))
                _state.update { it.copy(result = result, message = messageFor(recording, result)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                showProblem(unavailable(e.message))
            } catch (e: Exception) {
                log("check failed: $e")
                showProblem(Message("Something went wrong: ${e.message ?: e}", "कुछ गड़बड़ हो गई। फिर कोशिश कीजिए।"))
            } finally {
                _state.update { it.copy(listening = false, working = false, level = 0f) }
                recorder = null
            }
        }
    }

    private suspend fun recognize(sentenceId: String, source: String, recording: Recording): CheckResult {
        val base = CheckResult(sentenceId, source, recording.durationMs, recording.endReason, null, null, null, null, null)
        // Nothing loud enough to be speech: the recognizer isn't run, so Whisper can't invent words.
        if (!recording.speechHeard) return base
        val start = clock()
        val transcript = recognition.transcribe(recording.samples)
        val wait = clock() - start
        val sentence = lesson.sentences.first { it.id == sentenceId }
        val heard = Normalizer.normalizeHeard(transcript.text)
        if (heard.isEmpty()) return base.copy(heard = transcript.text, model = transcript.model, waitMs = wait)
        val alignment = Aligner.align(sentence.targetWords, heard, lesson.accept).withUnscored(lesson.unscoredIn(sentence))
        return base.copy(
            heard = transcript.text, model = transcript.model, words = alignment.words,
            score = Scorer.score(alignment, config), waitMs = wait,
        )
    }

    private fun messageFor(recording: Recording, result: CheckResult): ShownMessage? = when {
        recording.noSound -> ShownMessage(MessageKind.PROBLEM, silent(permission()))
        result.words == null -> ShownMessage(MessageKind.INFO, DIDNT_HEAR)
        else -> null
    }

    private fun showProblem(m: Message) = _state.update { it.copy(message = ShownMessage(MessageKind.PROBLEM, m)) }

    private fun logLine(r: CheckResult) = listOf(
        "result", "model=${r.model?.id ?: "-"}", "sentence=${r.sentenceId}", "source=${r.source}", "audio_ms=${r.audioMs}",
        "end=${r.endReason}", "wait_ms=${r.waitMs ?: "-"}", "heard=${r.heard ?: "(no speech)"}",
        "score=${r.score?.let { "${it.correct}/${it.total} ${starsText(it.stars)}" } ?: "didn't hear you"}",
    ).joinToString(" | ")

    companion object {
        private const val PROBE_SAMPLES = 16_000

        /** The pilot's help line for the practice screen. */
        const val HELP_HI = "पहले वाक्य सुनिए। फिर नारंगी माइक बटन दबाइए और वाक्य बोलिए।"

        /** The pilot's wording. */
        val DIDNT_HEAR = Message("Didn’t hear you. Press the mic and speak a little louder.", "आवाज़ नहीं आई। माइक दबाकर थोड़ा ज़ोर से बोलिए।")

        // The messages below are drafted for the desktop app and need the teacher review.

        fun silent(permission: MicPermission): Message = when (permission) {
            MicPermission.DENIED, MicPermission.RESTRICTED -> Message(
                "The microphone is blocked. Open System Settings › Privacy & Security › Microphone, turn on Terminal, then press the mic again.",
                "माइक बंद है। System Settings › Privacy & Security › Microphone में Terminal चालू कीजिए, फिर माइक दबाइए।",
            )
            else -> Message(
                "The microphone sends no sound. If macOS asked about the microphone, click Allow and try again. Check that the lid is open and no other app is using the microphone.",
                "माइक से आवाज़ नहीं आ रही। अगर macOS ने माइक के बारे में पूछा तो Allow दबाकर फिर कोशिश कीजिए। देखिए कि लैपटॉप का ढक्कन खुला है।",
            )
        }

        fun unavailable(detail: String?): Message = Message(
            "No microphone could be opened${detail?.let { ": $it" } ?: ""}. Check that a microphone is connected.",
            "माइक नहीं खुला। देखिए कि माइक जुड़ा है।",
        )
    }
}
