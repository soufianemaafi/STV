package com.example.soukitv.core.player
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.content.pm.Signature
import androidx.core.content.pm.PackageInfoCompat
import com.example.soukitv.BuildConfig
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.StreamSource
import com.example.soukitv.features.home.domain.model.toChannel
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.security.MessageDigest
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
    fun launchChannel(
        channel: Channel,
        authSecret: String = "",
        minStvVersionCode: Long = minVersionCode
    ): LaunchResult =
        launch(
            channel = channel,
            sources = null,
            headers = emptyMap(),
            authSecret = authSecret,
            minStvVersionCode = minStvVersionCode
        )
    /** Variante avec en-têtes HTTP optionnels (Referer, User-Agent, jetons Nginx...). */
    fun launchChannel(
        channel: Channel,
        headers: Map<String, String>,
        authSecret: String = "",
        minStvVersionCode: Long = minVersionCode
    ): LaunchResult =
        launch(
            channel = channel,
            sources = null,
            headers = headers,
            authSecret = authSecret,
            minStvVersionCode = minStvVersionCode
        )
    /**
     * Lance un match. Si [MatchItem.sources] n'est pas vide, la liste est sérialisée en JSON
     * (extra `sources_json`) et la première source devient l'URL par défaut (`VIDEO_URL`).
     */
    fun launchMatch(
        match: MatchItem,
        authSecret: String = "",
        minStvVersionCode: Long = minVersionCode
    ): LaunchResult = launchMatch(match, emptyMap(), authSecret, minStvVersionCode)
    fun launchMatch(
        match: MatchItem,
        headers: Map<String, String>,
        authSecret: String = "",
        minStvVersionCode: Long = minVersionCode
    ): LaunchResult {
        val primaryUrl = match.streamUrl?.trim().orEmpty()
        val sources = match.sources
            ?.mapNotNull { source ->
                source.url.trim()
                    .takeIf(::isAllowedUrl)
                    ?.let { url -> source.copy(url = url) }
            }
            ?.takeIf { it.isNotEmpty() }
        val streamUrl = primaryUrl
            .takeIf(::isAllowedUrl)
            ?: sources?.firstOrNull()?.url?.trim()
            ?: return LaunchResult.Failed("Aucun flux vidéo disponible pour cet événement.")
        val baseChannel = match.toChannel()
        val channel = baseChannel.copy(streamUrl = streamUrl)
        return launch(
            channel = channel,
            sources = sources,
            headers = headers,
            authSecret = authSecret,
            minStvVersionCode = minStvVersionCode
        )
    }
    private fun launch(
        channel: Channel,
        sources: List<StreamSource>?,
        headers: Map<String, String>,
        authSecret: String,
        minStvVersionCode: Long
    ): LaunchResult {
        // Rejeter immédiatement les URLs vides ou non HTTP(S), avant toute résolution de package.
        if (!isAllowedUrl(channel.streamUrl)) {
            return LaunchResult.Failed("Unsupported stream URL")
        }
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
                setDataAndType(uriParser(channel.streamUrl), VIDEO_MIME_TYPE)
                putExtra(EXTRA_VIDEO_URL, channel.streamUrl)
                putExtra(EXTRA_URL, channel.streamUrl)
                putExtra(EXTRA_TITLE, channel.name)
                if (!sources.isNullOrEmpty()) {
                    putExtra(EXTRA_SOURCES_JSON, sourcesToJson(sources))
                }
                putHeaders(this, headers, authSecret)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult.Launched
        } catch (e: Exception) {
            LaunchResult.Failed(e.message ?: "Error launching STV Player")
        }
    }
    /** Convertit la Map d'en-têtes en Bundle (`headers`) + extra `referer` pour compatibilité. */
    private fun putHeaders(intent: Intent, headers: Map<String, String>, authSecret: String) {
        val clean = headers.filter { (key, value) -> key.isNotBlank() && value.isNotBlank() }
        val bundle = bundleFactory().apply {
            clean.forEach { (key, value) -> putString(key.trim(), value) }
            putString("X-STV-Auth", authSecret)
            putString("User-Agent", "NexiPlayer-Official-Client/v3.0")
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
            val expectedDigests = listOf(
                unmaskDigest(MASKED_SIGNATURE_ONE),
                unmaskDigest(MASKED_SIGNATURE_TWO)
            )

            signatures.any { signature ->
                val actualDigest = MessageDigest.getInstance("SHA-256")
                    .digest(signature.toByteArray())
                expectedDigests.any { expectedDigest ->
                    MessageDigest.isEqual(actualDigest, expectedDigest)
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun unmaskDigest(maskedDigest: ByteArray): ByteArray =
        maskedDigest.map { (it.toInt() xor XOR_KEY.toInt()).toByte() }.toByteArray()
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
        private const val VIDEO_MIME_TYPE = "video/*"
        private const val HEADER_REFERER = "Referer"
        private const val GOOGLE_PLAY_PACKAGE = "com.android.vending"
        private const val PLAY_STORE_MARKET_URL = "market://details?id=$PACKAGE_PROD"
        private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_PROD"
        private const val XOR_KEY: Byte = 0x5A
        private val MASKED_SIGNATURE_ONE = byteArrayOf(
            0xF3.toByte(), 0xE8.toByte(), 0x0A, 0x26, 0x0B, 0x20, 0xA2.toByte(), 0xF6.toByte(),
            0x21, 0xF3.toByte(), 0x8D.toByte(), 0x86.toByte(), 0x2D, 0xCB.toByte(), 0x57, 0xA3.toByte(),
            0xB8.toByte(), 0xA5.toByte(), 0xC3.toByte(), 0xD5.toByte(), 0x36, 0x5C, 0xD9.toByte(), 0x3C,
            0xF4.toByte(), 0xC5.toByte(), 0xF0.toByte(), 0x70, 0x9C.toByte(), 0x3A, 0xB2.toByte(), 0x45
        )
        private val MASKED_SIGNATURE_TWO = byteArrayOf(
            0x92.toByte(), 0x67, 0x30, 0x13, 0x2A, 0x4B, 0x4F, 0xEE.toByte(),
            0xFD.toByte(), 0x2A, 0x1C, 0x14, 0x76, 0x4A, 0x5C, 0x57,
            0xF4.toByte(), 0x90.toByte(), 0x79, 0xD8.toByte(), 0x9B.toByte(), 0x2E, 0xB5.toByte(), 0xBB.toByte(),
            0x99.toByte(), 0xE6.toByte(), 0xD6.toByte(), 0x05, 0xC4.toByte(), 0x69, 0x40, 0x43
        )
        /** Ordre = priorité : production d'abord, puis variante dev. */
        private val TARGET_PACKAGES = listOf(PACKAGE_PROD, PACKAGE_DEV)
    }
}