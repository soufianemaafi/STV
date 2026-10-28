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
    fun launch_withoutHeaders_doesNotAddHeadersExtra() {
        installed(prod, versionCode = 2)

        launcher().launchChannel(channel)

        verify(exactly = 0) { intent.putExtra("headers", any<Bundle>()) }
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

    private fun installed(packageName: String, versionCode: Int) {
        val info = PackageInfo().apply { this.versionCode = versionCode }
        every { packageManager.getPackageInfo(packageName, 0) } returns info
    }

    private fun launcher(minVersionCode: Long = StvPlayerLauncher.MIN_STV_VERSION_CODE) =
        StvPlayerLauncher(
            context = context,
            minVersionCode = minVersionCode,
            intentFactory = { action ->
                createdActions += action
                intent
            },
            bundleFactory = { bundle },
            uriParser = { uri }
        )
}

