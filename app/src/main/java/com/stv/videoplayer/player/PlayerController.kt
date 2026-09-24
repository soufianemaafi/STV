package com.stv.videoplayer.player

import android.content.Context
import android.content.Intent
import android.util.Patterns
import com.stv.videoplayer.R

/**
 * ContrÃ´leur mÃ©tier pour orchestrer l'initialisation du player.
 * GÃ¨re la validation d'URL, la rÃ©solution d'URL depuis les intents, et la prÃ©paration de la lecture.
 */
class PlayerController(private val context: Context) {

    /**
     * RÃ©sout l'URL vidÃ©o Ã  partir d'un Intent.
     * Supporte : extra VIDEO_URL, deep links stv://play?url=..., et data URI directes.
     * @param intent L'intent reÃ§u par PlayerActivity.
     * @return L'URL rÃ©solue, ou null si aucune URL trouvÃ©e.
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
     * @param url L'URL Ã  valider.
     * @return Null si valide, sinon un message d'erreur localisÃ©.
     */
    fun validateStreamUrl(url: String?): String? {
        if (url.isNullOrBlank()) {
            return context.getString(R.string.error_url_missing)
        }

        val trimmedUrl = url.trim()

        // VÃ©rifier le schÃ©ma (http ou https)
        if (!trimmedUrl.startsWith("http://", ignoreCase = true) &&
            !trimmedUrl.startsWith("https://", ignoreCase = true)) {
            return context.getString(R.string.error_url_invalid_scheme)
        }

        // VÃ©rifier la longueur (limiter les abus)
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
     * Optionnel : whitelist de domaines autorisÃ©s.
     */
    fun isWhitelistedDomain(url: String, allowedDomains: List<String> = emptyList()): Boolean {
        if (allowedDomains.isEmpty()) return true // Pas de whitelist si vide

        return try {
            val uri = android.net.Uri.parse(url)
            val host = uri.host ?: return false
            allowedDomains.any { host.endsWith(it) }
        } catch (_: Exception) {
            false
        }
    }
}


