package com.soundvisualization.accessibility

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifierResult
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.components.containers.AudioData.AudioDataFormat
import com.google.mediapipe.tasks.core.BaseOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class SoundDetectionController(
    private val context: Context,
    private val onState: (SoundRuntimeState) -> Unit,
    private val onEvent: (DetectedSoundEvent) -> Unit,
) {
    @Volatile
    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: AudioRecord? = null
    private val lifecycleLock = Any()
    private val classifierLock = Any()
    private var executor: ExecutorService? = null
    private val releaseExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            },
            "SoundDetectionRelease",
        ).apply {
            priority = Thread.MIN_PRIORITY
        }
    }
    private val classifierExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            },
            "SoundClassifierWarmup",
        ).apply {
            priority = Thread.MIN_PRIORITY
        }
    }

    @Volatile
    private var running = false
    @Volatile
    private var detectionEnabled = false
    private var channelCount = 1
    private var sensitivity = STABLE_GLOBAL_SENSITIVITY
    private var alertSettings = AlertSettings()
    @Volatile
    private var latestState = SoundRuntimeState()
    @Volatile
    private var currentLevel = 0f
    private var ambientLevel = 0f
    private var peakLevel = 0f
    private var lastStatePublishAt = 0L
    private var lastInferenceAt = 0L
    private val smoothedSpectrumBands = FloatArray(SoundFeatureExtractor.LIVE_SPECTRUM_BAND_COUNT)
    private val lastEventAt = mutableMapOf<AlertKind, Long>()
    private val temporalScores = mutableMapOf<AlertKind, Float>()
    private val temporalHits = mutableMapOf<AlertKind, Int>()
    private var pendingKind = AlertKind.Idle
    private var pendingHits = 0
    @Volatile
    private var latestInputFingerprint: List<Float> = emptyList()
    @Volatile
    private var sharedSpeechAudioSink: ((FloatArray) -> Unit)? = null
    private val inferenceInFlight = AtomicBoolean(false)
    private val classifierPreparing = AtomicBoolean(false)

    fun setSharedSpeechAudioSink(sink: ((FloatArray) -> Unit)?) {
        sharedSpeechAudioSink = sink
    }

    fun setSensitivity(value: Float) {
        sensitivity = value.stableGlobalSensitivity()
        alertSettings = alertSettings.copy(sensitivity = sensitivity)
    }

    fun updateSettings(settings: AlertSettings) {
        sensitivity = settings.sensitivity.stableGlobalSensitivity()
        alertSettings = settings.copy(sensitivity = sensitivity)
    }

    fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    fun prepare() {
        if (audioClassifier != null) return
        if (!classifierPreparing.compareAndSet(false, true)) return

        classifierExecutor.execute {
            try {
                runCatching { prepareClassifier() }
                    .onFailure { error -> Log.w(TAG, "Classifier preload failed", error) }
            } finally {
                classifierPreparing.set(false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startAudio() {
        startCapture(enableDetection = false)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        startCapture(enableDetection = true)
    }

    @SuppressLint("MissingPermission")
    private fun startCapture(enableDetection: Boolean) {
        if (!hasRecordPermission()) {
            return
        }

        val worker = Executors.newSingleThreadExecutor { runnable ->
            Thread(
                {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                    runnable.run()
                },
                "SoundDetectionWorker",
            ).apply {
                priority = Thread.NORM_PRIORITY - 1
            }
        }
        var alreadyRunning = false

        synchronized(lifecycleLock) {
            if (running) {
                worker.shutdownNow()
                if (enableDetection && !detectionEnabled) {
                    pendingKind = AlertKind.Idle
                    pendingHits = 0
                    latestInputFingerprint = emptyList()
                    resetTemporalScores()
                    lastInferenceAt = 0L
                    inferenceInFlight.set(false)
                }
                detectionEnabled = detectionEnabled || enableDetection
                alreadyRunning = true
            } else {
                running = true
                detectionEnabled = enableDetection
                executor = worker
                inferenceInFlight.set(false)
                currentLevel = 0f
                ambientLevel = 0f
                peakLevel = 0f
                resetTemporalScores()
                resetSpectrum()
            }
        }

        if (alreadyRunning) {
            publish(
                latestState.copy(
                    isAudioActive = true,
                    isListening = detectionEnabled,
                ),
            )
            if (enableDetection) {
                prepareClassifierAsync()
            }
            return
        }

        worker.execute {
            var createdRecord: AudioRecord? = null
            try {
                val configuredRecord = createAudioRecord(preferStereo = false, preferUnprocessed = true)
                    ?: createAudioRecord(preferStereo = false, preferUnprocessed = false)
                createdRecord = configuredRecord?.record
                val record = configuredRecord?.record ?: error("Microphone input is unavailable on this device")

                if (!isCurrentWorker(worker)) {
                    releaseAudioRecord(record)
                    return@execute
                }

                synchronized(lifecycleLock) {
                    audioRecord = record
                    channelCount = if (record.channelCount >= 2) 2 else 1
                }

                record.startRecording()
                publish(
                    latestState.copy(
                        isAudioActive = true,
                        isListening = detectionEnabled,
                        speechRecognitionAvailable = latestState.speechRecognitionAvailable,
                    ),
                )

                if (enableDetection) {
                    prepareClassifierAsync()
                }
                captureLoop(record, worker, configuredRecord.sampleRate)
            } catch (error: Throwable) {
                if (isCurrentWorker(worker)) {
                    Log.e(TAG, "Failed to start audio capture", error)
                    markStopped(worker)
                    publish(latestState.copy(isAudioActive = false, isListening = false))
                }
                releaseAudioRecord(createdRecord)
            }
        }
    }

    fun stopDetection() {
        val shouldPublish: Boolean
        val audioActive: Boolean

        synchronized(lifecycleLock) {
            shouldPublish = detectionEnabled || latestState.isListening
            detectionEnabled = false
            inferenceInFlight.set(false)
            pendingKind = AlertKind.Idle
            pendingHits = 0
            latestInputFingerprint = emptyList()
            resetTemporalScores()
            sharedSpeechAudioSink = null
            audioActive = running
        }

        if (shouldPublish) {
            publish(
                latestState.copy(
                    isAudioActive = audioActive,
                    isListening = false,
                ),
            )
        }
    }

    fun stop(
        publishState: Boolean = true,
        asyncRelease: Boolean = true,
    ) {
        val recordToRelease: AudioRecord?
        val workerToStop: ExecutorService?
        val shouldPublish: Boolean

        synchronized(lifecycleLock) {
            shouldPublish = running ||
                audioRecord != null ||
                executor != null ||
                latestState.isAudioActive ||
                latestState.isListening
            running = false
            detectionEnabled = false
            sharedSpeechAudioSink = null
            resetTemporalScores()
            recordToRelease = audioRecord
            audioRecord = null
            workerToStop = executor
            executor = null
            inferenceInFlight.set(false)
            currentLevel = 0f
        }

        val releaseWork = {
            workerToStop?.shutdownNow()
            releaseAudioRecord(recordToRelease)
        }
        if (asyncRelease) {
            releaseExecutor.execute {
                releaseWork()
            }
        } else {
            releaseWork()
        }

        if (publishState && shouldPublish) {
            publish(
                latestState.copy(
                    isAudioActive = false,
                    isListening = false,
                    level = 0f,
                    peakLevel = peakLevel,
                    ambientLevel = ambientLevel,
                    spectrumBands = emptyList(),
                    dominantFrequencyHz = 0f,
                ),
            )
        }
    }

    fun close() {
        stop(asyncRelease = false)
        val classifier = synchronized(classifierLock) {
            audioClassifier.also {
                audioClassifier = null
            }
        }
        classifier?.close()
        classifierExecutor.shutdownNow()
        releaseExecutor.shutdownNow()
    }

    private fun isCurrentWorker(worker: ExecutorService): Boolean =
        synchronized(lifecycleLock) {
            running && executor === worker
        }

    private fun markStopped(worker: ExecutorService) {
        synchronized(lifecycleLock) {
            if (executor === worker) {
                running = false
                detectionEnabled = false
                audioRecord = null
                executor = null
                inferenceInFlight.set(false)
                resetTemporalScores()
            }
        }
        worker.shutdownNow()
    }

    private fun releaseAudioRecord(record: AudioRecord?) {
        if (record == null) return

        runCatching {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        }
        runCatching { record.release() }
    }

    fun setSpeechRecognitionAvailable(isAvailable: Boolean) {
        if (latestState.speechRecognitionAvailable == isAvailable) return

        publish(latestState.copy(speechRecognitionAvailable = isAvailable))
    }

    private fun prepareClassifierAsync() {
        if (audioClassifier != null) return
        if (!classifierPreparing.compareAndSet(false, true)) return

        classifierExecutor.execute {
            try {
                Thread.sleep(CLASSIFIER_WARMUP_DELAY_MS)
                if (!running || Thread.currentThread().isInterrupted) return@execute
                runCatching { prepareClassifier() }
                    .onFailure { error -> Log.w(TAG, "Classifier warmup failed", error) }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } finally {
                classifierPreparing.set(false)
            }
        }
    }

    private fun prepareClassifier() {
        if (audioClassifier != null) return

        synchronized(classifierLock) {
            if (audioClassifier != null) return

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET)
                .build()
            val options = AudioClassifier.AudioClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.AUDIO_STREAM)
                .setMaxResults(CLASSIFIER_MAX_RESULTS)
                .setScoreThreshold(0.001f)
                .setResultListener(::handleResult)
                .setErrorListener { error ->
                    inferenceInFlight.set(false)
                    Log.w(TAG, error.message ?: "classifier error")
                }
                .build()

            audioClassifier = AudioClassifier.createFromOptions(context, options)
        }
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(preferStereo: Boolean, preferUnprocessed: Boolean): ConfiguredAudioRecord? {
        for (sampleRate in CAPTURE_SAMPLE_RATE_CANDIDATES) {
            val record = createAudioRecord(
                preferStereo = preferStereo,
                preferUnprocessed = preferUnprocessed,
                sampleRate = sampleRate,
            ) ?: continue
            return ConfiguredAudioRecord(record, sampleRate)
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(
        preferStereo: Boolean,
        preferUnprocessed: Boolean,
        sampleRate: Int,
    ): AudioRecord? {
        val channelMask = if (preferStereo) {
            AudioFormat.CHANNEL_IN_STEREO
        } else {
            AudioFormat.CHANNEL_IN_MONO
        }
        val audioSource = if (preferUnprocessed) {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.MIC
        }
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelMask,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        if (minBufferSize <= 0) return null

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(channelMask)
            .build()

        val bufferSize = max(
            minBufferSize,
            (sampleRate * INPUT_SECONDS * BYTES_PER_PCM_16_SAMPLE * if (preferStereo) 4 else 2).toInt(),
        )

        val record = runCatching {
            AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSize)
                .build()
        }.getOrNull() ?: return null

        return if (record.state == AudioRecord.STATE_INITIALIZED) {
            record
        } else {
            record.release()
            null
        }
    }

    private fun captureLoop(record: AudioRecord, worker: ExecutorService, captureSampleRate: Int) {
        val framesPerInference = (captureSampleRate * INPUT_SECONDS).toInt()
        val framesPerRead = (captureSampleRate * READ_SECONDS).toInt()
        val inputBuffer = ShortArray(framesPerRead * channelCount)
        val inferenceBuffer = ShortArray(framesPerInference * channelCount)
        val visualMonoBuffer = FloatArray(framesPerRead)
        var inferenceFrames = 0

        while (isCurrentWorker(worker) && !Thread.currentThread().isInterrupted) {
            val sampleCount = record.read(
                inputBuffer,
                0,
                inputBuffer.size,
                AudioRecord.READ_BLOCKING,
            )

            if (sampleCount <= 0) continue

            val frameCount = sampleCount / channelCount
            if (frameCount <= 0) continue

            var monoEnergy = 0.0
            inferenceFrames = appendToInferenceBuffer(
                target = inferenceBuffer,
                currentFrames = inferenceFrames,
                source = inputBuffer,
                sourceFrames = frameCount,
                channels = channelCount,
            )

            for (frame in 0 until frameCount) {
                val left = inputBuffer[frame * channelCount] / PCM_16_MAX_VALUE
                val right = if (channelCount >= 2) {
                    inputBuffer[frame * channelCount + 1] / PCM_16_MAX_VALUE
                } else {
                    left
                }
                val mixed = (left + right) * 0.5f
                visualMonoBuffer[frame] = mixed

                monoEnergy += mixed * mixed
            }

            val monoRms = sqrt(monoEnergy / frameCount)
            val rawLevel = perceptualLevel(monoRms * LEVEL_INPUT_GAIN)
            val level = smoothLevel(rawLevel)
            val frequencyResponse = smoothFrequencyResponse(
                SoundFeatureExtractor.liveSpectrum(
                    samples = visualMonoBuffer,
                    sampleCount = frameCount,
                    sampleRate = captureSampleRate,
                    level = level,
                ),
            )
            currentLevel = level
            maybePublishAudioState(level, frequencyResponse)
            val now = SystemClock.uptimeMillis()
            if (!detectionEnabled) {
                inferenceFrames = 0
                continue
            }

            sharedSpeechAudioSink?.invoke(
                buildSharedSpeechBuffer(
                    samples = inputBuffer,
                    frameCount = frameCount,
                    channels = channelCount,
                    sourceSampleRate = captureSampleRate,
                    targetSampleRate = MODEL_SAMPLE_RATE,
                ),
            )

            if (inferenceFrames < framesPerInference) continue
            if (!shouldClassify(level, now)) continue
            val classifier = audioClassifier ?: continue
            if (!inferenceInFlight.compareAndSet(false, true)) continue

            lastInferenceAt = now
            val mono = buildMonoBuffer(
                samples = inferenceBuffer,
                frameCount = framesPerInference,
                channels = channelCount,
                sourceSampleRate = captureSampleRate,
                targetSampleRate = MODEL_SAMPLE_RATE,
            )
            latestInputFingerprint = SoundFeatureExtractor.fingerprint(mono, MODEL_SAMPLE_RATE)
            runCatching {
                AudioData.create(
                    AudioDataFormat.builder()
                        .setNumOfChannels(1)
                        .setSampleRate(MODEL_SAMPLE_RATE.toFloat())
                        .build(),
                    mono.size,
                ).also { audioData ->
                    audioData.load(mono)
                    classifier.classifyAsync(audioData, now)
                }
            }.onFailure { error ->
                inferenceInFlight.set(false)
                Log.w(TAG, "Failed to submit audio frame", error)
            }
        }
    }

    private fun appendToInferenceBuffer(
        target: ShortArray,
        currentFrames: Int,
        source: ShortArray,
        sourceFrames: Int,
        channels: Int,
    ): Int {
        val targetFrames = target.size / channels
        val sourceSamples = sourceFrames * channels
        if (sourceFrames >= targetFrames) {
            val sourceOffset = (sourceFrames - targetFrames) * channels
            System.arraycopy(source, sourceOffset, target, 0, target.size)
            return targetFrames
        }

        if (currentFrames + sourceFrames > targetFrames) {
            val framesToKeep = targetFrames - sourceFrames
            System.arraycopy(
                target,
                (currentFrames - framesToKeep) * channels,
                target,
                0,
                framesToKeep * channels,
            )
            System.arraycopy(source, 0, target, framesToKeep * channels, sourceSamples)
            return targetFrames
        }

        System.arraycopy(source, 0, target, currentFrames * channels, sourceSamples)
        return currentFrames + sourceFrames
    }

    private fun buildMonoBuffer(
        samples: ShortArray,
        frameCount: Int,
        channels: Int,
        sourceSampleRate: Int,
        targetSampleRate: Int,
    ): FloatArray {
        val mono = resampleMonoBuffer(
            samples = samples,
            frameCount = frameCount,
            channels = channels,
            sourceSampleRate = sourceSampleRate,
            targetSampleRate = targetSampleRate,
            gain = 1f,
        )
        var energy = 0.0
        for (sample in mono) {
            energy += sample * sample
        }

        val gain = adaptiveInputGain(sqrt(energy / mono.size.coerceAtLeast(1)))
        for (index in mono.indices) {
            mono[index] = (mono[index] * gain).coerceIn(-0.98f, 0.98f)
        }
        return mono
    }

    private fun buildSharedSpeechBuffer(
        samples: ShortArray,
        frameCount: Int,
        channels: Int,
        sourceSampleRate: Int,
        targetSampleRate: Int,
    ): FloatArray = resampleMonoBuffer(
        samples = samples,
        frameCount = frameCount,
        channels = channels,
        sourceSampleRate = sourceSampleRate,
        targetSampleRate = targetSampleRate,
        gain = SHARED_SPEECH_GAIN,
    )

    private fun resampleMonoBuffer(
        samples: ShortArray,
        frameCount: Int,
        channels: Int,
        sourceSampleRate: Int,
        targetSampleRate: Int,
        gain: Float,
    ): FloatArray {
        if (frameCount <= 0 || sourceSampleRate <= 0 || targetSampleRate <= 0) return FloatArray(0)

        val targetFrames = max(1, (frameCount.toLong() * targetSampleRate / sourceSampleRate).toInt())
        val output = FloatArray(targetFrames)
        for (targetFrame in 0 until targetFrames) {
            val sourcePosition = targetFrame * sourceSampleRate.toDouble() / targetSampleRate
            val leftFrame = sourcePosition.toInt().coerceIn(0, frameCount - 1)
            val rightFrame = (leftFrame + 1).coerceAtMost(frameCount - 1)
            val fraction = (sourcePosition - leftFrame).toFloat().coerceIn(0f, 1f)
            val leftSample = monoSample(samples, leftFrame, channels)
            val rightSample = monoSample(samples, rightFrame, channels)
            output[targetFrame] = ((leftSample + (rightSample - leftSample) * fraction) * gain)
                .coerceIn(-0.98f, 0.98f)
        }
        return output
    }

    private fun monoSample(samples: ShortArray, frame: Int, channels: Int): Float {
        val offset = frame * channels
        val left = samples.getOrElse(offset) { 0 } / PCM_16_MAX_VALUE
        val right = if (channels >= 2) {
            samples.getOrElse(offset + 1) { samples.getOrElse(offset) { 0 } } / PCM_16_MAX_VALUE
        } else {
            left
        }
        return (left + right) * 0.5f
    }

    private fun adaptiveInputGain(rms: Double): Float {
        val autoGain = (TARGET_INFERENCE_RMS / (rms + INPUT_GAIN_EPSILON))
            .coerceIn(1.0, MAX_AUTOMATIC_INPUT_GAIN)
        return (SOFTWARE_INPUT_GAIN * autoGain).coerceAtMost(MAX_TOTAL_INPUT_GAIN).toFloat()
    }

    private fun handleResult(result: AudioClassifierResult) {
        inferenceInFlight.set(false)
        if (!running || !detectionEnabled) return

        val classifications = result.classificationResults()
            .firstOrNull()
            ?.classifications()
            .orEmpty()
        val scoredLabels = ArrayList<ScoredLabel>(CLASSIFIER_MAX_RESULTS)
        for (classification in classifications) {
            for (category in classification.categories()) {
                scoredLabels.add(
                    ScoredLabel(
                        label = category.normalizedLabel(),
                        score = category.score(),
                    ),
                )
            }
        }

        var bestMatch: DetectionMatch? = null
        var secondRatio = 0f
        val frameMatches = ArrayList<DetectionMatch>(DETECTION_CANDIDATES.size)

        fun consider(match: DetectionMatch) {
            frameMatches.add(match)
            val currentBest = bestMatch
            if (currentBest == null || match.ratio > currentBest.ratio) {
                secondRatio = currentBest?.ratio ?: 0f
                bestMatch = match
            } else if (match.ratio > secondRatio) {
                secondRatio = match.ratio
            }
        }

        DETECTION_CANDIDATES.forEach { candidate ->
            if (!alertSettings.categorySetting(candidate.kind).enabled) return@forEach

            val threshold = threshold(candidate.baseThreshold, candidate.kind)
            val score = candidate.weightedScore(
                categories = scoredLabels,
                threshold = threshold,
                signalBoost = signalHeuristicBoost(candidate.kind),
            ) ?: return@forEach
            val strongScore = threshold(candidate.strongThreshold, candidate.kind)
            val levelOk = currentLevel >= candidate.minLevel || score >= strongScore * 1.05f
            if (score < threshold || !levelOk) return@forEach

            val adjusted = adjustedConfidence(candidate.kind, score)
            consider(
                DetectionMatch(
                    candidate = candidate,
                    confidence = adjusted,
                    ratio = adjusted / threshold,
                    strong = adjusted >= strongScore,
                    source = "YAMNet",
                ),
            )
        }

        considerPersonalizedMatches { match -> consider(match) }

        val temporalBest = updateTemporalScores(frameMatches)
        if (temporalBest != null) {
            consider(temporalBest)
        }

        val best = bestMatch?.takeIf { match ->
            match.strong || secondRatio <= 0f || match.ratio >= secondRatio * CANDIDATE_MARGIN_RATIO
        }

        if (best != null) {
            maybeEmitEvent(best.candidate.kind, best.confidence, best.source)
        } else {
            registerCandidate(AlertKind.Idle)
        }
    }

    private fun perceptualLevel(rms: Double): Float {
        val normalized = rms.coerceAtLeast(0.0)
        val quietLift = sqrt(normalized) * PERCEPTUAL_LEVEL_GAIN
        val linearLift = normalized * LINEAR_LEVEL_GAIN
        return max(quietLift, linearLift).toFloat().coerceIn(0f, 1f)
    }

    private fun smoothLevel(rawLevel: Float): Float {
        ambientLevel = if (ambientLevel == 0f) {
            rawLevel.coerceAtMost(INITIAL_AMBIENT_CAP)
        } else {
            val alpha = if (rawLevel < ambientLevel) 0.16f else 0.02f
            ambientLevel * (1f - alpha) + rawLevel * alpha
        }
        peakLevel = max(peakLevel * 0.99f, rawLevel)
        return (latestState.level * 0.34f + rawLevel * 0.66f).coerceIn(0f, 1f)
    }

    private fun smoothFrequencyResponse(
        response: SoundFeatureExtractor.FrequencyResponse,
    ): SoundFeatureExtractor.FrequencyResponse {
        if (response.bands.isEmpty()) return response

        val output = ArrayList<Float>(response.bands.size)
        for (index in response.bands.indices) {
            val next = response.bands[index].coerceIn(0f, 1f)
            val previous = smoothedSpectrumBands.getOrElse(index) { 0f }
            val alpha = if (next > previous) 0.98f else 0.58f
            val smoothed = previous + (next - previous) * alpha
            if (index < smoothedSpectrumBands.size) {
                smoothedSpectrumBands[index] = smoothed
            }
            output.add(smoothed.coerceIn(0f, 1f))
        }
        return response.copy(bands = output)
    }

    private fun resetSpectrum() {
        for (index in smoothedSpectrumBands.indices) {
            smoothedSpectrumBands[index] = 0f
        }
    }

    private fun resetTemporalScores() {
        temporalScores.clear()
        temporalHits.clear()
    }

    private fun shouldClassify(level: Float, now: Long): Boolean {
        val minInterval = if (level > LOUD_LEVEL) 280L else 430L
        if (now - lastInferenceAt < minInterval) return false
        return level >= max(MIN_CLASSIFY_LEVEL, ambientLevel + AMBIENT_EVENT_MARGIN)
    }

    private fun maybePublishAudioState(
        level: Float,
        frequencyResponse: SoundFeatureExtractor.FrequencyResponse,
    ) {
        val now = SystemClock.uptimeMillis()
        val levelChanged = abs(level - latestState.level) >= STATE_LEVEL_CHANGE_EPSILON
        val spectrumChanged = hasSpectrumChanged(
            current = frequencyResponse.bands,
            previous = latestState.spectrumBands,
        )
        if (now - lastStatePublishAt < STATE_PUBLISH_INTERVAL_MS) {
            return
        }
        if (!levelChanged && !spectrumChanged) {
            return
        }

        lastStatePublishAt = now
        publish(
            latestState.copy(
                level = level,
                isAudioActive = true,
                isListening = detectionEnabled,
                peakLevel = peakLevel.coerceIn(0f, 1f),
                ambientLevel = ambientLevel.coerceIn(0f, 1f),
                spectrumBands = frequencyResponse.bands,
                dominantFrequencyHz = frequencyResponse.dominantFrequencyHz,
                direction = SoundDirection.Unknown,
            ),
        )
    }

    private fun hasSpectrumChanged(current: List<Float>, previous: List<Float>): Boolean {
        if (current.size != previous.size) return current.isNotEmpty()

        for (index in current.indices) {
            if (abs(current[index] - previous[index]) >= STATE_SPECTRUM_CHANGE_EPSILON) {
                return true
            }
        }
        return false
    }

    private fun registerCandidate(kind: AlertKind) {
        if (pendingKind == kind) {
            pendingHits += 1
        } else {
            pendingKind = kind
            pendingHits = if (kind == AlertKind.Idle) 0 else 1
        }
    }

    private fun maybeEmitEvent(kind: AlertKind, confidence: Float, source: String) {
        registerCandidate(kind)

        val now = SystemClock.uptimeMillis()
        val previousEventAt = lastEventAt[kind] ?: 0L
        if (now - previousEventAt < EVENT_COOLDOWN_MS) return

        val candidate = DETECTION_CANDIDATES.firstOrNull { it.kind == kind }
        val strongThreshold = candidate?.strongThreshold ?: DEFAULT_STRONG_THRESHOLD

        val strongHit = confidence >= threshold(strongThreshold, kind)
        if (!strongHit && pendingHits < REQUIRED_CONSECUTIVE_HITS) return

        lastEventAt[kind] = now
        pendingHits = 0
        onEvent(detectedEvent(kind, confidence, SoundDirection.Unknown, source))
    }

    private fun updateTemporalScores(frameMatches: List<DetectionMatch>): DetectionMatch? {
        if (frameMatches.isEmpty()) {
            decayTemporalScores(emptySet())
            return null
        }

        val activeKinds = frameMatches.mapTo(mutableSetOf()) { it.candidate.kind }
        decayTemporalScores(activeKinds)

        for (match in frameMatches) {
            val kind = match.candidate.kind
            val previous = temporalScores[kind] ?: 0f
            val normalizedRatio = (match.ratio / TEMPORAL_RATIO_NORMALIZER).coerceIn(0f, 1f)
            val evidence = (
                match.confidence * TEMPORAL_CONFIDENCE_WEIGHT +
                    normalizedRatio * TEMPORAL_RATIO_WEIGHT +
                    if (match.strong) TEMPORAL_STRONG_BONUS else 0f
                ).coerceIn(0f, 1f)
            val next = (previous * TEMPORAL_ACTIVE_DECAY + evidence * TEMPORAL_ATTACK).coerceIn(0f, 1f)
            temporalScores[kind] = next
            temporalHits[kind] = ((temporalHits[kind] ?: 0) + if (evidence >= TEMPORAL_HIT_EVIDENCE) 1 else 0)
                .coerceAtMost(TEMPORAL_MAX_HITS)
        }

        val bestKind = temporalScores.maxByOrNull { it.value }?.key ?: return null
        val bestScore = temporalScores[bestKind] ?: return null
        val hits = temporalHits[bestKind] ?: 0
        val candidate = DETECTION_CANDIDATES.firstOrNull { it.kind == bestKind } ?: return null
        val temporalThreshold = when (bestKind.defaultSeverity()) {
            AlertSeverity.Emergency -> TEMPORAL_EMERGENCY_THRESHOLD
            AlertSeverity.Caution -> TEMPORAL_CAUTION_THRESHOLD
            AlertSeverity.Info -> TEMPORAL_INFO_THRESHOLD
        }
        if (bestScore < temporalThreshold || hits < TEMPORAL_MIN_HITS) return null

        return DetectionMatch(
            candidate = candidate,
            confidence = bestScore.coerceIn(TEMPORAL_MIN_CONFIDENCE, TEMPORAL_MAX_CONFIDENCE),
            ratio = bestScore / threshold(candidate.baseThreshold, candidate.kind),
            strong = bestScore >= temporalThreshold + TEMPORAL_STRONG_MARGIN,
            source = "YAMNet+temporal",
        )
    }

    private fun decayTemporalScores(activeKinds: Set<AlertKind>) {
        if (temporalScores.isEmpty()) return

        val iterator = temporalScores.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key in activeKinds) continue

            val decayed = entry.value * TEMPORAL_IDLE_DECAY
            if (decayed < TEMPORAL_DROP_FLOOR) {
                temporalHits.remove(entry.key)
                iterator.remove()
            } else {
                entry.setValue(decayed)
                temporalHits[entry.key] = ((temporalHits[entry.key] ?: 0) - 1).coerceAtLeast(0)
            }
        }
    }

    private fun adjustedConfidence(kind: AlertKind, score: Float): Float {
        val profile = alertSettings.customProfile(kind)
        return (score + profile.confidenceBoost).coerceIn(0f, 1f)
    }

    private inline fun considerPersonalizedMatches(consider: (DetectionMatch) -> Unit) {
        val fingerprint = latestInputFingerprint
        if (fingerprint.isEmpty()) return

        for (candidate in DETECTION_CANDIDATES) {
            if (!alertSettings.categorySetting(candidate.kind).enabled) continue

            val profile = alertSettings.customProfile(candidate.kind)
            val templateVectors = profile.featureVectors.ifEmpty {
                if (profile.featureVector.isNotEmpty()) listOf(profile.featureVector) else emptyList()
            }
            if (profile.sampleCount <= 0 || templateVectors.isEmpty()) continue

            val similarity = templateVectors.maxOfOrNull { template ->
                SoundFeatureExtractor.similarity(fingerprint, template)
            } ?: 0f
            val required = (CUSTOM_MATCH_THRESHOLD - profile.sampleCount * CUSTOM_SAMPLE_THRESHOLD_STEP)
                .coerceAtLeast(CUSTOM_MIN_MATCH_THRESHOLD)
            val levelOk = currentLevel >= candidate.minLevel * CUSTOM_MIN_LEVEL_MULTIPLIER
            if (similarity < required || !levelOk) continue

            val confidence = (
                CUSTOM_BASE_CONFIDENCE +
                    (similarity - required) * CUSTOM_CONFIDENCE_GAIN +
                    profile.confidenceBoost * CUSTOM_PROFILE_BOOST_WEIGHT
                ).coerceIn(CUSTOM_MIN_CONFIDENCE, CUSTOM_MAX_CONFIDENCE)

            consider(
                DetectionMatch(
                    candidate = candidate,
                    confidence = confidence,
                    ratio = similarity / required,
                    strong = similarity >= required + CUSTOM_STRONG_MARGIN,
                    source = "custom-template",
                ),
            )
        }
    }

    private fun threshold(base: Float, kind: AlertKind): Float {
        val categorySensitivity = alertSettings.categorySetting(kind).sensitivity
        val profile = alertSettings.customProfile(kind)
        val trainedBoost = profile.confidenceBoost *
            (profile.sampleCount / MIN_CUSTOM_SAMPLES.toFloat()).coerceIn(0f, 1f)
        val effectiveSensitivity = (
            sensitivity * 0.52f +
                categorySensitivity * 0.48f +
                trainedBoost
            ).coerceIn(DETECTION_SENSITIVITY_FLOOR, 0.98f)
        val multiplier = 1.18f - effectiveSensitivity * 0.55f
        return (base * multiplier).coerceAtLeast(MIN_CANDIDATE_THRESHOLD)
    }

    private fun DetectionCandidate.weightedScore(
        categories: List<ScoredLabel>,
        threshold: Float,
        signalBoost: Float,
    ): Float? {
        var primary0 = 0f
        var primary1 = 0f
        var primary2 = 0f
        var support0 = 0f
        var support1 = 0f
        var support2 = 0f

        for (category in categories) {
            when {
                category.label.containsAny(primaryTerms) -> {
                    val value = category.score * PRIMARY_TERM_WEIGHT
                    when {
                        value > primary0 -> {
                            primary2 = primary1
                            primary1 = primary0
                            primary0 = value
                        }
                        value > primary1 -> {
                            primary2 = primary1
                            primary1 = value
                        }
                        value > primary2 -> {
                            primary2 = value
                        }
                    }
                }
                category.label.containsAny(supportingTerms) -> {
                    val value = category.score * SUPPORTING_TERM_WEIGHT
                    when {
                        value > support0 -> {
                            support2 = support1
                            support1 = support0
                            support0 = value
                        }
                        value > support1 -> {
                            support2 = support1
                            support1 = value
                        }
                        value > support2 -> {
                            support2 = value
                        }
                    }
                }
            }
        }

        val primaryPeak = primary0
        val gate = threshold * PRIMARY_GATE_RATIO
        if (primaryPeak < gate && signalBoost < gate * SIGNAL_GATE_RATIO) return null

        val primaryEvidence = (primary1 + primary2) * PRIMARY_ENSEMBLE_WEIGHT
        val supportingEvidence = (support0 + support1 + support2) * SCORE_ENSEMBLE_WEIGHT

        return (primaryPeak + primaryEvidence + supportingEvidence + signalBoost).coerceIn(0f, 1f)
    }

    private fun signalHeuristicBoost(kind: AlertKind): Float {
        val fingerprint = latestInputFingerprint
        val spectrum = latestState.spectrumBands
        if (fingerprint.size < SIGNAL_FEATURE_COUNT || spectrum.isEmpty()) return 0f

        val rms = fingerprint[0]
        val peak = fingerprint[1]
        val zcr = fingerprint[2]
        val crest = fingerprint[3]
        val transient = fingerprint[4]
        val onsetRate = fingerprint[5]
        val centroid = fingerprint[6]
        val low = spectrum.bandAverage(0, 2)
        val lowMid = spectrum.bandAverage(2, 4)
        val mid = spectrum.bandAverage(4, 6)
        val high = spectrum.bandAverage(6, 8)
        val air = spectrum.bandAverage(7, 8)
        val level = currentLevel.coerceIn(0f, 1f)
        if (level < SIGNAL_MIN_LEVEL) return 0f

        fun shaped(value: Float, floor: Float, ceiling: Float): Float =
            ((value - floor) / (ceiling - floor).coerceAtLeast(0.001f)).coerceIn(0f, 1f)

        val boost = when (kind) {
            AlertKind.Knock -> {
                if (level < SIGNAL_TRANSIENT_MIN_LEVEL) return 0f
                val impact = shaped(transient, 0.045f, 0.18f) * 0.34f +
                    shaped(crest, 0.09f, 0.26f) * 0.28f +
                    shaped(onsetRate, 0.02f, 0.16f) * 0.20f +
                    shaped(peak, 0.04f, 0.20f) * 0.18f
                impact * 0.070f
            }
            AlertKind.Doorbell,
            AlertKind.PhoneRing,
            AlertKind.ApplianceBeep -> {
                val tonal = shaped(mid + high, 0.035f, 0.28f) * 0.34f +
                    shaped(zcr, 0.035f, 0.22f) * 0.20f +
                    shaped(onsetRate, 0.015f, 0.13f) * 0.22f +
                    shaped(crest, 0.06f, 0.20f) * 0.24f
                tonal * when (kind) {
                    AlertKind.Doorbell -> 0.062f
                    AlertKind.PhoneRing -> 0.058f
                    else -> 0.066f
                }
            }
            AlertKind.Siren,
            AlertKind.FireAlarm -> {
                val warningTone = shaped(mid + high + air, 0.045f, 0.38f) * 0.42f +
                    shaped(level, 0.018f, 0.16f) * 0.28f +
                    shaped(zcr, 0.04f, 0.26f) * 0.16f +
                    shaped(rms, 0.012f, 0.10f) * 0.14f
                warningTone * if (kind == AlertKind.FireAlarm) 0.064f else 0.070f
            }
            AlertKind.BabyCry -> {
                val voiceLike = shaped(mid + high, 0.04f, 0.34f) * 0.32f +
                    shaped(centroid, 0.20f, 0.72f) * 0.22f +
                    shaped(zcr, 0.05f, 0.30f) * 0.18f +
                    shaped(level, 0.018f, 0.15f) * 0.28f
                voiceLike * 0.052f
            }
            AlertKind.WaterRunning -> {
                val continuousNoise = shaped(lowMid + mid + high, 0.055f, 0.40f) * 0.38f +
                    shaped(zcr, 0.06f, 0.34f) * 0.24f +
                    (1f - shaped(transient, 0.08f, 0.22f)) * 0.16f +
                    shaped(level, 0.012f, 0.11f) * 0.22f
                continuousNoise * 0.060f
            }
            AlertKind.NameCall,
            AlertKind.Idle -> 0f
        }

        return boost.coerceIn(0f, SIGNAL_MAX_BOOST)
    }

    private fun List<Float>.bandAverage(start: Int, endInclusive: Int): Float {
        if (isEmpty()) return 0f

        val startIndex = start.coerceIn(indices)
        val endIndex = endInclusive.coerceIn(indices)
        if (endIndex < startIndex) return 0f

        var total = 0f
        var count = 0
        for (index in startIndex..endIndex) {
            total += this[index].coerceIn(0f, 1f)
            count += 1
        }
        return if (count == 0) 0f else total / count
    }

    private fun String.containsAny(terms: List<String>): Boolean {
        for (term in terms) {
            if (contains(term)) return true
        }
        return false
    }

    private fun com.google.mediapipe.tasks.components.containers.Category.normalizedLabel(): String {
        val label = "${categoryName()} ${displayName()}".lowercase()
        return label.replace('_', ' ')
    }

    private fun publish(next: SoundRuntimeState) {
        if (next == latestState) return

        latestState = next
        onState(next)
    }

    private data class ConfiguredAudioRecord(
        val record: AudioRecord,
        val sampleRate: Int,
    )

    companion object {
        private const val TAG = "SoundDetection"
        private const val MODEL_ASSET = "yamnet.tflite"
        private const val CLASSIFIER_WARMUP_DELAY_MS = 2200L
        private val CAPTURE_SAMPLE_RATE_CANDIDATES = intArrayOf(48_000, 44_100, 32_000, 16_000)
        private const val CAPTURE_SAMPLE_RATE = 48_000
        private const val MODEL_SAMPLE_RATE = 16_000
        private const val BYTES_PER_PCM_16_SAMPLE = 2
        private const val PCM_16_MAX_VALUE = 32768f
        private const val INPUT_SECONDS = 0.975f
        private const val READ_SECONDS = 0.032f
        private const val STATE_PUBLISH_INTERVAL_MS = 24L
        private const val STATE_LEVEL_CHANGE_EPSILON = 0.003f
        private const val STATE_SPECTRUM_CHANGE_EPSILON = 0.002f
        private const val EVENT_COOLDOWN_MS = 2100L
        private const val REQUIRED_CONSECUTIVE_HITS = 2
        private const val MIN_CUSTOM_SAMPLES = 3
        private const val MIN_CLASSIFY_LEVEL = 0.009f
        private const val INITIAL_AMBIENT_CAP = 0.08f
        private const val LOUD_LEVEL = 0.13f
        private const val AMBIENT_EVENT_MARGIN = 0.007f
        private const val DETECTION_SENSITIVITY_FLOOR = 0.70f
        private const val MIN_CANDIDATE_THRESHOLD = 0.025f
        private const val DEFAULT_STRONG_THRESHOLD = 0.20f
        private const val LEVEL_INPUT_GAIN = 1.45
        private const val SOFTWARE_INPUT_GAIN = 1.28
        private const val TARGET_INFERENCE_RMS = 0.075
        private const val MAX_AUTOMATIC_INPUT_GAIN = 2.4
        private const val MAX_TOTAL_INPUT_GAIN = 3.0
        private const val INPUT_GAIN_EPSILON = 0.000001
        private const val SHARED_SPEECH_GAIN = 1.18f
        private const val CLASSIFIER_MAX_RESULTS = 128
        private const val PERCEPTUAL_LEVEL_GAIN = 1.55
        private const val LINEAR_LEVEL_GAIN = 7.0
        private const val PRIMARY_TERM_WEIGHT = 1.0f
        private const val SUPPORTING_TERM_WEIGHT = 0.34f
        private const val PRIMARY_GATE_RATIO = 0.36f
        private const val SIGNAL_GATE_RATIO = 0.88f
        private const val CANDIDATE_MARGIN_RATIO = 1.08f
        private const val PRIMARY_ENSEMBLE_WEIGHT = 0.24f
        private const val SCORE_ENSEMBLE_WEIGHT = 0.22f
        private const val SIGNAL_FEATURE_COUNT = 7
        private const val SIGNAL_MAX_BOOST = 0.075f
        private const val SIGNAL_MIN_LEVEL = 0.010f
        private const val SIGNAL_TRANSIENT_MIN_LEVEL = 0.020f
        private const val TEMPORAL_ACTIVE_DECAY = 0.58f
        private const val TEMPORAL_IDLE_DECAY = 0.68f
        private const val TEMPORAL_ATTACK = 0.62f
        private const val TEMPORAL_CONFIDENCE_WEIGHT = 0.70f
        private const val TEMPORAL_RATIO_WEIGHT = 0.30f
        private const val TEMPORAL_STRONG_BONUS = 0.08f
        private const val TEMPORAL_RATIO_NORMALIZER = 3.0f
        private const val TEMPORAL_HIT_EVIDENCE = 0.12f
        private const val TEMPORAL_MAX_HITS = 5
        private const val TEMPORAL_MIN_HITS = 2
        private const val TEMPORAL_INFO_THRESHOLD = 0.16f
        private const val TEMPORAL_CAUTION_THRESHOLD = 0.18f
        private const val TEMPORAL_EMERGENCY_THRESHOLD = 0.20f
        private const val TEMPORAL_MIN_CONFIDENCE = 0.58f
        private const val TEMPORAL_MAX_CONFIDENCE = 0.94f
        private const val TEMPORAL_STRONG_MARGIN = 0.08f
        private const val TEMPORAL_DROP_FLOOR = 0.015f
        private const val CUSTOM_MATCH_THRESHOLD = 0.90f
        private const val CUSTOM_MIN_MATCH_THRESHOLD = 0.855f
        private const val CUSTOM_SAMPLE_THRESHOLD_STEP = 0.008f
        private const val CUSTOM_MIN_LEVEL_MULTIPLIER = 0.75f
        private const val CUSTOM_BASE_CONFIDENCE = 0.64f
        private const val CUSTOM_CONFIDENCE_GAIN = 1.9f
        private const val CUSTOM_PROFILE_BOOST_WEIGHT = 0.55f
        private const val CUSTOM_MIN_CONFIDENCE = 0.58f
        private const val CUSTOM_MAX_CONFIDENCE = 0.96f
        private const val CUSTOM_STRONG_MARGIN = 0.035f
        private data class DetectionCandidate(
            val kind: AlertKind,
            val baseThreshold: Float,
            val strongThreshold: Float,
            val minLevel: Float,
            val primaryTerms: List<String>,
            val supportingTerms: List<String> = emptyList(),
        )

        private data class DetectionMatch(
            val candidate: DetectionCandidate,
            val confidence: Float,
            val ratio: Float,
            val strong: Boolean,
            val source: String,
        )

        private val DETECTION_CANDIDATES = listOf(
            DetectionCandidate(
                kind = AlertKind.FireAlarm,
                baseThreshold = 0.055f,
                strongThreshold = 0.125f,
                minLevel = 0.014f,
                primaryTerms = listOf(
                    "smoke detector",
                    "smoke alarm",
                    "fire alarm",
                    "smoke detector, smoke alarm",
                ),
                supportingTerms = listOf(
                    "alarm",
                    "buzzer",
                    "beep, bleep",
                    "ding",
                    "ping",
                ),
            ),
            DetectionCandidate(
                kind = AlertKind.Siren,
                baseThreshold = 0.06f,
                strongThreshold = 0.135f,
                minLevel = 0.014f,
                primaryTerms = listOf(
                    "siren",
                    "civil defense siren",
                    "emergency vehicle",
                    "police car",
                    "ambulance",
                    "fire engine",
                    "fire truck",
                    "police car (siren)",
                    "ambulance (siren)",
                    "fire engine, fire truck (siren)",
                ),
                supportingTerms = listOf(
                    "alarm",
                    "air horn",
                    "vehicle horn",
                    "car horn",
                    "honking",
                    "foghorn",
                    "steam whistle",
                ),
            ),
            DetectionCandidate(
                kind = AlertKind.Doorbell,
                baseThreshold = 0.048f,
                strongThreshold = 0.115f,
                minLevel = 0.01f,
                primaryTerms = listOf(
                    "doorbell",
                    "ding-dong",
                    "door bell",
                    "chime",
                ),
                supportingTerms = listOf(
                    "bell",
                    "chime",
                    "ding",
                    "jingle",
                    "buzzer",
                ),
            ),
            DetectionCandidate(
                kind = AlertKind.Knock,
                baseThreshold = 0.058f,
                strongThreshold = 0.128f,
                minLevel = 0.018f,
                primaryTerms = listOf(
                    "knock",
                    "knocking",
                ),
                supportingTerms = listOf(
                    "tap",
                    "tapping",
                    "thump",
                    "thud",
                    "thunk",
                    "bang",
                    "slam",
                    "clicking",
                    "clatter",
                    "slap",
                ),
            ),
            DetectionCandidate(
                kind = AlertKind.BabyCry,
                baseThreshold = 0.06f,
                strongThreshold = 0.13f,
                minLevel = 0.014f,
                primaryTerms = listOf(
                    "baby cry",
                    "infant cry",
                    "baby cry, infant cry",
                    "crying, sobbing",
                ),
                supportingTerms = listOf(
                    "wail",
                    "moan",
                    "whimper",
                    "screaming",
                    "children shouting",
                    "child speech",
                    "children playing",
                ),
            ),
            DetectionCandidate(
                kind = AlertKind.WaterRunning,
                baseThreshold = 0.062f,
                strongThreshold = 0.14f,
                minLevel = 0.016f,
                primaryTerms = listOf(
                    "water tap",
                    "faucet",
                    "water tap, faucet",
                    "sink",
                    "sink (filling or washing)",
                    "water",
                    "bathtub",
                    "fill",
                    "pour",
                    "trickle",
                    "dribble",
                    "gush",
                ),
                supportingTerms = listOf(
                    "liquid",
                    "drip",
                    "splash",
                    "splatter",
                    "slosh",
                    "stream",
                    "gurgling",
                    "spray",
                    "pump",
                ),
            ),
            DetectionCandidate(
                kind = AlertKind.ApplianceBeep,
                baseThreshold = 0.046f,
                strongThreshold = 0.11f,
                minLevel = 0.01f,
                primaryTerms = listOf(
                    "beep, bleep",
                    "microwave oven",
                    "alarm clock",
                    "timer",
                    "dishwasher",
                    "buzzer",
                    "reversing beeps",
                    "ding",
                ),
                supportingTerms = listOf(
                    "ding",
                    "ping",
                    "electronic tuner",
                    "telephone dialing",
                    "dtmf",
                    "reversing beeps",
                    "cash register",
                    "printer",
                    "camera",
                ),
            ),
            DetectionCandidate(
                kind = AlertKind.PhoneRing,
                baseThreshold = 0.05f,
                strongThreshold = 0.12f,
                minLevel = 0.012f,
                primaryTerms = listOf(
                    "telephone bell ringing",
                    "ringtone",
                    "telephone",
                    "telephone ringing",
                ),
                supportingTerms = listOf(
                    "dial tone",
                    "busy signal",
                    "telephone dialing",
                    "dtmf",
                    "bell",
                    "jingle",
                ),
            ),
        )
    }
}

private data class ScoredLabel(
    val label: String,
    val score: Float,
)
