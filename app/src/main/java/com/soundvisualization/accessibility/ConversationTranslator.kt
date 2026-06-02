package com.soundvisualization.accessibility

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.core.net.toUri
import java.util.LinkedHashMap
import java.util.Locale
import org.json.JSONObject

private data class ProtectedText(
    val text: String,
    val placeholders: Map<String, String>,
)

private data class LocalTranslationRequest(
    val original: String,
    val normalized: String,
    val protectedText: String,
    val placeholders: Map<String, String>,
    val isQuestion: Boolean,
)

private data class TranslationPair(
    val source: String,
    val target: String,
)

private data class EngineTranslationResult(
    val text: String,
    val fromModel: Boolean,
    val error: String? = null,
)

class ConversationTranslator(
    context: Context,
    private val onState: (TranslationState) -> Unit,
) {
    private val engine = BergamotLocalTranslationEngine(context)
    private var state = TranslationState()
    private var lastRequestedText = ""
    private var requestVersion = 0L
    private var closed = false

    private val translationCache = object : LinkedHashMap<String, String>(48, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > MAX_CACHE_ENTRIES
    }

    fun updateSettings(next: TranslationState) {
        val forcedNext = next.copy(
            source = normalizedTranslationSource(next.source),
            target = normalizedTranslationTarget(next.target),
            autoDetectSource = false,
        )
        val languageChanged = state.source.modelCode != forcedNext.source.modelCode ||
            state.target.modelCode != forcedNext.target.modelCode
        val enabledChanged = state.enabled != next.enabled

        if (languageChanged || enabledChanged) {
            requestVersion += 1
        }

        updateState(forcedNext.copy(
            translatedText = if (languageChanged) "" else forcedNext.translatedText,
            modelReady = forcedNext.enabled &&
                engine.hasRoute(forcedNext.source.modelCode, forcedNext.target.modelCode),
        ))
    }

    fun preview(text: String, detectedLanguageTag: String? = null) {
        val normalized = normalizeInput(text)
        if (!state.enabled || normalized.isBlank()) return

        val sourceLanguage = resolveSourceLanguage(normalized, detectedLanguageTag)
        val cleaned = prepareSpeechTextForTranslation(normalized, sourceLanguage.modelCode)
        val effectiveTarget = effectiveTargetLanguage(sourceLanguage)
        updateState(state.copy(
            source = sourceLanguage,
            sourceText = cleaned,
            translatedText = if (sourceLanguage.modelCode == effectiveTarget.modelCode) {
                cleaned
            } else if (cleaned != state.sourceText) {
                ""
            } else {
                state.translatedText
            },
            modelReady = engine.hasRoute(sourceLanguage.modelCode, effectiveTarget.modelCode),
        ))
    }

    fun prepare() {
        if (!state.enabled) return
        engine.prepare()
    }

    fun translate(text: String, detectedLanguageTag: String? = null) {
        val sourceLanguage = resolveSourceLanguage(text, detectedLanguageTag)
        val targetLanguage = effectiveTargetLanguage(sourceLanguage)
        val request = buildLocalRequest(text, sourceLanguage)
        if (!state.enabled || request.normalized.isBlank()) return

        updateState(state.copy(
            source = sourceLanguage,
            sourceText = request.original,
            modelReady = engine.hasRoute(sourceLanguage.modelCode, targetLanguage.modelCode),
        ))

        if (sourceLanguage.modelCode == targetLanguage.modelCode) {
            updateState(state.copy(translatedText = request.original, modelReady = true))
            return
        }

        lastRequestedText = request.normalized

        cachedTranslation(request.normalized)?.let { cached ->
            updateState(state.copy(
                sourceText = request.original,
                translatedText = cached,
                modelReady = true,
            ))
            return
        }

        if (!engine.hasRoute(sourceLanguage.modelCode, targetLanguage.modelCode)) {
            updateState(state.copy(
                translatedText = engine.fallbackText(
                    text = request.original,
                    source = sourceLanguage.modelCode,
                    target = targetLanguage.modelCode,
                ),
                modelReady = false,
            ))
            return
        }

        val version = ++requestVersion
        engine.translate(
            text = request.protectedText,
            source = sourceLanguage.modelCode,
            target = targetLanguage.modelCode,
        ) { result ->
            if (closed || version != requestVersion || request.normalized != lastRequestedText) return@translate

            val translated = result.text
                .let { restoreAndPolishTranslation(it, request.placeholders, request.isQuestion) }
                .ifBlank {
                    engine.fallbackText(
                        text = request.original,
                        source = sourceLanguage.modelCode,
                        target = targetLanguage.modelCode,
                    )
                }

            updateState(state.copy(
                source = sourceLanguage,
                target = targetLanguage,
                sourceText = request.original,
                translatedText = translated,
                modelReady = result.fromModel && result.error == null,
            ))
            if (translated.isNotBlank() && result.fromModel) {
                cacheTranslation(request.normalized, translated)
            }
        }
    }

    private fun updateState(next: TranslationState) {
        if (next == state) return

        state = next
        onState(next)
    }

    private fun effectiveTargetLanguage(sourceLanguage: TranslationLanguage): TranslationLanguage =
        state.target

    fun close() {
        closed = true
        translationCache.clear()
        engine.close()
    }

    fun releaseIdleResources() {
        if (closed) return
        engine.close()
    }

    private fun buildLocalRequest(text: String, sourceLanguage: TranslationLanguage): LocalTranslationRequest {
        val normalized = normalizeInput(text)
        val speechRepaired = prepareSpeechTextForTranslation(normalized, sourceLanguage.modelCode)
        val conversational = applyInferredConversationPunctuation(
            text = speechRepaired,
            language = sourceLanguage.modelCode,
        )
        val protected = protectLiterals(conversational)
        return LocalTranslationRequest(
            original = conversational,
            normalized = conversational,
            protectedText = protected.text,
            placeholders = protected.placeholders,
            isQuestion = looksLikeQuestion(conversational, sourceLanguage.modelCode),
        )
    }

    private fun prepareSpeechTextForTranslation(text: String, languageCode: String): String =
        inferSpeechRecognitionText(
            text = repairSpeechRecognitionText(text),
            languageCode = languageCode,
        )

    private fun resolveSourceLanguage(text: String, detectedLanguageTag: String?): TranslationLanguage {
        return normalizedTranslationSource(state.source)
    }

    private fun repairSpeechRecognitionText(text: String): String {
        if (text.isBlank()) return text
        return text
            .replace(invisibleControlRegex, "")
            .replace(spaceBeforeSpeechPunctuationRegex, "$1")
            .replace(missingSpaceAfterSpeechPunctuationRegex, "$1 $2")
            .replace(repeatedPunctuationRegex, "$1$1")
            .replace(whitespaceRegex, " ")
            .trim()
    }

    private fun protectLiterals(text: String): ProtectedText {
        var working = text
        val placeholders = linkedMapOf<String, String>()
        protectedLiteralPatterns.forEach { pattern ->
            working = pattern.replace(working) { match ->
                val key = " SVTOKEN${placeholders.size} "
                placeholders[key.trim()] = match.value
                key
            }
        }

        return ProtectedText(
            text = working.replace(whitespaceRegex, " ").trim(),
            placeholders = placeholders,
        )
    }

    private fun restoreAndPolishTranslation(
        text: String,
        placeholders: Map<String, String>,
        sourceWasQuestion: Boolean,
    ): String {
        var restored = text.safeCleanTranslationOutput()
        placeholders.forEach { (token, original) ->
            restored = restored
                .replace(token, original)
                .replace(token.lowercase(Locale.ROOT), original)
                .replace(token.uppercase(Locale.ROOT), original)
        }

        return restored
            .replace(spaceBeforeOutputPunctuationRegex, "$1")
            .replace(spaceAfterOpeningBracketRegex, "$1")
            .replace(spaceBeforeClosingBracketRegex, "$1")
            .replace(whitespaceRegex, " ")
            .polishForTargetLanguage(sourceWasQuestion)
            .trim()
    }

    private fun cacheKey(text: String): String =
        "${state.source.modelCode}:${state.target.modelCode}:$text"

    private fun cachedTranslation(text: String): String? =
        synchronized(translationCache) {
            translationCache[cacheKey(text)]
        }

    private fun cacheTranslation(text: String, translated: String) {
        synchronized(translationCache) {
            translationCache[cacheKey(text)] = translated
        }
    }

    private fun normalizeInput(text: String): String =
        text.replace(whitespaceRegex, " ").trim()

    private fun String.safeCleanTranslationOutput(): String {
        val cleaned = trim()
            .replace(translationPrefixRegex, "")
            .trim()
        return if (cleaned.length >= 2 &&
            ((cleaned.first() == '"' && cleaned.last() == '"') ||
                (cleaned.first() == '\'' && cleaned.last() == '\''))
        ) {
            cleaned.substring(1, cleaned.length - 1).trim()
        } else {
            cleaned
        }
    }

    private fun String.polishForTargetLanguage(sourceWasQuestion: Boolean): String {
        val conversational = when (state.target.modelCode) {
            "en" -> polishEnglishConversation()
            "ko" -> polishKoreanConversation()
            else -> this
        }
        return conversational.ensureQuestionPunctuation(sourceWasQuestion)
    }

    private fun String.polishEnglishConversation(): String {
        var text = replace(englishContractionGlueRegex, "$1'$2")
            .replace(englishPronounRegex, "I")

        englishContractions.forEach { (pattern, replacement) ->
            text = text.replace(pattern, replacement)
        }
        return text.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
        }
    }

    private fun String.polishKoreanConversation(): String =
        koreanConversationRewrites.fold(this) { current, (pattern, replacement) ->
            current.replace(pattern, replacement)
        }

    private fun applyInferredConversationPunctuation(text: String, language: String): String {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return trimmed

        val question = looksLikeQuestion(trimmed, language)
        if (!question) return trimmed

        return trimmed.ensureQuestionPunctuation(sourceWasQuestion = true)
    }

    private fun String.ensureQuestionPunctuation(sourceWasQuestion: Boolean): String {
        if (!sourceWasQuestion) return this
        val trimmed = trim()
        if (trimmed.isBlank() || trimmed.endsWith("?") || trimmed.endsWith("\uFF1F")) return trimmed

        val last = trimmed.last()
        return if (last == '.' || last == '!' || last == '\u3002' || last == '\uFF01') {
            trimmed.dropLast(1).trimEnd() + "?"
        } else {
            "$trimmed?"
        }
    }

    private fun looksLikeQuestion(text: String, language: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return false
        if (trimmed.endsWith("?") || trimmed.endsWith("\uFF1F")) return true

        val withoutTerminal = trimmed.trimEnd('.', '!', '\u3002', '\uFF01').trim()
        return when (language) {
            "ko" -> looksLikeKoreanQuestion(withoutTerminal)
            "en" -> englishQuestionStart.containsMatchIn(withoutTerminal.lowercase(Locale.ROOT))
            "ja" -> looksLikeJapaneseQuestion(withoutTerminal)
            else -> false
        }
    }

    private fun looksLikeKoreanQuestion(text: String): Boolean {
        val compact = text.replace(whitespaceRegex, "")
        if (compact.isBlank()) return false

        if (koreanQuestionEndings.any { compact.endsWith(it) }) return true
        return koreanQuestionCues.any { compact.contains(it) }
    }

    private fun looksLikeJapaneseQuestion(text: String): Boolean {
        val compact = text.replace(whitespaceRegex, "")
        if (compact.isBlank()) return false
        if (compact.endsWith("\u304b") || compact.endsWith("\u306e") || compact.endsWith("\u3067\u3059\u304b")) {
            return true
        }
        return japaneseQuestionCues.any { compact.contains(it) }
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 96

        private val invisibleControlRegex = Regex("[\\u200B-\\u200D\\uFEFF]")
        private val whitespaceRegex = Regex("\\s+")
        private val spaceBeforeSpeechPunctuationRegex = Regex("\\s+([,.!?;:])")
        private val missingSpaceAfterSpeechPunctuationRegex = Regex("([,.!?;:])([^\\s,.!?;:])")
        private val repeatedPunctuationRegex = Regex("([!?.,])\\1{2,}")
        private val spaceBeforeOutputPunctuationRegex = Regex("\\s+([,.!?;:%])")
        private val spaceAfterOpeningBracketRegex = Regex("([\\(\\[{])\\s+")
        private val spaceBeforeClosingBracketRegex = Regex("\\s+([\\)\\]}])")
        private val translationPrefixRegex =
            Regex("^(Translation)\\s*[:\\uFF1A]\\s*", RegexOption.IGNORE_CASE)
        private val englishPronounRegex = Regex("\\bi\\b")
        private val englishContractionGlueRegex =
            Regex("\\b([A-Za-z]+)\\s+'\\s*(m|re|ve|ll|d|s|t)\\b")

        private val protectedLiteralPatterns = listOf(
            Regex("https?://\\S+", RegexOption.IGNORE_CASE),
            Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE),
            Regex("\\b\\+?\\d[\\d\\s().-]{5,}\\d\\b"),
            Regex(
                "\\b\\d+(?:[.,:]\\d+)*(?:\\s?(?:%|kg|g|km|m|cm|mm|ml|l|dollar|minute|second))?\\b",
                RegexOption.IGNORE_CASE,
            ),
        )

        private val englishQuestionStart =
            "^(who|what|where|when|why|how|which|whose|whom|do|does|did|can|could|would|should|is|are|am|was|were|will|have|has|had|may|might|shall)\\b"
                .toRegex()

        private val englishContractions = listOf(
            Regex("\\bI am\\b") to "I'm",
            Regex("\\bI have\\b") to "I've",
            Regex("\\bI will\\b") to "I'll",
            Regex("\\bI would\\b") to "I'd",
            Regex("\\bYou are\\b") to "You're",
            Regex("\\byou are\\b") to "you're",
            Regex("\\bWe are\\b") to "We're",
            Regex("\\bwe are\\b") to "we're",
            Regex("\\bThey are\\b") to "They're",
            Regex("\\bthey are\\b") to "they're",
            Regex("\\bIt is\\b") to "It's",
            Regex("\\bit is\\b") to "it's",
            Regex("\\bThat is\\b") to "That's",
            Regex("\\bthat is\\b") to "that's",
            Regex("\\bThere is\\b") to "There's",
            Regex("\\bthere is\\b") to "there's",
            Regex("\\bDo not\\b") to "Don't",
            Regex("\\bdo not\\b") to "don't",
            Regex("\\bDoes not\\b") to "Doesn't",
            Regex("\\bdoes not\\b") to "doesn't",
            Regex("\\bDid not\\b") to "Didn't",
            Regex("\\bdid not\\b") to "didn't",
            Regex("\\bCannot\\b") to "Can't",
            Regex("\\bcannot\\b") to "can't",
            Regex("\\bCan not\\b") to "Can't",
            Regex("\\bcan not\\b") to "can't",
            Regex("\\bWill not\\b") to "Won't",
            Regex("\\bwill not\\b") to "won't",
            Regex("\\bLet us\\b") to "Let's",
            Regex("\\blet us\\b") to "let's",
        )

        private val koreanConversationRewrites = listOf(
            Regex("\uc548\ub155\ud558\uc2ed\ub2c8\uae4c") to "\uc548\ub155\ud558\uc138\uc694",
            Regex("\uac10\uc0ac\ud569\ub2c8\ub2e4") to "\uace0\ub9c8\uc6cc\uc694",
            Regex("\ud558\uc9c0 \ub9c8\uc2ed\uc2dc\uc624") to "\ud558\uc9c0 \ub9c8\uc138\uc694",
            Regex("\uc5b4\ub5bb\uac8c \uc9c0\ub0b4\uc2ed\ub2c8\uae4c\\?") to "\uc798 \uc9c0\ub0b4\uc138\uc694?",
            Regex("\ubb34\uc5c7\uc744 \ud558\uace0 \uc788\uc2b5\ub2c8\uae4c\\?") to "\ubb50 \ud558\uace0 \uc788\uc5b4\uc694?",
            Regex("\uc5b4\ub514\uc5d0 \uac00\uc2ed\ub2c8\uae4c\\?") to "\uc5b4\ub514 \uac00\uc138\uc694?",
        )

        private val koreanQuestionCues = listOf(
            "\ubb50",
            "\ubb34\uc5c7",
            "\uc5b4\ub514",
            "\uc5b8\uc81c",
            "\ub204\uad6c",
            "\uc65c",
            "\uc5b4\ub5bb\uac8c",
            "\uc5b4\ub5a1",
            "\uc5bc\ub9c8",
            "\uba87",
            "\ubb34\uc2a8",
            "\uc5b4\ub290",
            "\uc5b4\ub5a4",
        )

        private val koreanQuestionEndings = listOf(
            "\ub098\uc694",
            "\uac00\uc694",
            "\uae4c\uc694",
            "\uc778\uac00\uc694",
            "\uc2b5\ub2c8\uae4c",
            "\uc785\ub2c8\uae4c",
            "\uc5c8\ub098\uc694",
            "\uc744\uae4c\uc694",
            "\uc904\ub798\uc694",
            "\ud574\uc904\ub798\uc694",
            "\uc904\ub798",
            "\ud574\uc904\ub798",
            "\ub2c8",
            "\ub0d0",
        )

        private val japaneseQuestionCues = listOf(
            "\u4f55",
            "\u306a\u306b",
            "\u3069\u3053",
            "\u3044\u3064",
            "\u8ab0",
            "\u3060\u308c",
            "\u306a\u305c",
            "\u3069\u3046",
            "\u3044\u304f\u3089",
        )

    }
}

private class BergamotLocalTranslationEngine(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(appContext))
        .build()

    private var webView: WebView? = null
    private var ready = false
    private var nextRequestId = 0L

    private val pending = linkedMapOf<Long, PendingTranslation>()

    fun hasRoute(source: String, target: String): Boolean =
        source == target ||
            hasDirectRoute(source, target) ||
            (hasDirectRoute(source, PIVOT_LANGUAGE) && hasDirectRoute(PIVOT_LANGUAGE, target))

    fun translate(
        text: String,
        source: String,
        target: String,
        callback: (EngineTranslationResult) -> Unit,
    ) {
        if (text.isBlank()) {
            callback(EngineTranslationResult("", fromModel = false))
            return
        }
        if (source == target) {
            callback(EngineTranslationResult(text, fromModel = true))
            return
        }
        if (!hasRoute(source, target)) {
            callback(
                EngineTranslationResult(
                    text = fallbackText(text, source, target),
                    fromModel = false,
                    error = "Unsupported bundled route",
                ),
            )
            return
        }

        mainHandler.post {
            val id = ++nextRequestId
            pending[id] = PendingTranslation(text, source, target, callback)
            ensureWebView()
            if (ready) {
                dispatch(id)
            }
            mainHandler.postDelayed({
                val request = pending.remove(id) ?: return@postDelayed
                request.callback(
                    EngineTranslationResult(
                        text = fallbackText(request.text, request.source, request.target),
                        fromModel = false,
                        error = "Local translation timed out",
                    ),
                )
            }, TRANSLATION_TIMEOUT_MS)
        }
    }

    fun prepare() {
        mainHandler.post { ensureWebView() }
    }

    fun fallbackText(text: String, source: String, target: String): String {
        if (text.isBlank()) return ""
        if (source == target) return text

        return when (target) {
            "en" -> "The bundled local model cannot translate this sentence yet: $text"
            "ko" -> "\ub85c\uceec \ubaa8\ub378\uc774 \uc774 \ubb38\uc7a5\uc744 \ubc88\uc5ed\ud558\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4: $text"
            "ja" -> "\u30a2\u30d7\u30ea\u5185\u306e\u30ed\u30fc\u30ab\u30eb\u7ffb\u8a33\u30e2\u30c7\u30eb\u306f\u3053\u306e\u6587\u3092\u307e\u3060\u51e6\u7406\u3067\u304d\u307e\u305b\u3093: $text"
            "es" -> "El modelo local incluido todavia no puede traducir esta frase: $text"
            "vi" -> "Mo hinh dich cuc bo trong ung dung chua xu ly duoc cau nay: $text"
            else -> text
        }
    }

    fun close() {
        mainHandler.post {
            pending.clear()
            ready = false
            webView?.destroy()
            webView = null
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureWebView() {
        if (webView != null) return

        webView = WebView(appContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            settings.safeBrowsingEnabled = true

            addJavascriptInterface(BergamotBridge(), "AndroidBridge")
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage): Boolean {
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                @Suppress("OVERRIDE_DEPRECATION")
                override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
                    assetLoader.shouldInterceptRequest(url.toUri())
            }
            loadUrl(BRIDGE_URL)
        }
    }

    private fun flushPending() {
        pending.keys.toList().forEach(::dispatch)
    }

    private fun dispatch(id: Long) {
        val request = pending[id] ?: return
        val script = buildString {
            append("window.SoundVizTranslate(")
            append(id)
            append(',')
            append(JSONObject.quote(request.source))
            append(',')
            append(JSONObject.quote(request.target))
            append(',')
            append(JSONObject.quote(request.text))
            append(");")
        }
        webView?.evaluateJavascript(script, null)
    }

    private fun handleBridgeMessage(message: String) {
        val json = runCatching { JSONObject(message) }.getOrNull() ?: return
        when (json.optString("type")) {
            "ready" -> {
                ready = true
                flushPending()
            }
            "translation" -> {
                val id = json.optLong("id", -1L)
                val request = pending.remove(id) ?: return
                request.callback(
                    EngineTranslationResult(
                        text = json.optString("text"),
                        fromModel = true,
                    ),
                )
            }
            "translation-error" -> {
                val id = json.optLong("id", -1L)
                val request = pending.remove(id) ?: return
                val error = json.optString("message", "Local translation failed")
                Log.w(TAG, error)
                request.callback(
                    EngineTranslationResult(
                        text = fallbackText(request.text, request.source, request.target),
                        fromModel = false,
                        error = error,
                    ),
                )
            }
            "engine-error" -> {
                val error = json.optString("message", "Local translation engine failed")
                Log.w(TAG, error)
            }
        }
    }

    private inner class BergamotBridge {
        @JavascriptInterface
        fun postMessage(message: String) {
            mainHandler.post { handleBridgeMessage(message) }
        }
    }

    private data class PendingTranslation(
        val text: String,
        val source: String,
        val target: String,
        val callback: (EngineTranslationResult) -> Unit,
    )

    companion object {
        private const val TAG = "BergamotTranslator"
        private const val PIVOT_LANGUAGE = "en"
        private const val BRIDGE_URL =
            "https://appassets.androidplatform.net/assets/bergamot/bridge.html"
        private const val TRANSLATION_TIMEOUT_MS = 45_000L

        private fun hasDirectRoute(source: String, target: String): Boolean =
            TranslationPair(source, target) in supportedPairs

        private val supportedPairs = setOf(
            TranslationPair("ko", "en"),
            TranslationPair("en", "ko"),
            TranslationPair("ja", "en"),
        )
    }
}
