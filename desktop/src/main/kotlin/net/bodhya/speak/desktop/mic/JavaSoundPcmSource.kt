package net.bodhya.speak.desktop.mic

import net.bodhya.speak.speech.PcmSource
import java.io.IOException
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.Mixer
import javax.sound.sampled.TargetDataLine

/** The microphone could not be opened, for example because no input is connected. */
class MicrophoneUnavailableException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * A Mac's microphone through Java Sound: 16 kHz mono 16-bit PCM, the format
 * the recorder and Whisper use (milestone b's settings). macOS records at the
 * device's own rate (48 kHz on a MacBook) and converts.
 *
 * [input] is a Java Sound mixer name (see [MicAccess.inputs]); null means the
 * Mac's current default input. A line opens on the input that is current at
 * [start], so one source per answer follows input changes.
 *
 * When macOS hasn't been allowed to give this app the microphone, or the lid
 * is closed, the line opens and delivers exact zeros; the recorder reports
 * that as `noSound`.
 */
class JavaSoundPcmSource(
    private val input: String? = null,
    override val sampleRate: Int = 16_000,
) : PcmSource {
    private var line: TargetDataLine? = null
    private var bytes = ByteArray(0)

    override fun start() {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val l = try {
            val mixer = input?.let { name ->
                AudioSystem.getMixerInfo().firstOrNull { it.name == name }
                    ?: throw MicrophoneUnavailableException("the microphone \"$name\" is not connected")
            }
            (if (mixer == null) AudioSystem.getLine(info) else AudioSystem.getMixer(mixer).getLine(info)) as TargetDataLine
        } catch (e: LineUnavailableException) {
            throw MicrophoneUnavailableException("no microphone can record $sampleRate Hz mono", e)
        } catch (e: IllegalArgumentException) {
            throw MicrophoneUnavailableException("no microphone can record $sampleRate Hz mono", e)
        }
        try {
            // Half a second of buffer, so a slow moment doesn't lose audio (Java Sound drops the oldest).
            l.open(format, sampleRate / 2 * BYTES_PER_SAMPLE)
        } catch (e: LineUnavailableException) {
            throw MicrophoneUnavailableException("the microphone could not be opened (another app may be using it)", e)
        }
        line = l
        l.start()
    }

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int {
        val l = checkNotNull(line) { "not started" }
        if (bytes.size < size * BYTES_PER_SAMPLE) bytes = ByteArray(size * BYTES_PER_SAMPLE)
        val n = l.read(bytes, 0, size * BYTES_PER_SAMPLE) / BYTES_PER_SAMPLE
        for (i in 0 until n) {
            buffer[offset + i] = ((bytes[2 * i].toInt() and 0xff) or (bytes[2 * i + 1].toInt() shl 8)).toShort()
        }
        return n
    }

    override fun close() {
        line?.let {
            it.stop()
            it.close()
        }
        line = null
    }

    private companion object {
        const val BYTES_PER_SAMPLE = 2
    }
}

/** Java Sound mixers that can record, by name ("MacBook Pro Microphone", an iPhone, AirPods). */
internal fun recordingMixers(): List<Mixer.Info> = AudioSystem.getMixerInfo().filter { info ->
    AudioSystem.getMixer(info).targetLineInfo.any { it is DataLine.Info } && !info.name.startsWith("Port ")
}
