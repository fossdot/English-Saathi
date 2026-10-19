package englishbolo.speech

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import englishbolo.domain.Aligner
import englishbolo.domain.GameConfig
import englishbolo.domain.Normalizer
import englishbolo.domain.Scorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileNotFoundException

/**
 * Milestone b's proof: the bundled model recognizes a test WAV on the
 * device, and how long that takes and how much memory it uses. Results go
 * to logcat under the tag "EnglishBoloTest".
 */
@RunWith(AndroidJUnit4::class)
class WhisperOnDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val appAssets = instrumentation.targetContext.assets
    private val testAssets = instrumentation.context.assets

    private fun testClip(name: String): ShortArray = testAssets.open("asr-test/$name").use { WavReader.readPcm16Mono(it) }

    private fun log(message: String) {
        Log.i(TAG, message)
        println("$TAG: $message")
    }

    /**
     * Its memory figures belong to the model alone only when it runs by
     * itself (see README); otherwise it says so in the log.
     */
    @Test
    fun recognizesTheLibriSpeechClipAndMeasuresTheEndToEndPipeline() {
        val before = MemoryStats.read()
        val loadStart = SystemClock.elapsedRealtime()
        val asr = WhisperRecognizer.load(appAssets)
        val loadMs = SystemClock.elapsedRealtime() - loadStart
        val loaded = MemoryStats.read()
        // In a full run, other tests may have loaded models in this process first.
        val isolated = before != null && before.peakKb - before.residentKb < 30 * 1024
        val note = if (isolated) "" else " (NOT ISOLATED: other tests ran first; run this test alone for memory figures)"
        log("model loaded in $loadMs ms; before: $before; after: $loaded$note")

        asr.use {
            // 1. Accuracy: LibriSpeech test-clean 0.wav, transcribed word for word.
            val clip = testClip("librispeech-0.wav")
            val text = asr.transcribe(WavReader.toFloats(clip))
            log("librispeech-0.wav (${clip.size / 16} ms): \"$text\"")
            assertEquals(Normalizer.normalize(LIBRISPEECH_0), Normalizer.normalizeHeard(text))

            // 2. The spec's latency target: a 3 s clip, from the end of the recording to the
            //    scored result. The audio goes through the recorder as live audio would.
            val threeSeconds = clip.copyOf(3 * 16_000)
            val times = (1..5).map {
                val recording = Recorder(ArrayPcmSource(threeSeconds), EndpointConfig(maxMs = 3_000)).record()
                val ended = SystemClock.elapsedRealtime()
                val heard = Normalizer.normalizeHeard(asr.transcribe(recording.samples))
                val score = Scorer.score(Aligner.align(Normalizer.normalize(LIBRISPEECH_0), heard), GameConfig())
                assertTrue(score.correct > 0)
                SystemClock.elapsedRealtime() - ended
            }
            log("3 s clip, end of recording -> scored result: ${times.joinToString()} ms, median ${times.sorted()[2]} ms")
            assertTrue("median ${times.sorted()[2]} ms is over the 3 s target", times.sorted()[2] < 3_000)

            // 3. The full 6.6 s clip, for comparison.
            val full = WavReader.toFloats(clip)
            val fullTimes = (1..3).map {
                val start = SystemClock.elapsedRealtime()
                asr.transcribe(full)
                SystemClock.elapsedRealtime() - start
            }
            log("6.6 s clip: ${fullTimes.joinToString()} ms")

            // Empty audio is refused before it reaches the native code.
            assertThrows(IllegalArgumentException::class.java) { asr.transcribe(FloatArray(0)) }
        }
        log("memory after recognition: ${MemoryStats.read()}")
    }

    /** The lesson sentences, when scripts/make_test_wavs.sh has made them (macOS). Logged, not asserted. */
    @Test
    fun logsTheLessonSentenceClips() {
        val clips = appAssets.list("asr-test").orEmpty().filter { it.endsWith(".wav") }.sorted()
        if (clips.isEmpty()) {
            log("no lesson sentence clips in the app's debug assets; skipped")
            return
        }
        WhisperRecognizer.load(appAssets).use { asr ->
            for (name in clips) {
                val pcm = appAssets.open("asr-test/$name").use { WavReader.readPcm16Mono(it) }
                val recording = Recorder(ArrayPcmSource(pcm)).record()
                val start = SystemClock.elapsedRealtime()
                val text = asr.transcribe(recording.samples)
                log("$name: ${recording.durationMs} ms, ${recording.endReason}, ${SystemClock.elapsedRealtime() - start} ms -> \"$text\"")
            }
        }
    }

    @Test
    fun threadCounts() {
        val clip = WavReader.toFloats(testClip("librispeech-0.wav").copyOf(3 * 16_000))
        for (threads in listOf(1, 2, 4)) {
            WhisperRecognizer.load(appAssets, AsrSettings(numThreads = threads)).use { asr ->
                asr.transcribe(clip) // warm-up
                val times = (1..5).map {
                    val start = SystemClock.elapsedRealtime()
                    asr.transcribe(clip)
                    SystemClock.elapsedRealtime() - start
                }.sorted()
                log("$threads thread(s), 3 s clip: median ${times[2]} ms (${times.joinToString()})")
            }
        }
    }

    @Test
    fun aMissingModelFileIsAnErrorNotACrash() {
        // The test APK's own assets have no model: sherpa-onnx would end the process here.
        try {
            WhisperRecognizer.load(testAssets)
            fail("expected FileNotFoundException")
        } catch (expected: FileNotFoundException) {
        }
    }

    private companion object {
        const val TAG = "EnglishBoloTest"
        const val LIBRISPEECH_0 =
            "AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS"
    }
}
