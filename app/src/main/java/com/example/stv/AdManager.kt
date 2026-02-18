package com.example.stv

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {

    private var interstitialAd: InterstitialAd? = null
    private val TAG = "AdManager"

    // Test Ad Unit ID for Interstitial
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    interstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    this@AdManager.interstitialAd = interstitialAd
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed fullscreen content.")
                    interstitialAd = null
                    loadInterstitialAd() // Preload the next ad
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show fullscreen content.")
                    interstitialAd = null
                    onAdDismissed() // Proceed even if ad fails to show
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                    interstitialAd = null
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            loadInterstitialAd() // Try to load for next time
            onAdDismissed() // Proceed if ad is not ready
        }
    }
}


