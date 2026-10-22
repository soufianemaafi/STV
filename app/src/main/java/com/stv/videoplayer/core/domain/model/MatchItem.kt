package com.stv.videoplayer.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Source de diffusion alternative (qualité / serveur) d'un match. */
@Serializable
data class StreamSource(
    @SerialName("quality") val quality: String,
    @SerialName("url") val url: String
)

/**
 * Modèle pur d'un match (zéro dépendance Android).
 * Si [sources] est null ou vide, le [streamUrl] classique est utilisé.
 */
@Serializable
data class MatchItem(
    @SerialName("id") val id: String? = null,
    @SerialName("streamUrl") val streamUrl: String? = null,
    @SerialName("sources") val sources: List<StreamSource>? = null
)

/** Parsing tolérant et sécurisé de l'extra IPC `sources_json`. */
object StreamSourceParser {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Retourne les sources valides (schéma http/https uniquement, qualité non vide).
     * Ne lève jamais d'exception : JSON invalide => liste vide.
     */
    fun parse(rawJson: String?): List<StreamSource> {
        if (rawJson.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<StreamSource>>(rawJson)
                .map { StreamSource(it.quality.trim(), it.url.trim()) }
                .filter { source ->
                    val lower = source.url.lowercase()
                    source.quality.isNotEmpty() &&
                        (lower.startsWith("http://") || lower.startsWith("https://"))
                }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

