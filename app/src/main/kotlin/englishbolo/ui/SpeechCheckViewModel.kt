package englishbolo.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import englishbolo.EnglishBoloApp
import englishbolo.domain.GameConfig
import englishbolo.domain.Score
import englishbolo.domain.WordResult
import englishbolo.speech.ArrayPcmSource
import englishbolo.speech.AudioRecordSource
import englishbolo.speech.EndReason
import englishbolo.speech.EngineState
import englishbolo.speech.MemoryStats
import englishbolo.speech.Recorder
import englishbolo.speech.Recording
import englishbolo.speech.WavReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/** A lesson sentence for the check. Lesson files arrive in milestone c. */
data class CheckSentence(val en: String, val hi: String)

val CHECK_SENTENCES = listOf(
    CheckSentence("Where is the bus stop?", "बस स्टॉप कहाँ है?"),
    CheckSentence("When will the bus come?", "बस कब आएगी?"),
    CheckSentence("One ticket to Patna, please.", "पटना का एक टिकट दीजिए।"),
    CheckSentence("How much is the ticket?", "टिकट कितने का है?"),
    CheckSentence("Does this bus go to Bettiah?", "क्या यह बस बेतिया जाती है?"),
)

sealed interface ModelState {
    data object Loading : ModelState
    data class Ready(val loadMs: Long) : ModelState
    data class Failed(val message: String) : ModelState
}

/** One recognised answer, compared with the sentence chosen when recording started. */
data class CheckResult(
    val source: String,
    val audioMs: Long,
    val endReason: EndReason,
    /** What the recognizer returned, word for word; null when it wasn't run because no speech was heard. */
    val recognized: String?,
    /** Null when nothing was heard: no speech, or only non-speech tags. */
    val transcript: String?,
    val words: List<WordResult>,
    val score: Score?,
    /** From the end of the recording to the result being ready; null if the recognizer wasn't run. */
    val resultMs: Long?,
    val memory: MemoryStats?,
)

/** A problem to show: what happened, in English and Hindi, and the technical detail. */
data class CheckError(val en: String, val hi: String, val detail: String)

data class SpeechCheckState(
    val model: ModelState = ModelState.Loading,
    val sentence: Int = 0,
    val testClips: List<String> = emptyList(),
    val listening: Boolean = false,
    val level: Float = 0f,
    val working: Boolean = false,
    val result: CheckResult? = null,
    val error: CheckError? = null,
)

/**
 * Milestone b's check that recording and recognition work end to end:
 * record (or replay a test clip through the same recorder), recognise,
 * and compare with a lesson sentence using the domain rules.
 */
class SpeechCheckViewModel(app: Application) : AndroidViewModel(app) {
    private val engine = (app as EnglishBoloApp).speech
    private val config = GameConfig()

    // Touched only on the main thread.
    private var recorder: Recorder? = null

    private val _state = MutableStateFlow(SpeechCheckState())
    val state: StateFlow<SpeechCheckState> = _state.asStateFlow()

    private val busy get() = _state.value.listening || _state.value.working
    private val modelReady get() = _state.value.model is ModelState.Ready

    init {
        val clips = app.assets.list(TEST_CLIP_DIR).orEmpty().filter { it.endsWith(".wav") }.sorted()
        _state.update { it.copy(testClips = clips) }
        viewModelScope.launch {
            engine.state.collect { s ->
                val model = when (s) {
                    EngineState.Loading -> ModelState.Loading
                    is EngineState.Ready -> ModelState.Ready(s.loadMs).also {
                        Log.i(TAG, "model loaded in ${s.loadMs} ms; ${MemoryStats.read()}")
                    }
                    is EngineState.Failed -> ModelState.Failed(s.error.message ?: s.error.toString()).also {
                        Log.e(TAG, "model failed to load", s.error)
                    }
                }
                _state.update { it.copy(model = model) }
            }
        }
    }

    fun selectSentence(index: Int) {
        if (busy) return
        _state.update { it.copy(sentence = index, result = null) }
    }

    /** Stops the current recording; returns false if nothing was being recorded. */
    fun stopIfListening(): Boolean {
        if (!_state.value.listening) return false
        recorder?.stop()
        return true
    }

    /** Records from the microphone. The screen asks for the permission first. */
    fun record() {
        val app = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        if (!modelReady || busy) return
        val r = Recorder(AudioRecordSource())
        recorder = r
        check(source = MICROPHONE, listening = true) { r.record { level -> _state.update { it.copy(level = level) } } }
    }

    /** Replays a bundled test clip through the recorder, as if it came from the microphone. */
    fun runTestClip(name: String) {
        if (!modelReady || busy) return
        val assets = getApplication<Application>().assets
        check(source = name, listening = false) {
            val pcm = assets.open("$TEST_CLIP_DIR/$name").use { WavReader.readPcm16Mono(it) }
            Recorder(ArrayPcmSource(pcm)).record()
        }
    }

    private fun check(source: String, listening: Boolean, capture: () -> Recording) {
        // The answer belongs to the sentence on screen when recording started.
        val sentence = CHECK_SENTENCES[_state.value.sentence]
        _state.update { it.copy(listening = listening, working = !listening, level = 0f, result = null, error = null) }
        viewModelScope.launch {
            try {
                val recording = withContext(Dispatchers.IO) { capture() }
                if (source == MICROPHONE && recording.noSound) {
                    Log.w(TAG, "the microphone delivered only digital silence (${recording.durationMs} ms)")
                    _state.update { it.copy(listening = false, working = false, level = 0f, error = NO_SOUND) }
                    return@launch
                }
                val ended = SystemClock.elapsedRealtime()
                _state.update { it.copy(listening = false, working = true, level = 0f) }
                val answer = checkAnswer(sentence.en, recording, config) { engine.transcribe(it) }
                val resultMs = SystemClock.elapsedRealtime() - ended
                val memory = MemoryStats.read()
                Log.i(
                    TAG,
                    "source=$source audio=${recording.durationMs}ms end=${recording.endReason} speech=${recording.speechHeard} " +
                        "result=${resultMs}ms text=\"${answer.recognized.orEmpty()}\" $memory",
                )
                val result = CheckResult(
                    source = source,
                    audioMs = recording.durationMs,
                    endReason = recording.endReason,
                    recognized = answer.recognized,
                    transcript = answer.alignment?.let { answer.recognized },
                    words = answer.alignment?.words.orEmpty(),
                    score = answer.score,
                    resultMs = answer.recognized?.let { resultMs },
                    memory = memory,
                )
                _state.update { it.copy(working = false, result = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing here may crash the app: show what went wrong instead.
                Log.e(TAG, "check failed for $source", e)
                _state.update { it.copy(listening = false, working = false, level = 0f, error = errorFor(source, e)) }
            } finally {
                recorder = null
            }
        }
    }

    private fun errorFor(source: String, e: Exception): CheckError {
        val detail = e.message ?: e.toString()
        return when {
            source == MICROPHONE && e is IOException ->
                CheckError("The microphone could not be used.", "माइक्रोफ़ोन इस्तेमाल नहीं हो सका।", detail)
            else -> CheckError("Something went wrong. Please try again.", "कुछ गड़बड़ हो गई। फिर से कोशिश कीजिए।", detail)
        }
    }

    override fun onCleared() {
        // The speech model belongs to the app, not to this screen, so only the microphone is released here.
        recorder?.stop()
    }

    companion object {
        const val TAG = "EnglishBolo"
        const val TEST_CLIP_DIR = "asr-test"
        private const val MICROPHONE = "microphone"
        private val NO_SOUND = CheckError(
            "The microphone sends no sound. It may be switched off in the phone's settings, or in use by a call.",
            "माइक्रोफ़ोन से कोई आवाज़ नहीं आ रही। हो सकता है वह फ़ोन की सेटिंग में बंद हो, या किसी कॉल में इस्तेमाल हो रहा हो।",
            "every sample was zero",
        )
    }
}
