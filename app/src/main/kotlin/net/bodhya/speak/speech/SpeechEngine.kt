package net.bodhya.speak.speech

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Turns 16 kHz mono speech into text. Blocking. [WhisperRecognizer] is the real one. */
fun interface Transcriber {
    fun transcribe(samples: FloatArray): String
}

sealed interface EngineState {
    data object Loading : EngineState

    data class Ready(val loadMs: Long) : EngineState

    data class Failed(val error: Throwable) : EngineState
}

/**
 * The app's one speech recognizer. It starts loading the model as soon as it
 * is created, and does every load and recognition on [dispatcher], which must
 * be a single thread: sherpa-onnx's native objects must not be used from two
 * threads at once. It lives as long as the app process, so the model is
 * loaded only once.
 */
class SpeechEngine(
    scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    clock: () -> Long,
    load: () -> Transcriber,
) {
    private val _state = MutableStateFlow<EngineState>(EngineState.Loading)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    private val transcriber = CompletableDeferred<Transcriber>()

    init {
        scope.launch(dispatcher) {
            val start = clock()
            try {
                transcriber.complete(load())
                _state.value = EngineState.Ready(clock() - start)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Errors too: a speech library that can't load (UnsatisfiedLinkError, e.g. an
                // APK copied without the part for this phone's processor) must show up as
                // "the model failed to load", not end the app.
                transcriber.completeExceptionally(e)
                _state.value = EngineState.Failed(e)
            }
        }
    }

    /**
     * Recognizes [samples] on the engine's thread, waiting for the model if
     * it is still loading. Throws the load error if the model failed to load.
     */
    suspend fun transcribe(samples: FloatArray): String {
        val t = transcriber.await()
        return withContext(dispatcher) { t.transcribe(samples) }
    }
}
