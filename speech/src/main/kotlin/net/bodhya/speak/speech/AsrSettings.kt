package net.bodhya.speak.speech

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
