package com.stv.videoplayer.player

import android.content.Intent
import android.os.Bundle
import android.util.Log
import java.util.Locale

/**
 * Extrait et assainit les headers HTTP transmis par un Intent entrant (extras `headers` et `referer`).
 *
 * - Sans état : aucune donnée n'est conservée entre deux appels (pas de fuite entre sessions vidéo).
 * - 100 % Safe-call : un extra corrompu ou malveillant ne lève jamais d'exception.
 * - Anti header-injection : CR/LF et caractères de contrôle neutralisés, noms de headers
 *   restreints aux "token chars" RFC 7230 (un nom invalide est rejeté).
 *
 * Formats supportés pour l'extra `headers` : `Bundle`, `Map`, `Array<String>`, `Iterable<String>`
 * (entrées `Nom: valeur` ou `Nom=valeur`) et `String` (une entrée par ligne).
 * Les extras `referer` / `Referer` sont prioritaires sur un éventuel header `Referer` explicite.
 *
 * La logique de parsing est portée par [parse], pure et testable en JVM sans Robolectric.
 */
object HeaderExtractor {

    const val EXTRA_HEADERS = "headers"
    const val EXTRA_REFERER = "referer"

    private const val EXTRA_REFERER_CAPITALIZED = "Referer"
    private const val HEADER_REFERER = "Referer"
    private const val HEADER_USER_AGENT = "User-Agent"
    private const val TOKEN_SPECIAL_CHARS = "!#\$%&'*+-.^_`|~"
    private const val TAG = "HeaderExtractor"

    /** Extrait les headers d'un Intent (lecture des extras protégée contre les extras corrompus). */
    fun extract(intent: Intent?): Map<String, String> {
        val extras = try {
            intent?.extras
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de lire les extras de l'Intent", e)
            null
        }
        return extract(extras)
    }

    /** Extrait les headers d'un [Bundle] d'extras. Retourne une map vide si rien d'exploitable. */
    fun extract(extras: Bundle?): Map<String, String> {
        if (extras == null) return emptyMap()
        return parse(
            rawHeaders = readValue(extras, EXTRA_HEADERS),
            referers = arrayOf(
                readValue(extras, EXTRA_REFERER),
                readValue(extras, EXTRA_REFERER_CAPITALIZED)
            )
        )
    }

    /**
     * Cœur pur du parsing : transforme une valeur brute (`Map`, `Array`, `Iterable`, `String`, `Bundle`)
     * et d'éventuels referers en une map propre. Ne lève jamais d'exception.
     */
    fun parse(rawHeaders: Any?, vararg referers: Any?): Map<String, String> {
        val collected = LinkedHashMap<String, Pair<String, String>>()

        try {
            collect(rawHeaders, collected)
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'extraire les headers HTTP", e)
        }
        referers.forEach { putHeader(collected, HEADER_REFERER, it?.toString()) }

        return collected.values.associate { (name, value) -> name to value }
    }

    private fun collect(raw: Any?, target: MutableMap<String, Pair<String, String>>) {
        when (raw) {
            null -> Unit
            is Bundle -> raw.keySet().forEach { key ->
                putHeader(target, key, readValue(raw, key)?.toString())
            }
            is Map<*, *> -> raw.forEach { (key, value) ->
                putHeader(target, key?.toString(), value?.toString())
            }
            is Array<*> -> raw.forEach { putEntry(target, it?.toString()) }
            is Iterable<*> -> raw.forEach { putEntry(target, it?.toString()) }
            is CharSequence -> raw.toString().split('\r', '\n').forEach { putEntry(target, it) }
        }
    }

    /** `Bundle.get` est déprécié (API 33) mais reste le seul accès générique ; protégé contre les extras corrompus. */
    @Suppress("DEPRECATION")
    private fun readValue(bundle: Bundle, key: String): Any? {
        return try {
            bundle.get(key)
        } catch (e: Exception) {
            Log.w(TAG, "Extra '$key' illisible", e)
            null
        }
    }

    private fun putEntry(target: MutableMap<String, Pair<String, String>>, rawEntry: String?) {
        val entry = parseEntry(rawEntry) ?: return
        putHeader(target, entry.first, entry.second)
    }

    private fun putHeader(
        target: MutableMap<String, Pair<String, String>>,
        rawName: String?,
        rawValue: String?
    ) {
        val name = cleanName(rawName) ?: return
        val value = cleanValue(rawValue) ?: return
        // Clé insensible à la casse : `referer` et `Referer` désignent le même header (le dernier gagne).
        target[name.lowercase(Locale.ROOT)] = name to value
    }

    /**
     * Découpe `Nom: valeur` ou `Nom=valeur` sur le PREMIER séparateur rencontré, afin de
     * préserver les valeurs contenant `=` ou `:` (ex : `Authorization: Basic abc==`).
     */
    private fun parseEntry(rawEntry: String?): Pair<String, String>? {
        val safe = rawEntry?.replace('\r', ' ')?.replace('\n', ' ')?.trim().orEmpty()
        if (safe.isEmpty()) return null

        val separatorIndex = listOf(safe.indexOf(':'), safe.indexOf('='))
            .filter { it >= 0 }
            .minOrNull() ?: return null
        if (separatorIndex <= 0 || separatorIndex >= safe.lastIndex) return null

        return safe.substring(0, separatorIndex).trim() to safe.substring(separatorIndex + 1).trim()
    }

    private fun cleanName(rawName: String?): String? {
        val name = rawName?.replace('\r', ' ')?.replace('\n', ' ')?.trim().orEmpty()
        if (name.isEmpty() || !name.all(::isTokenChar)) return null

        return when (name.lowercase(Locale.ROOT)) {
            "referer" -> HEADER_REFERER
            "user-agent" -> HEADER_USER_AGENT
            else -> name
        }
    }

    private fun cleanValue(rawValue: String?): String? {
        return rawValue
            ?.replace('\r', ' ')
            ?.replace('\n', ' ')
            ?.filter { it == '\t' || (it >= ' ' && it != '\u007F') }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun isTokenChar(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') || TOKEN_SPECIAL_CHARS.indexOf(c) >= 0
    }
}

