package com.stv.videoplayer.features.ads

import android.app.Activity
import android.app.Application
import com.stv.videoplayer.AdManager
import com.stv.videoplayer.util.NetworkUtils
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdManagerPreloadTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var application: Application
    private lateinit var activity: Activity
    private lateinit var preloadedAd: InterstitialAd
    private val loadCallbacks = mutableListOf<InterstitialAdLoadCallback>()
    private val fullscreenCallbackSlot = slot<FullScreenContentCallback>()

    @Before
    fun setup() {
        resetSingleton()
        application = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        every { application.applicationContext } returns application

        mockkObject(NetworkUtils)
        every { NetworkUtils.isNetworkAvailable(any()) } returns true

        mockkStatic(InterstitialAd::class)
        preloadedAd = mockk(relaxed = true)
        every { preloadedAd.fullScreenContentCallback = capture(fullscreenCallbackSlot) } returns Unit
        every { preloadedAd.show(any()) } returns Unit

        every {
            InterstitialAd.load(
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            val callback = arg<InterstitialAdLoadCallback>(3)
            loadCallbacks += callback

            when (loadCallbacks.size) {
                1 -> callback.onAdLoaded(preloadedAd)
                2 -> callback.onAdLoaded(mockk(relaxed = true))
                else -> Unit
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        resetSingleton()
    }

    @Test
    fun `dismissal precharge automatiquement une nouvelle pub`() = runTest {
        AdManager.initialize(application, preloadScope = this)
        val manager = AdManager.instance

        manager.loadAndShow(
            activity = activity,
            onAdShowed = {},
            onAdDismissed = {},
            onFailed = {},
            onNoNetwork = {}
        )

        assertEquals(1, loadCallbacks.size)
        fullscreenCallbackSlot.captured.onAdDismissedFullScreenContent()
        assertEquals(2, loadCallbacks.size)
    }

    @Test
    fun `un echec de preload est relance apres le backoff`() = runTest {
        resetSingleton()

        every {
            InterstitialAd.load(
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            val callback = arg<InterstitialAdLoadCallback>(3)
            loadCallbacks += callback
            when (loadCallbacks.size) {
                1 -> callback.onAdFailedToLoad(mockk(relaxed = true))
                2 -> callback.onAdLoaded(preloadedAd)
                else -> Unit
            }
        }

        AdManager.initialize(application, preloadScope = this)

        assertEquals(1, loadCallbacks.size)
        advanceTimeBy(4_999L)
        assertEquals(1, loadCallbacks.size)
        advanceTimeBy(1L)
        runCurrent()
        assertEquals(2, loadCallbacks.size)
    }

    @Test
    fun `une erreur reseau de chargement appelle le callback reseau`() = runTest {
        val networkError = mockk<LoadAdError>(relaxed = true)
        every { networkError.code } returns AdRequest.ERROR_CODE_NETWORK_ERROR
        every {
            InterstitialAd.load(
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            val callback = arg<InterstitialAdLoadCallback>(3)
            loadCallbacks += callback
            callback.onAdFailedToLoad(networkError)
        }

        AdManager.initialize(application, preloadScope = this)
        var networkCallbackCalled = false
        var noNetworkCallbackCalled = false

        AdManager.instance.loadAndShow(
            activity = activity,
            onAdShowed = {},
            onAdDismissed = {},
            onFailed = {},
            onNoNetwork = { noNetworkCallbackCalled = true },
            onNetworkError = { networkCallbackCalled = true }
        )

        assertTrue(networkCallbackCalled)
        assertTrue(!noNetworkCallbackCalled)
    }

    private fun resetSingleton() {
        val field = AdManager::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
    }
}


