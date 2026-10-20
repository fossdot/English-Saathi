package net.bodhya.speak.speech

import java.io.IOException
import java.io.InputStream

/** Reads 16-bit PCM mono WAV files, the format the recorder produces. */
object WavReader {

    /** The 16-bit samples of a mono PCM WAV with the given sample rate. */
    fun readPcm16Mono(input: InputStream, expectedRate: Int = 16_000): ShortArray {
        val b = input.readBytes()
        if (b.size < 12 || tag(b, 0) != "RIFF" || tag(b, 8) != "WAVE") throw IOException("not a WAVE file")

        var formatSeen = false
        var pos = 12
        while (pos + 8 <= b.size) {
            val id = tag(b, pos)
            val size = int32(b, pos + 4)
            val body = pos + 8
            if (size < 0 || size > b.size - body) throw IOException("chunk \"$id\" is cut off")
            when (id) {
                "fmt " -> {
                    if (size < 16) throw IOException("fmt chunk too short ($size bytes)")
                    val format = int16(b, body)
                    val channels = int16(b, body + 2)
                    val rate = int32(b, body + 4)
                    val bits = int16(b, body + 14)
                    if (format != 1 || channels != 1 || bits != 16) {
                        throw IOException("need 16-bit PCM mono, got format $format, $channels channels, $bits bits")
                    }
                    if (rate != expectedRate) throw IOException("need $expectedRate Hz, got $rate Hz")
                    formatSeen = true
                }
                "data" -> {
                    if (!formatSeen) throw IOException("data chunk before fmt chunk")
                    return ShortArray(size / 2) { int16(b, body + 2 * it).toShort() }
                }
            }
            pos = body + size + size % 2 // chunks are padded to an even size
        }
        throw IOException("no data chunk")
    }

    /** Samples as floats in [-1, 1], the recognizer's input. */
    fun toFloats(samples: ShortArray): FloatArray = FloatArray(samples.size) { samples[it] / 32768f }

    private fun tag(b: ByteArray, at: Int) = String(CharArray(4) { (b[at + it].toInt() and 0xff).toChar() })

    private fun int16(b: ByteArray, at: Int): Int = ((b[at].toInt() and 0xff) or (b[at + 1].toInt() shl 8)).toShort().toInt()

    private fun int32(b: ByteArray, at: Int): Int =
        (b[at].toInt() and 0xff) or ((b[at + 1].toInt() and 0xff) shl 8) or
            ((b[at + 2].toInt() and 0xff) shl 16) or (b[at + 3].toInt() shl 24)
}

/** Writes 16-bit PCM mono WAV files, the format [WavReader] reads. */
object WavWriter {

    /** [samples] in [-1, 1] at [sampleRate], as a WAV file's bytes. */
    fun pcm16Mono(samples: FloatArray, sampleRate: Int): ByteArray {
        val data = samples.size * 2
        val out = java.nio.ByteBuffer.allocate(44 + data).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray()).putInt(36 + data).put("WAVE".toByteArray())
        out.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        out.put("data".toByteArray()).putInt(data)
        for (x in samples) out.putShort((x * 32768f).toInt().coerceIn(-32768, 32767).toShort())
        return out.array()
    }
}
