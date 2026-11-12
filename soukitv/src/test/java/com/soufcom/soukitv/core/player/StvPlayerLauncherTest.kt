package com.soufcom.soukitv.core.player

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import com.soufcom.soukitv.core.player.StvPlayerLauncher.LaunchResult
import com.soufcom.soukitv.features.home.domain.model.Channel
import com.soufcom.soukitv.features.home.domain.model.MatchItem
import com.soufcom.soukitv.features.home.domain.model.StreamSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests JVM purs du contrat IPC soukitv -> STV Player. */
class StvPlayerLauncherTest {

    private val prod = "com.soufcom.stv"
    private val dev = "com.soufcom.stv.dev"
    private val testAuthSecret = "test-auth-secret"

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
    fun launch_returnsNotInstalled_whenNoStvPackageIsPresent() = runTest {
        val result = launcher().launchChannel(channel, authSecret = testAuthSecret)

        assertEquals(LaunchResult.NotInstalled, result)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launch_returnsUpdateRequired_whenVersionCodeIsBelowMinimum() = runTest {
        installed(prod, versionCode = 1)

        val result = launcher(minVersionCode = 2).launchChannel(channel, authSecret = testAuthSecret)

        assertEquals(LaunchResult.UpdateRequired, result)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launch_succeeds_whenVersionCodeEqualsMinimum() = runTest {
        installed(prod, versionCode = 2)

        val result = launcher(minVersionCode = 2).launchChannel(channel, authSecret = testAuthSecret)

        assertEquals(LaunchResult.Launched, result)
        verify(exactly = 1) { context.startActivity(intent) }
    }

    @Test
    fun launch_targetsPlayStreamActionAndProductionPackage() = runTest {
        installed(prod, versionCode = 5)

        val result = launcher().launchChannel(channel, authSecret = testAuthSecret)

        assertEquals(LaunchResult.Launched, result)
        assertEquals(listOf("com.soufcom.stv.action.PLAY_STREAM"), createdActions)
        verify { intent.setPackage("com.soufcom.stv") }
        verify { intent.setDataAndType(uri, "video/*") }
        verify { intent.putExtra("title", "France 24") }
        verify { bundle.putString("X-STV-Auth", testAuthSecret) }
        verify { bundle.putString("User-Agent", "NexiPlayer-Official-Client/v3.0") }
        verify { intent.putExtra("headers", bundle) }
        verify { context.startActivity(intent) }
    }

    @Test
    fun launch_prefersProductionOverDevWhenBothInstalled() = runTest {
        installed(prod, versionCode = 3)
        installed(dev, versionCode = 3)

        launcher().launchChannel(channel, authSecret = testAuthSecret)

        verify { intent.setPackage(prod) }
        verify(exactly = 0) { intent.setPackage(dev) }
    }

    @Test
    fun launch_fallsBackToDevPackageWhenProductionIsMissing() = runTest {
        installed(dev, versionCode = 3)

        val result = launcher().launchChannel(channel, authSecret = testAuthSecret)

        assertEquals(LaunchResult.Launched, result)
        verify { intent.setPackage(dev) }
    }

    @Test
    fun launch_ignoresLegacyPackages() = runTest {
        installed("com.stv.videoplayer", versionCode = 9)
        installed("com.example.stv", versionCode = 9)

        assertEquals(
            LaunchResult.NotInstalled,
            launcher().launchChannel(channel, authSecret = testAuthSecret)
        )
    }

    @Test
    fun launch_putsHttpHeadersInBundleAndRefererExtra() = runTest {
        installed(prod, versionCode = 2)
        val headers = mapOf(
            "Referer" to "https://site.example/",
            "User-Agent" to "STV/1.0",
            "X-Token" to "abc123",
            " " to "ignored",
            "Empty" to ""
        )

        launcher().launchChannel(channel, headers, authSecret = testAuthSecret)

        verify { bundle.putString("Referer", "https://site.example/") }
        verify { bundle.putString("User-Agent", "STV/1.0") }
        verify { bundle.putString("X-Token", "abc123") }
        verify(exactly = 0) { bundle.putString("Empty", any()) }
        verify { intent.putExtra("headers", bundle) }
        verify { intent.putExtra("referer", "https://site.example/") }
    }

    @Test
    fun launch_rejectsBlankAuthSecret_beforeBuildingIntent() = runTest {
        installed(prod, versionCode = 2)

        val result = launcher().launchChannel(channel)

        assertEquals(
            LaunchResult.Failed(StvPlayerLauncher.SECURITY_CONFIGURATION_ERROR_MESSAGE),
            result
        )
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launch_injectsDynamicAuthSecret() = runTest {
        installed(prod, versionCode = 2)

        launcher().launchChannel(channel, authSecret = "rotating-secret")

        verify { bundle.putString("X-STV-Auth", "rotating-secret") }
    }

    @Test
    fun launch_resolvesDispatchUrl_beforeLaunchingWithDirectUrl() = runTest {
        installed(prod, versionCode = 2)
        val dispatchUrl = "https://balancer.example.com/dispatch"
        val resolvedUrl = "https://edge1.example.com/master.m3u8"
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
    fun launch_rewritesAllDispatchSourcesWithOneResolvedEdge_andKeepsExternalSourcesIntact() = runTest {
        installed(prod, versionCode = 2)
        val edgeUrl = "https://edge1.example.com/master.m3u8"
        val externalUrl = "https://external.example.com/stream.m3u8"
        val calls = mutableListOf<String>()
        val match = MatchItem(
            streamUrl = "https://balancer.example.com/dispatch",
            sources = listOf(
                StreamSource("HD", "https://balancer.example.com/dispatch?source=hd"),
                StreamSource("External", externalUrl),
                StreamSource("SD", "https://balancer.example.com/dispatch?source=sd")
            )
        )

        val result = launcher(
            streamDispatcher = StreamDispatcher { url, _ ->
                calls += url
                edgeUrl
            }
        ).launchMatch(match, authSecret = "rotating-secret")

        assertEquals(LaunchResult.Launched, result)
        assertEquals(listOf("https://balancer.example.com/dispatch"), calls)
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
    fun launch_resolvesOneDispatcherSource_whenPrimaryUrlIsExternal() = runTest {
        installed(prod, versionCode = 2)
        val primaryUrl = "https://external.example.com/stream.m3u8"
        val edgeUrl = "https://edge1.example.com/master.m3u8"
        var callCount = 0
        val match = MatchItem(
            streamUrl = primaryUrl,
            sources = listOf(
                StreamSource("Internal HD", "https://balancer.example.com/dispatch?source=hd"),
                StreamSource("Internal SD", "https://balancer.example.com/dispatch?source=sd")
            )
        )

        val result = launcher(
            streamDispatcher = StreamDispatcher { _, _ ->
                callCount++
                edgeUrl
            }
        ).launchMatch(match, authSecret = testAuthSecret)

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
    fun launch_fallsBackToFirstValidSecondarySource_whenDispatchFails() = runTest {
        installed(prod, versionCode = 2)
        val fallbackUrl = "https://relay.example/master.m3u8"
        val match = MatchItem(
            streamUrl = "https://balancer.example.com/dispatch",
            sources = listOf(
                StreamSource("bad", "file:///invalid"),
                StreamSource("HD", fallbackUrl)
            )
        )

        val result = launcher(
            streamDispatcher = StreamDispatcher { _, _ -> null }
        ).launchMatch(match, authSecret = testAuthSecret)

        assertEquals(LaunchResult.Launched, result)
        verify { intent.putExtra(StvPlayerLauncher.EXTRA_VIDEO_URL, fallbackUrl) }
        verify(exactly = 1) { context.startActivity(intent) }
    }

    @Test
    fun launch_returnsFailed_whenDispatchTimesOutAndNoFallbackExists() = runTest {
        installed(prod, versionCode = 2)

        val result = launcher(
            streamDispatcher = StreamDispatcher { _, _ -> throw java.net.SocketTimeoutException("timeout") }
        ).launchChannel(
            channel.copy(streamUrl = "https://balancer.example.com/dispatch"),
            authSecret = testAuthSecret
        )

        assertTrue(result is LaunchResult.Failed)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launch_rejectsNonHttpSchemes() = runTest {
        installed(prod, versionCode = 2)

        val httpResult = launcher().launchChannel(
            channel.copy(streamUrl = "http://example.com/live.m3u8"),
            authSecret = testAuthSecret
        )
        assertEquals(LaunchResult.Launched, httpResult)

        listOf("file:///sdcard/a.mp4", "content://x/y", "javascript:alert(1)", "intent://x").forEach { url ->
            val result = launcher().launchChannel(channel.copy(streamUrl = url))
            assertTrue("$url doit être rejeté", result is LaunchResult.Failed)
        }
        verify(exactly = 1) { context.startActivity(intent) }
    }

    @Test
    fun launchMatch_returnsFailed_whenPrimaryUrlIsNullAndNoValidSecondarySourceExists() = runTest {
        val match = MatchItem(streamUrl = null)

        val result = launcher().launchMatch(match, authSecret = testAuthSecret)

        assertEquals(
            LaunchResult.Failed("Aucun flux vidéo disponible pour cet événement."),
            result
        )
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launchMatch_returnsFailed_whenPrimaryUrlUsesUnsupportedScheme() = runTest {
        val match = MatchItem(streamUrl = "file:///sdcard/match.mp4")

        val result = launcher().launchMatch(match, authSecret = testAuthSecret)

        assertTrue(result is LaunchResult.Failed)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test
    fun launchMatch_usesFirstValidSecondarySource_whenPrimaryUrlIsNull() = runTest {
        installed(prod, versionCode = 2)
        val secondaryUrl = "https://example.com/secondary.m3u8"
        val match = MatchItem(
            streamUrl = null,
            sources = listOf(
                StreamSource("Invalid", "file:///sdcard/invalid.mp4"),
                StreamSource("HD", secondaryUrl)
            )
        )

        val result = launcher().launchMatch(match, authSecret = testAuthSecret)

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

