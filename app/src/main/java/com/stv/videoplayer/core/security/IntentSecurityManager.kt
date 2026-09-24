package com.stv.videoplayer.core.security

import android.content.Intent
import android.util.Log

/**
 * RÃ©sultat de la validation de sÃ©curitÃ© d'une URI/Intent vidÃ©o.
 * UtilisÃ© par les Activities pour dÃ©cider s'il faut charger la vidÃ©o
 * ou rejeter l'Intent en toute sÃ©curitÃ© (pas de crash, pas de leak d'info).
 */
sealed class IntentValidationResult {
    data class Valid(val videoUrl: String) : IntentValidationResult()
    data class Invalid(val reason: String) : IntentValidationResult()
}

/**
 * SÃ©curise les Intents/Deep Links entrants pour se prÃ©munir contre :
 *  - **Intent Hijacking** : une app malveillante envoie un Intent Ã  une Activity exportÃ©e
 *    (`MainActivity` / `PlayerActivity`) avec des extras/data forgÃ©s.
 *  - **Intent Redirection** : injection de schÃ©mas dangereux (`file://`, `content://`,
 *    `javascript:`, `intent://`, etc.) qui pourraient exposer le systÃ¨me de fichiers,
 *    exÃ©cuter du JS dans une WebView, ou rediriger vers un composant non exportÃ©.
 *
 * RÃ¨gle stricte : **seuls `http://` et `https://` sont acceptÃ©s** pour la lecture vidÃ©o.
 * Toute extraction est 100% Safe-call / try-catch : un Intent malformÃ© ne doit
 * JAMAIS faire crasher l'application (conformitÃ© Play Store).
 */
class IntentSecurityManager {

    companion object {
        private const val TAG = "IntentSecurityManager"

        /** Seuls ces schÃ©mas sont autorisÃ©s pour la lecture vidÃ©o. */
        private val ALLOWED_SCHEMES = setOf("http", "https")

        /** Extensions vidÃ©o reconnues (utilisÃ© pour un filtrage additionnel optionnel). */
        private val ALLOWED_VIDEO_EXTENSIONS = listOf(".m3u8", ".mp4", ".mpd", ".ts", ".webm", ".mkv")

        private const val EXTRA_VIDEO_URL = "VIDEO_URL"
        private const val DEEP_LINK_SCHEME = "stv"
        private const val DEEP_LINK_HOST = "play"
        private const val MAX_URL_LENGTH = 2048
    }

    /**
     * Point d'entrÃ©e principal : extrait puis valide l'URL vidÃ©o contenue dans un Intent entrant.
     * Ne lÃ¨ve jamais d'exception.
     */
    fun validateIncomingIntent(intent: Intent?): IntentValidationResult {
        if (intent == null) {
            Log.w(TAG, "Intent null reÃ§u â€” rejet sÃ©curisÃ©")
            return IntentValidationResult.Invalid("Intent manquant")
        }

        return try {
            val candidateUrl = extractCandidateUrl(intent)
            validateVideoUrl(candidateUrl)
        } catch (e: Exception) {
            // âœ… Garde-fou ultime : un Intent malformÃ© ne doit jamais crasher l'app
            Log.e(TAG, "Erreur inattendue lors de la validation de l'intent", e)
            IntentValidationResult.Invalid("Erreur lors de l'analyse de l'intent")
        }
    }

    /**
     * Valide directement une URL (string), indÃ©pendamment de tout Intent.
     * - Rejette les URLs nulles/vides/trop longues.
     * - Rejette tout schÃ©ma diffÃ©rent de http/https (file://, content://, javascript:, intent://, ...).
     * - Rejette les URLs sans hÃ´te aprÃ¨s le schÃ©ma.
     */
    fun validateVideoUrl(url: String?): IntentValidationResult {
        val safeUrl = try {
            url?.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de traiter l'URL fournie", e)
            null
        }

        if (safeUrl.isNullOrBlank()) {
            return IntentValidationResult.Invalid("URL manquante ou vide")
        }

        if (safeUrl.length > MAX_URL_LENGTH) {
            Log.w(TAG, "URL rejetÃ©e : trop longue (${safeUrl.length} caractÃ¨res)")
            return IntentValidationResult.Invalid("URL trop longue")
        }

        val scheme = extractScheme(safeUrl)
        if (scheme == null || scheme !in ALLOWED_SCHEMES) {
            Log.w(TAG, "URL rejetÃ©e : schÃ©ma non autorisÃ© ('$scheme')")
            return IntentValidationResult.Invalid("SchÃ©ma non autorisÃ©")
        }

        val afterScheme = safeUrl.substring(scheme.length + 3) // retire "http://" / "https://"
        if (afterScheme.isBlank() || afterScheme.startsWith("/")) {
            Log.w(TAG, "URL rejetÃ©e : hÃ´te manquant")
            return IntentValidationResult.Invalid("HÃ´te manquant dans l'URL")
        }

        return IntentValidationResult.Valid(safeUrl)
    }

    /**
     * True si l'URL se termine par une extension vidÃ©o connue (m3u8, mp4, mpd, ...).
     * Purement informatif â€” n'est PAS requis pour accepter une URL (les flux live n'ont
     * pas toujours d'extension), mais utile pour un filtrage additionnel cÃ´tÃ© UI.
     */
    fun hasVideoExtension(url: String?): Boolean {
        val safeUrl = try {
            url?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return false

        val withoutQuery = safeUrl.substringBefore('?')
        return ALLOWED_VIDEO_EXTENSIONS.any { withoutQuery.endsWith(it) }
    }

    /**
     * Extrait l'URL candidate depuis l'Intent : extra explicite `VIDEO_URL`,
     * deep link interne `stv://play?url=...`, ou data URI directe (`http(s)://...`).
     * 100% Safe-call : ne crash jamais si les extras/data sont null ou mal formÃ©s.
     */
    private fun extractCandidateUrl(intent: Intent): String? {
        // 1. Extra explicite (le plus exposÃ© Ã  l'Intent Hijacking si non validÃ© ensuite)
        val extraUrl = try {
            intent.getStringExtra(EXTRA_VIDEO_URL)
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de lire l'extra '$EXTRA_VIDEO_URL'", e)
            null
        }
        if (!extraUrl.isNullOrBlank()) return extraUrl

        // 2. Data URI (deep link ou ACTION_VIEW)
        val data = try {
            intent.data
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de lire l'intent.data", e)
            null
        } ?: return null

        return try {
            if (data.scheme == DEEP_LINK_SCHEME && data.host == DEEP_LINK_HOST) {
                // Deep link interne stv://play?url=...
                data.getQueryParameter("url")
            } else {
                data.toString()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de rÃ©soudre l'URI depuis intent.data", e)
            null
        }
    }

    /**
     * Extrait le schÃ©ma d'une URL de faÃ§on purement textuelle (pas de dÃ©pendance Ã 
     * `android.net.Uri.parse`, ce qui rend la validation testable en JVM pur).
     */
    private fun extractScheme(url: String): String? {
        val idx = url.indexOf("://")
        if (idx <= 0) return null
        return url.substring(0, idx).lowercase()
    }
}


