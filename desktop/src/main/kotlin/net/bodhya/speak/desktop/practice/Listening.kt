package net.bodhya.speak.desktop.practice

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
import net.bodhya.speak.speech.PcmSource
import net.bodhya.speak.speech.Recorder
import net.bodhya.speak.speech.Recording
import java.io.IOException

/** What one press of the mic produced. */
sealed interface Heard {
    /** The recognizer ran: [text] as it returned it (it may hold no words, e.g. "[BLANK_AUDIO]"). */
    data class Words(val text: String, val model: SpeechModel, val recording: Recording, val waitMs: Long) : Heard

    /** Nothing was loud enough to be speech, so the recognizer wasn't run (it would invent words). */
    data class NothingLoud(val recording: Recording) : Heard

    /** A second or more of exact zeros: the macOS permission, the closed lid, or another app has the microphone. */
    data class Silent(val recording: Recording, val permission: MicPermission) : Heard

    /** The microphone couldn't be opened or read. */
    data class NoMicrophone(val detail: String?) : Heard

    /** The recognizer failed (for example, the model didn't load). */
    data class Failed(val error: Throwable) : Heard
}

/**
 * Records one answer from the Mac's microphone and recognizes it, for the
 * practice and fixing screens: milestone b's recorder and silence rules, the
 * model chosen in Settings, and the input chosen there ([input]).
 *
 * One answer at a time. The caller decides what a result means and drops a
 * result that arrives after the learner moved on (docs/decisions.md, "Tie a
 * recording to its screen and step").
 */
class Listening(
    private val recognition: Recognition,
    private val openMicrophone: (input: String?) -> PcmSource,
    private val input: () -> String?,
    private val permission: () -> MicPermission,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val log: (String) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** [listening] while the microphone is open; [working] while the recognizer runs; [level] 0–1 for the mic ring. */
    data class State(val listening: Boolean = false, val working: Boolean = false, val level: Float = 0f) {
        val busy: Boolean get() = listening || working
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    @Volatile
    private var recorder: Recorder? = null

    /** Opens the microphone and calls [onHeard] on [scope] when done. Returns false if an answer is already in progress. */
    fun start(onHeard: (Heard) -> Unit): Boolean {
        if (_state.value.busy) return false
        val r = try {
            Recorder(openMicrophone(input()))
        } catch (e: IOException) {
            onHeard(Heard.NoMicrophone(e.message))
            return true
        }
        recorder = r
        _state.value = State(listening = true)
        scope.launch {
            val heard = try {
                val recording = withContext(io) { r.record { level -> _state.update { it.copy(level = level) } } }
                _state.value = State(working = true)
                when {
                    recording.noSound -> Heard.Silent(recording, withContext(io) { permission() })
                    !recording.speechHeard -> Heard.NothingLoud(recording)
                    else -> {
                        val start = clock()
                        val transcript = recognition.transcribe(recording.samples)
                        Heard.Words(transcript.text, transcript.model, recording, clock() - start)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Heard.NoMicrophone(e.message)
            } catch (e: Exception) {
                log("listening failed: $e")
                Heard.Failed(e)
            } finally {
                recorder = null
                _state.value = State()
            }
            onHeard(heard)
        }
        return true
    }

    /** Stops recording now (the mic pressed again, or the learner left); the answer so far is still recognized. */
    fun stop() {
        recorder?.stop()
    }
}
