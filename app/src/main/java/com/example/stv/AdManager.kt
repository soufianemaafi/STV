package com.example.stv

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.stv.util.NetworkUtils
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Singleton pour la gestion des publicités AdMob.
 *
 * Logique ultra-simple :
 * - preloadInterstitial() : charge en arrière-plan une pub prête à être affichée
 * - loadAndShow() : consomme une pub préchargée ou charge/affiche à la demande
 * - Résultat : onAdDismissed (pub fermée), onFailed (échec), onNoNetwork (pas de réseau)
 * - PAS de compteur. PAS de détection adblock.
 * - Le player ne démarre que si onAdDismissed est appelé.
 * - Si la pub échoue, l'appelant (`AdFlowController`) gère le retry ou le blocage.
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
    private val AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_ID
    private val preloadLock = Any()

    @Volatile
    private var preloadedInterstitial: InterstitialAd? = null

    @Volatile
    private var isPreloading = false

    init {
        preloadInterstitial()
    }

    /**
     * Lance le préchargement d'une interstitielle si aucune pub n'est déjà prête.
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
     * 3 résultats possibles :
     * - onAdDismissed : pub vue et fermée → player peut démarrer
     * - onFailed : pub pas dispo (internet OK mais échec chargement/affichage)
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
