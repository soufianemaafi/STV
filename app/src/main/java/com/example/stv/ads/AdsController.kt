package com.example.stv.ads

import android.app.Activity
import android.util.Log
import com.example.stv.AdManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Gère la logique d'affichage d'interstitiels avec timeout et fallback.
 * Découple la logique métier d'ads de l'UI/activité.
 */
class AdsController(private val adManager: AdManager) {

    private val TAG = "AdsController"

    /**
     * Tente de charger et afficher une publicité interstitielle.
     * @param activity Activité pour afficher la pub.
     * @param onAdShowed Callback appelé quand la pub commence à s'afficher.
     * @param onAdDismissed Callback appelé quand la pub est fermée.
     * @param timeoutMs Délai d'attente max (6000ms par défaut).
     * @return Résultat du chargement initial.
     */
    suspend fun showAdIfNeeded(
        activity: Activity,
        onAdShowed: () -> Unit = {},
        onAdDismissed: () -> Unit = {},
        timeoutMs: Long = 6000
    ): AdResult {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                suspendCancellableCoroutine<AdResult> { continuation ->
                    adManager.loadAndShowInterstitial(
                        activity = activity,
                        onAdShowed = {
                            Log.d(TAG, "Ad showed successfully")
                            onAdShowed() // ✅ Callback externe
                            if (continuation.isActive) continuation.resume(AdResult.AdShowed)
                        },
                        onAdDismissed = {
                            Log.d(TAG, "Ad dismissed")
                            onAdDismissed() // ✅ Callback externe
                            if (continuation.isActive) continuation.resume(AdResult.AdDismissed)
                        },
                        onFallbackAd = {
                            Log.d(TAG, "Fallback ad (banner)")
                            if (continuation.isActive) continuation.resume(AdResult.FallbackBanner)
                        },
                        onAdBlockDetected = {
                            Log.w(TAG, "Ad blocker detected")
                            if (continuation.isActive) continuation.resume(AdResult.AdBlockDetected)
                        }
                    )
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "Ad loading timeout after ${timeoutMs}ms")
            AdResult.Timeout
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during ad load", e)
            AdResult.FallbackBanner
        }
    }

    /**
     * Résultats possibles du chargement de pub.
     */
    sealed class AdResult {
        data object AdShowed : AdResult()
        data object AdDismissed : AdResult()
        data object FallbackBanner : AdResult()
        data object Timeout : AdResult()
        data object AdBlockDetected : AdResult()
    }
}

