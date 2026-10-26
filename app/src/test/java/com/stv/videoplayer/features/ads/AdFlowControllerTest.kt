package com.stv.videoplayer.features.ads

import android.app.Activity
import com.stv.videoplayer.AdManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdFlowControllerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var adManager: AdManager
    private lateinit var activity: Activity

    @Before
    fun setup() {
        adManager = mockk(relaxed = true)
        activity = mockk(relaxed = true)
    }

    @Test
    fun `le cooldown nul autorise une nouvelle demande de pub`() = runTest {
        val frequencyManager = AdFrequencyManager(clock = { 1_000L })
        frequencyManager.markAdShown(nowMs = 1_000L)

        val controller = AdFlowController(adManager, frequencyManager)

        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any(),
                onNetworkError = any(),
                isRequestActive = any()
            )
        } answers {
            arg<() -> Unit>(1)()
            arg<() -> Unit>(2)()
        }

        val result = controller.showAdIfNeeded(activity)

        assertEquals(AdFlowController.AdResult.AdDismissed, result)
        verify(exactly = 1) { adManager.loadAndShow(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `une pub affichee met a jour le cooldown`() = runTest {
        val now = 1_000L
        val frequencyManager = AdFrequencyManager(cooldownMs = 30_000L, clock = { now })
        val controller = AdFlowController(adManager, frequencyManager)

        verify(exactly = 1) { adManager.preloadInterstitial() }

        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any(),
                onNetworkError = any(),
                isRequestActive = any()
            )
        } answers {
            val onAdShowed = arg<() -> Unit>(1)
            val onAdDismissed = arg<() -> Unit>(2)
            onAdShowed()
            onAdDismissed()
        }

        val result = controller.showAdIfNeeded(activity)

        assertEquals(AdFlowController.AdResult.AdDismissed, result)
        assertTrue(frequencyManager.isCooldownActive())
    }

    @Test
    fun `le retry peut contourner le cooldown pour rechecker la pub`() = runTest {
        val now = 1_000L
        val frequencyManager = AdFrequencyManager(cooldownMs = 30_000L, clock = { now })
        frequencyManager.markAdShown(nowMs = 1_000L)
        val controller = AdFlowController(adManager, frequencyManager)

        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any(),
                onNetworkError = any(),
                isRequestActive = any()
            )
        } answers {
            val onAdShowed = arg<() -> Unit>(1)
            val onAdDismissed = arg<() -> Unit>(2)
            onAdShowed()
            onAdDismissed()
        }

        val result = controller.showAdIfNeeded(activity, bypassCooldown = true)

        assertEquals(AdFlowController.AdResult.AdDismissed, result)
        verify(exactly = 1) { adManager.loadAndShow(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `un timeout de chargement lance directement la video sans suspecter un adblocker`() = runTest {
        val frequencyManager = AdFrequencyManager(clock = { 1_000L })
        val controller = AdFlowController(adManager, frequencyManager)

        val result = async { controller.showAdIfNeeded(activity) }

        advanceTimeBy(9_999L)
        assertFalse(result.isCompleted)
        advanceTimeBy(1L)

        assertEquals(AdFlowController.AdResult.SkippedByTimeout, result.await())
        assertFalse(frequencyManager.isAdBlockerSuspected())
        verify(exactly = 1) { adManager.loadAndShow(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `une erreur reseau AdMob declenche le blocage AdBlock`() = runTest {
        val frequencyManager = AdFrequencyManager(clock = { 1_000L })
        val controller = AdFlowController(adManager, frequencyManager)

        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any(),
                onNetworkError = any(),
                isRequestActive = any()
            )
        } answers {
            arg<() -> Unit>(6)()
        }

        val result = controller.showAdIfNeeded(activity)

        assertEquals(AdFlowController.AdResult.AdBlockerBlocked, result)
        assertTrue(frequencyManager.isAdBlockerSuspected())
    }

    @Test
    fun `une pub affichee puis fermee est retournee comme succes`() = runTest {
        val frequencyManager = AdFrequencyManager(clock = { 1_000L })
        val controller = AdFlowController(adManager, frequencyManager)
        var showed = false

        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any(),
                onNetworkError = any(),
                isRequestActive = any()
            )
        } answers {
            arg<() -> Unit>(1)()
            arg<() -> Unit>(2)()
        }

        val result = controller.showAdIfNeeded(activity, onAdShowed = { showed = true })

        assertEquals(AdFlowController.AdResult.AdDismissed, result)
        assertTrue(showed)
        assertFalse(frequencyManager.isAdBlockerSuspected())
    }
}


