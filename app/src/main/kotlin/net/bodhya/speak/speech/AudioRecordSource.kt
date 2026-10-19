package net.bodhya.speak.speech

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.IOException

/** The microphone could not be opened, for example because another app is using it. */
class MicrophoneUnavailableException(message: String) : IOException(message)

/**
 * The phone's microphone: 16 kHz mono 16-bit PCM from the VOICE_RECOGNITION
 * source. Android's compatibility rules require that source to support this
 * format with noise suppression and automatic gain off by default, so the
 * recognizer gets unprocessed audio.
 */
class AudioRecordSource(override val sampleRate: Int = 16_000) : PcmSource {
    private var record: AudioRecord? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start() {
        val minBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING)
        if (minBytes <= 0) throw MicrophoneUnavailableException("$sampleRate Hz mono recording is not supported (code $minBytes)")
        // At least half a second of buffer, so a slow moment on the phone doesn't lose audio.
        val bufferBytes = maxOf(minBytes, sampleRate / 2 * BYTES_PER_SAMPLE)
        val r = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, CHANNEL, ENCODING, bufferBytes)
        if (r.state != AudioRecord.STATE_INITIALIZED) {
            r.release()
            throw MicrophoneUnavailableException("the microphone could not be opened")
        }
        record = r
        r.startRecording()
        if (r.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            close()
            throw MicrophoneUnavailableException("the microphone did not start (another app may be using it)")
        }
    }

    override fun read(buffer: ShortArray, offset: Int, size: Int): Int =
        checkNotNull(record) { "not started" }.read(buffer, offset, size)

    override fun close() {
        record?.let {
            if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop()
            it.release()
        }
        record = null
    }

    private companion object {
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_SAMPLE = 2
    }
}
