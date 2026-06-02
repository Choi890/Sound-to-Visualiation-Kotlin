package com.soundvisualization.accessibility

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class NameCallRecognizer(
    private val context: Context,
    private val watchedName: () -> String,
    private val speechLanguageTag: () -> String?,
    private val onPartialTranscript: (String, String?) -> Unit,
    private val onFinalTranscript: (String, String?) -> Unit,
    private val onDetected: (DetectedSoundEvent) -> Unit,
    private val onAvailabilityChanged: (Boolean) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onLevelChanged: (Float) -> Unit,
) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val recordingExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                runnable.run()
            },
            "NameCallRecorder",
        ).apply {
            priority = Thread.NORM_PRIORITY - 1
        }
    }
    private val decodingExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT + Process.THREAD_PRIORITY_LESS_FAVORABLE)
                runnable.run()
            },
            "NameCallDecoder",
        ).apply {
            priority = Thread.NORM_PRIORITY
        }
    }
    private val releaseExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(
            {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runnable.run()
            },
            "NameCallRelease",
        ).apply {
            priority = Thread.MIN_PRIORITY
        }
    }
    private val listening = AtomicBoolean(false)
    private val speechLanguageVersion = AtomicLong(0)
    private val recognizerLock = Any()

    @Volatile
    private var closed = false

    @Volatile
    private var recorder: AudioRecord? = null

    @Volatile
    private var recognizer: OfflineRecognizer? = null

    @Volatile
    private var recognizerLanguage: String? = null

    @Volatile
    private var availabilityCache: Boolean? = null

    @Volatile
    private var lastFinalPhrase = ""

    @Volatile
    private var lastFinalPhraseAt = 0L

    @Volatile
    private var lastNameCallAt = 0L

    @Volatile
    private var lastLevelPublishAt = 0L

    private val lastNameCallByKey = LinkedHashMap<String, Long>()

    @Volatile
    private var sharedAudioMode = false

    private val sharedSegmentLock = Any()
    private var sharedSegmenter = VoiceSegmenter(DETECTION_END_SILENCE_SAMPLES)
    private var pendingFinalTranscript = ""
    private var pendingFinalLanguageTag: String? = null
    private var pendingFinalRunnable: Runnable? = null

    @Volatile
    private var cachedWatchedNameRaw = ""

    @Volatile
    private var cachedWatchedNameTargets: List<NameTarget> = emptyList()

    fun isAvailable(): Boolean {
        if (!hasRecordPermission()) return false
        availabilityCache?.let { return it }

        val available = runCatching {
            appContext.assets.open("$SENSEVOICE_MODEL_DIR/model.int8.onnx").use { }
            appContext.assets.open("$SENSEVOICE_MODEL_DIR/tokens.txt").use { }
            true
        }.getOrDefault(false)
        availabilityCache = available
        return available
    }

    fun prepare() {
        if (closed || !hasRecordPermission()) return

        decodingExecutor.execute {
            if (closed) return@execute
            runCatching {
                availabilityCache = isAvailable()
                if (availabilityCache == true) {
                    obtainRecognizer(allowIdle = true)
                }
            }
        }
    }

    fun start() {
        if (closed) return

        if (!hasRecordPermission()) {
            postAvailability(false)
            postListening(false)
            return
        }
        if (!listening.compareAndSet(false, true)) return

        sharedAudioMode = false
        speechLanguageVersion.incrementAndGet()
        lastFinalPhrase = ""
        lastFinalPhraseAt = 0L
        lastNameCallByKey.clear()
        resetPendingFinalTranscript()
        postAvailability(true)
        postListening(true)
        recordingExecutor.execute(::recordLoop)
        decodingExecutor.execute {
            if (closed) return@execute
            val available = isAvailable()
            postAvailability(available)
            if (!available) {
                listening.set(false)
                postListening(false)
                return@execute
            }
            if (!closed && listening.get() && !sharedAudioMode) {
                runCatching { obtainRecognizer() }
                    .onFailure {
                        listening.set(false)
                        postAvailability(false)
                        postListening(false)
                    }
            }
        }
    }

    fun startSharedDetection(): Boolean {
        if (closed) return false

        if (!hasRecordPermission()) {
            postAvailability(false)
            return false
        }
        if (availabilityCache == false) {
            postAvailability(false)
            return false
        }
        if (!listening.compareAndSet(false, true)) return false

        sharedAudioMode = true
        speechLanguageVersion.incrementAndGet()
        lastFinalPhrase = ""
        lastFinalPhraseAt = 0L
        lastNameCallByKey.clear()
        resetPendingFinalTranscript()
        synchronized(sharedSegmentLock) {
            sharedSegmenter = VoiceSegmenter(DETECTION_END_SILENCE_SAMPLES)
        }
        postAvailability(true)
        decodingExecutor.execute {
            if (closed) return@execute
            val available = isAvailable()
            postAvailability(available)
            if (!available) {
                listening.set(false)
                sharedAudioMode = false
                return@execute
            }
            if (!closed && listening.get() && sharedAudioMode) {
                runCatching { obtainRecognizer() }
                    .onFailure {
                        listening.set(false)
                        sharedAudioMode = false
                        postAvailability(false)
                    }
            }
        }
        return true
    }

    fun acceptSharedAudio(samples: FloatArray) {
        if (closed || !sharedAudioMode || !listening.get() || samples.isEmpty()) return

        val segmentToDecode = synchronized(sharedSegmentLock) {
            sharedSegmenter.accept(samples)
        } ?: return

        enqueueDecode(segmentToDecode, detectOnly = true)
    }

    fun stop(notify: Boolean = true) {
        listening.set(false)
        sharedAudioMode = false
        if (notify) {
            flushPendingFinalTranscript()
        } else {
            resetPendingFinalTranscript()
        }
        synchronized(sharedSegmentLock) {
            sharedSegmenter = VoiceSegmenter(DETECTION_END_SILENCE_SAMPLES)
        }
        val activeRecorder = recorder
        recorder = null
        if (activeRecorder != null) {
            releaseExecutor.execute {
                releaseRecorder(activeRecorder)
            }
        }
        if (notify) {
            postListening(false)
        }
    }

    fun close() {
        closed = true
        stop()
        synchronized(recognizerLock) {
            runCatching { recognizer?.release() }
            recognizer = null
        }
        recordingExecutor.shutdownNow()
        decodingExecutor.shutdownNow()
        releaseExecutor.shutdownNow()
    }

    fun resetRecognizer() {
        stop(notify = false)
        synchronized(recognizerLock) {
            runCatching { recognizer?.release() }
            recognizer = null
            recognizerLanguage = null
        }
    }

    fun releaseRecognizerIfIdle() {
        if (closed || listening.get()) return

        releaseExecutor.execute {
            if (closed || listening.get()) return@execute
            synchronized(recognizerLock) {
                if (!listening.get()) {
                    runCatching { recognizer?.release() }
                    recognizer = null
                    recognizerLanguage = null
                }
            }
        }
    }

    fun refreshRecognizerForCurrentLanguage() {
        speechLanguageVersion.incrementAndGet()
        resetPendingFinalTranscript()
        lastFinalPhrase = ""
        lastFinalPhraseAt = 0L

        decodingExecutor.execute {
            if (closed) return@execute
            runCatching { obtainRecognizer() }
        }
    }

    private fun releaseRecorder(activeRecorder: AudioRecord) {
        runCatching {
            if (activeRecorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                activeRecorder.stop()
            }
        }
        runCatching { activeRecorder.release() }
    }

    @SuppressLint("MissingPermission")
    private fun recordLoop() {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = max(minBuffer.takeIf { it > 0 } ?: 0, SAMPLE_RATE)
        val audioRecord = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize * BYTES_PER_SAMPLE)
            .build()

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            listening.set(false)
            postAvailability(false)
            postListening(false)
            return
        }

        recorder = audioRecord
        val shortBuffer = ShortArray(FRAME_SAMPLES)
        val segmenter = VoiceSegmenter(SPEECH_END_SILENCE_SAMPLES)

        runCatching {
            audioRecord.startRecording()
            while (listening.get() && !closed) {
                val read = audioRecord.read(shortBuffer, 0, shortBuffer.size, AudioRecord.READ_BLOCKING)
                if (read <= 0) continue

                val frame = shortBuffer.toFloatArray(read)
                maybePostLevel(frame)
                val segment = segmenter.accept(frame)
                if (segment != null) {
                    enqueueDecode(segment)
                }
            }

            val pendingSegment = segmenter.flush()
            if (pendingSegment != null) {
                enqueueDecode(pendingSegment)
            }
        }.onFailure {
            if (!closed) {
                listening.set(false)
                postListening(false)
            }
        }

        if (recorder === audioRecord) {
            recorder = null
            releaseRecorder(audioRecord)
        }
    }

    private fun enqueueDecode(samples: FloatArray, detectOnly: Boolean = false) {
        if (samples.size < MIN_DECODE_SAMPLES || closed) return

        val decodeLanguageVersion = speechLanguageVersion.get()
        decodingExecutor.execute {
            if (closed) return@execute
            if (!listening.get()) return@execute
            if (decodeLanguageVersion != speechLanguageVersion.get()) return@execute
            val result = runCatching { decode(samples) }.getOrNull() ?: return@execute
            if (decodeLanguageVersion != speechLanguageVersion.get()) return@execute
            val text = result.text
            if (!text.isMeaningfulTranscript()) return@execute

            val languageTag = speechLanguageTag() ?: result.languageTag

            handler.post {
                if (closed || !listening.get()) return@post
                if (decodeLanguageVersion != speechLanguageVersion.get()) return@post
                val now = SystemClock.uptimeMillis()
                if (text == lastFinalPhrase && now - lastFinalPhraseAt < DUPLICATE_TRANSCRIPT_WINDOW_MS) {
                    return@post
                }
                lastFinalPhrase = text
                lastFinalPhraseAt = now
                if (!detectOnly) {
                    if (languageTag != null) {
                        queueTranscriptForTranslation(text, languageTag)
                    }
                }
                detectWatchedName(text)
            }
        }
    }

    private fun queueTranscriptForTranslation(text: String, languageTag: String) {
        val currentLanguage = pendingFinalLanguageTag
        if (
            pendingFinalTranscript.isNotBlank() &&
            currentLanguage != null &&
            currentLanguage != languageTag
        ) {
            flushPendingFinalTranscript()
        }

        pendingFinalTranscript = mergeAdjacentSpeech(pendingFinalTranscript, text)
        pendingFinalLanguageTag = languageTag
        onPartialTranscript(pendingFinalTranscript, languageTag)

        pendingFinalRunnable?.let(handler::removeCallbacks)
        val runnable = Runnable {
            pendingFinalRunnable = null
            flushPendingFinalTranscript()
        }
        pendingFinalRunnable = runnable
        handler.postDelayed(runnable, FINAL_TRANSCRIPT_DELAY_MS)
    }

    private fun flushPendingFinalTranscript() {
        pendingFinalRunnable?.let(handler::removeCallbacks)
        pendingFinalRunnable = null

        val finalText = pendingFinalTranscript.trim()
        val finalLanguage = pendingFinalLanguageTag
        pendingFinalTranscript = ""
        pendingFinalLanguageTag = null

        if (closed || finalText.isBlank() || finalLanguage == null) return
        onFinalTranscript(finalText, finalLanguage)
    }

    private fun resetPendingFinalTranscript() {
        pendingFinalRunnable?.let(handler::removeCallbacks)
        pendingFinalRunnable = null
        pendingFinalTranscript = ""
        pendingFinalLanguageTag = null
    }

    private fun mergeAdjacentSpeech(current: String, next: String): String {
        val left = current.trim()
        val right = next.trim()
        if (left.isBlank()) return right
        if (right.isBlank() || left == right) return left
        if (right.startsWith(left)) return right
        if (left.endsWith(right)) return left
        return "$left $right"
    }

    private fun decode(samples: FloatArray): SpeechDecodeResult {
        val engine = obtainRecognizer()
        val stream = engine.createStream()
        return try {
            stream.acceptWaveform(samples, SAMPLE_RATE)
            engine.decode(stream)
            val result = engine.getResult(stream)
            SpeechDecodeResult(
                text = result.text.cleanAsrText(),
                languageTag = result.lang.toSpeechLanguageTag(),
            )
        } finally {
            stream.release()
        }
    }

    private fun createRecognizer(language: String): OfflineRecognizer {
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(
                sampleRate = SAMPLE_RATE,
                featureDim = 80,
                dither = 0.0f,
            ),
            modelConfig = OfflineModelConfig(
                senseVoice = OfflineSenseVoiceModelConfig(
                    model = "$SENSEVOICE_MODEL_DIR/model.int8.onnx",
                    language = language,
                    useInverseTextNormalization = true,
                ),
                tokens = "$SENSEVOICE_MODEL_DIR/tokens.txt",
                numThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(2).coerceAtLeast(1),
                provider = "cpu",
                modelingUnit = "cjkchar",
            ),
            decodingMethod = "greedy_search",
            maxActivePaths = 4,
        )

        return OfflineRecognizer(assetManager = appContext.assets, config = config)
    }

    private fun obtainRecognizer(allowIdle: Boolean = false): OfflineRecognizer =
        speechLanguageTag().toSenseVoiceLanguage().let { language ->
            synchronized(recognizerLock) {
                val current = recognizer
                if (current != null && recognizerLanguage == language) {
                    return@let current
                }
            }

            val created = createRecognizer(language)
            synchronized(recognizerLock) {
                if (closed || (!allowIdle && !listening.get())) {
                    runCatching { created.release() }
                    error("Recognizer is no longer active")
                }

                val current = recognizer
                if (current != null && recognizerLanguage == language) {
                    runCatching { created.release() }
                    current
                } else {
                    runCatching { current?.release() }
                    recognizer = created
                    recognizerLanguage = language
                    created
                }
            }
        }

    private fun detectWatchedName(phrase: String) {
        val matchedName = findBestNameMatch(phrase) ?: return

        val now = SystemClock.uptimeMillis()
        val matchKey = matchedName.displayName.normalizeNameText()
        val lastForName = lastNameCallByKey[matchKey] ?: 0L
        if (now - lastForName < NAME_CALL_COOLDOWN_MS) return
        if (now - lastNameCallAt < NAME_CALL_GLOBAL_COOLDOWN_MS) return
        lastNameCallAt = now
        lastNameCallByKey[matchKey] = now

        onDetected(
            detectedEvent(
                kind = AlertKind.NameCall,
                confidence = matchedName.confidence,
                direction = SoundDirection.Unknown,
                source = "LocalSenseVoice",
            ).copy(
                title = matchedName.displayName,
                subtitle = NAME_CALL_SUBTITLE,
            ),
        )
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    private fun postAvailability(available: Boolean) {
        handler.post { onAvailabilityChanged(available) }
    }

    private fun postListening(active: Boolean) {
        handler.post { onListeningChanged(active) }
        if (!active) {
            postLevel(0f)
        }
    }

    private fun maybePostLevel(samples: FloatArray) {
        val now = SystemClock.uptimeMillis()
        if (now - lastLevelPublishAt < LEVEL_PUBLISH_INTERVAL_MS) return
        lastLevelPublishAt = now

        var sum = 0.0
        samples.forEach { sample ->
            sum += sample * sample
        }
        val rms = sqrt(sum / samples.size).toFloat()
        val level = ((rms - LEVEL_NOISE_FLOOR) / LEVEL_DYNAMIC_RANGE).coerceIn(0f, 1f)
        postLevel(level)
    }

    private fun postLevel(level: Float) {
        handler.post { onLevelChanged(level.coerceIn(0f, 1f)) }
    }

    private fun findBestNameMatch(phrase: String): NameMatch? {
        val normalizedPhrase = phrase.normalizeNameText().takeLast(MAX_NAME_MATCH_PHRASE_CHARS)
        if (normalizedPhrase.isBlank()) return null

        var bestMatch: NameMatch? = null
        for (target in watchedNameTargets()) {
            val confidence = target.bestConfidence(normalizedPhrase) ?: continue
            val current = bestMatch
            if (current == null || confidence > current.confidence) {
                bestMatch = NameMatch(
                    displayName = target.displayName,
                    confidence = confidence,
                )
            }
        }
        return bestMatch
    }

    private fun String.normalizeNameText(): String =
        lowercase(Locale.ROOT)
            .filter { it.isLetterOrDigit() }

    private fun String.nameTargetVariants(): Set<String> {
        val normalized = normalizeNameText()
        if (normalized.isBlank()) return emptySet()

        val stripped = normalized.stripNameSuffixes()

        val aliasCores = buildSet {
            add(stripped)
            stripped.koreanGivenNameAlias()?.let(::add)
        }

        return buildSet {
            add(normalized)
            aliasCores.forEach { core ->
                add(core)
                KOREAN_NAME_SUFFIXES.forEach { suffix ->
                    add(core + suffix)
                }
            }
        }
    }

    private fun String.stripNameSuffixes(): String =
        KOREAN_NAME_SUFFIXES.fold(this) { current, suffix ->
            if (current.length > suffix.length + 1 && current.endsWith(suffix)) {
                current.removeSuffix(suffix)
            } else {
                current
            }
        }

    private fun String.koreanGivenNameAlias(): String? =
        takeIf { value ->
            value.length >= 3 &&
                value.all { it in '\uAC00'..'\uD7AF' }
        }?.drop(1)

    private fun NameTarget.bestConfidence(normalizedPhrase: String): Float? {
        var best = 0f

        for (variant in variants) {
            if (normalizedPhrase.contains(variant)) {
                return EXACT_NAME_CONFIDENCE
            }

            val core = variant.stripNameSuffixes()
            if (core.length >= MIN_FUZZY_NAME_CHARS) {
                val prefixLength = max(MIN_FUZZY_NAME_CHARS, (core.length * 3 + 3) / 4)
                val prefix = core.take(prefixLength)
                if (normalizedPhrase.contains(prefix)) {
                    best = max(best, PREFIX_NAME_CONFIDENCE)
                }

                val fuzzyScore = normalizedPhrase.bestWindowSimilarity(core)
                if (fuzzyScore >= FUZZY_NAME_MATCH_THRESHOLD) {
                    val confidence = FUZZY_NAME_CONFIDENCE_MIN +
                        (fuzzyScore - FUZZY_NAME_MATCH_THRESHOLD) *
                        FUZZY_NAME_CONFIDENCE_SPAN /
                        (1f - FUZZY_NAME_MATCH_THRESHOLD)
                    best = max(best, confidence.coerceAtMost(FUZZY_NAME_CONFIDENCE_MAX))
                }
            }
        }

        return best.takeIf { it >= MIN_NAME_CONFIDENCE }
    }

    private fun String.bestWindowSimilarity(target: String): Float {
        if (length < MIN_FUZZY_NAME_CHARS || target.length < MIN_FUZZY_NAME_CHARS) return 0f

        val minWindow = max(MIN_FUZZY_NAME_CHARS, target.length - 1)
        val maxWindow = minOf(length, target.length + 1)
        val previous = IntArray(target.length + 1)
        val current = IntArray(target.length + 1)
        var best = 0f

        for (windowLength in minWindow..maxWindow) {
            val lastStart = length - windowLength
            for (start in 0..lastStart) {
                val maxDistance = max(1, target.length / 3)
                val distance = boundedLevenshteinDistance(
                    left = this,
                    leftStart = start,
                    leftLength = windowLength,
                    right = target,
                    maxDistance = maxDistance,
                    previousBuffer = previous,
                    currentBuffer = current,
                )
                if (distance <= maxDistance) {
                    val score = 1f - distance / max(windowLength, target.length).toFloat()
                    best = max(best, score)
                }
            }
        }

        return best
    }

    private fun boundedLevenshteinDistance(
        left: String,
        leftStart: Int,
        leftLength: Int,
        right: String,
        maxDistance: Int,
        previousBuffer: IntArray,
        currentBuffer: IntArray,
    ): Int {
        if (abs(leftLength - right.length) > maxDistance) return maxDistance + 1

        var previous = previousBuffer
        var current = currentBuffer
        for (index in 0..right.length) {
            previous[index] = index
        }

        for (leftIndex in 1..leftLength) {
            current[0] = leftIndex
            var rowMinimum = current[0]

            for (rightIndex in 1..right.length) {
                val substitutionCost = if (left[leftStart + leftIndex - 1] == right[rightIndex - 1]) 0 else 1
                val editDistance = minOf(
                    previous[rightIndex] + 1,
                    current[rightIndex - 1] + 1,
                    previous[rightIndex - 1] + substitutionCost,
                )
                current[rightIndex] = editDistance
                rowMinimum = minOf(rowMinimum, editDistance)
            }

            if (rowMinimum > maxDistance) return maxDistance + 1
            val swap = previous
            previous = current
            current = swap
        }

        return previous[right.length]
    }

    private fun watchedNameTargets(): List<NameTarget> {
        val raw = watchedName()
        val cached = cachedWatchedNameTargets
        if (raw == cachedWatchedNameRaw) return cached

        val next = parseWatchedNames(raw).map { name ->
            NameTarget(
                displayName = name,
                variants = name.nameTargetVariants()
                    .asSequence()
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sortedByDescending { it.length }
                    .toList(),
            )
        }
        cachedWatchedNameRaw = raw
        cachedWatchedNameTargets = next
        return next
    }

    private data class SpeechDecodeResult(
        val text: String,
        val languageTag: String?,
    )

    private data class NameTarget(
        val displayName: String,
        val variants: List<String>,
    )

    private data class NameMatch(
        val displayName: String,
        val confidence: Float,
    )

    private class VoiceSegmenter(
        private val endSilenceSamples: Int,
    ) {
        private val preRoll = FloatRingBuffer(PRE_ROLL_SAMPLES)
        private val segment = FloatAccumulator(MAX_SEGMENT_SAMPLES + PRE_ROLL_SAMPLES)
        private var activeSegment = false
        private var silenceSamples = 0
        private var noiseFloor = BASE_RMS_THRESHOLD

        fun accept(frame: FloatArray): FloatArray? {
            val energy = frame.energy()
            if (!activeSegment) {
                noiseFloor = ((noiseFloor * 0.95f) + (energy.rms * 0.05f)).coerceIn(0.0012f, 0.016f)
                preRoll.append(frame)
            } else {
                segment.append(frame)
            }

            val threshold = max(BASE_RMS_THRESHOLD, noiseFloor * NOISE_FLOOR_MULTIPLIER)
            val hasVoice = energy.rms >= threshold || energy.peak >= PEAK_THRESHOLD

            if (!activeSegment && hasVoice) {
                activeSegment = true
                silenceSamples = 0
                segment.clear()
                segment.append(preRoll.snapshot())
                preRoll.clear()
            }

            if (!activeSegment) return null

            silenceSamples = if (hasVoice) 0 else silenceSamples + frame.size
            val shouldDecode =
                (segment.size >= MIN_SEGMENT_SAMPLES && silenceSamples >= endSilenceSamples) ||
                    segment.size >= MAX_SEGMENT_SAMPLES

            if (!shouldDecode) return null

            val output = segment.snapshot()
            resetSegment()
            return output
        }

        fun flush(): FloatArray? {
            if (!activeSegment || segment.size < MIN_SEGMENT_SAMPLES) {
                reset()
                return null
            }

            val output = segment.snapshot()
            reset()
            return output
        }

        private fun resetSegment() {
            segment.clear()
            activeSegment = false
            silenceSamples = 0
        }

        private fun reset() {
            preRoll.clear()
            resetSegment()
        }
    }

    private class FloatAccumulator(initialCapacity: Int) {
        private var data = FloatArray(initialCapacity.coerceAtLeast(1))
        var size: Int = 0
            private set

        fun append(values: FloatArray) {
            ensureCapacity(size + values.size)
            values.copyInto(data, destinationOffset = size)
            size += values.size
        }

        fun snapshot(): FloatArray = data.copyOf(size)

        fun clear() {
            size = 0
        }

        private fun ensureCapacity(required: Int) {
            if (required <= data.size) return
            var nextSize = data.size
            while (nextSize < required) {
                nextSize *= 2
            }
            data = data.copyOf(nextSize)
        }
    }

    private class FloatRingBuffer(private val capacity: Int) {
        private val data = FloatArray(capacity)
        private var start = 0
        private var count = 0

        fun append(values: FloatArray) {
            for (value in values) {
                if (count < capacity) {
                    data[(start + count) % capacity] = value
                    count += 1
                } else {
                    data[start] = value
                    start = (start + 1) % capacity
                }
            }
        }

        fun snapshot(): FloatArray {
            val output = FloatArray(count)
            for (index in 0 until count) {
                output[index] = data[(start + index) % capacity]
            }
            return output
        }

        fun clear() {
            start = 0
            count = 0
        }
    }

    companion object {
        private const val SENSEVOICE_MODEL_DIR = "sherpa-onnx-sense-voice-ko-en-ja-int8-2024-07-17"
        private const val SAMPLE_RATE = 16_000
        private const val BYTES_PER_SAMPLE = 2
        private const val FRAME_SAMPLES = 800
        private const val PRE_ROLL_SAMPLES = SAMPLE_RATE / 3
        private const val MIN_SEGMENT_SAMPLES = SAMPLE_RATE * 3 / 10
        private const val MIN_DECODE_SAMPLES = SAMPLE_RATE / 4
        private const val DETECTION_END_SILENCE_SAMPLES = SAMPLE_RATE * 9 / 20
        private const val SPEECH_END_SILENCE_SAMPLES = SAMPLE_RATE * 13 / 10
        private const val MAX_SEGMENT_SAMPLES = SAMPLE_RATE * 12
        private const val BASE_RMS_THRESHOLD = 0.0055f
        private const val PEAK_THRESHOLD = 0.032f
        private const val NOISE_FLOOR_MULTIPLIER = 2.15f
        private const val LEVEL_PUBLISH_INTERVAL_MS = 32L
        private const val LEVEL_NOISE_FLOOR = 0.012f
        private const val LEVEL_DYNAMIC_RANGE = 0.16f
        private const val DUPLICATE_TRANSCRIPT_WINDOW_MS = 1800L
        private const val FINAL_TRANSCRIPT_DELAY_MS = 1250L
        private const val NAME_CALL_COOLDOWN_MS = 2400L
        private const val NAME_CALL_GLOBAL_COOLDOWN_MS = 700L
        private const val NAME_CALL_SUBTITLE = "\uc774\ub984 \ud638\ucd9c"
        private const val MAX_NAME_MATCH_PHRASE_CHARS = 96
        private const val MIN_FUZZY_NAME_CHARS = 4
        private const val EXACT_NAME_CONFIDENCE = 0.96f
        private const val PREFIX_NAME_CONFIDENCE = 0.84f
        private const val MIN_NAME_CONFIDENCE = 0.78f
        private const val FUZZY_NAME_MATCH_THRESHOLD = 0.72f
        private const val FUZZY_NAME_CONFIDENCE_MIN = 0.80f
        private const val FUZZY_NAME_CONFIDENCE_MAX = 0.90f
        private const val FUZZY_NAME_CONFIDENCE_SPAN = FUZZY_NAME_CONFIDENCE_MAX - FUZZY_NAME_CONFIDENCE_MIN
        private val KOREAN_NAME_SUFFIXES = listOf(
            "\uc120\uc0dd\ub2d8",
            "\uc774\uc57c",
            "\uc544\uc57c",
            "\uc57c",
            "\uc544",
            "\uc528",
            "\ub2d8",
        )
    }
}

private data class FrameEnergy(
    val rms: Float,
    val peak: Float,
)

private fun ShortArray.toFloatArray(length: Int): FloatArray {
    val output = FloatArray(length)
    for (index in 0 until length) {
        output[index] = this[index] / 32768.0f
    }
    return output
}

private fun FloatArray.energy(): FrameEnergy {
    if (isEmpty()) return FrameEnergy(rms = 0f, peak = 0f)

    var energy = 0.0
    var peak = 0f
    for (sample in this) {
        energy += sample * sample
        peak = max(peak, abs(sample))
    }
    return FrameEnergy(
        rms = sqrt((energy / size).toFloat()),
        peak = peak,
    )
}

private fun String.cleanAsrText(): String =
    replace(ASR_TAG_REGEX, "")
        .replace(ASR_WHITESPACE_REGEX, " ")
        .trim()

private fun String.isMeaningfulTranscript(): Boolean {
    val content = filter { it.isLetterOrDigit() }
    if (content.isBlank()) return false

    val hasCjkOrKana = any { char ->
        char in '\uAC00'..'\uD7AF' ||
            char in '\u3040'..'\u30FF' ||
            char in '\u4E00'..'\u9FFF'
    }
    if (hasCjkOrKana) return true

    val wordCount = trim()
        .split(ASR_WHITESPACE_REGEX)
        .count { word -> word.any { it.isLetterOrDigit() } }
    return content.length >= 4 || wordCount >= 2
}

private val ASR_TAG_REGEX = Regex("<\\|[^|]+\\|>")
private val ASR_WHITESPACE_REGEX = Regex("\\s+")

private fun String?.toSpeechLanguageTag(): String? {
    val code = this
        ?.replace("<|", "")
        ?.replace("|>", "")
        ?.substringBefore(',')
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return when (code) {
        "ko" -> "ko-KR"
        "en" -> "en-US"
        "ja" -> "ja-JP"
        else -> null
    }
}

internal fun preferredNameCallSpeechLanguageTag(watchedName: String): String {
    val normalizedNames = parseWatchedNames(watchedName).joinToString(separator = "")
    if (normalizedNames.isBlank()) return "ko-KR"

    var hangulCount = 0
    var kanaCount = 0
    var cjkCount = 0
    var latinCount = 0

    normalizedNames.forEach { char ->
        when {
            char in '\uAC00'..'\uD7AF' -> hangulCount += 1
            char in '\u3040'..'\u30FF' -> kanaCount += 1
            char in '\u4E00'..'\u9FFF' -> cjkCount += 1
            char in 'A'..'Z' || char in 'a'..'z' -> latinCount += 1
        }
    }

    val activeScriptCount = listOf(hangulCount, kanaCount + cjkCount, latinCount).count { it > 0 }
    if (activeScriptCount > 1) return "auto"

    return when {
        hangulCount > 0 -> "ko-KR"
        kanaCount > 0 || cjkCount > 0 -> "ja-JP"
        latinCount > 0 -> "en-US"
        else -> "ko-KR"
    }
}

private fun String?.toSenseVoiceLanguage(): String =
    when (this?.substringBefore('-')?.lowercase()) {
        "ko" -> "ko"
        "en" -> "en"
        "ja" -> "ja"
        "auto" -> "auto"
        else -> "ko"
    }
