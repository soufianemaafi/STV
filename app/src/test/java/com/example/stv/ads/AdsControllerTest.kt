package com.example.stv.ads

import android.app.Activity
import com.example.stv.AdManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires pour AdsController.
 * Vérifie la logique de retry, timeout, et les différents résultats.
 *
 * AdManager est mocké — on simule ses callbacks pour tester
 * la logique d'orchestration sans le SDK AdMob réel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AdsControllerTest {

    private lateinit var adManager: AdManager
    private lateinit var activity: Activity
    private lateinit var controller: AdsController

    @Before
    fun setup() {
        adManager = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        controller = AdsController(adManager)
    }

    /**
     * Helper : configure AdManager.loadAndShow pour appeler immédiatement un callback donné.
     */
    private fun mockAdManagerCallback(callback: String) {
        val onAdShowed = slot<() -> Unit>()
        val onAdDismissed = slot<() -> Unit>()
        val onFailed = slot<() -> Unit>()
        val onNoNetwork = slot<() -> Unit>()

        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = capture(onAdShowed),
                onAdDismissed = capture(onAdDismissed),
                onFailed = capture(onFailed),
                onNoNetwork = capture(onNoNetwork)
            )
        } answers {
            when (callback) {
                "dismissed" -> {
                    onAdShowed.captured()
                    onAdDismissed.captured()
                }
                "failed" -> onFailed.captured()
                "noNetwork" -> onNoNetwork.captured()
                "showed" -> onAdShowed.captured() // Affichée mais pas fermée
            }
        }
    }

    // ==================== Tests du résultat final ====================

    @Test
    fun `pub affichee et fermee retourne AdDismissed`() = runTest {
        mockAdManagerCallback("dismissed")

        val result = controller.showAdIfNeeded(activity)

        assertEquals(AdsController.AdResult.AdDismissed, result)
    }

    @Test
    fun `pas de reseau retourne NoNetwork immediatement`() = runTest {
        mockAdManagerCallback("noNetwork")

        val result = controller.showAdIfNeeded(activity)

        assertEquals(AdsController.AdResult.NoNetwork, result)
    }

    @Test
    fun `3 echecs consecutifs retourne NeedRetry`() = runTest {
        mockAdManagerCallback("failed")

        val result = controller.showAdIfNeeded(activity)

        assertEquals(AdsController.AdResult.NeedRetry, result)
    }

    @Test
    fun `NoNetwork ne declenche pas de retry`() = runTest {
        // NoNetwork doit retourner immédiatement, pas réessayer 3 fois
        var callCount = 0
        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any()
            )
        } answers {
            callCount++
            val onNoNetwork = arg<() -> Unit>(4)
            onNoNetwork()
        }

        controller.showAdIfNeeded(activity)

        // Doit être appelé 1 seule fois (pas 3)
        assertEquals(1, callCount)
    }

    @Test
    fun `AdDismissed au premier essai ne retry pas`() = runTest {
        var callCount = 0
        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any()
            )
        } answers {
            callCount++
            val onAdShowed = arg<() -> Unit>(1)
            val onAdDismissed = arg<() -> Unit>(2)
            onAdShowed()
            onAdDismissed()
        }

        controller.showAdIfNeeded(activity)

        // Doit être appelé 1 seule fois (pub réussie dès le premier essai)
        assertEquals(1, callCount)
    }

    @Test
    fun `echec puis succes retourne AdDismissed`() = runTest {
        var callCount = 0
        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any()
            )
        } answers {
            callCount++
            if (callCount <= 2) {
                // 2 premiers appels échouent
                val onFailed = arg<() -> Unit>(3)
                onFailed()
            } else {
                // 3ème appel réussit
                val onAdShowed = arg<() -> Unit>(1)
                val onAdDismissed = arg<() -> Unit>(2)
                onAdShowed()
                onAdDismissed()
            }
        }

        val result = controller.showAdIfNeeded(activity)

        assertEquals(AdsController.AdResult.AdDismissed, result)
        assertEquals(3, callCount)
    }

    @Test
    fun `onAdShowed est appele quand la pub est affichee`() = runTest {
        mockAdManagerCallback("dismissed")

        var adShowedCalled = false
        controller.showAdIfNeeded(activity) {
            adShowedCalled = true
        }

        assertTrue(adShowedCalled)
    }

    @Test
    fun `3 echecs appellent loadAndShow exactement 3 fois`() = runTest {
        var callCount = 0
        every {
            adManager.loadAndShow(
                activity = any(),
                onAdShowed = any(),
                onAdDismissed = any(),
                onFailed = any(),
                onNoNetwork = any()
            )
        } answers {
            callCount++
            val onFailed = arg<() -> Unit>(3)
            onFailed()
        }

        controller.showAdIfNeeded(activity)

        assertEquals(3, callCount)
    }
}

