package com.example.stv.player

import android.content.Context
import android.content.Intent
import android.util.Patterns
import com.example.stv.R

/**
 * Contrôleur métier pour orchestrer l'initialisation du player.
 * Gère la validation d'URL, la résolution d'URL depuis les intents, et la préparation de la lecture.
 */
class PlayerController(private val context: Context) {

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
     * @return Null si valide, sinon un message d'erreur localisé.
     */
    fun validateStreamUrl(url: String?): String? {
        if (url.isNullOrBlank()) {
            return context.getString(R.string.error_url_missing)
        }

        val trimmedUrl = url.trim()

        // Vérifier le schéma (http ou https)
        if (!trimmedUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedUrl.startsWith("https://", ignoreCase = true)) {
            return context.getString(R.string.error_url_invalid_scheme)
        }

        // Vérifier la longueur (limiter les abus)
        if (trimmedUrl.length > 2048) {
            return context.getString(R.string.error_url_too_long)
        }

        // Utiliser le validateur Android natif
        if (!Patterns.WEB_URL.matcher(trimmedUrl).matches()) {
            return context.getString(R.string.error_url_invalid_format)
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

