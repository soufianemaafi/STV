package com.example.soukitv.core.player

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import com.example.soukitv.core.player.StvPlayerLauncher.LaunchResult
import com.example.soukitv.features.home.domain.model.Channel
import com.example.soukitv.features.home.domain.model.MatchItem
import com.example.soukitv.features.home.domain.model.StreamSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests JVM purs du contrat IPC soukitv -> STV Player. */
class StvPlayerLauncherTest {

    private val prod = "com.soufcom.stv"
    private val dev = "com.soufcom.stv.dev"

    private val context: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk()
    private val intent: Intent = mockk(relaxed = true)
    private val bundle: Bundle = mockk(relaxed = true)
    private val uri: Uri = mockk()
    private val createdActions = mutableListOf<String>()

    private val channel = Channel(
        id = "1",
        name = "France 24",
        logoUrl = "",
        streamUrl = "https://example.com/live.m3u8",
        category = "News",
        country = "France"
    )

    @Before
    fun setUp() {
        every { context.packageManager } returns packageManager
        // Par défaut : aucun package STV installé.
        every { packageManager.getPackageInfo(any<String>(), any<Int>()) } throws
            PackageManager.NameNotFoundException()
    }

    @Test
    fun launch_returnsNotInstalled_whenNoStvPackageIsPresent() {
        val result = launcher().launchChannel(channel)

        assertEquals(LaunchResult.NotInstalled, result)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launch_returnsUpdateRequired_whenVersionCodeIsBelowMinimum() {
        installed(prod, versionCode = 1)

        val result = launcher(minVersionCode = 2).launchChannel(channel)

        assertEquals(LaunchResult.UpdateRequired, result)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launch_succeeds_whenVersionCodeEqualsMinimum() {
        installed(prod, versionCode = 2)

        val result = launcher(minVersionCode = 2).launchChannel(channel)

        assertEquals(LaunchResult.Launched, result)
        verify(exactly = 1) { context.startActivity(intent) }
    }

    @Test
    fun launch_targetsPlayStreamActionAndProductionPackage() {
        installed(prod, versionCode = 5)

        val result = launcher().launchChannel(channel)

        assertEquals(LaunchResult.Launched, result)
        assertEquals(listOf("com.soufcom.stv.action.PLAY_STREAM"), createdActions)
        verify { intent.setPackage("com.soufcom.stv") }
        verify { intent.setDataAndType(uri, "video/*") }
        verify { intent.putExtra("title", "France 24") }
        verify { bundle.putString("X-STV-Auth", "") }
        verify { bundle.putString("User-Agent", "NexiPlayer-Official-Client/v3.0") }
        verify { intent.putExtra("headers", bundle) }
        verify { context.startActivity(intent) }
    }

    @Test
    fun launch_prefersProductionOverDevWhenBothInstalled() {
        installed(prod, versionCode = 3)
        installed(dev, versionCode = 3)

        launcher().launchChannel(channel)

        verify { intent.setPackage(prod) }
        verify(exactly = 0) { intent.setPackage(dev) }
    }

    @Test
    fun launch_fallsBackToDevPackageWhenProductionIsMissing() {
        installed(dev, versionCode = 3)

        val result = launcher().launchChannel(channel)

        assertEquals(LaunchResult.Launched, result)
        verify { intent.setPackage(dev) }
    }

    @Test
    fun launch_ignoresLegacyPackages() {
        installed("com.stv.videoplayer", versionCode = 9)
        installed("com.example.stv", versionCode = 9)

        assertEquals(LaunchResult.NotInstalled, launcher().launchChannel(channel))
    }

    @Test
    fun launch_putsHttpHeadersInBundleAndRefererExtra() {
        installed(prod, versionCode = 2)
        val headers = mapOf(
            "Referer" to "https://site.example/",
            "User-Agent" to "STV/1.0",
            "X-Token" to "abc123",
            " " to "ignored",
            "Empty" to ""
        )

        launcher().launchChannel(channel, headers)

        verify { bundle.putString("Referer", "https://site.example/") }
        verify { bundle.putString("User-Agent", "STV/1.0") }
        verify { bundle.putString("X-Token", "abc123") }
        verify(exactly = 0) { bundle.putString("Empty", any()) }
        verify { intent.putExtra("headers", bundle) }
        verify { intent.putExtra("referer", "https://site.example/") }
    }

    @Test
    fun launch_withoutCustomHeaders_stillAddsLockInHeaders() {
        installed(prod, versionCode = 2)

        launcher().launchChannel(channel)

        verify { bundle.putString("X-STV-Auth", "") }
        verify { bundle.putString("User-Agent", "NexiPlayer-Official-Client/v3.0") }
        verify { intent.putExtra("headers", bundle) }
    }

    @Test
    fun launch_injectsDynamicAuthSecret() {
        installed(prod, versionCode = 2)

        launcher().launchChannel(channel, authSecret = "rotating-secret")

        verify { bundle.putString("X-STV-Auth", "rotating-secret") }
    }

    @Test
    fun launch_resolvesDispatchUrl_beforeLaunchingWithDirectUrl() {
        installed(prod, versionCode = 2)
        val dispatchUrl = "http://196.200.156.190/dispatch"
        val resolvedUrl = "http://196.200.156.184/master.m3u8"
        var receivedAuth = ""
        var receivedUrl = ""

        val result = launcher(
            streamDispatcher = StreamDispatcher { url, authSecret ->
                receivedUrl = url
                receivedAuth = authSecret
                resolvedUrl
            }
        ).launchChannel(channel.copy(streamUrl = dispatchUrl), authSecret = "dynamic-secret")

        assertEquals(LaunchResult.Launched, result)
        assertEquals(dispatchUrl, receivedUrl)
        assertEquals("dynamic-secret", receivedAuth)
        verify { intent.putExtra(StvPlayerLauncher.EXTRA_VIDEO_URL, resolvedUrl) }
        verify { intent.putExtra(StvPlayerLauncher.EXTRA_URL, resolvedUrl) }
    }

    @Test
    fun launch_rewritesAllDispatchSourcesWithOneResolvedEdge_andKeepsExternalSourcesIntact() {
        installed(prod, versionCode = 2)
        val edgeUrl = "http://196.200.156.184/master.m3u8"
        val externalUrl = "https://live-tv-example.run.app/live/index.m3u8?token=a%2Fb"
        val calls = mutableListOf<String>()
        val match = MatchItem(
            streamUrl = "http://196.200.156.190/dispatch",
            sources = listOf(
                StreamSource("HD", "http://196.200.156.190/dispatch?source=hd"),
                StreamSource("External", externalUrl),
                StreamSource("SD", "https://196.200.156.190/dispatch?source=sd")
            )
        )

        val result = launcher(
            streamDispatcher = StreamDispatcher { url, _ ->
                calls += url
                edgeUrl
            }
        ).launchMatch(match, authSecret = "rotating-secret")

        assertEquals(LaunchResult.Launched, result)
        assertEquals(listOf("http://196.200.156.190/dispatch"), calls)
        verify {
            intent.putExtra(
                StvPlayerLauncher.EXTRA_SOURCES_JSON,
                "[{\"quality\":\"HD\",\"url\":\"$edgeUrl\"}," +
                    "{\"quality\":\"External\",\"url\":\"$externalUrl\"}," +
                    "{\"quality\":\"SD\",\"url\":\"$edgeUrl\"}]"
            )
        }
    }

    @Test
    fun launch_resolvesOneDispatcherSource_whenPrimaryUrlIsExternal() {
        installed(prod, versionCode = 2)
        val primaryUrl = "https://external.example/live.m3u8"
        val edgeUrl = "http://196.200.156.184/master.m3u8"
        var callCount = 0
        val match = MatchItem(
            streamUrl = primaryUrl,
            sources = listOf(
                StreamSource("Internal HD", "http://196.200.156.190/dispatch?source=hd"),
                StreamSource("Internal SD", "http://196.200.156.190/dispatch?source=sd")
            )
        )

        val result = launcher(
            streamDispatcher = StreamDispatcher { _, _ ->
                callCount++
                edgeUrl
            }
        ).launchMatch(match)

        assertEquals(LaunchResult.Launched, result)
        assertEquals(1, callCount)
        verify { intent.putExtra(StvPlayerLauncher.EXTRA_VIDEO_URL, primaryUrl) }
        verify {
            intent.putExtra(
                StvPlayerLauncher.EXTRA_SOURCES_JSON,
                "[{\"quality\":\"Internal HD\",\"url\":\"$edgeUrl\"}," +
                    "{\"quality\":\"Internal SD\",\"url\":\"$edgeUrl\"}]"
            )
        }
    }

    @Test
    fun launch_fallsBackToFirstValidSecondarySource_whenDispatchFails() {
        installed(prod, versionCode = 2)
        val fallbackUrl = "https://relay.example/master.m3u8"
        val match = MatchItem(
            streamUrl = "http://196.200.156.190/dispatch",
            sources = listOf(
                StreamSource("bad", "file:///invalid"),
                StreamSource("HD", fallbackUrl)
            )
        )

        val result = launcher(
            streamDispatcher = StreamDispatcher { _, _ -> null }
        ).launchMatch(match)

        assertEquals(LaunchResult.Launched, result)
        verify { intent.putExtra(StvPlayerLauncher.EXTRA_VIDEO_URL, fallbackUrl) }
        verify(exactly = 1) { context.startActivity(intent) }
    }

    @Test
    fun launch_returnsFailed_whenDispatchTimesOutAndNoFallbackExists() {
        installed(prod, versionCode = 2)

        val result = launcher(
            streamDispatcher = StreamDispatcher { _, _ -> throw java.net.SocketTimeoutException("timeout") }
        ).launchChannel(channel.copy(streamUrl = "https://196.200.156.190/dispatch"))

        assertTrue(result is LaunchResult.Failed)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launch_rejectsNonHttpSchemes() {
        installed(prod, versionCode = 2)

        listOf("file:///sdcard/a.mp4", "content://x/y", "javascript:alert(1)", "intent://x").forEach { url ->
            val result = launcher().launchChannel(channel.copy(streamUrl = url))
            assertTrue("$url doit être rejeté", result is LaunchResult.Failed)
        }
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launchMatch_returnsFailed_whenPrimaryUrlIsNullAndNoValidSecondarySourceExists() {
        val match = MatchItem(streamUrl = null)

        val result = launcher().launchMatch(match)

        assertEquals(
            LaunchResult.Failed("Aucun flux vidéo disponible pour cet événement."),
            result
        )
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launchMatch_returnsFailed_whenPrimaryUrlUsesUnsupportedScheme() {
        val match = MatchItem(streamUrl = "file:///sdcard/match.mp4")

        val result = launcher().launchMatch(match)

        assertTrue(result is LaunchResult.Failed)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launchMatch_usesFirstValidSecondarySource_whenPrimaryUrlIsNull() {
        installed(prod, versionCode = 2)
        val secondaryUrl = "https://example.com/secondary.m3u8"
        val match = MatchItem(
            streamUrl = null,
            sources = listOf(
                StreamSource("Invalid", "file:///sdcard/invalid.mp4"),
                StreamSource("HD", secondaryUrl)
            )
        )

        val result = launcher().launchMatch(match)

        assertEquals(LaunchResult.Launched, result)
        verify { intent.putExtra(StvPlayerLauncher.EXTRA_VIDEO_URL, secondaryUrl) }
        verify(exactly = 1) { context.startActivity(intent) }
    }

    @Suppress("DEPRECATION")
    private fun installed(packageName: String, versionCode: Int) {
        val info = PackageInfo().apply { this.versionCode = versionCode }
        every { packageManager.getPackageInfo(packageName, 0) } returns info
    }

    private fun launcher(
        minVersionCode: Long = StvPlayerLauncher.MIN_STV_VERSION_CODE,
        streamDispatcher: StreamDispatcher = StreamDispatcher { _, _ -> null }
    ) =
        StvPlayerLauncher(
            context = context,
            minVersionCode = minVersionCode,
            intentFactory = { action ->
                createdActions += action
                intent
            },
            bundleFactory = { bundle },
            uriParser = { uri },
            streamDispatcher = streamDispatcher
        )
}

