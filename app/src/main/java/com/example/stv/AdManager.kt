package com.example.stv

import android.app.Activity
import android.app.AlertDialog
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

    // Test Ad Unit ID for Interstitial
    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

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
                    // Incrémente et sauvegarde automatiquement grâce au setter
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

            // Vérification du seuil de tolérance
            if (failedLoadAttempts >= MAX_FAILED_ATTEMPTS) {
                // Blocage strict : On ne lance PAS la vidéo, on force le retry
                showStrictBlockerDialog(activity)
            } else {
                // Tolérance : On laisse passer la vidéo, mais on tente de recharger pour la prochaine fois
                loadInterstitialAd()
                onAdDismissed()
            }
        }
    }

    private fun showStrictBlockerDialog(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.ad_block_strict_title))
            .setMessage(activity.getString(R.string.ad_block_strict_message))
            .setPositiveButton(activity.getString(R.string.ad_block_retry_button)) { dialog, _ ->
                // Bouton Réessayer : on tente de charger une pub et on ferme le dialog.
                // L'utilisateur devra recliquer sur "Lire le stream" une fois la pub chargée.
                dialog.dismiss()
                loadInterstitialAd()
            }
            .setNegativeButton(activity.getString(R.string.ad_block_close_button)) { dialog, _ ->
                // Bouton Fermer : on ferme juste le dialog, pas d'accès au contenu
                dialog.dismiss()
            }
            .setCancelable(false) // Obliger l'utilisateur à faire un choix
            .show()
    }
}
