package net.bodhya.speak.speech

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig

/** Recognizer settings; the measurements behind them are in docs/asr-model-comparison.md. */
data class AsrSettings(
    val numThreads: Int = 2,
    /**
     * Silent 10 ms frames sherpa-onnx adds after the audio so Whisper finds
     * the end of the sentence. 1000 was as accurate as the official model;
     * 300 is faster with no significant loss.
     */
    val tailPaddings: Int = 1000,
) {
    init {
        require(numThreads >= 1)
        // sherpa-onnx silently turns 0 or less into 1000.
        require(tailPaddings > 0)
    }
}

/**
 * Whisper tiny.en through sherpa-onnx 1.13.8, with the decoder whose token
 * embedding is quantized too (docs/asr-model-comparison.md).
 *
 * It runs exactly as the spec asks: greedy decoding (the only method
 * sherpa-onnx has for Whisper) and no initial prompt (sherpa-onnx has no
 * prompt setting). It never sees the target sentence. The result has no
 * per-word confidence.
 *
 * Not thread-safe: load and use it from one background thread.
 */
class WhisperRecognizer private constructor(private val recognizer: OfflineRecognizer) : Transcriber, AutoCloseable {

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
        const val MODEL_DIR = "asr/whisper-tiny.en"
        val MODEL_FILES = listOf(
            "$MODEL_DIR/tiny.en-encoder.int8.onnx",
            "$MODEL_DIR/tiny.en-decoder.int8-embedding.onnx",
            "$MODEL_DIR/tiny.en-tokens.txt",
        )

        /**
         * Loads the model from the app's assets (44 MB, copied into memory).
         * Slow: call it once, off the main thread. Throws
         * [java.io.FileNotFoundException] if a model file is missing.
         */
        fun load(assets: AssetManager, settings: AsrSettings = AsrSettings()): WhisperRecognizer {
            // sherpa-onnx ends the whole process when a model file is missing, so check first.
            for (path in MODEL_FILES) assets.open(path).close()
            val (encoder, decoder, tokens) = MODEL_FILES
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    whisper = OfflineWhisperModelConfig(
                        encoder = encoder,
                        decoder = decoder,
                        language = "en",
                        task = "transcribe",
                        tailPaddings = settings.tailPaddings,
                    ),
                    tokens = tokens,
                    numThreads = settings.numThreads,
                    modelType = "whisper",
                ),
                // Any other method ends the process for Whisper.
                decodingMethod = "greedy_search",
            )
            return WhisperRecognizer(OfflineRecognizer(assets, config))
        }
    }
}
