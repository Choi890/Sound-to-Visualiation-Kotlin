package com.soundvisualization.accessibility

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

@Immutable
data class TrainingRecordingResult(
    val kind: AlertKind,
    val filePath: String,
    val featureVector: List<Float>,
    val durationMs: Long,
    val level: Float,
)

class SoundTrainingRecorder(private val context: Context) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SoundTrainingRecorder").apply {
            priority = Thread.NORM_PRIORITY
        }
    }
    private val recording = AtomicBoolean(false)

    fun isRecording(): Boolean = recording.get()

    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun record(
        kind: AlertKind,
        onComplete: (Result<TrainingRecordingResult>) -> Unit,
    ) {
        if (!hasRecordPermission()) {
            onComplete(Result.failure(IllegalStateException("RECORD_AUDIO permission is required")))
            return
        }
        if (!recording.compareAndSet(false, true)) {
            onComplete(Result.failure(IllegalStateException("Recording is already running")))
            return
        }

        executor.execute {
            var record: AudioRecord? = null
            try {
                val minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                if (minBufferSize <= 0) error("AudioRecord buffer is unavailable")

                val frameCount = (SAMPLE_RATE * RECORD_SECONDS).toInt()
                val bufferSize = max(minBufferSize, frameCount * BYTES_PER_SAMPLE)
                val format = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()

                record = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                val audioRecord = record
                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    error("AudioRecord failed to initialize")
                }

                val pcm = ShortArray(frameCount)
                val buffer = ShortArray((SAMPLE_RATE * READ_SECONDS).toInt())
                var offset = 0

                audioRecord.startRecording()
                while (offset < pcm.size && recording.get()) {
                    val read = audioRecord.read(
                        buffer,
                        0,
                        minOf(buffer.size, pcm.size - offset),
                        AudioRecord.READ_BLOCKING,
                    )
                    if (read > 0) {
                        System.arraycopy(buffer, 0, pcm, offset, read)
                        offset += read
                    }
                }
                audioRecord.stop()

                val captured = pcm.copyOf(offset)
                if (captured.size < SAMPLE_RATE / 2) {
                    error("Recording was too short")
                }

                val floats = FloatArray(captured.size) { index ->
                    captured[index] / PCM_16_MAX_VALUE
                }
                val featureVector = SoundFeatureExtractor.fingerprint(floats, SAMPLE_RATE)
                if (featureVector.isEmpty()) {
                    error("Recording did not contain enough sound")
                }

                val file = outputFile(kind)
                writeWav(file, captured)
                val result = TrainingRecordingResult(
                    kind = kind,
                    filePath = file.absolutePath,
                    featureVector = featureVector,
                    durationMs = captured.size * 1000L / SAMPLE_RATE,
                    level = rms(floats),
                )
                onComplete(Result.success(result))
            } catch (error: Throwable) {
                onComplete(Result.failure(error))
            } finally {
                recording.set(false)
                runCatching { record?.release() }
            }
        }
    }

    fun close() {
        recording.set(false)
        executor.shutdownNow()
    }

    private fun outputFile(kind: AlertKind): File {
        val dir = File(context.filesDir, TRAINING_DIR).apply { mkdirs() }
        return File(dir, "${kind.name.lowercase()}_${System.currentTimeMillis()}.wav")
    }

    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * BYTES_PER_SAMPLE
        FileOutputStream(file).use { output ->
            output.writeAscii("RIFF")
            output.writeIntLE(36 + dataSize)
            output.writeAscii("WAVE")
            output.writeAscii("fmt ")
            output.writeIntLE(16)
            output.writeShortLE(1)
            output.writeShortLE(1)
            output.writeIntLE(SAMPLE_RATE)
            output.writeIntLE(SAMPLE_RATE * BYTES_PER_SAMPLE)
            output.writeShortLE(BYTES_PER_SAMPLE)
            output.writeShortLE(16)
            output.writeAscii("data")
            output.writeIntLE(dataSize)
            samples.forEach { sample -> output.writeShortLE(sample.toInt()) }
        }
    }

    private fun FileOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun FileOutputStream.writeIntLE(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
        write(value shr 16 and 0xff)
        write(value shr 24 and 0xff)
    }

    private fun FileOutputStream.writeShortLE(value: Int) {
        write(value and 0xff)
        write(value shr 8 and 0xff)
    }

    private fun rms(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        var energy = 0.0
        samples.forEach { sample -> energy += sample * sample }
        return kotlin.math.sqrt(energy / samples.size).toFloat().coerceIn(0f, 1f)
    }

    companion object {
        private const val TRAINING_DIR = "custom_sound_samples"
        private const val SAMPLE_RATE = 16000
        private const val RECORD_SECONDS = 2.4f
        private const val READ_SECONDS = 0.12f
        private const val BYTES_PER_SAMPLE = 2
        private const val PCM_16_MAX_VALUE = 32768f
    }
}
