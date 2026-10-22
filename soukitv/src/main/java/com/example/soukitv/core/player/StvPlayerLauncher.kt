package com.example.soukitv.core.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.StreamSource
import com.example.soukitv.features.home.domain.model.toChannel
import com.google.gson.Gson

class StvPlayerLauncher(private val context: Context) {

    fun launchChannel(channel: Channel): LaunchResult = launch(channel, sources = null)

    /**
     * Lance un match. Si [MatchItem.sources] n'est pas vide, la liste est sérialisée en JSON
     * (extra `sources_json`) et la première source devient l'URL par défaut (`VIDEO_URL`).
     */
    fun launchMatch(match: MatchItem): LaunchResult {
        val sources = match.sources
            ?.filter { it.url.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
        val baseChannel = match.toChannel()
        val channel = if (sources != null) baseChannel.copy(streamUrl = sources.first().url) else baseChannel
        return launch(channel, sources)
    }

    private fun launch(channel: Channel, sources: List<StreamSource>?): LaunchResult {
        val installedPackage = SUPPORTED_PACKAGES.firstOrNull { packageName ->
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                true
            } catch (_: Exception) {
                false
            }
        }

        if (installedPackage == null) return LaunchResult.NotInstalled

        return try {
            val intent = Intent(PLAY_STREAM_ACTION).apply {
                setPackage(installedPackage)
                val uri = channel.streamUrl.toUri()
                setDataAndType(uri, "video/*")
                putExtra(EXTRA_VIDEO_URL, channel.streamUrl)
                putExtra(EXTRA_URL, channel.streamUrl)
                putExtra(EXTRA_TITLE, channel.name)
                if (!sources.isNullOrEmpty()) {
                    putExtra(EXTRA_SOURCES_JSON, Gson().toJson(sources))
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult.Launched
        } catch (e: Exception) {
            LaunchResult.Failed(e.message ?: "Error launching STV Player")
        }
    }

    fun openPlayStore() {
        val marketIntent = Intent(Intent.ACTION_VIEW, PLAY_STORE_MARKET_URL.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, PLAY_STORE_WEB_URL.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    sealed interface LaunchResult {
        data object Launched : LaunchResult
        data object NotInstalled : LaunchResult
        data class Failed(val message: String) : LaunchResult
    }

    companion object {
        const val PLAY_STREAM_ACTION = "com.soufcom.stv.action.PLAY_STREAM"
        const val EXTRA_VIDEO_URL = "VIDEO_URL"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SOURCES_JSON = "sources_json"
        const val NOT_INSTALLED_MESSAGE = "STV Player is required to play this content."

        private const val PLAY_STORE_MARKET_URL = "market://details?id=com.soufcom.stv"
        private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=com.soufcom.stv"
        private val SUPPORTED_PACKAGES = listOf(
            "com.soufcom.stv",
            "com.soufcom.stv.dev",
            "com.stv.videoplayer",
            "com.stv.videoplayer.dev",
            "com.example.stv",
            "com.example.stv.dev"
        )
    }
}

