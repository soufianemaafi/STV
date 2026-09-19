package com.example.stv.ads

import android.app.Activity
import com.example.stv.AdManager
import com.example.stv.features.ads.AdFlowController
import com.example.stv.features.ads.AdFrequencyManager

/**
 * Façade de compatibilité : la logique réelle vit désormais dans
 * `com.example.stv.features.ads`.
 */
class AdsController(
    adManager: AdManager,
    frequencyManager: AdFrequencyManager = AdFrequencyManager()
) {

    private val delegate = AdFlowController(adManager, frequencyManager)

    suspend fun showAdIfNeeded(
        activity: Activity,
        onAdShowed: () -> Unit = {}
    ): AdResult {
        return when (delegate.showAdIfNeeded(activity, onAdShowed = onAdShowed)) {
            AdFlowController.AdResult.AdDismissed -> AdResult.AdDismissed
            AdFlowController.AdResult.SkippedByCooldown -> AdResult.AdDismissed
            AdFlowController.AdResult.AdBlockerBlocked -> AdResult.NeedRetry
            AdFlowController.AdResult.Failed -> AdResult.Failed
            AdFlowController.AdResult.NoNetwork -> AdResult.NoNetwork
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

