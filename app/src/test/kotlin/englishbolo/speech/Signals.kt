package englishbolo.speech

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Synthetic 16 kHz test audio, built from pieces with a given RMS level in dBFS. */
object Signals {
    const val RATE = 16_000

    private fun samples(ms: Int) = RATE * ms / 1000

    private fun amplitude(dbfs: Double) = 10.0.pow(dbfs / 20)

    /** White noise: room tone or background. */
    fun noise(ms: Int, dbfs: Double, seed: Int = 1): FloatArray {
        val rng = Random(seed)
        // Uniform noise in [-a, a] has RMS a / sqrt(3).
        val a = amplitude(dbfs) * 1.7320508
        return FloatArray(samples(ms)) { ((rng.nextDouble() * 2 - 1) * a).toFloat() }
    }

    /**
     * Speech-like sound: a 200 Hz tone at [dbfs] whose loudness rises and falls
     * four times a second, dipping well below the background between
     * syllables like real speech does.
     */
    fun speech(ms: Int, dbfs: Double): FloatArray {
        val peak = amplitude(dbfs) * 2
        return FloatArray(samples(ms)) { i ->
            val t = i.toDouble() / RATE
            val envelope = abs(sin(PI * 4 * t)).pow(3)
            (peak * envelope * sin(2 * PI * 200 * t)).toFloat()
        }
    }

    /** A steady tone at [dbfs] RMS. */
    fun tone(ms: Int, dbfs: Double): FloatArray {
        val peak = amplitude(dbfs) * 1.4142135
        return FloatArray(samples(ms)) { i -> (peak * sin(2 * PI * 300 * i / RATE)).toFloat() }
    }

    fun zeros(ms: Int) = FloatArray(samples(ms))

    fun concat(vararg parts: FloatArray): FloatArray {
        val out = FloatArray(parts.sumOf { it.size })
        var at = 0
        for (p in parts) {
            p.copyInto(out, at)
            at += p.size
        }
        return out
    }

    fun toPcm(x: FloatArray) = ShortArray(x.size) { (x[it] * 32767).toInt().coerceIn(-32768, 32767).toShort() }
}
