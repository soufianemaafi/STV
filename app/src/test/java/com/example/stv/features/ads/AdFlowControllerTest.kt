package com.example.stv.features.ads

import android.app.Activity
import com.example.stv.AdManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdFlowControllerTest {

    private lateinit var adManager: AdManager
    private lateinit var activity: Activity

    @Before
    fun setup() {
        adManager = mockk(relaxed = true)
        activity = mockk(relaxed = true)
    }

    @Test
    fun `le cooldown court-circuite le chargement de pub`() = runTest {
        val frequencyManager = AdFrequencyManager(cooldownMs = 180_000L, clock = { 1_000L })
        frequencyManager.markAdShown(nowMs = 1_000L)

        val controller = AdFlowController(adManager, frequencyManager)
        val result = controller.showAdIfNeeded(activity)

        assertEquals(AdFlowController.AdResult.SkippedByCooldown, result)
        verify(exactly = 0) { adManager.loadAndShow(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `une pub affichee met a jour le cooldown`() = runTest {
        val now = 1_000L
        val frequencyManager = AdFrequencyManager(cooldownMs = 180_000L, clock = { now })
        val controller = AdFlowController(adManager, frequencyManager)

        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any()
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
        val frequencyManager = AdFrequencyManager(cooldownMs = 180_000L, clock = { now })
        frequencyManager.markAdShown(nowMs = 1_000L)
        val controller = AdFlowController(adManager, frequencyManager)

        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any()
            )
        } answers {
            val onAdShowed = arg<() -> Unit>(1)
            val onAdDismissed = arg<() -> Unit>(2)
            onAdShowed()
            onAdDismissed()
        }

        val result = controller.showAdIfNeeded(activity, bypassCooldown = true)

        assertEquals(AdFlowController.AdResult.AdDismissed, result)
        verify(exactly = 1) { adManager.loadAndShow(any(), any(), any(), any(), any()) }
    }
}

