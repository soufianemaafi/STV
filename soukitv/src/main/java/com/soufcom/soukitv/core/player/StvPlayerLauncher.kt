package com.soufcom.soukitv.core.player
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.content.pm.Signature
import androidx.core.content.pm.PackageInfoCompat
import com.soufcom.soukitv.BuildConfig
import com.soufcom.soukitv.core.security.AppIntegrityGuard
import com.soufcom.soukitv.core.security.SecurityVault
import com.soufcom.soukitv.core.security.UnavailableSecurityVault
import com.soufcom.soukitv.core.security.defaultTrustedHosts
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import com.soufcom.soukitv.features.home.domain.model.Channel
import com.soufcom.soukitv.features.home.domain.model.MatchItem
import com.soufcom.soukitv.features.home.domain.model.StreamSource
import com.soufcom.soukitv.features.home.domain.model.toChannel
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Proxy
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
/**
 * Lance un flux dans STV Player via un Intent explicite (contrat IPC immuable).
 *
 * Sécurité :
 * - cible EXCLUSIVEMENT `com.soufcom.stv` (repli `com.soufcom.stv.dev`) via `setPackage` ;
 * - seules les URL HTTP(S) sont transmises ;
 * - STV Player doit avoir un `versionCode` >= [minVersionCode], sinon [LaunchResult.UpdateRequired].
 *
 * Les dépendances Android (Intent, Bundle, Uri) sont injectables pour permettre des tests JVM purs.
 */
class StvPlayerLauncher(
    private val context: Context,
    private val minVersionCode: Long = MIN_STV_VERSION_CODE,
    private val intentFactory: (action: String) -> Intent = { Intent(it) },
    private val bundleFactory: () -> Bundle = { Bundle() },
    private val uriParser: (String) -> Uri = { Uri.parse(it) },
    private val streamDispatcher: StreamDispatcher = OkHttpStreamDispatcher(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val integrityGuard: AppIntegrityGuard? = null,
    private val securityVault: SecurityVault = UnavailableSecurityVault
) {
    suspend fun launchChannel(
        channel: Channel,
        authSecret: String = "",
        minStvVersionCode: Long = minVersionCode,
        trustedHosts: Set<String> = defaultTrustedHosts(securityVault)
    ): LaunchResult =
        launch(
            channel = channel,
            sources = null,
            headers = emptyMap(),
            authSecret = authSecret,
            minStvVersionCode = minStvVersionCode,
            trustedHosts = trustedHosts
        )
    /** Variante avec en-têtes HTTP optionnels (Referer, User-Agent, jetons Nginx...). */
    suspend fun launchChannel(
        channel: Channel,
        headers: Map<String, String>,
        authSecret: String = "",
        minStvVersionCode: Long = minVersionCode,
        trustedHosts: Set<String> = defaultTrustedHosts(securityVault)
    ): LaunchResult =
        launch(
            channel = channel,
            sources = null,
            headers = headers,
            authSecret = authSecret,
            minStvVersionCode = minStvVersionCode,
            trustedHosts = trustedHosts
        )
    /**
     * Lance un match. Si [MatchItem.sources] n'est pas vide, la liste est sérialisée en JSON
     * (extra `sources_json`) et la première source devient l'URL par défaut (`VIDEO_URL`).
     */
    suspend fun launchMatch(
        match: MatchItem,
        authSecret: String = "",
        minStvVersionCode: Long = minVersionCode,
        trustedHosts: Set<String> = defaultTrustedHosts(securityVault)
    ): LaunchResult = launchMatch(match, emptyMap(), authSecret, minStvVersionCode, trustedHosts)
    suspend fun launchMatch(
        match: MatchItem,
        headers: Map<String, String>,
        authSecret: String = "",
        minStvVersionCode: Long = minVersionCode,
        trustedHosts: Set<String> = defaultTrustedHosts(securityVault)
    ): LaunchResult {
        val primaryUrl = match.streamUrl?.trim().orEmpty()
        val sources = match.sources
            ?.filter { source -> isAllowedUrl(source.url) }
            ?.takeIf { it.isNotEmpty() }
        val streamUrl = primaryUrl
            .takeIf(::isAllowedUrl)
            ?: sources?.firstOrNull()?.url
            ?: return LaunchResult.Failed("Aucun flux vidéo disponible pour cet événement.")
        val baseChannel = match.toChannel()
        val channel = baseChannel.copy(streamUrl = streamUrl)
        return launch(
            channel = channel,
            sources = sources,
            headers = headers,
            authSecret = authSecret,
            minStvVersionCode = minStvVersionCode,
            trustedHosts = trustedHosts
        )
    }
    private suspend fun launch(
        channel: Channel,
        sources: List<StreamSource>?,
        headers: Map<String, String>,
        authSecret: String,
        minStvVersionCode: Long,
        trustedHosts: Set<String>
    ): LaunchResult {
        // Rejeter immédiatement les URLs vides ou non HTTP(S), avant toute résolution de package.
        if (!isAllowedUrl(channel.streamUrl)) {
            return LaunchResult.Failed("Unsupported stream URL")
        }
        if (authSecret.isBlank()) {
            return LaunchResult.Failed(SECURITY_CONFIGURATION_ERROR_MESSAGE)
        }
        // Avant toute résolution /dispatch : le secret X-STV-Auth ne sort jamais d'un environnement non fiable.
        if (integrityGuard != null && !withContext(ioDispatcher) { integrityGuard.isTrusted() }) {
            return LaunchResult.Failed(INTEGRITY_ERROR_MESSAGE)
        }
        val dispatchTrustedHosts = trustedHosts.map { it.trim().lowercase() }.toSet()
        val dispatchUrl = sequenceOf(channel.streamUrl)
            .plus(sources.orEmpty().asSequence().map(StreamSource::url))
            .firstOrNull { requiresDispatch(it, dispatchTrustedHosts) }
        val resolvedEdgeUrl = dispatchUrl?.let {
            resolveDispatchUrl(it, authSecret)
        }
        val sessionTrustedHosts = dispatchTrustedHosts + listOfNotNull(
            resolvedEdgeUrl?.let(::hostOf)
        )
        val rewrittenSources = sources?.map { source ->
            if (requiresDispatch(source.url, dispatchTrustedHosts) && resolvedEdgeUrl != null) {
                source.copy(url = resolvedEdgeUrl)
            } else {
                source
            }
        }
        val resolvedPrimaryUrl = when {
            requiresDispatch(channel.streamUrl, dispatchTrustedHosts) -> resolvedEdgeUrl
            else -> channel.streamUrl
        }
        val fallbackUrl = sources
            .orEmpty()
            .firstOrNull { !requiresDispatch(it.url, dispatchTrustedHosts) }
            ?.url
        val finalUrl = resolvedPrimaryUrl
            ?: fallbackUrl
            ?: return LaunchResult.Failed("Impossible de résoudre le flux vidéo.")
        val resolvedChannel = channel.copy(streamUrl = finalUrl)
        val includeAuth = (listOf(finalUrl) + rewrittenSources.orEmpty().map(StreamSource::url))
            .any { hostOf(it) in sessionTrustedHosts }
        // 1. Package cible : production en priorité stricte, puis dev.
        val target = TARGET_PACKAGES.firstNotNullOfOrNull { packageName ->
            installedVersionCode(packageName)?.let { packageName to it }
        } ?: return LaunchResult.NotInstalled
        val (targetPackage, versionCode) = target
        // 2. Force update.
        if (versionCode < minStvVersionCode) return LaunchResult.UpdateRequired
        // 3. Vérifie que le lecteur cible est signé par une clé officielle.
        if (!isTargetPackageSignatureValid(targetPackage)) {
            return LaunchResult.Failed(
                "Version non officielle du lecteur détectée. Veuillez installer la version officielle depuis le Google Play Store."
            )
        }
        return try {
            val intent = intentFactory(PLAY_STREAM_ACTION).apply {
                setPackage(targetPackage)
                setDataAndType(uriParser(resolvedChannel.streamUrl), VIDEO_MIME_TYPE)
                putExtra(EXTRA_VIDEO_URL, resolvedChannel.streamUrl)
                putExtra(EXTRA_URL, resolvedChannel.streamUrl)
                putExtra(EXTRA_TITLE, resolvedChannel.name)
                if (!rewrittenSources.isNullOrEmpty()) {
                    putExtra(EXTRA_SOURCES_JSON, sourcesToJson(rewrittenSources))
                }
                putHeaders(this, headers, authSecret, includeAuth)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult.Launched
        } catch (e: Exception) {
            LaunchResult.Failed(e.message ?: "Error launching STV Player")
        }
    }

    /** Résout un seul ticket du balancer avant de le transmettre au lecteur. */
    private suspend fun resolveDispatchUrl(dispatchUrl: String, authSecret: String): String? =
        withContext(ioDispatcher) {
            runCatching { streamDispatcher.resolve(dispatchUrl, authSecret) }
                .getOrNull()
                ?.trim()
                ?.takeIf(::isAllowedUrl)
        }

    /** `/dispatch` n'est traité comme tel que sur un hôte STV de confiance (http/https). */
    private fun requiresDispatch(url: String, trustedHosts: Set<String>): Boolean {
        val parsed = url.trim().toHttpUrlOrNull() ?: return false
        val path = parsed.encodedPath
        return parsed.host.lowercase() in trustedHosts &&
            (path == "/dispatch" || path.startsWith("/dispatch/"))
    }

    private fun hostOf(url: String): String? =
        url.trim().toHttpUrlOrNull()?.host?.lowercase()

    /** Convertit la Map d'en-têtes en Bundle (`headers`) + extra `referer` pour compatibilité. */
    private fun putHeaders(
        intent: Intent,
        headers: Map<String, String>,
        authSecret: String,
        includeAuth: Boolean
    ) {
        val clean = headers.filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
        val bundle = bundleFactory().apply {
            clean.forEach { (key, value) -> putString(key.trim(), value) }
            if (includeAuth) putString("X-STV-Auth", authSecret)
            putString("User-Agent", OFFICIAL_USER_AGENT)
        }
        intent.putExtra(EXTRA_HEADERS, bundle)
        clean.entries
            .firstOrNull { it.key.trim().equals(HEADER_REFERER, ignoreCase = true) }
            ?.let { intent.putExtra(EXTRA_REFERER, it.value) }
    }
    /** versionCode de [packageName], ou null si le package n'est pas installé. */
    @Suppress("DEPRECATION")
    private fun installedVersionCode(packageName: String): Long? = try {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(packageName, 0))
    } catch (_: Exception) {
        null
    }

    private fun isTargetPackageSignatureValid(packageName: String): Boolean {
        if (BuildConfig.DEBUG) return true

        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
            val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures ?: emptyArray()
            }
            val expectedDigests = securityVault.playerDigests()
            signatures.any { signature ->
                val actualDigest = MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                expectedDigests.any { expectedDigest ->
                expectedDigest.size == 32 &&
                    MessageDigest.isEqual(actualDigest, expectedDigest)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Sérialise les sources en JSON `[{"quality":..,"url":..}]` sans réflexion (R8-safe). */
    private fun sourcesToJson(sources: List<StreamSource>): String {
        val array = JsonArray()
        sources.forEach { source ->
            array.add(
                JsonObject().apply {
                    addProperty("quality", source.quality)
                    addProperty("url", source.url)
                }
            )
        }
        return array.toString()
    }

    private fun isAllowedUrl(url: String): Boolean {
        val lower = url.trim().lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }
    fun openPlayStore() {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_MARKET_URL)).apply {
            setPackage(GOOGLE_PLAY_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_WEB_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
    sealed interface LaunchResult {
        data object Launched : LaunchResult
        data object NotInstalled : LaunchResult
        /** STV Player est installé mais trop ancien : mise à jour Play Store requise. */
        data object UpdateRequired : LaunchResult
        data class Failed(val message: String) : LaunchResult
    }
    companion object {
        const val PLAY_STREAM_ACTION = "com.soufcom.stv.action.PLAY_STREAM"
        const val PACKAGE_PROD = "com.soufcom.stv"
        const val PACKAGE_DEV = "com.soufcom.stv.dev"
        const val MIN_STV_VERSION_CODE = 2L
        const val EXTRA_VIDEO_URL = "VIDEO_URL"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_HEADERS = "headers"
        const val EXTRA_REFERER = "referer"
        const val EXTRA_SOURCES_JSON = "sources_json"
        const val NOT_INSTALLED_MESSAGE = "STV Player is required to play this content."
        const val UPDATE_REQUIRED_MESSAGE =
            "Please update STV Player from the Google Play Store to play this content."
        const val SECURITY_CONFIGURATION_ERROR_MESSAGE =
            "Configuration de sécurité indisponible. Veuillez vérifier votre connexion."
        const val INTEGRITY_ERROR_MESSAGE =
            "Environnement non sécurisé détecté. Lancement du flux refusé."
        private const val VIDEO_MIME_TYPE = "video/*"
        const val OFFICIAL_USER_AGENT = "NexiPlayer-Official-Client/v3.0"
        private const val HEADER_REFERER = "Referer"
        private const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
        private const val PLAY_STORE_MARKET_URL = "market://details?id=$PACKAGE_PROD"
        private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_PROD"
        private val TARGET_PACKAGES = listOf(PACKAGE_PROD, PACKAGE_DEV)
    }
}

/** Résolveur isolé pour rendre la résolution réseau testable sur JVM pure. */
fun interface StreamDispatcher {
    fun resolve(dispatchUrl: String, authSecret: String): String?
}

/** Implémentation de production du contrat sécurisé du balancer. */
private class OkHttpStreamDispatcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .proxy(Proxy.NO_PROXY)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
) : StreamDispatcher {
    override fun resolve(dispatchUrl: String, authSecret: String): String? {
        val request = Request.Builder()
            .url(dispatchUrl)
            .header("X-STV-Auth", authSecret)
            .header("User-Agent", StvPlayerLauncher.OFFICIAL_USER_AGENT)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful || response.code != 200) return null
            return response.body?.string()?.trim()?.takeIf(String::isNotEmpty)
        }
    }
}
