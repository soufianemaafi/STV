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
 * - loadAndShow() : charge et affiche une pub
 * - Résultat : onAdDismissed (pub fermée), onFailed (échec), onNoNetwork (pas de réseau)
 * - PAS de compteur. PAS de détection adblock.
 * - Le player ne démarre que si onAdDismissed est appelé.
 * - Si la pub échoue, l'appelant (AdsController) réessaie.
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

        Log.d(TAG, "Loading interstitial ad...")
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
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded. Showing...")
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Ad showing.")
                            onAdShowed()
                        }
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Ad dismissed.")
                            onAdDismissed()
                        }
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Ad failed to show: $adError")
                            onFailed()
                        }
                    }
                    ad.show(activity)
                }
            }
        )
    }
}
