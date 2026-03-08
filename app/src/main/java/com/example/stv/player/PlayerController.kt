package com.example.stv.player

import android.content.Intent
import android.util.Patterns

/**
 * Contrôleur métier pour orchestrer l'initialisation du player.
 * Gère la validation d'URL, la résolution d'URL depuis les intents, et la préparation de la lecture.
 */
class PlayerController {

    private val TAG = "PlayerController"

    /**
     * Résout l'URL vidéo à partir d'un Intent.
     * Supporte : extra VIDEO_URL, deep links stv://play?url=..., et data URI directes.
     * @param intent L'intent reçu par PlayerActivity.
     * @return L'URL résolue, ou null si aucune URL trouvée.
     */
    fun resolveVideoUrl(intent: Intent?): String? {
        // 1. Extra explicite (ex: depuis SoukiTV ou AddVideoActivity)
        val extraUrl = intent?.getStringExtra("VIDEO_URL")
        if (!extraUrl.isNullOrBlank()) {
            return extraUrl
        }
        // 2. Data URI (deep links, intents VIEW)
        val data = intent?.data ?: return null
        return if (data.scheme == "stv" && data.host == "play") {
            // Deep link stv://play?url=...
            data.getQueryParameter("url")
        } else {
            // URI directe (http://..., content://..., etc.)
            data.toString()
        }
    }

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

