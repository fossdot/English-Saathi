package englishbolo.speech

import android.Manifest
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.concurrent.thread

/**
 * The real microphone. On the emulator it hears silence (or the computer's
 * microphone, if enabled), so this checks the limits, not recognition.
 *
 * Where no microphone can be opened at all (the Android 8.0 emulator image
 * refuses every input), the tests are skipped: the app then shows an error
 * instead of crashing, which is all that can be checked there.
 */
@RunWith(AndroidJUnit4::class)
class MicrophoneOnDeviceTest {
    @get:Rule
    val mic: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private fun recordOrSkip(recorder: Recorder, onLevel: (Float) -> Unit = {}): Recording = try {
        recorder.record(onLevel)
    } catch (e: MicrophoneUnavailableException) {
        Log.w(TAG, "no microphone on this device: ${e.message}")
        assumeTrue("no microphone on this device: ${e.message}", false)
        throw e
    }

    @Test
    fun recordsAtMost8Seconds() {
        val start = System.nanoTime()
        val recording = recordOrSkip(Recorder(AudioRecordSource()))
        val wallMs = (System.nanoTime() - start) / 1_000_000
        Log.i(TAG, "microphone: ${recording.durationMs} ms, ${recording.endReason}, speech=${recording.speechHeard}, took $wallMs ms")
        assertTrue(recording.durationMs <= 8_000)
        assertTrue(recording.endReason == EndReason.MAX_LENGTH || recording.endReason == EndReason.SILENCE_AFTER_SPEECH)
        assertTrue("took $wallMs ms", wallMs < 9_500)
    }

    @Test
    fun stopEndsTheRecordingAndFreesTheMicrophone() {
        val recorder = Recorder(AudioRecordSource())
        var started = false
        val recording = recordOrSkip(recorder) {
            // Time the stop from the first audio: opening the microphone can be slow.
            if (!started) {
                started = true
                thread {
                    Thread.sleep(500)
                    recorder.stop()
                }
            }
        }
        assertEquals(EndReason.STOPPED, recording.endReason)
        assertTrue("${recording.durationMs} ms", recording.durationMs in 300L..1_500L)
        // The microphone was released: a new recording can start at once.
        val again = Recorder(AudioRecordSource(), EndpointConfig(maxMs = 200)).record()
        assertEquals(EndReason.MAX_LENGTH, again.endReason)
    }

    private companion object {
        const val TAG = "EnglishBoloTest"
    }
}
