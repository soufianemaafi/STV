package com.example.stv.player

import android.util.Patterns

/**
 * Contrôleur métier pour orchestrer l'initialisation du player.
 * Gère la validation d'URL et la préparation de la lecture.
 */
class PlayerController {

    private val TAG = "PlayerController"

    /**
     * Valide une URL de flux.
     * @param url L'URL à valider.
     * @return Null si valide, sinon un message d'erreur.
     */
    fun validateStreamUrl(url: String?): String? {
        if (url.isNullOrBlank()) {
            return "URL de flux manquante"
        }

        val trimmedUrl = url.trim()

        // Vérifier le schéma (http ou https)
        if (!trimmedUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedUrl.startsWith("https://", ignoreCase = true)) {
            return "Schéma invalide : seuls http:// et https:// sont acceptés"
        }

        // Vérifier la longueur (limiter les abus)
        if (trimmedUrl.length > 2048) {
            return "URL trop longue (max 2048 caractères)"
        }

        // Utiliser le validateur Android natif
        if (!Patterns.WEB_URL.matcher(trimmedUrl).matches()) {
            return "Format d'URL invalide"
        }

        return null // Valide
    }

    /**
     * Retourne true si l'URL est valide.
     */
    fun isValidStreamUrl(url: String?): Boolean {
        return validateStreamUrl(url) == null
    }

    /**
     * Optionnel : whitelist de domaines autorisés.
     */
    fun isWhitelistedDomain(url: String, allowedDomains: List<String> = emptyList()): Boolean {
        if (allowedDomains.isEmpty()) return true // Pas de whitelist si vide

        return try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host ?: return false
            allowedDomains.any { host.endsWith(it) }
        } catch (e: Exception) {
            false
        }
    }
}

