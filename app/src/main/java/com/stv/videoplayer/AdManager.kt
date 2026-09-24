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

/**
 * Singleton pour la gestion des publicitÃ©s AdMob.
 *
 * Logique ultra-simple :
 * - preloadInterstitial() : charge en arriÃ¨re-plan une pub prÃªte Ã  Ãªtre affichÃ©e
 * - loadAndShow() : consomme une pub prÃ©chargÃ©e ou charge/affiche Ã  la demande
 * - RÃ©sultat : onAdDismissed (pub fermÃ©e), onFailed (Ã©chec), onNoNetwork (pas de rÃ©seau)
 * - PAS de compteur. PAS de dÃ©tection adblock.
 * - Le player ne dÃ©marre que si onAdDismissed est appelÃ©.
 * - Si la pub Ã©choue, l'appelant (`AdFlowController`) gÃ¨re le retry ou le blocage.
 */
class AdManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AdManager? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = AdManager(context.applicationContext)
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
    private val AD_UNIT_ID = com.stv.videoplayer.BuildConfig.ADMOB_INTERSTITIAL_ID
    private val preloadLock = Any()

    @Volatile
    private var preloadedInterstitial: InterstitialAd? = null

    @Volatile
    private var isPreloading = false

    init {
        preloadInterstitial()
    }

    /**
     * Lance le prÃ©chargement d'une interstitielle si aucune pub n'est dÃ©jÃ  prÃªte.
     */
    fun preloadInterstitial() {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            Log.d(TAG, "Preload skipped: no network")
            return
        }

        synchronized(preloadLock) {
            if (preloadedInterstitial != null || isPreloading) {
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
                    preloadInterstitial()
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial preloaded")
                    synchronized(preloadLock) {
                        preloadedInterstitial = ad
                        isPreloading = false
                    }
                }
            }
        )
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
        onNoNetwork: () -> Unit
    ) {
        if (!NetworkUtils.isNetworkAvailable(appContext)) {
            Log.w(TAG, "No network.")
            onNoNetwork()
            return
        }

        val cachedAd = takePreloadedInterstitial()
        if (cachedAd != null) {
            Log.d(TAG, "Showing preloaded interstitial.")
            showInterstitial(
                activity = activity,
                ad = cachedAd,
                onAdShowed = onAdShowed,
                onAdDismissed = onAdDismissed,
                onFailed = onFailed
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
                    if (!NetworkUtils.isNetworkAvailable(appContext) || adError.code == AdRequest.ERROR_CODE_NETWORK_ERROR) {
                        onNoNetwork()
                    } else {
                        onFailed()
                    }
                    preloadInterstitial()
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded. Showing...")
                    showInterstitial(
                        activity = activity,
                        ad = ad,
                        onAdShowed = onAdShowed,
                        onAdDismissed = onAdDismissed,
                        onFailed = onFailed
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
        onFailed: () -> Unit
    ) {
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

