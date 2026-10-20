package net.bodhya.speak.desktop.voice

import net.bodhya.speak.speech.WavReader
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Speech from the macOS `say` command, as 16-bit mono samples, for the app to
 * slow down and play itself (docs/decisions.md, "Slow speech"). `say` writes
 * to a temporary file that is deleted as soon as it has been read: the macOS
 * licence allows the system voices while running macOS, not keeping or
 * sharing recordings of them.
 */
class SayRenderer(private val sayCommand: String = "say") {

    /** Lists the installed voices (`say -v '?'`). Takes about 0.4 s, so do it once. */
    fun listVoices(): Voices = Voices.parse(run(listOf(sayCommand, "-v", "?"), input = null).trim())

    /** Renders [text] with [voice] at its normal rate. Slow: call it off the main thread. */
    fun render(voice: Voice, text: String): ShortArray {
        val wav = File.createTempFile("saathi-say", ".wav")
        try {
            // The text goes in as UTF-8 on standard input: Java 17 would encode a
            // Hindi command-line argument by the system locale.
            run(
                listOf(sayCommand, "-v", voice.name, "-o", wav.path, "--file-format=WAVE", "--data-format=LEI16@$SAMPLE_RATE"),
                input = text,
            )
            return wav.inputStream().use { WavReader.readPcm16Mono(it, SAMPLE_RATE) }
        } finally {
            wav.delete()
        }
    }

    private fun run(command: List<String>, input: String?): String {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        process.outputStream.use { out -> input?.let { out.write(it.toByteArray(Charsets.UTF_8)) } }
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).readText()
        if (!process.waitFor(TIMEOUT_S, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw IOException("${command.first()} took more than $TIMEOUT_S s")
        }
        if (process.exitValue() != 0) throw IOException("${command.joinToString(" ")} failed (${process.exitValue()}): ${output.trim()}")
        return output
    }

    companion object {
        /** `say`'s output rate: enough for speech, half the size of 44.1 kHz. */
        const val SAMPLE_RATE = 22_050
        private const val TIMEOUT_S = 30L
    }
}
