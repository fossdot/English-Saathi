package net.bodhya.speak.desktop.voice

import sonic.Sonic

/**
 * Changes the speed of speech without changing its pitch, with Sonic
 * (desktop/src/main/java/sonic, Apache 2.0; see NOTICE). `say -r` barely
 * slows the macOS voices, so "Slowly" (0.6×) is done here instead
 * (docs/decisions.md, "Slow speech").
 */
object TimeStretch {

    /** [samples] (mono, [sampleRate] Hz) played at [speed]: 0.6 is slower and 1.67× as long; 1 returns them unchanged. */
    fun stretch(samples: ShortArray, sampleRate: Int, speed: Float): ShortArray {
        require(speed > 0f) { "speed must be positive, got $speed" }
        if (speed == 1f || samples.isEmpty()) return samples.copyOf()
        val sonic = Sonic(sampleRate, 1)
        sonic.speed = speed
        sonic.writeShortToStream(samples, samples.size)
        sonic.flushStream()
        val out = ShortArray(sonic.samplesAvailable())
        val n = sonic.readShortFromStream(out, out.size)
        return out.copyOf(n)
    }
}
