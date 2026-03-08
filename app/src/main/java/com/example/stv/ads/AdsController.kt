package com.example.stv.ads

import android.app.Activity
import android.util.Log
import com.example.stv.AdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Orchestre le flux des publicités.
 *
 * Règle unique : le player ne démarre que si l'utilisateur a VU et FERMÉ la pub.
 *
 * Logique :
 * - Tentative 1..3 : retry automatique avec délai (transparent pour l'utilisateur)
 * - Si 3 échecs consécutifs : retourne NeedRetry → affiche un écran "Réessayer"
 * - Si pas de réseau : retourne NoNetwork → affiche un écran spécifique
 * - Si pub fermée : retourne AdDismissed → player démarre
 *
 * Pas de compteur persistant. Pas de détection adblock.
 * L'utilisateur est bloqué naturellement si la pub ne charge jamais.
 */
class AdsController(private val adManager: AdManager) {

    private val TAG = "AdsController"
    private val AUTO_RETRIES = 3
    private val RETRY_DELAY_MS = 2000L

    suspend fun showAdIfNeeded(
        activity: Activity,
        onAdShowed: () -> Unit = {}
    ): AdResult {
        for (attempt in 1..AUTO_RETRIES) {
            Log.d(TAG, "Ad attempt $attempt / $AUTO_RETRIES")

            val result = tryOnce(activity, onAdShowed)

            when (result) {
                is AdResult.AdDismissed -> return result
                is AdResult.NoNetwork -> return result  // Afficher écran réseau immédiatement
                is AdResult.Failed -> {
                    if (attempt < AUTO_RETRIES) {
                        delay(RETRY_DELAY_MS)
                    }
                }
                else -> {}
            }
        }

        // 3 tentatives auto échouées → laisser l'utilisateur décider
        Log.d(TAG, "Auto retries exhausted → NeedRetry")
        return AdResult.NeedRetry
    }

    private suspend fun tryOnce(
        activity: Activity,
        onAdShowed: () -> Unit
    ): AdResult {
        return suspendCancellableCoroutine { cont ->
            // Timer pour le CHARGEMENT seulement (pas l'affichage)
            val timeoutJob = kotlinx.coroutines.CoroutineScope(cont.context).launch {
                delay(10_000)
                // 10s écoulées sans que la pub soit ni chargée ni échouée
                if (cont.isActive) {
                    Log.w(TAG, "Ad load timeout (10s)")
                    cont.resume(AdResult.Failed)
                }
            }

            adManager.loadAndShow(
                activity = activity,
                onAdShowed = {
                    // ✅ Pub affichée → ANNULER le timeout
                    // On attend maintenant onAdDismissed sans limite de temps
                    timeoutJob.cancel()
                    Log.d(TAG, "Ad showing → timeout cancelled, waiting for dismiss")
                    onAdShowed()
                },
                onAdDismissed = {
                    timeoutJob.cancel()
                    Log.d(TAG, "Ad dismissed")
                    if (cont.isActive) cont.resume(AdResult.AdDismissed)
                },
                onFailed = {
                    timeoutJob.cancel()
                    Log.d(TAG, "Ad failed")
                    if (cont.isActive) cont.resume(AdResult.Failed)
                },
                onNoNetwork = {
                    timeoutJob.cancel()
                    Log.d(TAG, "No network")
                    if (cont.isActive) cont.resume(AdResult.NoNetwork)
                }
            )
        }
    }

    sealed class AdResult {
        /** Pub vue et fermée → player peut démarrer */
        data object AdDismissed : AdResult()
        /** Pub échouée (no fill, timeout, erreur) */
        data object Failed : AdResult()
        /** Pas de réseau */
        data object NoNetwork : AdResult()
        /** 3 tentatives auto échouées → afficher bouton Réessayer */
        data object NeedRetry : AdResult()
    }
}

