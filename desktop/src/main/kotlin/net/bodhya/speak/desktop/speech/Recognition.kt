package net.bodhya.speak.desktop.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.bodhya.speak.speech.AsrSettings
import net.bodhya.speak.speech.EngineState
import net.bodhya.speak.speech.SpeechEngine
import java.io.File
import java.util.concurrent.Executors

/** Which model is in use, and whether it has loaded. */
data class RecognitionState(val model: SpeechModel, val engine: EngineState)

/** What the recognizer heard, and which model heard it (every attempt is logged with its model). */
data class Transcript(val text: String, val model: SpeechModel)

/** Speech recognition with a model that can be switched while the app runs. */
interface Recognition {
    val state: StateFlow<RecognitionState>

    /** Loads [model] instead of the current one. Recognitions already running finish on the old one. */
    fun switchTo(model: SpeechModel)

    /** Recognizes 16 kHz mono samples, waiting for the model if it is still loading. Throws the load error. */
    suspend fun transcribe(samples: FloatArray): Transcript
}

/**
 * The desktop app's speech recognition: one [SpeechEngine] per model, each on
 * its own thread (sherpa-onnx objects must stay on one thread). Switching
 * loads the new model, then frees the old one on its own thread.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ModelSwitcher(
    private val projectDir: File,
    private val settings: AsrSettings,
    first: SpeechModel,
) : Recognition {

    private class Loaded(val model: SpeechModel, val engine: SpeechEngine, val free: () -> Unit)

    private val current = MutableStateFlow(load(first))
    private val scope = CoroutineScope(SupervisorJob())

    override val state: StateFlow<RecognitionState> = current
        .flatMapLatest { loaded -> loaded.engine.state.map { RecognitionState(loaded.model, it) } }
        .stateIn(scope, SharingStarted.Eagerly, RecognitionState(first, EngineState.Loading))

    override fun switchTo(model: SpeechModel) {
        val old = current.value
        if (old.model == model) return
        current.value = load(model)
        old.free()
    }

    override suspend fun transcribe(samples: FloatArray): Transcript {
        val loaded = current.value
        return Transcript(loaded.engine.transcribe(samples), loaded.model)
    }

    private fun load(model: SpeechModel): Loaded {
        val executor = Executors.newSingleThreadExecutor { Thread(it, "speech-${model.id}").apply { isDaemon = true } }
        val dispatcher = executor.asCoroutineDispatcher()
        val scope = CoroutineScope(SupervisorJob())
        var recognizer: JvmWhisperRecognizer? = null
        val engine = SpeechEngine(scope, dispatcher, System::currentTimeMillis) {
            JvmWhisperRecognizer.load(model, model.files(projectDir), settings).also { recognizer = it }
        }
        // Queued behind any recognition still running on that thread, so the model is never freed mid-use.
        val free = { scope.launch(dispatcher) { recognizer?.close() }.invokeOnCompletion { scope.cancel(); executor.shutdown() }; Unit }
        return Loaded(model, engine, free)
    }
}
