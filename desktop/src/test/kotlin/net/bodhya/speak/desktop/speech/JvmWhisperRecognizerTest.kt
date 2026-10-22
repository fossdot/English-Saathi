package net.bodhya.speak.desktop.speech

import net.bodhya.speak.desktop.AppFiles
import net.bodhya.speak.domain.Normalizer
import net.bodhya.speak.speech.AsrSettings
import net.bodhya.speak.speech.WavReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException

/**
 * Runs the real models through sherpa-onnx's Java library, on this Mac's
 * own processor. The files come from scripts/fetch_deps.sh; the build checks
 * them before compiling.
 */
class JvmWhisperRecognizerTest {

    @Test
    fun tinyEnRecognizesTheLibriSpeechClipWordForWord() = assertRecognizes(SpeechModel.TINY_EN)

    @Test
    fun smallEnRecognizesTheLibriSpeechClipWordForWord() = assertRecognizes(SpeechModel.SMALL_EN)

    @Test
    fun aMissingModelFileIsAnErrorNotACrash() {
        val files = AppFiles.repo(projectDir).model(SpeechModel.TINY_EN).copy(decoder = File(projectDir, "no-such-decoder.onnx"))
        assertThrows(FileNotFoundException::class.java) {
            JvmWhisperRecognizer.load(SpeechModel.TINY_EN, files, AsrSettings())
        }
    }

    @Test
    fun knowsEachModelById() {
        assertEquals(SpeechModel.SMALL_EN, SpeechModel.byId("small.en"))
        assertThrows(IllegalArgumentException::class.java) { SpeechModel.byId("base.en") }
    }

    private fun assertRecognizes(model: SpeechModel) {
        val samples = File(projectDir, "app/src/androidTest/assets/asr-test/librispeech-0.wav").inputStream()
            .use { WavReader.toFloats(WavReader.readPcm16Mono(it)) }
        JvmWhisperRecognizer.load(model, AppFiles.repo(projectDir).model(model), AsrSettings(numThreads = 2)).use { asr ->
            assertEquals(Normalizer.normalize(LIBRISPEECH_TEXT), Normalizer.normalizeHeard(asr.transcribe(samples)))
        }
    }

    companion object {
        const val LIBRISPEECH_TEXT =
            "AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE THE SQUALID QUARTER OF THE BROTHELS"

        val projectDir = File(System.getProperty("projectDir") ?: error("run through Gradle: needs -DprojectDir"))

        @BeforeClass
        @JvmStatic
        fun loadNativeLibraries() {
            NativeLibraries.use(File(projectDir, "desktop/native"))
        }
    }
}
