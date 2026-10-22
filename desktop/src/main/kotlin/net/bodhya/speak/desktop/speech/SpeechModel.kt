package net.bodhya.speak.desktop.speech

import java.io.File

/** The files of one Whisper model: encoder, decoder and tokens. */
data class ModelFiles(val encoder: File, val decoder: File, val tokens: File) {
    val all: List<File> get() = listOf(encoder, decoder, tokens)
}

/**
 * The two Whisper models the desktop app can switch between (docs/decisions.md:
 * small.en by default, tiny.en as the fallback). Where their folders are is
 * [net.bodhya.speak.desktop.AppFiles]'s business: the repo, where
 * scripts/fetch_deps.sh puts them, or the app bundle.
 */
enum class SpeechModel(val id: String, private val encoder: String, private val decoder: String, private val tokens: String) {
    /** The Android app's 44.3 MB build, with the fully quantized decoder (docs/asr-model-comparison.md). */
    TINY_EN(
        "tiny.en",
        "tiny.en-encoder.int8.onnx",
        "tiny.en-decoder.int8-embedding.onnx",
        "tiny.en-tokens.txt",
    ),

    /** The official int8 build, 375.5 MB (docs/intel-macbook-benchmarks.md). */
    SMALL_EN(
        "small.en",
        "small.en-encoder.int8.onnx",
        "small.en-decoder.int8.onnx",
        "small.en-tokens.txt",
    ),
    ;

    /** The model's files in its folder [dir]. */
    fun files(dir: File): ModelFiles = ModelFiles(File(dir, encoder), File(dir, decoder), File(dir, tokens))

    companion object {
        fun byId(id: String): SpeechModel =
            entries.firstOrNull { it.id == id } ?: throw IllegalArgumentException("unknown model \"$id\"; use ${entries.joinToString { it.id }}")
    }
}
