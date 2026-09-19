package com.example.stv.core.security

import android.content.Intent
import android.util.Log

/**
 * Résultat de la validation de sécurité d'une URI/Intent vidéo.
 * Utilisé par les Activities pour décider s'il faut charger la vidéo
 * ou rejeter l'Intent en toute sécurité (pas de crash, pas de leak d'info).
 */
sealed class IntentValidationResult {
    data class Valid(val videoUrl: String) : IntentValidationResult()
    data class Invalid(val reason: String) : IntentValidationResult()
}

/**
 * Sécurise les Intents/Deep Links entrants pour se prémunir contre :
 *  - **Intent Hijacking** : une app malveillante envoie un Intent à une Activity exportée
 *    (`MainActivity` / `PlayerActivity`) avec des extras/data forgés.
 *  - **Intent Redirection** : injection de schémas dangereux (`file://`, `content://`,
 *    `javascript:`, `intent://`, etc.) qui pourraient exposer le système de fichiers,
 *    exécuter du JS dans une WebView, ou rediriger vers un composant non exporté.
 *
 * Règle stricte : **seuls `http://` et `https://` sont acceptés** pour la lecture vidéo.
 * Toute extraction est 100% Safe-call / try-catch : un Intent malformé ne doit
 * JAMAIS faire crasher l'application (conformité Play Store).
 */
class IntentSecurityManager {

    companion object {
        private const val TAG = "IntentSecurityManager"

        /** Seuls ces schémas sont autorisés pour la lecture vidéo. */
        private val ALLOWED_SCHEMES = setOf("http", "https")

        /** Extensions vidéo reconnues (utilisé pour un filtrage additionnel optionnel). */
        private val ALLOWED_VIDEO_EXTENSIONS = listOf(".m3u8", ".mp4", ".mpd", ".ts", ".webm", ".mkv")

        private const val EXTRA_VIDEO_URL = "VIDEO_URL"
        private const val DEEP_LINK_SCHEME = "stv"
        private const val DEEP_LINK_HOST = "play"
        private const val MAX_URL_LENGTH = 2048
    }

    /**
     * Point d'entrée principal : extrait puis valide l'URL vidéo contenue dans un Intent entrant.
     * Ne lève jamais d'exception.
     */
    fun validateIncomingIntent(intent: Intent?): IntentValidationResult {
        if (intent == null) {
            Log.w(TAG, "Intent null reçu — rejet sécurisé")
            return IntentValidationResult.Invalid("Intent manquant")
        }

        return try {
            val candidateUrl = extractCandidateUrl(intent)
            validateVideoUrl(candidateUrl)
        } catch (e: Exception) {
            // ✅ Garde-fou ultime : un Intent malformé ne doit jamais crasher l'app
            Log.e(TAG, "Erreur inattendue lors de la validation de l'intent", e)
            IntentValidationResult.Invalid("Erreur lors de l'analyse de l'intent")
        }
    }

    /**
     * Valide directement une URL (string), indépendamment de tout Intent.
     * - Rejette les URLs nulles/vides/trop longues.
     * - Rejette tout schéma différent de http/https (file://, content://, javascript:, intent://, ...).
     * - Rejette les URLs sans hôte après le schéma.
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
            Log.w(TAG, "URL rejetée : trop longue (${safeUrl.length} caractères)")
            return IntentValidationResult.Invalid("URL trop longue")
        }

        val scheme = extractScheme(safeUrl)
        if (scheme == null || scheme !in ALLOWED_SCHEMES) {
            Log.w(TAG, "URL rejetée : schéma non autorisé ('$scheme')")
            return IntentValidationResult.Invalid("Schéma non autorisé")
        }

        val afterScheme = safeUrl.substring(scheme.length + 3) // retire "http://" / "https://"
        if (afterScheme.isBlank() || afterScheme.startsWith("/")) {
            Log.w(TAG, "URL rejetée : hôte manquant")
            return IntentValidationResult.Invalid("Hôte manquant dans l'URL")
        }

        return IntentValidationResult.Valid(safeUrl)
    }

    /**
     * True si l'URL se termine par une extension vidéo connue (m3u8, mp4, mpd, ...).
     * Purement informatif — n'est PAS requis pour accepter une URL (les flux live n'ont
     * pas toujours d'extension), mais utile pour un filtrage additionnel côté UI.
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
     * 100% Safe-call : ne crash jamais si les extras/data sont null ou mal formés.
     */
    private fun extractCandidateUrl(intent: Intent): String? {
        // 1. Extra explicite (le plus exposé à l'Intent Hijacking si non validé ensuite)
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
            Log.w(TAG, "Impossible de résoudre l'URI depuis intent.data", e)
            null
        }
    }

    /**
     * Extrait le schéma d'une URL de façon purement textuelle (pas de dépendance à
     * `android.net.Uri.parse`, ce qui rend la validation testable en JVM pur).
     */
    private fun extractScheme(url: String): String? {
        val idx = url.indexOf("://")
        if (idx <= 0) return null
        return url.substring(0, idx).lowercase()
    }
}

