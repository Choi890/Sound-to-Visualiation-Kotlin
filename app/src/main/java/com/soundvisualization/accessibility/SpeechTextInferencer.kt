package com.soundvisualization.accessibility

import java.util.Locale
import kotlin.math.min

private data class SpeechDictionaryEntry(
    val canonical: String,
    val aliases: List<String>,
)

private data class SpeechAliasRule(
    val canonical: String,
    val alias: String,
    val separatedPattern: Regex,
    val inlinePattern: Regex?,
    val aliasKey: String,
    val canonicalKey: String,
)

internal fun inferSpeechRecognitionText(text: String, languageCode: String? = null): String {
    val cleaned = text
        .replace(INVISIBLE_CONTROL_REGEX, "")
        .replace(WHITESPACE_REGEX, " ")
        .trim()
    if (cleaned.isBlank()) return cleaned

    val rules = when (languageCode) {
        "en" -> englishSpeechAliasRules
        "ja" -> japaneseSpeechAliasRules
        "ko" -> koreanSpeechAliasRules
        else -> allSpeechAliasRules
    }

    return rules
        .fold(cleaned) { current, rule -> current.replaceLikelySpeechAlias(rule) }
        .replace(SPACE_BEFORE_PUNCTUATION_REGEX, "$1")
        .replace(MISSING_SPACE_AFTER_PUNCTUATION_REGEX, "$1 $2")
        .replace(WHITESPACE_REGEX, " ")
        .trim()
}

private fun List<SpeechDictionaryEntry>.toAliasRules(): List<SpeechAliasRule> =
    asSequence()
        .flatMap { entry -> entry.aliases.asSequence().map { alias -> entry to alias } }
        .filter { (entry, alias) -> alias.isNotBlank() && entry.canonical.isNotBlank() && alias != entry.canonical }
        .sortedWith(
            compareByDescending<Pair<SpeechDictionaryEntry, String>> { (_, alias) -> alias.length }
                .thenByDescending { (entry, _) -> entry.canonical.length },
        )
        .map { (entry, alias) ->
            SpeechAliasRule(
                canonical = entry.canonical,
                alias = alias,
                separatedPattern = "(?<![\\p{L}\\p{N}])${Regex.escape(alias)}(?=$|[^\\p{L}\\p{N}])"
                    .toRegex(RegexOption.IGNORE_CASE),
                inlinePattern = if (alias.any { it.isHangul() || it.isKanaOrCjk() }) {
                    Regex.escape(alias).toRegex()
                } else {
                    null
                },
                aliasKey = alias.normalizeSpeechKey(),
                canonicalKey = entry.canonical.normalizeSpeechKey(),
            )
        }
        .toList()

private fun String.replaceLikelySpeechAlias(rule: SpeechAliasRule): String {
    val alias = rule.alias
    val canonical = rule.canonical
    if (alias.isBlank() || canonical.isBlank() || alias == canonical) return this

    var updated = this.replace(rule.separatedPattern, canonical)
    updated = updated.replaceKoreanLikeAlias(rule)
    return updated
}

private fun String.replaceKoreanLikeAlias(rule: SpeechAliasRule): String {
    val pattern = rule.inlinePattern ?: return this

    return pattern.replace(this) { match ->
        val start = match.range.first
        val end = match.range.last + 1
        val previous = getOrNull(start - 1)
        val next = getOrNull(end)

        val leftOk = previous == null || previous.isSpeechBoundary()
        val rightOk = next == null || next.isSpeechBoundary() || next.isKoreanParticleLead()
        val likelyTruncation = rule.canonicalKey.startsWith(rule.aliasKey) &&
            rule.canonicalKey.length - rule.aliasKey.length in 1..5
        val likelyTypo = rule.alias.length >= 3 &&
            levenshteinDistance(rule.aliasKey, rule.canonicalKey) <= 1

        if (leftOk && rightOk && (likelyTruncation || likelyTypo)) rule.canonical else match.value
    }
}

private fun Char.isSpeechBoundary(): Boolean =
    isWhitespace() || when (this) {
        '.', ',', '!', '?', ';', ':', '/', '-', '(', ')', '[', ']', '{', '}' -> true
        else -> false
    }

private fun Char.isKoreanParticleLead(): Boolean =
    when (this) {
        '\uC740',
        '\uB294',
        '\uC774',
        '\uAC00',
        '\uC744',
        '\uB97C',
        '\uC5D0',
        '\uC640',
        '\uACFC',
        '\uB3C4',
        '\uB9CC',
        '\uB85C',
        '\uC73C',
        '\uC758' -> true
        else -> false
    }

private fun Char.isHangul(): Boolean = this in '\uAC00'..'\uD7AF'

private fun Char.isKanaOrCjk(): Boolean =
    this in '\u3040'..'\u30FF' || this in '\u4E00'..'\u9FFF'

private fun String.normalizeSpeechKey(): String =
    lowercase(Locale.ROOT).filter { it.isLetterOrDigit() || it.isHangul() || it.isKanaOrCjk() }

private fun levenshteinDistance(left: String, right: String): Int {
    if (left == right) return 0
    if (left.isEmpty()) return right.length
    if (right.isEmpty()) return left.length

    var previous = IntArray(right.length + 1) { it }
    var current = IntArray(right.length + 1)
    for (i in 1..left.length) {
        current[0] = i
        var rowMin = current[0]
        for (j in 1..right.length) {
            val cost = if (left[i - 1] == right[j - 1]) 0 else 1
            current[j] = min(
                min(current[j - 1] + 1, previous[j] + 1),
                previous[j - 1] + cost,
            )
            rowMin = min(rowMin, current[j])
        }
        if (rowMin > 1) return rowMin
        val swap = previous
        previous = current
        current = swap
    }
    return previous[right.length]
}

private val koreanSpeechDictionary = listOf(
    SpeechDictionaryEntry("\uC0BC\uC131\uC804\uC790", listOf("\uC0BC\uC131\uC804", "\uC0BC\uC131 \uC804", "\uC0BC\uC131 \uC804\uC790", "\uC0BC\uC120\uC804\uC790", "\uC0BC\uC1A1\uC804\uC790")),
    SpeechDictionaryEntry("LG\uC804\uC790", listOf("\uC5D8\uC9C0\uC804", "\uC5D8\uC9C0 \uC804", "\uC5D8\uC9C0\uC804\uC790", "\uC5D8\uC9C0 \uC804\uC790", "LG\uC804")),
    SpeechDictionaryEntry("SK\uD558\uC774\uB2C9\uC2A4", listOf("\uC5D0\uC2A4\uCF00\uC774\uD558\uC774\uB2C9", "\uC5D0\uC2A4\uCF00\uC774 \uD558\uC774\uB2C9", "\uC5D0\uC2A4\uCF00\uC774\uD558\uC774\uB2C9\uC2A4", "SK\uD558\uC774\uB2C9")),
    SpeechDictionaryEntry("\uD604\uB300\uC790\uB3D9\uCC28", listOf("\uD604\uB300\uC790", "\uD604\uB300\uC790\uB3D9", "\uD604\uB300 \uC790\uB3D9", "\uD604\uB300 \uC790\uB3D9\uCC28")),
    SpeechDictionaryEntry("\uAE30\uC544\uC790\uB3D9\uCC28", listOf("\uAE30\uC544\uC790", "\uAE30\uC544\uC790\uB3D9", "\uAE30\uC544 \uC790\uB3D9", "\uAE30\uC544 \uC790\uB3D9\uCC28")),
    SpeechDictionaryEntry("\uB124\uC774\uBC84\uD398\uC774", listOf("\uB124\uC774\uBC84\uD398", "\uB124\uC774\uBC84 \uD398\uC774")),
    SpeechDictionaryEntry("\uCE74\uCE74\uC624\uD1A1", listOf("\uCE74\uD1A1")),
    SpeechDictionaryEntry("\uCE74\uCE74\uC624\uD398\uC774", listOf("\uCE74\uCE74\uC624\uD398", "\uCE74\uCE74\uC624 \uD398\uC774")),
    SpeechDictionaryEntry("\uD1A0\uC2A4\uBC45\uD06C", listOf("\uD1A0\uC2A4\uBC45", "\uD1A0\uC2A4 \uBC45", "\uD1A0\uC2A4 \uBC45\uD06C")),
    SpeechDictionaryEntry("\uC2E0\uD55C\uC740\uD589", listOf("\uC2E0\uD55C\uC740", "\uC2E0\uD55C \uC740", "\uC2E0\uD55C \uC740\uD589")),
    SpeechDictionaryEntry("\uAD6D\uBBFC\uC740\uD589", listOf("\uAD6D\uBBFC\uC740", "\uAD6D\uBBFC \uC740", "\uAD6D\uBBFC \uC740\uD589")),
    SpeechDictionaryEntry("\uC6B0\uB9AC\uC740\uD589", listOf("\uC6B0\uB9AC\uC740", "\uC6B0\uB9AC \uC740", "\uC6B0\uB9AC \uC740\uD589")),
    SpeechDictionaryEntry("\uC548\uB4DC\uB85C\uC774\uB4DC", listOf("\uC548\uB4DC\uB85C\uC774")),
    SpeechDictionaryEntry("\uC544\uC774\uD3F0", listOf("\uC544\uC774\uD3EC")),
    SpeechDictionaryEntry("\uC544\uC774\uD328\uB4DC", listOf("\uC544\uC774\uD328")),
    SpeechDictionaryEntry("OpenAI", listOf("\uC624\uD508\uC5D0\uC774", "\uC624\uD508\uC5D0\uC774\uC544\uC774", "\uC624\uD508 AI", "\uC624\uD508\uC5D0\uC774 \uC544\uC774")),
    SpeechDictionaryEntry("ChatGPT", listOf("\uCC57\uC9C0\uD53C\uD2F0", "\uCC57 GPT", "\uCC57\uC9C0\uD53C", "\uCC44\uC9C0\uD53C\uD2F0")),
    SpeechDictionaryEntry("\uC9C0\uD558\uCCA0\uC5ED", listOf("\uC9C0\uD558\uCCA0 \uC5ED")),
    SpeechDictionaryEntry("\uC751\uAE09\uC2E4", listOf("\uC751\uAE09 \uC2E4")),
)

private val englishSpeechDictionary = listOf(
    SpeechDictionaryEntry("Samsung Electronics", listOf("Samsung Electron", "Samsung Electronic", "Sam Sung Electronics", "Sam Sung Electron")),
    SpeechDictionaryEntry("LG Electronics", listOf("LG Electron", "LG Electronic", "L G Electronics", "L G Electron")),
    SpeechDictionaryEntry("Hyundai Motor", listOf("Hyundai Moto", "Hyundai Motors", "Hyundai car")),
    SpeechDictionaryEntry("OpenAI", listOf("Open AI", "Open A I", "open ai")),
    SpeechDictionaryEntry("ChatGPT", listOf("Chat GPT", "chat gpt", "Chat G P T")),
    SpeechDictionaryEntry("Android", listOf("Androi", "An droid")),
)

private val japaneseSpeechDictionary = listOf(
    SpeechDictionaryEntry("\u30B5\u30E0\u30B9\u30F3\u96FB\u5B50", listOf("\u30B5\u30E0\u30B9\u30F3\u96FB", "\u30B5\u30E0\u30B9\u30F3 \u96FB\u5B50")),
    SpeechDictionaryEntry("\u30D2\u30E5\u30F3\u30C0\u30A4\u81EA\u52D5\u8ECA", listOf("\u30D2\u30E5\u30F3\u30C0\u30A4\u81EA", "\u30D2\u30E5\u30F3\u30C0\u30A4 \u81EA\u52D5\u8ECA")),
    SpeechDictionaryEntry("OpenAI", listOf("\u30AA\u30FC\u30D7\u30F3AI", "\u30AA\u30FC\u30D7\u30F3\u30A8\u30FC\u30A2\u30A4")),
    SpeechDictionaryEntry("ChatGPT", listOf("\u30C1\u30E3\u30C3\u30C8GPT", "\u30C1\u30E3\u30C3\u30C8\u30B8\u30FC\u30D4\u30FC\u30C6\u30A3\u30FC")),
)

private val koreanSpeechAliasRules by lazy(LazyThreadSafetyMode.PUBLICATION) {
    koreanSpeechDictionary.toAliasRules()
}

private val englishSpeechAliasRules by lazy(LazyThreadSafetyMode.PUBLICATION) {
    englishSpeechDictionary.toAliasRules()
}

private val japaneseSpeechAliasRules by lazy(LazyThreadSafetyMode.PUBLICATION) {
    japaneseSpeechDictionary.toAliasRules()
}

private val allSpeechAliasRules by lazy(LazyThreadSafetyMode.PUBLICATION) {
    (koreanSpeechDictionary + englishSpeechDictionary + japaneseSpeechDictionary).toAliasRules()
}

private val INVISIBLE_CONTROL_REGEX = Regex("[\\u200B-\\u200D\\uFEFF]")
private val WHITESPACE_REGEX = Regex("\\s+")
private val SPACE_BEFORE_PUNCTUATION_REGEX = Regex("\\s+([,.!?;:])")
private val MISSING_SPACE_AFTER_PUNCTUATION_REGEX = Regex("([,.!?;:])([^\\s,.!?;:])")
