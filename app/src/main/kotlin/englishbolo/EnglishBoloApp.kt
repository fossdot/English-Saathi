package englishbolo

import android.app.Application
import android.os.SystemClock
import englishbolo.speech.SpeechEngine
import englishbolo.speech.WhisperRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class EnglishBoloApp : Application() {

    /** The speech model, loaded once per process, on its own thread, the first time a screen needs it. */
    val speech: SpeechEngine by lazy {
        SpeechEngine(
            scope = CoroutineScope(SupervisorJob()),
            dispatcher = Executors.newSingleThreadExecutor { Thread(it, "speech") }.asCoroutineDispatcher(),
            clock = SystemClock::elapsedRealtime,
            load = { WhisperRecognizer.load(assets) },
        )
    }
}
