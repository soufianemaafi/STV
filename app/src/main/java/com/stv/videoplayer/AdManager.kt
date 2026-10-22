package com.stv.videoplayer

import android.app.Activity
import android.content.Context
import android.util.Log
import com.stv.videoplayer.util.NetworkUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Singleton pour la gestion des publicitÃ©s AdMob.
 *
 * Logique ultra-simple :
 * - preloadInterstitial() : charge en arriÃ¨re-plan une pub prÃªte Ã  Ãªtre affichÃ©e
 * - loadAndShow() : consomme une pub prÃ©chargÃ©e ou charge/affiche Ã  la demande
 * - RÃ©sultat : onAdDismissed (pub fermÃ©e), onFailed (Ã©chec), onNoNetwork (pas de rÃ©seau)
 * - Le cooldown et la dÃ©tection AdBlock sont orchestrÃ©s par AdFlowController.
 * - Le player ne dÃ©marre que si onAdDismissed est appelÃ©.
 * - Si la pub Ã©choue, l'appelant (`AdFlowController`) gÃ¨re le retry ou le blocage.
 */
class AdManager private constructor(
    context: Context,
    private val preloadScope: CoroutineScope,
    private val preloadRetryDelayMs: Long
) {

    companion object {
        private const val PRELOAD_RETRY_DELAY_MS = 5_000L
        private const val MAX_PRELOAD_RETRIES = 3

        @Volatile
        private var INSTANCE: AdManager? = null

        fun initialize(
            context: Context,
            preloadScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            preloadRetryDelayMs: Long = PRELOAD_RETRY_DELAY_MS
        ) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = AdManager(
                            context = context.applicationContext,
                            preloadScope = preloadScope,
                            preloadRetryDelayMs = preloadRetryDelayMs
                        )
                    }
                }
            }
        }

        val instance: AdManager
            get() = INSTANCE ?: throw IllegalStateException(
                "AdManager not initialized. Call AdManager.initialize(context) in STVApplication first."
            )
    }

    private val appContext = context.applicationContext
    private val TAG = "AdManager"
    private val AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_ID
    private val preloadLock = Any()

    @Volatile
    private var preloadedInterstitial: InterstitialAd? = null

    @Volatile
    private var isPreloading = false

    @Volatile
    private var isPreloadRetryScheduled = false

    private var preloadRetryCount = 0

    init {
        preloadInterstitial()
    }

    /**
     * Lance le prÃ©chargement d'une interstitielle si aucune pub n'est dÃ©jÃ  prÃªte.
     */
    fun preloadInterstitial() {
        preloadInterstitial(resetRetryBudget = true)
    }

    private fun preloadInterstitial(resetRetryBudget: Boolean) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            Log.d(TAG, "Preload skipped: no network")
            return
        }

        synchronized(preloadLock) {
            if (resetRetryBudget) {
                preloadRetryCount = 0
            }
            if (preloadedInterstitial != null || isPreloading || isPreloadRetryScheduled) {
                return
            }
            isPreloading = true
        }

        Log.d(TAG, "Preloading interstitial ad...")
        InterstitialAd.load(
            appContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Preload failed: $adError")
                    synchronized(preloadLock) {
                        isPreloading = false
                    }
                    schedulePreloadRetry()
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial preloaded")
                    synchronized(preloadLock) {
                        preloadedInterstitial = ad
                        isPreloading = false
                        preloadRetryCount = 0
                    }
                }
            }
        )
    }

    /**
     * Reprogramme un seul préchargement après un échec.
     * Aucun appel récursif immédiat : un no-fill ou une panne réseau persistante
     * ne peut donc plus provoquer une boucle de requêtes sans fin.
     */
    private fun schedulePreloadRetry() {
        synchronized(preloadLock) {
            if (isPreloadRetryScheduled || preloadedInterstitial != null) return
            if (preloadRetryCount >= MAX_PRELOAD_RETRIES) {
                Log.w(TAG, "Preload retry limit reached; waiting for the next explicit preload")
                return
            }
            preloadRetryCount++
            isPreloadRetryScheduled = true
        }

        preloadScope.launch {
            delay(preloadRetryDelayMs)
            synchronized(preloadLock) {
                isPreloadRetryScheduled = false
            }
            preloadInterstitial(resetRetryBudget = false)
        }
    }

    private fun takePreloadedInterstitial(): InterstitialAd? {
        synchronized(preloadLock) {
            return preloadedInterstitial.also {
                preloadedInterstitial = null
            }
        }
    }


    /**
     * Charge et affiche une pub interstitielle.
     *
     * 3 rÃ©sultats possibles :
     * - onAdDismissed : pub vue et fermÃ©e â†’ player peut dÃ©marrer
     * - onFailed : pub pas dispo (internet OK mais Ã©chec chargement/affichage)
     * - onNoNetwork : pas de connexion internet
     */
    fun loadAndShow(
        activity: Activity,
        onAdShowed: () -> Unit,
        onAdDismissed: () -> Unit,
        onFailed: () -> Unit,
        onNoNetwork: () -> Unit,
        isRequestActive: () -> Boolean = { true },
        onNetworkError: () -> Unit = {}
    ) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            Log.w(TAG, "No network.")
            onNoNetwork()
            return
        }

        val cachedAd = takePreloadedInterstitial()
        if (cachedAd != null) {
            if (!isRequestActive()) {
                preloadInterstitial()
                return
            }
            Log.d(TAG, "Showing preloaded interstitial.")
            showInterstitial(
                activity = activity,
                ad = cachedAd,
                onAdShowed = onAdShowed,
                onAdDismissed = onAdDismissed,
                onFailed = onFailed,
                isRequestActive = isRequestActive
            )
            return
        }

        Log.d(TAG, "Loading interstitial ad on-demand...")
        InterstitialAd.load(
            appContext,
            AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Ad failed to load: $adError")
                    if (adError.code == AdRequest.ERROR_CODE_NETWORK_ERROR) {
                        onNetworkError()
                    } else if (!NetworkUtils.isNetworkAvailable(appContext)) {
                        onNoNetwork()
                    } else {
                        onFailed()
                    }
                    preloadInterstitial()
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded. Showing...")
                    if (!isRequestActive()) {
                        preloadInterstitial()
                        return
                    }
                    showInterstitial(
                        activity = activity,
                        ad = ad,
                        onAdShowed = onAdShowed,
                        onAdDismissed = onAdDismissed,
                        onFailed = onFailed,
                        isRequestActive = isRequestActive
                    )
                }
            }
        )
    }

    private fun showInterstitial(
        activity: Activity,
        ad: InterstitialAd,
        onAdShowed: () -> Unit,
        onAdDismissed: () -> Unit,
        onFailed: () -> Unit,
        isRequestActive: () -> Boolean
    ) {
        if (!isRequestActive()) {
            preloadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showing.")
                onAdShowed()
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed.")
                onAdDismissed()
                preloadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show: $adError")
                onFailed()
                preloadInterstitial()
            }
        }

        ad.show(activity)
    }

}

