package com.soundvisualization.accessibility

import androidx.compose.runtime.Immutable
import kotlin.math.roundToInt

const val STABLE_GLOBAL_SENSITIVITY = 0.75f
const val MAX_STABLE_CATEGORY_SENSITIVITY = 0.80f
private const val SENSITIVITY_STEP = 0.05f

fun Float.quantizedSensitivity(): Float =
    ((coerceIn(0f, 1f) / SENSITIVITY_STEP).roundToInt() * SENSITIVITY_STEP).coerceIn(0f, 1f)

fun Float.stableGlobalSensitivity(): Float =
    quantizedSensitivity().coerceIn(0f, STABLE_GLOBAL_SENSITIVITY)

fun Float.stableCategorySensitivity(): Float =
    quantizedSensitivity().coerceIn(0f, MAX_STABLE_CATEGORY_SENSITIVITY)

enum class AlertKind {
    Idle,
    Doorbell,
    Siren,
    FireAlarm,
    Knock,
    BabyCry,
    WaterRunning,
    ApplianceBeep,
    PhoneRing,
    NameCall,
}

enum class AlertSeverity(val label: String) {
    Info("\uC815\uBCF4"),
    Caution("\uC8FC\uC758"),
    Emergency("\uAE34\uAE09"),
}

enum class SoundDirection {
    Left,
    Front,
    Right,
    Unknown,
}

enum class FeedbackStatus(val label: String) {
    None("\uBBF8\uD655\uC778"),
    Correct("\uB9DE\uC74C"),
    FalsePositive("\uC544\uB2D8"),
    Missed("\uB193\uCE68"),
}

@Immutable
data class DetectedSoundEvent(
    val id: Long = System.currentTimeMillis(),
    val kind: AlertKind,
    val title: String,
    val subtitle: String,
    val confidence: Float,
    val direction: SoundDirection,
    val source: String,
    val severity: AlertSeverity = kind.defaultSeverity(),
    val repeatCount: Int = 1,
    val feedback: FeedbackStatus = FeedbackStatus.None,
)

@Immutable
data class SoundCategorySetting(
    val enabled: Boolean = true,
    val sensitivity: Float = STABLE_GLOBAL_SENSITIVITY,
)

@Immutable
data class CustomSoundProfile(
    val kind: AlertKind,
    val sampleCount: Int = 0,
    val lastTrainedAt: Long = 0L,
    val confidenceBoost: Float = 0f,
    val featureVector: List<Float> = emptyList(),
    val featureVectors: List<List<Float>> = emptyList(),
    val sampleFilePaths: List<String> = emptyList(),
)

@Immutable
data class AlertSettings(
    val sensitivity: Float = STABLE_GLOBAL_SENSITIVITY,
    val vibrationEnabled: Boolean = true,
    val screenFlashEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val alwaysOnEnabled: Boolean = false,
    val privacyAudioStorageEnabled: Boolean = false,
    val categorySettings: Map<AlertKind, SoundCategorySetting> = defaultCategorySettings(),
    val customProfiles: Map<AlertKind, CustomSoundProfile> = defaultCustomProfiles(),
)

@Immutable
data class TranslationLanguage(
    val label: String,
    val speechLanguageTag: String,
    val modelCode: String,
)

@Immutable
data class TranslationState(
    val enabled: Boolean = true,
    val source: TranslationLanguage = englishTranslationLanguage(),
    val target: TranslationLanguage = koreanTranslationLanguage(),
    val autoDetectSource: Boolean = false,
    val sourceText: String = "",
    val translatedText: String = "",
    val modelReady: Boolean = false,
)

@Immutable
data class SoundRuntimeState(
    val isAudioActive: Boolean = false,
    val isListening: Boolean = false,
    val level: Float = 0f,
    val peakLevel: Float = 0f,
    val ambientLevel: Float = 0f,
    val spectrumBands: List<Float> = emptyList(),
    val dominantFrequencyHz: Float = 0f,
    val direction: SoundDirection = SoundDirection.Unknown,
    val speechRecognitionAvailable: Boolean = false,
)

fun idleEvent() = DetectedSoundEvent(
    id = 0L,
    kind = AlertKind.Idle,
    title = "\uB300\uAE30 \uC911",
    subtitle = "\uC8FC\uBCC0 \uC18C\uB9AC\uB97C \uBD84\uC11D\uD569\uB2C8\uB2E4",
    confidence = 0f,
    direction = SoundDirection.Unknown,
    source = "system",
)

fun detectedEvent(
    kind: AlertKind,
    confidence: Float,
    direction: SoundDirection,
    source: String,
): DetectedSoundEvent {
    val copy = when (kind) {
        AlertKind.Doorbell -> kind.title() to "\uD604\uAD00\uC758 \uC9C4\uB3D9"
        AlertKind.Siren -> kind.title() to "\uBD89\uC740 \uACBD\uACE0"
        AlertKind.FireAlarm -> kind.title() to "\uC989\uC2DC \uD655\uC778 \uD544\uC694"
        AlertKind.Knock -> kind.title() to "\uBB38 \uC8FC\uBCC0 \uD655\uC778"
        AlertKind.BabyCry -> kind.title() to "\uB3CC\uBD04 \uD655\uC778 \uD544\uC694"
        AlertKind.WaterRunning -> kind.title() to "\uBB3C\uC774 \uD750\uB974\uB294 \uC18C\uB9AC"
        AlertKind.ApplianceBeep -> kind.title() to "\uAC00\uC804\uC81C\uD488 \uC54C\uB9BC"
        AlertKind.PhoneRing -> kind.title() to "\uC804\uD654\uBCA8 \uC54C\uB9BC"
        AlertKind.NameCall -> kind.title() to "\uC800\uC7A5\uB41C \uC774\uB984 \uD638\uCD9C"
        AlertKind.Idle -> "\uB300\uAE30 \uC911" to "\uC8FC\uBCC0 \uC18C\uB9AC\uB97C \uBD84\uC11D\uD569\uB2C8\uB2E4"
    }

    return DetectedSoundEvent(
        kind = kind,
        title = copy.first,
        subtitle = copy.second,
        confidence = confidence.coerceIn(0f, 1f),
        direction = direction,
        source = source,
        severity = kind.defaultSeverity(),
    )
}

fun SoundDirection.label(): String = when (this) {
    SoundDirection.Left -> "\uC67C\uCABD"
    SoundDirection.Front -> "\uC815\uBA74"
    SoundDirection.Right -> "\uC624\uB978\uCABD"
    SoundDirection.Unknown -> "\uBC29\uD5A5 \uD655\uC778 \uC911"
}

private val koreanLanguage = TranslationLanguage("\uD55C\uAD6D\uC5B4", "ko-KR", "ko")
private val englishLanguage = TranslationLanguage("\uC601\uC5B4", "en-US", "en")
private val japaneseLanguage = TranslationLanguage("\uC77C\uBCF8\uC5B4", "ja-JP", "ja")

val supportedTranslationLanguages = listOf(
    koreanLanguage,
    englishLanguage,
)

val autoDetectTranslationLanguages = listOf(
    koreanLanguage,
    englishLanguage,
    japaneseLanguage,
)

val supportedInputTranslationLanguages = autoDetectTranslationLanguages

fun koreanTranslationLanguage(): TranslationLanguage =
    supportedInputTranslationLanguages.first { it.modelCode == "ko" }

fun englishTranslationLanguage(): TranslationLanguage =
    supportedInputTranslationLanguages.first { it.modelCode == "en" }

fun japaneseTranslationLanguage(): TranslationLanguage =
    supportedInputTranslationLanguages.first { it.modelCode == "ja" }

fun normalizedTranslationTarget(language: TranslationLanguage): TranslationLanguage =
    supportedTranslationLanguages.firstOrNull { it.modelCode == language.modelCode }
        ?: koreanTranslationLanguage()

fun normalizedTranslationSource(language: TranslationLanguage): TranslationLanguage =
    supportedInputTranslationLanguages.firstOrNull { it.modelCode == language.modelCode }
        ?: englishTranslationLanguage()

fun translationLanguageForTag(tag: String?): TranslationLanguage? {
    val primary = tag
        ?.substringBefore(',')
        ?.substringBefore('-')
        ?.substringBefore('_')
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    return autoDetectTranslationLanguages.firstOrNull { language ->
        language.modelCode == primary ||
            language.speechLanguageTag.substringBefore('-').lowercase() == primary
    }
}

fun resolveTranslationLanguage(
    text: String,
    detectedLanguageTag: String? = null,
    fallback: TranslationLanguage? = null,
): TranslationLanguage? {
    val detected = translationLanguageForTag(detectedLanguageTag)
    val scores = TranslationLanguageScores()

    when (detected?.modelCode) {
        "ko" -> scores.korean += 3
        "ja" -> scores.japanese += 3
        "en" -> scores.english += 3
    }

    scoreTextLanguage(text, scores)

    val best = when {
        scores.korean >= scores.japanese + 3 && scores.korean >= scores.english + 2 -> "ko"
        scores.japanese >= scores.korean + 3 && scores.japanese >= scores.english + 2 -> "ja"
        scores.english >= scores.korean + 3 && scores.english >= scores.japanese + 3 -> "en"
        detected != null && scores.hasSignalFor(detected.modelCode) -> detected.modelCode
        scores.korean > 0 && scores.korean >= scores.japanese -> "ko"
        scores.japanese > 0 -> "ja"
        scores.english > 0 -> "en"
        else -> null
    }

    return translationLanguageForTag(best) ?: detected ?: fallback
}

fun inferTranslationLanguageFromText(text: String): TranslationLanguage? =
    resolveTranslationLanguage(text)

private data class TranslationLanguageScores(
    var korean: Int = 0,
    var japanese: Int = 0,
    var english: Int = 0,
) {
    fun hasSignalFor(code: String): Boolean = when (code) {
        "ko" -> korean > 0
        "ja" -> japanese > 0
        "en" -> english > 0
        else -> false
    }
}

private fun scoreTextLanguage(text: String, scores: TranslationLanguageScores) {
    val trimmed = text.trim()
    if (trimmed.isBlank()) return

    var hangul = 0
    var kana = 0
    var han = 0
    var latin = 0

    for (char in trimmed) {
        when {
            char.isHangul() -> hangul += 1
            char.isKana() -> kana += 1
            char.isHan() -> han += 1
            char in 'A'..'Z' || char in 'a'..'z' -> latin += 1
        }
    }

    scores.korean += hangul * 5
    scores.japanese += kana * 6
    scores.english += latin * 2

    if (kana > 0) {
        scores.japanese += 8
    }
    if (hangul > 0) {
        scores.korean += 8
    }
    if (han > 0 && hangul == 0) {
        scores.japanese += 3 + han
    }

    val compact = trimmed.replace(languageWhitespaceRegex, "")
    koreanLanguageHints.forEach { hint ->
        if (compact.contains(hint)) scores.korean += 2
    }
    japaneseLanguageHints.forEach { hint ->
        if (compact.contains(hint)) scores.japanese += 2
    }
    koreanEndingHints.forEach { ending ->
        if (compact.endsWith(ending)) scores.korean += 3
    }
    japaneseEndingHints.forEach { ending ->
        if (compact.endsWith(ending)) scores.japanese += 3
    }
}

private fun Char.isHangul(): Boolean =
    this in '\uAC00'..'\uD7AF' ||
        this in '\u1100'..'\u11FF' ||
        this in '\u3130'..'\u318F' ||
        this in '\uA960'..'\uA97F' ||
        this in '\uD7B0'..'\uD7FF'

private fun Char.isKana(): Boolean =
    this in '\u3040'..'\u30FF' ||
        this in '\u31F0'..'\u31FF' ||
        this in '\uFF66'..'\uFF9D'

private fun Char.isHan(): Boolean =
    this in '\u3400'..'\u9FFF' ||
        this in '\uF900'..'\uFAFF'

private val languageWhitespaceRegex = Regex("\\s+")

private val koreanLanguageHints = listOf(
    "\uC740", "\uB294", "\uC774", "\uAC00", "\uC744", "\uB97C",
    "\uC5D0\uC11C", "\uC5D0\uAC8C", "\uC73C\uB85C", "\uD558\uACE0",
    "\uBB50", "\uC5B4\uB514", "\uC5B8\uC81C", "\uB204\uAD6C", "\uC65C",
    "\uC5B4\uB5BB\uAC8C", "\uC548\uB155", "\uAC10\uC0AC", "\uAD1C\uCC2E",
)

private val koreanEndingHints = listOf(
    "\uC694", "\uB2C8\uB2E4", "\uB2C8\uAE4C", "\uC138\uC694",
    "\uC608\uC694", "\uC774\uC5D0\uC694", "\uAC70\uC57C", "\uD588\uC5B4",
    "\uB410\uC5B4", "\uC788\uC5B4", "\uC5C6\uC5B4", "\uB124\uC694", "\uC8E0",
)

private val japaneseLanguageHints = listOf(
    "\u3067\u3059", "\u307E\u3059", "\u3053\u308C", "\u305D\u308C",
    "\u3042\u308A\u304C\u3068\u3046", "\u3053\u3093\u306B\u3061\u306F",
    "\u3053\u3093\u3070\u3093\u306F", "\u3059\u307F\u307E\u305B\u3093",
    "\u304F\u3060\u3055\u3044", "\u3067\u3059\u304B", "\u3067\u3057\u3087\u3046",
    "\u3058\u3083\u306A\u3044", "\u3060\u3063\u305F", "\u3057\u305F", "\u3057\u3066",
)

private val japaneseEndingHints = listOf(
    "\u3067\u3059", "\u307E\u3059", "\u307E\u305B\u3093", "\u3067\u3057\u305F",
    "\u304F\u3060\u3055\u3044", "\u3067\u3057\u3087\u3046", "\u3067\u3059\u306D",
    "\u3067\u3059\u3088", "\u304B\u306A", "\u3060", "\u3088", "\u306D",
    "\u304B", "\u305F",
)

val detectableAlertKinds = listOf(
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

fun defaultCategorySettings(): Map<AlertKind, SoundCategorySetting> =
    detectableAlertKinds.associateWith { kind ->
        SoundCategorySetting(
            enabled = true,
            sensitivity = kind.defaultSensitivity(),
        )
    }

fun defaultCustomProfiles(): Map<AlertKind, CustomSoundProfile> =
    detectableAlertKinds.associateWith { kind -> CustomSoundProfile(kind = kind) }

fun AlertKind.title(): String = when (this) {
    AlertKind.Idle -> "\uB300\uAE30 \uC911"
    AlertKind.Doorbell -> "\uCD08\uC778\uC885"
    AlertKind.Siren -> "\uC0AC\uC774\uB80C"
    AlertKind.FireAlarm -> "\uD654\uC7AC/\uC5F0\uAE30 \uACBD\uBCF4"
    AlertKind.Knock -> "\uB178\uD06C"
    AlertKind.BabyCry -> "\uC544\uAE30 \uC6B8\uC74C"
    AlertKind.WaterRunning -> "\uBB3C \uD750\uB984"
    AlertKind.ApplianceBeep -> "\uAC00\uC804\uC81C\uD488 \uC54C\uB9BC\uC74C"
    AlertKind.PhoneRing -> "\uC804\uD654\uBCA8"
    AlertKind.NameCall -> "\uC774\uB984 \uD638\uCD9C"
}

fun AlertKind.defaultSeverity(): AlertSeverity = when (this) {
    AlertKind.Siren,
    AlertKind.FireAlarm -> AlertSeverity.Emergency
    AlertKind.BabyCry,
    AlertKind.WaterRunning,
    AlertKind.Knock,
    AlertKind.NameCall -> AlertSeverity.Caution
    AlertKind.Doorbell,
    AlertKind.ApplianceBeep,
    AlertKind.PhoneRing,
    AlertKind.Idle -> AlertSeverity.Info
}

fun AlertKind.defaultSensitivity(): Float = when (this) {
    AlertKind.Siren,
    AlertKind.FireAlarm -> MAX_STABLE_CATEGORY_SENSITIVITY
    AlertKind.NameCall,
    AlertKind.Knock,
    AlertKind.BabyCry,
    AlertKind.Doorbell -> 0.75f
    AlertKind.WaterRunning,
    AlertKind.PhoneRing,
    AlertKind.ApplianceBeep -> 0.70f
    AlertKind.Idle -> 0f
}

fun AlertKind.needsDirection(): Boolean = false

fun DetectedSoundEvent.confidenceLabel(): String = when {
    confidence >= 0.82f -> "\uD655\uC2E4\uD568"
    confidence >= 0.58f -> "\uAC00\uB2A5\uC131 \uB192\uC74C"
    else -> "\uAC00\uB2A5\uC131 \uC788\uC74C"
}

fun AlertSettings.categorySetting(kind: AlertKind): SoundCategorySetting =
    categorySettings[kind] ?: SoundCategorySetting(
        enabled = kind != AlertKind.Idle,
        sensitivity = kind.defaultSensitivity(),
    )

fun AlertSettings.customProfile(kind: AlertKind): CustomSoundProfile =
    customProfiles[kind] ?: CustomSoundProfile(kind = kind)
