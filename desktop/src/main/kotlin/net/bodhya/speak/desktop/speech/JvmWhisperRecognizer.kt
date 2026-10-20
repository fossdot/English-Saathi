package net.bodhya.speak.desktop.speech

import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import net.bodhya.speak.speech.AsrSettings
import net.bodhya.speak.speech.Transcriber
import java.io.FileNotFoundException

/**
 * Whisper through sherpa-onnx 1.13.8's Java library: the desktop
 * counterpart of the Android app's WhisperRecognizer, with the same
 * settings (greedy decoding, no prompt, never shown the target sentence).
 *
 * Not thread-safe: load and use it from one background thread
 * ([net.bodhya.speak.speech.SpeechEngine] does that).
 */
class JvmWhisperRecognizer private constructor(
    private val recognizer: OfflineRecognizer,
    val model: SpeechModel,
) : Transcriber, AutoCloseable {

    /** Transcribes 16 kHz mono samples in [-1, 1]. Slow: call it off the main thread. */
    override fun transcribe(samples: FloatArray): String {
        require(samples.isNotEmpty()) { "no audio" }
        val stream = recognizer.createStream()
        try {
            // One waveform per stream: a second acceptWaveform on the same stream aborts the process.
            stream.acceptWaveform(samples, SAMPLE_RATE)
            recognizer.decode(stream)
            return recognizer.getResult(stream).text
        } finally {
            stream.release()
        }
    }

    override fun close() = recognizer.release()

    companion object {
        const val SAMPLE_RATE = 16_000

        /**
         * Loads [model] from [files]. Slow: call it once, off the main thread.
         * [NativeLibraries.use] must have been called first. Throws
         * [FileNotFoundException] if a model file is missing.
         *
         * The files must be the checksummed ones (scripts/deps.sha256): a
         * damaged model file makes sherpa-onnx end the whole process.
         */
        fun load(model: SpeechModel, files: ModelFiles, settings: AsrSettings): JvmWhisperRecognizer {
            for (f in files.all) if (!f.isFile) throw FileNotFoundException("$f is missing; run scripts/fetch_deps.sh")
            val whisper = OfflineWhisperModelConfig.builder()
                .setEncoder(files.encoder.path)
                .setDecoder(files.decoder.path)
                .setLanguage("en")
                .setTask("transcribe")
                .setTailPaddings(settings.tailPaddings)
                .build()
            val modelConfig = OfflineModelConfig.builder()
                .setWhisper(whisper)
                .setTokens(files.tokens.path)
                .setNumThreads(settings.numThreads)
                .setModelType("whisper")
                // The Java library's default is true: it prints the whole config to stderr.
                .setDebug(false)
                .build()
            val config = OfflineRecognizerConfig.builder()
                .setOfflineModelConfig(modelConfig)
                // Any other method ends the process for Whisper.
                .setDecodingMethod("greedy_search")
                .build()
            return JvmWhisperRecognizer(OfflineRecognizer(config), model)
        }
    }
}
