package com.example.soukitv.core.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.soukitv.features.home.domain.model.Channel

class StvPlayerLauncher(private val context: Context) {

    fun launchChannel(channel: Channel): LaunchResult {
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
                val uri = Uri.parse(channel.streamUrl)
                setDataAndType(uri, "video/*")
                putExtra(EXTRA_VIDEO_URL, channel.streamUrl)
                putExtra(EXTRA_URL, channel.streamUrl)
                putExtra(EXTRA_TITLE, channel.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult.Launched
        } catch (e: Exception) {
            LaunchResult.Failed(e.message ?: "Error launching STV Player")
        }
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
        data class Failed(val message: String) : LaunchResult
    }

    companion object {
        const val PLAY_STREAM_ACTION = "com.stv.videoplayer.action.PLAY_STREAM"
        const val EXTRA_VIDEO_URL = "VIDEO_URL"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val NOT_INSTALLED_MESSAGE = "STV Player is required to play this content."

        private const val PLAY_STORE_MARKET_URL = "market://details?id=com.stv.videoplayer"
        private const val PLAY_STORE_WEB_URL = "https://play.google.com/store/apps/details?id=com.stv.videoplayer"
        private val SUPPORTED_PACKAGES = listOf(
            "com.stv.videoplayer",
            "com.stv.videoplayer.dev",
            "com.example.stv",
            "com.example.stv.dev"
        )
    }
}

