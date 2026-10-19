package englishbolo.speech

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

class WavReaderTest {

    private class Wav {
        val out = ByteArrayOutputStream()
        fun tag(s: String) = apply { out.write(s.toByteArray(Charsets.US_ASCII)) }
        fun i16(v: Int) = apply { out.write(v and 0xff); out.write((v shr 8) and 0xff) }
        fun i32(v: Int) = apply { i16(v and 0xffff); i16((v shr 16) and 0xffff) }
        fun bytes(vararg b: Int) = apply { b.forEach { out.write(it) } }
        fun fmt(format: Int = 1, channels: Int = 1, rate: Int = 16_000, bits: Int = 16) = apply {
            tag("fmt ").i32(16).i16(format).i16(channels).i32(rate).i32(rate * channels * bits / 8)
                .i16(channels * bits / 8).i16(bits)
        }
        fun data(vararg samples: Int) = apply {
            tag("data").i32(samples.size * 2)
            samples.forEach { i16(it) }
        }
        fun build(): ByteArray {
            val body = out.toByteArray()
            return ByteArrayOutputStream().apply {
                write("RIFF".toByteArray())
                val size = body.size + 4
                write(byteArrayOf(size.toByte(), (size shr 8).toByte(), (size shr 16).toByte(), (size shr 24).toByte()))
                write("WAVE".toByteArray())
                write(body)
            }.toByteArray()
        }
    }

    private fun read(wav: ByteArray) = WavReader.readPcm16Mono(wav.inputStream())

    private fun assertRejected(wav: ByteArray, expected: String) {
        try {
            read(wav)
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue("${e.message}", e.message!!.contains(expected))
        }
    }

    @Test
    fun `reads the samples`() {
        assertArrayEquals(shortArrayOf(0, 1, -1, 32767, -32768), read(Wav().fmt().data(0, 1, -1, 32767, -32768).build()))
    }

    @Test
    fun `skips other chunks, including odd-sized ones with their padding byte`() {
        val wav = Wav().fmt().tag("LIST").i32(3).bytes(1, 2, 3, 0).tag("FLLR").i32(2).bytes(9, 9).data(5, -5).build()
        assertArrayEquals(shortArrayOf(5, -5), read(wav))
    }

    @Test
    fun `converts to floats in -1 to 1`() {
        val f = WavReader.toFloats(shortArrayOf(-32768, 16384, 0))
        assertEquals(-1f, f[0], 0f)
        assertEquals(0.5f, f[1], 0f)
        assertEquals(0f, f[2], 0f)
    }

    @Test
    fun `rejects what the recognizer can't take`() {
        assertRejected(Wav().fmt(channels = 2).data(1, 2).build(), "2 channels")
        assertRejected(Wav().fmt(bits = 8).data(1).build(), "8 bits")
        assertRejected(Wav().fmt(format = 3).data(1).build(), "format 3")
        assertRejected(Wav().fmt(rate = 44_100).data(1).build(), "44100 Hz")
        assertRejected(Wav().data(1).fmt().build(), "before fmt")
        assertRejected(Wav().fmt().build(), "no data")
        assertRejected("hello world, not a wav".toByteArray(), "not a WAVE")
    }

    @Test
    fun `rejects a format chunk too short to read`() {
        assertRejected(Wav().tag("fmt ").i32(2).i16(1).build(), "too short")
    }

    @Test
    fun `rejects a cut-off file`() {
        val wav = Wav().fmt().data(1, 2, 3, 4).build()
        assertRejected(wav.copyOf(wav.size - 3), "cut off")
    }
}
