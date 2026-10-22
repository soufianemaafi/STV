package com.stv.videoplayer.player

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Transport des headers HTTP DANS le [MediaItem] (`RequestMetadata.extras`).
 *
 * `MediaItem.RequestMetadata` n'a pas de champ "headers" natif et aucune `DataSource` Media3
 * ne le lit : on y stocke donc les headers (sous l'extra [HeaderExtractor.EXTRA_HEADERS]) et
 * [HeaderAwareMediaSourceFactory] les relit pour CHAQUE item. Aucun état global n'est impliqué.
 */
object MediaItemHeaders {

    /** Ajoute les headers (assainis) au builder. Sans header exploitable, le builder est inchangé. */
    fun write(builder: MediaItem.Builder, headers: Map<String, String>): MediaItem.Builder {
        val clean = HeaderExtractor.parse(headers)
        if (clean.isEmpty()) return builder

        val headersBundle = Bundle().apply { clean.forEach { (name, value) -> putString(name, value) } }
        val extras = Bundle().apply { putBundle(HeaderExtractor.EXTRA_HEADERS, headersBundle) }
        return builder.setRequestMetadata(MediaItem.RequestMetadata.Builder().setExtras(extras).build())
    }

    /** Relit (et ré-assainit) les headers portés par un [MediaItem]. Map vide si aucun. */
    fun read(mediaItem: MediaItem): Map<String, String> {
        return HeaderExtractor.extract(mediaItem.requestMetadata.extras)
    }
}

/**
 * [MediaSource.Factory] qui applique, item par item, les headers HTTP portés par
 * `MediaItem.RequestMetadata.extras` (voir [MediaItemHeaders]).
 *
 * Remplace l'ancien `DataSource.Factory` qui lisait une variable statique partagée
 * (`pendingRequestHeaders`) : les headers d'une session ne peuvent plus fuiter vers une autre.
 */
@OptIn(UnstableApi::class)
class HeaderAwareMediaSourceFactory : MediaSource.Factory {

    private var drmSessionManagerProvider: DrmSessionManagerProvider? = null
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy? = null

    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider
    ): MediaSource.Factory = apply { this.drmSessionManagerProvider = drmSessionManagerProvider }

    override fun setLoadErrorHandlingPolicy(
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy
    ): MediaSource.Factory = apply { this.loadErrorHandlingPolicy = loadErrorHandlingPolicy }

    override fun getSupportedTypes(): IntArray = delegateFor(emptyMap()).supportedTypes

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        return delegateFor(MediaItemHeaders.read(mediaItem)).createMediaSource(mediaItem)
    }

    private fun delegateFor(headers: Map<String, String>): DefaultMediaSourceFactory {
        val requestProperties = LinkedHashMap(headers)
        val userAgentKey = requestProperties.keys.firstOrNull { it.equals(HEADER_USER_AGENT, ignoreCase = true) }
        val userAgent = userAgentKey?.let { requestProperties.remove(it) } ?: DEFAULT_USER_AGENT

        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(HTTP_TIMEOUT_MS)
            .setReadTimeoutMs(HTTP_TIMEOUT_MS)
            .setUserAgent(userAgent)
        if (requestProperties.isNotEmpty()) {
            httpFactory.setDefaultRequestProperties(requestProperties)
        }

        return DefaultMediaSourceFactory(httpFactory).also { factory ->
            drmSessionManagerProvider?.let { factory.setDrmSessionManagerProvider(it) }
            loadErrorHandlingPolicy?.let { factory.setLoadErrorHandlingPolicy(it) }
        }
    }

    private companion object {
        const val HEADER_USER_AGENT = "User-Agent"
        const val HTTP_TIMEOUT_MS = 8_000
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 STV/1.0"
    }
}

