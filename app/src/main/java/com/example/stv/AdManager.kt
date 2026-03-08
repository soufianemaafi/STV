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

class AdManager(context: Context) {

    private var interstitialAd: InterstitialAd? = null
    // Utiliser applicationContext pour éviter les fuites de mémoire
    private val appContext = context.applicationContext

    // Persistance : On récupère les préférences avec le contexte de l'application
    private val prefs = appContext.getSharedPreferences("ad_prefs", Context.MODE_PRIVATE)
    // On initialise le compteur avec la valeur sauvegardée (0 par défaut)
    private var failedLoadAttempts: Int
        get() = prefs.getInt("failed_attempts", 0)
        set(value) {
            val editor = prefs.edit()
            editor.putInt("failed_attempts", value)
            editor.apply()
        }

    private val MAX_FAILED_ATTEMPTS = 3 // Seuil de tolérance
    private val TAG = "AdManager"

    // ✅ ID AdMob interstitiel provenant du BuildConfig (flavor-specific)
    private val AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_ID

    fun loadAndShowInterstitial(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onFallbackAd: () -> Unit,
        onAdShowed: () -> Unit, // Nouveau callback
        onAdBlockDetected: (() -> Unit)? = null // Nouveau callback optionnel pour gérer le blocage dans PlayerActivity
    ) {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            appContext,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    interstitialAd = null
                    failedLoadAttempts++

                    // NOUVELLE LOGIQUE "SOFT FAILOVER"
                    // Si c'est une erreur "No Fill" (3), ce n'est pas un blocage, c'est juste pas de pub dispo.
                    if (adError.code == AdRequest.ERROR_CODE_NO_FILL) {
                         Log.d(TAG, "No fill received, falling back to banner or content directly.")
                         // On ne compte pas ça comme un échec bloquant
                         failedLoadAttempts = 0
                         // On passe directement au fallback (bannière)
                         onFallbackAd()
                         return
                    }

                    // Si le chargement échoue, on passe inmédiatement au fallback ou à la suite
                    if (failedLoadAttempts >= MAX_FAILED_ATTEMPTS) {
                         // Trop d'échecs : détection AdBlocker
                         if (adError.code == AdRequest.ERROR_CODE_NETWORK_ERROR) {
                             // Erreur réseau persistante : on tente le fallback bannière
                             onFallbackAd()
                         } else {
                             // ✅ Notifier l'Activity via callback (qui affichera un dialogue Compose)
                             onAdBlockDetected?.invoke()
                         }
                    } else {
                         onFallbackAd()
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    this@AdManager.interstitialAd = interstitialAd
                    failedLoadAttempts = 0
                    // La pub est chargée, on l'affiche directement
                    showInterstitial(activity, onAdDismissed, onFallbackAd, onAdShowed)
                }
            }
        )
    }

    fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        // Utiliser appContext pour le chargement
        InterstitialAd.load(
            appContext,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.toString())
                    interstitialAd = null
                    failedLoadAttempts++
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    this@AdManager.interstitialAd = interstitialAd
                    // Reset et sauvegarde automatiquement grâce au setter
                    failedLoadAttempts = 0
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onFallbackAd: () -> Unit,
        onAdShowed: (() -> Unit)? = null,
        onAdBlockDetected: (() -> Unit)? = null
    ) {
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
                    onAdShowed?.invoke()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")

            // Vérification du seuil de tolérance
            if (failedLoadAttempts >= MAX_FAILED_ATTEMPTS) {
                // ✅ Blocage strict : notifier via callback (dialogue Compose côté UI)
                onAdBlockDetected?.invoke()
            } else {
                // Tolérance : fallback bannière
                loadInterstitialAd()
                onFallbackAd()
            }
        }
    }
}
