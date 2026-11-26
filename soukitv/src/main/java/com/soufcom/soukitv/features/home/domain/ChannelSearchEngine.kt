package com.soufcom.soukitv.features.home.domain

import com.soufcom.soukitv.features.home.domain.model.Channel
import java.text.Normalizer
import java.util.Locale

/** Chaîne pré-indexée : tous les champs de recherche sont normalisés une seule fois. */
class IndexedChannel internal constructor(
    val channel: Channel,
    internal val nameNorm: String,
    internal val nameCompact: String,
    internal val nameWords: Set<String>,
    internal val extraCompacts: List<String>,
    internal val allWords: Set<String>
)

/**
 * Moteur de recherche de chaînes : accents/arabe normalisés, multi-mots dans n'importe quel ordre,
 * formes compactes ("bein1") et tolérance aux fautes de frappe. Pur Kotlin, testable sur JVM.
 */
object ChannelSearchEngine {

    private const val SCORE_EXACT = 100
    private const val SCORE_PREFIX = 90
    private const val SCORE_WORD_PREFIX = 80
    private const val SCORE_CONTAINS = 70
    private const val SCORE_NAME_WORDS = 60
    private const val SCORE_EXTRA_FIELDS = 40
    private const val SCORE_FUZZY = 10

    private const val FUZZY_MIN_LENGTH = 4
    private const val FUZZY_LONG_LENGTH = 7

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
    private val LETTER_DIGIT_BOUNDARY = Regex("(?<=\\p{L})(?=\\p{N})|(?<=\\p{N})(?=\\p{L})")

    fun normalize(text: String): String {
        val stripped = DIACRITICS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
        val unified = StringBuilder(stripped.length)
        for (c in stripped) {
            when (c) {
                '\u0640' -> Unit
                '\u0629' -> unified.append('\u0647')
                '\u0649', '\u0626' -> unified.append('\u064A')
                '\u0624' -> unified.append('\u0648')
                else -> unified.append(c)
            }
        }
        return SEPARATORS.replace(unified.toString().lowercase(Locale.ROOT), " ").trim()
    }

    fun compact(text: String): String = normalize(text).replace(" ", "")

    fun index(channels: List<Channel>): List<IndexedChannel> = channels.map(::indexChannel)

    private fun indexChannel(channel: Channel): IndexedChannel {
        val nameNorm = normalize(channel.name)
        val extras = LinkedHashSet<String>()
        (channel.categories + channel.primaryCategory).forEach { path ->
            extras += normalize(path)
            path.split('/').forEach { extras += normalize(it) }
        }
        channel.country?.let { extras += normalize(it) }
        extras.remove("")

        val nameWords = wordsOf(nameNorm)
        val extraWords = extras.flatMapTo(LinkedHashSet()) { wordsOf(it) }
        return IndexedChannel(
            channel = channel,
            nameNorm = nameNorm,
            nameCompact = nameNorm.replace(" ", ""),
            nameWords = nameWords,
            extraCompacts = extras.map { it.replace(" ", "") },
            allWords = nameWords + extraWords
        )
    }

    /** Retourne les chaînes correspondantes, triées par pertinence (ordre stable à score égal). */
    fun search(indexedChannels: List<IndexedChannel>, query: String): List<Channel> {
        val normalized = normalize(query)
        if (normalized.isEmpty()) return indexedChannels.map { it.channel }
        val compact = normalized.replace(" ", "")
        val tokens = normalized.split(' ')

        return indexedChannels
            .map { it to score(it, normalized, compact, tokens) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first.channel }
    }

    private fun score(
        item: IndexedChannel,
        query: String,
        compact: String,
        tokens: List<String>
    ): Int {
        if (item.nameNorm == query || item.nameCompact == compact) return SCORE_EXACT
        if (occurs(item.nameNorm, query, prefixOnly = true) ||
            occurs(item.nameCompact, compact, prefixOnly = true)
        ) {
            return SCORE_PREFIX
        }
        if (item.nameWords.any { occurs(it, query, prefixOnly = true) }) return SCORE_WORD_PREFIX
        if (occurs(item.nameNorm, query, prefixOnly = false) ||
            occurs(item.nameCompact, compact, prefixOnly = false)
        ) {
            return SCORE_CONTAINS
        }
        if (tokens.all { matchesToken(it, item.nameWords) }) return SCORE_NAME_WORDS
        if (tokens.all { matchesToken(it, item.allWords) } ||
            item.extraCompacts.any { occurs(it, compact, prefixOnly = false) }
        ) {
            return SCORE_EXTRA_FIELDS
        }
        if (tokens.all { matchesToken(it, item.allWords) || matchesFuzzy(it, item) }) {
            return SCORE_FUZZY
        }
        return 0
    }

    private fun wordsOf(normalized: String): Set<String> {
        if (normalized.isEmpty()) return emptySet()
        val words = LinkedHashSet<String>()
        normalized.split(' ').forEach { word ->
            words += word
            words += splitMixed(word)
        }
        return words
    }

    private fun splitMixed(word: String): List<String> =
        word.split(LETTER_DIGIT_BOUNDARY).filter(String::isNotEmpty)

    /** Un token matche un mot ; un token mixte ("bein1") peut aussi matcher via ses parties. */
    private fun matchesToken(token: String, words: Set<String>): Boolean {
        if (words.any { wordMatches(it, token) }) return true
        val parts = splitMixed(token)
        return parts.size > 1 && parts.all { part -> words.any { wordMatches(it, part) } }
    }

    private fun wordMatches(word: String, token: String): Boolean =
        if (token.all(Char::isDigit)) word == token else occurs(word, token, prefixOnly = false)

    private fun matchesFuzzy(token: String, item: IndexedChannel): Boolean {
        if (token.length < FUZZY_MIN_LENGTH || token.any(Char::isDigit)) return false
        val maxDistance = if (token.length >= FUZZY_LONG_LENGTH) 2 else 1
        return item.allWords.any { fuzzyPrefix(token, it, maxDistance) } ||
            fuzzyPrefix(token, item.nameCompact, maxDistance) ||
            item.extraCompacts.any { fuzzyPrefix(token, it, maxDistance) }
    }

    /** Compare le token à des préfixes du texte de longueur proche (faute de frappe en fin de saisie). */
    private fun fuzzyPrefix(token: String, text: String, maxDistance: Int): Boolean {
        val minLength = maxOf(1, token.length - maxDistance)
        val maxLength = minOf(text.length, token.length + maxDistance)
        for (length in minLength..maxLength) {
            if (levenshtein(token, text.substring(0, length)) <= maxDistance) return true
        }
        return false
    }

    /** Évite de confondre "1" et "12" : une borne numérique ne peut pas couper une suite de chiffres. */
    private fun occurs(text: String, query: String, prefixOnly: Boolean): Boolean {
        if (query.isEmpty() || query.length > text.length) return false
        var start = text.indexOf(query)
        while (start >= 0) {
            if (prefixOnly && start != 0) return false
            val end = start + query.length
            val startOk = !(query.first().isDigit() && start > 0 && text[start - 1].isDigit())
            val endOk = !(query.last().isDigit() && end < text.length && text[end].isDigit())
            if (startOk && endOk) return true
            if (prefixOnly) return false
            start = text.indexOf(query, start + 1)
        }
        return false
    }

    internal fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
