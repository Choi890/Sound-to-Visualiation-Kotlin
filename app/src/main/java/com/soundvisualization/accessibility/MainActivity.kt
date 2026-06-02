package com.soundvisualization.accessibility

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

private const val POPUP_BACKDROP_BLUR_DP = 6f
private const val ALARM_BACKDROP_BLUR_DP = 7f
private const val WINDOW_BLUR_BEHIND_DP = 18f
private val PLATFORM_POPUP_MIN_HEIGHT = 420.dp
private val PLATFORM_POPUP_MAX_HEIGHT = 590.dp

private object AuraMotion {
    const val PressMillis = 96
    const val ControlMillis = 190
    const val BackdropMillis = 280
    const val PageMillis = 360
    const val PopupEnterMillis = 420
    const val PopupExitMillis = 300
    const val LiquidPopupEnterMillis = 500
    const val LiquidPopupExitMillis = 360
    const val PageSlideMillis = 390
    const val AlarmEnterMillis = 340
    const val AlarmExitMillis = 260
}

private fun pageSlideSpec(): FiniteAnimationSpec<Float> =
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 430f,
        visibilityThreshold = 0.002f,
    )

enum class AppColorMode {
    Dark,
    Light;

    fun toggled(): AppColorMode = if (this == Dark) Light else Dark

    companion object {
        fun fromStoredValue(value: String?): AppColorMode =
            entries.firstOrNull { it.name == value } ?: Dark
    }
}

class MainActivity : ComponentActivity() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        )
        @Suppress("DEPRECATION")
        window.attributes = window.attributes.apply {
            preferredRefreshRate = 60f
        }
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            show(WindowInsetsCompat.Type.statusBars())
            show(WindowInsetsCompat.Type.navigationBars())
        }

        setContent {
            var appLanguage by rememberSaveable {
                mutableStateOf(LocalAppStore.loadAppLanguage(applicationContext))
            }
            var appColorMode by rememberSaveable {
                mutableStateOf(LocalAppStore.loadAppColorMode(applicationContext))
            }

            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = appColorMode == AppColorMode.Light
                    isAppearanceLightNavigationBars = appColorMode == AppColorMode.Light
                }
            }

            SoundVisualizationTheme(colorMode = appColorMode) {
                CompositionLocalProvider(LocalAppStrings provides appLanguage.strings) {
                var runtimeState by remember { mutableStateOf(SoundRuntimeState()) }
                var activeEvent by remember { mutableStateOf(idleEvent()) }
                var watchedName by remember { mutableStateOf(LocalAppStore.loadWatchedName(applicationContext)) }
                var watchedNameDraft by rememberSaveable { mutableStateOf("") }
                var alertSettings by remember {
                    mutableStateOf(LocalAppStore.loadAlertSettings(applicationContext))
                }
                var translationState by remember { mutableStateOf(TranslationState()) }
                var speechListening by remember { mutableStateOf(false) }
                var speechInputLevel by remember { mutableFloatStateOf(0f) }
                var pendingSpeechStart by remember { mutableStateOf(false) }
                var pendingAlwaysOnStart by remember { mutableStateOf(false) }
                var pendingSoundStatusStart by remember { mutableStateOf(false) }
                var pendingSoundDetectionStart by remember { mutableStateOf(false) }
                var pendingTrainingKind by remember { mutableStateOf<AlertKind?>(null) }
                var recordingTrainingKind by remember { mutableStateOf<AlertKind?>(null) }
                var appMode by remember { mutableStateOf(AppMode.SoundDetection) }
                var uiPreloadComplete by remember { mutableStateOf(false) }
                var featureReadyMode by remember { mutableStateOf<AppMode?>(null) }
                var eventHistory by remember {
                    mutableStateOf(LocalAppStore.loadEvents(applicationContext))
                }
                var speechHistory by remember { mutableStateOf<List<SpeechRecognitionHistoryItem>>(emptyList()) }
                var flashEvent by remember { mutableStateOf<DetectedSoundEvent?>(null) }
                val storeScope = rememberCoroutineScope()

                fun registerEvent(event: DetectedSoundEvent) {
                    if (event.kind != AlertKind.Idle) {
                        activeEvent = event

                        if (alertSettings.screenFlashEnabled) {
                            flashEvent = event
                        }

                        if (alertSettings.vibrationEnabled) {
                            vibrateFor(event.kind)
                        }

                        storeScope.launch(Dispatchers.IO) {
                            val nextHistory = LocalAppStore.appendEvent(applicationContext, event)
                            withContext(Dispatchers.Main.immediate) {
                                eventHistory = nextHistory
                                if (activeEvent.id == event.id) {
                                    activeEvent = nextHistory.firstOrNull() ?: event
                                }
                            }
                        }
                    } else {
                        activeEvent = event
                    }
                }

                fun saveEventHistory(next: List<DetectedSoundEvent>) {
                    eventHistory = next
                    storeScope.launch(Dispatchers.IO) {
                        LocalAppStore.saveEvents(applicationContext, next)
                    }
                }

                suspend fun refreshAlwaysOnEvents(): Boolean {
                    val storedSettings = withContext(Dispatchers.IO) {
                        LocalAppStore.loadAlertSettings(applicationContext)
                    }
                    if (!storedSettings.alwaysOnEnabled && alertSettings.alwaysOnEnabled) {
                        alertSettings = alertSettings.copy(alwaysOnEnabled = false)
                        return false
                    }

                    val latestEvents = withContext(Dispatchers.IO) {
                        LocalAppStore.loadEvents(applicationContext)
                    }
                    val latestEvent = latestEvents.firstOrNull()
                    val currentEventId = eventHistory.firstOrNull()?.id
                    eventHistory = latestEvents
                    if (latestEvent != null && latestEvent.id != currentEventId) {
                        activeEvent = latestEvent
                        if (alertSettings.screenFlashEnabled) {
                            flashEvent = latestEvent
                        }
                    }
                    return true
                }

                fun adjustKindSensitivity(kind: AlertKind, delta: Float) {
                    if (kind == AlertKind.Idle) return
                    val current = alertSettings.categorySetting(kind)
                    val nextSettings = alertSettings.categorySettings.toMutableMap().apply {
                        this[kind] = current.copy(
                            sensitivity = (current.sensitivity + delta).stableCategorySensitivity(),
                        )
                    }
                    alertSettings = alertSettings.copy(categorySettings = nextSettings)
                }

                fun recordEventFeedback(event: DetectedSoundEvent, feedback: FeedbackStatus) {
                    val next = eventHistory.map { item ->
                        if (item.id == event.id) item.copy(feedback = feedback) else item
                    }
                    saveEventHistory(next)
                    if (activeEvent.id == event.id) {
                        activeEvent = idleEvent()
                    }

                    when (feedback) {
                        FeedbackStatus.Correct -> adjustKindSensitivity(event.kind, 0.05f)
                        FeedbackStatus.FalsePositive -> adjustKindSensitivity(event.kind, -0.05f)
                        FeedbackStatus.Missed -> adjustKindSensitivity(event.kind, 0.10f)
                        FeedbackStatus.None -> Unit
                    }
                }

                fun deleteSampleFiles(paths: List<String>) {
                    paths.forEach { path ->
                        runCatching { File(path).delete() }
                    }
                }

                fun addTrainingSample(result: TrainingRecordingResult) {
                    val kind = result.kind
                    val profile = alertSettings.customProfile(kind)
                    val sampleCount = (profile.sampleCount + 1).coerceAtMost(5)
                    val keepRawAudio = alertSettings.privacyAudioStorageEnabled
                    val nextRawAudioPaths = if (keepRawAudio) {
                        profile.sampleFilePaths + result.filePath
                    } else {
                        emptyList()
                    }
                    val nextPaths = nextRawAudioPaths.takeLast(5)
                    val droppedPaths = if (keepRawAudio) {
                        nextRawAudioPaths.dropLast(5)
                    } else {
                        profile.sampleFilePaths + result.filePath
                    }
                    val previousVectors = profile.featureVectors.ifEmpty {
                        if (profile.featureVector.isNotEmpty()) listOf(profile.featureVector) else emptyList()
                    }
                    val nextVectors = (previousVectors + listOf(result.featureVector)).takeLast(5)
                    deleteSampleFiles(droppedPaths)
                    val nextProfile = profile.copy(
                        sampleCount = sampleCount,
                        lastTrainedAt = System.currentTimeMillis(),
                        confidenceBoost = (sampleCount * 0.035f).coerceAtMost(0.20f),
                        featureVector = SoundFeatureExtractor.merge(
                            existing = profile.featureVector,
                            existingCount = profile.sampleCount,
                            next = result.featureVector,
                        ),
                        featureVectors = nextVectors,
                        sampleFilePaths = nextPaths,
                    )
                    alertSettings = alertSettings.copy(
                        customProfiles = alertSettings.customProfiles.toMutableMap().apply {
                            this[kind] = nextProfile
                        },
                    )
                }

                fun resetTrainingProfile(kind: AlertKind) {
                    val currentCategory = alertSettings.categorySetting(kind)
                    val currentProfile = alertSettings.customProfile(kind)
                    deleteSampleFiles(currentProfile.sampleFilePaths)
                    alertSettings = alertSettings.copy(
                        categorySettings = alertSettings.categorySettings.toMutableMap().apply {
                            this[kind] = currentCategory.copy(
                                sensitivity = kind.defaultSensitivity(),
                            )
                        },
                        customProfiles = alertSettings.customProfiles.toMutableMap().apply {
                            this[kind] = CustomSoundProfile(kind = kind)
                        },
                    )
                }

                fun rememberSpeechRecognition(transcript: String, detectedLanguageTag: String?) {
                    val rawSourceText = transcript.trim()
                    if (rawSourceText.isBlank()) return

                    val sourceLanguage = normalizedTranslationSource(translationState.source)
                    val sourceText = inferSpeechRecognitionText(rawSourceText, sourceLanguage.modelCode)
                    val targetLanguage = normalizedTranslationTarget(translationState.target)
                    val latest = speechHistory.firstOrNull()
                    val isDuplicate = latest != null &&
                        latest.sourceText == sourceText &&
                            latest.sourceLanguage == sourceLanguage.modelCode &&
                            latest.targetLanguage == targetLanguage.modelCode &&
                        System.currentTimeMillis() - latest.id < 2500L

                    if (!isDuplicate) {
                        speechHistory = (
                            listOf(
                                SpeechRecognitionHistoryItem(
                                    sourceText = sourceText,
                                    sourceLanguage = sourceLanguage.modelCode,
                                    targetLanguage = targetLanguage.modelCode,
                                ),
                            ) + speechHistory
                            ).take(30)
                    }
                }

                fun updateLatestSpeechHistory(next: TranslationState) {
                    val sourceText = next.sourceText.trim()
                    val translatedText = next.translatedText.trim()
                    if (sourceText.isBlank() || translatedText.isBlank()) return

                    val index = speechHistory.indexOfFirst { item ->
                        item.sourceText == sourceText &&
                            item.sourceLanguage == next.source.modelCode &&
                            item.targetLanguage == next.outputLanguage().modelCode
                    }
                    if (index < 0 || speechHistory[index].translatedText == translatedText) return

                    speechHistory = speechHistory.toMutableList().also { history ->
                        history[index] = history[index].copy(translatedText = translatedText)
                    }
                }

                val detector = remember {
                    SoundDetectionController(
                        context = applicationContext,
                        onState = { next ->
                            mainHandler.post {
                                runtimeState = next
                            }
                        },
                        onEvent = { event ->
                            mainHandler.post {
                                registerEvent(event)
                            }
                        },
                    )
                }
                val trainingRecorder = remember {
                    SoundTrainingRecorder(applicationContext)
                }
                val translator = remember {
                    ConversationTranslator(
                        context = this@MainActivity,
                        onState = { next ->
                            mainHandler.post {
                                translationState = next
                                updateLatestSpeechHistory(next)
                            }
                        },
                    )
                }
                val nameRecognizer = remember {
                    NameCallRecognizer(
                        context = this@MainActivity,
                        watchedName = {
                            if (alertSettings.categorySetting(AlertKind.NameCall).enabled) watchedName else ""
                        },
                        speechLanguageTag = {
                            if (appMode == AppMode.SpeechRecognition) {
                                translationState.source.speechLanguageTag
                            } else if (parseWatchedNames(watchedName).isEmpty()) {
                                translationState.source.speechLanguageTag
                            } else {
                                preferredNameCallSpeechLanguageTag(watchedName)
                            }
                        },
                        onPartialTranscript = { transcript, detectedLanguageTag ->
                            mainHandler.post {
                                translator.preview(transcript, detectedLanguageTag)
                            }
                        },
                        onFinalTranscript = { transcript, detectedLanguageTag ->
                            mainHandler.post {
                                rememberSpeechRecognition(transcript, detectedLanguageTag)
                                translator.translate(transcript, detectedLanguageTag)
                            }
                        },
                        onDetected = { event ->
                            mainHandler.post {
                                registerEvent(event)
                            }
                        },
                        onAvailabilityChanged = { available ->
                            mainHandler.post {
                                detector.setSpeechRecognitionAvailable(available)
                            }
                        },
                        onListeningChanged = { listening ->
                            mainHandler.post {
                                speechListening = listening
                                if (!listening) {
                                    speechInputLevel = 0f
                                }
                            }
                        },
                        onLevelChanged = { level ->
                            mainHandler.post {
                                speechInputLevel = level
                            }
                        },
                    )
                }

                fun startSoundStatusGranted() {
                    if (alertSettings.alwaysOnEnabled) {
                        return
                    }
                    if (speechListening) {
                        speechListening = false
                    }
                    nameRecognizer.stop(notify = false)
                    detector.setSharedSpeechAudioSink(null)
                    detector.startAudio()
                }

                fun startSoundDetectionGranted() {
                    if (alertSettings.alwaysOnEnabled) {
                        return
                    }
                    if (speechListening) {
                        speechListening = false
                    }
                    nameRecognizer.stop(notify = false)
                    if (
                        alertSettings.categorySetting(AlertKind.NameCall).enabled &&
                        parseWatchedNames(watchedName).isNotEmpty()
                    ) {
                        val started = nameRecognizer.startSharedDetection()
                        detector.setSharedSpeechAudioSink(
                            if (started) nameRecognizer::acceptSharedAudio else null,
                        )
                    } else {
                        detector.setSharedSpeechAudioSink(null)
                    }
                    detector.start()
                }

                fun startSpeechRecognitionGranted() {
                    if (alertSettings.alwaysOnEnabled) {
                        SoundMonitorService.stop(applicationContext)
                        alertSettings = alertSettings.copy(alwaysOnEnabled = false)
                    }
                    detector.setSharedSpeechAudioSink(null)
                    detector.stop(publishState = false)
                    runtimeState = runtimeState.copy(
                        isAudioActive = false,
                        isListening = false,
                        level = 0f,
                        direction = SoundDirection.Unknown,
                    )
                    activeEvent = idleEvent()
                    translationState = translationState.copy(
                        sourceText = "",
                        translatedText = "",
                    )
                    translator.prepare()
                    nameRecognizer.prepare()
                    nameRecognizer.start()
                }

                fun launchAlwaysOnService() {
                    detector.setSharedSpeechAudioSink(null)
                    nameRecognizer.stop()
                    detector.stop()
                    speechListening = false
                    runtimeState = runtimeState.copy(
                        isAudioActive = false,
                        isListening = false,
                        level = 0f,
                        direction = SoundDirection.Unknown,
                    )
                    SoundMonitorService.start(applicationContext)
                }

                fun beginTrainingRecording(kind: AlertKind) {
                    if (recordingTrainingKind != null) return

                    nameRecognizer.stop(notify = false)
                    detector.setSharedSpeechAudioSink(null)
                    detector.stop(publishState = false)
                    runtimeState = runtimeState.copy(
                        isAudioActive = false,
                        isListening = false,
                        level = 0f,
                        direction = SoundDirection.Unknown,
                    )
                    speechListening = false
                    activeEvent = idleEvent()
                    recordingTrainingKind = kind

                    trainingRecorder.record(kind) { result ->
                        mainHandler.post {
                            result.onSuccess(::addTrainingSample)
                            recordingTrainingKind = null
                        }
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        val trainingKind = pendingTrainingKind
                        if (trainingKind != null) {
                            beginTrainingRecording(trainingKind)
                        } else if (pendingAlwaysOnStart) {
                            launchAlwaysOnService()
                        } else if (pendingSpeechStart) {
                            startSpeechRecognitionGranted()
                        } else if (pendingSoundDetectionStart) {
                            startSoundDetectionGranted()
                        } else if (pendingSoundStatusStart) {
                            startSoundStatusGranted()
                        } else {
                            startSoundStatusGranted()
                        }
                    } else {
                        runtimeState = runtimeState.copy(isAudioActive = false, isListening = false)
                        if (pendingAlwaysOnStart) {
                            alertSettings = alertSettings.copy(alwaysOnEnabled = false)
                        }
                    }
                    pendingTrainingKind = null
                    pendingSpeechStart = false
                    pendingAlwaysOnStart = false
                    pendingSoundStatusStart = false
                    pendingSoundDetectionStart = false
                }

                fun startTrainingRecording(kind: AlertKind) {
                    if (recordingTrainingKind != null) return

                    if (trainingRecorder.hasRecordPermission()) {
                        beginTrainingRecording(kind)
                    } else {
                        pendingTrainingKind = kind
                        pendingSpeechStart = false
                        pendingAlwaysOnStart = false
                        pendingSoundStatusStart = false
                        pendingSoundDetectionStart = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        launchAlwaysOnService()
                    } else {
                        alertSettings = alertSettings.copy(alwaysOnEnabled = false)
                    }
                }

                fun hasRecordPermission(): Boolean =
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED

                fun hasNotificationPermission(): Boolean =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED

                fun startAlwaysOnService() {
                    if (!hasRecordPermission()) {
                        pendingSpeechStart = false
                        pendingAlwaysOnStart = true
                        pendingSoundStatusStart = false
                        pendingSoundDetectionStart = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return
                    }

                    if (!hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return
                    }

                    launchAlwaysOnService()
                }

                fun changeAlwaysOn(enabled: Boolean) {
                    alertSettings = alertSettings.copy(alwaysOnEnabled = enabled)
                    if (enabled) {
                        startAlwaysOnService()
                    } else {
                        SoundMonitorService.stop(applicationContext)
                        if (appMode == AppMode.SoundDetection) {
                            if (hasRecordPermission()) {
                                startSoundStatusGranted()
                            } else {
                                pendingSpeechStart = false
                                pendingAlwaysOnStart = false
                                pendingSoundStatusStart = true
                                pendingSoundDetectionStart = false
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                }

                fun startListening() {
                    if (hasRecordPermission()) {
                        startSoundDetectionGranted()
                    } else {
                        pendingSpeechStart = false
                        pendingAlwaysOnStart = false
                        pendingSoundStatusStart = false
                        pendingSoundDetectionStart = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                fun startSoundStatus() {
                    if (hasRecordPermission()) {
                        startSoundStatusGranted()
                    } else {
                        pendingSpeechStart = false
                        pendingAlwaysOnStart = false
                        pendingSoundStatusStart = true
                        pendingSoundDetectionStart = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                fun startSpeechRecognition() {
                    if (speechListening) return

                    if (hasRecordPermission()) {
                        startSpeechRecognitionGranted()
                    } else {
                        pendingSpeechStart = true
                        pendingAlwaysOnStart = false
                        pendingSoundStatusStart = false
                        pendingSoundDetectionStart = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                fun stopListening() {
                    detector.setSharedSpeechAudioSink(null)
                    nameRecognizer.stop()
                    detector.stopDetection()
                    speechListening = false
                }

                fun toggleSpeechRecognition() {
                    if (speechListening) {
                        nameRecognizer.stop()
                        speechListening = false
                        return
                    }

                    startSpeechRecognition()
                }

                fun stopRealtimeWorkForTransition() {
                    nameRecognizer.stop(notify = false)
                    detector.setSharedSpeechAudioSink(null)
                    detector.stop(publishState = false)
                    runtimeState = runtimeState.copy(
                        isAudioActive = false,
                        isListening = false,
                        level = 0f,
                        direction = SoundDirection.Unknown,
                    )
                    if (speechListening) {
                        speechListening = false
                    }
                }

                fun startActiveMode(mode: AppMode) {
                    when (mode) {
                        AppMode.SoundDetection -> {
                            startSoundStatus()
                        }

                        AppMode.SpeechRecognition -> {
                            translator.prepare()
                            nameRecognizer.prepare()
                        }

                        AppMode.SoundManagement -> Unit
                    }
                }

                fun changeAppMode(nextMode: AppMode) {
                    if (appMode == nextMode) return

                    val previousMode = appMode
                    featureReadyMode = null
                    stopRealtimeWorkForTransition()
                    if (previousMode == AppMode.SpeechRecognition && nextMode != AppMode.SpeechRecognition) {
                        translator.releaseIdleResources()
                    }
                    if (
                        nextMode != AppMode.SpeechRecognition &&
                        (nextMode != AppMode.SoundDetection || parseWatchedNames(watchedName).isEmpty())
                    ) {
                        nameRecognizer.releaseRecognizerIfIdle()
                    }
                    activeEvent = idleEvent()
                    appMode = nextMode
                }

                LaunchedEffect(alertSettings.alwaysOnEnabled, uiPreloadComplete) {
                    if (!alertSettings.alwaysOnEnabled || !uiPreloadComplete) return@LaunchedEffect

                    withFrameNanos { }
                    delay(MODE_START_DELAY_MS)
                    startAlwaysOnService()
                }

                LaunchedEffect(alertSettings.alwaysOnEnabled, appMode, uiPreloadComplete) {
                    if (
                        !alertSettings.alwaysOnEnabled ||
                        appMode != AppMode.SoundDetection ||
                        !uiPreloadComplete
                    ) {
                        return@LaunchedEffect
                    }

                    if (!refreshAlwaysOnEvents()) return@LaunchedEffect
                    while (true) {
                        delay(ALWAYS_ON_EVENT_POLL_MS)
                        if (!refreshAlwaysOnEvents()) break
                    }
                }

                LaunchedEffect(appMode, uiPreloadComplete) {
                    featureReadyMode = null
                    withFrameNanos { }
                    if (!uiPreloadComplete) return@LaunchedEffect

                    delay(MODE_START_DELAY_MS)
                    featureReadyMode = appMode
                    withFrameNanos { }
                    startActiveMode(appMode)
                    if (appMode == AppMode.SpeechRecognition) {
                        delay(TRANSLATOR_PREPARE_DELAY_MS)
                        if (appMode == AppMode.SpeechRecognition) {
                            translator.prepare()
                            nameRecognizer.prepare()
                        }
                    }
                }

                LaunchedEffect(uiPreloadComplete) {
                    if (!uiPreloadComplete) return@LaunchedEffect

                    withFrameNanos { }
                    delay(SOUND_CLASSIFIER_PRELOAD_DELAY_MS)
                    detector.prepare()
                }

                LaunchedEffect(activeEvent.id) {
                    if (activeEvent.kind != AlertKind.Idle) {
                        delay(5200)
                        activeEvent = idleEvent()
                    }
                }

                LaunchedEffect(flashEvent?.id) {
                    if (flashEvent != null) {
                        delay(900)
                        flashEvent = null
                    }
                }

                LaunchedEffect(alertSettings) {
                    val settingsToSave = if (!alertSettings.privacyAudioStorageEnabled) {
                        val storedAudioPaths = alertSettings.customProfiles.values
                            .flatMap { it.sampleFilePaths }
                        if (storedAudioPaths.isNotEmpty()) {
                            withContext(Dispatchers.IO) {
                                deleteSampleFiles(storedAudioPaths)
                            }
                            alertSettings.copy(
                                customProfiles = alertSettings.customProfiles.mapValues { (_, profile) ->
                                    profile.copy(sampleFilePaths = emptyList())
                                },
                            )
                        } else {
                            alertSettings
                        }
                    } else {
                        alertSettings
                    }
                    if (settingsToSave != alertSettings) {
                        alertSettings = settingsToSave
                        return@LaunchedEffect
                    }

                    detector.updateSettings(settingsToSave)
                    withContext(Dispatchers.IO) {
                        LocalAppStore.saveAlertSettings(applicationContext, settingsToSave)
                    }
                    if (!settingsToSave.alwaysOnEnabled) {
                        SoundMonitorService.stop(applicationContext)
                    }
                }

                LaunchedEffect(appLanguage) {
                    val languageToSave = appLanguage
                    withContext(Dispatchers.IO) {
                        LocalAppStore.saveAppLanguage(applicationContext, languageToSave)
                    }
                }

                LaunchedEffect(appColorMode) {
                    val colorModeToSave = appColorMode
                    withContext(Dispatchers.IO) {
                        LocalAppStore.saveAppColorMode(applicationContext, colorModeToSave)
                    }
                }

                LaunchedEffect(
                    watchedName,
                    alertSettings.categorySetting(AlertKind.NameCall).enabled,
                    appMode,
                    alertSettings.alwaysOnEnabled,
                ) {
                    val watchedNameToSave = watchedName
                    withContext(Dispatchers.IO) {
                        LocalAppStore.saveWatchedName(applicationContext, watchedNameToSave)
                    }
                    delay(320)
                    if (
                        appMode == AppMode.SoundDetection &&
                        runtimeState.isListening &&
                        !alertSettings.alwaysOnEnabled
                    ) {
                        nameRecognizer.stop(notify = false)
                        if (
                            alertSettings.categorySetting(AlertKind.NameCall).enabled &&
                            parseWatchedNames(watchedName).isNotEmpty()
                        ) {
                            val started = nameRecognizer.startSharedDetection()
                            detector.setSharedSpeechAudioSink(
                                if (started) nameRecognizer::acceptSharedAudio else null,
                            )
                        } else {
                            detector.setSharedSpeechAudioSink(null)
                        }
                    }
                }

                LaunchedEffect(
                    translationState.enabled,
                    translationState.source,
                    translationState.target,
                ) {
                    translator.updateSettings(translationState)
                    if (!translationState.enabled && speechListening) {
                        nameRecognizer.stop()
                        speechListening = false
                    }
                }

                var lastRecognizerLanguageCode by remember { mutableStateOf(translationState.source.modelCode) }
                LaunchedEffect(translationState.source.modelCode) {
                    val nextLanguageCode = translationState.source.modelCode
                    val changedWhileListening = lastRecognizerLanguageCode != nextLanguageCode &&
                        uiPreloadComplete &&
                        appMode == AppMode.SpeechRecognition &&
                        speechListening
                    lastRecognizerLanguageCode = nextLanguageCode
                    if (changedWhileListening) {
                        withFrameNanos { }
                        delay(140)
                        nameRecognizer.refreshRecognizerForCurrentLanguage()
                    } else if (
                        uiPreloadComplete &&
                        appMode == AppMode.SpeechRecognition &&
                        !speechListening
                    ) {
                        withFrameNanos { }
                        delay(160)
                        nameRecognizer.prepare()
                    }
                }

                DisposableEffect(alertSettings.keepScreenOn) {
                    if (alertSettings.keepScreenOn) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }

                    onDispose {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        nameRecognizer.close()
                        trainingRecorder.close()
                        translator.close()
                        detector.close()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val featureActiveMode = when {
                        appMode == AppMode.SoundDetection &&
                            featureReadyMode == AppMode.SoundDetection &&
                            (runtimeState.isAudioActive || runtimeState.isListening || alertSettings.alwaysOnEnabled) ->
                            AppMode.SoundDetection
                        appMode == AppMode.SpeechRecognition &&
                            featureReadyMode == AppMode.SpeechRecognition &&
                            speechListening -> AppMode.SpeechRecognition
                        appMode == AppMode.SoundManagement && featureReadyMode == AppMode.SoundManagement ->
                            AppMode.SoundManagement
                        else -> null
                    }
                    SoundVisualizationScreen(
                        runtimeState = runtimeState,
                        appMode = appMode,
                        featureActiveMode = featureActiveMode,
                        activeEvent = activeEvent,
                        watchedName = watchedNameDraft,
                        savedWatchedName = watchedName,
                        alertSettings = alertSettings,
                        recordingTrainingKind = recordingTrainingKind,
                        translationState = translationState,
                        speechListening = speechListening,
                        speechInputLevel = speechInputLevel,
                        eventHistory = eventHistory,
                        speechHistory = speechHistory,
                        onWatchedNameChange = { watchedNameDraft = it },
                        onWatchedNameSave = {
                            val saved = mergeWatchedNames(watchedName, watchedNameDraft)
                            watchedName = saved
                            watchedNameDraft = ""
                        },
                        onSavedWatchedNameChange = { next ->
                            val saved = serializeWatchedNames(parseWatchedNames(next))
                            watchedName = saved
                        },
                        onSettingsChange = { alertSettings = it },
                        onAlwaysOnChange = ::changeAlwaysOn,
                        appLanguage = appLanguage,
                        appColorMode = appColorMode,
                        onUiPreloadComplete = { uiPreloadComplete = true },
                        onAppLanguageChange = { appLanguage = it },
                        onAppColorModeChange = { appColorMode = it },
                        onTranslationChange = { translationState = it },
                        onClearHistory = { saveEventHistory(emptyList()) },
                        onStartStop = {
                            if (runtimeState.isListening) stopListening() else startListening()
                        },
                        onSpeechStartStop = ::toggleSpeechRecognition,
                        onEventFeedback = ::recordEventFeedback,
                        onTrainingSample = ::startTrainingRecording,
                        onResetTraining = ::resetTrainingProfile,
                        onModeChange = ::changeAppMode,
                        onReset = {
                            activeEvent = idleEvent()
                        },
                        onDemo = { kind ->
                            registerEvent(
                                detectedEvent(
                                    kind = kind,
                                    confidence = 0.97f,
                                    direction = SoundDirection.Unknown,
                                    source = "demo",
                                ),
                            )
                        },
                    )
                    ScreenFlashOverlay(
                        event = flashEvent,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        }
    }

    private fun vibrateFor(kind: AlertKind) {
        if (kind == AlertKind.Idle) return

        val pattern = when (kind.defaultSeverity()) {
            AlertSeverity.Emergency -> longArrayOf(0, 180, 70, 180, 70, 260)
            AlertSeverity.Caution -> longArrayOf(0, 120, 80, 180)
            AlertSeverity.Info -> longArrayOf(0, 70, 55, 70)
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}

@Composable
private fun Modifier.animatedPressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.972f else 1f,
        animationSpec = auraTween(AuraMotion.PressMillis, exiting = pressed),
        label = "press-scale",
    )
    val lift by animateFloatAsState(
        targetValue = if (pressed) -1.8f else 0f,
        animationSpec = auraTween(AuraMotion.PressMillis, exiting = pressed),
        label = "press-lift",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = lift
    }
}

@Composable
private fun AnimatedTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.animatedPressScale(interactionSource),
        content = content,
    )
}

private const val FEATURE_ACTIVATION_DELAY_MS = 1080
private const val MODE_START_DELAY_MS = FEATURE_ACTIVATION_DELAY_MS.toLong()
private const val SOUND_CLASSIFIER_PRELOAD_DELAY_MS = 1300L
private const val TRANSLATOR_PREPARE_DELAY_MS = 2400L
private const val ALWAYS_ON_EVENT_POLL_MS = 1200L

private fun TranslationState.outputLanguage(): TranslationLanguage =
    target

@Immutable
private data class AuraPalette(
    val ink: Color,
    val inkSoft: Color,
    val muted: Color,
    val hairline: Color,
    val panel: Color,
    val panelSoft: Color,
    val control: Color,
    val blue: Color,
    val doorbell: Color,
    val siren: Color,
    val name: Color,
    val gold: Color,
    val glassEdge: Color,
    val glassShadow: Color,
    val backgroundTop: Color,
    val backgroundMiddle: Color,
    val backgroundBottom: Color,
    val glassTop: Color,
    val glassMiddle: Color,
    val glassBottom: Color,
    val onAccent: Color,
)

private val DarkAuraPalette = AuraPalette(
    ink = Color(0xFFF7F7FB),
    inkSoft = Color(0xFFD7D9E2),
    muted = Color(0xFF9EA3B3),
    hairline = Color(0xFF424754),
    panel = Color(0xF22A2D36),
    panelSoft = Color(0xEF343844),
    control = Color(0xF03F4452),
    blue = Color(0xFF8FA2FF),
    doorbell = Color(0xFF7FA8FF),
    siren = Color(0xFFFF6F83),
    name = Color(0xFF65D8CC),
    gold = Color(0xFFEAC777),
    glassEdge = Color(0x2CFFFFFF),
    glassShadow = Color(0x99000000),
    backgroundTop = Color(0xFF101219),
    backgroundMiddle = Color(0xFF090B10),
    backgroundBottom = Color(0xFF07080C),
    glassTop = Color(0xEA3B3F4A),
    glassMiddle = Color(0xF22B2F39),
    glassBottom = Color(0xF020232B),
    onAccent = Color(0xFF080A12),
)

private val LightAuraPalette = AuraPalette(
    ink = Color(0xFF15161C),
    inkSoft = Color(0xFF505866),
    muted = Color(0xFF7E8796),
    hairline = Color(0x26111620),
    panel = Color(0xFFFFFFFF),
    panelSoft = Color(0xFFF1F4F9),
    control = Color(0xFFE8ECF4),
    blue = Color(0xFF405BE8),
    doorbell = Color(0xFF2F61D8),
    siren = Color(0xFFD23D54),
    name = Color(0xFF087D75),
    gold = Color(0xFF9A6C10),
    glassEdge = Color(0x24111620),
    glassShadow = Color(0x1D000000),
    backgroundTop = Color(0xFFFAFBFE),
    backgroundMiddle = Color(0xFFF4F6FA),
    backgroundBottom = Color(0xFFEDF1F7),
    glassTop = Color(0xFFFFFFFF),
    glassMiddle = Color(0xFFF9FAFD),
    glassBottom = Color(0xFFECEFF5),
    onAccent = Color(0xFFFFFFFF),
)

private val LocalAuraPalette = compositionLocalOf { DarkAuraPalette }

private val AppFontFamily = FontFamily(
    Font(R.font.pretendard_variable, FontWeight.Normal),
    Font(R.font.pretendard_variable, FontWeight.Medium),
    Font(R.font.pretendard_variable, FontWeight.Bold),
    Font(R.font.pretendard_variable, FontWeight.ExtraBold),
)

private val AppTypography = androidx.compose.material3.Typography().withAppFont()

private val ObservatoryCardShape = RoundedCornerShape(34.dp)
private val ObservatoryPanelShape = RoundedCornerShape(26.dp)
private val ObservatoryTileShape = RoundedCornerShape(22.dp)
private val ObservatoryPillShape = RoundedCornerShape(99.dp)

@Composable
private fun DepthLighting(
    tint: Color,
    raised: Boolean,
    modifier: Modifier = Modifier,
) {
    val topGlow = if (raised) 0.09f else 0.035f
    val lowerShadow = if (raised) 0.24f else 0.18f
    val accentGlow = if (raised) 0.07f else 0.035f

    Canvas(modifier = modifier) {
        drawCircle(
            color = Color.White.copy(alpha = topGlow),
            radius = size.width * 0.50f,
            center = Offset(size.width * 0.36f, -size.height * 0.14f),
        )
        drawCircle(
            color = tint.copy(alpha = accentGlow),
            radius = size.width * 0.44f,
            center = Offset(size.width * 0.88f, size.height * 0.18f),
        )
        drawCircle(
            color = Color.Black.copy(alpha = lowerShadow),
            radius = size.width * 0.56f,
            center = Offset(size.width * 0.48f, size.height * 1.18f),
        )
        drawLine(
            color = Color.White.copy(alpha = if (raised) 0.13f else 0.055f),
            start = Offset(size.width * 0.10f, 0f),
            end = Offset(size.width * 0.90f, 0f),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = Color.Black.copy(alpha = if (raised) 0.12f else 0.18f),
            start = Offset(size.width * 0.12f, size.height),
            end = Offset(size.width * 0.88f, size.height),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private object AuraColors {
    val Ink: Color
        @Composable get() = LocalAuraPalette.current.ink
    val InkSoft: Color
        @Composable get() = LocalAuraPalette.current.inkSoft
    val Muted: Color
        @Composable get() = LocalAuraPalette.current.muted
    val Hairline: Color
        @Composable get() = LocalAuraPalette.current.hairline
    val Panel: Color
        @Composable get() = LocalAuraPalette.current.panel
    val PanelSoft: Color
        @Composable get() = LocalAuraPalette.current.panelSoft
    val Control: Color
        @Composable get() = LocalAuraPalette.current.control
    val Blue: Color
        @Composable get() = LocalAuraPalette.current.blue
    val Doorbell: Color
        @Composable get() = LocalAuraPalette.current.doorbell
    val Siren: Color
        @Composable get() = LocalAuraPalette.current.siren
    val Name: Color
        @Composable get() = LocalAuraPalette.current.name
    val Gold: Color
        @Composable get() = LocalAuraPalette.current.gold
    val GlassEdge: Color
        @Composable get() = LocalAuraPalette.current.glassEdge
    val GlassShadow: Color
        @Composable get() = LocalAuraPalette.current.glassShadow
    val BackgroundTop: Color
        @Composable get() = LocalAuraPalette.current.backgroundTop
    val BackgroundMiddle: Color
        @Composable get() = LocalAuraPalette.current.backgroundMiddle
    val BackgroundBottom: Color
        @Composable get() = LocalAuraPalette.current.backgroundBottom
    val GlassTop: Color
        @Composable get() = LocalAuraPalette.current.glassTop
    val GlassMiddle: Color
        @Composable get() = LocalAuraPalette.current.glassMiddle
    val GlassBottom: Color
        @Composable get() = LocalAuraPalette.current.glassBottom
    val OnAccent: Color
        @Composable get() = LocalAuraPalette.current.onAccent
}

@Composable
private fun isLightAura(): Boolean = LocalAuraPalette.current == LightAuraPalette

@Composable
private fun exchangeSurfaceBrush(
    tint: Color = AuraColors.Blue,
    selected: Boolean = false,
): Brush {
    val light = isLightAura()
    val panel = AuraColors.Panel
    val panelSoft = AuraColors.PanelSoft
    return remember(light, tint, selected, panel, panelSoft) {
        if (light) {
            val tintMix = if (selected) 0.080f else 0.026f
            Brush.verticalGradient(
                listOf(
                    Color.White,
                    lerp(Color.White, tint, tintMix),
                    panelSoft,
                ),
            )
        } else {
            val baseTop = Color(0xFF34343A)
            val baseMiddle = Color(0xFF2B2B30)
            val baseBottom = Color(0xFF202127)
            val tintMix = if (selected) 0.115f else 0.045f
            Brush.verticalGradient(
                listOf(
                    lerp(baseTop, tint, tintMix),
                    lerp(baseMiddle, tint, tintMix * 0.72f),
                    baseBottom,
                ),
            )
        }
    }
}

@Composable
private fun exchangeControlColor(selected: Boolean = false, tint: Color = AuraColors.Blue): Color {
    val light = isLightAura()
    val control = AuraColors.Control
    return if (selected) {
        lerp(control, tint, if (light) 0.16f else 0.24f)
    } else {
        control
    }
}

@Composable
private fun exchangePressedColor(pressed: Boolean, tint: Color = AuraColors.Blue): Color {
    if (!pressed) return Color.Transparent
    val light = isLightAura()
    return if (light) {
        lerp(Color.Black.copy(alpha = 0.045f), tint.copy(alpha = 0.10f), 0.28f)
    } else {
        lerp(Color.White.copy(alpha = 0.070f), tint.copy(alpha = 0.13f), 0.32f)
    }
}

@Composable
private fun exchangeHighlightColor(alphaLight: Float, alphaDark: Float): Color =
    if (isLightAura()) Color.Black.copy(alpha = alphaLight) else Color.White.copy(alpha = alphaDark)

enum class AppMode {
    SoundDetection,
    SpeechRecognition,
    SoundManagement,
}

private enum class ManagementSection {
    AlertSettings,
    SoundCategories,
    Calibration,
    EventHistory,
    Test,
}

@Immutable
private data class ManagementCardOrigin(
    val centerX: Float = 0f,
    val centerY: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val visual: ManagementCardVisual? = null,
)

@Immutable
private data class ManagementOpenRequest(
    val section: ManagementSection,
    val origin: ManagementCardOrigin,
    val id: Long = System.nanoTime(),
)

private fun AppMode.icon(): ImageVector = when (this) {
    AppMode.SoundDetection -> Icons.Rounded.GraphicEq
    AppMode.SpeechRecognition -> Icons.Rounded.Mic
    AppMode.SoundManagement -> Icons.Rounded.Settings
}

@Composable
private fun AppMode.navigationTint(): Color = when (this) {
    AppMode.SoundDetection -> AuraColors.Blue
    AppMode.SpeechRecognition -> AuraColors.Name
    AppMode.SoundManagement -> AuraColors.Gold
}

private fun AppStrings.t(ko: String, en: String): String =
    if (this === KoreanAppStrings) ko else en

private fun AppStrings.recentCount(count: Int): String =
    t("\uCD5C\uADFC $count", "Recent $count")

private fun AppStrings.spokenLanguageLabel(language: TranslationLanguage): String =
    t("${languageName(language)} \uC74C\uC131", "${languageName(language)} speech")

private fun AppStrings.captionPlaceholder(source: TranslationLanguage, target: TranslationLanguage): String =
    t(
        "${languageName(source)} \uC74C\uC131\uC744 ${languageName(target)} \uC790\uB9C9\uC73C\uB85C \uD45C\uC2DC\uD569\uB2C8\uB2E4",
        "Showing ${languageName(source)} speech as ${languageName(target)} captions",
    )

private fun AppStrings.vibrationCueText(severity: AlertSeverity): String = when (severity) {
    AlertSeverity.Emergency -> t("\uAC15\uD55C \uBC18\uBCF5 \uC9C4\uB3D9", "Strong repeating vibration")
    AlertSeverity.Caution -> t("\uC9E7\uC740 \uBC18\uBCF5 \uC9C4\uB3D9", "Short repeating vibration")
    AlertSeverity.Info -> t("\uAC00\uBCBC\uC6B4 \uC9C4\uB3D9", "Light vibration")
}

private fun AppStrings.userActionText(event: DetectedSoundEvent): String = when (event.kind) {
    AlertKind.FireAlarm -> t("\uD654\uC7AC \uB610\uB294 \uC5F0\uAE30 \uACBD\uBCF4\uAC00 \uAC10\uC9C0\uB410\uC2B5\uB2C8\uB2E4. \uC8FC\uBCC0\uC744 \uC989\uC2DC \uD655\uC778\uD558\uC138\uC694.", "Fire or smoke alarm detected. Check your surroundings immediately.")
    AlertKind.Siren -> t("\uC0AC\uC774\uB80C\uC774 \uAC10\uC9C0\uB410\uC2B5\uB2C8\uB2E4. \uB3C4\uB85C\uB098 \uC678\uBD80 \uC0C1\uD669\uC744 \uD655\uC778\uD558\uC138\uC694.", "Siren detected. Check outside or toward the road.")
    AlertKind.Doorbell -> t("\uCD08\uC778\uC885\uC774 \uAC10\uC9C0\uB410\uC2B5\uB2C8\uB2E4. \uD604\uAD00\uC774\uB098 \uC778\uD130\uD3F0\uC744 \uD655\uC778\uD558\uC138\uC694.", "Doorbell detected. Check the entrance or intercom.")
    AlertKind.Knock -> t("\uB178\uD06C\uAC00 \uAC10\uC9C0\uB410\uC2B5\uB2C8\uB2E4. \uBB38 \uC8FC\uBCC0\uC744 \uD655\uC778\uD558\uC138\uC694.", "Knock detected. Check the door area.")
    AlertKind.BabyCry -> t("\uC544\uAE30 \uC6B8\uC74C\uC774 \uAC10\uC9C0\uB410\uC2B5\uB2C8\uB2E4. \uB3CC\uBD04\uC774 \uD544\uC694\uD55C\uC9C0 \uD655\uC778\uD558\uC138\uC694.", "Baby crying detected. Check whether care is needed.")
    AlertKind.WaterRunning -> t("\uBB3C \uD750\uB974\uB294 \uC18C\uB9AC\uAC00 \uAC10\uC9C0\uB410\uC2B5\uB2C8\uB2E4. \uC218\uB3C4\uB098 \uBC30\uC218\uAD6C\uB97C \uD655\uC778\uD558\uC138\uC694.", "Running water detected. Check taps or drainage.")
    AlertKind.ApplianceBeep -> t("\uAC00\uC804\uC81C\uD488 \uC54C\uB9BC\uC74C\uC774 \uAC10\uC9C0\uB410\uC2B5\uB2C8\uB2E4. \uC8FC\uBCC0 \uAE30\uAE30\uB97C \uD655\uC778\uD558\uC138\uC694.", "Appliance alert detected. Check nearby devices.")
    AlertKind.PhoneRing -> t("\uC804\uD654\uBCA8\uC774 \uAC10\uC9C0\uB410\uC2B5\uB2C8\uB2E4. \uD734\uB300\uD3F0\uC774\uB098 \uC804\uD654\uAE30\uB97C \uD655\uC778\uD558\uC138\uC694.", "Phone ring detected. Check your phone.")
    AlertKind.NameCall -> t("\uC800\uC7A5\uB41C \uC774\uB984\uC774 \uBD88\uB838\uC2B5\uB2C8\uB2E4. \uC8FC\uBCC0 \uC0AC\uB78C\uC744 \uD655\uC778\uD558\uC138\uC694.", "A saved name was called. Check the people nearby.")
    AlertKind.Idle -> t("\uC8FC\uBCC0 \uC18C\uB9AC\uB97C \uBD84\uC11D \uC911\uC785\uB2C8\uB2E4.", "Analyzing nearby sound.")
}
@Immutable
private data class SpeechRecognitionHistoryItem(
    val id: Long = System.currentTimeMillis(),
    val sourceText: String,
    val translatedText: String = "",
    val sourceLanguage: String,
    val targetLanguage: String,
)

private val calibrationKinds = listOf(
    AlertKind.Doorbell,
    AlertKind.Knock,
    AlertKind.NameCall,
    AlertKind.ApplianceBeep,
)

@Composable
private fun SoundVisualizationTheme(
    colorMode: AppColorMode,
    content: @Composable () -> Unit,
) {
    val palette = when (colorMode) {
        AppColorMode.Dark -> DarkAuraPalette
        AppColorMode.Light -> LightAuraPalette
    }

    val scheme = remember(palette, colorMode) {
        if (colorMode == AppColorMode.Light) {
            androidx.compose.material3.lightColorScheme(
                primary = palette.blue,
                secondary = palette.name,
                tertiary = palette.gold,
                background = palette.backgroundMiddle,
                surface = palette.panel,
                surfaceVariant = palette.panelSoft,
                outline = palette.hairline,
                onPrimary = palette.onAccent,
                onSecondary = palette.onAccent,
                onBackground = palette.ink,
                onSurface = palette.ink,
                onSurfaceVariant = palette.inkSoft,
            )
        } else {
            androidx.compose.material3.darkColorScheme(
                primary = palette.ink,
                secondary = palette.blue,
                tertiary = palette.name,
                background = palette.backgroundMiddle,
                surface = palette.panel,
                surfaceVariant = palette.panelSoft,
                outline = palette.hairline,
                onPrimary = palette.onAccent,
                onSecondary = palette.onAccent,
                onBackground = palette.ink,
                onSurface = palette.ink,
                onSurfaceVariant = palette.inkSoft,
            )
        }
    }

    CompositionLocalProvider(LocalAuraPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content,
        )
    }
}

private fun androidx.compose.material3.Typography.withAppFont(): androidx.compose.material3.Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = AppFontFamily),
    displayMedium = displayMedium.copy(fontFamily = AppFontFamily),
    displaySmall = displaySmall.copy(fontFamily = AppFontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = AppFontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = AppFontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = AppFontFamily),
    titleLarge = titleLarge.copy(fontFamily = AppFontFamily),
    titleMedium = titleMedium.copy(fontFamily = AppFontFamily),
    titleSmall = titleSmall.copy(fontFamily = AppFontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = AppFontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = AppFontFamily),
    bodySmall = bodySmall.copy(fontFamily = AppFontFamily),
    labelLarge = labelLarge.copy(fontFamily = AppFontFamily),
    labelMedium = labelMedium.copy(fontFamily = AppFontFamily),
    labelSmall = labelSmall.copy(fontFamily = AppFontFamily),
)

@Composable
private fun SoundVisualizationScreen(
    runtimeState: SoundRuntimeState,
    appMode: AppMode,
    featureActiveMode: AppMode?,
    activeEvent: DetectedSoundEvent,
    watchedName: String,
    savedWatchedName: String,
    alertSettings: AlertSettings,
    recordingTrainingKind: AlertKind?,
    translationState: TranslationState,
    speechListening: Boolean,
    speechInputLevel: Float,
    eventHistory: List<DetectedSoundEvent>,
    speechHistory: List<SpeechRecognitionHistoryItem>,
    appLanguage: AppLanguage,
    appColorMode: AppColorMode,
    onUiPreloadComplete: () -> Unit,
    onWatchedNameChange: (String) -> Unit,
    onWatchedNameSave: () -> Unit,
    onSavedWatchedNameChange: (String) -> Unit,
    onSettingsChange: (AlertSettings) -> Unit,
    onAlwaysOnChange: (Boolean) -> Unit,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onAppColorModeChange: (AppColorMode) -> Unit,
    onTranslationChange: (TranslationState) -> Unit,
    onClearHistory: () -> Unit,
    onStartStop: () -> Unit,
    onSpeechStartStop: () -> Unit,
    onEventFeedback: (DetectedSoundEvent, FeedbackStatus) -> Unit,
    onTrainingSample: (AlertKind) -> Unit,
    onResetTraining: (AlertKind) -> Unit,
    onModeChange: (AppMode) -> Unit,
    onReset: () -> Unit,
    onDemo: (AlertKind) -> Unit,
) {
    val pages = remember { AppMode.values().toList() }
    val selectedPageIndex = remember(pages, appMode) {
        pages.indexOf(appMode).takeIf { it >= 0 } ?: 0
    }
    val pageProgressState = animateFloatAsState(
        targetValue = selectedPageIndex.toFloat(),
        animationSpec = pageSlideSpec(),
        label = "page-capsule-progress",
    )
    var managementOpenRequest by remember { mutableStateOf<ManagementOpenRequest?>(null) }
    var nameCallPopupOpen by remember { mutableStateOf(false) }
    var pendingDemoKind by remember { mutableStateOf<AlertKind?>(null) }
    val modalPopupOpen = managementOpenRequest != null || nameCallPopupOpen
    val alarmVisible = activeEvent.kind != AlertKind.Idle && !modalPopupOpen
    val backgroundBlur by animateFloatAsState(
        targetValue = when {
            modalPopupOpen -> POPUP_BACKDROP_BLUR_DP
            alarmVisible -> ALARM_BACKDROP_BLUR_DP
            else -> 0f
        },
        animationSpec = auraTween(
            durationMillis = AuraMotion.BackdropMillis,
            exiting = !modalPopupOpen && !alarmVisible,
        ),
        label = "exchange-bg-blur",
    )
    val blurModifier = if (backgroundBlur > 0.01f) Modifier.blur(backgroundBlur.dp) else Modifier

    LaunchedEffect(Unit) {
        withFrameNanos { }
        onUiPreloadComplete()
    }

    LaunchedEffect(pendingDemoKind, managementOpenRequest) {
        val kind = pendingDemoKind ?: return@LaunchedEffect
        if (managementOpenRequest != null) return@LaunchedEffect

        withFrameNanos { }
        onDemo(kind)
        pendingDemoKind = null
    }

    fun changeModeFromBottomBar(mode: AppMode) {
        managementOpenRequest = null
        nameCallPopupOpen = false
        pendingDemoKind = null
        onModeChange(mode)
    }

    Scaffold(
        containerColor = AuraColors.BackgroundMiddle,
        contentWindowInsets = WindowInsets(0.dp),
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AuraColors.BackgroundMiddle)
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(blurModifier),
            ) {
                ExchangeBackdrop(Modifier.matchParentSize())
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 190.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LiquidCommandHeader(
                        appMode = appMode,
                        pageProgressState = pageProgressState,
                        featureActive = featureActiveMode == appMode,
                        activeEvent = activeEvent,
                        appLanguage = appLanguage,
                        appColorMode = appColorMode,
                        onAppLanguageChange = onAppLanguageChange,
                        onAppColorModeChange = onAppColorModeChange,
                    )
                    ModeContentSlider(
                        pages = pages,
                        appMode = appMode,
                        pageProgressState = pageProgressState,
                        runtimeState = runtimeState,
                        alwaysOnEnabled = alertSettings.alwaysOnEnabled,
                        activeEvent = activeEvent,
                        alertSettings = alertSettings,
                        recordingTrainingKind = recordingTrainingKind,
                        eventHistory = eventHistory,
                        watchedName = watchedName,
                        savedWatchedName = savedWatchedName,
                        translationState = translationState,
                        speechListening = speechListening,
                        speechInputLevel = speechInputLevel,
                        speechHistory = speechHistory,
                        onStartStop = onStartStop,
                        onAlwaysOnChange = onAlwaysOnChange,
                        onReset = onReset,
                        onWatchedNameChange = onWatchedNameChange,
                        onWatchedNameSave = onWatchedNameSave,
                        onSavedWatchedNameChange = onSavedWatchedNameChange,
                        onSavedNamesPopupChange = { opened ->
                            nameCallPopupOpen = opened
                        },
                        onNameCallEnabledChange = { enabled ->
                            val current = alertSettings.categorySetting(AlertKind.NameCall)
                            onSettingsChange(
                                alertSettings.copy(
                                    categorySettings = alertSettings.categorySettings.toMutableMap().apply {
                                        this[AlertKind.NameCall] = current.copy(enabled = enabled)
                                    },
                                ),
                            )
                        },
                        onTranslationChange = onTranslationChange,
                        onSpeechStartStop = onSpeechStartStop,
                        onOpenManagement = { section, origin ->
                            managementOpenRequest = ManagementOpenRequest(section = section, origin = origin)
                        },
                    )
                }
            }
            val request = managementOpenRequest
            if (request == null && pendingDemoKind == null) {
                PremiumDetectedSoundAlarm(
                    event = activeEvent,
                    onDismiss = onReset,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 22.dp)
                        .zIndex(2f),
                )
            }
            if (request != null) {
                ExchangeManagementPopupDialog(
                    request = request,
                    settings = alertSettings,
                    recordingTrainingKind = recordingTrainingKind,
                    eventHistory = eventHistory,
                    onSettingsChange = onSettingsChange,
                    onTrainingSample = onTrainingSample,
                    onResetTraining = onResetTraining,
                    onEventFeedback = onEventFeedback,
                    onClearHistory = onClearHistory,
                    onDemo = { kind ->
                        onReset()
                        pendingDemoKind = kind
                    },
                    onDismiss = { managementOpenRequest = null },
                )
            }
            ExchangeBottomBar(
                pages = pages,
                selectedMode = appMode,
                featureActiveMode = featureActiveMode,
                navigationProgressState = pageProgressState,
                onModeChange = ::changeModeFromBottomBar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(4f),
            )
        }
    }
}

@Composable
private fun ModeContentSlider(
    pages: List<AppMode>,
    appMode: AppMode,
    pageProgressState: State<Float>,
    runtimeState: SoundRuntimeState,
    alwaysOnEnabled: Boolean,
    activeEvent: DetectedSoundEvent,
    alertSettings: AlertSettings,
    recordingTrainingKind: AlertKind?,
    eventHistory: List<DetectedSoundEvent>,
    watchedName: String,
    savedWatchedName: String,
    translationState: TranslationState,
    speechListening: Boolean,
    speechInputLevel: Float,
    speechHistory: List<SpeechRecognitionHistoryItem>,
    onStartStop: () -> Unit,
    onAlwaysOnChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onWatchedNameChange: (String) -> Unit,
    onWatchedNameSave: () -> Unit,
    onSavedWatchedNameChange: (String) -> Unit,
    onSavedNamesPopupChange: (Boolean) -> Unit,
    onNameCallEnabledChange: (Boolean) -> Unit,
    onTranslationChange: (TranslationState) -> Unit,
    onSpeechStartStop: () -> Unit,
    onOpenManagement: (ManagementSection, ManagementCardOrigin) -> Unit,
) {
    var currentContentMode by remember { mutableStateOf(appMode) }
    var outgoingContentMode by remember { mutableStateOf<AppMode?>(null) }
    if (currentContentMode != appMode) {
        outgoingContentMode = currentContentMode
        currentContentMode = appMode
    }
    LaunchedEffect(currentContentMode, outgoingContentMode) {
        if (outgoingContentMode != null) {
            delay(AuraMotion.PageSlideMillis.toLong() + 40L)
            outgoingContentMode = null
        }
    }
    val renderedModes = remember(currentContentMode, outgoingContentMode) {
        listOfNotNull(outgoingContentMode, currentContentMode).distinct()
    }
    var pageWidthPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { size ->
                pageWidthPx = size.width.toFloat()
            },
    ) {
        renderedModes.forEach { mode ->
            val pageIndex = pages.indexOf(mode).takeIf { it >= 0 } ?: return@forEach
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val delta = pageIndex - pageProgressState.value
                        translationX = delta * pageWidthPx
                    }
                    .zIndex(if (mode == currentContentMode) 1f else 0f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ModePageContent(
                    mode = mode,
                    runtimeState = runtimeState,
                    alwaysOnEnabled = alwaysOnEnabled,
                    activeEvent = activeEvent,
                    alertSettings = alertSettings,
                    recordingTrainingKind = recordingTrainingKind,
                    eventHistory = eventHistory,
                    watchedName = watchedName,
                    savedWatchedName = savedWatchedName,
                    translationState = translationState,
                    speechListening = speechListening,
                    speechInputLevel = speechInputLevel,
                    speechHistory = speechHistory,
                    onStartStop = onStartStop,
                    onAlwaysOnChange = onAlwaysOnChange,
                    onReset = onReset,
                    onWatchedNameChange = onWatchedNameChange,
                    onWatchedNameSave = onWatchedNameSave,
                    onSavedWatchedNameChange = onSavedWatchedNameChange,
                    onSavedNamesPopupChange = onSavedNamesPopupChange,
                    onNameCallEnabledChange = onNameCallEnabledChange,
                    onTranslationChange = onTranslationChange,
                    onSpeechStartStop = onSpeechStartStop,
                    onOpenManagement = onOpenManagement,
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ModePageContent(
    mode: AppMode,
    runtimeState: SoundRuntimeState,
    alwaysOnEnabled: Boolean,
    activeEvent: DetectedSoundEvent,
    alertSettings: AlertSettings,
    recordingTrainingKind: AlertKind?,
    eventHistory: List<DetectedSoundEvent>,
    watchedName: String,
    savedWatchedName: String,
    translationState: TranslationState,
    speechListening: Boolean,
    speechInputLevel: Float,
    speechHistory: List<SpeechRecognitionHistoryItem>,
    onStartStop: () -> Unit,
    onAlwaysOnChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onWatchedNameChange: (String) -> Unit,
    onWatchedNameSave: () -> Unit,
    onSavedWatchedNameChange: (String) -> Unit,
    onSavedNamesPopupChange: (Boolean) -> Unit,
    onNameCallEnabledChange: (Boolean) -> Unit,
    onTranslationChange: (TranslationState) -> Unit,
    onSpeechStartStop: () -> Unit,
    onOpenManagement: (ManagementSection, ManagementCardOrigin) -> Unit,
) {
    when (mode) {
        AppMode.SoundDetection -> {
            LiquidSoundPage(
                runtimeState = runtimeState,
                alwaysOnEnabled = alwaysOnEnabled,
                activeEvent = activeEvent,
                alertSettings = alertSettings,
                eventHistory = eventHistory,
                watchedName = watchedName,
                savedWatchedName = savedWatchedName,
                onStartStop = onStartStop,
                onAlwaysOnChange = onAlwaysOnChange,
                onReset = onReset,
                onWatchedNameChange = onWatchedNameChange,
                onWatchedNameSave = onWatchedNameSave,
                onSavedWatchedNameChange = onSavedWatchedNameChange,
                onSavedNamesPopupChange = onSavedNamesPopupChange,
                onNameCallEnabledChange = onNameCallEnabledChange,
            )
        }

        AppMode.SpeechRecognition -> {
                LiquidSpeechPage(
                    translationState = translationState,
                    speechListening = speechListening,
                    speechInputLevel = speechInputLevel,
                    speechHistory = speechHistory,
                    onTranslationChange = onTranslationChange,
                onSpeechStartStop = onSpeechStartStop,
            )
        }

        AppMode.SoundManagement -> {
            LiquidManagementPage(
                alertSettings = alertSettings,
                recordingTrainingKind = recordingTrainingKind,
                eventHistory = eventHistory,
                onOpen = onOpenManagement,
            )
        }
    }
}

@Composable
private fun ExchangeBackdrop(modifier: Modifier = Modifier) {
    val top = AuraColors.BackgroundTop
    val middle = AuraColors.BackgroundMiddle
    val bottom = AuraColors.BackgroundBottom
    val brush = remember(top, middle, bottom) {
        Brush.verticalGradient(
            listOf(
                top,
                middle,
                bottom,
            ),
        )
    }
    Box(modifier = modifier.background(brush))
}

@Composable
private fun LiquidCommandHeader(
    appMode: AppMode,
    pageProgressState: State<Float>,
    featureActive: Boolean,
    activeEvent: DetectedSoundEvent,
    appLanguage: AppLanguage,
    appColorMode: AppColorMode,
    onAppLanguageChange: (AppLanguage) -> Unit,
    onAppColorModeChange: (AppColorMode) -> Unit,
) {
    val strings = LocalAppStrings.current
    val liveEvent = activeEvent.takeIf { appMode == AppMode.SoundDetection && it.kind != AlertKind.Idle }
    val accent = liveEvent?.severity?.priorityColor() ?: appMode.navigationTint()
    val headerTitleSize = if (appLanguage == AppLanguage.English) 15.sp else 19.sp
    var headerContentWidthPx by remember { mutableFloatStateOf(0f) }
    ExchangeCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        corner = 28.dp,
        tint = accent,
        selected = featureActive,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clipToBounds()
                        .onSizeChanged { size ->
                            headerContentWidthPx = size.width.toFloat()
                        },
                ) {
                    AppMode.values().forEach { mode ->
                        val modeEvent = activeEvent.takeIf {
                            mode == AppMode.SoundDetection && it.kind != AlertKind.Idle
                        }
                        val modeAccent = modeEvent?.severity?.priorityColor() ?: mode.navigationTint()
                        val modeTitle = when (mode) {
                            AppMode.SoundDetection -> modeEvent?.let { strings.alertTitle(it.kind) } ?: strings.modeTitle(mode)
                            AppMode.SpeechRecognition -> strings.t("\uC2E4\uC2DC\uAC04 \uC790\uB9C9", "Live Captions")
                            AppMode.SoundManagement -> strings.modeTitle(mode)
                        }
                        val modeFeatureActive = featureActive && mode == appMode
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val delta = mode.ordinal - pageProgressState.value
                                    val selection = (1f - abs(delta).coerceIn(0f, 1f))
                                    translationX = delta * headerContentWidthPx
                                    alpha = 0.62f + selection * 0.38f
                                    scaleX = 0.985f + selection * 0.015f
                                    scaleY = 0.985f + selection * 0.015f
                                    compositingStrategy = CompositingStrategy.ModulateAlpha
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(exchangeControlColor(modeFeatureActive, modeAccent), ObservatoryPillShape)
                                    .border(1.dp, AuraColors.GlassEdge, ObservatoryPillShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = modeEvent?.kind?.icon() ?: mode.icon(),
                                    contentDescription = null,
                                    tint = AuraColors.Ink,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Text(
                                text = modeTitle,
                                color = AuraColors.Ink,
                                fontSize = headerTitleSize,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                AppLanguageToggle(
                    language = appLanguage,
                    onLanguageChange = onAppLanguageChange,
                    modifier = Modifier.width(54.dp),
                )
                AppColorModeButton(
                    colorMode = appColorMode,
                    onColorModeChange = onAppColorModeChange,
                )
            }
        }
    }
}

@Composable
private fun LiquidSoundPage(
    runtimeState: SoundRuntimeState,
    alwaysOnEnabled: Boolean,
    activeEvent: DetectedSoundEvent,
    alertSettings: AlertSettings,
    eventHistory: List<DetectedSoundEvent>,
    watchedName: String,
    savedWatchedName: String,
    onStartStop: () -> Unit,
    onAlwaysOnChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onWatchedNameChange: (String) -> Unit,
    onWatchedNameSave: () -> Unit,
    onSavedWatchedNameChange: (String) -> Unit,
    onSavedNamesPopupChange: (Boolean) -> Unit,
    onNameCallEnabledChange: (Boolean) -> Unit,
) {
    val strings = LocalAppStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LiquidSoundStage(
            runtimeState = runtimeState,
            alwaysOnEnabled = alwaysOnEnabled,
            activeEvent = activeEvent,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LiquidActionTile(
                title = if (runtimeState.isListening) strings.t("\uAC10\uC9C0 \uC815\uC9C0", "Stop Detection") else strings.t("\uAC10\uC9C0 \uC2DC\uC791", "Start Detection"),
                subtitle = strings.t("\uC8FC\uBCC0 \uC18C\uB9AC \uBD84\uC11D", "Nearby sound analysis"),
                value = if (runtimeState.isListening) "ON" else "OFF",
                icon = if (runtimeState.isListening) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                tint = if (runtimeState.isListening) AuraColors.Siren else AuraColors.Name,
                selected = runtimeState.isListening,
                modifier = Modifier.weight(1f),
                height = 88.dp,
                onClick = onStartStop,
            )
            LiquidActionTile(
                title = strings.t("\uC0C1\uC2DC \uAC10\uC9C0", "Always-on"),
                subtitle = strings.t("\uC911\uC694 \uC18C\uB9AC \uC54C\uB9BC", "Critical alerts"),
                value = if (alertSettings.alwaysOnEnabled) "ON" else "OFF",
                icon = Icons.Rounded.Notifications,
                tint = AuraColors.Blue,
                selected = alertSettings.alwaysOnEnabled,
                modifier = Modifier.weight(1f),
                height = 88.dp,
                onClick = { onAlwaysOnChange(!alertSettings.alwaysOnEnabled) },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LiquidActionTile(
                title = strings.t("\uCD5C\uADFC \uAC10\uC9C0", "Recent Detection"),
                subtitle = eventHistory.firstOrNull()?.let { strings.alertTitle(it.kind) } ?: strings.noHistory,
                value = eventHistory.size.coerceAtMost(99).toString(),
                icon = eventHistory.firstOrNull()?.icon() ?: Icons.Rounded.Notifications,
                tint = eventHistory.firstOrNull()?.tint() ?: AuraColors.Muted,
                modifier = Modifier.weight(1f),
                height = 88.dp,
            )
            LiquidActionTile(
                title = strings.t("\uCD08\uAE30\uD654", "Reset"),
                subtitle = strings.t("\uD654\uBA74 \uC0C1\uD0DC \uCD08\uAE30\uD654", "Clear screen state"),
                value = "",
                icon = Icons.Rounded.Refresh,
                tint = AuraColors.Gold,
                modifier = Modifier.weight(1f),
                height = 88.dp,
                onClick = onReset,
            )
        }
        LiquidNameCallPanel(
            watchedName = watchedName,
            savedWatchedName = savedWatchedName,
            nameCallEnabled = alertSettings.categorySetting(AlertKind.NameCall).enabled,
            onWatchedNameChange = onWatchedNameChange,
            onWatchedNameSave = onWatchedNameSave,
            onSavedWatchedNameChange = onSavedWatchedNameChange,
            onSavedNamesPopupChange = onSavedNamesPopupChange,
            onNameCallEnabledChange = onNameCallEnabledChange,
        )
    }
}

@Composable
private fun LiquidSoundStage(
    runtimeState: SoundRuntimeState,
    alwaysOnEnabled: Boolean,
    activeEvent: DetectedSoundEvent,
) {
    val strings = LocalAppStrings.current
    val event = activeEvent.takeIf { it.kind != AlertKind.Idle }
    val kind = event?.kind ?: AlertKind.Idle
    val severity = event?.severity
    val accent = severity?.priorityColor() ?: AuraColors.Blue
    ExchangeCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (event != null) 292.dp else 268.dp),
        corner = 38.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(stageBrush(kind))
                .padding(18.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (event != null) {
                        SeveritySignalStrip(event = event)
                    } else {
                        HeaderStatusChip(
                            text = when {
                                alwaysOnEnabled -> strings.t("\uC0C1\uC2DC \uAC10\uC9C0", "Always-on")
                                runtimeState.isListening -> strings.t("\uAC10\uC9C0 \uC911", "Listening")
                                runtimeState.isAudioActive -> strings.t("\uC18C\uB9AC \uC0C1\uD0DC", "Sound Field")
                                else -> strings.t("\uC900\uBE44", "Ready")
                            },
                            color = accent,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (event != null) {
                        Text(
                            text = scoreText(event.confidence),
                            color = accent,
                            fontSize = 14.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    } else {
                        LevelMeter(runtimeState.level)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (event != null) 116.dp else 108.dp)
                        .background(AuraColors.Control.copy(alpha = 0.58f), RoundedCornerShape(34.dp))
                        .border(1.dp, AuraColors.GlassEdge, RoundedCornerShape(34.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (kind == AlertKind.Idle) {
                        LiveFrequencyWaveformGraph(
                            level = runtimeState.level,
                            spectrumBands = runtimeState.spectrumBands,
                            active = runtimeState.isAudioActive,
                            modifier = Modifier
                                .matchParentSize()
                                .padding(horizontal = 28.dp, vertical = 20.dp),
                        )
                    } else {
                        StageCanvas(
                            kind = kind,
                            level = runtimeState.level,
                            motionEnabled = runtimeState.isListening || event != null,
                            modifier = Modifier
                                .size(104.dp)
                                .align(Alignment.Center),
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text(
                        text = event?.let { strings.alertTitle(it.kind) } ?: strings.t("\uC18C\uB9AC \uC0C1\uD0DC", "Sound Field"),
                        color = AuraColors.Ink,
                        fontSize = if (event != null) 31.sp else 28.sp,
                        lineHeight = if (event != null) 35.sp else 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            event != null -> strings.userActionText(event)
                            alwaysOnEnabled -> strings.t("\uC571\uC774 \uB2EB\uD600 \uC788\uC5B4\uB3C4 \uC911\uC694\uD55C \uC18C\uB9AC\uB97C \uC54C\uB9BD\uB2C8\uB2E4", "Critical sounds can alert you even when the app is closed")
                            runtimeState.isListening -> strings.t("\uC54C\uB9BC\uC744 \uC704\uD55C \uC18C\uB9AC \uAC10\uC9C0\uB97C \uC2E4\uD589 \uC911\uC785\uB2C8\uB2E4", "Alert detection is running")
                            runtimeState.isAudioActive -> strings.t("\uC8FC\uD30C\uC218 \uD30C\uD615\uC744 \uC2E4\uC2DC\uAC04\uC73C\uB85C \uBD84\uC11D \uC911\uC785\uB2C8\uB2E4", "Analyzing the live frequency waveform")
                            else -> strings.t("\uCD08\uC778\uC885, \uC0AC\uC774\uB80C, \uACBD\uBCF4\uC74C\uC744 \uAC10\uC9C0\uD558\uB824\uBA74 \uC2DC\uC791\uD558\uC138\uC694", "Start detection to identify doorbells, sirens, and alarms")
                        },
                        color = AuraColors.InkSoft.copy(alpha = if (event != null) 0.90f else 0.76f),
                        fontSize = if (event != null) 14.sp else 13.sp,
                        lineHeight = if (event != null) 19.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (event != null) {
                    EventCertaintyMeter(event = event)
                }
            }
        }
    }
}

@Composable
private fun SeveritySignalStrip(event: DetectedSoundEvent) {
    val strings = LocalAppStrings.current
    val severityColor = event.severity.priorityColor()
    Row(
        modifier = Modifier
            .background(severityColor.copy(alpha = 0.16f), ObservatoryPillShape)
            .border(1.dp, severityColor.copy(alpha = 0.34f), ObservatoryPillShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            imageVector = event.severity.signalIcon(),
            contentDescription = null,
            tint = severityColor,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = "${strings.severityLabel(event.severity)} - ${strings.vibrationCueText(event.severity)}",
            color = AuraColors.Ink,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EventCertaintyMeter(event: DetectedSoundEvent) {
    val strings = LocalAppStrings.current
    val severityColor = event.severity.priorityColor()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = strings.confidenceLabel(event.confidence),
                color = AuraColors.InkSoft,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = scoreText(event.confidence),
                color = severityColor,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(AuraColors.Control, ObservatoryPillShape)
                .border(1.dp, AuraColors.GlassEdge, ObservatoryPillShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(event.confidence.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(severityColor, ObservatoryPillShape),
            )
        }
    }
}

@Composable
private fun AlertSeverity.priorityColor(): Color = when (this) {
    AlertSeverity.Emergency -> AuraColors.Siren
    AlertSeverity.Caution -> AuraColors.Gold
    AlertSeverity.Info -> AuraColors.Blue
}

private fun AlertSeverity.signalIcon(): ImageVector = when (this) {
    AlertSeverity.Emergency,
    AlertSeverity.Caution -> Icons.Rounded.Warning
    AlertSeverity.Info -> Icons.Rounded.Notifications
}

@Composable
private fun LiquidNameCallPanel(
    watchedName: String,
    savedWatchedName: String,
    nameCallEnabled: Boolean,
    onWatchedNameChange: (String) -> Unit,
    onWatchedNameSave: () -> Unit,
    onSavedWatchedNameChange: (String) -> Unit,
    onSavedNamesPopupChange: (Boolean) -> Unit,
    onNameCallEnabledChange: (Boolean) -> Unit,
) {
    val strings = LocalAppStrings.current
    var savedNamesOpen by remember { mutableStateOf(false) }
    val savedNames = remember(savedWatchedName) { parseWatchedNames(savedWatchedName) }
    val saveInteractionSource = remember { MutableInteractionSource() }
    val menuInteractionSource = remember { MutableInteractionSource() }
    val hasUnsavedName = watchedName.isNotBlank() &&
        parseWatchedNames(mergeWatchedNames(savedWatchedName, watchedName)).size > savedNames.size

    LaunchedEffect(savedNamesOpen) {
        onSavedNamesPopupChange(savedNamesOpen)
    }
    DisposableEffect(Unit) {
        onDispose { onSavedNamesPopupChange(false) }
    }

    ExchangeCard(modifier = Modifier.fillMaxWidth(), corner = 34.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(icon = Icons.Rounded.Person, tint = AuraColors.Name)
                Column(modifier = Modifier.weight(1f)) {
                    Text(strings.t("\uC774\uB984 \uD638\uCD9C", "Name Call"), color = AuraColors.Ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = if (savedNames.isEmpty()) strings.t("\uC800\uC7A5\uD55C \uC774\uB984\uC744 \uBD80\uB974\uBA74 \uC54C\uB9BD\uB2C8\uB2E4", "Saved names trigger alerts") else savedNames.joinToString(", "),
                        color = AuraColors.Muted,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Switch(
                    checked = nameCallEnabled,
                    onCheckedChange = onNameCallEnabledChange,
                    modifier = Modifier.semantics {
                        contentDescription = strings.t("\uC774\uB984 \uD638\uCD9C", "Name call")
                        stateDescription = if (nameCallEnabled) {
                            strings.t("\uCF1C\uC9D0", "On")
                        } else {
                            strings.t("\uAEBC\uC9D0", "Off")
                        }
                    },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = watchedName,
                    onValueChange = onWatchedNameChange,
                    enabled = nameCallEnabled,
                    label = { Text(strings.watchedName) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onWatchedNameSave,
                    interactionSource = saveInteractionSource,
                    enabled = hasUnsavedName,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraColors.Name,
                        contentColor = AuraColors.OnAccent,
                        disabledContainerColor = AuraColors.Control,
                        disabledContentColor = AuraColors.Muted,
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .animatedPressScale(saveInteractionSource),
                ) {
                    Text(strings.t("\uC800\uC7A5", "Save"), fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .animatedPressScale(menuInteractionSource)
                        .background(AuraColors.Control, RoundedCornerShape(20.dp))
                        .border(1.dp, AuraColors.GlassEdge, RoundedCornerShape(20.dp))
                        .clickable(
                            interactionSource = menuInteractionSource,
                            indication = null,
                            role = Role.Button,
                            onClick = { savedNamesOpen = true },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Menu,
                        contentDescription = strings.t("\uC800\uC7A5\uB41C \uC774\uB984", "Saved names"),
                        tint = AuraColors.Ink,
                    )
                }
            }
        }
    }

    if (savedNamesOpen) {
        SavedNamesDialog(
            savedName = savedWatchedName,
            onSavedNameChange = onSavedWatchedNameChange,
            onDismiss = { savedNamesOpen = false },
        )
    }
}

@Composable
private fun LiquidSpeechPage(
    translationState: TranslationState,
    speechListening: Boolean,
    speechInputLevel: Float,
    speechHistory: List<SpeechRecognitionHistoryItem>,
    onTranslationChange: (TranslationState) -> Unit,
    onSpeechStartStop: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LiquidSpeechStage(
            translationState = translationState,
            speechListening = speechListening,
            speechInputLevel = speechInputLevel,
            speechHistory = speechHistory,
        )
        LiquidSpeechRoutePanel(
            translationState = translationState,
            speechListening = speechListening,
            speechHistory = speechHistory,
            onTranslationChange = onTranslationChange,
            onSpeechStartStop = onSpeechStartStop,
        )
    }
}

@Composable
private fun LiquidSpeechStage(
    translationState: TranslationState,
    speechListening: Boolean,
    speechInputLevel: Float,
    speechHistory: List<SpeechRecognitionHistoryItem>,
) {
    val strings = LocalAppStrings.current
    val displayText = translationState.translatedText.ifBlank {
        translationState.sourceText.ifBlank {
            strings.captionPlaceholder(translationState.source, translationState.target)
        }
    }
    ExchangeCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(252.dp),
        corner = 38.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(exchangeHeroBrush(AuraColors.Name))
                .padding(18.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(AuraColors.Control.copy(alpha = 0.72f), ObservatoryPillShape)
                            .border(1.dp, AuraColors.GlassEdge, ObservatoryPillShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LiveFrequencyWaveformGraph(
                            level = speechInputLevel,
                            spectrumBands = emptyList(),
                            active = speechListening,
                            compact = true,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    Text(
                        text = strings.t("\uC2E4\uC2DC\uAC04 \uC790\uB9C9", "Live Captions"),
                        color = AuraColors.Ink,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    HeaderStatusChip(
                        text = if (speechListening) strings.t("\uB4E3\uB294 \uC911", "LISTENING") else strings.t("\uC900\uBE44", "READY"),
                        color = AuraColors.Name,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = displayText,
                            color = AuraColors.Ink,
                            fontSize = 23.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (translationState.sourceText.isNotBlank() && translationState.translatedText.isNotBlank()) {
                            Text(
                                text = translationState.sourceText,
                                color = AuraColors.Muted,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CaptionLanguageBadge(
                        label = strings.t("\uC785\uB825", "Input"),
                        language = strings.languageName(translationState.source),
                        color = AuraColors.Name,
                    )
                    CaptionLanguageBadge(
                        label = strings.t("\uBC88\uC5ED", "Output"),
                        language = strings.languageName(translationState.target),
                        color = AuraColors.Blue,
                    )
                    Text(
                        text = strings.recentCount(speechHistory.size),
                        color = AuraColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptionLanguageBadge(
    label: String,
    language: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .background(AuraColors.Control, ObservatoryPillShape)
            .border(1.dp, color.copy(alpha = 0.22f), ObservatoryPillShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            color = color.copy(alpha = 0.94f),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
        Text(
            text = language,
            color = AuraColors.Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LiquidSpeechRoutePanel(
    translationState: TranslationState,
    speechListening: Boolean,
    speechHistory: List<SpeechRecognitionHistoryItem>,
    onTranslationChange: (TranslationState) -> Unit,
    onSpeechStartStop: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val languageLabelWidth = if (strings === KoreanAppStrings) 72.dp else 58.dp
    ExchangeCard(modifier = Modifier.fillMaxWidth(), corner = 34.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AuraColors.Control, ObservatoryPillShape)
                    .border(1.dp, AuraColors.GlassEdge, ObservatoryPillShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.t("\uBC88\uC5ED \uBC29\uD5A5", "Translation Direction"),
                    color = AuraColors.Muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${strings.languageName(translationState.source)} -> ${strings.languageName(translationState.target)}",
                    color = AuraColors.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LiquidActionTile(
                    title = if (speechListening) strings.t("\uC74C\uC131 \uC778\uC2DD \uC815\uC9C0", "Stop Speech") else strings.t("\uC74C\uC131 \uC778\uC2DD \uC2DC\uC791", "Start Speech"),
                    subtitle = strings.spokenLanguageLabel(translationState.source),
                    value = if (speechListening) "ON" else "OFF",
                    icon = if (speechListening) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                    tint = if (speechListening) AuraColors.Siren else AuraColors.Name,
                    selected = speechListening,
                    modifier = Modifier.weight(1f),
                    height = 96.dp,
                    onClick = onSpeechStartStop,
                )
                LiquidActionTile(
                    title = strings.t("\uCD5C\uADFC", "Recent"),
                    subtitle = speechHistory.firstOrNull()?.translatedText?.ifBlank { speechHistory.firstOrNull()?.sourceText } ?: strings.noHistory,
                    value = speechHistory.size.coerceAtMost(99).toString(),
                    icon = Icons.Rounded.Notifications,
                    tint = AuraColors.Gold,
                    modifier = Modifier.weight(1f),
                    height = 96.dp,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(strings.t("\uC785\uB825", "Input"), color = AuraColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(languageLabelWidth), maxLines = 1, overflow = TextOverflow.Ellipsis)
                ExchangeSmallButton(strings.t(strings.languageName("ko"), "KO"), translationState.source.modelCode == "ko") {
                    onTranslationChange(translationState.copy(source = koreanTranslationLanguage(), sourceText = "", translatedText = "", autoDetectSource = false))
                }
                ExchangeSmallButton(strings.t(strings.languageName("en"), "EN"), translationState.source.modelCode == "en") {
                    onTranslationChange(translationState.copy(source = englishTranslationLanguage(), sourceText = "", translatedText = "", autoDetectSource = false))
                }
                ExchangeSmallButton(strings.t(strings.languageName("ja"), "JA"), translationState.source.modelCode == "ja") {
                    onTranslationChange(translationState.copy(source = japaneseTranslationLanguage(), sourceText = "", translatedText = "", autoDetectSource = false))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(strings.t("\uBC88\uC5ED", "Target"), color = AuraColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(languageLabelWidth), maxLines = 1, overflow = TextOverflow.Ellipsis)
                ExchangeSmallButton(strings.t(strings.languageName("ko"), "KO"), translationState.target.modelCode == "ko") {
                    onTranslationChange(translationState.copy(target = koreanTranslationLanguage(), translatedText = "", autoDetectSource = false))
                }
                ExchangeSmallButton(strings.t(strings.languageName("en"), "EN"), translationState.target.modelCode == "en") {
                    onTranslationChange(translationState.copy(target = englishTranslationLanguage(), translatedText = "", autoDetectSource = false))
                }
            }
        }
    }
}

@Composable
private fun LiquidManagementPage(
    alertSettings: AlertSettings,
    recordingTrainingKind: AlertKind?,
    eventHistory: List<DetectedSoundEvent>,
    onOpen: (ManagementSection, ManagementCardOrigin) -> Unit,
) {
    val strings = LocalAppStrings.current
    val enabledCount = detectableAlertKinds.count { alertSettings.categorySetting(it).enabled }
    val sampleCount = calibrationKinds.sumOf { alertSettings.customProfile(it).sampleCount }
    val cards = listOf(
        ManagementSection.AlertSettings to ManagementCardCopy(strings.alertSettings, strings.t("\uC9C4\uB3D9, \uD654\uBA74, \uAC1C\uC778\uC815\uBCF4", "Alerts & privacy"), "3/4", AuraColors.Blue, Icons.Rounded.Notifications),
        ManagementSection.SoundCategories to ManagementCardCopy(strings.t(strings.soundCategories, "Sound Types"), strings.t("\uAC10\uC9C0\uD560 \uC18C\uB9AC \uC120\uD0DD", "Choose sounds"), "$enabledCount/${detectableAlertKinds.size}", AuraColors.Name, Icons.Rounded.GraphicEq),
        ManagementSection.Calibration to ManagementCardCopy(strings.t(strings.calibration, "Accuracy"), if (recordingTrainingKind != null) strings.recording else strings.t("\uAC1C\uC778 \uB9DE\uCDA4 \uBCF4\uC815", "Personal tuning"), sampleCount.toString(), AuraColors.Gold, Icons.Rounded.Settings),
        ManagementSection.EventHistory to ManagementCardCopy(strings.eventHistory, strings.t("\uCD5C\uADFC \uAC10\uC9C0 \uD0C0\uC784\uB77C\uC778", "Recent detections"), eventHistory.size.coerceAtMost(99).toString(), AuraColors.Doorbell, Icons.Rounded.Notifications),
        ManagementSection.Test to ManagementCardCopy(strings.test, strings.t("\uC54C\uB9BC \uD654\uBA74 \uBBF8\uB9AC\uBCF4\uAE30", "Preview alert visuals"), "9", AuraColors.Siren, Icons.Rounded.PlayArrow),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            LiquidActionTile(strings.t("\uC18C\uB9AC", "Sounds"), strings.t("\uD65C\uC131", "Active"), "$enabledCount/${detectableAlertKinds.size}", Icons.Rounded.Notifications, AuraColors.Blue, modifier = Modifier.weight(1f))
            LiquidActionTile(strings.t("\uBCF4\uC815", "Tuning"), strings.t("\uC0D8\uD50C", "Samples"), sampleCount.toString(), Icons.Rounded.Settings, AuraColors.Gold, modifier = Modifier.weight(1f))
            LiquidActionTile(strings.t("\uAE30\uB85D", "History"), strings.t("\uC774\uBCA4\uD2B8", "Events"), eventHistory.size.coerceAtMost(99).toString(), Icons.Rounded.GraphicEq, AuraColors.Name, modifier = Modifier.weight(1f))
        }
        ExchangeCard(modifier = Modifier.fillMaxWidth(), corner = 36.dp) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(strings.t("\uAD00\uB9AC \uCE74\uB4DC", "Controls"), color = AuraColors.Ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    LiquidManagementTile(cards[0].first, cards[0].second, Modifier.weight(1f), onOpen)
                    LiquidManagementTile(cards[1].first, cards[1].second, Modifier.weight(1f), onOpen)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    LiquidManagementTile(cards[2].first, cards[2].second, Modifier.weight(1f), onOpen)
                    LiquidManagementTile(cards[3].first, cards[3].second, Modifier.weight(1f), onOpen)
                }
                LiquidManagementTile(cards[4].first, cards[4].second, Modifier.fillMaxWidth(), onOpen)
            }
        }
    }
}

@Composable
private fun LiquidManagementTile(
    section: ManagementSection,
    copy: ManagementCardCopy,
    modifier: Modifier = Modifier,
    onOpen: (ManagementSection, ManagementCardOrigin) -> Unit,
) {
    var origin by remember { mutableStateOf(ManagementCardOrigin()) }
    LiquidActionTile(
        title = copy.title,
        subtitle = copy.subtitle,
        value = copy.value,
        icon = copy.icon,
        tint = copy.tint,
        modifier = modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size
            val nextOrigin = ManagementCardOrigin(
                centerX = position.x + size.width / 2f,
                centerY = position.y + size.height / 2f,
                width = size.width.toFloat(),
                height = size.height.toFloat(),
                visual = ManagementCardVisual(copy, section.ordinal),
            )
            if (
                abs(origin.centerX - nextOrigin.centerX) > 0.5f ||
                abs(origin.centerY - nextOrigin.centerY) > 0.5f ||
                abs(origin.width - nextOrigin.width) > 0.5f ||
                abs(origin.height - nextOrigin.height) > 0.5f ||
                origin.visual != nextOrigin.visual
            ) {
                origin = nextOrigin
            }
        },
        onClick = { onOpen(section, origin) },
    )
}

@Composable
private fun LiquidActionTile(
    title: String,
    subtitle: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    height: Dp = 118.dp,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tileShape = RoundedCornerShape(28.dp)
    val pressedOverlay by animateColorAsState(
        targetValue = exchangePressedColor(pressed, tint),
        animationSpec = auraTween(AuraMotion.ControlMillis, exiting = !pressed),
        label = "liquid-action-press",
    )
    Box(
        modifier = modifier
            .height(height)
            .then(if (onClick != null) Modifier.animatedPressScale(interactionSource) else Modifier)
            .clip(tileShape)
            .background(exchangeSurfaceBrush(tint = tint, selected = selected))
            .background(pressedOverlay)
            .border(1.dp, if (selected) tint.copy(alpha = 0.24f) else AuraColors.GlassEdge, tileShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.align(Alignment.TopStart).size(23.dp))
        if (value.isNotBlank()) {
            Text(
                text = value,
                color = AuraColors.Ink,
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = AuraColors.Ink, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = AuraColors.Muted, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
@Composable
private fun ExchangeManagementPopupDialog(
    request: ManagementOpenRequest,
    settings: AlertSettings,
    recordingTrainingKind: AlertKind?,
    eventHistory: List<DetectedSoundEvent>,
    onSettingsChange: (AlertSettings) -> Unit,
    onTrainingSample: (AlertKind) -> Unit,
    onResetTraining: (AlertKind) -> Unit,
    onEventFeedback: (DetectedSoundEvent, FeedbackStatus) -> Unit,
    onClearHistory: () -> Unit,
    onDemo: (AlertKind) -> Unit,
    onDismiss: () -> Unit,
) {
    val section = request.section
    val strings = LocalAppStrings.current
    val listState = rememberLazyListState()
    var visible by remember(request.id) { mutableStateOf(false) }
    var closing by remember(request.id) { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = auraTween(
            durationMillis = if (visible) AuraMotion.PopupEnterMillis else AuraMotion.PopupExitMillis,
            exiting = !visible,
        ),
        label = "management-natural-popup",
    )
    val overlayInteraction = remember { MutableInteractionSource() }
    val popupInteraction = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val popupCorner by animateDpAsState(
        targetValue = if (visible) 32.dp else 24.dp,
        animationSpec = auraTween(
            durationMillis = if (visible) AuraMotion.PopupEnterMillis else AuraMotion.PopupExitMillis,
            exiting = !visible,
        ),
        label = "management-popup-corner",
    )
    val popupLift by animateDpAsState(
        targetValue = if (visible) 0.dp else 22.dp,
        animationSpec = auraTween(
            durationMillis = if (visible) AuraMotion.PopupEnterMillis else AuraMotion.PopupExitMillis,
            exiting = !visible,
        ),
        label = "management-popup-lift",
    )

    fun close() {
        if (closing) return
        closing = true
        visible = false
    }

    LaunchedEffect(request.id) {
        withFrameNanos { }
        visible = true
    }
    LaunchedEffect(closing, visible) {
        if (closing && !visible) {
            var frameCount = 0
            while (progress > 0.006f && frameCount < 24) {
                withFrameNanos { }
                frameCount += 1
            }
            onDismiss()
        }
    }
    val visualProgress = progress.coerceIn(0f, 1f)
    val popupLiftPx = with(density) { popupLift.toPx() }
    val popupScale = 0.945f + visualProgress * 0.055f + sin(visualProgress * Math.PI).toFloat() * 0.008f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(3f)
            .clickable(
                interactionSource = overlayInteraction,
                indication = null,
                onClick = ::close,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
            Surface(
                shape = RoundedCornerShape(popupCorner),
                color = Color.Transparent,
                border = BorderStroke(1.dp, section.tint().copy(alpha = 0.28f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PLATFORM_POPUP_MIN_HEIGHT, max = PLATFORM_POPUP_MAX_HEIGHT)
                    .graphicsLayer {
                        alpha = visualProgress
                        scaleX = popupScale
                        scaleY = popupScale
                        translationY = popupLiftPx
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                    .clickable(
                        interactionSource = popupInteraction,
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(liquidGlassBrush(tint = section.tint().copy(alpha = 0.08f))),
                ) {
                    LiquidPopupBorder(
                        tint = section.tint(),
                        progress = visualProgress,
                        modifier = Modifier.matchParentSize(),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconTile(
                                icon = section.icon(),
                                tint = section.tint(),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = section.title(strings),
                                    color = AuraColors.Ink,
                                    fontSize = 24.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = section.subtitle(strings),
                                    color = AuraColors.InkSoft,
                                    fontSize = 12.sp,
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (section == ManagementSection.Test) {
                            ScrollEdgeBlurFrame(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 430.dp),
                                backgroundColor = AuraColors.Panel,
                                edgeHeight = 34.dp,
                                edgeOutset = 6.dp,
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 430.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    item("test-demo-grid") {
                                        ScrollEdgeFadingItem(
                                            listState = listState,
                                            itemKey = "test-demo-grid",
                                        ) {
                                            DemoContent { kind ->
                                                onDemo(kind)
                                                close()
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            ScrollEdgeBlurFrame(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 430.dp),
                                backgroundColor = AuraColors.Panel,
                                edgeHeight = 34.dp,
                                edgeOutset = 6.dp,
                            ) {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 430.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    item(section.name) {
                                        ScrollEdgeFadingItem(
                                            listState = listState,
                                            itemKey = section.name,
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                when (section) {
                                                    ManagementSection.AlertSettings -> AlertSettingsContent(
                                                        settings = settings,
                                                        onSettingsChange = onSettingsChange,
                                                    )
                                                    ManagementSection.SoundCategories -> SoundCategoryContent(
                                                        settings = settings,
                                                        onSettingsChange = onSettingsChange,
                                                    )
                                                    ManagementSection.Calibration -> PersonalizationContent(
                                                        settings = settings,
                                                        recordingKind = recordingTrainingKind,
                                                        onTrainingSample = onTrainingSample,
                                                        onResetTraining = onResetTraining,
                                                    )
                                                    ManagementSection.EventHistory -> EventHistoryContent(
                                                        eventHistory = eventHistory,
                                                        onEventFeedback = onEventFeedback,
                                                        onClearHistory = onClearHistory,
                                                    )
                                                    ManagementSection.Test -> Unit
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        AnimatedTextButton(
                            onClick = ::close,
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(strings.close, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
}

@Composable
private fun ExchangeBottomBar(
    pages: List<AppMode>,
    selectedMode: AppMode,
    featureActiveMode: AppMode?,
    navigationProgressState: State<Float>,
    onModeChange: (AppMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(30.dp)
    val selectorShape = RoundedCornerShape(24.dp)
    val accent = selectedMode.navigationTint()
    val density = LocalDensity.current
    val light = isLightAura()
    val horizontalPadding = 10.dp
    var barWidthPx by remember { mutableIntStateOf(0) }
    val horizontalPaddingPx = with(density) { horizontalPadding.toPx() }
    val itemWidthPx = remember(barWidthPx, horizontalPaddingPx, pages.size) {
        if (pages.isEmpty() || barWidthPx <= 0) {
            0f
        } else {
            ((barWidthPx - horizontalPaddingPx * 2f) / pages.size.toFloat()).coerceAtLeast(0f)
        }
    }
    val itemWidthDp = with(density) { itemWidthPx.toDp() }

    val panel = AuraColors.Panel
    val panelSoft = AuraColors.PanelSoft
    val surfaceBrush = remember(accent, panel, panelSoft) {
        Brush.verticalGradient(
            listOf(
                panel.copy(alpha = 0.98f),
                lerp(panel, accent, 0.035f).copy(alpha = 0.98f),
                panelSoft.copy(alpha = 0.95f),
            ),
        )
    }
    val selectorBrush = remember(accent, panel, panelSoft, light) {
        Brush.verticalGradient(
            listOf(
                lerp(Color.White, accent, 0.11f).copy(alpha = if (light) 0.92f else 0.16f),
                lerp(panel, accent, 0.12f).copy(alpha = 0.92f),
                lerp(panelSoft, accent, 0.075f).copy(alpha = 0.96f),
            ),
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .background(surfaceBrush, barShape)
                .border(1.dp, AuraColors.GlassEdge, barShape)
                .clip(barShape)
                .onSizeChanged { size ->
                    barWidthPx = size.width
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(itemWidthDp)
                    .height(60.dp)
                    .graphicsLayer {
                        val navigationProgress = navigationProgressState.value
                        translationX = horizontalPaddingPx + itemWidthPx * navigationProgress
                    }
                    .padding(horizontal = 5.dp, vertical = 7.dp)
                    .clip(selectorShape)
                    .background(selectorBrush, selectorShape)
                    .border(1.dp, accent.copy(alpha = if (light) 0.18f else 0.24f), selectorShape),
            )
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = horizontalPadding),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.forEach { mode ->
                    ExchangeNavItem(
                        mode = mode,
                        selected = selectedMode == mode,
                        featureActive = featureActiveMode == mode,
                        navigationProgress = navigationProgressState.value,
                        onModeChange = onModeChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExchangeNavItem(
    mode: AppMode,
    selected: Boolean,
    featureActive: Boolean,
    navigationProgress: Float,
    onModeChange: (AppMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val selectionProgress = (1f - abs(mode.ordinal - navigationProgress).coerceIn(0f, 1f))
    val color = lerp(AuraColors.Muted, AuraColors.Ink, selectionProgress)
    val iconScale = 0.98f + selectionProgress * 0.10f
    val indicatorProgress = if (featureActive) 1f else 0f
    val contentLift = -3.5f * selectionProgress
    val modeLabel = strings.modeTitle(mode)
    Column(
        modifier = modifier
            .semantics {
                contentDescription = modeLabel
                stateDescription = when {
                    selected && featureActive -> strings.t("\uC120\uD0DD\uB428, \uD65C\uC131", "Selected, active")
                    selected -> strings.t("\uC120\uD0DD\uB428", "Selected")
                    featureActive -> strings.t("\uD65C\uC131", "Active")
                    else -> strings.t("\uC120\uD0DD \uC548 \uB428", "Not selected")
                }
            }
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = { onModeChange(mode) },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationY = contentLift
            },
        ) {
            Icon(
                mode.icon(),
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-2).dp)
                    .size(6.dp)
                    .graphicsLayer {
                        alpha = indicatorProgress
                        scaleX = indicatorProgress
                        scaleY = indicatorProgress
                    }
                    .background(mode.navigationTint(), ObservatoryPillShape),
            )
        }
        Text(
            text = modeLabel,
            color = color,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                translationY = contentLift
            },
        )
    }
}

@Composable
private fun ExchangeCard(
    modifier: Modifier = Modifier,
    corner: Dp = 28.dp,
    tint: Color = AuraColors.Blue,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .shadow(if (selected) 12.dp else 4.dp, shape, clip = false)
            .clip(shape)
            .background(exchangeSurfaceBrush(tint = tint, selected = selected))
            .border(1.dp, if (selected) tint.copy(alpha = 0.26f) else AuraColors.GlassEdge, shape),
    ) {
        content()
    }
}
@Composable
private fun ExchangeSmallButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val contentColor by animateColorAsState(
        targetValue = if (selected) AuraColors.Blue else AuraColors.Muted,
        animationSpec = auraTween(AuraMotion.ControlMillis),
        label = "exchange-small-button-content",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) AuraColors.Blue.copy(alpha = 0.14f) else AuraColors.Control,
        animationSpec = auraTween(AuraMotion.ControlMillis),
        label = "exchange-small-button-container",
    )
    Text(
        text = text,
        color = contentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .animatedPressScale(interactionSource)
            .heightIn(min = 48.dp)
            .background(containerColor, ObservatoryPillShape)
            .border(
                1.dp,
                if (selected) AuraColors.Blue.copy(alpha = 0.35f) else AuraColors.GlassEdge,
                ObservatoryPillShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    if (!selected) onClick()
                },
            )
            .padding(horizontal = 13.dp, vertical = 14.dp),
    )
}
@Composable
private fun exchangeHeroBrush(accent: Color): Brush =
    AuraColors.Panel.let { base ->
        remember(base, accent) {
            Brush.verticalGradient(colors = listOf(lerp(base, accent, 0.025f), lerp(base, accent, 0.025f)))
        }
    }

@Composable
private fun ScrollEdgeFadingItem(
    listState: LazyListState,
    itemKey: Any,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val fadeDistancePx = with(LocalDensity.current) { 96.dp.toPx() }
    val targetAlpha by remember(listState, itemKey, fadeDistancePx) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount <= visibleItems.size) {
                1f
            } else {
                val item = visibleItems.firstOrNull { it.key == itemKey }
                if (item == null) {
                    0.22f
                } else {
                    val itemCenter = item.offset + item.size / 2f
                    val topDistance = itemCenter - layoutInfo.viewportStartOffset
                    val bottomDistance = layoutInfo.viewportEndOffset - itemCenter
                    minOf(
                        (topDistance / fadeDistancePx).coerceIn(0.22f, 1f),
                        (bottomDistance / fadeDistancePx).coerceIn(0.22f, 1f),
                    )
                }
            }
        }
    }
    val alpha = targetAlpha
    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
    ) {
        content()
    }
}

@Composable
private fun ScrollEdgeBlurFrame(
    modifier: Modifier = Modifier,
    backgroundColor: Color = AuraColors.BackgroundMiddle,
    edgeHeight: Dp = 58.dp,
    edgeOutset: Dp = 12.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.clipToBounds()) {
        content()
        ScrollEdgeBlurOverlay(
            backgroundColor = backgroundColor,
            edgeHeight = edgeHeight,
            edgeOutset = edgeOutset,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun ScrollEdgeBlurOverlay(
    backgroundColor: Color,
    edgeHeight: Dp,
    edgeOutset: Dp,
    modifier: Modifier = Modifier,
) {
    val glassTop = AuraColors.GlassTop
    val glassMiddle = AuraColors.GlassMiddle
    val glassBottom = AuraColors.GlassBottom
    val edgeLine = AuraColors.GlassEdge
    val ambient = AuraColors.BackgroundMiddle
    Box(modifier = modifier) {
        ScrollEdgeMist(
            top = true,
            backgroundColor = backgroundColor,
            glassTop = glassTop,
            glassMiddle = glassMiddle,
            glassBottom = glassBottom,
            ambient = ambient,
            edgeLine = edgeLine,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -edgeOutset)
                .fillMaxWidth()
                .height(edgeHeight),
        )
        ScrollEdgeMist(
            top = false,
            backgroundColor = backgroundColor,
            glassTop = glassTop,
            glassMiddle = glassMiddle,
            glassBottom = glassBottom,
            ambient = ambient,
            edgeLine = edgeLine,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = edgeOutset)
                .fillMaxWidth()
                .height(edgeHeight),
        )
    }
}

@Composable
private fun ScrollEdgeMist(
    top: Boolean,
    backgroundColor: Color,
    glassTop: Color,
    glassMiddle: Color,
    glassBottom: Color,
    ambient: Color,
    edgeLine: Color,
    modifier: Modifier = Modifier,
) {
    val mistBrush = if (top) {
        remember(backgroundColor, glassTop, glassMiddle, ambient) {
            Brush.verticalGradient(
                0.00f to backgroundColor.copy(alpha = 0.98f),
                0.22f to glassTop.copy(alpha = 0.82f),
                0.52f to glassMiddle.copy(alpha = 0.44f),
                0.78f to ambient.copy(alpha = 0.12f),
                1.00f to Color.Transparent,
            )
        }
    } else {
        remember(backgroundColor, glassMiddle, glassBottom, ambient) {
            Brush.verticalGradient(
                0.00f to Color.Transparent,
                0.22f to ambient.copy(alpha = 0.10f),
                0.50f to glassMiddle.copy(alpha = 0.40f),
                0.78f to glassBottom.copy(alpha = 0.76f),
                1.00f to backgroundColor.copy(alpha = 0.98f),
            )
        }
    }
    val sheenBrush = remember(edgeLine) {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                edgeLine.copy(alpha = 0.10f),
                edgeLine.copy(alpha = 0.34f),
                edgeLine.copy(alpha = 0.10f),
                Color.Transparent,
            ),
        )
    }
    val softEdgeBrush = remember(top, ambient, backgroundColor) {
        Brush.verticalGradient(
            colors = if (top) {
                listOf(
                    Color.Transparent,
                    ambient.copy(alpha = 0.04f),
                    Color.Transparent,
                )
            } else {
                listOf(
                    Color.Transparent,
                    backgroundColor.copy(alpha = 0.05f),
                    Color.Transparent,
                )
            },
        )
    }

    Box(modifier = modifier.background(mistBrush)) {
        Box(
            modifier = Modifier
                .align(if (top) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(sheenBrush),
        )
        Box(
            modifier = Modifier
                .align(if (top) Alignment.BottomCenter else Alignment.TopCenter)
                .fillMaxWidth()
                .height(18.dp)
                .background(softEdgeBrush),
        )
    }
}

@Composable
private fun HeaderStatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        modifier = modifier
            .background(color.copy(alpha = 0.13f), ObservatoryPillShape)
            .border(1.dp, color.copy(alpha = 0.28f), ObservatoryPillShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun AppLanguageToggle(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(48.dp)
            .semantics {
                contentDescription = strings.t("\uC571 \uC5B8\uC5B4", "App language")
                stateDescription = if (language == AppLanguage.Korean) strings.t("\uD55C\uAE00", "Korean") else "English"
            }
            .animatedPressScale(interactionSource)
            .background(AuraColors.Control, ObservatoryPillShape)
            .border(1.dp, AuraColors.GlassEdge, ObservatoryPillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onLanguageChange(language.toggled()) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (language == AppLanguage.Korean) strings.koreanToggle else strings.englishToggle,
            color = AuraColors.Blue,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppColorModeButton(
    colorMode: AppColorMode,
    onColorModeChange: (AppColorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val interactionSource = remember { MutableInteractionSource() }
    val tint = if (colorMode == AppColorMode.Light) AuraColors.Gold else AuraColors.Blue
    Box(
        modifier = modifier
            .size(48.dp)
            .animatedPressScale(interactionSource)
            .background(AuraColors.Control, ObservatoryPillShape)
            .border(1.dp, AuraColors.GlassEdge, ObservatoryPillShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onColorModeChange(colorMode.toggled()) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (colorMode == AppColorMode.Light) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            contentDescription = if (colorMode == AppColorMode.Light) {
                strings.t("\uB77C\uC774\uD2B8 \uBAA8\uB4DC", "Light mode")
            } else {
                strings.t("\uB2E4\uD06C \uBAA8\uB4DC", "Dark mode")
            },
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
    }
}
@Immutable
private data class ManagementCardCopy(
    val title: String,
    val subtitle: String,
    val value: String,
    val tint: Color,
    val icon: ImageVector,
)

@Immutable
private data class ManagementCardVisual(
    val copy: ManagementCardCopy,
    val variant: Int,
)
@Composable
private fun ManagementCardFace(
    copy: ManagementCardCopy,
    variant: Int,
    modifier: Modifier = Modifier,
) {
    val iconTone = if (isLightAura()) copy.tint else AuraColors.Ink
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(managementCardBrush(copy.tint, variant))
            .padding(15.dp),
    ) {
        Icon(
            imageVector = copy.icon,
            contentDescription = null,
            tint = copy.tint.copy(alpha = if (isLightAura()) 0.24f else 0.30f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(72.dp),
        )
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = copy.icon,
                contentDescription = null,
                tint = iconTone,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = copy.title,
                color = AuraColors.Ink,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = copy.value,
            color = AuraColors.Ink,
            fontSize = 21.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            text = copy.subtitle,
            color = AuraColors.InkSoft,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = 8.dp),
        )
    }
}

@Composable
private fun managementCardBrush(tint: Color, variant: Int): Brush {
    val light = isLightAura()
    val panel = AuraColors.Panel
    val panelSoft = AuraColors.PanelSoft
    val variantKey = variant % 5
    return remember(tint, variantKey, light, panel, panelSoft) {
        val palette = if (light) {
            when (variantKey) {
                0 -> listOf(lerp(Color.White, tint, 0.11f), panel, panelSoft)
                1 -> listOf(panel, lerp(Color.White, tint, 0.09f), panelSoft)
                2 -> listOf(lerp(panelSoft, tint, 0.08f), panel, panelSoft)
                3 -> listOf(panel, lerp(Color.White, tint, 0.12f), Color(0xFFECEFF5))
                else -> listOf(lerp(Color.White, tint, 0.10f), panel, Color(0xFFEAEFF4))
            }
        } else {
            when (variantKey) {
                0 -> listOf(tint.copy(alpha = 0.30f), Color(0xFF343856).copy(alpha = 0.92f), panel)
                1 -> listOf(Color(0xFF3A344D).copy(alpha = 0.92f), tint.copy(alpha = 0.24f), panel)
                2 -> listOf(Color(0xFF2E4551).copy(alpha = 0.78f), tint.copy(alpha = 0.20f), panel)
                3 -> listOf(tint.copy(alpha = 0.22f), Color(0xFF373650).copy(alpha = 0.88f), panel)
                else -> listOf(Color(0xFF4B3343).copy(alpha = 0.72f), tint.copy(alpha = 0.18f), panel)
            }
        }
        Brush.linearGradient(
            colors = palette,
            start = Offset.Zero,
            end = Offset(420f, 360f),
        )
    }
}

@Composable
private fun LiquidPopupBorder(
    tint: Color,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val flow = sin(progress * Math.PI).toFloat().coerceAtLeast(0f)
        val radius = 32.dp.toPx()
        val stroke = (1.2f + flow * 2.4f).dp.toPx()
        drawRoundRect(
            color = tint.copy(alpha = 0.14f + flow * 0.22f),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke),
        )

        val sweep = size.width * (0.20f + progress * 0.50f)
        val y = size.height * (0.12f + flow * 0.03f)
        val highlight = Path().apply {
            moveTo(-size.width * 0.10f + sweep, y)
            cubicTo(
                size.width * 0.18f + sweep,
                y - size.height * 0.08f * flow,
                size.width * 0.40f + sweep,
                y + size.height * 0.08f * flow,
                size.width * 0.72f + sweep,
                y,
            )
        }
        drawPath(
            path = highlight,
            color = Color.White.copy(alpha = flow * 0.18f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        val lowerWave = Path().apply {
            moveTo(size.width * (0.12f - progress * 0.10f), size.height * 0.88f)
            cubicTo(
                size.width * 0.30f,
                size.height * (0.82f - flow * 0.04f),
                size.width * 0.58f,
                size.height * (0.96f + flow * 0.02f),
                size.width * 0.86f,
                size.height * 0.86f,
            )
        }
        drawPath(
            path = lowerWave,
            color = tint.copy(alpha = flow * 0.16f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun <T> auraTween(
    durationMillis: Int = AuraMotion.ControlMillis,
    exiting: Boolean = false,
): FiniteAnimationSpec<T> =
    spring(
        dampingRatio = if (exiting) 0.96f else motionSpringDamping(durationMillis),
        stiffness = motionSpringStiffness(durationMillis),
    )

private fun motionSpringDamping(durationMillis: Int): Float =
    when {
        durationMillis <= AuraMotion.PressMillis -> 0.88f
        durationMillis <= AuraMotion.ControlMillis -> 0.90f
        durationMillis <= AuraMotion.AlarmEnterMillis -> 0.90f
        durationMillis <= AuraMotion.PageSlideMillis -> 0.90f
        durationMillis <= AuraMotion.PopupEnterMillis -> 0.90f
        else -> 0.90f
    }

private fun motionSpringStiffness(durationMillis: Int): Float =
    when {
        durationMillis <= AuraMotion.PressMillis -> 980f
        durationMillis <= AuraMotion.ControlMillis -> 560f
        durationMillis <= AuraMotion.AlarmEnterMillis -> 530f
        durationMillis <= AuraMotion.PageSlideMillis -> 520f
        durationMillis <= AuraMotion.PopupEnterMillis -> 500f
        else -> 460f
    }

private fun curvedPopupOffset(
    originX: Float,
    originY: Float,
    centerX: Float,
    centerY: Float,
    progress: Float,
    enabled: Boolean,
): Offset {
    if (!enabled) return Offset.Zero

    val t = progress.coerceIn(0f, 1f)
    val dx = originX - centerX
    val dy = originY - centerY
    val eased = pinterestMotionProgress(t)
    val remaining = 1f - eased
    val arc = sin(t * Math.PI).toFloat() * 30f
    val arcDirection = if (dy >= 0f) -1f else 1f
    return Offset(
        x = dx * remaining,
        y = dy * remaining + arc * arcDirection,
    )
}

private fun pinterestMotionProgress(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    val easeOut = 1f - (1f - t) * (1f - t) * (1f - t)
    val settle = sin(t * Math.PI).toFloat() * 0.035f
    return (easeOut + settle).coerceIn(0f, 1f)
}

private fun popupShellProgress(progress: Float): Float {
    return pinterestMotionProgress(((progress - 0.04f) / 0.90f).coerceIn(0f, 1f))
}

private fun popupChildProgress(progress: Float, delay: Float): Float {
    val normalized = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
    return pinterestMotionProgress(normalized)
}

@Composable
private fun PopupOriginCardFace(
    origin: ManagementCardOrigin?,
    progress: Float,
    fallbackTitle: String,
    fallbackIcon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    if (origin == null) return

    val cardProgress = ((0.82f - progress) / 0.70f).coerceIn(0f, 1f)
    val visual = origin.visual

    if (visual != null) {
        ManagementCardFace(
            copy = visual.copy,
            variant = visual.variant,
            modifier = modifier
                .graphicsLayer {
                    alpha = cardProgress
                    scaleX = 0.985f + cardProgress * 0.015f
                    scaleY = 0.985f + cardProgress * 0.015f
                },
        )
    } else {
        Row(
            modifier = modifier
                .background(liquidGlassBrush(tint = tint.copy(alpha = 0.10f)))
                .padding(horizontal = 12.dp)
                .graphicsLayer {
                    alpha = cardProgress
                    scaleX = 0.985f + cardProgress * 0.015f
                    scaleY = 0.985f + cardProgress * 0.015f
                },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = fallbackTitle,
                color = AuraColors.Ink,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LiquidPopupDialog(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    tint: Color,
    onDismiss: () -> Unit,
    origin: ManagementCardOrigin? = null,
    maxContentHeight: Dp = 420.dp,
    footer: (@Composable RowScope.(close: () -> Unit) -> Unit)? = null,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val strings = LocalAppStrings.current
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var overlayTopLeft by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var targetPopupSize by remember { mutableStateOf(IntSize.Zero) }
    val layoutReady = overlaySize.width > 0 &&
        overlaySize.height > 0 &&
        targetPopupSize.width > 0 &&
        targetPopupSize.height > 0
    val targetProgress = if (visible && layoutReady) 1f else 0f
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = auraTween(
            durationMillis = if (targetProgress == 1f) {
                AuraMotion.LiquidPopupEnterMillis
            } else {
                AuraMotion.LiquidPopupExitMillis
            },
            exiting = targetProgress == 0f,
        ),
        label = "liquid-dialog-progress",
    )
    val overlayInteraction = remember { MutableInteractionSource() }
    val popupInteraction = remember { MutableInteractionSource() }

    fun close() {
        if (closing) return
        closing = true
        visible = false
    }

    LaunchedEffect(layoutReady) {
        if (layoutReady && !closing) {
            withFrameNanos { }
            visible = true
        }
    }
    LaunchedEffect(closing, visible) {
        if (closing && !visible) {
            var frameCount = 0
            while (progress > 0.006f && frameCount < 30) {
                withFrameNanos { }
                frameCount += 1
            }
            onDismiss()
        }
    }

    val overlayPaddingHorizontal = 16.dp
    val overlayPaddingVertical = 14.dp

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogView = LocalView.current
        val windowBlurRadiusPx = with(LocalDensity.current) { WINDOW_BLUR_BEHIND_DP.dp.toPx().toInt() }
        DisposableEffect(dialogView, windowBlurRadiusPx) {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            dialogWindow?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dialogWindow != null) {
                val originalDimAmount = dialogWindow.attributes.dimAmount
                val originalBlurRadius = dialogWindow.attributes.blurBehindRadius
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                dialogWindow.attributes = dialogWindow.attributes.apply {
                    dimAmount = 0f
                    blurBehindRadius = windowBlurRadiusPx
                }
                onDispose {
                    dialogWindow.attributes = dialogWindow.attributes.apply {
                        dimAmount = originalDimAmount
                        blurBehindRadius = originalBlurRadius
                    }
                    dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
            } else {
                onDispose { }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    overlayTopLeft = coordinates.positionInRoot()
                    overlaySize = coordinates.size
                }
                .clickable(
                    interactionSource = overlayInteraction,
                    indication = null,
                    onClick = ::close,
                )
                .padding(horizontal = overlayPaddingHorizontal, vertical = overlayPaddingVertical),
            contentAlignment = Alignment.Center,
        ) {
            val density = LocalDensity.current
            val targetCenterX = overlaySize.width / 2f
            val targetCenterY = overlaySize.height / 2f
            val hasOrigin = origin != null
            val originX = origin?.let { it.centerX - overlayTopLeft.x } ?: targetCenterX
            val originY = origin?.let { it.centerY - overlayTopLeft.y } ?: targetCenterY
            val motionProgress = progress.coerceIn(0f, 1f)
            val initialScaleX = if (origin != null && targetPopupSize.width > 0 && origin.width > 0f) {
                (origin.width / targetPopupSize.width).coerceIn(0.16f, 0.92f)
            } else {
                1f
            }
            val initialScaleY = if (origin != null && targetPopupSize.height > 0 && origin.height > 0f) {
                (origin.height / targetPopupSize.height).coerceIn(0.16f, 0.82f)
            } else {
                1f
            }
            val scaleX = (initialScaleX + (1f - initialScaleX) * motionProgress).coerceAtLeast(0.05f)
            val scaleY = (initialScaleY + (1f - initialScaleY) * motionProgress).coerceAtLeast(0.05f)
            val organicPulse = 1f + sin(motionProgress * Math.PI).toFloat() * 0.010f
            val popupOffset = curvedPopupOffset(
                originX = originX,
                originY = originY,
                centerX = targetCenterX,
                centerY = targetCenterY,
                progress = motionProgress,
                enabled = hasOrigin,
            )
            val shellProgress = popupShellProgress(motionProgress)
            val headerBurst = popupChildProgress(shellProgress, 0f)
            val bodyBurst = popupChildProgress(shellProgress, 0.08f)
            val footerBurst = popupChildProgress(shellProgress, 0.16f)
            val renderPopupChrome = layoutReady
            val renderPopupBody = layoutReady
            val originCorner = if (origin != null && origin.height < with(density) { 96.dp.toPx() }) 8.dp else 30.dp
            val startCorner = (originCorner.value / min(scaleX, scaleY).coerceAtLeast(0.08f)).dp
            val cornerRadius = (startCorner.value + (32f - startCorner.value) * motionProgress).dp
            val cardBorderTint = origin?.visual?.copy?.tint ?: tint
            val cardBorderAlpha = if (origin?.visual != null) 0.22f else 0.24f
            val containerBorder = lerp(
                cardBorderTint.copy(alpha = cardBorderAlpha),
                tint.copy(alpha = 0.30f),
                shellProgress,
            )

            Surface(
                shape = RoundedCornerShape(cornerRadius),
                color = Color.Transparent,
                border = BorderStroke(1.dp, containerBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PLATFORM_POPUP_MIN_HEIGHT, max = PLATFORM_POPUP_MAX_HEIGHT)
                    .onGloballyPositioned { coordinates ->
                        if (targetPopupSize == IntSize.Zero &&
                            coordinates.size.width > 0 &&
                            coordinates.size.height > 0
                        ) {
                            targetPopupSize = coordinates.size
                        }
                    }
                    .graphicsLayer {
                        translationX = popupOffset.x
                        translationY = popupOffset.y
                        this.scaleX = scaleX * organicPulse
                        this.scaleY = scaleY * organicPulse
                        alpha = if (layoutReady) 1f else 0f
                    }
                    .clickable(
                        interactionSource = popupInteraction,
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                alpha = shellProgress
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                            }
                            .background(liquidGlassBrush(tint = tint.copy(alpha = 0.08f))),
                    )
                    PopupOriginCardFace(
                        origin = origin,
                        progress = motionProgress,
                        fallbackTitle = title,
                        fallbackIcon = icon,
                        tint = tint,
                        modifier = Modifier.matchParentSize(),
                    )
                    if (renderPopupChrome) {
                        LiquidPopupBorder(
                            tint = tint,
                            progress = shellProgress,
                            modifier = Modifier
                                .matchParentSize()
                                .graphicsLayer {
                                    alpha = shellProgress
                                    compositingStrategy = CompositingStrategy.ModulateAlpha
                                },
                        )
                    }
                    if (renderPopupChrome) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = shellProgress
                                    compositingStrategy = CompositingStrategy.ModulateAlpha
                                }
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                        Row(
                            modifier = Modifier.graphicsLayer {
                                translationY = (1f - headerBurst) * -28f
                                this.scaleX = 0.96f + headerBurst * 0.04f
                                this.scaleY = 0.96f + headerBurst * 0.04f
                                alpha = headerBurst
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconTile(icon = icon, tint = tint)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = AuraColors.Ink,
                                    fontSize = 24.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!subtitle.isNullOrBlank()) {
                                    Text(
                                        text = subtitle,
                                        color = AuraColors.InkSoft,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        if (renderPopupBody) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = maxContentHeight)
                                    .graphicsLayer {
                                        translationY = (1f - bodyBurst) * -36f
                                        this.scaleX = 0.98f + bodyBurst * 0.02f
                                        this.scaleY = 0.98f + bodyBurst * 0.02f
                                        alpha = bodyBurst
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                },
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                content(::close)
                            }
                        }
                        if (renderPopupBody) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .graphicsLayer {
                                        translationY = (1f - footerBurst) * -16f
                                        this.scaleX = 0.98f + footerBurst * 0.02f
                                        this.scaleY = 0.98f + footerBurst * 0.02f
                                        alpha = footerBurst
                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                    },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (footer != null) {
                                    footer(::close)
                                } else {
                                    AnimatedTextButton(onClick = ::close) {
                                        Text(strings.close, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagementSection.title(strings: AppStrings): String = when (this) {
    ManagementSection.AlertSettings -> strings.alertSettings
    ManagementSection.SoundCategories -> strings.soundCategories
    ManagementSection.Calibration -> strings.calibration
    ManagementSection.EventHistory -> strings.eventHistory
    ManagementSection.Test -> strings.test
}

@Composable
private fun ManagementSection.subtitle(strings: AppStrings): String = when (this) {
    ManagementSection.AlertSettings -> strings.t("\uC54C\uB9BC \uB3D9\uC791\uACFC \uAC1C\uC778\uC815\uBCF4 \uC124\uC815", "Adjust alert behavior and privacy")
    ManagementSection.SoundCategories -> strings.t("\uAC10\uC9C0\uD560 \uC18C\uB9AC \uC120\uD0DD", "Choose sound categories")
    ManagementSection.Calibration -> strings.t("\uB0B4 \uC18C\uB9AC\uB85C \uC815\uD655\uB3C4 \uBCF4\uC815", "Tune accuracy with your own sounds")
    ManagementSection.EventHistory -> strings.t("\uAC10\uC9C0\uB41C \uC18C\uB9AC \uAE30\uB85D \uD655\uC778", "Review detected sound events")
    ManagementSection.Test -> strings.t("\uC54C\uB9BC \uD654\uBA74 \uBBF8\uB9AC\uBCF4\uAE30", "Preview alert visualization")
}

@Composable
private fun ManagementSection.tint(): Color = when (this) {
    ManagementSection.AlertSettings -> AuraColors.Blue
    ManagementSection.SoundCategories -> AuraColors.Name
    ManagementSection.Calibration -> AuraColors.Gold
    ManagementSection.EventHistory -> AuraColors.Doorbell
    ManagementSection.Test -> AuraColors.Siren
}

private fun ManagementSection.icon(): ImageVector = when (this) {
    ManagementSection.AlertSettings -> Icons.Rounded.Notifications
    ManagementSection.SoundCategories -> Icons.Rounded.GraphicEq
    ManagementSection.Calibration -> Icons.Rounded.Settings
    ManagementSection.EventHistory -> Icons.Rounded.Notifications
    ManagementSection.Test -> Icons.Rounded.PlayArrow
}

@Composable
private fun LiveFrequencyWaveformGraph(
    level: Float,
    spectrumBands: List<Float>,
    active: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val palette = LocalAuraPalette.current
    val safeBands = remember(spectrumBands, compact) {
        when {
            spectrumBands.isNotEmpty() -> spectrumBands
            compact -> List(SoundFeatureExtractor.LIVE_SPECTRUM_BAND_COUNT) { 0f }
            else -> List(SoundFeatureExtractor.LIVE_SPECTRUM_BAND_COUNT) { 0f }
        }
    }
    val shouldAnimate = active && (level > 0.002f || safeBands.any { it > 0.002f })
    var waveformPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(shouldAnimate) {
        if (!shouldAnimate) {
            waveformPhase = 0f
            return@LaunchedEffect
        }
        while (true) {
            withFrameNanos { frameTime ->
                waveformPhase = (frameTime % 10_000_000_000L) / 1_000_000_000f
            }
        }
    }
    val animatedLevel by animateFloatAsState(
        targetValue = if (active) level.coerceIn(0f, 1f) else 0f,
        animationSpec = spring(
            dampingRatio = 0.30f,
            stiffness = 1180f,
            visibilityThreshold = 0.001f,
        ),
        label = "live-waveform-level",
    )
    val pressScale = 1f + animatedLevel * if (compact) 0.040f else 0.050f

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
        },
    ) {
        val bandCount = if (compact) 40 else WAVEFORM_VISUAL_ZONE_COUNT
        if (bandCount <= 1) return@Canvas

        val eqBandCount = SoundFeatureExtractor.LIVE_SPECTRUM_BAND_COUNT
        val centerY = size.height * 0.5f
        val left = size.width * if (compact) 0.05f else 0.025f
        val right = size.width * if (compact) 0.95f else 0.975f
        val width = right - left
        val spacing = width / (bandCount - 1).coerceAtLeast(1)
        val edgeMargin = (if (compact) 2.dp else 3.dp).toPx()
        val maxHeight = (centerY - edgeMargin).coerceAtLeast(size.height * if (compact) 0.40f else 0.46f)
        val minHeight = size.height * if (compact) 0.030f else 0.024f
        val stroke = (if (compact) 1.75f else 1.25f).dp.toPx()
        val glowStroke = stroke * if (compact) 2.10f else 1.85f
        val low = Color(0xFF315BFF)
        val middle = Color(0xFF4FEAFF)
        val high = Color(0xFF2444FF)
        var strongestBandIndex = 0
        var strongestValue = 0f

        for (index in 0 until eqBandCount) {
            val sourceIndex = index.coerceIn(0, safeBands.lastIndex)
            val value = safeBands.getOrElse(sourceIndex) { 0f }.coerceIn(0f, 1f)
            if (value > strongestValue) {
                strongestValue = value
                strongestBandIndex = index
            }
        }
        val soundGate = (((maxOf(animatedLevel * 1.85f, strongestValue * 1.18f) - WAVEFORM_SILENCE_FLOOR) /
            WAVEFORM_GATE_RANGE)).coerceIn(0f, 1f)
        if (!active || soundGate <= 0.015f) {
            val lineColor = lerp(low, middle, 0.54f)
            drawLine(
                color = lineColor.copy(alpha = if (active) 0.62f else 0.38f),
                start = Offset(left, centerY),
                end = Offset(right, centerY),
                strokeWidth = (if (compact) 2.0f else 2.4f).dp.toPx(),
                cap = StrokeCap.Round,
            )
            return@Canvas
        }

        for (index in 0 until bandCount) {
            val ratio = index / (bandCount - 1f)
            val exactBandPosition = ((index + 0.5f) * eqBandCount / bandCount)
                .coerceIn(0f, eqBandCount - 0.001f)
            val bandIndex = exactBandPosition.toInt().coerceIn(0, eqBandCount - 1)
            val localRatio = (exactBandPosition - bandIndex).coerceIn(0f, 1f)
            val previousBand = safeBands.getOrElse((bandIndex - 1).coerceAtLeast(0)) { 0f }.coerceIn(0f, 1f)
            val currentBand = safeBands.getOrElse(bandIndex.coerceIn(0, safeBands.lastIndex)) { 0f }.coerceIn(0f, 1f)
            val nextBand = safeBands.getOrElse((bandIndex + 1).coerceAtMost(safeBands.lastIndex)) { currentBand }.coerceIn(0f, 1f)
            val bandBlend = when {
                localRatio < 0.18f && bandIndex > 0 -> lerpScalar(previousBand, currentBand, localRatio / 0.18f)
                localRatio > 0.82f && bandIndex < eqBandCount - 1 -> lerpScalar(currentBand, nextBand, (localRatio - 0.82f) / 0.18f)
                else -> currentBand
            }.coerceIn(0f, 1f)
            val reference = referenceWaveformEnvelope(ratio, compact)
            val liveResponse = if (active) {
                val levelCurve = (animatedLevel / (animatedLevel + 0.22f)).coerceIn(0f, 1f)
                val reactiveBand = (bandBlend / (bandBlend + 0.040f + levelCurve * 0.030f)).coerceIn(0f, 1f)
                val loudnessLimiter = (1.08f - levelCurve * 0.24f).coerceIn(0.78f, 1.08f)
                val eqPunch = (reactiveBand * 3.28f * loudnessLimiter + levelCurve * 0.72f).coerceIn(0f, 3.45f)
                (0.16f + eqPunch) * soundGate
            } else {
                0f
            }
            val bandMotion = sin(waveformPhase * (9.4f + bandIndex * 0.52f) + localRatio * 5.8f)
            val fineMotion = sin(waveformPhase * (15.2f + bandIndex * 0.35f) + ratio * 31f)
            val detail = 0.90f + bandMotion * 0.18f * bandBlend + fineMotion * 0.08f * soundGate
            val barHeight = (minHeight * soundGate + maxHeight * reference * liveResponse * detail)
                .coerceIn(minHeight, maxHeight)
            val x = left + spacing * index
            val color = if (ratio <= 0.52f) {
                lerp(low, middle, (ratio / 0.52f).coerceIn(0f, 1f))
            } else {
                lerp(middle, high, ((ratio - 0.52f) / 0.48f).coerceIn(0f, 1f))
            }
            val dominantBoost = if (bandIndex == strongestBandIndex && strongestValue > 0.08f) 1f else 0f
            val top = Offset(x, centerY - barHeight * (1f + dominantBoost * 0.08f))
            val bottom = Offset(x, centerY + barHeight * (1f + dominantBoost * 0.08f))

            drawLine(
                color = color.copy(alpha = 0.16f + animatedLevel * 0.08f + dominantBoost * 0.06f),
                start = top,
                end = bottom,
                strokeWidth = glowStroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color.copy(alpha = if (active) 0.96f else 0.78f),
                start = top,
                end = bottom,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun referenceWaveformEnvelope(ratio: Float, compact: Boolean): Float {
    val x = ratio.coerceIn(0f, 1f)
    val base = interpolateReferenceWaveform(x)
    val edgeTaper = sin(Math.PI.toFloat() * x).coerceAtLeast(0f).let { 0.42f + it * 0.58f }
    val compactLift = if (compact) 0.88f else 1f

    return (base * edgeTaper * compactLift).coerceIn(0.06f, 1f)
}

private fun interpolateReferenceWaveform(ratio: Float): Float {
    var index = 0
    while (index < ReferenceWaveformEnvelope.size - 2) {
        val leftX = ReferenceWaveformEnvelope[index]
        val rightX = ReferenceWaveformEnvelope[index + 2]
        if (ratio <= rightX) {
            val leftY = ReferenceWaveformEnvelope[index + 1]
            val rightY = ReferenceWaveformEnvelope[index + 3]
            val fraction = ((ratio - leftX) / (rightX - leftX)).coerceIn(0f, 1f)
            return lerpScalar(leftY, rightY, fraction)
        }
        index += 2
    }
    return ReferenceWaveformEnvelope.last()
}

private fun lerpScalar(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction.coerceIn(0f, 1f)

private const val WAVEFORM_SILENCE_FLOOR = 0.004f
private const val WAVEFORM_GATE_RANGE = 0.030f
private const val WAVEFORM_VISUAL_ZONE_COUNT = 80

private val ReferenceWaveformEnvelope = floatArrayOf(
    0.000f, 0.12f,
    0.030f, 0.20f,
    0.060f, 0.27f,
    0.090f, 0.34f,
    0.120f, 0.44f,
    0.150f, 0.58f,
    0.178f, 0.80f,
    0.205f, 1.00f,
    0.230f, 0.52f,
    0.252f, 0.68f,
    0.278f, 0.83f,
    0.305f, 0.62f,
    0.335f, 0.46f,
    0.370f, 0.38f,
    0.405f, 0.34f,
    0.438f, 0.50f,
    0.468f, 0.84f,
    0.492f, 0.43f,
    0.520f, 0.74f,
    0.548f, 0.32f,
    0.585f, 0.36f,
    0.620f, 0.42f,
    0.655f, 0.51f,
    0.688f, 0.66f,
    0.718f, 0.86f,
    0.748f, 1.00f,
    0.778f, 0.84f,
    0.808f, 0.62f,
    0.840f, 0.47f,
    0.874f, 0.38f,
    0.908f, 0.33f,
    0.938f, 0.48f,
    0.966f, 0.68f,
    1.000f, 0.12f,
)

@Composable
private fun StageCanvas(
    kind: AlertKind,
    level: Float,
    motionEnabled: Boolean,
    modifier: Modifier,
) {
    val palette = LocalAuraPalette.current
    val animatedLevel by animateFloatAsState(
        targetValue = if (motionEnabled) {
            (level.coerceIn(0f, 1f) * 1.38f).coerceIn(0f, 1f)
        } else {
            0f
        },
        animationSpec = spring(
            dampingRatio = 0.34f,
            stiffness = 760f,
            visibilityThreshold = 0.002f,
        ),
        label = "stage-volume-bounce",
    )
    val pulse = stagePulse(kind, animatedLevel)

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = min(size.width, size.height) * 0.28f

        if (!motionEnabled) {
            drawStaticStageGlyph(
                kind = kind,
                center = center,
                base = base,
                palette = palette,
            )
            return@Canvas
        }

        when (kind) {
            AlertKind.Doorbell -> drawDoorbell(center, base, pulse, animatedLevel, palette.doorbell)
            AlertKind.PhoneRing -> drawDoorbell(center, base, pulse, animatedLevel, palette.blue)
            AlertKind.ApplianceBeep -> drawDoorbell(center, base, pulse, animatedLevel, palette.gold)
            AlertKind.WaterRunning -> drawDoorbell(center, base, pulse, animatedLevel, palette.name)
            AlertKind.BabyCry -> drawDoorbell(center, base, pulse, animatedLevel, palette.name)
            AlertKind.Siren,
            AlertKind.FireAlarm -> drawSiren(center, base, pulse, palette.siren, palette.gold)
            AlertKind.NameCall,
            AlertKind.Knock -> drawDoorbell(center, base, pulse, animatedLevel, palette.name)
            AlertKind.Idle -> drawStaticStageGlyph(kind, center, base, palette)
        }
    }
}

private fun stagePulse(kind: AlertKind, level: Float): Float {
    val base = if (kind == AlertKind.Idle) 0.24f else 0.42f
    return (base + level.coerceIn(0f, 1f) * 0.46f).coerceIn(0f, 1f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDoorbell(
    center: Offset,
    base: Float,
    pulse: Float,
    level: Float,
    blue: Color,
) {
    repeat(3) { index ->
        val phase = (pulse + index * 0.33f) % 1f
        drawCircle(
            color = blue.copy(alpha = (1f - phase) * 0.55f),
            radius = base * (0.42f + phase * (0.9f + level * 0.18f)),
            center = center,
            style = Stroke(width = 4.dp.toPx()),
        )
    }
    drawCircle(blue.copy(alpha = 0.12f), base * 0.36f, center)
    drawRoundRect(
        color = blue,
        topLeft = Offset(center.x - base * 0.18f, center.y - base * 0.24f),
        size = androidx.compose.ui.geometry.Size(base * 0.36f, base * 0.5f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
        style = Stroke(width = 5.dp.toPx()),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStaticStageGlyph(
    kind: AlertKind,
    center: Offset,
    base: Float,
    palette: AuraPalette,
) {
    val accent = when (kind) {
        AlertKind.Doorbell -> palette.doorbell
        AlertKind.PhoneRing -> palette.blue
        AlertKind.ApplianceBeep -> palette.gold
        AlertKind.Siren,
        AlertKind.FireAlarm -> palette.siren
        AlertKind.NameCall,
        AlertKind.Knock,
        AlertKind.BabyCry,
        AlertKind.WaterRunning -> palette.name
        AlertKind.Idle -> palette.muted
    }

    drawCircle(
        color = accent.copy(alpha = 0.12f),
        radius = base * 0.86f,
        center = center,
    )
    drawCircle(
        color = accent.copy(alpha = 0.42f),
        radius = base * 0.72f,
        center = center,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
    )

    when (kind) {
        AlertKind.Siren,
        AlertKind.FireAlarm -> drawSiren(center, base * 0.86f, 0.18f, palette.siren, palette.gold)
        AlertKind.Idle -> drawCircle(
            color = palette.muted.copy(alpha = 0.24f),
            radius = base * 0.26f,
            center = center,
        )
        else -> {
            drawCircle(
                color = accent,
                radius = base * 0.24f,
                center = center,
            )
            drawCircle(
                color = accent.copy(alpha = 0.62f),
                radius = base * 0.48f,
                center = center,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSiren(
    center: Offset,
    base: Float,
    pulse: Float,
    red: Color,
    amber: Color,
) {
    val path = Path().apply {
        moveTo(center.x, center.y - base * 0.92f)
        lineTo(center.x + base * 0.92f, center.y + base * 0.74f)
        lineTo(center.x - base * 0.92f, center.y + base * 0.74f)
        close()
    }
    drawPath(path, red.copy(alpha = 0.18f + pulse * 0.15f))
    drawPath(path, red.copy(alpha = 0.82f), style = Stroke(width = 5.dp.toPx()))
    drawLine(
        red,
        Offset(center.x, center.y - base * 0.42f),
        Offset(center.x, center.y + base * 0.18f),
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(red, 6.dp.toPx(), Offset(center.x, center.y + base * 0.42f))
    drawLine(
        amber.copy(alpha = 0.72f),
        Offset(center.x - base, center.y + base * 0.94f),
        Offset(center.x + base, center.y + base * 0.94f),
        strokeWidth = 8.dp.toPx(),
        cap = StrokeCap.Round,
    )
}

@Composable
private fun stageBrush(kind: AlertKind): Brush {
    val panel = AuraColors.Panel
    val doorbell = AuraColors.Doorbell
    val siren = AuraColors.Siren
    val name = AuraColors.Name
    val gold = AuraColors.Gold

    return remember(kind, panel, doorbell, siren, name, gold) {
        val tint = when (kind) {
            AlertKind.Doorbell,
            AlertKind.PhoneRing -> doorbell
            AlertKind.ApplianceBeep -> gold
            AlertKind.Siren,
            AlertKind.FireAlarm -> siren
            AlertKind.NameCall,
            AlertKind.Knock,
            AlertKind.BabyCry,
            AlertKind.WaterRunning -> name
            AlertKind.Idle -> panel
        }
        val base = lerp(panel, tint, if (kind == AlertKind.Idle) 0f else 0.055f)
        Brush.verticalGradient(listOf(base, base))
    }
}

@Composable
private fun LevelMeter(level: Float) {
    val blue = AuraColors.Blue
    val name = AuraColors.Name
    val gold = AuraColors.Gold
    val meterBrush = remember(blue, name, gold) {
        Brush.horizontalGradient(listOf(blue, name, gold))
    }
    Box(
        modifier = Modifier
            .width(170.dp)
            .height(8.dp)
            .background(AuraColors.Control, ObservatoryPillShape)
            .border(1.dp, AuraColors.GlassEdge, ObservatoryPillShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(level.coerceIn(0f, 1f))
                .height(8.dp)
                .background(
                    meterBrush,
                    ObservatoryPillShape,
                ),
        )
    }
}
@Composable
private fun PremiumDetectedSoundAlarm(
    event: DetectedSoundEvent,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var retainedEvent by remember { mutableStateOf<DetectedSoundEvent?>(null) }
    val visible = event.kind != AlertKind.Idle
    val displayEvent = if (visible) event else retainedEvent

    LaunchedEffect(event.id, event.kind) {
        if (event.kind != AlertKind.Idle) {
            retainedEvent = event
        }
    }
    val alarmCorner by animateDpAsState(
        targetValue = if (visible) 28.dp else 20.dp,
        animationSpec = auraTween(
            durationMillis = if (visible) AuraMotion.AlarmEnterMillis else AuraMotion.AlarmExitMillis,
            exiting = !visible,
        ),
        label = "premium-alarm-corner",
    )
    val alarmLift by animateDpAsState(
        targetValue = if (visible) 0.dp else 20.dp,
        animationSpec = auraTween(
            durationMillis = if (visible) AuraMotion.AlarmEnterMillis else AuraMotion.AlarmExitMillis,
            exiting = !visible,
        ),
        label = "premium-alarm-lift",
    )
    val density = LocalDensity.current
    val alarmLiftPx = with(density) { alarmLift.toPx() }

    AnimatedVisibility(
        visible = visible && displayEvent != null,
        enter = scaleIn(
            animationSpec = auraTween(AuraMotion.AlarmEnterMillis),
            initialScale = 0.80f,
        ),
        exit = scaleOut(
            animationSpec = auraTween(AuraMotion.AlarmExitMillis, exiting = true),
            targetScale = 0.86f,
        ),
        modifier = modifier,
    ) {
        val alarmEvent = displayEvent ?: return@AnimatedVisibility
        val strings = LocalAppStrings.current
        val accent = alarmEvent.severity.priorityColor()
        val title = if (alarmEvent.kind == AlertKind.NameCall && alarmEvent.title.isNotBlank()) {
            alarmEvent.title
        } else {
            strings.alertTitle(alarmEvent.kind)
        }
        val glowAlpha = when (alarmEvent.severity) {
            AlertSeverity.Emergency -> 0.42f
            AlertSeverity.Caution -> 0.34f
            AlertSeverity.Info -> 0.28f
        }
        val light = isLightAura()
        val titleColor = if (light) AuraColors.Ink else Color.White.copy(alpha = 0.96f)
        val bodyColor = if (light) AuraColors.InkSoft else Color.White.copy(alpha = 0.86f)
        val captionColor = if (light) AuraColors.Muted else Color.White.copy(alpha = 0.58f)
        val alarmTileBackground = if (light) Color.White.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.12f)
        val alarmTileBorder = if (light) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.18f)
        val particleColor = if (light) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.22f)
        val severityLabel = strings.severityLabel(alarmEvent.severity)
        val actionText = strings.userActionText(alarmEvent)
        val confidenceText = strings.confidenceLabel(alarmEvent.confidence)
        val vibrationText = strings.vibrationCueText(alarmEvent.severity)

        Surface(
            shape = RoundedCornerShape(alarmCorner),
            color = Color.Transparent,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(342.dp)
                .graphicsLayer {
                    translationY = alarmLiftPx
                }
                .shadow(24.dp, RoundedCornerShape(alarmCorner), clip = false)
                .semantics {
                    contentDescription = "$severityLabel. $title. $actionText. $confidenceText. $vibrationText"
                    stateDescription = strings.t("\uC54C\uB9BC \uD45C\uC2DC \uC911", "Alert visible")
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onDismiss,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(premiumAlarmBrush(accent, alarmEvent.severity))
                    .padding(horizontal = 22.dp, vertical = 22.dp),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    val base = size.minDimension
                    drawCircle(
                        color = accent.copy(alpha = glowAlpha),
                        radius = base * 0.62f,
                        center = Offset(size.width * 0.90f, size.height * 0.18f),
                    )
                    drawCircle(
                        color = accent.copy(alpha = glowAlpha * 0.55f),
                        radius = base * 0.46f,
                        center = Offset(size.width * 0.12f, size.height * 0.94f),
                    )
                    repeat(9) { index ->
                        val x = size.width * (0.70f + index * 0.032f).coerceAtMost(0.97f)
                        val y = size.height * (0.28f + ((index * 17) % 44) / 100f)
                        drawCircle(
                            color = particleColor.copy(alpha = particleColor.alpha + (index % 3) * 0.04f),
                            radius = 1.4.dp.toPx() + (index % 2) * 0.9.dp.toPx(),
                            center = Offset(x, y),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(alarmTileBackground, ObservatoryTileShape)
                            .border(1.dp, alarmTileBorder, ObservatoryTileShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = alarmEvent.icon(),
                            contentDescription = null,
                            tint = alarmIconTint(alarmEvent.severity, accent),
                            modifier = Modifier.size(42.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = title,
                            color = titleColor,
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = actionText,
                            color = bodyColor,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "$confidenceText - $vibrationText",
                            color = captionColor,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    AlarmSeverityStrip(
                        severity = alarmEvent.severity,
                        accent = accent,
                        label = severityLabel,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmSeverityStrip(
    severity: AlertSeverity,
    accent: Color,
    label: String,
) {
    val light = isLightAura()
    val level = when (severity) {
        AlertSeverity.Emergency -> 4
        AlertSeverity.Caution -> 3
        AlertSeverity.Info -> 2
    }
    val progress by animateFloatAsState(
        targetValue = level / 4f,
        animationSpec = auraTween(AuraMotion.ControlMillis),
        label = "alarm-severity-strip",
    )
    Row(
        modifier = Modifier
            .background(if (light) AuraColors.Control else Color.White.copy(alpha = 0.08f), ObservatoryPillShape)
            .border(1.dp, accent.copy(alpha = 0.26f), ObservatoryPillShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { index ->
                val active = index < (progress * 4f).toInt().coerceIn(1, 4)
                Box(
                    modifier = Modifier
                        .width(if (active) 13.dp else 8.dp)
                        .height(6.dp)
                        .background(
                            if (active) alarmIconTint(severity, accent) else exchangeHighlightColor(alphaLight = 0.10f, alphaDark = 0.16f),
                            RoundedCornerShape(99.dp),
                        ),
                )
            }
        }
        Text(
            text = label,
            color = alarmIconTint(severity, accent),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun premiumAlarmBrush(accent: Color, severity: AlertSeverity): Brush {
    val light = isLightAura()
    val base = when (severity) {
        AlertSeverity.Emergency -> if (light) Color(0xFFFFEEF0) else Color(0xFF5C1519)
        AlertSeverity.Caution -> if (light) Color(0xFFFFF5DF) else Color(0xFF4B3410)
        AlertSeverity.Info -> if (light) Color(0xFFEFF8FF) else Color(0xFF153D2A)
    }
    val panel = AuraColors.Panel
    val panelSoft = AuraColors.PanelSoft
    return remember(accent, severity, light, panel, panelSoft) {
        if (light) {
            Brush.linearGradient(
                colors = listOf(
                    lerp(panel, accent, 0.09f),
                    base,
                    panelSoft,
                ),
                start = Offset.Zero,
                end = Offset(920f, 320f),
            )
        } else {
        Brush.linearGradient(
            colors = listOf(
                base,
                lerp(Color(0xFF17131A), accent, 0.42f),
                Color(0xFF121117),
            ),
            start = Offset.Zero,
            end = Offset(920f, 320f),
        )
        }
    }
}

@Composable
private fun alarmIconTint(severity: AlertSeverity, accent: Color): Color = when (severity) {
    AlertSeverity.Emergency -> Color(0xFFFF8D85)
    AlertSeverity.Caution -> AuraColors.Gold
    AlertSeverity.Info -> accent
}

@Composable
private fun NaturalPopupDialog(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    tint: Color,
    onDismiss: () -> Unit,
    maxContentHeight: Dp = 420.dp,
    footer: (@Composable RowScope.(close: () -> Unit) -> Unit)? = null,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    val strings = LocalAppStrings.current
    val listInteraction = remember { MutableInteractionSource() }
    val popupInteraction = remember { MutableInteractionSource() }
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = auraTween(
            durationMillis = if (visible) AuraMotion.PopupEnterMillis else AuraMotion.PopupExitMillis,
            exiting = !visible,
        ),
        label = "natural-popup-progress",
    )
    val density = LocalDensity.current
    val popupCorner by animateDpAsState(
        targetValue = if (visible) 32.dp else 24.dp,
        animationSpec = auraTween(
            durationMillis = if (visible) AuraMotion.PopupEnterMillis else AuraMotion.PopupExitMillis,
            exiting = !visible,
        ),
        label = "natural-popup-corner",
    )
    val popupLift by animateDpAsState(
        targetValue = if (visible) 0.dp else 22.dp,
        animationSpec = auraTween(
            durationMillis = if (visible) AuraMotion.PopupEnterMillis else AuraMotion.PopupExitMillis,
            exiting = !visible,
        ),
        label = "natural-popup-lift",
    )
    val visualProgress = progress.coerceIn(0f, 1f)
    val popupLiftPx = with(density) { popupLift.toPx() }
    val popupScale = 0.945f + visualProgress * 0.055f + sin(visualProgress * Math.PI).toFloat() * 0.008f
    val headerProgress = popupChildProgress(visualProgress, 0f)
    val bodyProgress = popupChildProgress(visualProgress, 0.07f)
    val footerProgress = popupChildProgress(visualProgress, 0.14f)

    fun close() {
        if (closing) return
        closing = true
        visible = false
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        visible = true
    }
    LaunchedEffect(closing, visible) {
        if (closing && !visible) {
            var frameCount = 0
            while (progress > 0.006f && frameCount < 24) {
                withFrameNanos { }
                frameCount += 1
            }
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogView = LocalView.current
        val windowBlurRadiusPx = with(LocalDensity.current) { WINDOW_BLUR_BEHIND_DP.dp.toPx().toInt() }
        DisposableEffect(dialogView, windowBlurRadiusPx) {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            dialogWindow?.setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
            dialogWindow?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dialogWindow != null) {
                val originalDimAmount = dialogWindow.attributes.dimAmount
                val originalBlurRadius = dialogWindow.attributes.blurBehindRadius
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                dialogWindow.attributes = dialogWindow.attributes.apply {
                    dimAmount = 0f
                    blurBehindRadius = windowBlurRadiusPx
                }
                onDispose {
                    dialogWindow.attributes = dialogWindow.attributes.apply {
                        dimAmount = originalDimAmount
                        blurBehindRadius = originalBlurRadius
                    }
                    dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
            } else {
                onDispose { }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = listInteraction,
                    indication = null,
                    onClick = ::close,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(popupCorner),
                color = Color.Transparent,
                border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PLATFORM_POPUP_MIN_HEIGHT, max = PLATFORM_POPUP_MAX_HEIGHT)
                    .graphicsLayer {
                        alpha = visualProgress
                        scaleX = popupScale
                        scaleY = popupScale
                        translationY = popupLiftPx
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                    }
                    .clickable(
                        interactionSource = popupInteraction,
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(liquidGlassBrush(tint = tint.copy(alpha = 0.08f))),
                ) {
                    LiquidPopupBorder(
                        tint = tint,
                        progress = visualProgress,
                        modifier = Modifier.matchParentSize(),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.graphicsLayer {
                                alpha = headerProgress
                                translationY = (1f - headerProgress) * -18f
                                scaleX = 0.98f + headerProgress * 0.02f
                                scaleY = 0.98f + headerProgress * 0.02f
                                compositingStrategy = CompositingStrategy.ModulateAlpha
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconTile(icon = icon, tint = tint)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = AuraColors.Ink,
                                    fontSize = 24.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!subtitle.isNullOrBlank()) {
                                    Text(
                                        text = subtitle,
                                        color = AuraColors.InkSoft,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxContentHeight)
                                .graphicsLayer {
                                    alpha = bodyProgress
                                    translationY = (1f - bodyProgress) * -24f
                                    scaleX = 0.985f + bodyProgress * 0.015f
                                    scaleY = 0.985f + bodyProgress * 0.015f
                                    compositingStrategy = CompositingStrategy.ModulateAlpha
                                },
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            content(::close)
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.End)
                                .graphicsLayer {
                                    alpha = footerProgress
                                    translationY = (1f - footerProgress) * -12f
                                    scaleX = 0.99f + footerProgress * 0.01f
                                    scaleY = 0.99f + footerProgress * 0.01f
                                    compositingStrategy = CompositingStrategy.ModulateAlpha
                                },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (footer != null) {
                                footer(::close)
                            } else {
                                AnimatedTextButton(onClick = ::close) {
                                    Text(strings.close, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedNamesDialog(
    savedName: String,
    onSavedNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val savedNames = remember(savedName) { parseWatchedNames(savedName) }
    val savedNamesListState = rememberLazyListState()
    var pendingDeleteName by remember { mutableStateOf<String?>(null) }
    var clearAllRequested by remember { mutableStateOf(false) }
    NaturalPopupDialog(
        title = if (strings === KoreanAppStrings) "\uc800\uc7a5\ub41c \uc774\ub984" else "Saved Names",
        subtitle = if (strings === KoreanAppStrings) {
            "\ucd1d ${savedNames.size}/$MAX_WATCHED_NAMES"
        } else {
            "Total ${savedNames.size}/$MAX_WATCHED_NAMES"
        },
        icon = Icons.Rounded.Person,
        tint = AuraColors.Name,
        onDismiss = onDismiss,
        maxContentHeight = 250.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (strings === KoreanAppStrings) {
                        "\uc774\ub984\uc744 \uc800\uc7a5\ud558\uba74 \uc8fc\ubcc0 \uc18c\ub9ac \uac10\uc9c0\uc5d0 \uc5f0\ub3d9\ub429\ub2c8\ub2e4"
                    } else {
                        "Saved names are linked to sound detection."
                    },
                    color = AuraColors.InkSoft,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                AnimatedTextButton(
                    enabled = savedNames.isNotEmpty(),
                    onClick = { clearAllRequested = true },
                ) {
                    Text(
                        text = if (strings === KoreanAppStrings) "\uc804\uccb4 \uc0ad\uc81c" else "Clear All",
                        color = if (savedNames.isNotEmpty()) AuraColors.Siren else AuraColors.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            if (savedNames.isEmpty()) {
                Text(
                    text = strings.none,
                    color = AuraColors.InkSoft,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                ScrollEdgeBlurFrame(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 188.dp),
                    backgroundColor = AuraColors.Panel,
                    edgeHeight = 48.dp,
                    edgeOutset = 12.dp,
                ) {
                    LazyColumn(
                        state = savedNamesListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 188.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(savedNames, key = { it }) { name ->
                            ScrollEdgeFadingItem(
                                listState = savedNamesListState,
                                itemKey = name,
                            ) {
                                SavedNameRow(
                                    name = name,
                                    onDelete = { pendingDeleteName = name },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    pendingDeleteName?.let { name ->
        ConfirmSavedNameDeleteDialog(
            message = name,
            onConfirm = {
                pendingDeleteName = null
                onSavedNameChange(serializeWatchedNames(savedNames.filterNot { it == name }))
            },
            onDismiss = { pendingDeleteName = null },
        )
    }
    if (clearAllRequested) {
        ConfirmSavedNameDeleteDialog(
            message = if (strings === KoreanAppStrings) {
                "\uc800\uc7a5\ub41c \uc774\ub984 \uc804\uccb4"
            } else {
                "All saved names"
            },
            onConfirm = {
                clearAllRequested = false
                onSavedNameChange("")
            },
            onDismiss = { clearAllRequested = false },
        )
    }
}

@Composable
private fun ConfirmSavedNameDeleteDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    var confirmRequested by remember { mutableStateOf(false) }
    NaturalPopupDialog(
        title = if (strings === KoreanAppStrings) "\uc0ad\uc81c\ud558\uc2dc\uaca0\uc2b5\ub2c8\uae4c?" else "Delete?",
        subtitle = message,
        icon = Icons.Rounded.Warning,
        tint = AuraColors.Siren,
        onDismiss = {
            if (confirmRequested) {
                onConfirm()
            } else {
                onDismiss()
            }
        },
        maxContentHeight = 96.dp,
        content = {
            Text(
                text = if (strings === KoreanAppStrings) {
                    "\uc0ad\uc81c\ud558\uba74 \ud638\ucd9c \uc774\ub984 \uac10\uc9c0\uc5d0\uc11c \uc81c\uc678\ub429\ub2c8\ub2e4."
                } else {
                    "Deleted names are removed from call-name detection."
                },
                color = AuraColors.InkSoft,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        footer = { close ->
            AnimatedTextButton(onClick = close) {
                Text(
                    text = if (strings === KoreanAppStrings) "\ucde8\uc18c" else "Cancel",
                    fontWeight = FontWeight.Bold,
                )
            }
            AnimatedTextButton(
                onClick = {
                    confirmRequested = true
                    close()
                },
            ) {
                Text(
                    text = if (strings === KoreanAppStrings) "\uc0ad\uc81c" else "Delete",
                    color = AuraColors.Siren,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
    )
}

@Composable
private fun SavedNameRow(
    name: String,
    onDelete: () -> Unit,
) {
    val deleteInteractionSource = remember(name) { MutableInteractionSource() }
    val deletePressed by deleteInteractionSource.collectIsPressedAsState()
    val deleteBackground by animateColorAsState(
        targetValue = if (deletePressed) AuraColors.Siren.copy(alpha = 0.18f) else AuraColors.Control,
        animationSpec = auraTween(AuraMotion.ControlMillis, exiting = !deletePressed),
        label = "saved-name-delete-bg",
    )
    val deleteBorder by animateColorAsState(
        targetValue = if (deletePressed) AuraColors.Siren.copy(alpha = 0.36f) else AuraColors.GlassEdge,
        animationSpec = auraTween(AuraMotion.ControlMillis, exiting = !deletePressed),
        label = "saved-name-delete-border",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuraColors.PanelSoft.copy(alpha = 0.72f), ObservatoryPanelShape)
            .border(1.dp, AuraColors.GlassEdge, ObservatoryPanelShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = null,
            tint = AuraColors.Name,
        )
        Text(
            text = name,
            color = AuraColors.Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onDelete,
            interactionSource = deleteInteractionSource,
            modifier = Modifier
                .animatedPressScale(deleteInteractionSource)
                .size(34.dp)
                .background(deleteBackground, ObservatoryTileShape)
                .border(1.dp, deleteBorder, ObservatoryTileShape),
        ) {
            Text(
                text = "-",
                color = AuraColors.Siren,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AlertSettingsContent(
    settings: AlertSettings,
    onSettingsChange: (AlertSettings) -> Unit,
) {
    val strings = LocalAppStrings.current
    ToggleRow(
        label = strings.vibration,
        checked = settings.vibrationEnabled,
        onCheckedChange = { onSettingsChange(settings.copy(vibrationEnabled = it)) },
    )
    ToggleRow(
        label = strings.screenFlash,
        checked = settings.screenFlashEnabled,
        onCheckedChange = { onSettingsChange(settings.copy(screenFlashEnabled = it)) },
    )
    ToggleRow(
        label = strings.keepScreenOn,
        checked = settings.keepScreenOn,
        onCheckedChange = { onSettingsChange(settings.copy(keepScreenOn = it)) },
    )
    ToggleRow(
        label = strings.allowAudioStorage,
        checked = settings.privacyAudioStorageEnabled,
        onCheckedChange = { onSettingsChange(settings.copy(privacyAudioStorageEnabled = it)) },
    )
}

@Composable
private fun SoundCategoryContent(
    settings: AlertSettings,
    onSettingsChange: (AlertSettings) -> Unit,
) {
    val strings = LocalAppStrings.current
    fun updateKind(kind: AlertKind, setting: SoundCategorySetting) {
        onSettingsChange(
            settings.copy(
                categorySettings = settings.categorySettings.toMutableMap().apply {
                    this[kind] = setting
                },
            ),
        )
    }

    detectableAlertKinds.forEach { kind ->
        val setting = settings.categorySetting(kind)
        val rowShape = RoundedCornerShape(18.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(exchangeSurfaceBrush(tint = kind.tint()), rowShape)
                .border(1.dp, AuraColors.GlassEdge, rowShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.alertTitle(kind),
                        color = AuraColors.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = strings.severityLabel(kind.defaultSeverity()),
                        color = AuraColors.InkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Switch(
                    checked = setting.enabled,
                    onCheckedChange = { enabled ->
                        updateKind(kind, setting.copy(enabled = enabled))
                    },
                )
            }
        }
    }
}

@Composable
private fun PersonalizationContent(
    settings: AlertSettings,
    recordingKind: AlertKind?,
    onTrainingSample: (AlertKind) -> Unit,
    onResetTraining: (AlertKind) -> Unit,
) {
    val strings = LocalAppStrings.current
    var resetTarget by remember { mutableStateOf<AlertKind?>(null) }

    calibrationKinds.forEach { kind ->
        val profile = settings.customProfile(kind)
        val isRecording = recordingKind == kind
        val recordInteractionSource = remember(kind) { MutableInteractionSource() }
        val rowShape = RoundedCornerShape(18.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(exchangeSurfaceBrush(tint = kind.tint(), selected = isRecording), rowShape)
                .border(1.dp, if (isRecording) kind.tint().copy(alpha = 0.28f) else AuraColors.GlassEdge, rowShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.alertTitle(kind),
                        color = AuraColors.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${strings.recordingCount(profile.sampleCount)} - ${strings.correctionPercent(profile.confidenceBoost)}",
                        color = AuraColors.InkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = { onTrainingSample(kind) },
                    enabled = recordingKind == null,
                    interactionSource = recordInteractionSource,
                    modifier = Modifier
                        .animatedPressScale(recordInteractionSource)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraColors.Control,
                        contentColor = AuraColors.Ink,
                    ),
                ) {
                    Text(
                        text = if (isRecording) strings.recording else strings.record,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                    )
                }
            }
            CalibrationResetBar(
                profile = profile,
                onReset = { resetTarget = kind },
            )
        }
    }

    val target = resetTarget
    if (target != null) {
        var resetConfirmed by remember(target) { mutableStateOf(false) }
        LiquidPopupDialog(
            title = strings.resetConfirmTitle,
            subtitle = strings.alertTitle(target),
            icon = Icons.Rounded.Refresh,
            tint = AuraColors.Siren,
            onDismiss = {
                if (resetConfirmed) {
                    onResetTraining(target)
                }
                resetTarget = null
            },
            maxContentHeight = 110.dp,
            content = {
                Text(
                    text = strings.resetConfirmText(target),
                    color = AuraColors.InkSoft,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            footer = { close ->
                AnimatedTextButton(onClick = close) {
                    Text(strings.cancel, fontWeight = FontWeight.Bold)
                }
                AnimatedTextButton(
                    onClick = {
                        resetConfirmed = true
                        close()
                    },
                ) {
                    Text(strings.reset, fontWeight = FontWeight.ExtraBold)
                }
            },
        )
    }
}

@Composable
private fun CalibrationResetBar(
    profile: CustomSoundProfile,
    onReset: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val progress = (profile.sampleCount / 5f).coerceIn(0f, 1f)
    val enabled = profile.sampleCount > 0 || profile.confidenceBoost > 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = auraTween(AuraMotion.ControlMillis),
        label = "calibration-reset-progress",
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(AuraColors.Control, RoundedCornerShape(99.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(8.dp)
                    .background(AuraColors.Name, RoundedCornerShape(99.dp)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            AnimatedTextButton(
                onClick = onReset,
                enabled = enabled,
                modifier = Modifier.height(36.dp),
            ) {
                Text(
                    text = strings.reset,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(exchangeSurfaceBrush(tint = AuraColors.Blue, selected = checked), RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (checked) AuraColors.Blue.copy(alpha = 0.22f) else AuraColors.GlassEdge,
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = AuraColors.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun FeedbackButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor by animateColorAsState(
        targetValue = if (selected) AuraColors.Ink else AuraColors.Control,
        animationSpec = auraTween(AuraMotion.ControlMillis),
        label = "feedback-button",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) AuraColors.OnAccent else AuraColors.Ink,
        animationSpec = auraTween(AuraMotion.ControlMillis),
        label = "feedback-content",
    )
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .animatedPressScale(interactionSource)
            .height(42.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DemoContent(onDemo: (AlertKind) -> Unit) {
    val strings = LocalAppStrings.current
    val demos = listOf(
        AlertKind.Doorbell,
        AlertKind.Siren,
        AlertKind.FireAlarm,
        AlertKind.Knock,
        AlertKind.BabyCry,
        AlertKind.WaterRunning,
        AlertKind.ApplianceBeep,
        AlertKind.PhoneRing,
        AlertKind.NameCall,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        demos.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { kind ->
                    DemoButton(
                        kind = kind,
                        text = strings.alertTitle(kind),
                        severity = strings.severityLabel(kind.defaultSeverity()),
                        modifier = Modifier.weight(1f),
                        onClick = { onDemo(kind) },
                    )
                }
                repeat(2 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EventHistoryContent(
    eventHistory: List<DetectedSoundEvent>,
    onEventFeedback: (DetectedSoundEvent, FeedbackStatus) -> Unit,
    onClearHistory: () -> Unit,
) {
    val strings = LocalAppStrings.current
    if (eventHistory.isEmpty()) {
        Text(
            text = strings.noHistory,
            color = AuraColors.Muted,
            fontSize = 14.sp,
        )
    } else {
        eventHistory.take(6).forEach { event ->
            HistoryRow(
                event = event,
                onEventFeedback = onEventFeedback,
            )
        }
        AnimatedTextButton(
            onClick = onClearHistory,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.clearHistory)
        }
    }
}

@Composable
private fun HistoryRow(
    event: DetectedSoundEvent,
    onEventFeedback: (DetectedSoundEvent, FeedbackStatus) -> Unit,
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                liquidGlassBrush(
                    listOf(
                        AuraColors.GlassTop,
                        event.tint().copy(alpha = 0.08f),
                        AuraColors.GlassBottom,
                    ),
                ),
                ObservatoryPanelShape,
            )
            .border(1.dp, event.tint().copy(alpha = 0.20f), ObservatoryPanelShape)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconTile(
                icon = event.icon(),
                tint = event.tint(),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.alertTitle(event.kind),
                    color = AuraColors.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatEventTime(event.id)} - ${strings.severityLabel(event.severity)} - ${strings.repeatCount(event.repeatCount)}",
                    color = AuraColors.InkSoft,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = scoreText(event.confidence),
                color = event.tint(),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FeedbackButton(
                text = strings.confidenceLabel(event.confidence),
                selected = false,
                modifier = Modifier.weight(1f),
                onClick = {},
            )
            FeedbackButton(
                text = if (event.feedback == FeedbackStatus.FalsePositive) {
                    strings.falsePositiveApplied
                } else {
                    strings.falsePositive
                },
                selected = event.feedback == FeedbackStatus.FalsePositive,
                modifier = Modifier.weight(1f),
                onClick = { onEventFeedback(event, FeedbackStatus.FalsePositive) },
            )
        }
    }
}

@Composable
private fun Panel(
    title: String,
    icon: ImageVector,
    tint: Color = AuraColors.Blue,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = ObservatoryPanelShape,
        border = BorderStroke(1.dp, AuraColors.GlassEdge),
        colors = CardDefaults.cardColors(containerColor = AuraColors.Panel),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .background(liquidGlassBrush()),
        ) {
            DepthLighting(
                tint = tint,
                raised = true,
                modifier = Modifier.matchParentSize(),
            )
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(AuraColors.PanelSoft, ObservatoryTileShape)
                            .border(1.dp, AuraColors.Hairline, ObservatoryTileShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, contentDescription = null, tint = AuraColors.InkSoft, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraColors.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    headerAction?.invoke()
                }
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun DemoButton(
    kind: AlertKind,
    text: String,
    severity: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tint = kind.tint()
    val cardShape = RoundedCornerShape(20.dp)
    val pressedOverlay by animateColorAsState(
        targetValue = exchangePressedColor(pressed, tint),
        animationSpec = auraTween(AuraMotion.ControlMillis, exiting = !pressed),
        label = "demo-card-press",
    )
    val border by animateColorAsState(
        targetValue = if (pressed) tint.copy(alpha = 0.48f) else AuraColors.GlassEdge,
        animationSpec = auraTween(AuraMotion.ControlMillis, exiting = !pressed),
        label = "demo-card-border",
    )

    Surface(
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, border),
        shadowElevation = 0.dp,
        modifier = modifier
            .height(112.dp)
            .animatedPressScale(interactionSource)
            .semantics {
                contentDescription = "$text, $severity"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(exchangeSurfaceBrush(tint = tint, selected = pressed))
                .background(pressedOverlay)
                .padding(12.dp),
        ) {
            Icon(
                imageVector = kind.icon(),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(22.dp),
            )
            Text(
                text = severity,
                color = tint,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(tint.copy(alpha = 0.12f), ObservatoryPillShape)
                    .border(1.dp, tint.copy(alpha = 0.24f), ObservatoryPillShape)
                    .padding(horizontal = 7.dp, vertical = 4.dp),
            )
            Text(
                text = text,
                color = AuraColors.Ink,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(AuraColors.PanelSoft, ObservatoryTileShape)
            .border(1.dp, tint.copy(alpha = 0.28f), ObservatoryTileShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun ScreenFlashOverlay(
    event: DetectedSoundEvent?,
    modifier: Modifier = Modifier,
) {
    if (event == null || event.kind == AlertKind.Idle) return

    Box(
        modifier = modifier.background(event.tint().copy(alpha = 0.12f)),
    )
}

private fun DetectedSoundEvent.icon(): ImageVector = kind.icon()

private fun AlertKind.icon(): ImageVector = when (this) {
    AlertKind.Doorbell,
    AlertKind.PhoneRing,
    AlertKind.ApplianceBeep -> Icons.Rounded.Notifications
    AlertKind.Siren,
    AlertKind.FireAlarm -> Icons.Rounded.Warning
    AlertKind.NameCall,
    AlertKind.Knock,
    AlertKind.BabyCry -> Icons.Rounded.Person
    AlertKind.WaterRunning,
    AlertKind.Idle -> Icons.Rounded.GraphicEq
}

@Composable
private fun DetectedSoundEvent.tint(): Color = kind.tint()

@Composable
private fun AlertKind.tint(): Color = when (this) {
    AlertKind.Doorbell,
    AlertKind.PhoneRing -> AuraColors.Doorbell
    AlertKind.Siren,
    AlertKind.FireAlarm -> AuraColors.Siren
    AlertKind.NameCall,
    AlertKind.Knock,
    AlertKind.BabyCry,
    AlertKind.WaterRunning -> AuraColors.Name
    AlertKind.ApplianceBeep -> AuraColors.Gold
    AlertKind.Idle -> AuraColors.Muted
}

private fun formatEventTime(timeMillis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date(timeMillis))

@Composable
private fun liquidGlassBrush(
    colors: List<Color>? = null,
    tint: Color = Color.Transparent,
): Brush {
    val palette = LocalAuraPalette.current
    return remember(colors, tint, palette.panel, palette.glassTop, palette.glassMiddle, palette.glassBottom) {
        val mixedTint = if (tint == Color.Transparent) {
            palette.panel
        } else {
            lerp(
                palette.glassMiddle,
                tint.copy(alpha = 1f),
                (tint.alpha * 1.35f).coerceIn(0.04f, 0.22f),
            )
        }
        Brush.verticalGradient(
            colors = colors ?: listOf(
                palette.glassTop,
                mixedTint,
                palette.glassMiddle,
                palette.glassBottom,
            ),
        )
    }
}

private fun scoreText(value: Float): String = "${(value.coerceIn(0f, 1f) * 100f).toInt()}%"
