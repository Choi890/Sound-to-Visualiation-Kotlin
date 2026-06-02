package com.soundvisualization.accessibility

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal const val MAX_WATCHED_NAMES = 9

private val WATCHED_NAME_DELIMITERS = charArrayOf(',', '/', ';', '\n')

internal fun parseWatchedNames(raw: String): List<String> =
    normalizeWatchedNameList(raw.split(*WATCHED_NAME_DELIMITERS))

internal fun serializeWatchedNames(names: Iterable<String>): String =
    normalizeWatchedNameList(names).joinToString("\n")

internal fun mergeWatchedNames(existingRaw: String, inputRaw: String): String =
    serializeWatchedNames(parseWatchedNames(existingRaw) + parseWatchedNames(inputRaw))

private fun normalizeWatchedNameList(names: Iterable<String>): List<String> {
    val keys = LinkedHashSet<String>()
    val normalized = ArrayList<String>(MAX_WATCHED_NAMES)
    for (name in names) {
        val candidate = name.trim()
        val key = candidate.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
        if (candidate.isBlank() || key.isBlank() || !keys.add(key)) continue
        normalized += candidate
        if (normalized.size == MAX_WATCHED_NAMES) break
    }
    return normalized
}

object LocalAppStore {
    private const val PREFS_NAME = "sound_visualization_local_store"
    private const val KEY_SETTINGS = "alert_settings"
    private const val KEY_EVENTS = "event_history"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_APP_COLOR_MODE = "app_color_mode"
    private const val KEY_WATCHED_NAME = "watched_name"
    private const val CURRENT_SENSITIVITY_PROFILE_VERSION = 3
    private const val MAX_EVENTS = 80
    private const val REPEAT_WINDOW_MS = 8000L

    @Synchronized
    fun loadAlertSettings(context: Context): AlertSettings {
        val raw = prefs(context).getString(KEY_SETTINGS, null) ?: return AlertSettings()
        return runCatching {
            val json = JSONObject(raw)
            val defaults = AlertSettings()
            val upgradeLegacySensitivity =
                json.optInt("sensitivityProfileVersion", 0) < CURRENT_SENSITIVITY_PROFILE_VERSION
            defaults.copy(
                sensitivity = upgradedSensitivity(
                    value = json.optDouble("sensitivity", defaults.sensitivity.toDouble()).toFloat(),
                    minimum = defaults.sensitivity,
                    maximum = STABLE_GLOBAL_SENSITIVITY,
                    upgrade = upgradeLegacySensitivity,
                ),
                vibrationEnabled = json.optBoolean("vibrationEnabled", defaults.vibrationEnabled),
                screenFlashEnabled = json.optBoolean("screenFlashEnabled", defaults.screenFlashEnabled),
                keepScreenOn = json.optBoolean("keepScreenOn", defaults.keepScreenOn),
                alwaysOnEnabled = json.optBoolean("alwaysOnEnabled", defaults.alwaysOnEnabled),
                privacyAudioStorageEnabled = json.optBoolean(
                    "privacyAudioStorageEnabled",
                    defaults.privacyAudioStorageEnabled,
                ),
                categorySettings = decodeCategorySettings(
                    json = json.optJSONObject("categorySettings"),
                    upgradeLegacySensitivity = upgradeLegacySensitivity,
                ),
                customProfiles = decodeCustomProfiles(json.optJSONObject("customProfiles")),
            )
        }.getOrDefault(AlertSettings())
    }

    @Synchronized
    fun saveAlertSettings(context: Context, settings: AlertSettings) {
        val json = JSONObject()
            .put("sensitivityProfileVersion", CURRENT_SENSITIVITY_PROFILE_VERSION)
            .put("sensitivity", settings.sensitivity.stableGlobalSensitivity().toDouble())
            .put("vibrationEnabled", settings.vibrationEnabled)
            .put("screenFlashEnabled", settings.screenFlashEnabled)
            .put("keepScreenOn", settings.keepScreenOn)
            .put("alwaysOnEnabled", settings.alwaysOnEnabled)
            .put("privacyAudioStorageEnabled", settings.privacyAudioStorageEnabled)
            .put("categorySettings", encodeCategorySettings(settings.categorySettings))
            .put("customProfiles", encodeCustomProfiles(settings.customProfiles))

        prefs(context).edit { putString(KEY_SETTINGS, json.toString()) }
    }

    @Synchronized
    fun loadAppLanguage(context: Context): AppLanguage =
        AppLanguage.fromStoredValue(prefs(context).getString(KEY_APP_LANGUAGE, null))

    @Synchronized
    fun saveAppLanguage(context: Context, language: AppLanguage) {
        prefs(context).edit { putString(KEY_APP_LANGUAGE, language.name) }
    }

    @Synchronized
    fun loadAppColorMode(context: Context): AppColorMode =
        AppColorMode.fromStoredValue(prefs(context).getString(KEY_APP_COLOR_MODE, null))

    @Synchronized
    fun saveAppColorMode(context: Context, colorMode: AppColorMode) {
        prefs(context).edit { putString(KEY_APP_COLOR_MODE, colorMode.name) }
    }

    @Synchronized
    fun loadWatchedName(context: Context): String =
        serializeWatchedNames(parseWatchedNames(prefs(context).getString(KEY_WATCHED_NAME, "").orEmpty()))

    @Synchronized
    fun saveWatchedName(context: Context, value: String) {
        prefs(context).edit {
            putString(KEY_WATCHED_NAME, serializeWatchedNames(parseWatchedNames(value)))
        }
    }

    @Synchronized
    fun loadEvents(context: Context): List<DetectedSoundEvent> {
        val raw = prefs(context).getString(KEY_EVENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    decodeEvent(array.optJSONObject(index))?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun saveEvents(context: Context, events: List<DetectedSoundEvent>) {
        val array = JSONArray()
        events.take(MAX_EVENTS).forEach { event -> array.put(encodeEvent(event)) }
        prefs(context).edit { putString(KEY_EVENTS, array.toString()) }
    }

    @Synchronized
    fun appendEvent(context: Context, event: DetectedSoundEvent): List<DetectedSoundEvent> {
        val current = loadEvents(context)
        val latest = current.firstOrNull()
        val next = if (
            latest != null &&
            latest.kind == event.kind &&
            event.id - latest.id <= REPEAT_WINDOW_MS
        ) {
            val merged = latest.copy(
                id = event.id,
                title = event.title,
                subtitle = event.subtitle,
                confidence = maxOf(latest.confidence, event.confidence),
                direction = SoundDirection.Unknown,
                source = event.source,
                severity = event.severity,
                repeatCount = latest.repeatCount + 1,
                feedback = latest.feedback,
            )
            listOf(merged) + current.drop(1)
        } else {
            listOf(event) + current
        }.take(MAX_EVENTS)

        saveEvents(context, next)
        return next
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun encodeCategorySettings(settings: Map<AlertKind, SoundCategorySetting>): JSONObject {
        val json = JSONObject()
        detectableAlertKinds.forEach { kind ->
            val setting = settings[kind] ?: SoundCategorySetting(sensitivity = kind.defaultSensitivity())
            json.put(
                kind.name,
                JSONObject()
                    .put("enabled", setting.enabled)
                    .put("sensitivity", setting.sensitivity.stableCategorySensitivity().toDouble()),
            )
        }
        return json
    }

    private fun decodeCategorySettings(
        json: JSONObject?,
        upgradeLegacySensitivity: Boolean = false,
    ): Map<AlertKind, SoundCategorySetting> {
        val defaults = defaultCategorySettings()
        if (json == null) return defaults

        return detectableAlertKinds.associateWith { kind ->
            val item = json.optJSONObject(kind.name)
            val default = defaults[kind] ?: SoundCategorySetting(sensitivity = kind.defaultSensitivity())
            if (item == null) {
                default
            } else {
                default.copy(
                    enabled = item.optBoolean("enabled", default.enabled),
                    sensitivity = upgradedSensitivity(
                        value = item.optDouble("sensitivity", default.sensitivity.toDouble()).toFloat(),
                        minimum = default.sensitivity,
                        maximum = MAX_STABLE_CATEGORY_SENSITIVITY,
                        upgrade = upgradeLegacySensitivity,
                    ),
                )
            }
        }
    }

    private fun upgradedSensitivity(
        value: Float,
        minimum: Float,
        maximum: Float,
        upgrade: Boolean,
    ): Float {
        val normalized = value.coerceIn(0f, 1f)
        return (if (upgrade) maxOf(normalized, minimum) else normalized)
            .quantizedSensitivity()
            .coerceIn(0f, maximum)
    }

    private fun encodeCustomProfiles(profiles: Map<AlertKind, CustomSoundProfile>): JSONObject {
        val json = JSONObject()
        detectableAlertKinds.forEach { kind ->
            val profile = profiles[kind] ?: CustomSoundProfile(kind = kind)
            json.put(
                kind.name,
                JSONObject()
                    .put("sampleCount", profile.sampleCount)
                    .put("lastTrainedAt", profile.lastTrainedAt)
                    .put("confidenceBoost", profile.confidenceBoost.toDouble())
                    .put("featureVector", encodeFloatList(profile.featureVector))
                    .put("featureVectors", encodeFloatMatrix(profile.featureVectors))
                    .put("sampleFilePaths", encodeStringList(profile.sampleFilePaths)),
            )
        }
        return json
    }

    private fun decodeCustomProfiles(json: JSONObject?): Map<AlertKind, CustomSoundProfile> {
        val defaults = defaultCustomProfiles()
        if (json == null) return defaults

        return detectableAlertKinds.associateWith { kind ->
            val item = json.optJSONObject(kind.name)
            val default = defaults[kind] ?: CustomSoundProfile(kind = kind)
            if (item == null) {
                default
            } else {
                val mergedFeatureVector = decodeFloatList(item.optJSONArray("featureVector"))
                val sampleFeatureVectors = decodeFloatMatrix(item.optJSONArray("featureVectors"))
                default.copy(
                    sampleCount = item.optInt("sampleCount", default.sampleCount).coerceAtLeast(0),
                    lastTrainedAt = item.optLong("lastTrainedAt", default.lastTrainedAt),
                    confidenceBoost = item.optDouble("confidenceBoost", default.confidenceBoost.toDouble())
                        .toFloat()
                        .coerceIn(0f, 0.20f),
                    featureVector = mergedFeatureVector,
                    featureVectors = sampleFeatureVectors.ifEmpty {
                        if (mergedFeatureVector.isNotEmpty()) listOf(mergedFeatureVector) else emptyList()
                    },
                    sampleFilePaths = decodeStringList(item.optJSONArray("sampleFilePaths")),
                )
            }
        }
    }

    private fun encodeFloatList(values: List<Float>): JSONArray {
        val array = JSONArray()
        values.forEach { value -> array.put(value.toDouble()) }
        return array
    }

    private fun encodeFloatMatrix(values: List<List<Float>>): JSONArray {
        val array = JSONArray()
        values.forEach { row -> array.put(encodeFloatList(row)) }
        return array
    }

    private fun decodeFloatList(array: JSONArray?): List<Float> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                add(array.optDouble(index, 0.0).toFloat().coerceIn(0f, 1f))
            }
        }
    }

    private fun decodeFloatMatrix(array: JSONArray?): List<List<Float>> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                decodeFloatList(array.optJSONArray(index))
                    .takeIf { it.isNotEmpty() }
                    ?.let(::add)
            }
        }
    }

    private fun encodeStringList(values: List<String>): JSONArray {
        val array = JSONArray()
        values.forEach { value -> array.put(value) }
        return array
    }

    private fun decodeStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun encodeEvent(event: DetectedSoundEvent): JSONObject =
        JSONObject()
            .put("id", event.id)
            .put("kind", event.kind.name)
            .put("title", event.title)
            .put("subtitle", event.subtitle)
            .put("confidence", event.confidence.toDouble())
            .put("direction", event.direction.name)
            .put("source", event.source)
            .put("severity", event.severity.name)
            .put("repeatCount", event.repeatCount)
            .put("feedback", event.feedback.name)

    private fun decodeEvent(json: JSONObject?): DetectedSoundEvent? {
        if (json == null) return null
        val kind = enumValueOrDefault(json.optString("kind"), AlertKind.Idle)
        if (kind == AlertKind.Idle) return null

        return DetectedSoundEvent(
            id = json.optLong("id", System.currentTimeMillis()),
            kind = kind,
            title = json.optString("title", kind.title()),
            subtitle = json.optString("subtitle", ""),
            confidence = json.optDouble("confidence", 0.5).toFloat().coerceIn(0f, 1f),
            direction = enumValueOrDefault(json.optString("direction"), SoundDirection.Unknown),
            source = json.optString("source", "local"),
            severity = enumValueOrDefault(json.optString("severity"), kind.defaultSeverity()),
            repeatCount = json.optInt("repeatCount", 1).coerceAtLeast(1),
            feedback = enumValueOrDefault(json.optString("feedback"), FeedbackStatus.None),
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, default: T): T =
        runCatching {
            enumValueOf<T>(name.orEmpty())
        }.getOrDefault(default)
}
