package com.soundvisualization.accessibility

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage {
    Korean,
    English;

    val strings: AppStrings
        get() = when (this) {
            Korean -> KoreanAppStrings
            English -> EnglishAppStrings
        }

    fun toggled(): AppLanguage = if (this == Korean) English else Korean

    companion object {
        fun fromStoredValue(value: String?): AppLanguage =
            entries.firstOrNull { it.name == value } ?: Korean
    }
}

val LocalAppStrings = staticCompositionLocalOf { KoreanAppStrings }

@Immutable
data class AppStrings(
    val appLanguage: String,
    val koreanToggle: String,
    val englishToggle: String,
    val status: String,
    val listening: String,
    val waiting: String,
    val recent: String,
    val none: String,
    val sensitivity: String,
    val detectionPanel: String,
    val stop: String,
    val startDetection: String,
    val resetState: String,
    val watchedName: String,
    val watchedNameHelp: String,
    val volume: String,
    val peak: String,
    val ambientNoise: String,
    val direction: String,
    val name: String,
    val available: String,
    val unsupported: String,
    val translationPanel: String,
    val autoTranslation: String,
    val startSpeech: String,
    val stopSpeech: String,
    val inputLanguage: String,
    val targetLanguage: String,
    val autoDetect: String,
    val original: String,
    val originalPlaceholder: String,
    val translation: String,
    val translationPlaceholder: String,
    val speechHistory: String,
    val selectTranslationLanguage: String,
    val noSpeechHistory: String,
    val close: String,
    val alertSettings: String,
    val alwaysOn: String,
    val vibration: String,
    val screenFlash: String,
    val keepScreenOn: String,
    val allowAudioStorage: String,
    val soundCategories: String,
    val calibration: String,
    val record: String,
    val recording: String,
    val correction: String,
    val reset: String,
    val cancel: String,
    val resetConfirmTitle: String,
    val resetConfirmSuffix: String,
    val test: String,
    val eventHistory: String,
    val noHistory: String,
    val clearHistory: String,
    val correct: String,
    val wrong: String,
    val falsePositive: String,
    val falsePositiveApplied: String,
    val detectedSuffix: String,
    val monitorTitle: String,
    val monitorMessage: String,
    val recentDetectionPrefix: String,
    val stopAlwaysOn: String,
    val monitorChannelName: String,
    val monitorChannelDescription: String,
    val alertChannelName: String,
    val alertChannelDescription: String,
) {
    fun t(korean: String, english: String): String =
        if (this === KoreanAppStrings) korean else english

    fun modeTitle(mode: AppMode): String = when (mode) {
        AppMode.SoundDetection -> t("주변 소리 감지", "Sound Detection")
        AppMode.SpeechRecognition -> t("실시간 음성 인식", "Live Speech")
        AppMode.SoundManagement -> t("보정과 기록", "Calibration")
    }

    fun alertTitle(kind: AlertKind): String = when (kind) {
        AlertKind.Idle -> waiting
        AlertKind.Doorbell -> t("초인종", "Doorbell")
        AlertKind.Siren -> t("사이렌", "Siren")
        AlertKind.FireAlarm -> t("화재/연기 경보", "Fire/Smoke Alarm")
        AlertKind.Knock -> t("노크", "Knock")
        AlertKind.BabyCry -> t("아기 울음", "Baby Cry")
        AlertKind.WaterRunning -> t("물 흐름", "Water Running")
        AlertKind.ApplianceBeep -> t("가전제품 알림음", "Appliance Alert")
        AlertKind.PhoneRing -> t("전화벨", "Phone Ring")
        AlertKind.NameCall -> t("이름 호출", "Name Call")
    }

    fun alertSubtitle(kind: AlertKind): String = when (kind) {
        AlertKind.Idle -> t("주변 소리를 분석합니다", "Analyzing nearby sound")
        AlertKind.Doorbell -> t("파란 원형 파동", "Blue pulse wave")
        AlertKind.Siren -> t("붉은 경고", "Red warning")
        AlertKind.FireAlarm -> t("즉시 확인 필요", "Check immediately")
        AlertKind.Knock -> t("문 쪽 소리", "Door sound")
        AlertKind.BabyCry -> t("보호자 확인 필요", "Needs caregiver attention")
        AlertKind.WaterRunning -> t("물이 흐르는 소리", "Running water sound")
        AlertKind.ApplianceBeep -> t("가전제품 알림", "Appliance notification")
        AlertKind.PhoneRing -> t("전화벨 알림", "Phone ring alert")
        AlertKind.NameCall -> t("호출 이름 감지", "Name call detected")
    }

    fun directionLabel(direction: SoundDirection): String = when (direction) {
        SoundDirection.Left -> t("왼쪽", "Left")
        SoundDirection.Front -> t("앞", "Front")
        SoundDirection.Right -> t("오른쪽", "Right")
        SoundDirection.Unknown -> t("방향 확인 중", "Locating")
    }

    fun severityLabel(severity: AlertSeverity): String = when (severity) {
        AlertSeverity.Info -> t("정보", "Info")
        AlertSeverity.Caution -> t("주의", "Caution")
        AlertSeverity.Emergency -> t("긴급", "Emergency")
    }

    fun confidenceLabel(confidence: Float): String = when {
        confidence >= 0.82f -> t("확실함", "Certain")
        confidence >= 0.58f -> t("가능성 높음", "Likely")
        else -> t("가능성 있음", "Possible")
    }

    fun languageName(language: TranslationLanguage): String =
        languageName(language.modelCode)

    fun languageName(code: String): String = when (code) {
        "ko" -> t("한국어", "Korean")
        "en" -> t("영어", "English")
        "ja" -> t("일본어", "Japanese")
        else -> code.uppercase()
    }

    fun repeatCount(count: Int): String =
        if (this === KoreanAppStrings) "${count}회" else "${count}x"

    fun correctionPercent(value: Float): String =
        if (this === KoreanAppStrings) "보정 ${(value * 100f).toInt()}%" else "Boost ${(value * 100f).toInt()}%"

    fun recordingCount(count: Int): String =
        if (this === KoreanAppStrings) "녹음 $count/5" else "Samples $count/5"

    fun resetConfirmText(kind: AlertKind): String =
        if (this === KoreanAppStrings) {
            "${alertTitle(kind)} 맞춤 보정이 초기화됩니다."
        } else {
            "${alertTitle(kind)} calibration will be reset."
        }
}

val KoreanAppStrings = AppStrings(
    appLanguage = "앱 언어",
    koreanToggle = "한글",
    englishToggle = "EN",
    status = "상태",
    listening = "감지 중",
    waiting = "대기",
    recent = "최근",
    none = "없음",
    sensitivity = "민감도",
    detectionPanel = "감지",
    stop = "정지",
    startDetection = "감지 시작",
    resetState = "상태 초기화",
    watchedName = "호출 이름",
    watchedNameHelp = "쉼표로 여러 이름을 구분",
    volume = "음량",
    peak = "피크",
    ambientNoise = "환경 소음",
    direction = "방향",
    name = "이름",
    available = "사용 가능",
    unsupported = "미지원",
    translationPanel = "실시간 대화 번역",
    autoTranslation = "자동 번역",
    startSpeech = "음성 인식 시작",
    stopSpeech = "음성 인식 정지",
    inputLanguage = "입력 언어",
    targetLanguage = "번역 언어",
    autoDetect = "자동 감지",
    original = "원문",
    originalPlaceholder = "말이 인식되면 여기에 표시됩니다",
    translation = "번역",
    translationPlaceholder = "번역 결과 대기 중",
    speechHistory = "과거 음성 인식 내용",
    selectTranslationLanguage = "번역 언어 선택",
    noSpeechHistory = "아직 인식된 문장이 없습니다",
    close = "닫기",
    alertSettings = "알림 설정",
    alwaysOn = "상시 감지",
    vibration = "진동",
    screenFlash = "화면 플래시",
    keepScreenOn = "화면 켜짐 유지",
    allowAudioStorage = "오디오 저장 허용",
    soundCategories = "소리별 감지",
    calibration = "정확도 보정",
    record = "녹음",
    recording = "녹음 중",
    correction = "보정",
    reset = "리셋",
    cancel = "취소",
    resetConfirmTitle = "리셋하시겠습니까?",
    resetConfirmSuffix = "맞춤 보정이 초기화됩니다.",
    test = "테스트",
    eventHistory = "이벤트 기록",
    noHistory = "기록 없음",
    clearHistory = "기록 지우기",
    correct = "맞음",
    wrong = "아님",
    falsePositive = "오탐",
    falsePositiveApplied = "오탐 반영됨",
    detectedSuffix = "감지",
    monitorTitle = "상시 감지 중",
    monitorMessage = "주변 소리를 기기 안에서 분석합니다",
    recentDetectionPrefix = "최근 감지",
    stopAlwaysOn = "상시 감지 끄기",
    monitorChannelName = "상시 소리 감지",
    monitorChannelDescription = "백그라운드에서 주변 소리를 감지할 때 표시됩니다",
    alertChannelName = "중요 소리 알림",
    alertChannelDescription = "사이렌, 화재/연기 경보, 초인종을 감지하면 즉시 알립니다",
)

val EnglishAppStrings = AppStrings(
    appLanguage = "App Language",
    koreanToggle = "KR",
    englishToggle = "EN",
    status = "Status",
    listening = "Listening",
    waiting = "Standby",
    recent = "Recent",
    none = "None",
    sensitivity = "Sensitivity",
    detectionPanel = "Detection",
    stop = "Stop",
    startDetection = "Start",
    resetState = "Reset status",
    watchedName = "Call Name",
    watchedNameHelp = "Add names that should trigger an alert",
    volume = "Volume",
    peak = "Peak",
    ambientNoise = "Ambient Noise",
    direction = "Direction",
    name = "Name",
    available = "Available",
    unsupported = "Unsupported",
    translationPanel = "Live Conversation Translation",
    autoTranslation = "Auto Translation",
    startSpeech = "Start Speech Recognition",
    stopSpeech = "Stop Speech Recognition",
    inputLanguage = "Input Language",
    targetLanguage = "Target Language",
    autoDetect = "Auto Detect",
    original = "Original",
    originalPlaceholder = "Recognized speech will appear here",
    translation = "Translation",
    translationPlaceholder = "Waiting for translation",
    speechHistory = "Speech History",
    selectTranslationLanguage = "Select Target Language",
    noSpeechHistory = "No recognized sentences yet",
    close = "Close",
    alertSettings = "Alert Settings",
    alwaysOn = "Always-on Detection",
    vibration = "Vibration",
    screenFlash = "Screen Flash",
    keepScreenOn = "Keep Screen On",
    allowAudioStorage = "Allow Audio Storage",
    soundCategories = "Sound Categories",
    calibration = "Accuracy Calibration",
    record = "Record",
    recording = "Recording",
    correction = "Boost",
    reset = "Reset",
    cancel = "Cancel",
    resetConfirmTitle = "Reset calibration?",
    resetConfirmSuffix = "custom calibration will be reset.",
    test = "Test",
    eventHistory = "Event History",
    noHistory = "No history",
    clearHistory = "Clear History",
    correct = "Correct",
    wrong = "Wrong",
    falsePositive = "False Alert",
    falsePositiveApplied = "False Alert Applied",
    detectedSuffix = "Detected",
    monitorTitle = "Always-on Detection",
    monitorMessage = "Analyzing nearby sound on device",
    recentDetectionPrefix = "Recent",
    stopAlwaysOn = "Turn Off",
    monitorChannelName = "Always-on Sound Detection",
    monitorChannelDescription = "Shown while nearby sound is detected in the background",
    alertChannelName = "Critical Sound Alerts",
    alertChannelDescription = "Alerts immediately for sirens, fire/smoke alarms, and doorbells",
)
