package com.example.soukitv.core.player
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.StreamSource
import com.example.soukitv.features.home.domain.model.toChannel
import com.google.gson.JsonArray
import com.google.gson.JsonObject
/**
 * Lance un flux dans STV Player via un Intent explicite (contrat IPC immuable).
 *
 * Sécurité :
 * - cible EXCLUSIVEMENT `com.soufcom.stv` (repli `com.soufcom.stv.dev`) via `setPackage` ;
 * - seules les URL http/https sont transmises ;
 * - STV Player doit avoir un `versionCode` >= [minVersionCode], sinon [LaunchResult.UpdateRequired].
 *
 * Les dépendances Android (Intent, Bundle, Uri) sont injectables pour permettre des tests JVM purs.
 */
class StvPlayerLauncher(
    private val context: Context,
    private val minVersionCode: Long = MIN_STV_VERSION_CODE,
    private val intentFactory: (action: String) -> Intent = { Intent(it) },
    private val bundleFactory: () -> Bundle = { Bundle() },
    private val uriParser: (String) -> Uri = { Uri.parse(it) }
) {
    fun launchChannel(channel: Channel): LaunchResult =
        launch(channel, sources = null, headers = emptyMap())
    /** Variante avec en-têtes HTTP optionnels (Referer, User-Agent, jetons Nginx...). */
    fun launchChannel(channel: Channel, headers: Map<String, String>): LaunchResult =
        launch(channel, sources = null, headers = headers)
    /**
     * Lance un match. Si [MatchItem.sources] n'est pas vide, la liste est sérialisée en JSON
     * (extra `sources_json`) et la première source devient l'URL par défaut (`VIDEO_URL`).
     */
    fun launchMatch(match: MatchItem): LaunchResult = launchMatch(match, emptyMap())
    fun launchMatch(match: MatchItem, headers: Map<String, String>): LaunchResult {
        val sources = match.sources
            ?.filter { it.url.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
        val baseChannel = match.toChannel()
        val channel = if (sources != null) baseChannel.copy(streamUrl = sources.first().url) else baseChannel
        return launch(channel, sources, headers)
    }
    private fun launch(
        channel: Channel,
        sources: List<StreamSource>?,
        headers: Map<String, String>
    ): LaunchResult {
        // 1. Package cible : production en priorité stricte, puis dev.
        val target = TARGET_PACKAGES.firstNotNullOfOrNull { packageName ->
            installedVersionCode(packageName)?.let { packageName to it }
        } ?: return LaunchResult.NotInstalled
        val (targetPackage, versionCode) = target
        // 2. Force update.
        if (versionCode < minVersionCode) return LaunchResult.UpdateRequired
        // 3. Seuls http/https sont autorisés (anti-hijacking).
        if (!isAllowedUrl(channel.streamUrl)) {
            return LaunchResult.Failed("Unsupported stream URL")
        }
        return try {
            val intent = intentFactory(PLAY_STREAM_ACTION).apply {
                setPackage(targetPackage)
                setDataAndType(uriParser(channel.streamUrl), VIDEO_MIME_TYPE)
                putExtra(EXTRA_VIDEO_URL, channel.streamUrl)
                putExtra(EXTRA_URL, channel.streamUrl)
                putExtra(EXTRA_TITLE, channel.name)
                if (!sources.isNullOrEmpty()) {
                    putExtra(EXTRA_SOURCES_JSON, sourcesToJson(sources))
                }
                putHeaders(this, headers)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult.Launched
        } catch (e: Exception) {
            LaunchResult.Failed(e.message ?: "Error launching STV Player")
        }
    }
    /** Convertit la Map d'en-têtes en Bundle (`headers`) + extra `referer` pour compatibilité. */
    private fun putHeaders(intent: Intent, headers: Map<String, String>) {
        val clean = headers.filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
        if (clean.isEmpty()) return
        val bundle = bundleFactory()
        clean.forEach { (key, value) -> bundle.putString(key.trim(), value) }
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
        private const val VIDEO_MIME_TYPE = "video/*"
        private const val HEADER_REFERER = "Referer"
        private const val PLAY_STORE_MARKET_URL = "market://details?id=$PACKAGE_PROD"
        private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_PROD"
        /** Ordre = priorité : production d'abord, puis variante dev. */
        private val TARGET_PACKAGES = listOf(PACKAGE_PROD, PACKAGE_DEV)
    }
}