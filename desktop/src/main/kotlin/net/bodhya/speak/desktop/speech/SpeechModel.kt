package net.bodhya.speak.desktop.speech

import java.io.File

/** The files of one Whisper model: encoder, decoder and tokens. */
data class ModelFiles(val encoder: File, val decoder: File, val tokens: File) {
    val all: List<File> get() = listOf(encoder, decoder, tokens)
}

/**
 * The two Whisper models the desktop app can switch between until student
 * recordings decide the default (docs/decisions.md, "Desktop app: owner
 * answers"). Paths are relative to the project folder, where
 * scripts/fetch_deps.sh puts them.
 */
enum class SpeechModel(val id: String, private val dir: String, private val encoder: String, private val decoder: String, private val tokens: String) {
    /** The Android app's 44.3 MB build, with the fully quantized decoder (docs/asr-model-comparison.md). */
    TINY_EN(
        "tiny.en",
        "app/src/main/assets/asr/whisper-tiny.en",
        "tiny.en-encoder.int8.onnx",
        "tiny.en-decoder.int8-embedding.onnx",
        "tiny.en-tokens.txt",
    ),

    /** The official int8 build, 375.5 MB (docs/intel-macbook-benchmarks.md). */
    SMALL_EN(
        "small.en",
        "desktop/models/whisper-small.en",
        "small.en-encoder.int8.onnx",
        "small.en-decoder.int8.onnx",
        "small.en-tokens.txt",
    ),
    ;

    fun files(projectDir: File): ModelFiles {
        val d = File(projectDir, dir)
        return ModelFiles(File(d, encoder), File(d, decoder), File(d, tokens))
    }

    companion object {
        fun byId(id: String): SpeechModel =
            entries.firstOrNull { it.id == id } ?: throw IllegalArgumentException("unknown model \"$id\"; use ${entries.joinToString { it.id }}")
    }
}
